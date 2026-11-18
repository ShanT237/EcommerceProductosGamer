import { Routes } from '@angular/router';
import { Inicio } from './componentes/inicio/inicio';
import { Login } from './componentes/login/login';
import { Registro } from './componentes/registro/registro';
import { OlvideContrasena } from './componentes/olvide-contrasena/olvide-contrasena';
import { RestablecerContrasena } from './componentes/restablecer-contrasena/restablecer-contrasena';
import { Catalogo } from './componentes/catalogo/catalogo';
import { Carrito } from './componentes/carrito/carrito';
import { MisCompras } from './componentes/mis-compras/mis-compras';
import { Favoritos } from './componentes/favoritos/favoritos';
import { Combos } from './componentes/combos/combos';
import { Perfil } from './componentes/perfil/perfil';
import { Notificaciones } from './componentes/notificaciones/notificaciones';
import { ChatEnVivo } from './componentes/chat-en-vivo/chat-en-vivo';
import { Regalos } from './componentes/regalos/regalos';
import { PanelVendedor } from './componentes/panel-vendedor/panel-vendedor';
import { guestGuard, rolGuard } from './core/guards';

export const routes: Routes = [
  { path: '', component: Inicio, title: 'GamerZone · Marketplace Gamer' },
  { path: 'login', component: Login, canActivate: [guestGuard], title: 'Ingresar · GamerZone' },
  { path: 'registro', component: Registro, title: 'Crear cuenta · GamerZone' },
  { path: 'olvide-contrasena', component: OlvideContrasena, canActivate: [guestGuard], title: 'Recuperar contraseña · GamerZone' },
  { path: 'restablecer', component: RestablecerContrasena, title: 'Nueva contraseña · GamerZone' },
  { path: 'catalogo', component: Catalogo, title: 'Catálogo · GamerZone' },
  { path: 'combos', component: Combos, title: 'Combos · GamerZone' },
  { path: 'carrito', component: Carrito, canActivate: [rolGuard('COMPRADOR')], title: 'Carrito · GamerZone' },
  { path: 'mis-compras', component: MisCompras, canActivate: [rolGuard('COMPRADOR')], title: 'Mis compras · GamerZone' },
  { path: 'favoritos', component: Favoritos, canActivate: [rolGuard('COMPRADOR')], title: 'Favoritos · GamerZone' },
  { path: 'perfil', component: Perfil, canActivate: [rolGuard('COMPRADOR', 'VENDEDOR')], title: 'Mi perfil · GamerZone' },
  { path: 'regalos', component: Regalos, canActivate: [rolGuard('COMPRADOR')], title: 'Regalos · GamerZone' },
  { path: 'notificaciones', component: Notificaciones, canActivate: [rolGuard('COMPRADOR', 'VENDEDOR')], title: 'Notificaciones · GamerZone' },
  { path: 'chat', component: ChatEnVivo, canActivate: [rolGuard('COMPRADOR', 'VENDEDOR')], title: 'Chat · GamerZone' },
  { path: 'chat/:id', component: ChatEnVivo, canActivate: [rolGuard('COMPRADOR', 'VENDEDOR')], title: 'Chat · GamerZone' },
  { path: 'vendedor', component: PanelVendedor, canActivate: [rolGuard('VENDEDOR')], title: 'Panel de vendedor · GamerZone' },
  { path: '**', pathMatch: 'full', redirectTo: '' },
];
