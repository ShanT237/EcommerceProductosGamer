const moneda = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const fecha = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

/** 250000 -> "$ 250.000" */
export const precio = (valor: number) => moneda.format(valor);
/** "2026-10-19T10:00:00" -> "19 oct 2026, 10:00 a. m." */
export const fechaHora = (iso: string) => fecha.format(new Date(iso));
