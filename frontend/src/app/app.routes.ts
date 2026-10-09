import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'publicaciones'
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/refugio/login/login.component').then(m => m.LoginComponent),
    title: 'Iniciar Sesión'
  },
  {
    path: 'publicaciones',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/refugio/publicaciones-list/publicaciones-list.component').then(m => m.PublicacionesListComponent),
    title: 'Publicaciones de Adopción'
  },
  {
    path: 'publicaciones/nueva',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/refugio/publicacion-form/publicacion-form.component').then(m => m.PublicacionFormComponent),
    title: 'Nueva Publicación'
  },
  {
    path: 'publicaciones/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/refugio/publicacion-detalle/publicacion-detalle.component').then(m => m.PublicacionDetalleComponent),
    title: 'Detalle de Publicación'
  },
  {
    path: '**',
    redirectTo: 'publicaciones'
  }
];

