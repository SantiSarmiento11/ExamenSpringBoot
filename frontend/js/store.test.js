/**
 * TechNova IT Helpdesk - Pruebas Unitarias para el Módulo de Store y Caché Persistente
 */

import { Store } from './store.js';
import { clearSession } from './session.js';

export function runStoreTests() {
  const results = [];

  const assert = (condition, description) => {
    results.push({ description, passed: Boolean(condition) });
    if (!condition) {
      console.error(`[FAIL] ${description}`);
    } else {
      console.log(`[PASS] ${description}`);
    }
  };

  Store.clearAll();

  // Test 1: Estado inicial vacío
  assert(Store.getTickets() === null, 'Estado inicial: getTickets() retorna null');
  assert(Store.getMetrics() === null, 'Estado inicial: getMetrics() retorna null');
  assert(Store.getCategories() === null, 'Estado inicial: getCategories() retorna null');
  assert(Store.getPriorities() === null, 'Estado inicial: getPriorities() retorna null');
  assert(Store.getTechnicians() === null, 'Estado inicial: getTechnicians() retorna null');
  assert(Store.getTicket(999) === null, 'Estado inicial: getTicket(999) retorna null');

  // Test 2: Almacenamiento y recuperación de tickets
  const mockTickets = [
    { id: 101, title: 'Fallo en VPN', status: 'ABIERTA', priority: 'ALTA' },
    { id: 102, title: 'Error en ERP', status: 'EN_PROCESO', priority: 'MEDIA' },
  ];
  Store.setTickets(mockTickets);
  const cachedTickets = Store.getTickets();
  assert(Array.isArray(cachedTickets) && cachedTickets.length === 2, 'setTickets() / getTickets() preserva array');
  assert(cachedTickets[0].title === 'Fallo en VPN', 'Datos de ticket 1 coinciden');

  // Test 3: Almacenamiento y recuperación de métricas
  const mockMetrics = {
    totalTickets: 15,
    openTickets: 5,
    assignedTickets: 3,
    inProgressTickets: 4,
    resolvedTickets: 2,
    closedTickets: 1,
  };
  Store.setMetrics(mockMetrics);
  const cachedMetrics = Store.getMetrics();
  assert(cachedMetrics && cachedMetrics.totalTickets === 15, 'setMetrics() / getMetrics() almacena métricas reales');
  assert(cachedMetrics.openTickets === 5, 'Métrica openTickets es consistente');

  // Test 4: Catálogos de categorías, prioridades y técnicos
  Store.setCategories([{ id: 1, name: 'Hardware' }, { id: 2, name: 'Redes' }]);
  Store.setPriorities([{ id: 1, name: 'ALTA' }, { id: 2, name: 'BAJA' }]);
  Store.setTechnicians([{ id: 5, fullName: 'Carlos Técnico' }]);

  assert(Store.getCategories().length === 2, 'getCategories() recupera catálogo de categorías');
  assert(Store.getPriorities().length === 2, 'getPriorities() recupera catálogo de prioridades');
  assert(Store.getTechnicians().length === 1, 'getTechnicians() recupera lista de técnicos');

  // Test 5: Cache de detalle de ticket individual
  const detailedTicket = { id: 101, title: 'Fallo en VPN', status: 'ABIERTA', comments: [] };
  Store.setTicket(101, detailedTicket);
  const cachedDetail = Store.getTicket(101);
  assert(cachedDetail && cachedDetail.id === 101, 'setTicket() / getTicket(id) almacena detalle específico');

  // Test 6: Búsqueda fallback en tickets general si no existe detalle específico
  const fallbackTicket = Store.getTicket(102);
  assert(fallbackTicket && fallbackTicket.id === 102, 'getTicket(id) hace fallback a lista general de tickets');

  // Test 7: Actualización de ticket individual en lista de tickets
  Store.updateTicketInCache({ id: 101, title: 'Fallo en VPN Solucionado', status: 'RESUELTA' });
  const updatedTickets = Store.getTickets();
  const found = updatedTickets.find(t => t.id === 101);
  assert(found && found.status === 'RESUELTA', 'updateTicketInCache() actualiza el estado del ticket en la lista');

  // Test 8: clearSession() limpia también los datos de Store
  clearSession();
  assert(Store.getTickets() === null, 'clearSession() purga la caché de tickets');
  assert(Store.getMetrics() === null, 'clearSession() purga la caché de métricas');

  return results;
}
