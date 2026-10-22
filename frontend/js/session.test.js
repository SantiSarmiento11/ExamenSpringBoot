/**
 * TechNova IT Helpdesk - Pruebas Unitarias para el Módulo de Sesión y RBAC
 */

import { saveSession, getUser, getToken, isLoggedIn, hasRole, can, clearSession, parseJwt, isTokenExpired } from './session.js';

export function runSessionTests() {
  const results = [];

  const assert = (condition, description) => {
    results.push({ description, passed: Boolean(condition) });
    if (!condition) {
      console.error(`[FAIL] ${description}`);
    } else {
      console.log(`[PASS] ${description}`);
    }
  };

  clearSession();

  // Test 1: Estado inicial sin sesión
  assert(getUser() === null, 'Estado inicial: getUser() debe ser null');
  assert(getToken() === null, 'Estado inicial: getToken() debe ser null');
  assert(isLoggedIn() === false, 'Estado inicial: isLoggedIn() debe ser false');

  // Test 2: Guardado de sesión con rol ADMIN
  const mockAdminAuth = {
    token: 'jwt.mock.token.123',
    type: 'Bearer',
    id: 1,
    email: 'admin@technova.com',
    firstName: 'Admin',
    lastName: 'TechNova',
    roles: ['ADMIN'],
  };

  const mockAdminProfile = {
    id: 1,
    email: 'admin@technova.com',
    firstName: 'Admin',
    lastName: 'TechNova',
    roles: [{ name: 'ROLE_ADMIN' }],
    permissions: [{ name: 'TICKET_VIEW_ALL' }, { name: 'TICKET_ASSIGN' }],
  };

  saveSession(mockAdminAuth, mockAdminProfile);

  assert(isLoggedIn() === true, 'isLoggedIn() es true tras login');
  assert(getToken() === 'jwt.mock.token.123', 'getToken() retorna token almacenado');

  const adminUser = getUser();
  assert(adminUser && adminUser.id === 1, 'getUser() retorna ID correcto');
  assert(adminUser.fullName === 'Admin TechNova', 'fullName se construye adecuadamente');
  assert(hasRole('ADMIN') === true, 'hasRole("ADMIN") es true para administrador');
  assert(hasRole('ROLE_ADMIN') === true, 'hasRole("ROLE_ADMIN") normaliza prefijo ROLE_');
  assert(hasRole('TECHNICIAN') === false, 'hasRole("TECHNICIAN") es false para administrador');
  assert(can('TICKET_ASSIGN') === true, 'can("TICKET_ASSIGN") retorna true');
  assert(can('CUALQUIER_PERMISO') === true, 'ADMIN tiene acceso universal con can()');

  // Test 3: Guardado de sesión con rol USER
  clearSession();
  const mockUserAuth = {
    token: 'jwt.user.token.456',
    id: 3,
    email: 'usuario@technova.com',
    firstName: 'Santiago',
    lastName: 'Usuario',
    roles: ['USER'],
  };

  const mockUserProfile = {
    id: 3,
    email: 'usuario@technova.com',
    firstName: 'Santiago',
    lastName: 'Usuario',
    roles: ['USER'],
    permissions: ['TICKET_CREATE', 'TICKET_VIEW_OWN', 'TICKET_COMMENT_OWN'],
  };

  saveSession(mockUserAuth, mockUserProfile);
  assert(hasRole('USER') === true, 'hasRole("USER") es true para usuario solicitante');
  assert(hasRole('ADMIN') === false, 'hasRole("ADMIN") es false para usuario solicitante');
  assert(can('TICKET_CREATE') === true, 'can("TICKET_CREATE") es true');
  assert(can('TICKET_ASSIGN') === false, 'can("TICKET_ASSIGN") es false para USER');

  // Test 4: Decodificación y validación de expiración JWT
  const validExp = Math.floor(Date.now() / 1000) + 3600; // expira en 1 hora
  const validPayload = btoa(JSON.stringify({ sub: 'user@test.com', exp: validExp }));
  const fakeValidJwt = `header.${validPayload}.signature`;
  const parsed = parseJwt(fakeValidJwt);
  assert(parsed && parsed.sub === 'user@test.com', 'parseJwt() extrae payload con éxito');

  clearSession();
  saveSession({ token: fakeValidJwt, roles: ['USER'], email: 'user@test.com' });
  assert(isTokenExpired() === false, 'isTokenExpired() es false para token con exp futura');

  const expiredExp = Math.floor(Date.now() / 1000) - 300; // expiró hace 5 min
  const expiredPayload = btoa(JSON.stringify({ sub: 'user@test.com', exp: expiredExp }));
  const fakeExpiredJwt = `header.${expiredPayload}.signature`;
  saveSession({ token: fakeExpiredJwt, roles: ['USER'], email: 'user@test.com' });
  assert(isTokenExpired() === true, 'isTokenExpired() es true para token con exp pasada');
  assert(isLoggedIn() === false, 'isLoggedIn() limpia sesión y retorna false si el token expiró');

  // Test 5: Limpieza de sesión
  clearSession();
  assert(getUser() === null, 'clearSession() elimina datos del usuario');
  assert(getToken() === null, 'clearSession() elimina token');
  assert(isLoggedIn() === false, 'isLoggedIn() es false tras clearSession()');

  return results;
}
