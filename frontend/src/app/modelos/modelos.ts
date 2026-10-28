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

export interface ErrorApi {
  status?: number;
  mensaje?: string;
  errores?: Record<string, string> | string[];
}
