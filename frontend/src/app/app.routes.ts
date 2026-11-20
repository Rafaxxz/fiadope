import { Routes } from '@angular/router';
import { invitadoGuard, mfaGuard, rolGuard } from './core/guards';
import { Shell } from './shared/shell';

export const routes: Routes = [
  { path: 'login', canActivate: [invitadoGuard], title: 'FiadoPe · Acceso', loadComponent: () => import('./pages/login/login').then((m) => m.LoginPage) },
  { path: 'verificar', canActivate: [mfaGuard], title: 'FiadoPe · Verificación', loadComponent: () => import('./pages/mfa/mfa').then((m) => m.MfaPage) },
  {
    path: '',
    component: Shell,
    children: [
      { path: 'admin/tiendas', canActivate: [rolGuard('ADMIN_PLATAFORMA')], title: 'FiadoPe · Tiendas', loadComponent: () => import('./pages/tiendas/tiendas').then((m) => m.TiendasPage) },
      { path: 'panel', canActivate: [rolGuard('ADMIN_NEGOCIO')], title: 'FiadoPe · Panel', loadComponent: () => import('./pages/panel/panel').then((m) => m.PanelPage) },
      { path: 'catalogo', canActivate: [rolGuard('ADMIN_NEGOCIO')], title: 'FiadoPe · Catálogo', loadComponent: () => import('./pages/catalogo/catalogo').then((m) => m.CatalogoPage) },
      { path: 'clientes', canActivate: [rolGuard('ADMIN_NEGOCIO')], title: 'FiadoPe · Clientes', loadComponent: () => import('./pages/clientes/clientes').then((m) => m.ClientesPage) },
      { path: 'clientes/nuevo', canActivate: [rolGuard('ADMIN_NEGOCIO')], title: 'FiadoPe · Nuevo cliente', loadComponent: () => import('./pages/ficha/ficha').then((m) => m.FichaPage) },
      { path: 'clientes/:id', canActivate: [rolGuard('ADMIN_NEGOCIO')], title: 'FiadoPe · Ficha de cliente', loadComponent: () => import('./pages/ficha/ficha').then((m) => m.FichaPage) },
      { path: 'nueva-compra', canActivate: [rolGuard('ADMIN_NEGOCIO')], title: 'FiadoPe · Nueva compra', loadComponent: () => import('./pages/compra/compra').then((m) => m.CompraPage) },
      { path: 'cronograma', canActivate: [rolGuard('ADMIN_NEGOCIO')], title: 'FiadoPe · Cronograma', loadComponent: () => import('./pages/cronograma/cronograma').then((m) => m.CronogramaPage) },
      { path: 'cronograma/:id', canActivate: [rolGuard('ADMIN_NEGOCIO')], title: 'FiadoPe · Cronograma', loadComponent: () => import('./pages/cronograma/cronograma').then((m) => m.CronogramaPage) },
      { path: 'corte', canActivate: [rolGuard('ADMIN_NEGOCIO')], title: 'FiadoPe · Corte del mes', loadComponent: () => import('./pages/corte/corte').then((m) => m.CortePage) },
      { path: 'pagos', canActivate: [rolGuard('ADMIN_NEGOCIO')], title: 'FiadoPe · Registrar pago', loadComponent: () => import('./pages/pago/pago').then((m) => m.PagoPage) },
      { path: 'mi-cuenta', canActivate: [rolGuard('CLIENTE')], title: 'FiadoPe · Mi libreta', loadComponent: () => import('./pages/mi-cuenta/mi-cuenta').then((m) => m.MiCuentaPage) },
      { path: 'bitacora', canActivate: [rolGuard('ADMIN_NEGOCIO', 'ADMIN_PLATAFORMA')], title: 'FiadoPe · Bitácora', loadComponent: () => import('./pages/bitacora/bitacora').then((m) => m.BitacoraPage) },
      { path: '', pathMatch: 'full', redirectTo: 'login' },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
