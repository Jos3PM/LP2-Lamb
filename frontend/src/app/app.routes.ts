import { Routes } from '@angular/router';
import { App } from './app';
import { adminGuard } from './auth.guard';

export const routes: Routes = [
  { path: '', component: App },
  { path: 'planes-academicos', canActivate: [adminGuard], loadComponent: () => import('./academic-plans.component').then(m => m.AcademicPlansComponent) },
  { path: 'organizacion-docente', canActivate: [adminGuard], loadComponent: () => import('./teacher-organization.component').then(m => m.TeacherOrganizationComponent) },
  { path: 'cuentas-profesores', canActivate: [adminGuard], loadComponent: () => import('./teacher-accounts.component').then(m => m.TeacherAccountsComponent) },
  { path: '**', redirectTo: '' },
];
