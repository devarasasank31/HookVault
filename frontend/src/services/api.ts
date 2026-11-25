import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import type {
  ApiResponse,
  PaginatedResponse,
  RegisterRequest,
  LoginRequest,
  AuthResponse,
  CreateEndpointRequest,
  UpdateEndpointRequest,
  EndpointResponse,
  IngestEventRequest,
  EventResponse,
  DeliveryAttemptResponse,
  DeadLetterResponse,
  AnalyticsDashboardResponse,
} from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// Token management
let accessToken: string | null = null;
let refreshToken: string | null = null;

export const setTokens = (access: string, refresh: string) => {
  accessToken = access;
  refreshToken = refresh;
  localStorage.setItem('accessToken', access);
  localStorage.setItem('refreshToken', refresh);
};

export const getAccessToken = () => accessToken || localStorage.getItem('accessToken');
export const getRefreshToken = () => refreshToken || localStorage.getItem('refreshToken');

export const clearTokens = () => {
  accessToken = null;
  refreshToken = null;
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
};

// Request interceptor to add auth header
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for token refresh
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: unknown) => void;
  reject: (reason: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        }).catch((err) => {
          return Promise.reject(err);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refresh = getRefreshToken();
        if (!refresh) {
          throw new Error('No refresh token');
        }

        const response = await axios.post<ApiResponse<AuthResponse>>(
          `${API_BASE_URL}/v1/auth/refresh`,
          {},
          { headers: { Authorization: `Bearer ${refresh}` } }
        );

        const { accessToken: newAccess, refreshToken: newRefresh } = response.data.data!;
        setTokens(newAccess, newRefresh);

        processQueue(null, newAccess);
        originalRequest.headers.Authorization = `Bearer ${newAccess}`;
        return api(originalRequest);
      } catch (err) {
        processQueue(err, null);
        clearTokens();
        window.location.href = '/login';
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

// Auth API
export const authApi = {
  register: (data: RegisterRequest) =>
    api.post<ApiResponse<AuthResponse>>('/v1/auth/register', data),

  login: (data: LoginRequest) =>
    api.post<ApiResponse<AuthResponse>>('/v1/auth/login', data),

  refresh: () =>
    api.post<ApiResponse<AuthResponse>>('/v1/auth/refresh'),

  logout: () =>
    api.post<ApiResponse<void>>('/v1/auth/logout'),
};

// Endpoints API
export const endpointsApi = {
  list: (params?: { page?: number; size?: number; sort?: string }) =>
    api.get<ApiResponse<PaginatedResponse<EndpointResponse>>>('/v1/endpoints', { params }),

  get: (id: string) =>
    api.get<ApiResponse<EndpointResponse>>(`/v1/endpoints/${id}`),

  create: (data: CreateEndpointRequest) =>
    api.post<ApiResponse<EndpointResponse>>('/v1/endpoints', data),

  update: (id: string, data: UpdateEndpointRequest) =>
    api.patch<ApiResponse<EndpointResponse>>(`/v1/endpoints/${id}`, data),

  rotateSecret: (id: string) =>
    api.post<ApiResponse<{ signingSecret: string }>>(`/v1/endpoints/${id}/rotate-secret`),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`/v1/endpoints/${id}`),
};

// Events API
export const eventsApi = {
  list: (params?: {
    page?: number;
    size?: number;
    sort?: string;
    endpointId?: string;
    eventType?: string;
    status?: string;
  }) =>
    api.get<ApiResponse<PaginatedResponse<EventResponse>>>('/v1/events', { params }),

  get: (id: string) =>
    api.get<ApiResponse<EventResponse>>(`/v1/events/${id}`),

  ingest: (data: IngestEventRequest) =>
    api.post<ApiResponse<EventResponse>>('/v1/events', data),
};

// Delivery Attempts API
export const deliveryAttemptsApi = {
  list: (params?: {
    page?: number;
    size?: number;
    sort?: string;
    eventId?: string;
    endpointId?: string;
    status?: string;
  }) =>
    api.get<ApiResponse<PaginatedResponse<DeliveryAttemptResponse>>>('/v1/delivery-attempts', { params }),

  get: (id: string) =>
    api.get<ApiResponse<DeliveryAttemptResponse>>(`/v1/delivery-attempts/${id}`),

  getByEvent: (eventId: string, params?: { page?: number; size?: number; sort?: string }) =>
    api.get<ApiResponse<PaginatedResponse<DeliveryAttemptResponse>>>(`/v1/delivery-attempts/event/${eventId}`, { params }),
};

// Dead Letters API
export const deadLettersApi = {
  list: (params?: {
    page?: number;
    size?: number;
    sort?: string;
    endpointId?: string;
    resolved?: boolean;
  }) =>
    api.get<ApiResponse<PaginatedResponse<DeadLetterResponse>>>('/v1/dead-letters', { params }),

  get: (id: string) =>
    api.get<ApiResponse<DeadLetterResponse>>(`/v1/dead-letters/${id}`),

  replay: (id: string) =>
    api.post<ApiResponse<DeadLetterResponse>>(`/v1/dead-letters/${id}/replay`),

  resolve: (id: string) =>
    api.post<ApiResponse<DeadLetterResponse>>(`/v1/dead-letters/${id}/resolve`),

  dismiss: (id: string) =>
    api.delete<ApiResponse<void>>(`/v1/dead-letters/${id}`),
};

// Analytics API
export const analyticsApi = {
  getDashboard: () =>
    api.get<ApiResponse<AnalyticsDashboardResponse>>('/v1/analytics/dashboard'),
};

export default api;