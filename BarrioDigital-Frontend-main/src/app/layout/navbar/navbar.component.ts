import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm">
      <div class="container-fluid px-4">
        <a class="navbar-brand fw-bold d-flex align-items-center gap-2" routerLink="/dashboard">
          <i class="bi bi-buildings-fill fs-4 text-warning"></i>
          <span>BarrioDigital</span>
          <span class="badge bg-light text-primary fs-7 ms-1">Duoc UC</span>
        </a>

        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarMain">
          <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="navbarMain">
          <!-- Menú de navegación según roles -->
          <ul class="navbar-nav me-auto mb-2 mb-lg-0" *ngIf="authService.currentUser$ | async as user">
            <li class="nav-item">
              <a class="nav-link" routerLink="/dashboard" routerLinkActive="active">
                <i class="bi bi-speedometer2 me-1"></i> Dashboard
              </a>
            </li>
            <li class="nav-item">
              <a class="nav-link" routerLink="/requests" routerLinkActive="active">
                <i class="bi bi-card-checklist me-1"></i> Trámites
              </a>
            </li>
            <li class="nav-item" *ngIf="user.role === 'Admin' || user.role === 'Funcionario'">
              <a class="nav-link" routerLink="/catalog" routerLinkActive="active">
                <i class="bi bi-grid-3x3-gap me-1"></i> Catálogo y Cupos
              </a>
            </li>
            <li class="nav-item" *ngIf="user.role === 'Admin'">
              <a class="nav-link" routerLink="/reports" routerLinkActive="active">
                <i class="bi bi-bar-chart-line me-1"></i> Reportería KPIs
              </a>
            </li>
            <li class="nav-item" *ngIf="user.role === 'Admin' || user.role === 'Auditor'">
              <a class="nav-link" routerLink="/audit" routerLinkActive="active">
                <i class="bi bi-shield-check me-1"></i> Auditoría
              </a>
            </li>
          </ul>

          <!-- Perfil y acciones de usuario -->
          <div class="d-flex align-items-center gap-3" *ngIf="authService.currentUser$ | async as user; else loginBtn">
            <div class="text-end text-light">
              <div class="fw-semibold small">{{ user.name }}</div>
              <div class="d-flex align-items-center justify-content-end gap-1">
                <span class="badge" [ngClass]="getRoleBadgeClass(user.role)">
                  {{ user.role }}
                </span>
                <span class="badge bg-secondary small" *ngIf="user.isMicrosoftAuth">
                  <i class="bi bi-microsoft me-1"></i>Azure AD
                </span>
              </div>
            </div>
            <button class="btn btn-outline-light btn-sm" (click)="logout()" title="Cerrar sesión">
              <i class="bi bi-box-arrow-right me-1"></i> Salir
            </button>
          </div>

          <ng-template #loginBtn>
            <div class="d-flex ms-auto">
              <a class="btn btn-light btn-sm fw-semibold" routerLink="/login">
                <i class="bi bi-person-fill me-1"></i> Iniciar Sesión
              </a>
            </div>
          </ng-template>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      background: linear-gradient(135deg, #1e3a8a 0%, #0f172a 100%) !important;
    }
    .nav-link {
      font-weight: 500;
      color: rgba(255, 255, 255, 0.85) !important;
      transition: all 0.2s ease;
    }
    .nav-link:hover, .nav-link.active {
      color: #38bdf8 !important;
      font-weight: 600;
    }
    .fs-7 {
      font-size: 0.72rem;
    }
  `]
})
export class NavbarComponent {
  authService = inject(AuthService);

  logout(): void {
    this.authService.logout();
  }

  getRoleBadgeClass(role: string): string {
    switch (role?.toLowerCase()) {
      case 'admin':
        return 'bg-danger text-white';
      case 'funcionario':
        return 'bg-info text-dark';
      case 'vecino':
        return 'bg-success text-white';
      case 'auditor':
        return 'bg-warning text-dark';
      default:
        return 'bg-light text-dark';
    }
  }
}
