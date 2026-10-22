/**
 * TechNova IT Helpdesk - Detalle de Ticket, Ciclo de Vida RBAC y Comentarios
 * Conecta los endpoints:
 * - GET /api/tickets/{id}
 * - PUT /api/tickets/{id}/assign (ADMIN)
 * - PUT /api/tickets/{id}/status (Máquina de estados)
 * - GET & POST /api/tickets/{id}/comments
 * - GET /api/tickets/{id}/history
 * - GET /api/users/technicians
 */

import { get, post, put, ApiError } from './api.js';
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
  showConfirmModal,
} from './ui.js';

let currentTicket = null;
let ticketId = null;

document.addEventListener('DOMContentLoaded', async () => {
  const urlParams = new URLSearchParams(window.location.search);
  const paramId = urlParams.get('id');

  if (!paramId) {
    showToast('Identificador de ticket no proporcionado.', 'error');
    setTimeout(() => (window.location.href = 'tickets.html'), 1500);
    return;
  }

  ticketId = paramId;

  setupModals();
  setupCommentSubmission();
  setupAuditButton();
  setupRefreshButton();

  // 1. Carga inmediata desde el almacén de cliente si ya fue consultado
  const cached = Store.getTicket(ticketId);
  if (cached) {
    currentTicket = cached;
    renderTicketData(currentTicket);
    updateStepper(currentTicket.statusName);
    renderLifecycleActions(currentTicket);
  }

  // 2. Consulta y sincronización en segundo plano con la API
  await loadTicketDetails();
  await Promise.all([loadComments(), loadHistory(), loadCsatRating()]);
});

/**
 * Consulta el detalle completo del ticket: GET /api/tickets/{id}
 */
async function loadTicketDetails() {
  try {
    let numericId = Number(ticketId);

    // Si el parámetro no es un número puro (ej. TK-0001), buscar por código en la API
    if (isNaN(numericId) || !Number.isInteger(numericId)) {
      const searchResults = await get(`${ENDPOINTS.TICKETS.BASE}?query=${encodeURIComponent(ticketId)}`);
      const matched = Array.isArray(searchResults)
        ? searchResults.find((t) => t.code && t.code.toUpperCase() === ticketId.toUpperCase()) || searchResults[0]
        : null;

      if (!matched || !matched.id) {
        throw new Error(`No se encontró ningún ticket con el código "${ticketId}".`);
      }
      numericId = matched.id;
      ticketId = String(numericId);
    }

    const data = await get(ENDPOINTS.TICKETS.BY_ID(numericId));
    if (!data) throw new Error('El ticket solicitado no fue encontrado.');

    currentTicket = data;
    Store.setTicket(numericId, data);
    renderTicketData(currentTicket);
    updateStepper(currentTicket.statusName);
    renderLifecycleActions(currentTicket);
  } catch (err) {
    console.error('Error al cargar detalle del ticket:', err);
    showToast(err.message || 'No se pudo cargar la información del ticket.', 'error');
    const titleEl = document.getElementById('ticket-title');
    if (titleEl) titleEl.textContent = 'Ticket no encontrado o acceso restringido';
  }
}

/**
 * Rellena los elementos de la interfaz con los datos del ticket.
 */
function renderTicketData(ticket) {
  const elBreadcrumbId = document.getElementById('breadcrumb-id');
  const elTitle = document.getElementById('ticket-title');
  const elDescription = document.getElementById('ticket-description');
  const elPriority = document.getElementById('ticket-priority');
  const elStatus = document.getElementById('ticket-status');

  const elCode = document.getElementById('ticket-code');
  const elRequester = document.getElementById('ticket-requester');
  const elEmail = document.getElementById('ticket-email');
  const elTechnician = document.getElementById('ticket-technician');
  const elCategory = document.getElementById('ticket-category');
  const elSla = document.getElementById('ticket-sla');
  const elCreated = document.getElementById('ticket-created');
  const elUpdated = document.getElementById('ticket-updated');

  const displayCode = ticket.code || `TK-${String(ticket.id).padStart(4, '0')}`;

  if (elBreadcrumbId) elBreadcrumbId.textContent = displayCode;
  if (elCode) elCode.textContent = displayCode;
  if (elTitle) elTitle.textContent = ticket.title;
  if (elDescription) elDescription.textContent = ticket.description;

  if (elPriority) {
    elPriority.innerHTML = getPriorityBadge(ticket.priorityName);
  }

  if (elStatus) {
    elStatus.innerHTML = getStatusBadge(ticket.statusName);
  }

  if (elRequester) elRequester.textContent = ticket.requesterName || 'Usuario';
  if (elEmail) elEmail.textContent = ticket.requesterEmail || '';
  if (elTechnician) {
    elTechnician.textContent = ticket.assignedTechnicianName || 'Pendiente de asignación';
    elTechnician.style.fontStyle = ticket.assignedTechnicianName ? 'normal' : 'italic';
  }

  if (elCategory) elCategory.textContent = ticket.categoryName || 'GENERAL';
  if (elSla) elSla.innerHTML = formatSla(ticket.dueDate, ticket.statusName);
  if (elCreated) elCreated.textContent = formatDate(ticket.createdAt);
  if (elUpdated) elUpdated.textContent = formatDate(ticket.updatedAt || ticket.createdAt);
}

/**
 * Actualiza la barra de progreso (Stepper) del ciclo de vida estricto:
 * 1. ABIERTA -> 2. ASIGNADA -> 3. EN_PROCESO -> 4. RESUELTA -> 5. CERRADA
 */
function updateStepper(statusName) {
  const steps = ['ABIERTA', 'ASIGNADA', 'EN_PROCESO', 'RESUELTA', 'CERRADA'];
  const nodes = document.querySelectorAll('.stepper-node');
  const progressLine = document.querySelector('.stepper-progress');

  const clean = (statusName || '').trim().toUpperCase();
  let activeIndex = steps.indexOf(clean);
  if (activeIndex === -1) {
    // Si viene con variante como EN-PROGRESO
    if (clean.includes('PROCES') || clean.includes('PROGRES')) activeIndex = 2;
    else activeIndex = 0;
  }

  nodes.forEach((node, idx) => {
    node.classList.remove('active', 'completed');
    if (idx < activeIndex) {
      node.classList.add('completed');
    } else if (idx === activeIndex) {
      node.classList.add('active');
    }
  });

  if (progressLine) {
    const percentage = (activeIndex / (steps.length - 1)) * 100;
    progressLine.style.width = `${percentage}%`;
  }
}

/**
 * Genera dinámicamente las acciones del ciclo de vida permitidas según el rol y estado:
 * - ABIERTA: ADMIN puede asignar técnico.
 * - ASIGNADA: Técnico asignado (o ADMIN) puede pasar a EN_PROCESO.
 * - EN_PROCESO: Técnico asignado (o ADMIN) puede marcar como RESUELTA.
 * - RESUELTA: Solicitante (o ADMIN) puede pasar a CERRADA.
 * - CERRADA: Estado final inmutable.
 */
function renderLifecycleActions(ticket) {
  const container = document.getElementById('lifecycleActionsContainer');
  if (!container) return;

  const user = getUser();
  const status = (ticket.statusName || '').trim().toUpperCase();
  const isAdmin = hasRole('ADMIN');
  const isTechnician = hasRole('TECHNICIAN');
  const isAssignedTech = ticket.assignedTechnicianId && user && ticket.assignedTechnicianId === user.id;
  const isRequester = ticket.requesterId && user && ticket.requesterId === user.id;

  let actionsHtml = '';

  if (status === 'ABIERTA') {
    if (isAdmin) {
      actionsHtml = `
        <button class="btn btn-primary btn-full" id="btnOpenAssignModal" title="Vincular un técnico disponible (Avanza estado a ASIGNADA)">
          <span class="material-symbols-outlined">person_add</span>
          <span>Asignar Técnico Especialista (ADMIN)</span>
        </button>
      `;
    } else {
      actionsHtml = `
        <div style="padding: 0.85rem; background: var(--surface-container-low); border-radius: var(--radius-lg); text-align: center; color: var(--on-surface-variant); font-size: 0.875rem;">
          <span class="material-symbols-outlined" style="vertical-align: middle; margin-right: 0.25rem;">hourglass_top</span>
          Pendiente de asignación por un Administrador TI
        </div>
      `;
    }
  } else if (status === 'ASIGNADA') {
    if (isAssignedTech || isAdmin) {
      actionsHtml = `
        <button class="btn btn-primary btn-full" id="btnStartProgress" title="Registra el inicio de atención activa (Avanza estado a EN_PROCESO)">
          <span class="material-symbols-outlined">play_arrow</span>
          <span>Iniciar Atención Técnica (EN PROCESO)</span>
        </button>
      `;
    } else {
      actionsHtml = `
        <div style="padding: 0.85rem; background: var(--surface-container-low); border-radius: var(--radius-lg); text-align: center; color: var(--on-surface-variant); font-size: 0.875rem;">
          <span class="material-symbols-outlined" style="vertical-align: middle; margin-right: 0.25rem;">engineering</span>
          Asignado a: <strong>${escapeHtml(ticket.assignedTechnicianName || 'Técnico')}</strong>
        </div>
      `;
    }
  } else if (status === 'EN_PROCESO') {
    if (isAssignedTech || isAdmin) {
      actionsHtml = `
        <button class="btn btn-primary btn-full" id="btnMarkResolved" style="background-color: var(--tertiary);" title="Registra la solución técnica del incidente (Avanza estado a RESUELTA)">
          <span class="material-symbols-outlined">check_circle</span>
          <span>Solucionar y Marcar como Resuelta</span>
        </button>
      `;
    } else {
      actionsHtml = `
        <div style="padding: 0.85rem; background: var(--surface-container-low); border-radius: var(--radius-lg); text-align: center; color: var(--on-surface-variant); font-size: 0.875rem;">
          <span class="material-symbols-outlined" style="vertical-align: middle; margin-right: 0.25rem;">build</span>
          Técnico trabajando en la resolución del incidente
        </div>
      `;
    }
  } else if (status === 'RESUELTA') {
    if (isRequester || isAdmin) {
      actionsHtml = `
        <button class="btn btn-primary btn-full" id="btnCloseTicket" style="background-color: #0f172a;" title="Confirma la conformidad del servicio y pasa a estado final (CERRADA)">
          <span class="material-symbols-outlined">verified</span>
          <span>Confirmar Conformidad y Cerrar Ticket</span>
        </button>
      `;
    } else {
      actionsHtml = `
        <div style="padding: 0.85rem; background: var(--surface-container-low); border-radius: var(--radius-lg); text-align: center; color: var(--on-surface-variant); font-size: 0.875rem;">
          <span class="material-symbols-outlined" style="vertical-align: middle; margin-right: 0.25rem;">done_all</span>
          Incidencia resuelta. Esperando confirmación del solicitante para cierre.
        </div>
      `;
    }
  } else if (status === 'CERRADA') {
    actionsHtml = `
      <div style="padding: 0.85rem; background: var(--surface-container); border-radius: var(--radius-lg); text-align: center; color: var(--outline); font-weight: 600; font-size: 0.875rem;">
        <span class="material-symbols-outlined" style="vertical-align: middle; margin-right: 0.25rem;">lock</span>
        Ticket cerrado formalmente (Estado final inmutable)
      </div>
    `;
  }

  container.innerHTML = actionsHtml;
  attachLifecycleActionListeners();
}

/**
 * Vincula los eventos a los botones de acción del ciclo de vida.
 */
function attachLifecycleActionListeners() {
  const btnOpenAssign = document.getElementById('btnOpenAssignModal');
  const btnStart = document.getElementById('btnStartProgress');
  const btnResolve = document.getElementById('btnMarkResolved');
  const btnClose = document.getElementById('btnCloseTicket');

  if (btnOpenAssign) {
    btnOpenAssign.addEventListener('click', async () => {
      await loadTechniciansSelect();
      openModal('assignTechModal');
    });
  }

  if (btnStart) {
    btnStart.addEventListener('click', async () => {
      await executeStatusTransition('EN_PROCESO', 'Iniciando atención técnica...');
    });
  }

  if (btnResolve) {
    btnResolve.addEventListener('click', async () => {
      await executeStatusTransition('RESUELTA', 'Marcando como resuelto...');
    });
  }

  if (btnClose) {
    btnClose.addEventListener('click', () => {
      showConfirmModal({
        title: '¿Confirmar Cierre de Incidencia?',
        message: '¿Confirma que la incidencia ha sido resuelta satisfactoriamente y desea cerrar el ticket formalmente?',
        icon: 'task_alt',
        iconColor: '#16a34a',
        iconBg: 'rgba(22, 163, 74, 0.1)',
        confirmText: 'Cerrar Ticket',
        confirmColor: '#16a34a',
        cancelText: 'Cancelar',
        onConfirm: async () => {
          await executeStatusTransition('CERRADA', 'Cerrando ticket formalmente...');
        },
      });
    });
  }
}

/**
 * Ejecuta una transición formal de estado llamando a PUT /api/tickets/{id}/status.
 */
async function executeStatusTransition(newStatus, actionLabel) {
  try {
    showToast(actionLabel, 'info', 2000);
    const updated = await put(ENDPOINTS.TICKETS.STATUS(ticketId), {
      status: newStatus,
    });

    currentTicket = updated;
    Store.setTicket(ticketId, updated);
    showToast(`Estado actualizado a ${newStatus} exitosamente.`, 'success');
    renderTicketData(currentTicket);
    updateStepper(currentTicket.statusName);
    renderLifecycleActions(currentTicket);
    await Promise.all([loadHistory(), loadComments()]);
  } catch (err) {
    showToast(err.message || 'Error al ejecutar la transición de estado.', 'error');
  }
}

/**
 * Carga técnicos disponibles desde GET /api/users/technicians para el modal de asignación.
 */
async function loadTechniciansSelect() {
  const select = document.getElementById('assign-technician-select');
  if (!select) return;

  const cachedTechs = Store.getTechnicians();
  if (cachedTechs && cachedTechs.length > 0) {
    select.innerHTML = '<option value="">Seleccione un técnico disponible...</option>' +
      cachedTechs.map((t) => `<option value="${t.id}">${escapeHtml(t.fullName || t.email)} (${escapeHtml(t.email)})</option>`).join('');
  } else {
    select.innerHTML = '<option value="">Cargando técnicos disponibles...</option>';
  }

  try {
    const techs = await get(ENDPOINTS.CATALOGS.TECHNICIANS);
    const list = Array.isArray(techs) ? techs : [];
    Store.setTechnicians(list);

    if (list.length === 0) {
      select.innerHTML = '<option value="">No hay técnicos activos registrados</option>';
      return;
    }

    select.innerHTML =
      '<option value="">Seleccione un especialista...</option>' +
      list
        .map(
          (t) =>
            `<option value="${t.id}">${escapeHtml(t.firstName || '')} ${escapeHtml(t.lastName || '')} (${escapeHtml(t.email)}) - Carga activa: ${t.activeTicketsCount ?? 0}</option>`
        )
        .join('');
  } catch (err) {
    console.error('Error al cargar técnicos:', err);
    select.innerHTML = '<option value="">Error cargando técnicos</option>';
  }
}

/**
 * Consulta y renderiza comentarios: GET /api/tickets/{ticketId}/comments
 */
async function loadComments() {
  const listEl = document.getElementById('commentsList');
  const emptyState = document.getElementById('emptyCommentsState');
  const countSpan = document.getElementById('commentsCount');

  try {
    const comments = await get(ENDPOINTS.TICKETS.COMMENTS(ticketId));
    const list = Array.isArray(comments) ? comments : [];

    if (countSpan) countSpan.textContent = `(${list.length})`;

    if (list.length === 0) {
      if (emptyState) emptyState.style.display = 'flex';
      if (listEl) listEl.innerHTML = '';
      return;
    }

    if (emptyState) emptyState.style.display = 'none';

    if (listEl) {
      listEl.innerHTML = list
        .map(
          (c) => `
        <div class="card" style="padding: 1.15rem 1.4rem; background: var(--surface-container-low); border-radius: var(--radius-xl); margin-bottom: 0.85rem;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
            <div style="display: flex; align-items: center; gap: 0.5rem;">
              <span class="material-symbols-outlined" style="font-size: 18px; color: var(--primary);">person</span>
              <span style="font-weight: 700; font-size: 0.9rem; color: var(--on-surface);">
                ${escapeHtml(c.userName || 'Usuario')}
              </span>
              <span style="font-size: 0.775rem; color: var(--on-surface-variant);">
                (${escapeHtml(c.userEmail || '')})
              </span>
            </div>
            <span style="font-size: 0.775rem; color: var(--on-surface-variant);">
              ${formatDate(c.createdAt)}
            </span>
          </div>
          <p style="font-size: 0.925rem; color: var(--on-surface); line-height: 1.5; white-space: pre-wrap; margin: 0;">
            ${escapeHtml(c.content)}
          </p>
        </div>
      `
        )
        .join('');
    }
  } catch (err) {
    console.error('Error al cargar comentarios:', err);
  }
}

/**
 * Configura el envío de nuevos comentarios mediante POST /api/tickets/{ticketId}/comments
 */
function setupCommentSubmission() {
  const input = document.getElementById('comment-input');
  const submitBtn = document.getElementById('submitCommentBtn');
  const charCounter = document.getElementById('commentCharCount');

  if (input && charCounter) {
    input.addEventListener('input', () => {
      const len = input.value.length;
      charCounter.textContent = `${len} / 2000`;
      if (len > 1900) {
        charCounter.style.color = '#b91c1c';
        charCounter.style.fontWeight = '700';
      } else {
        charCounter.style.color = 'var(--outline)';
        charCounter.style.fontWeight = 'normal';
      }
    });
  }

  if (submitBtn && input) {
    submitBtn.addEventListener('click', async () => {
      const content = input.value.trim();
      if (!content) {
        showToast('El comentario no puede estar vacío.', 'warning');
        return;
      }

      if (content.length > 2000) {
        showToast('El comentario excede el límite máximo de 2000 caracteres permitido por el servidor.', 'warning');
        return;
      }

      const originalHtml = submitBtn.innerHTML;
      try {
        submitBtn.innerHTML = '<span class="spinner"></span> <span>Publicando...</span>';
        submitBtn.disabled = true;

        await post(ENDPOINTS.TICKETS.COMMENTS(ticketId), { content });

        input.value = '';
        if (charCounter) {
          charCounter.textContent = '0 / 2000';
          charCounter.style.color = 'var(--outline)';
          charCounter.style.fontWeight = 'normal';
        }
        showToast('Comentario publicado correctamente.', 'success');
        await loadComments();
      } catch (err) {
        showToast(err.message || 'Error al publicar comentario.', 'error');
      } finally {
        submitBtn.innerHTML = originalHtml;
        submitBtn.disabled = false;
      }
    });
  }
}

/**
 * Consulta y renderiza la bitácora histórica de auditoría: GET /api/tickets/{id}/history
 */
async function loadHistory() {
  const container = document.getElementById('auditTimelineContent');
  if (!container) return;

  try {
    const history = await get(ENDPOINTS.TICKETS.HISTORY(ticketId));
    const list = Array.isArray(history) ? history : [];

    if (list.length === 0) {
      container.innerHTML = `
        <div style="padding: 1.5rem; text-align: center; color: var(--on-surface-variant); font-size: 0.875rem;">
          No hay registros de transiciones en el historial de este ticket.
        </div>
      `;
      return;
    }

    container.innerHTML = list
      .map(
        (h) => `
        <div style="padding: 0.85rem 1rem; border-left: 3px solid var(--primary); background: var(--surface-container-low); border-radius: 0 var(--radius-lg) var(--radius-lg) 0; margin-bottom: 0.75rem;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.35rem;">
            <div style="display: flex; align-items: center; gap: 0.35rem;">
              <span class="badge badge-cerrada">${escapeHtml(h.previousStatusName || 'INICIO')}</span>
              <span class="material-symbols-outlined" style="font-size: 14px;">arrow_forward</span>
              ${getStatusBadge(h.newStatusName)}
            </div>
            <span style="font-size: 0.75rem; color: var(--on-surface-variant);">${formatDate(h.changedAt)}</span>
          </div>
          <div style="font-size: 0.825rem; color: var(--on-surface);">
            Por: <strong>${escapeHtml(h.changedByName || 'Sistema')}</strong> <span style="color: var(--on-surface-variant);">(${escapeHtml(h.changedByEmail || '')})</span>
          </div>
        </div>
      `
      )
      .join('');
  } catch (err) {
    console.error('Error al cargar historial:', err);
    container.innerHTML = `
      <div style="padding: 1rem; color: var(--error); font-size: 0.85rem;">
        Error al cargar la bitácora de auditoría.
      </div>
    `;
  }
}

/**
 * Configura los modales de Asignación de Técnico y de Auditoría.
 */
function setupModals() {
  // Modal Asignar Técnico
  const assignModalId = 'assignTechModal';
  const closeAssignBtn = document.getElementById('closeAssignModalBtn');
  const cancelAssignBtn = document.getElementById('cancelAssignBtn');
  const assignForm = document.getElementById('assignTechForm');

  if (closeAssignBtn) closeAssignBtn.addEventListener('click', () => closeModal(assignModalId));
  if (cancelAssignBtn) cancelAssignBtn.addEventListener('click', () => closeModal(assignModalId));

  const assignModalOverlay = document.getElementById(assignModalId);
  if (assignModalOverlay) {
    assignModalOverlay.addEventListener('click', (e) => {
      if (e.target === assignModalOverlay) closeModal(assignModalId);
    });
  }

  if (assignForm) {
    assignForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const techSelect = document.getElementById('assign-technician-select');
      const technicianId = techSelect ? Number(techSelect.value) : null;

      if (!technicianId) {
        showToast('Seleccione un técnico válido.', 'warning');
        return;
      }

      const submitBtn = assignForm.querySelector('button[type="submit"]');
      const originalHtml = submitBtn ? submitBtn.innerHTML : 'Asignar';

      try {
        if (submitBtn) {
          submitBtn.innerHTML = '<span class="spinner"></span> <span>Asignando...</span>';
          submitBtn.disabled = true;
        }

        // PUT /api/tickets/{id}/assign con TicketAssignRequestDto: { technicianId }
        const updated = await put(ENDPOINTS.TICKETS.ASSIGN(ticketId), { technicianId });

        currentTicket = updated;
        Store.setTicket(ticketId, updated);
        showToast(`Ticket asignado exitosamente a ${updated.assignedTechnicianName}.`, 'success');
        closeModal(assignModalId);

        renderTicketData(currentTicket);
        updateStepper(currentTicket.statusName);
        renderLifecycleActions(currentTicket);
        await Promise.all([loadHistory(), loadComments()]);
      } catch (err) {
        showToast(err.message || 'Error al asignar el técnico.', 'error');
      } finally {
        if (submitBtn) {
          submitBtn.innerHTML = originalHtml;
          submitBtn.disabled = false;
        }
      }
    });
  }

  // Modal Historial de Auditoría
  const historyModalId = 'auditHistoryModal';
  const closeHistoryBtn = document.getElementById('closeAuditModalBtn');
  if (closeHistoryBtn) closeHistoryBtn.addEventListener('click', () => closeModal(historyModalId));

  const historyModalOverlay = document.getElementById(historyModalId);
  if (historyModalOverlay) {
    historyModalOverlay.addEventListener('click', (e) => {
      if (e.target === historyModalOverlay) closeModal(historyModalId);
    });
  }
}

/**
 * Configura el botón para abrir el modal de trazabilidad de auditoría.
 */
function setupAuditButton() {
  const btnAudit = document.getElementById('btnOpenAuditModal');
  if (btnAudit) {
    btnAudit.addEventListener('click', () => {
      openModal('auditHistoryModal');
    });
  }
}

/**
 * Configura el botón de refresco manual del ticket y sus dependencias.
 */
function setupRefreshButton() {
  const btnRefresh = document.getElementById('btnRefreshTicketDetail');
  if (btnRefresh) {
    btnRefresh.addEventListener('click', async () => {
      const originalHtml = btnRefresh.innerHTML;
      try {
        btnRefresh.disabled = true;
        btnRefresh.innerHTML = '<span class="material-symbols-outlined spin">sync</span> <span>Sincronizando...</span>';
        await loadTicketDetails();
        await Promise.all([loadComments(), loadHistory()]);
        showToast('Ticket sincronizado correctamente con el servidor.', 'info');
      } catch (err) {
        showToast('Error al refrescar ticket: ' + (err.message || ''), 'error');
      } finally {
        btnRefresh.disabled = false;
        btnRefresh.innerHTML = originalHtml;
      }
    });
  }
}

let selectedCsatScore = 5;

/**
 * Consulta y renderiza la encuesta de satisfacción del usuario (CSAT).
 */
async function loadCsatRating() {
  const card = document.getElementById('csatCard');
  const ratedView = document.getElementById('csatRatedView');
  const rateForm = document.getElementById('csatRateForm');
  const starsDisplay = document.getElementById('csatStarsDisplay');
  const commentDisplay = document.getElementById('csatCommentDisplay');
  if (!card) return;

  const isClosed = currentTicket && currentTicket.statusName === 'CERRADA';
  if (!isClosed) {
    card.style.display = 'none';
    return;
  }

  card.style.display = 'block';

  try {
    const rating = await get(`/api/tickets/${ticketId}/calificacion`);
    if (rating && rating.puntuacion) {
      if (rateForm) rateForm.style.display = 'none';
      if (ratedView) ratedView.style.display = 'block';
      if (starsDisplay) starsDisplay.textContent = '★'.repeat(rating.puntuacion) + '☆'.repeat(5 - rating.puntuacion);
      if (commentDisplay) commentDisplay.textContent = rating.comentario ? `"${escapeHtml(rating.comentario)}"` : 'Sin comentario adicional.';
      return;
    }
  } catch {
    // Si no ha sido calificado
  }

  const user = getUser();
  const isRequester = currentTicket && currentTicket.requesterId && user && currentTicket.requesterId === user.id;

  if (isRequester) {
    if (ratedView) ratedView.style.display = 'none';
    if (rateForm) rateForm.style.display = 'block';

    const starBtns = document.querySelectorAll('.star-btn');
    starBtns.forEach(b => {
      if (!b.dataset.bound) {
        b.dataset.bound = 'true';
        b.addEventListener('click', () => {
          selectedCsatScore = parseInt(b.getAttribute('data-score'), 10);
          starBtns.forEach(other => {
            const sc = parseInt(other.getAttribute('data-score'), 10);
            if (sc <= selectedCsatScore) {
              other.style.background = 'var(--primary)';
              other.style.color = 'white';
            } else {
              other.style.background = 'var(--surface-container-high)';
              other.style.color = 'var(--on-surface)';
            }
          });
        });
      }
    });

    const submitBtn = document.getElementById('btnSubmitCsat');
    if (submitBtn && !submitBtn.dataset.bound) {
      submitBtn.dataset.bound = 'true';
      submitBtn.addEventListener('click', async () => {
        const comment = (document.getElementById('csatCommentInput')?.value || '').trim();
        try {
          submitBtn.disabled = true;
          await post(`/api/tickets/${ticketId}/calificacion`, {
            puntuacion: selectedCsatScore,
            comentario: comment || null
          });
          showToast('¡Gracias por su calificación del servicio!', 'success');
          await loadCsatRating();
        } catch (err) {
          showToast(err.message || 'Error al enviar calificación.', 'error');
        } finally {
          submitBtn.disabled = false;
        }
      });
    }
  } else {
    card.style.display = 'none';
  }
}

