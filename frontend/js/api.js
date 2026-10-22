/**
 * TechNova IT Helpdesk - Cliente HTTP Centralizado para API REST
 * Realiza solicitudes asíncronas con inyección de JWT y manejo estandarizado de excepciones.
 */

import { API_BASE } from './config.js';
import { clearSession, getToken } from './session.js';

export class ApiError extends Error {
  constructor(status, message, validationErrors = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.validationErrors = validationErrors;
  }
}

/**
 * Cliente HTTP estandarizado para la API REST de TechNova IT Helpdesk.
 * @param {string} path - Ruta relativa o completa al endpoint (ej. '/api/tickets')
 * @param {Object} options - Opciones de configuración (method, body, auth, headers)
 * @returns {Promise<any>}
 */
export async function api(path, { method = 'GET', body, auth = true, headers = {} } = {}) {
  const reqHeaders = {
    Accept: 'application/json',
    ...headers,
  };

  if (body !== undefined) {
    reqHeaders['Content-Type'] = 'application/json';
  }

  const token = getToken();
  if (auth && token) {
    reqHeaders.Authorization = `Bearer ${token}`;
  }

  const url = path.startsWith('http') ? path : `${API_BASE}${path}`;

  let response;
  try {
    response = await fetch(url, {
      method,
      headers: reqHeaders,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(
      0,
      'No se pudo conectar con el servidor backend (http://localhost:8081). Verifique que el servicio Spring Boot esté activo y la base de datos conectada.'
    );
  }

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    // Si la sesión expira o el token es inválido y no es la pantalla de login
    if (response.status === 401 && auth && !window.location.pathname.endsWith('index.html')) {
      clearSession();
      window.location.replace('index.html?expired=1');
      return;
    }

    // Extraer mensaje de error del backend
    let message = 'Error en la solicitud al servidor';
    if (data && typeof data === 'object') {
      if (Array.isArray(data.validationErrors) && data.validationErrors.length > 0) {
        message = data.validationErrors.join(' | ');
      } else {
        message = data.message || data.error || message;
      }
    } else if (typeof data === 'string' && data.length > 0) {
      message = data;
    }

    throw new ApiError(response.status, message, data?.validationErrors || null);
  }

  return data;
}

export const get = (path, options = {}) => api(path, { method: 'GET', ...options });
export const post = (path, body, options = {}) => api(path, { method: 'POST', body, ...options });
export const put = (path, body, options = {}) => api(path, { method: 'PUT', body, ...options });
export const del = (path, options = {}) => api(path, { method: 'DELETE', ...options });
