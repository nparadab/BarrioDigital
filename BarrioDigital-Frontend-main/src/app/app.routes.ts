import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { RequestsComponent } from './pages/requests/requests.component';
import { CatalogComponent } from './pages/catalog/catalog.component';
import { ReportsComponent } from './pages/reports/reports.component';
import { AuditComponent } from './pages/audit/audit.component';
import { roleGuard } from './auth/role.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [roleGuard]
  },
  {
    path: 'requests',
    component: RequestsComponent,
    canActivate: [roleGuard],
    data: { roles: ['Admin', 'Funcionario', 'Vecino', 'Auditor'] }
  },
  {
    path: 'catalog',
    component: CatalogComponent,
    canActivate: [roleGuard],
    data: { roles: ['Admin', 'Funcionario'] }
  },
  {
    path: 'reports',
    component: ReportsComponent,
    canActivate: [roleGuard],
    data: { roles: ['Admin'] }
  },
  {
    path: 'audit',
    component: AuditComponent,
    canActivate: [roleGuard],
    data: { roles: ['Admin', 'Auditor'] }
  },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: 'dashboard' }
];
