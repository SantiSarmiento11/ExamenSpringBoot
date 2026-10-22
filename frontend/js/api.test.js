/**
 * TechNova IT Helpdesk - Pruebas Unitarias para el Módulo HTTP y ApiError
 */

import { ApiError } from './api.js';

export function runApiTests() {
  const results = [];

  const assert = (condition, description) => {
    results.push({ description, passed: Boolean(condition) });
    if (!condition) {
      console.error(`[FAIL] ${description}`);
    } else {
      console.log(`[PASS] ${description}`);
    }
  };

  // Test 1: Instanciación de ApiError
  const err = new ApiError(400, 'Datos inválidos', ['Campo título requerido']);
  assert(err instanceof Error, 'ApiError hereda de Error');
  assert(err.name === 'ApiError', 'err.name es "ApiError"');
  assert(err.status === 400, 'err.status es 400');
  assert(err.message === 'Datos inválidos', 'err.message es "Datos inválidos"');
  assert(Array.isArray(err.validationErrors), 'err.validationErrors es un arreglo');
  assert(err.validationErrors[0] === 'Campo título requerido', 'Contenido de validationErrors correcto');

  // Test 2: ApiError para 401 Unauthorized
  const authErr = new ApiError(401, 'Credenciales inválidas');
  assert(authErr.status === 401, 'Status 401 para error de autenticación');
  assert(authErr.validationErrors === null, 'validationErrors es null si no se proporciona');

  // Test 3: ApiError para 403 Forbidden
  const forbiddenErr = new ApiError(403, 'Acceso denegado: rol insuficiente');
  assert(forbiddenErr.status === 403, 'Status 403 para error de autorización');

  // Test 4: ApiError de conexión (status 0)
  const netErr = new ApiError(0, 'Servidor no disponible');
  assert(netErr.status === 0, 'Status 0 para falla de red');

  return results;
}
