/**
 * Cliente SSE basado en `fetch`. `EventSource` no permite cabeceras y el JWT viaja solo en
 * `Authorization`, así que se lee el stream a mano, con reconexión (backoff + jitter), watchdog
 * de conexiones medio muertas y aviso `alReconectado` para que el llamador haga su "catch-up".
 */

export type EstadoConexion = 'conectando' | 'conectado' | 'reconectando' | 'desconectado';

export interface OpcionesSse {
  url: string;
  /** Se consulta antes de cada intento; si devuelve null se termina en `desconectado` sin reintentar. */
  obtenerToken: () => string | null;
  alEvento: (evento: string, datos: string) => void;
  alEstado: (estado: EstadoConexion) => void;
  /** Cada conexión exitosa que no sea la primera. */
  alReconectado: () => void;
  alNoAutorizado: (cuentaInactiva: boolean) => void;
}

const ESPERA_INICIAL_MS = 1000;
const ESPERA_MAXIMA_MS = 30_000;
const JITTER = 0.2;
/** Sin recibir ningún byte durante este tiempo (el servidor manda latido cada 25 s) se reconecta. */
const WATCHDOG_MS = 60_000;
/** Una conexión que cae antes de esto cuenta como fallo (evita un bucle de reconexión sin espera). */
const DURACION_MINIMA_MS = 2000;
const CODIGO_CUENTA_INACTIVA = 'CUENTA_INACTIVA';

type ResultadoIntento = 'fin' | 'terminar' | 'reintentar';

export function abrirSse(opciones: OpcionesSse): { cerrar: () => void } {
  let cerrado = false;
  let enCurso = false;
  let yaConecto = false;
  let fallos = 0;
  let estado: EstadoConexion | null = null;
  let controlador: AbortController | null = null;
  let temporizadorReintento: ReturnType<typeof setTimeout> | undefined;
  let temporizadorWatchdog: ReturnType<typeof setTimeout> | undefined;
  let reintentoInmediato = false;

  const fijarEstado = (nuevo: EstadoConexion) => {
    if (cerrado || nuevo === estado) return;
    estado = nuevo;
    opciones.alEstado(nuevo);
  };

  const detenerWatchdog = () => {
    if (temporizadorWatchdog !== undefined) clearTimeout(temporizadorWatchdog);
    temporizadorWatchdog = undefined;
  };

  const reiniciarWatchdog = (ctl: AbortController) => {
    detenerWatchdog();
    temporizadorWatchdog = setTimeout(() => ctl.abort(), WATCHDOG_MS);
  };

  const esperaConBackoff = (): number => {
    const base = Math.min(ESPERA_INICIAL_MS * 2 ** Math.max(0, fallos - 1), ESPERA_MAXIMA_MS);
    return Math.round(base * (1 + (Math.random() * 2 - 1) * JITTER));
  };

  const programarReintento = (espera: number) => {
    if (cerrado) return;
    if (temporizadorReintento !== undefined) clearTimeout(temporizadorReintento);
    temporizadorReintento = setTimeout(() => {
      temporizadorReintento = undefined;
      void conectar();
    }, espera);
  };

  /** Interpreta un bloque SSE (líneas hasta la línea en blanco) y lo entrega si trae evento o datos. */
  const despachar = (bloque: string) => {
    let evento = '';
    const datos: string[] = [];
    for (const linea of bloque.split('\n')) {
      if (linea === '' || linea.startsWith(':')) continue; // latido o comentario
      const dosPuntos = linea.indexOf(':');
      const campo = dosPuntos === -1 ? linea : linea.slice(0, dosPuntos);
      let valor = dosPuntos === -1 ? '' : linea.slice(dosPuntos + 1);
      if (valor.startsWith(' ')) valor = valor.slice(1);
      if (campo === 'event') evento = valor;
      else if (campo === 'data') datos.push(valor);
    }
    if (evento === '' && datos.length === 0) return;
    opciones.alEvento(evento || 'message', datos.join('\n'));
  };

  const leer = async (cuerpo: ReadableStream<Uint8Array>, ctl: AbortController): Promise<void> => {
    const lector = cuerpo.getReader();
    const decodificador = new TextDecoder('utf-8');
    let pendiente = '';
    let cr = ''; // un \r al final de un fragmento puede ser la mitad de un \r\n
    try {
      while (!cerrado) {
        const { done, value } = await lector.read();
        if (done) return;
        reiniciarWatchdog(ctl); // cualquier byte (también un latido) prueba que la conexión vive
        let texto = cr + decodificador.decode(value, { stream: true });
        cr = texto.endsWith('\r') ? '\r' : '';
        if (cr) texto = texto.slice(0, -1);
        pendiente += texto.replace(/\r\n?/g, '\n');
        let corte = pendiente.indexOf('\n\n');
        while (corte !== -1) {
          despachar(pendiente.slice(0, corte));
          pendiente = pendiente.slice(corte + 2);
          corte = pendiente.indexOf('\n\n');
        }
      }
    } finally {
      lector.cancel().catch(() => undefined);
    }
  };

  const intentar = async (token: string, ctl: AbortController): Promise<ResultadoIntento> => {
    const respuesta = await fetch(opciones.url, {
      headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
      signal: ctl.signal,
      cache: 'no-store',
    });

    if (respuesta.status === 401) {
      const cuerpo = (await respuesta.json().catch(() => null)) as { codigo?: string } | null;
      fijarEstado('desconectado');
      opciones.alNoAutorizado(cuerpo?.codigo === CODIGO_CUENTA_INACTIVA);
      return 'terminar';
    }
    if (!respuesta.ok) {
      const reintentable = respuesta.status >= 500 || respuesta.status === 408 || respuesta.status === 429;
      if (reintentable) return 'reintentar';
      fijarEstado('desconectado');
      return 'terminar';
    }
    if (!respuesta.body) return 'reintentar';

    const inicio = Date.now();
    const eraReconexion = yaConecto;
    yaConecto = true;
    fijarEstado('conectado');
    if (eraReconexion) opciones.alReconectado();
    reiniciarWatchdog(ctl);

    await leer(respuesta.body, ctl);
    // El servidor cerró el stream (p. ej. el timeout de 5 min): se reconecta ya, sin mostrar "reconectando".
    return Date.now() - inicio >= DURACION_MINIMA_MS ? 'fin' : 'reintentar';
  };

  const conectar = async (): Promise<void> => {
    if (cerrado || enCurso) return;
    const token = opciones.obtenerToken();
    if (!token) {
      fijarEstado('desconectado');
      return;
    }

    enCurso = true;
    const ctl = new AbortController();
    controlador = ctl;
    let resultado: ResultadoIntento = 'reintentar';
    try {
      resultado = await intentar(token, ctl);
      if (resultado === 'fin') fallos = 0;
    } catch {
      resultado = 'reintentar'; // error de red o aborto (watchdog / reconexión forzada)
    } finally {
      enCurso = false;
      controlador = null;
      detenerWatchdog();
    }

    if (cerrado || resultado === 'terminar') return;
    if (resultado === 'fin') {
      programarReintento(0);
      return;
    }
    fallos += 1;
    fijarEstado('reconectando');
    const inmediato = reintentoInmediato;
    reintentoInmediato = false;
    if (inmediato) fallos = 0;
    programarReintento(inmediato ? 0 : esperaConBackoff());
  };

  const alVolverLaRed = () => {
    if (cerrado || estado === 'conectado' || estado === 'desconectado') return;
    if (temporizadorReintento !== undefined) {
      clearTimeout(temporizadorReintento);
      temporizadorReintento = undefined;
    }
    fallos = 0;
    if (controlador) {
      reintentoInmediato = true; // hay un intento colgado: se aborta y se reintenta sin espera
      controlador.abort();
    } else {
      void conectar();
    }
  };

  const alPerderLaRed = () => {
    if (cerrado) return;
    fijarEstado('reconectando');
  };

  if (typeof window !== 'undefined') {
    window.addEventListener('online', alVolverLaRed);
    window.addEventListener('offline', alPerderLaRed);
  }

  fijarEstado('conectando');
  void conectar();

  return {
    cerrar: () => {
      cerrado = true;
      if (temporizadorReintento !== undefined) clearTimeout(temporizadorReintento);
      temporizadorReintento = undefined;
      detenerWatchdog();
      controlador?.abort();
      if (typeof window !== 'undefined') {
        window.removeEventListener('online', alVolverLaRed);
        window.removeEventListener('offline', alPerderLaRed);
      }
    },
  };
}
