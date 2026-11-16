/**
 * Configuración de producción. Ajustar `apiUrl` a la URL pública del backend
 * (por ejemplo 'https://api.midominio.com/api'). Con '/api' se asume que un proxy inverso
 * sirve el frontend y la API bajo el mismo dominio.
 */
export const environment = {
  production: true,
  apiUrl: '/api',
};
