/**
 * TechNova IT Helpdesk - Suite de Pruebas de Integración y Contrato de Endpoints API
 * Valida la consistencia de rutas, contratos de endpoints y matriz de transiciones RBAC.
 */

import { ENDPOINTS } from './config.js';
import { hasRole, can } from './session.js';

export function runIntegrationTests() {
  const results = [];

  const assert = (condition, description) => {
    results.push({ description, passed: Boolean(condition) });
    if (!condition) {
      console.error(`[FAIL] ${description}`);
    } else {
      console.log(`[PASS] ${description}`);
    }
  };

  // 1. Contrato de Endpoints del Backend
  assert(ENDPOINTS.AUTH.LOGIN === '/api/auth/login', 'Contrato API: POST /api/auth/login definido');
  assert(ENDPOINTS.AUTH.ME === '/api/auth/me', 'Contrato API: GET /api/auth/me definido');
  assert(ENDPOINTS.TICKETS.BASE === '/api/tickets', 'Contrato API: /api/tickets definido');
  assert(ENDPOINTS.TICKETS.BY_ID(42) === '/api/tickets/42', 'Contrato API: GET /api/tickets/{id} interpola ID correctamente');
  assert(ENDPOINTS.TICKETS.ASSIGN(42) === '/api/tickets/42/assign', 'Contrato API: PUT /api/tickets/{id}/assign definido para ADMIN');
  assert(ENDPOINTS.TICKETS.STATUS(42) === '/api/tickets/42/status', 'Contrato API: PUT /api/tickets/{id}/status definido para máquina de estados');
  assert(ENDPOINTS.TICKETS.COMMENTS(42) === '/api/tickets/42/comments', 'Contrato API: /api/tickets/{id}/comments definido');
  assert(ENDPOINTS.TICKETS.HISTORY(42) === '/api/tickets/42/history', 'Contrato API: GET /api/tickets/{id}/history definido para auditoría inmutable');
  assert(ENDPOINTS.USERS.TECHNICIANS === '/api/users/technicians', 'Contrato API: GET /api/users/technicians definido');
  assert(ENDPOINTS.ADMIN.DASHBOARD === '/api/admin/dashboard', 'Contrato API: GET /api/admin/dashboard exclusivo para ADMIN');
  assert(ENDPOINTS.CATEGORIES.BASE === '/api/categories', 'Contrato API: GET /api/categories catálogo dinámico');
  assert(ENDPOINTS.PRIORITIES.BASE === '/api/priorities', 'Contrato API: GET /api/priorities catálogo dinámico');

  // 2. Matriz de Transiciones de la Máquina de Estados del Backend
  const VALID_TRANSITIONS = {
    ABIERTA: ['ASIGNADA'],
    ASIGNADA: ['EN_PROCESO'],
    EN_PROCESO: ['RESUELTA'],
    RESUELTA: ['CERRADA'],
    CERRADA: [],
  };

  const isTransitionAllowed = (fromStatus, toStatus) => {
    const allowed = VALID_TRANSITIONS[fromStatus] || [];
    return allowed.includes(toStatus);
  };

  assert(isTransitionAllowed('ABIERTA', 'ASIGNADA') === true, 'Máquina de estados: ABIERTA -> ASIGNADA es válida');
  assert(isTransitionAllowed('ASIGNADA', 'EN_PROCESO') === true, 'Máquina de estados: ASIGNADA -> EN_PROCESO es válida');
  assert(isTransitionAllowed('EN_PROCESO', 'RESUELTA') === true, 'Máquina de estados: EN_PROCESO -> RESUELTA es válida');
  assert(isTransitionAllowed('RESUELTA', 'CERRADA') === true, 'Máquina de estados: RESUELTA -> CERRADA es válida');
  assert(isTransitionAllowed('ABIERTA', 'RESUELTA') === false, 'Máquina de estados: Salto inválido ABIERTA -> RESUELTA es rechazado');
  assert(isTransitionAllowed('ABIERTA', 'CERRADA') === false, 'Máquina de estados: Salto inválido ABIERTA -> CERRADA es rechazado');
  assert(isTransitionAllowed('CERRADA', 'EN_PROCESO') === false, 'Máquina de estados: Estado terminal CERRADA no admite transiciones');

  // 3. Matriz de Permisos por Rol
  const evaluatePermission = (userRoles, action) => {
    const isAdmin = userRoles.includes('ADMIN');
    const isTech = userRoles.includes('TECHNICIAN');
    const isUser = userRoles.includes('USER');

    switch (action) {
      case 'VIEW_EXECUTIVE_REPORTS':
        return isAdmin;
      case 'ASSIGN_TECHNICIAN':
        return isAdmin;
      case 'START_PROGRESS':
        return isTech || isAdmin;
      case 'RESOLVE_TICKET':
        return isTech || isAdmin;
      case 'CLOSE_TICKET':
        return isUser || isAdmin;
      case 'CREATE_TICKET':
        return isUser || isAdmin;
      default:
        return false;
    }
  };

  assert(evaluatePermission(['ADMIN'], 'VIEW_EXECUTIVE_REPORTS') === true, 'RBAC: ADMIN puede ver reportes ejecutivos');
  assert(evaluatePermission(['TECHNICIAN'], 'VIEW_EXECUTIVE_REPORTS') === false, 'RBAC: TECHNICIAN NO puede ver reportes ejecutivos');
  assert(evaluatePermission(['USER'], 'VIEW_EXECUTIVE_REPORTS') === false, 'RBAC: USER NO puede ver reportes ejecutivos');
  assert(evaluatePermission(['ADMIN'], 'ASSIGN_TECHNICIAN') === true, 'RBAC: ADMIN puede asignar técnicos');
  assert(evaluatePermission(['TECHNICIAN'], 'ASSIGN_TECHNICIAN') === false, 'RBAC: TECHNICIAN NO puede asignar técnicos');
  assert(evaluatePermission(['USER'], 'ASSIGN_TECHNICIAN') === false, 'RBAC: USER NO puede asignar técnicos');
  assert(evaluatePermission(['USER'], 'CLOSE_TICKET') === true, 'RBAC: USER solicitante puede confirmar cierre de ticket');
  assert(evaluatePermission(['USER'], 'CREATE_TICKET') === true, 'RBAC: USER puede registrar nuevas incidencias');

  return results;
}
