import { Component, computed, input } from '@angular/core';
import { Gama } from '../../modelos/modelos';

let contador = 0;

type Tipo = 'teclado' | 'mouse' | 'audifonos' | 'silla' | 'monitor' | 'control' | 'cristal';

/**
 * Ilustración vectorial del producto. El backend aún no guarda imágenes, así que el arte se elige
 * según el nombre/categoría del producto y se tiñe con el color de su gama.
 */
@Component({
  selector: 'app-arte-producto',
  templateUrl: './arte-producto.html',
  styleUrl: './arte-producto.css',
})
export class ArteProducto {
  nombre = input.required<string>();
  categoria = input<string | null>(null);
  gama = input<Gama>('MEDIA');

  // Cada instancia necesita ids propios: si se repiten, todas las tarjetas heredarían el color de la primera.
  protected readonly uid = `arte${contador++}`;
  protected readonly urlHalo = `url(#${this.uid}-halo)`;
  protected readonly urlCuerpo = `url(#${this.uid}-cuerpo)`;
  protected readonly urlAcento = `url(#${this.uid}-acento)`;
  protected readonly urlBrillo = `url(#${this.uid}-brillo)`;

  protected tipo = computed<Tipo>(() => {
    const t = `${this.nombre()} ${this.categoria() ?? ''}`.toLowerCase();
    if (/teclad|keyboard/.test(t)) return 'teclado';
    if (/mouse|rat[oó]n/.test(t)) return 'mouse';
    if (/audif|aur[ií]c|headset|diadema/.test(t)) return 'audifonos';
    if (/silla|chair/.test(t)) return 'silla';
    if (/monitor|pantalla|display/.test(t)) return 'monitor';
    if (/control|mando|gamepad|joystick/.test(t)) return 'control';
    return 'cristal';
  });

  protected teclas = Array.from({ length: 36 }, (_, i) => ({
    x: 38 + (i % 12) * 13.2,
    y: 64 + Math.floor(i / 12) * 14,
    retraso: ((i % 12) * 0.09 + Math.floor(i / 12) * 0.2).toFixed(2),
  }));
}
