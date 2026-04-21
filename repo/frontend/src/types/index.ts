export type UserRole = 'PASSENGER' | 'DISPATCHER' | 'ADMIN';

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  message?: string | null;
}

export interface AuthUser {
  userId: number;
  username: string;
  role: UserRole;
  displayName?: string;
  token: string;
}

export interface RouteSearchResult {
  id: number;
  routeNumber: string;
  name: string;
  description?: string;
  status?: string;
  stopCount: number;
}

export interface StopSearchResult {
  id: number;
  nameEn: string;
  nameCn?: string;
  address?: string;
  sequenceNumber: number;
  routeId: number;
  routeNumber: string;
  popularityScore: number;
  sortScore: number;
}

export interface ReservationResponse {
  id: number;
  routeId: number;
  routeNumber: string;
  routeName: string;
  stopId: number;
  stopNameEn: string;
  stopNameCn?: string;
  scheduledTime: string;
  status: string;
  createdAt: string;
}

export interface NotificationResponse {
  id: number;
  type: string;
  title: string;
  content: string;
  read: boolean;
  entityId?: number;
  createdAt: string;
}

export interface NotificationPreference {
  id: number;
  routeId?: number;
  stopId?: number;
  reminderMinutes: number;
  enabled: boolean;
  dndEnabled: boolean;
  dndStart?: string;
  dndEnd?: string;
}

export interface WorkflowTaskResponse {
  id: number;
  taskNumber: string;
  type: string;
  title: string;
  description?: string;
  status: string;
  assignedToUsername?: string;
  assignedToId?: number;
  deadline?: string;
  escalated: boolean;
  payload?: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowApprovalResponse {
  id: number;
  taskId: number;
  approverUsername: string;
  action: string;
  comment?: string;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
