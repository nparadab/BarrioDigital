import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService, UserProfile } from '../../auth/auth.service';
import { RequestItem, RequestStatus, AuditLog } from '../../core/models/request.model';
import { ProcedureType } from '../../core/models/procedure.model';

@Component({
  selector: 'app-requests',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container py-4">
      <!-- Encabezado -->
      <div class="d-flex flex-wrap justify-content-between align-items-center mb-4 pb-3 border-bottom">
        <div>
          <h2 class="fw-bold text-dark mb-1">
            <i class="bi bi-card-checklist text-primary me-2"></i>Gestión de Trámites Comunales
          </h2>
          <p class="text-muted mb-0">
            Ingreso, avance de estados y trazabilidad en tiempo real.
          </p>
        </div>
        <div class="mt-2 mt-md-0 d-flex gap-2">
          <button
            *ngIf="canCreateRequest()"
            class="btn btn-primary"
            (click)="openCreateModal()"
          >
            <i class="bi bi-plus-circle me-1"></i> Ingresar Trámite
          </button>
        </div>
      </div>

      <!-- Alertas informativas -->
      <div *ngIf="alertMessage" class="alert alert-dismissible fade show" [ngClass]="alertClass" role="alert">
        <i class="bi me-2" [ngClass]="alertIcon"></i>{{ alertMessage }}
        <button type="button" class="btn-close" (click)="closeAlert()"></button>
      </div>

      <!-- Barra de Filtros -->
      <div class="card shadow-sm border-0 mb-4 p-3 bg-light">
        <div class="row g-3 align-items-center">
          <div class="col-md-4">
            <label class="form-label small fw-bold text-muted">Filtrar por Estado:</label>
            <select class="form-select" [(ngModel)]="filterStatus" (change)="applyFilters()">
              <option value="">Todos los estados</option>
              <option value="INGRESADO">INGRESADO</option>
              <option value="ADMITIDO">ADMITIDO</option>
              <option value="EN_GESTION">EN_GESTIÓN</option>
              <option value="EN_TERRENO">EN_TERRENO</option>
              <option value="RESUELTO">RESUELTO</option>
              <option value="RECHAZADO">RECHAZADO</option>
            </select>
          </div>
          <div class="col-md-5">
            <label class="form-label small fw-bold text-muted">Buscar por Código, Vecino o DNI:</label>
            <div class="input-group">
              <span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
              <input
                type="text"
                class="form-control"
                placeholder="Ej: BD-2025-0001, Juan..."
                [(ngModel)]="searchQuery"
                (input)="applyFilters()"
              />
            </div>
          </div>
          <div class="col-md-3 text-end d-flex align-items-end">
            <button class="btn btn-outline-secondary w-100" (click)="resetFilters()">
              <i class="bi bi-arrow-clockwise me-1"></i> Limpiar Filtros
            </button>
          </div>
        </div>
      </div>

      <!-- Tabla de Solicitudes -->
      <div class="card shadow-sm border-0">
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
              <thead class="table-light text-uppercase small text-muted">
                <tr>
                  <th>N° Seguimiento</th>
                  <th>Trámite</th>
                  <th>Vecino / Contacto</th>
                  <th>Dirección</th>
                  <th>Estado</th>
                  <th>Fecha</th>
                  <th class="text-center">Acciones</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let req of filteredRequests">
                  <td class="fw-bold text-primary">{{ req.trackingNumber }}</td>
                  <td>
                    <div class="fw-semibold">{{ req.procedureName }}</div>
                    <div class="text-muted small text-truncate" style="max-width: 200px;">
                      {{ req.description }}
                    </div>
                  </td>
                  <td>
                    <div>{{ req.citizenName }}</div>
                    <div class="small text-muted">{{ req.citizenDni }}</div>
                  </td>
                  <td class="small">{{ req.address }}</td>
                  <td>
                    <span class="badge" [ngClass]="getStatusBadge(req.status)">
                      {{ req.status }}
                    </span>
                    <div *ngIf="req.assignedCrew" class="small text-muted mt-1">
                      <i class="bi bi-truck me-1"></i>{{ req.assignedCrew }}
                    </div>
                  </td>
                  <td class="small text-muted">
                    {{ req.createdAt | date:'short' }}
                  </td>
                  <td class="text-center">
                    <div class="btn-group btn-group-sm">
                      <!-- Botón cambiar estado (Funcionario / Admin) -->
                      <button
                        *ngIf="canChangeStatus()"
                        class="btn btn-outline-primary"
                        (click)="openStatusModal(req)"
                        title="Cambiar Estado"
                      >
                        <i class="bi bi-arrow-right-circle"></i> Estado
                      </button>
                      <!-- Botón Ver Historial -->
                      <button
                        class="btn btn-outline-secondary"
                        (click)="openTimelineModal(req)"
                        title="Ver Timeline"
                      >
                        <i class="bi bi-clock-history"></i>
                      </button>
                    </div>
                  </td>
                </tr>
                <tr *ngIf="filteredRequests.length === 0">
                  <td colspan="7" class="text-center py-4 text-muted">
                    No se encontraron solicitudes con los criterios de búsqueda aplicados.
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- MODAL 1: Ingresar Trámite -->
      <div class="modal fade show d-block" *ngIf="showCreateModal" style="background-color: rgba(0,0,0,0.5);">
        <div class="modal-dialog modal-lg">
          <div class="modal-content rounded-4 shadow">
            <div class="modal-header bg-primary text-white">
              <h5 class="modal-title fw-bold">
                <i class="bi bi-pencil-square me-2"></i>Ingresar Solicitud Comunal
              </h5>
              <button type="button" class="btn-close btn-close-white" (click)="closeCreateModal()"></button>
            </div>
            <div class="modal-body p-4">
              <form (ngSubmit)="submitCreateRequest()">
                <div class="row g-3">
                  <div class="col-md-6">
                    <label class="form-label fw-semibold">Tipo de Trámite *</label>
                    <select class="form-select" [(ngModel)]="newRequest.procedureTypeId" name="procedureTypeId" (change)="onProcedureChange()" required>
                      <option [ngValue]="null" disabled selected>Seleccione un trámite...</option>
                      <option *ngFor="let p of procedures" [ngValue]="p.id">
                        {{ p.name }} (Cupos hoy: {{ p.availableQuota }}/{{ p.dailyQuota }})
                      </option>
                    </select>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label fw-semibold">RUT / DNI del Vecino *</label>
                    <input type="text" class="form-control" [(ngModel)]="newRequest.citizenDni" name="citizenDni" placeholder="Ej: 18.234.567-8" maxlength="20" required />
                  </div>
                  <div class="col-md-6">
                    <label class="form-label fw-semibold">Nombre Completo *</label>
                    <input type="text" class="form-control" [(ngModel)]="newRequest.citizenName" name="citizenName" placeholder="Nombre del solicitante" required />
                  </div>
                  <div class="col-md-6">
                    <label class="form-label fw-semibold">Correo Electrónico</label>
                    <input type="email" class="form-control" [(ngModel)]="newRequest.citizenEmail" name="citizenEmail" placeholder="vecino@ejemplo.cl" />
                  </div>
                  <div class="col-md-6">
                    <label class="form-label fw-semibold">Teléfono de Contacto</label>
                    <input type="text" class="form-control" [(ngModel)]="newRequest.citizenPhone" name="citizenPhone" placeholder="+56 9 1234 5678" />
                  </div>
                  <div class="col-md-6">
                    <label class="form-label fw-semibold">Dirección de la Solicitud *</label>
                    <input type="text" class="form-control" [(ngModel)]="newRequest.address" name="address" placeholder="Calle, número, villa o junta vecinal" required />
                  </div>
                  <div class="col-12">
                    <label class="form-label fw-semibold">Descripción del Problema o Solicitud *</label>
                    <textarea class="form-control" rows="3" [(ngModel)]="newRequest.description" name="description" placeholder="Describa en detalle la situación..." required></textarea>
                  </div>
                </div>

                <div class="mt-4 text-end">
                  <button type="button" class="btn btn-secondary me-2" (click)="closeCreateModal()">Cancelar</button>
                  <button type="submit" class="btn btn-primary px-4" [disabled]="!isCreateFormValid()">
                    <i class="bi bi-send me-1"></i> Enviar Trámite
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>

      <!-- MODAL 2: Cambiar Estado (con regla de negocio EN_TERRENO) -->
      <div class="modal fade show d-block" *ngIf="showStatusModal && selectedRequest" style="background-color: rgba(0,0,0,0.5);">
        <div class="modal-dialog">
          <div class="modal-content rounded-4 shadow">
            <div class="modal-header bg-dark text-white">
              <h5 class="modal-title fw-bold">
                <i class="bi bi-arrow-repeat me-2"></i>Cambio de Estado - {{ selectedRequest.trackingNumber }}
              </h5>
              <button type="button" class="btn-close btn-close-white" (click)="closeStatusModal()"></button>
            </div>
            <div class="modal-body p-4">
              <div class="mb-3">
                <span class="text-muted small">Trámite:</span>
                <div class="fw-bold">{{ selectedRequest.procedureName }}</div>
                <div class="small text-muted">Vecino: {{ selectedRequest.citizenName }}</div>
              </div>

              <div class="mb-3">
                <label class="form-label fw-semibold">Estado Actual:</label>
                <div>
                  <span class="badge" [ngClass]="getStatusBadge(selectedRequest.status)">
                    {{ selectedRequest.status }}
                  </span>
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label fw-semibold">Nuevo Estado *</label>
                <select class="form-select" [(ngModel)]="newStatus" name="newStatus">
                  <option value="INGRESADO" [disabled]="selectedRequest.status !== 'INGRESADO'">INGRESADO</option>
                  <option value="ADMITIDO">ADMITIDO (Disminuye cupo diario)</option>
                  <option value="EN_GESTION">EN_GESTIÓN</option>
                  <option value="EN_TERRENO">EN_TERRENO (Cuadrilla operativa)</option>
                  <option value="RESUELTO">RESUELTO</option>
                  <option value="RECHAZADO">RECHAZADO</option>
                </select>
              </div>

              <!-- ALERTA DE REGLA DE NEGOCIO CLAVE: No se puede pasar a EN_TERRENO sin ADMITIR -->
              <div *ngIf="selectedRequest.status === 'INGRESADO' && newStatus === 'EN_TERRENO'" class="alert alert-danger py-2 small">
                <i class="bi bi-shield-x me-1"></i>
                <strong>Regla de negocio:</strong> No se puede pasar a <code>EN_TERRENO</code> sin antes haber sido <code>ADMITIDO</code>. Por favor admite la solicitud primero.
              </div>

              <!-- Cuadrilla (solo si EN_TERRENO) -->
              <div class="mb-3" *ngIf="newStatus === 'EN_TERRENO'">
                <label class="form-label fw-semibold">Asignar Cuadrilla *</label>
                <input type="text" class="form-control" [(ngModel)]="assignedCrew" placeholder="Ej: Cuadrilla Alumbrado N° 2" />
              </div>

              <!-- Notas de resolución -->
              <div class="mb-3" *ngIf="newStatus === 'RESUELTO' || newStatus === 'RECHAZADO'">
                <label class="form-label fw-semibold">Notas de Resolución / Cierre *</label>
                <textarea class="form-control" rows="2" [(ngModel)]="resolutionNotes" placeholder="Indicar resolución técnica o motivo de rechazo"></textarea>
              </div>

              <div class="mb-3">
                <label class="form-label fw-semibold">Comentarios de Auditoría</label>
                <input type="text" class="form-control" [(ngModel)]="statusComment" placeholder="Motivo del cambio de estado..." />
              </div>

              <div class="mt-4 text-end">
                <button type="button" class="btn btn-secondary me-2" (click)="closeStatusModal()">Cancelar</button>
                <button
                  type="button"
                  class="btn btn-primary px-4"
                  (click)="submitStatusUpdate()"
                  [disabled]="selectedRequest.status === 'INGRESADO' && newStatus === 'EN_TERRENO'"
                >
                  Confirmar Cambio
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- MODAL 3: Timeline / Historial de Auditoría -->
      <div class="modal fade show d-block" *ngIf="showTimelineModal && selectedRequest" style="background-color: rgba(0,0,0,0.5);">
        <div class="modal-dialog modal-lg">
          <div class="modal-content rounded-4 shadow">
            <div class="modal-header bg-secondary text-white">
              <h5 class="modal-title fw-bold">
                <i class="bi bi-clock-history me-2"></i>Trazabilidad del Trámite: {{ selectedRequest.trackingNumber }}
              </h5>
              <button type="button" class="btn-close btn-close-white" (click)="closeTimelineModal()"></button>
            </div>
            <div class="modal-body p-4">
              <div *ngIf="timelineLogs.length === 0" class="text-center py-4 text-muted">
                Cargando historial de eventos...
              </div>

              <div class="timeline" *ngIf="timelineLogs.length > 0">
                <div class="border-start border-3 border-primary ps-3 mb-4" *ngFor="let log of timelineLogs">
                  <div class="d-flex justify-content-between align-items-center mb-1">
                    <span class="badge bg-primary">{{ log.newStatus }}</span>
                    <span class="text-muted small">{{ log.timestamp | date:'medium' }}</span>
                  </div>
                  <div class="fw-semibold">{{ log.action }}</div>
                  <div class="small text-dark">
                    Realizado por: <strong>{{ log.performedBy }}</strong> ({{ log.userRole }})
                  </div>
                  <div class="small text-muted mt-1" *ngIf="log.comments">
                    "{{ log.comments }}"
                  </div>
                </div>
              </div>

              <div class="text-end mt-3">
                <button type="button" class="btn btn-secondary" (click)="closeTimelineModal()">Cerrar</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class RequestsComponent implements OnInit {
  private apiService = inject(ApiService);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);

  currentUser: UserProfile | null = null;
  requests: RequestItem[] = [];
  filteredRequests: RequestItem[] = [];
  procedures: ProcedureType[] = [];

  filterStatus = '';
  searchQuery = '';

  alertMessage = '';
  alertClass = 'alert-success';
  alertIcon = 'bi-check-circle-fill';

  // Modales
  showCreateModal = false;
  showStatusModal = false;
  showTimelineModal = false;

  selectedRequest: RequestItem | null = null;
  timelineLogs: AuditLog[] = [];

  // Datos para nuevo trámite
  newRequest: Partial<RequestItem> = {
    procedureTypeId: undefined,
    citizenDni: '',
    citizenName: '',
    citizenEmail: '',
    citizenPhone: '',
    address: '',
    description: ''
  };

  // Datos para cambio de estado
  newStatus: RequestStatus = 'ADMITIDO';
  assignedCrew = '';
  resolutionNotes = '';
  statusComment = '';

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadRequests();
    this.loadProcedures();
  }

  loadRequests(): void {
    this.apiService.getRequests().subscribe({
      next: (data) => {
        this.requests = data;
        this.applyFilters();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.showAlert('Error al cargar trámites desde el backend.', 'alert-danger');
        this.cdr.detectChanges();
      }
    });
  }

  loadProcedures(): void {
    this.apiService.getProcedures().subscribe({
      next: (data) => {
        this.procedures = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando catálogo:', err);
        this.cdr.detectChanges();
      }
    });
  }

  applyFilters(): void {
    let result = [...this.requests];

    if (this.filterStatus) {
      result = result.filter((r) => r.status === this.filterStatus);
    }

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(
        (r) =>
          r.trackingNumber?.toLowerCase().includes(q) ||
          r.citizenName.toLowerCase().includes(q) ||
          r.citizenDni.toLowerCase().includes(q) ||
          r.procedureName.toLowerCase().includes(q)
      );
    }

    this.filteredRequests = result;
    this.cdr.detectChanges();
  }

  resetFilters(): void {
    this.filterStatus = '';
    this.searchQuery = '';
    this.filteredRequests = [...this.requests];
    this.cdr.detectChanges();
  }

  canCreateRequest(): boolean {
    return this.authService.hasAnyRole(['Admin', 'Funcionario', 'Vecino']);
  }

  canChangeStatus(): boolean {
    return this.authService.hasAnyRole(['Admin', 'Funcionario']);
  }

  openCreateModal(): void {
    const user = this.currentUser;
    this.newRequest = {
      procedureTypeId: this.procedures.length ? this.procedures[0].id : undefined,
      procedureName: this.procedures.length ? this.procedures[0].name : '',
      citizenDni: user?.role === 'Vecino' ? '18.432.110-5' : '',
      citizenName: user?.role === 'Vecino' ? user.name : '',
      citizenEmail: user?.email || '',
      citizenPhone: '+56911223344',
      address: '',
      description: ''
    };
    this.showCreateModal = true;
    this.cdr.detectChanges();
  }

  onProcedureChange(): void {
    const proc = this.procedures.find((p) => p.id === this.newRequest.procedureTypeId);
    if (proc) {
      this.newRequest.procedureName = proc.name;
    }
  }

  isCreateFormValid(): boolean {
    return !!(
      this.newRequest.procedureTypeId &&
      this.newRequest.citizenDni &&
      this.newRequest.citizenName &&
      this.newRequest.address &&
      this.newRequest.description
    );
  }

  submitCreateRequest(): void {
    if (!this.isCreateFormValid()) return;

    this.apiService.createRequest(this.newRequest).subscribe({
      next: (created) => {
        this.showCreateModal = false;
        this.showAlert(
          `¡Trámite creado exitosamente! Número de seguimiento asignado: ${created.trackingNumber}`,
          'alert-success'
        );
        this.loadRequests();
        this.loadProcedures(); // Actualizar cupos
      },
      error: (err) => {
        this.showAlert('Error al registrar la solicitud: ' + (err.error?.message || err.message), 'alert-danger');
      }
    });
  }

  openStatusModal(req: RequestItem): void {
    this.selectedRequest = req;
    this.newStatus = req.status;
    this.assignedCrew = req.assignedCrew || '';
    this.resolutionNotes = req.resolutionNotes || '';
    this.statusComment = '';
    this.showStatusModal = true;
    this.cdr.detectChanges();
  }

  submitStatusUpdate(): void {
    if (!this.selectedRequest?.id) return;

    const payload = {
      status: this.newStatus,
      assignedCrew: this.assignedCrew,
      resolutionNotes: this.resolutionNotes,
      comments: this.statusComment
    };

    this.apiService.updateRequestStatus(this.selectedRequest.id, payload).subscribe({
      next: (updated) => {
        this.showStatusModal = false;
        this.showAlert(
          `Trámite ${updated.trackingNumber} actualizado al estado ${updated.status}`,
          'alert-success'
        );
        this.loadRequests();
        this.loadProcedures();
      },
      error: (err) => {
        const errorMsg = err.error?.message || err.message;
        this.showAlert('Error en transición de estado: ' + errorMsg, 'alert-danger');
      }
    });
  }

  openTimelineModal(req: RequestItem): void {
    this.selectedRequest = req;
    this.timelineLogs = [];
    this.showTimelineModal = true;
    this.cdr.detectChanges();

    if (req.id) {
      this.apiService.getRequestTimeline(req.id).subscribe({
        next: (logs) => {
          this.timelineLogs = logs;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando timeline:', err);
          this.cdr.detectChanges();
        }
      });
    }
  }

  showAlert(message: string, cssClass: string): void {
    this.alertMessage = message;
    this.alertClass = cssClass;
    this.alertIcon = cssClass.includes('success') ? 'bi-check-circle-fill' : 'bi-exclamation-triangle-fill';
    this.cdr.detectChanges();
  }

  closeAlert(): void {
    this.alertMessage = '';
    this.cdr.detectChanges();
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.cdr.detectChanges();
  }

  closeStatusModal(): void {
    this.showStatusModal = false;
    this.selectedRequest = null;
    this.cdr.detectChanges();
  }

  closeTimelineModal(): void {
    this.showTimelineModal = false;
    this.selectedRequest = null;
    this.timelineLogs = [];
    this.cdr.detectChanges();
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
}
