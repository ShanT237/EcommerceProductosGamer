import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../servicios/auth.service';
import { ReporteService } from '../../servicios/reporte.service';
import { ReporteGlobal } from '../../modelos/modelos';
import { precio } from '../../servicios/formato';
import { Escena3d } from '../escena3d/escena3d';

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, Escena3d],
  templateUrl: './inicio.html',
  styleUrl: './inicio.css',
})
export class Inicio implements OnInit {
  protected auth = inject(AuthService);
  private reportes = inject(ReporteService);
  protected global = signal<ReporteGlobal | null>(null);
  protected precio = precio;

  protected pasos = [
    { n: '01', titulo: 'Explora', texto: 'Recorre el catálogo de vendedores independientes y filtra por gama, precio y exclusividad.' },
    { n: '02', titulo: 'Compra', texto: 'Reserva tu producto y confirma el pago simulado. El stock se descuenta al instante.' },
    { n: '03', titulo: 'Gana', texto: 'Cada compra completada suma puntos que luego puedes redimir por recompensas.' },
  ];

  protected ventajas = [
    { icono: '⚡', titulo: 'Vendedores verificados', texto: 'Personas y empresas con documento registrado. Cada vendedor gestiona su propio inventario.' },
    { icono: '🛡', titulo: 'Reglas que te protegen', texto: 'Reembolso dentro de 48 horas, reseñas solo de compras reales y eliminación lógica de productos.' },
    { icono: '🎁', titulo: 'Puntos y regalos', texto: 'Acumula puntos, redímelos y desbloquea regalos por metas de compra.' },
  ];

  ngOnInit() {
    if (this.auth.autenticado()) {
      this.reportes.global().subscribe({ next: (g) => this.global.set(g), error: () => this.global.set(null) });
    }
  }
}
