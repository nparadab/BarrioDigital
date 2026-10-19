import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RequestItem, AuditLog, KpisResponse } from '../models/request.model';
import { ProcedureType } from '../models/procedure.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Trámites (Requests)
  getRequests(status?: string, dni?: string, email?: string): Observable<RequestItem[]> {
    let params: any = {};
    if (status) params.status = status;
    if (dni) params.citizenDni = dni;
    if (email) params.citizenEmail = email;
    return this.http.get<RequestItem[]>(`${this.baseUrl}/requests`, { params });
  }

  getRequestById(id: number): Observable<RequestItem> {
    return this.http.get<RequestItem>(`${this.baseUrl}/requests/${id}`);
  }

  getByTrackingNumber(trackingNumber: string): Observable<RequestItem> {
    return this.http.get<RequestItem>(`${this.baseUrl}/requests/tracking/${trackingNumber}`);
  }

  createRequest(request: Partial<RequestItem>): Observable<RequestItem> {
    return this.http.post<RequestItem>(`${this.baseUrl}/requests`, request);
  }

  updateRequestStatus(
    id: number,
    payload: {
      status: string;
      assignedCrew?: string;
      resolutionNotes?: string;
      comments?: string;
    }
  ): Observable<RequestItem> {
    return this.http.put<RequestItem>(`${this.baseUrl}/requests/${id}/status`, payload);
  }

  // Catálogo (Catalog)
  getProcedures(): Observable<ProcedureType[]> {
    return this.http.get<ProcedureType[]>(`${this.baseUrl}/catalog/procedures`);
  }

  getProcedureById(id: number): Observable<ProcedureType> {
    return this.http.get<ProcedureType>(`${this.baseUrl}/catalog/procedures/${id}`);
  }

  createProcedure(procedure: ProcedureType): Observable<ProcedureType> {
    return this.http.post<ProcedureType>(`${this.baseUrl}/catalog/procedures`, procedure);
  }

  updateProcedure(id: number, procedure: Partial<ProcedureType>): Observable<ProcedureType> {
    return this.http.put<ProcedureType>(`${this.baseUrl}/catalog/procedures/${id}`, procedure);
  }

  deleteProcedure(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/catalog/procedures/${id}`);
  }


  // Reportería y KPIs
  getKpis(range: string = 'last24h'): Observable<KpisResponse> {
    return this.http.get<KpisResponse>(`${this.baseUrl}/report/kpis`, { params: { range } });
  }

  getTopProcedures(range: string = 'last7d'): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/report/top-procedures`, { params: { range } });
  }

  // Auditoría
  getAuditTimeline(): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.baseUrl}/audit/timeline`);
  }

  getRequestTimeline(requestId: number): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.baseUrl}/audit/requests/${requestId}`);
  }
}
