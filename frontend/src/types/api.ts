export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  errorCode?: string;
  timestamp: string;
  path?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number;
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

// Auth types
export interface RegisterRequest {
  email: string;
  password: string;
  fullName?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    id: string;
    email: string;
    fullName?: string;
    emailVerified: boolean;
  };
}

// Endpoint types
export interface CreateEndpointRequest {
  name: string;
  url: string;
  description?: string;
  headers?: Record<string, string>;
}

export interface UpdateEndpointRequest {
  name?: string;
  url?: string;
  description?: string;
  headers?: Record<string, string>;
  active?: boolean;
}

export interface EndpointResponse {
  id: string;
  name: string;
  url: string;
  description?: string;
  active: boolean;
  headers: Record<string, string>;
  createdAt: string;
  updatedAt: string;
  signingSecret?: string;
}

// Event types
export interface IngestEventRequest {
  eventType: string;
  endpointId: string;
  payload: Record<string, unknown>;
  idempotencyKey?: string;
}

export interface EventResponse {
  id: string;
  endpointId: string;
  eventType: string;
  payload: Record<string, unknown>;
  idempotencyKey?: string;
  status: 'PENDING' | 'DELIVERED' | 'FAILED' | 'RETRY_SCHEDULED' | 'DEAD_LETTERED';
  createdAt: string;
  updatedAt: string;
  deliveredAt?: string;
}

// Delivery attempt types
export interface DeliveryAttemptResponse {
  id: string;
  eventId: string;
  endpointId: string;
  attemptNumber: number;
  requestUrl: string;
  requestMethod: string;
  requestHeaders: Record<string, string>;
  requestBody: Record<string, unknown>;
  responseStatus?: number;
  responseBody?: string;
  responseHeaders: Record<string, string>;
  durationMs?: number;
  errorMessage?: string;
  status: 'IN_PROGRESS' | 'SUCCESS' | 'FAILURE' | 'TIMEOUT';
  attemptedAt: string;
  completedAt?: string;
}

// Dead letter types
export interface DeadLetterResponse {
  id: string;
  eventId: string;
  endpointId: string;
  eventType: string;
  failureReason?: string;
  attemptCount: number;
  lastAttemptAt?: string;
  resolved: boolean;
  resolvedAt?: string;
  resolvedBy?: string;
  createdAt: string;
}

// Analytics types
export interface AnalyticsDashboardResponse {
  totalEvents: number;
  totalDelivered: number;
  totalFailed: number;
  totalPending: number;
  totalDeadLettered: number;
  totalRetries: number;
  successRate: number;
  averageResponseTimeMs: number;
  recentEvents: RecentEventInfo[];
  endpointPerformance: EndpointPerformanceResponse[];
  volumeOverTime: VolumeOverTimeResponse[];
}

export interface RecentEventInfo {
  id: string;
  eventType: string;
  status: string;
  endpointName: string;
  createdAt: string;
}

export interface EndpointPerformanceResponse {
  endpointId: string;
  endpointName: string;
  totalEvents: number;
  delivered: number;
  failed: number;
  pending: number;
  deadLettered: number;
  successRate: number;
  averageResponseTimeMs: number;
}

export interface VolumeOverTimeResponse {
  date: string;
  total: number;
  delivered: number;
  failed: number;
}