/**
 * TechNova IT Helpdesk - Controlador del Panel Principal (Dashboard)
 * Conecta las métricas ejecutivas, los últimos tickets activos y la actividad en tiempo real.
 */

import { get, post, ApiError } from './api.js';
import { ENDPOINTS } from './config.js';
import { getUser, hasRole } from './session.js';
import { Store } from './store.js';
import {
  formatDate,
  formatSla,
  getStatusBadge,
  getPriorityBadge,
  escapeHtml,
  openModal,
  closeModal,
  showToast,
} from './ui.js';

let availableCategories = [];
let availablePriorities = [];

document.addEventListener('DOMContentLoaded', async () => {
  setupGreeting();
  setupModalHandlers();
  setupRefreshHandler();

  // 1. Renderizado instantáneo de datos persistentes del backend (Cero parpadeo)
  const cachedCats = Store.getCategories();
  const cachedPrios = Store.getPriorities();
  if (cachedCats && cachedCats.length > 0) {
    availableCategories = cachedCats;
  }
  if (cachedPrios && cachedPrios.length > 0) {
    availablePriorities = cachedPrios;
  }
  if (availableCategories.length > 0 && availablePriorities.length > 0) {
    populateModalSelects();
  }

  const cachedMetrics = Store.getMetrics();
  if (cachedMetrics) {
    renderMetricsData(cachedMetrics);
  }

  const cachedTickets = Store.getTickets();
  if (cachedTickets && cachedTickets.length > 0) {
    renderRecentTicketsData(cachedTickets);
  }

  // 2. Sincronización en segundo plano con la API REST
  await loadCatalogs();
  await Promise.all([loadMetrics(), loadRecentTickets()]);
});

/**
 * Configura el botón de refresco manual del tablero.
 */
function setupRefreshHandler() {
  const refreshBtn = document.getElementById('btnRefreshDashboard');
  if (refreshBtn) {
    refreshBtn.addEventListener('click', async () => {
      refreshBtn.disabled = true;
      const icon = refreshBtn.querySelector('.material-symbols-outlined');
      if (icon) icon.classList.add('spinner-primary');
      try {
        await Promise.all([loadMetrics(), loadRecentTickets()]);
        showToast('Datos del tablero sincronizados con éxito.', 'info', 2000);
      } catch (err) {
        showToast('Error al refrescar datos del servidor.', 'error');
      } finally {
        refreshBtn.disabled = false;
        if (icon) icon.classList.remove('spinner-primary');
      }
    });
  }
}

/**
 * Personaliza el saludo del encabezado según el usuario autenticado.
 */
function setupGreeting() {
  const user = getUser();
  const greetingEl = document.getElementById('dashboardGreeting');
  if (greetingEl && user) {
    const name = user.firstName || user.email.split('@')[0];
    greetingEl.textContent = `Buenos días, ${name}`;
  }
}

/**
 * Carga los catálogos auxiliares de Categorías y Prioridades del Backend.
 */
async function loadCatalogs() {
  try {
    const [categories, priorities] = await Promise.all([
      get(ENDPOINTS.CATALOGS.CATEGORIES),
      get(ENDPOINTS.CATALOGS.PRIORITIES),
    ]);
    availableCategories = Array.isArray(categories) ? categories : [];
    availablePriorities = Array.isArray(priorities) ? priorities : [];
    Store.setCategories(availableCategories);
    Store.setPriorities(availablePriorities);
    populateModalSelects();
  } catch (err) {
    console.error('Error cargando catálogos:', err);
  }
}

/**
 * Llena los selects del modal de nuevo ticket con IDs reales del backend.
 */
function populateModalSelects() {
  const catSelect = document.getElementById('modal-category');
  if (catSelect && availableCategories.length > 0) {
    catSelect.innerHTML = availableCategories
      .map(
        (c) =>
          `<option value="${c.id}">${escapeHtml(c.name)} - ${escapeHtml(c.description || '')}</option>`
      )
      .join('');
  }

  const prioSelect = document.getElementById('modal-priority');
  if (prioSelect && availablePriorities.length > 0) {
    prioSelect.innerHTML = availablePriorities
      .map(
        (p) =>
          `<option value="${p.id}">${escapeHtml(p.name)} (SLA: ${p.slaHours || 0}h)</option>`
      )
      .join('');
  }
}

/**
 * Renderiza los 4 contadores de KPIs cuantitativos en el encabezado.
 */
function renderMetricsData(metrics) {
  if (!metrics) return;
  const kpiTotal = document.getElementById('kpi-total-tickets');
  const kpiOpen = document.getElementById('kpi-open-tickets');
  const kpiProcess = document.getElementById('kpi-process-tickets');
  const kpiSla = document.getElementById('kpi-sla-compliance');

  if (kpiTotal && metrics.total !== undefined) kpiTotal.textContent = metrics.total;
  if (kpiOpen && metrics.open !== undefined) kpiOpen.textContent = metrics.open;
  if (kpiProcess && metrics.inProcess !== undefined) kpiProcess.textContent = metrics.inProcess;
  if (kpiSla && metrics.sla !== undefined) kpiSla.textContent = metrics.sla;
}

/**
 * Carga métricas cuantitativas desde GET /api/admin/dashboard (si es ADMIN)
 * o consolida métricas a partir del listado de tickets propio si es USER/TECHNICIAN.
 */
async function loadMetrics() {
  try {
    let metricObj = null;

    if (hasRole('ADMIN')) {
      const data = await get(ENDPOINTS.ADMIN.DASHBOARD);
      if (data) {
        const inProcess = (data.assignedTickets || 0) + (data.inProgressTickets || 0);
        const percentage = data.slaCompliancePercentage != null
          ? Number(data.slaCompliancePercentage).toFixed(1)
          : '100.0';

        metricObj = {
          total: data.totalTickets ?? 0,
          open: data.openTickets ?? 0,
          inProcess,
          sla: `${percentage}%`,
        };
      }
    } else {
      const tickets = await get(ENDPOINTS.TICKETS.BASE);
      const list = Array.isArray(tickets) ? tickets : [];
      const total = list.length;
      const open = list.filter((t) => t.statusName === 'ABIERTA').length;
      const inProcess = list.filter((t) => t.statusName === 'ASIGNADA' || t.statusName === 'EN_PROCESO').length;
      const resolved = list.filter((t) => t.statusName === 'RESUELTA' || t.statusName === 'CERRADA').length;
      const rate = total > 0 ? ((resolved / total) * 100).toFixed(1) : '100.0';

      metricObj = {
        total,
        open,
        inProcess,
        sla: `${rate}%`,
      };
    }

    if (metricObj) {
      Store.setMetrics(metricObj);
      renderMetricsData(metricObj);
    }
  } catch (err) {
    console.error('Error cargando métricas:', err);
  }
}

/**
 * Renderiza la tabla de tickets recientes y el feed de actividad con datos activos.
 */
function renderRecentTicketsData(list) {
  const tableBody = document.getElementById('ticketsTableBody');
  const countText = document.getElementById('ticketCountText');
  const activityList = document.getElementById('activityFeedList');

  if (!tableBody) return;
  if (!Array.isArray(list)) list = [];

  if (countText) {
    countText.textContent = `Mostrando ${Math.min(list.length, 6)} de ${list.length} ticket(s) registrados`;
  }

  if (list.length === 0) {
    tableBody.innerHTML = `
      <tr>
        <td colspan="9" style="text-align: center; padding: 3rem; color: var(--on-surface-variant);">
          <div style="display: flex; flex-direction: column; align-items: center; gap: 0.5rem;">
            <span class="material-symbols-outlined" style="font-size: 36px; color: var(--outline-variant);">inbox</span>
            <p class="label-lg">No hay tickets registrados en esta vista</p>
            <button class="btn btn-primary btn-sm" id="btnEmptyNewTicket">Crear Primer Ticket</button>
          </div>
        </td>
      </tr>
    `;
    const emptyBtn = document.getElementById('btnEmptyNewTicket');
    if (emptyBtn) emptyBtn.addEventListener('click', () => openModal('newTicketModal'));
    if (activityList) {
      activityList.innerHTML = `
        <div style="padding: 1.5rem; text-align: center; color: var(--on-surface-variant); font-size: 0.85rem;">
          Sin actividad reciente reportada.
        </div>
      `;
    }
    return;
  }

  const recent = list.slice(0, 6);

  tableBody.innerHTML = recent
    .map(
      (t) => `
      <tr class="ticket-row" onclick="window.location.href='ticket-detail.html?id=${t.id}'" style="cursor: pointer;">
        <td>
          <span style="font-weight: 700; color: var(--primary); font-family: var(--font-family); font-size: 0.95rem;">
            ${escapeHtml(t.code || `TK-${String(t.id).padStart(4, '0')}`)}
          </span>
        </td>
        <td style="max-width: 320px;">
          <div style="font-weight: 600; color: var(--on-surface); line-height: 1.35; font-size: 0.95rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
            ${escapeHtml(t.title)}
          </div>
        </td>
        <td>
          <span style="font-weight: 600; font-size: 0.825rem; letter-spacing: 0.05em; color: var(--on-surface-variant);">
            ${escapeHtml(t.categoryName || 'GENERAL')}
          </span>
        </td>
        <td>
          ${getPriorityBadge(t.priorityName)}
        </td>
        <td>
          ${getStatusBadge(t.statusName)}
        </td>
        <td>
          <div style="line-height: 1.25;">
            <div style="font-weight: 600; color: var(--on-surface); font-size: 0.9rem;">
              ${escapeHtml(t.requesterName || 'Usuario')}
            </div>
            <div style="font-size: 0.775rem; color: var(--on-surface-variant);">
              ${escapeHtml(t.requesterEmail || '')}
            </div>
          </div>
        </td>
        <td>
          <div style="color: ${t.assignedTechnicianName ? 'var(--on-surface)' : 'var(--outline)'}; font-style: ${t.assignedTechnicianName ? 'normal' : 'italic'}; font-size: 0.9rem;">
            ${escapeHtml(t.assignedTechnicianName || 'Sin asignar')}
          </div>
        </td>
        <td style="white-space: nowrap; font-size: 0.825rem; color: var(--on-surface-variant);">
          ${formatSla(t.dueDate, t.statusName)}
        </td>
        <td style="white-space: nowrap; font-size: 0.825rem; color: var(--on-surface-variant);">
          ${formatDate(t.updatedAt || t.createdAt)}
        </td>
      </tr>
    `
    )
    .join('');

  if (activityList) {
    activityList.innerHTML = recent.slice(0, 4).map((t) => {
      let icon = 'add_circle';
      let iconColor = 'var(--primary)';
      let desc = `Ticket creado por <strong>${escapeHtml(t.requesterName || 'Usuario')}</strong>`;

      if (t.statusName === 'ASIGNADA') {
        icon = 'person_add';
        iconColor = 'var(--secondary)';
        desc = `Asignado a <strong>${escapeHtml(t.assignedTechnicianName || 'Técnico')}</strong>`;
      } else if (t.statusName === 'EN_PROCESO') {
        icon = 'engineering';
        iconColor = 'var(--primary-container)';
        desc = `Técnico atendiendo incidencia activa`;
      } else if (t.statusName === 'RESUELTA') {
        icon = 'check_circle';
        iconColor = 'var(--tertiary)';
        desc = `Marcado como resuelto por el técnico`;
      } else if (t.statusName === 'CERRADA') {
        icon = 'verified';
        iconColor = 'var(--outline)';
        desc = `Ticket confirmado y cerrado formalmente`;
      }

      return `
        <div class="timeline-item">
          <div class="timeline-icon" style="color: ${iconColor};">
            <span class="material-symbols-outlined" style="font-size: 16px;">${icon}</span>
          </div>
          <p class="body-sm" style="color: var(--on-surface); line-height: 1.4;">
            ${desc} en <a href="ticket-detail.html?id=${t.id}" style="color: var(--primary); font-weight: 700;">#${escapeHtml(t.code || `TK-${t.id}`)}</a>
          </p>
          <span class="code-sm" style="color: var(--on-surface-variant); font-size: 0.75rem;">
            ${formatDate(t.updatedAt || t.createdAt)}
          </span>
        </div>
      `;
    }).join('');
  }
}

/**
 * Carga los tickets recientes desde GET /api/tickets y actualiza la tabla y feed.
 */
async function loadRecentTickets() {
  const tableBody = document.getElementById('ticketsTableBody');
  if (!tableBody) return;

  // Solo mostrar spinner si la tabla está completamente vacía y no hay caché previo
  if (!tableBody.querySelector('.ticket-row')) {
    tableBody.innerHTML = `
      <tr>
        <td colspan="9" style="text-align: center; padding: 2rem; color: var(--on-surface-variant);">
          <div style="display: flex; align-items: center; justify-content: center; gap: 0.5rem;">
            <span class="spinner spinner-primary"></span>
            <span>Sincronizando tickets en tiempo real desde el backend...</span>
          </div>
        </td>
      </tr>
    `;
  }

  try {
    const tickets = await get(ENDPOINTS.TICKETS.BASE);
    const list = Array.isArray(tickets) ? tickets : [];
    Store.setTickets(list);
    renderRecentTicketsData(list);
  } catch (err) {
    console.error('Error al cargar tickets recientes:', err);
    if (!tableBody.querySelector('.ticket-row')) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="9" style="text-align: center; padding: 2rem; color: var(--error);">
            <div style="display: flex; flex-direction: column; align-items: center; gap: 0.5rem;">
              <span class="material-symbols-outlined" style="font-size: 28px;">error</span>
              <p class="label-md">Error al sincronizar con el backend de tickets.</p>
              <span style="font-size: 0.8rem; color: var(--on-surface-variant);">${escapeHtml(err.message || '')}</span>
            </div>
          </td>
        </tr>
      `;
    }
  }
}

/**
 * Controla la apertura, cierre y envío del modal para creación de nuevo ticket.
 */
function setupModalHandlers() {
  const modalId = 'newTicketModal';
  const openBtn = document.getElementById('btnNewTicket');
  const closeBtn = document.getElementById('closeModalBtn');
  const cancelBtn = document.getElementById('cancelModalBtn');
  const form = document.getElementById('newTicketForm');

  if (openBtn) openBtn.addEventListener('click', () => openModal(modalId));
  if (closeBtn) closeBtn.addEventListener('click', () => closeModal(modalId));
  if (cancelBtn) cancelBtn.addEventListener('click', () => closeModal(modalId));

  const modalOverlay = document.getElementById(modalId);
  if (modalOverlay) {
    modalOverlay.addEventListener('click', (e) => {
      if (e.target === modalOverlay) closeModal(modalId);
    });
  }

  const titleInput = document.getElementById('modal-title');
  const titleCharCounter = document.getElementById('titleCharCount');
  if (titleInput && titleCharCounter) {
    titleInput.addEventListener('input', () => {
      const len = titleInput.value.length;
      titleCharCounter.textContent = `${len} / 200`;
      if (len > 180) {
        titleCharCounter.style.color = '#b91c1c';
        titleCharCounter.style.fontWeight = '700';
      } else {
        titleCharCounter.style.color = 'var(--outline)';
        titleCharCounter.style.fontWeight = 'normal';
      }
    });
  }

  // Envío del formulario de creación de ticket mediante POST /api/tickets
  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const catSelect = document.getElementById('modal-category');
      const prioSelect = document.getElementById('modal-priority');
      const descInput = document.getElementById('modal-description');

      const title = titleInput ? titleInput.value.trim() : '';
      const categoryId = catSelect ? Number(catSelect.value) : null;
      const priorityId = prioSelect ? Number(prioSelect.value) : null;
      const description = descInput ? descInput.value.trim() : '';

      if (!title || !description || !categoryId || !priorityId) {
        showToast('Por favor complete todos los campos obligatorios.', 'warning');
        return;
      }

      if (title.length > 200) {
        showToast('El título excede el límite máximo de 200 caracteres permitido por el backend.', 'warning');
        return;
      }

      const submitBtn = form.querySelector('button[type="submit"]');
      const originalBtnHtml = submitBtn ? submitBtn.innerHTML : 'Registrar Ticket';

      try {
        if (submitBtn) {
          submitBtn.innerHTML = '<span class="spinner"></span> <span>Creando...</span>';
          submitBtn.disabled = true;
        }

        // POST /api/tickets con TicketCreateRequestDto: { title, description, categoryId, priorityId }
        const createdTicket = await post(ENDPOINTS.TICKETS.BASE, {
          title,
          description,
          categoryId,
          priorityId,
        });

        showToast(`Ticket #${createdTicket.code || createdTicket.id} registrado exitosamente.`, 'success');
        Store.setTicket(createdTicket.id, createdTicket);
        form.reset();
        if (titleCharCounter) {
          titleCharCounter.textContent = '0 / 200';
          titleCharCounter.style.color = 'var(--outline)';
        }
        populateModalSelects();
        closeModal(modalId);

        // Recargar datos actualizados
        await Promise.all([loadMetrics(), loadRecentTickets()]);
      } catch (err) {
        showToast(err.message || 'Error al registrar el ticket en el servidor.', 'error');
      } finally {
        if (submitBtn) {
          submitBtn.innerHTML = originalBtnHtml;
          submitBtn.disabled = false;
        }
      }
    });
  }
}
