import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container py-5">
      <div class="row justify-content-center">
        <div class="col-lg-6 col-md-8">
          <div class="card shadow-lg border-0 rounded-4 overflow-hidden">
            <div class="card-header bg-dark text-white p-4 text-center" style="background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 100%);">
              <div class="display-6 fw-bold mb-2">
                <i class="bi bi-buildings-fill text-warning me-2"></i>BarrioDigital
              </div>
              <p class="mb-0 text-light opacity-75">
                Plataforma de Trámites Comunales y Atención Vecinal &bull; Duoc UC
              </p>
            </div>

            <div class="card-body p-4 p-md-5 text-center">
              <!-- Alerta de sesión expirada -->
              <div *ngIf="isExpiredNotice" class="alert alert-warning alert-dismissible fade show text-start mb-4" role="alert">
                <div class="d-flex align-items-center mb-1">
                  <i class="bi bi-clock-history fs-5 me-2"></i>
                  <strong>Sesión expirada</strong>
                </div>
                <div class="small">
                  Tu sesión previa ha expirado o el servidor se actualizó. Por favor, inicia sesión nuevamente para continuar.
                </div>
                <button type="button" class="btn-close" (click)="isExpiredNotice = false"></button>
              </div>

              <!-- Alerta de error -->
              <div *ngIf="errorMessage" class="alert alert-danger alert-dismissible fade show text-start mb-4" role="alert">
                <div class="d-flex align-items-center mb-1">
                  <i class="bi bi-exclamation-triangle-fill fs-5 me-2"></i>
                  <strong>Error de autenticación</strong>
                </div>
                <div class="small">{{ errorMessage }}</div>
                <button type="button" class="btn-close" (click)="errorMessage = ''"></button>
              </div>

              <div class="mb-4">
                <div class="badge bg-primary-subtle text-primary border border-primary-subtle px-3 py-2 rounded-pill mb-3">
                  <i class="bi bi-shield-lock-fill me-1"></i> Identidad Corporativa IDaaS
                </div>
                <h4 class="fw-bold mb-2">Iniciar Sesión</h4>
                <p class="text-muted small px-3">
                  Ingresa con tu cuenta institucional Duoc UC a través de <strong>Microsoft Entra ID (Azure AD)</strong> para acceder a los trámites y servicios municipales.
                </p>
              </div>

              <!-- Botón Principal Microsoft Azure AD -->
              <div class="d-grid gap-2 col-10 mx-auto mb-4">
                <button
                  class="btn btn-dark btn-lg py-3 d-inline-flex align-items-center justify-content-center gap-3 shadow"
                  (click)="loginWithMicrosoft()"
                  [disabled]="loading"
                >
                  <span *ngIf="loading" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                  <i *ngIf="!loading" class="bi bi-microsoft text-primary fs-5"></i>
                  <span class="fw-semibold">{{ loading ? 'Redirigiendo a Microsoft...' : 'Iniciar sesión con Microsoft' }}</span>
                </button>
              </div>

              <div class="text-muted small">
                <i class="bi bi-info-circle me-1"></i> Serás redirigido a la pantalla de inicio de sesión institucional de Microsoft.
              </div>
            </div>

            <div class="card-footer bg-light py-3 px-4 text-center text-muted small">
              <i class="bi bi-shield-check text-success me-1"></i>
              Flujo Seguro: MSAL &bull; AWS API Gateway &bull; Spring Security JWT Resource Server
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .btn-dark {
      background-color: #1e293b;
      border-color: #0f172a;
      transition: all 0.2s ease-in-out;
    }
    .btn-dark:hover:not(:disabled) {
      background-color: #0f172a;
      transform: translateY(-2px);
      box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15) !important;
    }
  `]
})
export class LoginComponent implements OnInit {
  authService = inject(AuthService);
  router = inject(Router);
  route = inject(ActivatedRoute);

  loading = false;
  errorMessage = '';
  isExpiredNotice = false;

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['expired'] === 'true') {
        this.isExpiredNotice = true;
      }
    });

    if (this.authService.isAuthenticated()) {
      const user = this.authService.getCurrentUser();
      if (user && !this.authService.isTokenExpired(user.token)) {
        this.router.navigate(['/dashboard']);
      }
    }
  }

  async loginWithMicrosoft(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';
    try {
      await this.authService.loginWithMicrosoft();
    } catch (err: any) {
      console.error('Error durante autenticación:', err);
      this.errorMessage = err?.message || err?.errorMessage || (typeof err === 'string' ? err : JSON.stringify(err));
      this.loading = false;
    }
  }
}
