/**
 * TechNova IT Helpdesk - Pruebas Unitarias para Formateadores y Utilidades UI
 */

import { formatDate, formatSla, getStatusBadge, getPriorityBadge, escapeHtml } from './ui.js';

export function runUiTests() {
  const results = [];

  const assert = (condition, description) => {
    results.push({ description, passed: Boolean(condition) });
    if (!condition) {
      console.error(`[FAIL] ${description}`);
    } else {
      console.log(`[PASS] ${description}`);
    }
  };

  // Test 1: escapeHtml
  assert(escapeHtml('<script>alert("xss")</script>') === '&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;', 'escapeHtml neutraliza etiquetas HTML peligrosas');
  assert(escapeHtml('Normal text') === 'Normal text', 'escapeHtml preserva texto normal');
  assert(escapeHtml(null) === '', 'escapeHtml maneja null');
  assert(escapeHtml(undefined) === '', 'escapeHtml maneja undefined');

  // Test 2: formatDate
  assert(formatDate(null) === 'Sin fecha', 'formatDate maneja valores nulos');
  const formatted = formatDate('2026-10-14T10:30:00');
  assert(formatted.includes('14/10/2026') || formatted.includes('14/10/2026, 10:30'), 'formatDate convierte fecha ISO a formato local');

  // Test 3: getStatusBadge
  assert(getStatusBadge('ABIERTA').includes('badge-abierta'), 'getStatusBadge asigna badge-abierta');
  assert(getStatusBadge('ASIGNADA').includes('badge-asignada'), 'getStatusBadge asigna badge-asignada');
  assert(getStatusBadge('EN_PROCESO').includes('badge-en_proceso'), 'getStatusBadge asigna badge-en_proceso');
  assert(getStatusBadge('RESUELTA').includes('badge-resuelta'), 'getStatusBadge asigna badge-resuelta');
  assert(getStatusBadge('CERRADA').includes('badge-cerrada'), 'getStatusBadge asigna badge-cerrada');

  // Test 4: getPriorityBadge
  assert(getPriorityBadge('CRITICA').includes('badge-critica'), 'getPriorityBadge asigna badge-critica');
  assert(getPriorityBadge('ALTA').includes('badge-alta'), 'getPriorityBadge asigna badge-alta');
  assert(getPriorityBadge('MEDIA').includes('badge-media'), 'getPriorityBadge asigna badge-media');
  assert(getPriorityBadge('BAJA').includes('badge-baja'), 'getPriorityBadge asigna badge-baja');

  // Test 5: formatSla
  assert(formatSla(null, 'ABIERTA').includes('Sin SLA'), 'formatSla maneja ausencia de SLA');
  assert(formatSla('2026-10-14T12:00:00', 'RESUELTA').includes('font-weight: 600'), 'formatSla considera cumplido un ticket resuelto');
  assert(formatSla('2020-01-01T00:00:00', 'ABIERTA').includes('Vencido'), 'formatSla detecta SLA vencido');

  return results;
}
