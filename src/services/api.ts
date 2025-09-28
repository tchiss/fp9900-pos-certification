import axios, { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import Config from '../config/Config';
import AuthService from './AuthService';
import MonitoringService from './MonitoringService';

// Extend Axios request config to include metadata
declare module 'axios' {
  interface InternalAxiosRequestConfig {
    metadata?: {
      startTime: number;
    };
  }
}

// Create axios instance with configuration
export const api = axios.create({
  baseURL: Config.apiBaseUrl,
  timeout: Config.apiTimeout,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'X-Terminal-ID': 'FP9900_001', // TODO: Get from Config or AuthService
  },
});

// Request interceptor for logging, auth, and monitoring
api.interceptors.request.use(
  (config) => {
    const startTime = Date.now();
    config.metadata = { startTime };
    
    console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`);
    
    // Add auth token if available
    const token = AuthService.getCurrentToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  (error) => {
    console.error('[API] Request error:', error);
    MonitoringService.recordError('api_request', error);
    return Promise.reject(error);
  }
);

// Response interceptor for error handling and monitoring
api.interceptors.response.use(
  (response: AxiosResponse) => {
    const endTime = Date.now();
    const startTime = response.config.metadata?.startTime;
    const latency = startTime ? endTime - startTime : 0;
    
    console.log(`[API] ${response.status} ${response.config.url} (${latency}ms)`);
    
    // Record API call metrics
    MonitoringService.recordApiCall(
      response.config.url || 'unknown',
      response.config.method || 'unknown',
      latency,
      true,
      response.status
    );
    
    return response;
  },
  (error: AxiosError) => {
    const endTime = Date.now();
    const startTime = error.config?.metadata?.startTime;
    const latency = startTime ? endTime - startTime : 0;
    
    console.error('[API] Response error:', error.response?.status, error.message);
    
    // Record API call metrics for failed requests
    if (error.config) {
      MonitoringService.recordApiCall(
        error.config.url || 'unknown',
        error.config.method || 'unknown',
        latency,
        false,
        error.response?.status
      );
    }
    
    // Handle authentication errors
    if (error.response?.status === 401) {
      // Token expired, try to refresh
      AuthService.refreshToken().catch(() => {
        // Refresh failed, user needs to login again
        console.warn('[API] Authentication failed, user needs to login');
      });
    }
    
    return Promise.reject(error);
  }
);

export type InvoiceLine = {
  label: string;
  qty: number;
  unitPrice: number;
};

export type InvoiceRequest = {
  issuerIFU: string;
  buyerIFU?: string;
  buyerName?: string;
  items: InvoiceLine[];
  total: number;
  metadata?: Record<string, any>;
};

export type InvoiceResponse = {
  status: 'CERTIFIED' | 'PENDING' | 'REJECTED';
  mecefCode?: string;
  qrData?: string;
  dgiInvoiceId?: string;
  warnings?: string[];
  reasons?: string[];
  timestamp?: string;
};

export interface ApiErrorData {
  message: string;
  code?: string;
  details?: any;
}

export class ApiError extends Error {
  constructor(
    message: string,
    public code?: string,
    public status?: number,
    public details?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export async function certifyInvoice(payload: InvoiceRequest): Promise<InvoiceResponse> {
  try {
    const response = await api.post<InvoiceResponse>('/api/invoices', payload);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const data = error.response?.data;
      
      // Network error
      if (!error.response) {
        throw new ApiError(
          'Erreur de connexion - Vérifiez votre réseau',
          'NETWORK_ERROR',
          undefined,
          error.message
        );
      }
      
      // HTTP errors
      switch (status) {
        case 400:
          throw new ApiError(
            'Données invalides - Vérifiez les informations saisies',
            'BAD_REQUEST',
            400,
            data
          );
        case 401:
          throw new ApiError(
            'Non autorisé - Vérifiez vos identifiants',
            'UNAUTHORIZED',
            401,
            data
          );
        case 403:
          throw new ApiError(
            'Accès refusé - Permissions insuffisantes',
            'FORBIDDEN',
            403,
            data
          );
        case 404:
          throw new ApiError(
            'Service non trouvé - Contactez le support',
            'NOT_FOUND',
            404,
            data
          );
        case 422:
          throw new ApiError(
            'Données de facture invalides - Vérifiez le format',
            'VALIDATION_ERROR',
            422,
            data
          );
        case 500:
          throw new ApiError(
            'Erreur serveur - Réessayez plus tard',
            'SERVER_ERROR',
            500,
            data
          );
        case 503:
          throw new ApiError(
            'Service temporairement indisponible - Service DGI en maintenance',
            'SERVICE_UNAVAILABLE',
            503,
            data
          );
        default:
          throw new ApiError(
            `Erreur ${status} - ${data?.message || 'Erreur inconnue'}`,
            'UNKNOWN_ERROR',
            status,
            data
          );
      }
    }
    
    // Non-Axios error
    throw new ApiError(
      'Erreur inattendue - Réessayez plus tard',
      'UNEXPECTED_ERROR',
      undefined,
      error
    );
  }
}

// Health check function
export async function checkApiHealth(): Promise<boolean> {
  try {
    const response = await api.get('/health', { timeout: 5000 });
    return response.status === 200;
  } catch (error) {
    console.error('[API] Health check failed:', error);
    return false;
  }
}

// Retry utility
export async function withRetry<T>(
  fn: () => Promise<T>,
  maxRetries: number = 3,
  delay: number = 1000
): Promise<T> {
  let lastError: Error;
  
  for (let i = 0; i <= maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error as Error;
      
      // Don't retry on certain errors
      if (error instanceof ApiError) {
        if (error.status === 400 || error.status === 401 || error.status === 403) {
          throw error;
        }
      }
      
      if (i === maxRetries) {
        throw lastError;
      }
      
      // Exponential backoff
      const waitTime = delay * Math.pow(2, i);
      console.log(`[API] Retry ${i + 1}/${maxRetries} in ${waitTime}ms`);
      await new Promise(resolve => setTimeout(resolve, waitTime));
    }
  }
  
  throw lastError!;
}
