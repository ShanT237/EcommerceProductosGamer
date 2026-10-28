import { Injectable, signal } from '@angular/core';

export interface Toast { id: number; tipo: 'ok' | 'error' | 'info'; texto: string; }

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private n = 0;

  ok(texto: string) { this.mostrar('ok', texto); }
  error(texto: string) { this.mostrar('error', texto, 6000); }
  info(texto: string) { this.mostrar('info', texto); }

  private mostrar(tipo: Toast['tipo'], texto: string, ms = 3800) {
    const id = ++this.n;
    this.toasts.update((t) => [...t, { id, tipo, texto }]);
    setTimeout(() => this.quitar(id), ms);
  }

  quitar(id: number) { this.toasts.update((t) => t.filter((x) => x.id !== id)); }
}
