/**
 * TechNova IT Helpdesk - Configuración Global del Entorno y Endpoints
 * Conecta el cliente frontend con el backend Spring Boot (puerto 8080).
 */

export const API_BASE = 'http://localhost:8081';
export const FRONTEND_PORT = 5050;

export const ENDPOINTS = {
  AUTH: {
    LOGIN: '/api/auth/login',
    ME: '/api/auth/me',
  },
  TICKETS: {
    BASE: '/api/tickets',
    PAGED: '/api/tickets/paged',
    BY_ID: (id) => `/api/tickets/${id}`,
    ASSIGN: (id) => `/api/tickets/${id}/assign`,
    STATUS: (id) => `/api/tickets/${id}/status`,
    HISTORY: (id) => `/api/tickets/${id}/history`,
    COMMENTS: (id) => `/api/tickets/${id}/comments`,
  },
  CATALOGS: {
    CATEGORIES: '/api/categories',
    PRIORITIES: '/api/priorities',
    TECHNICIANS: '/api/users/technicians',
  },
  USERS: {
    TECHNICIANS: '/api/users/technicians',
  },
  CATEGORIES: {
    BASE: '/api/categories',
  },
  PRIORITIES: {
    BASE: '/api/priorities',
  },
  ADMIN: {
    DASHBOARD: '/api/admin/dashboard',
  },
};

export const ROLES = {
  ADMIN: 'ADMIN',
  TECHNICIAN: 'TECHNICIAN',
  USER: 'USER',
};

export const TICKET_STATUS = {
  ABIERTA: 'ABIERTA',
  ASIGNADA: 'ASIGNADA',
  EN_PROCESO: 'EN_PROCESO',
  RESUELTA: 'RESUELTA',
  CERRADA: 'CERRADA',
};

export const TICKET_PRIORITY = {
  BAJA: 'BAJA',
  MEDIA: 'MEDIA',
  ALTA: 'ALTA',
  CRITICA: 'CRITICA',
};
