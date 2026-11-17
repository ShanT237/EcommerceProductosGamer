// Interfaces con la forma de los datos que devuelve el backend.

export type Rol = 'COMPRADOR' | 'VENDEDOR';
export type Gama = 'BAJA' | 'MEDIA' | 'ALTA';
export type Exclusividad = 'NORMAL' | 'EXCLUSIVO';
export type EstadoCompra = 'PENDIENTE' | 'COMPLETADA' | 'REEMBOLSADA' | 'CANCELADA' | 'EXPIRADA';
export type TipoVendedor = 'PERSONA_NATURAL' | 'EMPRESA';

export interface Sesion {
  id: string;
  correo: string;
  rol: Rol;
  exp: number;
}

/**
 * Datos de la cuenta autenticada que muestra la pantalla de perfil. Lo comparten comprador y vendedor:
 * `puntos` solo viene del comprador; `tipo` y `documento` solo del vendedor (no se editan).
 */
export interface PerfilCuenta {
  id: string;
  nombre: string;
  correo: string;
  activo: boolean;
  puntos?: number;
  tipo?: TipoVendedor;
  documento?: string;
}

/** Solo los productos DIGITAL admiten «descargar»; los productos sin tipo se tratan como FISICO. */
export type TipoProducto = 'DIGITAL' | 'FISICO';

export interface Producto {
  id: string;
  vendedorId: string;
  nombre: string;
  descripcion: string;
  precio: number;
  stock: number;
  gama: Gama;
  exclusividad: Exclusividad;
  categoria: string | null;
  /** Precio antes del descuento inmediato; `null` o ausente si no hay descuento activo. */
  precioOriginal?: number | null;
  /** Porcentaje del descuento activo; `null` o ausente si no hay. */
  porcentajeDescuento?: number | null;
  /** Precio que se cobrará con la promoción de temporada vigente hoy; `null` o ausente si no hay. */
  precioPromocional?: number | null;
  /** Porcentaje de la promoción de temporada vigente. */
  porcentajePromocion?: number | null;
  /** Último día de la promoción (AAAA-MM-DD, inclusive). */
  promocionHasta?: string | null;
  /** `true` si el vendedor lo pausó (no se ve en el catálogo público). Solo lo trae `GET /productos/mios`. */
  pausado?: boolean;
  /** Físico o digital; puede faltar en respuestas antiguas (equivale a `FISICO`). */
  tipo?: TipoProducto;
  /** URL HTTPS de Cloudinary; ausente en productos publicados sin fotografía. */
  imagenUrl?: string | null;
}

/** Respuesta de `POST /api/descuentos` y `DELETE /api/descuentos/{productoId}`. */
export interface ProductoDescuento {
  id: string;
  nombre: string;
  nuevoPrecio: number;
  precioOriginal: number | null;
  porcentajeDescuento: number | null;
}

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface FiltroCatalogo {
  texto?: string;
  categoria?: string;
  gama?: Gama | '';
  precioMin?: number | null;
  precioMax?: number | null;
  soloPreventa?: boolean;
}

export interface Compra {
  id: string;
  usuarioId: string;
  productoId: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  fecha: string;
  estado: EstadoCompra;
  /** Pedido al que pertenece la línea; `null` si es una compra individual. */
  pedidoId: string | null;
  /** Combo de origen, si la línea se compró como parte de un combo. */
  comboId?: string | null;
  /** Nombre guardado al momento de comprar, aunque luego se elimine o cambie el combo. */
  comboNombre?: string | null;
  /** `true` si ya se marcó la descarga (un producto digital descargado ya no se puede reembolsar). */
  descargado?: boolean;
}

/** Respuesta de `POST /api/pedidos` y de `PUT /api/pedidos/{id}/confirmar|cancelar`: una compra por producto. */
export interface Pedido {
  pedidoId: string;
  compras: Compra[];
  total: number;
  /** `true` si alguna línea recibió el descuento de un cupón (el total ya lo incluye). */
  cuponAplicado?: boolean;
}

export interface MovimientoPuntos {
  id: string;
  cantidad: number;
  tipo: 'GANADO' | 'REDIMIDO';
  fecha: string;
}

export interface ReporteVentas {
  ingresos: number;
  unidades: number;
  comision: number;
}

export interface ProductoVendido {
  productoId: string;
  nombre: string;
  unidades: number;
  calificacionPromedio: number;
}

export interface ProductoStockBajo {
  productoId: string;
  nombre: string;
  stock: number;
  agotado: boolean;
}

export interface MetricasResenas {
  promedio: number;
  total: number;
  distribucion: Record<string, number>;
  positivas: number;
  negativas: number;
}

export interface ReporteGlobal {
  comprasCompletadas: number;
  ingresos: number;
  productosActivos: number;
  vendedoresActivos: number;
}

export interface NuevoProducto {
  nombre: string;
  descripcion: string;
  precio: number;
  stock: number;
  gama: Gama;
  exclusividad: Exclusividad;
  /** Opcional: si no se envía, el backend publica el producto como FISICO. */
  tipo?: TipoProducto;
}

export interface ImagenProductoSubida {
  productoId: string;
  imagenUrl: string;
}

/** Avisos del comprador (wishlist, metas, regalos) y del vendedor (stock, ventas, reembolsos, reseñas). */
export type TipoNotificacion =
  | 'DESCUENTO' | 'STOCK_DISPONIBLE' | 'PREVENTA_DISPONIBLE' | 'META_ALCANZADA' | 'REGALO_ENVIADO'
  | 'STOCK_BAJO' | 'STOCK_AGOTADO' | 'NUEVA_VENTA' | 'REEMBOLSO_SOLICITADO' | 'NUEVA_RESENA' | 'NUEVA_VALORACION'
  | 'REGALO_ELEGIDO' | 'REGALO_CANCELADO';

export interface Notificacion {
  id: string;
  /** Opcional: avisos como META_ALCANZADA no se refieren a un producto. */
  productoId: string | null;
  tipo: TipoNotificacion;
  mensaje: string;
  fecha: string;
  leida: boolean;
}

export interface ErrorApi {
  status?: number;
  mensaje?: string;
  errores?: Record<string, string> | string[];
}

export interface PromocionTemporada {
  id: string;
  productoId: string;
  productoNombre: string;
  porcentaje: number;
  desde: string; // ISO yyyy-MM-dd
  hasta: string;
}

/** Preventa programada para un producto con fecha de lanzamiento futura. */
export interface Preventa {
  id: string;
  productoId: string;
  fechaLanzamiento: string;
  descripcion: string;
}

/** Producto dentro de un combo, tal como lo devuelve `GET /api/combos`. */
export interface ProductoDeCombo {
  id: string;
  nombre: string;
  precio: number;
  stock: number;
}

/** Combo con sus productos y los precios ya calculados por el backend (`GET /api/combos` y `/combos/mios`). */
export interface Combo {
  id: string;
  vendedorId: string;
  nombre: string;
  descripcion: string;
  /** Porcentaje de descuento del combo (0 a 99). */
  descuento: number;
  productos: ProductoDeCombo[];
  precioSinDescuento: number;
  precioConDescuento: number;
}

/** Respuesta de crear o modificar un combo: solo trae los ids de sus productos. */
export interface ComboCreado {
  id: string;
  vendedorId: string;
  nombre: string;
  descripcion: string;
  descuento: number;
  productos: string[];
}

export interface NuevoCombo {
  nombre: string;
  descripcion: string;
  descuento: number;
  productoIds: string[];
}

export interface ModificarCombo {
  nombre: string;
  descripcion: string;
  descuento: number;
}

/** Respuesta de `POST /api/combos/{id}/comprar`: líneas de un único pedido del combo. */
export interface ResultadoCompraCombo {
  comboId: string;
  pedidoId: string;
  compras: Compra[];
  total: number;
  cuponAplicado?: boolean;
}

/** Cupón de descuento (`/api/cupones`). `vendedorId` es `null` si es global; si tiene dueño solo descuenta sus productos. */
export interface Cupon {
  id: string;
  codigo: string;
  /** Porcentaje de descuento (mayor a 0 y menor que 100). */
  porcentaje: number;
  /** Último día de uso (AAAA-MM-DD, inclusive). */
  vence: string;
  usosMaximos: number;
  usos: number;
  vendedorId: string | null;
}

export interface NuevoCupon {
  codigo: string;
  porcentaje: number;
  vence: string;
  usosMaximos: number;
}

export type EstadoRegalo = 'DISPONIBLE' | 'ELEGIDO' | 'ENVIADO' | 'ENTREGADO' | 'CANCELADO';

/** Respuesta de la API de regalos. Apunta a un producto o a un combo (nunca a ambos). */
export interface Regalo {
  id: string;
  estado: EstadoRegalo;
  idProducto: string | null;
  idCombo: string | null;
  idUsuario: string | null;
  notificado: boolean;
  fechaCreacion: string;
  fechaEntrega: string | null;
}

/** `GET /api/regalos/progreso`: un regalo por cada `comprasPorRegalo` compras completadas. */
export interface ProgresoRegalos {
  comprasCompletadas: number;
  comprasPorRegalo: number;
  regalosAsignados: number;
  regalosDisponibles: number;
  comprasParaSiguiente: number;
}

/** Cuerpo de `POST /api/regalos`: debe venir exactamente uno de los dos. */
export interface NuevoRegalo {
  productoId?: string;
  comboId?: string;
}

/** Meta de recompensa (`/api/metas-recompensa`): al llegar a `comprasRequeridas` compras se ofrece `productoRegaloId`. */
export interface MetaRecompensa {
  id: string;
  comprasRequeridas: number;
  productoRegaloId: string;
  activa: boolean;
}

/** Cuerpo de `POST /api/metas-recompensa`. */
export interface NuevaMeta {
  comprasRequeridas: number;
  productoRegaloId: string;
}
