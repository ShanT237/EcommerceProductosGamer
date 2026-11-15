import { TestBed } from '@angular/core/testing';
import { ConfirmacionService } from './confirmacion.service';

describe('ConfirmacionService', () => {
  let servicio: ConfirmacionService;

  beforeEach(() => {
    servicio = TestBed.inject(ConfirmacionService);
  });

  it('abre el aviso con los valores por defecto y se resuelve en true al aceptar', async () => {
    const respuesta = servicio.pedir({ mensaje: '¿Eliminar X?' });

    expect(servicio.actual()).toMatchObject({ titulo: 'Confirmar', confirmar: 'Aceptar', cancelar: 'Cancelar', peligro: true });
    servicio.responder(true);

    expect(await respuesta).toBe(true);
    expect(servicio.actual()).toBeNull();
  });

  it('se resuelve en false al cancelar', async () => {
    const respuesta = servicio.pedir({ mensaje: '¿Seguro?' });
    servicio.responder(false);

    expect(await respuesta).toBe(false);
  });

  it('un aviso nuevo cancela el anterior que seguía abierto', async () => {
    const primero = servicio.pedir({ mensaje: 'uno' });
    const segundo = servicio.pedir({ mensaje: 'dos', peligro: false });

    expect(await primero).toBe(false);
    expect(servicio.actual()?.mensaje).toBe('dos');
    servicio.responder(true);
    expect(await segundo).toBe(true);
  });
});
