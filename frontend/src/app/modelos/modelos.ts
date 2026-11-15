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
  /** Pedido al que pertenece la línea; `null` si es una compra suelta (p. ej. un combo). */
  pedidoId: string | null;
}

/** Respuesta de `POST /api/pedidos` y de `PUT /api/pedidos/{id}/confirmar|cancelar`: una compra por producto. */
export interface Pedido {
  pedidoId: string;
  compras: Compra[];
  total: number;
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
}

export interface Notificacion {
  id: string;
  productoId: string;
  tipo: 'DESCUENTO';
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

/** Respuesta de `POST /api/combos/{id}/comprar`: una compra PENDIENTE por producto del combo. */
export interface ResultadoCompraCombo {
  comboId: string;
  compras: Compra[];
  total: number;
}
