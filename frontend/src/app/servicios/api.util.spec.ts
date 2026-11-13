import { HttpErrorResponse } from '@angular/common/http';
import { mensajeDeError } from './api.util';

describe('mensajeDeError', () => {
  it('avisa cuando no hay conexión con el servidor (status 0)', () => {
    const error = new HttpErrorResponse({ status: 0 });
    expect(mensajeDeError(error)).toBe('No hay conexión con el servidor. ¿Está encendido el backend?');
  });

  describe('prioriza el mensaje que devuelve el backend', () => {
    it('422 con mensaje de regla de negocio', () => {
      const error = new HttpErrorResponse({ status: 422, error: { mensaje: 'No hay suficiente stock disponible' } });
      expect(mensajeDeError(error)).toBe('No hay suficiente stock disponible');
    });

    it('400 con mensaje de validación', () => {
      const error = new HttpErrorResponse({ status: 400, error: { mensaje: 'nombre: El nombre es obligatorio' } });
      expect(mensajeDeError(error)).toBe('nombre: El nombre es obligatorio');
    });

    it('500 con mensaje propio no se reemplaza por el texto genérico', () => {
      const error = new HttpErrorResponse({ status: 500, error: { mensaje: 'Fallo controlado' } });
      expect(mensajeDeError(error)).toBe('Fallo controlado');
    });
  });

  describe('usa un texto por defecto cuando no hay cuerpo', () => {
    const casos: Array<[number, string]> = [
      [400, 'Los datos enviados no son válidos.'],
      [401, 'Tu sesión no es válida. Inicia sesión de nuevo.'],
      [403, 'No tienes permiso para hacer esto.'],
      [404, 'No se encontró lo que buscabas.'],
      [409, 'Esa información ya existe o cambió. Revisa e inténtalo de nuevo.'],
      [422, 'La operación no cumple las reglas del negocio.'],
      [500, 'Error del servidor. Inténtalo de nuevo en unos minutos.'],
    ];

    it.each(casos)('status %i', (status, esperado) => {
      const error = new HttpErrorResponse({ status });
      expect(mensajeDeError(error)).toBe(esperado);
    });
  });

  it('un status sin texto definido cae en el mensaje inesperado', () => {
    const error = new HttpErrorResponse({ status: 418 });
    expect(mensajeDeError(error)).toBe('Ocurrió un error inesperado.');
  });

  it('un Error cualquiera devuelve el mensaje inesperado', () => {
    expect(mensajeDeError(new Error('boom'))).toBe('Ocurrió un error inesperado.');
  });
});
