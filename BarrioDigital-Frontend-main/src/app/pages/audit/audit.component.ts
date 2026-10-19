import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuditLog } from '../../core/models/request.model';

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container py-4">
      <!-- Encabezado -->
      <div class="d-flex flex-wrap justify-content-between align-items-center mb-4 pb-3 border-bottom">
        <div>
          <h2 class="fw-bold text-dark mb-1">
            <i class="bi bi-shield-check text-primary me-2"></i>Auditoría y Trazabilidad de Trámites
          </h2>
          <p class="text-muted mb-0">
            Registro inmutable de eventos: quién ingresó, admitió, visitó en terreno o resolvió cada solicitud.
          </p>
        </div>
        <div class="mt-2 mt-md-0">
          <span class="badge bg-warning text-dark fs-6 px-3 py-2">
            <i class="bi bi-eye-fill me-1"></i>Acceso Solo Lectura &bull; Admin & Auditor
          </span>
        </div>
      </div>

      <!-- Filtros de Auditoría -->
      <div class="card shadow-sm border-0 mb-4 p-3 bg-light">
        <div class="row g-3 align-items-center">
          <div class="col-md-5">
            <label class="form-label small fw-bold text-muted">Buscar por Trámite o Usuario:</label>
            <div class="input-group">
              <span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
              <input
                type="text"
                class="form-control"
                placeholder="Ej: BD-2025-0001, funcionario@..."
                [(ngModel)]="searchQuery"
                (input)="applyFilters()"
              />
            </div>
          </div>
          <div class="col-md-4">
            <label class="form-label small fw-bold text-muted">Filtrar por Acción:</label>
            <select class="form-select" [(ngModel)]="filterAction" (change)="applyFilters()">
              <option value="">Todas las acciones</option>
              <option value="CREACION_SOLICITUD">CREACION_SOLICITUD</option>
              <option value="ADMISION_SOLICITUD">ADMISION_SOLICITUD</option>
              <option value="CAMBIO_ESTADO">CAMBIO_ESTADO</option>
              <option value="ASIGNACION_CUADRILLA">ASIGNACION_CUADRILLA</option>
            </select>
          </div>
          <div class="col-md-3 d-flex align-items-end">
            <button class="btn btn-outline-secondary w-100" (click)="resetFilters()">
              <i class="bi bi-arrow-clockwise me-1"></i> Restablecer
            </button>
          </div>
        </div>
      </div>

      <!-- Tabla de Eventos de Auditoría -->
      <div class="card shadow-sm border-0">
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
              <thead class="table-light text-uppercase small text-muted">
                <tr>
                  <th>Timestamp</th>
                  <th>N° Trámite</th>
                  <th>Acción</th>
                  <th>Transición</th>
                  <th>Ejecutado Por</th>
                  <th>Rol</th>
                  <th>Comentarios</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let log of filteredLogs">
                  <td class="small text-muted text-nowrap">
                    {{ log.timestamp | date:'medium' }}
                  </td>
                  <td class="fw-bold text-primary">{{ log.trackingNumber }}</td>
                  <td>
                    <span class="badge bg-secondary">{{ log.action }}</span>
                  </td>
                  <td>
                    <span *ngIf="log.previousStatus" class="small text-muted">{{ log.previousStatus }} &rarr; </span>
                    <span class="badge bg-primary small">{{ log.newStatus }}</span>
                  </td>
                  <td class="fw-semibold">{{ log.performedBy }}</td>
                  <td>
                    <span class="badge" [ngClass]="getRoleBadgeClass(log.userRole)">
                      {{ log.userRole }}
                    </span>
                  </td>
                  <td class="small text-muted" style="max-width: 250px;">
                    {{ log.comments || '-' }}
                  </td>
                </tr>
                <tr *ngIf="filteredLogs.length === 0">
                  <td colspan="7" class="text-center py-4 text-muted">
                    No se encontraron registros de auditoría coincidentes.
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  `
})
export class AuditComponent implements OnInit {
  private apiService = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);

  logs: AuditLog[] = [];
  filteredLogs: AuditLog[] = [];

  searchQuery = '';
  filterAction = '';

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.apiService.getAuditTimeline().subscribe({
      next: (data) => {
        this.logs = data;
        this.applyFilters();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando registros de auditoría:', err);
        this.cdr.detectChanges();
      }
    });
  }

  applyFilters(): void {
    let result = [...this.logs];

    if (this.filterAction) {
      result = result.filter((l) => l.action === this.filterAction);
    }

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(
        (l) =>
          l.trackingNumber.toLowerCase().includes(q) ||
          l.performedBy.toLowerCase().includes(q) ||
          (l.comments && l.comments.toLowerCase().includes(q))
      );
    }

    this.filteredLogs = result;
    this.cdr.detectChanges();
  }

  resetFilters(): void {
    this.searchQuery = '';
    this.filterAction = '';
    this.filteredLogs = [...this.logs];
    this.cdr.detectChanges();
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
