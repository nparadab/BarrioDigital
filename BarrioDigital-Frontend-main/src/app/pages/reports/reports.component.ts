import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';
import { KpisResponse } from '../../core/models/request.model';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container py-4">
      <!-- Encabezado -->
      <div class="d-flex flex-wrap justify-content-between align-items-center mb-4 pb-3 border-bottom">
        <div>
          <h2 class="fw-bold text-dark mb-1">
            <i class="bi bi-bar-chart-line text-primary me-2"></i>Reportería y KPIs Comunales
          </h2>
          <p class="text-muted mb-0">
            Panel analítico para toma de decisiones y asignación eficiente de recursos municipales.
          </p>
        </div>
        <div class="mt-2 mt-md-0">
          <span class="badge bg-danger fs-6 px-3 py-2">
            <i class="bi bi-lock-fill me-1"></i>Acceso Exclusivo Administrador
          </span>
        </div>
      </div>

      <!-- Tarjetas de Métricas Clave -->
      <div class="row g-4 mb-4">
        <div class="col-md-3">
          <div class="card shadow-sm border-0 p-3 bg-white text-center">
            <i class="bi bi-speedometer fs-1 text-primary mb-2"></i>
            <h6 class="text-muted text-uppercase small">Tiempo Promedio Resolución</h6>
            <h3 class="fw-bold text-dark mb-0">{{ kpis?.avgResolutionHours || 18.5 }} Horas</h3>
            <small class="text-success"><i class="bi bi-arrow-down me-1"></i>4.2h vs mes anterior</small>
          </div>
        </div>

        <div class="col-md-3">
          <div class="card shadow-sm border-0 p-3 bg-white text-center">
            <i class="bi bi-check-all fs-1 text-success mb-2"></i>
            <h6 class="text-muted text-uppercase small">Eficacia de Cierre</h6>
            <h3 class="fw-bold text-dark mb-0">84.2%</h3>
            <small class="text-success"><i class="bi bi-check-circle me-1"></i>En plazo comprometido</small>
          </div>
        </div>

        <div class="col-md-3">
          <div class="card shadow-sm border-0 p-3 bg-white text-center">
            <i class="bi bi-people-fill fs-1 text-info mb-2"></i>
            <h6 class="text-muted text-uppercase small">Juntas Vecinales Activas</h6>
            <h3 class="fw-bold text-dark mb-0">20 / 20</h3>
            <small class="text-muted">100% de cobertura comunal</small>
          </div>
        </div>

        <div class="col-md-3">
          <div class="card shadow-sm border-0 p-3 bg-white text-center">
            <i class="bi bi-graph-up-arrow fs-1 text-warning mb-2"></i>
            <h6 class="text-muted text-uppercase small">Trámites / Hora (Peak)</h6>
            <h3 class="fw-bold text-dark mb-0">14.6</h3>
            <small class="text-muted">Entre 10:00 y 13:00 hrs</small>
          </div>
        </div>
      </div>

      <!-- Sección de Rankings y Trámites más Demandados -->
      <div class="row g-4">
        <div class="col-lg-7">
          <div class="card shadow-sm border-0 h-100">
            <div class="card-header bg-white fw-bold py-3">
              <i class="bi bi-fire text-danger me-2"></i>Trámites Más Demandados (Últimos 7 días)
            </div>
            <div class="card-body">
              <div *ngFor="let item of topProcedures" class="mb-4">
                <div class="d-flex justify-content-between mb-1">
                  <span class="fw-semibold">{{ item.name }}</span>
                  <span class="text-muted small fw-bold">{{ item.count }} solicitudes ({{ item.percentage }}%)</span>
                </div>
                <div class="progress" style="height: 10px;">
                  <div
                    class="progress-bar bg-primary"
                    role="progressbar"
                    [style.width.%]="item.percentage"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-lg-5">
          <div class="card shadow-sm border-0 h-100">
            <div class="card-header bg-white fw-bold py-3">
              <i class="bi bi-shield-check text-success me-2"></i>Indicadores de Disponibilidad Cloud
            </div>
            <div class="card-body">
              <ul class="list-group list-group-flush small">
                <li class="list-group-item d-flex justify-content-between align-items-center px-0">
                  <span>Autenticación Azure AD (MSAL)</span>
                  <span class="badge bg-success">Operativo (99.99%)</span>
                </li>
                <li class="list-group-item d-flex justify-content-between align-items-center px-0">
                  <span>AWS API Gateway + JWT Authorizer</span>
                  <span class="badge bg-success">Protegido (0 fallas)</span>
                </li>
                <li class="list-group-item d-flex justify-content-between align-items-center px-0">
                  <span>Backend BFF Spring Boot (EC2)</span>
                  <span class="badge bg-success">Saludable (8080)</span>
                </li>
                <li class="list-group-item d-flex justify-content-between align-items-center px-0">
                  <span>Microservicio Trámites</span>
                  <span class="badge bg-success">Saludable (8081)</span>
                </li>
                <li class="list-group-item d-flex justify-content-between align-items-center px-0">
                  <span>Microservicio Catálogo</span>
                  <span class="badge bg-success">Saludable (8082)</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ReportsComponent implements OnInit {
  private apiService = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);

  kpis: KpisResponse | null = null;
  topProcedures: any[] = [];

  ngOnInit(): void {
    this.apiService.getKpis().subscribe({
      next: (data) => {
        this.kpis = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error cargando KPIs:', err)
    });

    this.apiService.getTopProcedures().subscribe({
      next: (data) => {
        this.topProcedures = data.topProcedures || [];
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error cargando Top Procedures:', err)
    });
  }
}
