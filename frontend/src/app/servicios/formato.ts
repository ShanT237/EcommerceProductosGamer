import type { Producto } from '../modelos/modelos';

const moneda = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const fecha = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
const dia = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short' });

/** 250000 -> "$ 250.000" */
export const precio = (valor: number) => moneda.format(valor);
/** "2026-10-19T10:00:00" -> "19 oct 2026, 10:00 a. m." */
export const fechaHora = (iso: string) => fecha.format(new Date(iso));
/** "2026-11-30" -> "30 nov" (se arma con hora local para que la zona horaria no corra el día). */
export const fechaDia = (iso: string) => dia.format(new Date(`${iso}T00:00:00`));

/** Precio que realmente se cobra hoy: con la promoción de temporada vigente si la hay. */
export const precioVigente = (p: Producto): number => p.precioPromocional ?? p.precio;

/** Precio de lista que se muestra tachado, o `null` si no hay ninguna rebaja: el original si hay descuento inmediato, o el actual si solo hay promoción. */
export const precioTachado = (p: Producto): number | null =>
  p.precioOriginal ?? (p.precioPromocional != null ? p.precio : null);
