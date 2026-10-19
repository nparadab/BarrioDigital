export interface ProcedureType {
  id?: number;
  code: string;
  name: string;
  description?: string;
  department?: string;
  dailyQuota: number;
  availableQuota: number;
  requirements?: string;
  active?: boolean;
}
