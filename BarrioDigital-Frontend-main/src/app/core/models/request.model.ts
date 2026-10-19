export type RequestStatus =
  | 'INGRESADO'
  | 'ADMITIDO'
  | 'EN_GESTION'
  | 'EN_TERRENO'
  | 'RESUELTO'
  | 'RECHAZADO';

export interface RequestItem {
  id?: number;
  trackingNumber?: string;
  citizenDni: string;
  citizenName: string;
  citizenEmail?: string;
  citizenPhone?: string;
  procedureTypeId: number;
  procedureName: string;
  description: string;
  address: string;
  status: RequestStatus;
  assignedCrew?: string;
  resolutionNotes?: string;
  createdBy?: string;
  admittedBy?: string;
  visitedBy?: string;
  resolvedBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AuditLog {
  id: number;
  requestId: number;
  trackingNumber: string;
  previousStatus?: string;
  newStatus: string;
  action: string;
  performedBy: string;
  userRole: string;
  comments?: string;
  timestamp: string;
}

export interface KpisResponse {
  totalRequests: number;
  activeRequests: number;
  resolvedRequests: number;
  rejectedRequests: number;
  byStatus: Record<RequestStatus, number>;
  avgResolutionHours: number;
  topProcedure: string;
}
