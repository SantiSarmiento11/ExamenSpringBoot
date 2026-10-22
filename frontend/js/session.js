/**
 * TechNova IT Helpdesk - Gestión Centralizada de Sesión y Control de Acceso (RBAC)
 * Almacena y valida el JWT y el perfil de usuario con sus roles y permisos.
 */

const TOKEN_KEY = 'technova_token';
const USER_KEY = 'technova_user';

/**
 * Guarda el token JWT y el perfil de usuario normalizado.
 * @param {Object} auth - Respuesta de login ({ token, type, id, email, firstName, lastName, roles })
 * @param {Object} [profile] - Perfil opcional detallado de /api/auth/me ({ permissions, ... })
 */
export function saveSession(auth, profile = null) {
  if (!auth || !auth.token) return;

  localStorage.setItem(TOKEN_KEY, auth.token);

  // Normalizar roles (pueden venir como string o como objeto con authority o name)
  const rawRoles = profile?.roles || auth.roles || [];
  const normalizedRoles = rawRoles.map((r) => {
    let name = typeof r === 'string' ? r : r.name || r.authority || '';
    if (name.startsWith('ROLE_')) name = name.substring(5);
    return name.toUpperCase();
  });

  const rawPermissions = profile?.permissions || auth.permissions || [];
  const normalizedPermissions = rawPermissions.map((p) => {
    return typeof p === 'string' ? p : p.name || '';
  });

  const firstName = profile?.firstName || auth.firstName || '';
  const lastName = profile?.lastName || auth.lastName || '';
  const fullName = `${firstName} ${lastName}`.trim() || auth.email;

  const user = {
    id: profile?.id ?? auth.id,
    email: profile?.email || auth.email,
    firstName,
    lastName,
    fullName,
    roles: normalizedRoles,
    permissions: normalizedPermissions,
  };

  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

/**
 * Obtiene el token JWT actual.
 * @returns {string|null}
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

/**
 * Obtiene el perfil del usuario autenticado actual.
 * @returns {Object|null}
 */
export function getUser() {
  const data = localStorage.getItem(USER_KEY);
  if (!data) return null;
  try {
    return JSON.parse(data);
  } catch {
    return null;
  }
}

/**
 * Decodifica el payload de un token JWT estándar sin librerías externas.
 * @param {string} token
 * @returns {Object|null}
 */
export function parseJwt(token) {
  if (!token || typeof token !== 'string') return null;
  try {
    const parts = token.split('.');
    if (parts.length < 2) return null;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

/**
 * Verifica si el token JWT actual ha expirado.
 * @returns {boolean}
 */
export function isTokenExpired() {
  const token = getToken();
  if (!token) return true;
  const payload = parseJwt(token);
  if (!payload || !payload.exp) return false;
  const nowInSeconds = Math.floor(Date.now() / 1000);
  return payload.exp < nowInSeconds;
}

/**
 * Verifica si hay una sesión activa con token vigente.
 * @returns {boolean}
 */
export function isLoggedIn() {
  const token = getToken();
  if (!token) return false;
  if (isTokenExpired()) {
    clearSession();
    return false;
  }
  return true;
}

/**
 * Elimina la sesión actual del almacenamiento local.
 */
export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  try {
    sessionStorage.clear();
  } catch {}
}

/**
 * Verifica si el usuario autenticado posee un rol determinado (ej. 'ADMIN', 'TECHNICIAN', 'USER').
 * @param {string} roleName
 * @returns {boolean}
 */
export function hasRole(roleName) {
  const user = getUser();
  if (!user || !user.roles) return false;
  const target = roleName.startsWith('ROLE_') ? roleName.substring(5).toUpperCase() : roleName.toUpperCase();
  return user.roles.some((r) => r.toUpperCase() === target);
}

/**
 * Verifica si el usuario autenticado posee un permiso específico (o es ADMIN).
 * @param {string} permissionName
 * @returns {boolean}
 */
export function can(permissionName) {
  const user = getUser();
  if (!user) return false;
  if (hasRole('ADMIN')) return true;
  return Array.isArray(user.permissions) && user.permissions.includes(permissionName);
}

/**
 * Guardián de ruta: Redirige a index.html si no hay sesión activa.
 * @returns {boolean}
 */
export function requireAuth() {
  if (!isLoggedIn()) {
    window.location.replace('index.html');
    return false;
  }
  return true;
}

/**
 * Guardián de ruta con validación de roles permitidos.
 * @param {string|string[]} allowedRoles
 * @param {string} fallbackUrl
 * @returns {boolean}
 */
export function requireRole(allowedRoles, fallbackUrl = 'dashboard.html') {
  if (!requireAuth()) return false;
  const roles = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];
  const hasAccess = roles.some((r) => hasRole(r));
  if (!hasAccess) {
    window.location.replace(fallbackUrl);
    return false;
  }
  return true;
}
