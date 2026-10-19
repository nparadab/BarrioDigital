import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService, UserProfile } from '../../auth/auth.service';
import { ProcedureType } from '../../core/models/procedure.model';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container py-4">
      <!-- Encabezado -->
      <div class="d-flex flex-wrap justify-content-between align-items-center mb-4 pb-3 border-bottom">
        <div>
          <h2 class="fw-bold text-dark mb-1">
            <i class="bi bi-grid-3x3-gap text-primary me-2"></i>Catálogo y Cupos Diarios Comunales
          </h2>
          <p class="text-muted mb-0">
            Administración de tipos de solicitud comunal, requisitos y disponibilidad de cuadrillas.
          </p>
        </div>
        <div class="mt-2 mt-md-0" *ngIf="currentUser?.role === 'Admin'">
          <button class="btn btn-primary" (click)="openCreateModal()">
            <i class="bi bi-plus-circle me-1"></i> Nuevo Trámite
          </button>
        </div>
      </div>

      <!-- Alertas -->
      <div *ngIf="alertMessage" class="alert alert-dismissible fade show" [ngClass]="alertClass" role="alert">
        <i class="bi me-2" [ngClass]="alertIcon"></i>{{ alertMessage }}
        <button type="button" class="btn-close" (click)="closeAlert()"></button>
      </div>

      <!-- Tarjetas de Tipos de Trámite -->
      <div class="row g-4">
        <div class="col-md-6 col-lg-4" *ngFor="let proc of procedures">
          <div class="card h-100 shadow-sm border-0 rounded-3 overflow-hidden">
            <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
              <span class="badge bg-primary fs-7">{{ proc.code }}</span>
              <span class="badge" [ngClass]="proc.availableQuota > 0 ? 'bg-success' : 'bg-danger'">
                {{ proc.availableQuota > 0 ? 'Cupos Disponibles' : 'Sin Cupo Hoy' }}
              </span>
            </div>
            <div class="card-body">
              <h5 class="card-title fw-bold text-dark mb-2">{{ proc.name }}</h5>
              <p class="card-text text-muted small mb-3">{{ proc.description }}</p>

              <div class="mb-3">
                <span class="text-muted small fw-semibold d-block">Departamento Responsable:</span>
                <span class="text-dark small"><i class="bi bi-building me-1"></i>{{ proc.department || 'Municipio' }}</span>
              </div>

              <!-- Barra de Disponibilidad de Cupo -->
              <div class="mb-3">
                <div class="d-flex justify-content-between small mb-1">
                  <span class="fw-semibold text-muted">Cupo Diario Restante:</span>
                  <span class="fw-bold">{{ proc.availableQuota }} de {{ proc.dailyQuota }}</span>
                </div>
                <div class="progress" style="height: 8px;">
                  <div
                    class="progress-bar"
                    [ngClass]="getQuotaProgressClass(proc.availableQuota, proc.dailyQuota)"
                    [style.width.%]="(proc.availableQuota / proc.dailyQuota) * 100"
                  ></div>
                </div>
              </div>

              <div class="p-2 bg-light rounded small text-muted">
                <i class="bi bi-info-circle me-1 text-primary"></i>
                <strong>Requisitos:</strong> {{ proc.requirements || 'Sin requisitos especiales.' }}
              </div>
            </div>

            <div class="card-footer bg-white border-top py-2 d-flex justify-content-between align-items-center" *ngIf="currentUser?.role === 'Admin'">
              <span class="small text-muted">Configuración</span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-secondary" (click)="openEditModal(proc)" title="Editar Cupo">
                  <i class="bi bi-gear me-1"></i> Editar
                </button>
                <button class="btn btn-sm btn-outline-danger" (click)="deleteProcedure(proc)" title="Eliminar Trámite">
                  <i class="bi bi-trash me-1"></i> Eliminar
                </button>
              </div>
            </div>

          </div>
        </div>
      </div>

      <!-- Modal para Crear/Editar Trámite -->
      <div class="modal fade show d-block" *ngIf="showModal" style="background-color: rgba(0,0,0,0.5);">
        <div class="modal-dialog">
          <div class="modal-content rounded-4 shadow">
            <div class="modal-header bg-primary text-white">
              <h5 class="modal-title fw-bold">
                {{ isEditing ? 'Editar Cupos y Requisitos' : 'Registrar Nuevo Tipo de Trámite' }}
              </h5>
              <button type="button" class="btn-close btn-close-white" (click)="closeModal()"></button>
            </div>
            <div class="modal-body p-4">
              <form (ngSubmit)="saveProcedure()">
                <div class="mb-3" *ngIf="!isEditing">
                  <label class="form-label fw-semibold">
                    <i class="bi bi-tag-fill text-primary me-1"></i>Tipo / Categoría de Trámite *
                  </label>
                  <select class="form-select" [(ngModel)]="selectedCategory" (change)="onCategoryChange()" name="selectedCategory" required>
                    <option value="" disabled selected>-- Selecciona el tipo de servicio comunal --</option>
                    <option value="LUM">💡 Luminaria Pública (LUM)</option>
                    <option value="ESC">🚛 Retiro de Escombros / Voluminosos (ESC)</option>
                    <option value="PAV">🚧 Bacheo y Pavimentación (PAV)</option>
                    <option value="POD">🌳 Poda de Árboles en Vía Pública (POD)</option>
                    <option value="OTR">📋 Otro servicio comunal</option>
                  </select>
                </div>

                <div class="mb-3" *ngIf="!isEditing">
                  <label class="form-label fw-semibold">Código del Trámite *</label>
                  <div class="input-group">
                    <span class="input-group-text bg-light fw-bold text-primary">
                      <i class="bi bi-upc-scan me-1"></i>
                    </span>
                    <input
                      type="text"
                      class="form-control fw-bold"
                      [(ngModel)]="currentProcedure.code"
                      name="code"
                      placeholder="Selecciona categoría para autogenerar código"
                      required
                    />
                  </div>
                  <div class="form-text text-success small" *ngIf="currentProcedure.code">
                    <i class="bi bi-check-circle me-1"></i>Código autogenerado según correlativo: <strong>{{ currentProcedure.code }}</strong>
                  </div>
                </div>

                <div class="mb-3">
                  <label class="form-label fw-semibold">Nombre del Trámite *</label>
                  <input type="text" class="form-control" [(ngModel)]="currentProcedure.name" name="name" placeholder="Ej: Mantención de Semáforos" required />
                </div>
                <div class="mb-3">
                  <label class="form-label fw-semibold">Departamento *</label>
                  <input type="text" class="form-control" [(ngModel)]="currentProcedure.department" name="department" placeholder="Ej: Tránsito y Transporte" required />
                </div>
                <div class="row g-2 mb-3">
                  <div class="col-6">
                    <label class="form-label fw-semibold">Cupo Diario *</label>
                    <input type="number" class="form-control" [(ngModel)]="currentProcedure.dailyQuota" name="dailyQuota" min="1" required />
                  </div>
                  <div class="col-6">
                    <label class="form-label fw-semibold">Cupo Disponible</label>
                    <input type="number" class="form-control" [(ngModel)]="currentProcedure.availableQuota" name="availableQuota" min="0" required />
                  </div>
                </div>
                <div class="mb-3">
                  <label class="form-label fw-semibold">Descripción</label>
                  <textarea class="form-control" rows="2" [(ngModel)]="currentProcedure.description" name="description"></textarea>
                </div>
                <div class="mb-3">
                  <label class="form-label fw-semibold">Requisitos</label>
                  <textarea class="form-control" rows="2" [(ngModel)]="currentProcedure.requirements" name="requirements"></textarea>
                </div>

                <div class="mt-4 text-end">
                  <button type="button" class="btn btn-secondary me-2" (click)="closeModal()">Cancelar</button>
                  <button type="submit" class="btn btn-primary px-4">Guardar</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .fs-7 { font-size: 0.75rem; }
  `]
})
export class CatalogComponent implements OnInit {
  private apiService = inject(ApiService);
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);

  currentUser: UserProfile | null = null;
  procedures: ProcedureType[] = [];

  showModal = false;
  isEditing = false;
  selectedCategory = '';
  currentProcedure: Partial<ProcedureType> = {};

  alertMessage = '';
  alertClass = 'alert-success';
  alertIcon = 'bi-check-circle-fill';

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadProcedures();
  }

  loadProcedures(): void {
    this.apiService.getProcedures().subscribe({
      next: (data) => {
        this.procedures = data;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.showAlert('Error cargando catálogo comunal.', 'alert-danger');
        this.cdr.detectChanges();
      }
    });
  }

  openCreateModal(): void {
    this.isEditing = false;
    this.selectedCategory = '';
    this.currentProcedure = {
      code: '',
      name: '',
      department: '',
      dailyQuota: 10,
      availableQuota: 10,
      description: '',
      requirements: '',
      active: true
    };
    this.showModal = true;
    this.cdr.detectChanges();
  }

  onCategoryChange(): void {
    if (!this.selectedCategory) return;

    const categoryMap: { [key: string]: { prefix: string; department: string } } = {
      LUM: { prefix: 'LUM', department: 'Alumbrado Público' },
      ESC: { prefix: 'ESC', department: 'Medio Ambiente' },
      PAV: { prefix: 'PAV', department: 'Obras Comunales' },
      POD: { prefix: 'POD', department: 'Aseo y Ornato' },
      OTR: { prefix: 'COM', department: 'Atención Vecinal' }
    };

    const config = categoryMap[this.selectedCategory];
    if (config) {
      this.currentProcedure.code = this.calculateNextCode(config.prefix);
      this.currentProcedure.department = config.department;
      this.cdr.detectChanges();
    }
  }

  calculateNextCode(prefix: string): string {
    // 1. Contar cuántos trámites ya existen creados con este prefijo/tipo
    const matching = this.procedures.filter(p =>
      p.code && p.code.toUpperCase().startsWith(prefix.toUpperCase())
    );

    // Número siguiente según la cantidad ya creada para este tipo
    let nextNum = matching.length + 1;
    let candidate = `${prefix}-${String(nextNum).padStart(2, '0')}`;

    // Si por algún motivo ya existe este código en el catálogo, buscar el siguiente correlativo disponible
    while (this.procedures.some(p => p.code?.toUpperCase() === candidate.toUpperCase())) {
      nextNum++;
      candidate = `${prefix}-${String(nextNum).padStart(2, '0')}`;
    }

    return candidate;
  }


  openEditModal(proc: ProcedureType): void {
    this.isEditing = true;
    this.currentProcedure = { ...proc };
    this.showModal = true;
    this.cdr.detectChanges();
  }

  closeModal(): void {
    this.showModal = false;
    this.currentProcedure = {};
    this.cdr.detectChanges();
  }

  saveProcedure(): void {
    if (this.isEditing && this.currentProcedure.id) {
      this.apiService.updateProcedure(this.currentProcedure.id, this.currentProcedure).subscribe({
        next: () => {
          this.showModal = false;
          this.showAlert('Trámite actualizado correctamente.', 'alert-success');
          this.loadProcedures();
        },
        error: (err) => {
          this.showAlert('Error al actualizar: ' + err.message, 'alert-danger');
          this.cdr.detectChanges();
        }
      });
    } else {
      this.apiService.createProcedure(this.currentProcedure as ProcedureType).subscribe({
        next: () => {
          this.showModal = false;
          this.showAlert('Nuevo tipo de trámite añadido al catálogo.', 'alert-success');
          this.loadProcedures();
        },
        error: (err) => {
          this.showAlert('Error al crear trámite: ' + err.message, 'alert-danger');
          this.cdr.detectChanges();
        }
      });
    }
  }

  deleteProcedure(proc: ProcedureType): void {
    if (!proc.id) return;
    const confirmDelete = confirm(`¿Estás seguro de que deseas eliminar "${proc.name}" (${proc.code}) del catálogo comunal?`);
    if (confirmDelete) {
      this.apiService.deleteProcedure(proc.id).subscribe({
        next: () => {
          this.showAlert(`Trámite "${proc.name}" (${proc.code}) eliminado exitosamente.`, 'alert-success');
          this.loadProcedures();
        },
        error: (err) => {
          this.showAlert('Error al eliminar trámite: ' + (err.error?.message || err.message), 'alert-danger');
          this.cdr.detectChanges();
        }
      });
    }
  }

  getQuotaProgressClass(available: number, total: number): string {

    const ratio = available / total;
    if (ratio > 0.5) return 'bg-success';
    if (ratio > 0.2) return 'bg-warning';
    return 'bg-danger';
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
}
