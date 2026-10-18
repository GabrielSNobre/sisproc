import { Routes } from '@angular/router';
import { autenticadoGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: 'inicio', canActivate: [autenticadoGuard], loadComponent: () => import('./pages/inicio/inicio.component').then(m => m.InicioComponent) },
  { path: 'perfil', canActivate: [autenticadoGuard], loadComponent: () => import('./pages/perfil/perfil.component').then(m => m.PerfilComponent) },
  { path: 'usuarios', canActivate: [autenticadoGuard], loadComponent: () => import('./pages/usuarios/usuarios.component').then(m => m.UsuariosComponent) },
  { path: 'equipes', canActivate: [autenticadoGuard], loadComponent: () => import('./pages/equipes/equipes.component').then(m => m.EquipesComponent) },
  { path: 'processos', canActivate: [autenticadoGuard], loadComponent: () => import('./pages/processos/processos.component').then(m => m.ProcessosComponent) },
  { path: 'minhas-tarefas', canActivate: [autenticadoGuard], loadComponent: () => import('./pages/minhas-tarefas/minhas-tarefas.component').then(m => m.MinhasTarefasComponent) },
  { path: '', pathMatch: 'full', redirectTo: 'inicio' },
  { path: '**', redirectTo: 'inicio' }
];
