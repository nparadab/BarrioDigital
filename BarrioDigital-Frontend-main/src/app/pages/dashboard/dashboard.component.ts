import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService, UserProfile } from '../../auth/auth.service';
import { ApiService } from '../../core/services/api.service';
import { RequestItem, KpisResponse, AuditLog } from '../../core/models/request.model';
import { ProcedureType } from '../../core/models/procedure.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="container py-4" *ngIf="currentUser">
      <!-- Encabezado de bienvenida -->
      <div class="d-flex flex-wrap justify-content-between align-items-center mb-4 pb-3 border-bottom">
        <div>
          <h2 class="fw-bold text-dark mb-1">
            Panel de Operaciones &bull; {{ currentUser.role }}
          </h2>
          <p class="text-muted mb-0">
            Bienvenido, <span class="fw-semibold text-primary">{{ currentUser.name }}</span> ({{ currentUser.email }})
          </p>
        </div>
        <div class="mt-2 mt-md-0 d-flex gap-2">
          <a routerLink="/requests" class="btn btn-primary">
            <i class="bi bi-card-checklist me-1"></i> Ver Trámites
          </a>
          <a *ngIf="currentUser.role === 'Admin'" routerLink="/reports" class="btn btn-outline-secondary">
            <i class="bi bi-bar-chart me-1"></i> Ver Métricas
          </a>
          <a *ngIf="currentUser.role === 'Auditor'" routerLink="/audit" class="btn btn-warning text-dark fw-semibold">
            <i class="bi bi-shield-check me-1"></i> Ver Auditoría
          </a>
        </div>
      </div>

      <!-- Tarjetas de Resumen Rápido -->
      <div class="row g-3 mb-4">
        <div class="col-sm-6 col-lg-3">
          <div class="card shadow-sm border-0 border-start border-primary border-4 p-3 bg-white">
            <div class="text-muted small fw-semibold">TRÁMITES TOTALES</div>
            <div class="fs-3 fw-bold text-dark">{{ requests.length }}</div>
            <div class="text-primary small"><i class="bi bi-buildings me-1"></i>Red comunal</div>
          </div>
        </div>

        <div class="col-sm-6 col-lg-3">
          <div class="card shadow-sm border-0 border-start border-warning border-4 p-3 bg-white">
            <div class="text-muted small fw-semibold">PENDIENTES ADMISIÓN</div>
            <div class="fs-3 fw-bold text-warning">{{ admissionQueue.length }}</div>
            <div class="text-muted small">Por admitir</div>
          </div>
        </div>

        <div class="col-sm-6 col-lg-3">
          <div class="card shadow-sm border-0 border-start border-info border-4 p-3 bg-white">
            <div class="text-muted small fw-semibold">EN GESTIÓN / TERRENO</div>
            <div class="fs-3 fw-bold text-info">{{ terrainQueue.length }}</div>
            <div class="text-muted small">Cuadrillas operativas</div>
          </div>
        </div>

        <div class="col-sm-6 col-lg-3">
          <div class="card shadow-sm border-0 border-start border-success border-4 p-3 bg-white">
            <div class="text-muted small fw-semibold">RESUELTOS</div>
            <div class="fs-3 fw-bold text-success">{{ resolvedQueue.length }}</div>
            <div class="text-success small"><i class="bi bi-check-circle me-1"></i>Completados</div>
          </div>
        </div>
      </div>

      <!-- VISTA CONTEXTUAL SEGÚN EL ROL -->

      <!-- 1. VISTA FUNCIONARIO / ADMIN / AUDITOR: Bandeja de Admisión, Cuadrillas y Trámites Resueltos -->
      <div *ngIf="currentUser.role === 'Funcionario' || currentUser.role === 'Admin' || currentUser.role === 'Auditor'" class="mb-4">
        <div class="row g-4 mb-4">
          <div class="col-lg-6">
            <div class="card shadow-sm border-0 h-100">
              <div class="card-header bg-white fw-bold py-3 d-flex justify-content-between align-items-center">
                <span><i class="bi bi-hourglass-split text-warning me-2"></i>Bandeja de Admisión (Pendientes)</span>
                <span class="badge bg-warning text-dark">{{ admissionQueue.length }} nuevos</span>
              </div>
              <div class="card-body p-0">
                <div class="table-responsive">
                  <table class="table table-hover align-middle mb-0">
                    <thead class="table-light small">
                      <tr>
                        <th>Código</th>
                        <th>Trámite</th>
                        <th>Vecino</th>
                        <th>Acción</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr *ngFor="let req of admissionQueue">
                        <td class="fw-bold text-primary">{{ req.trackingNumber }}</td>
                        <td>{{ req.procedureName }}</td>
                        <td>{{ req.citizenName }}</td>
                        <td>
                          <a [routerLink]="['/requests']" class="btn btn-sm" [ngClass]="currentUser.role === 'Auditor' ? 'btn-outline-secondary' : 'btn-outline-primary'">
                            <i class="bi" [ngClass]="currentUser.role === 'Auditor' ? 'bi-eye me-1' : 'bi-arrow-right-short me-1'"></i>
                            {{ currentUser.role === 'Auditor' ? 'Inspeccionar' : 'Gestionar' }}
                          </a>
                        </td>
                      </tr>
                      <tr *ngIf="admissionQueue.length === 0">
                        <td colspan="4" class="text-center text-muted py-3">
                          No hay solicitudes pendientes de admisión en este momento.
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>

          <div class="col-lg-6">
            <div class="card shadow-sm border-0 h-100">
              <div class="card-header bg-white fw-bold py-3 d-flex justify-content-between align-items-center">
                <span><i class="bi bi-truck text-primary me-2"></i>Cuadrillas en Terreno Activas</span>
                <span class="badge bg-primary">{{ terrainQueue.length }} en ruta</span>
              </div>
              <div class="card-body p-0">
                <div class="table-responsive">
                  <table class="table table-hover align-middle mb-0">
                    <thead class="table-light small">
                      <tr>
                        <th>Código</th>
                        <th>Cuadrilla</th>
                        <th>Dirección</th>
                        <th>Estado</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr *ngFor="let req of terrainQueue">
                        <td class="fw-bold text-primary">{{ req.trackingNumber }}</td>
                        <td>{{ req.assignedCrew || 'Sin asignar' }}</td>
                        <td class="small text-truncate" style="max-width: 180px;">{{ req.address }}</td>
                        <td><span class="badge" [ngClass]="getStatusBadge(req.status)">{{ req.status }}</span></td>
                      </tr>
                      <tr *ngIf="terrainQueue.length === 0">
                        <td colspan="4" class="text-center text-muted py-3">
                          No hay cuadrillas asignadas en terreno actualmente.
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Tabla de Trámites Resueltos / Terminados -->
        <div class="card shadow-sm border-0 mb-4">
          <div class="card-header bg-white fw-bold py-3 d-flex justify-content-between align-items-center">
            <span><i class="bi bi-check-circle-fill text-success me-2"></i>Trámites Resueltos / Finalizados</span>
            <span class="badge bg-success">{{ resolvedQueue.length }} completados</span>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-hover align-middle mb-0">
                <thead class="table-light small">
                  <tr>
                    <th>Código</th>
                    <th>Trámite</th>
                    <th>Vecino</th>
                    <th>Cuadrilla / Resolución</th>
                    <th>Estado</th>
                    <th>Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let req of resolvedQueue">
                    <td class="fw-bold text-primary">{{ req.trackingNumber }}</td>
                    <td>{{ req.procedureName }}</td>
                    <td>{{ req.citizenName }}</td>
                    <td class="small text-muted">
                      <span *ngIf="req.assignedCrew"><i class="bi bi-truck me-1"></i>{{ req.assignedCrew }}</span>
                      <span *ngIf="req.resolutionNotes" class="d-block text-truncate" style="max-width: 250px;">{{ req.resolutionNotes }}</span>
                    </td>
                    <td>
                      <span class="badge bg-success">RESUELTO</span>
                    </td>
                    <td class="small text-muted">{{ req.updatedAt || req.createdAt | date:'short' }}</td>
                  </tr>
                  <tr *ngIf="resolvedQueue.length === 0">
                    <td colspan="6" class="text-center text-muted py-3">
                      Aún no hay trámites con estado RESUELTO.
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Panel de Auditoría y Trazabilidad en Vivo (Visible para Auditor y Admin) -->
        <div *ngIf="currentUser.role === 'Auditor' || currentUser.role === 'Admin'" class="card shadow-sm border-0 mb-4">
          <div class="card-header bg-white fw-bold py-3 d-flex justify-content-between align-items-center">
            <span><i class="bi bi-shield-check text-warning me-2"></i>Eventos de Auditoría Recientes (Trazabilidad en Vivo)</span>
            <a routerLink="/audit" class="btn btn-sm btn-outline-warning text-dark">
              <i class="bi bi-arrow-right-circle me-1"></i> Ver Todo el Registro
            </a>
          </div>
          <div class="card-body p-0">
            <div class="table-responsive">
              <table class="table table-hover align-middle mb-0">
                <thead class="table-light small">
                  <tr>
                    <th>Fecha / Hora</th>
                    <th>N° Seguimiento</th>
                    <th>Acción</th>
                    <th>Transición de Estado</th>
                    <th>Operador</th>
                    <th>Rol</th>
                    <th>Comentarios</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let log of auditLogs">
                    <td class="small text-muted text-nowrap">{{ log.timestamp | date:'short' }}</td>
                    <td class="fw-bold text-primary">{{ log.trackingNumber }}</td>
                    <td><span class="badge bg-secondary small">{{ log.action }}</span></td>
                    <td>
                      <span *ngIf="log.previousStatus" class="small text-muted">{{ log.previousStatus }} &rarr; </span>
                      <span class="badge bg-primary small">{{ log.newStatus }}</span>
                    </td>
                    <td class="fw-semibold">{{ log.performedBy }}</td>
                    <td><span class="badge bg-light text-dark border">{{ log.userRole }}</span></td>
                    <td class="small text-muted text-truncate" style="max-width: 260px;">{{ log.comments || '-' }}</td>
                  </tr>
                  <tr *ngIf="auditLogs.length === 0">
                    <td colspan="7" class="text-center text-muted py-3">
                      No hay eventos de auditoría registrados aún.
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- 2. VISTA VECINO: Mis Solicitudes y Seguimiento -->
      <div *ngIf="currentUser.role === 'Vecino'" class="row g-4 mb-4">
        <div class="col-lg-8">
          <div class="card shadow-sm border-0">
            <div class="card-header bg-white fw-bold py-3 d-flex justify-content-between align-items-center">
              <span><i class="bi bi-folder2-open text-primary me-2"></i>Mis Solicitudes Comunales</span>
              <a routerLink="/requests" class="btn btn-sm btn-primary">
                <i class="bi bi-plus-lg me-1"></i> Ingresar Trámite
              </a>
            </div>
            <div class="card-body p-0">
              <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                  <thead class="table-light small">
                    <tr>
                      <th>N° Seguimiento</th>
                      <th>Trámite</th>
                      <th>Fecha</th>
                      <th>Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let req of requests">
                      <td class="fw-bold text-primary">{{ req.trackingNumber }}</td>
                      <td>{{ req.procedureName }}</td>
                      <td class="small text-muted">{{ req.createdAt | date:'short' }}</td>
                      <td>
                        <span class="badge" [ngClass]="getStatusBadge(req.status)">
                          {{ req.status }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <div class="col-lg-4">
          <div class="card shadow-sm border-0 p-3 bg-light">
            <h6 class="fw-bold mb-3"><i class="bi bi-info-circle text-primary me-2"></i>¿Cómo funciona tu trámite?</h6>
            <ol class="small text-muted ps-3 mb-3">
              <li class="mb-2"><strong>Ingresado:</strong> Recibes tu número único de seguimiento.</li>
              <li class="mb-2"><strong>Admitido:</strong> Un funcionario valida los antecedentes y descuenta el cupo comunal.</li>
              <li class="mb-2"><strong>En Terreno:</strong> La cuadrilla de operaciones visita el lugar.</li>
              <li><strong>Resuelto:</strong> Se concluye la labor y se emite constancia.</li>
            </ol>
            <div class="alert alert-warning small mb-0">
              <i class="bi bi-shield-lock me-1"></i> Recuerda guardar tu código de seguimiento para consultas.
            </div>
          </div>
        </div>
      </div>

      <!-- 3. VISTA ADMIN / AUDITOR: Resumen de Catálogo y Auditoría -->
      <div *ngIf="currentUser.role === 'Admin' || currentUser.role === 'Auditor'" class="row g-4 mb-4">
        <div class="col-lg-7">
          <div class="card shadow-sm border-0">
            <div class="card-header bg-white fw-bold py-3 d-flex justify-content-between align-items-center">
              <span><i class="bi bi-list-check text-primary me-2"></i>Últimos Trámites en el Sistema</span>
              <a routerLink="/requests" class="btn btn-sm btn-outline-primary">Ver Todos</a>
            </div>
            <div class="card-body p-0">
              <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                  <thead class="table-light small">
                    <tr>
                      <th>Código</th>
                      <th>Trámite</th>
                      <th>Vecino</th>
                      <th>Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let req of requests.slice(0, 5)">
                      <td class="fw-bold text-primary">{{ req.trackingNumber }}</td>
                      <td>{{ req.procedureName }}</td>
                      <td>{{ req.citizenName }}</td>
                      <td>
                        <span class="badge" [ngClass]="getStatusBadge(req.status)">
                          {{ req.status }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <div class="col-lg-5">
          <div class="card shadow-sm border-0">
            <div class="card-header bg-white fw-bold py-3">
              <i class="bi bi-pie-chart text-info me-2"></i>Distribución de Estados
            </div>
            <div class="card-body">
              <div *ngFor="let statusKey of statusKeys" class="mb-2">
                <div class="d-flex justify-content-between small mb-1">
                  <span>{{ statusKey }}</span>
                  <span class="fw-bold">{{ countByStatus(statusKey) }}</span>
                </div>
                <div class="progress" style="height: 6px;">
                  <div
                    class="progress-bar"
                    [ngClass]="getProgressBarClass(statusKey)"
                    [style.width.%]="getProgressPercent(statusKey)"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private apiService = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);

  currentUser: UserProfile | null = null;
  requests: RequestItem[] = [];
  kpis: KpisResponse | null = null;
  auditLogs: AuditLog[] = [];

  statusKeys = ['INGRESADO', 'ADMITIDO', 'EN_GESTION', 'EN_TERRENO', 'RESUELTO', 'RECHAZADO'];

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadData();
  }

  loadData(): void {
    this.apiService.getRequests().subscribe({
      next: (data) => {
        this.requests = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error cargando trámites:', err)
    });

    this.apiService.getKpis().subscribe({
      next: (data) => {
        this.kpis = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error cargando KPIs:', err)
    });

    if (this.currentUser?.role === 'Auditor' || this.currentUser?.role === 'Admin') {
      this.apiService.getAuditTimeline().subscribe({
        next: (logs) => {
          this.auditLogs = logs.slice(0, 6);
          this.cdr.detectChanges();
        },
        error: (err) => console.error('Error cargando auditoría en dashboard:', err)
      });
    }
  }

  get admissionQueue(): RequestItem[] {
    return this.requests.filter((r) => r.status === 'INGRESADO');
  }

  get terrainQueue(): RequestItem[] {
    return this.requests.filter((r) => r.status === 'EN_TERRENO' || r.status === 'EN_GESTION');
  }

  get resolvedQueue(): RequestItem[] {
    return this.requests.filter((r) => r.status === 'RESUELTO');
  }

  countActive(): number {
    return this.requests.filter((r) => r.status !== 'RESUELTO' && r.status !== 'RECHAZADO').length;
  }

  countResolved(): number {
    return this.requests.filter((r) => r.status === 'RESUELTO').length;
  }

  countByStatus(status: string): number {
    return this.requests.filter((r) => r.status === status).length;
  }

  getProgressPercent(status: string): number {
    if (!this.requests.length) return 0;
    return (this.countByStatus(status) / this.requests.length) * 100;
  }

  getStatusBadge(status: string): string {
    switch (status) {
      case 'INGRESADO':
        return 'bg-secondary text-white';
      case 'ADMITIDO':
        return 'bg-primary text-white';
      case 'EN_GESTION':
        return 'bg-warning text-dark';
      case 'EN_TERRENO':
        return 'bg-info text-dark';
      case 'RESUELTO':
        return 'bg-success text-white';
      case 'RECHAZADO':
        return 'bg-danger text-white';
      default:
        return 'bg-light text-dark';
    }
  }

  getProgressBarClass(status: string): string {
    switch (status) {
      case 'INGRESADO':
        return 'bg-secondary';
      case 'ADMITIDO':
        return 'bg-primary';
      case 'EN_GESTION':
        return 'bg-warning';
      case 'EN_TERRENO':
        return 'bg-info';
      case 'RESUELTO':
        return 'bg-success';
      case 'RECHAZADO':
        return 'bg-danger';
      default:
        return 'bg-dark';
    }
  }
}
