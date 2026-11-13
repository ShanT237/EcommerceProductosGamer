import { Routes } from '@angular/router';
import { Inicio } from './componentes/inicio/inicio';
import { Login } from './componentes/login/login';
import { Registro } from './componentes/registro/registro';
import { Catalogo } from './componentes/catalogo/catalogo';
import { MisCompras } from './componentes/mis-compras/mis-compras';
import { Perfil } from './componentes/perfil/perfil';
import { PanelVendedor } from './componentes/panel-vendedor/panel-vendedor';
import { authGuard, rolGuard } from './core/guards';

export const routes: Routes = [
  { path: '', component: Inicio, title: 'GamerZone · Marketplace Gamer' },
  { path: 'login', component: Login, title: 'Ingresar · GamerZone' },
  { path: 'registro', component: Registro, title: 'Crear cuenta · GamerZone' },
  { path: 'catalogo', component: Catalogo, canActivate: [authGuard], title: 'Catálogo · GamerZone' },
  { path: 'mis-compras', component: MisCompras, canActivate: [rolGuard('COMPRADOR')], title: 'Mis compras · GamerZone' },
  { path: 'perfil', component: Perfil, canActivate: [rolGuard('COMPRADOR')], title: 'Mi perfil · GamerZone' },
  { path: 'vendedor', component: PanelVendedor, canActivate: [rolGuard('VENDEDOR')], title: 'Panel de vendedor · GamerZone' },
  { path: '**', pathMatch: 'full', redirectTo: '' },
];
