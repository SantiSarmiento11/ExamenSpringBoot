/**
 * TechNova IT Helpdesk - Gestión y Filtrado Avanzado de Tickets
 * Conecta los endpoints GET /api/tickets, catálogos y creación formal de incidencias.
 */

import { get, post } from './api.js';
import { ENDPOINTS } from './config.js';
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

let allCategories = [];
let allPriorities = [];
let allTickets = [];
let currentPage = 0;
const pageSize = 10;

document.addEventListener('DOMContentLoaded', async () => {
  setupModal();
  setupFilterListeners();
  setupPaginationControls();

  // 1. Carga inmediata desde el almacén de cliente (sin parpadeo)
  const cachedCats = Store.getCategories();
  const cachedPrios = Store.getPriorities();
  if (cachedCats && cachedCats.length > 0) {
    allCategories = cachedCats;
    populateFilterOptions();
    populateModalSelects();
  }
  if (cachedPrios && cachedPrios.length > 0) {
    allPriorities = cachedPrios;
    populateFilterOptions();
    populateModalSelects();
  }

  const cachedTickets = Store.getTickets();
  if (cachedTickets && cachedTickets.length > 0) {
    allTickets = cachedTickets;
    applyFiltersAndRender();
  }

  // 2. Sincronización en segundo plano con el backend
  await loadCatalogs();
  await fetchTickets();
});

/**
 * Consulta los catálogos del backend: GET /api/categories y GET /api/priorities
 */
async function loadCatalogs() {
  try {
    const [categories, priorities] = await Promise.all([
      get(ENDPOINTS.CATALOGS.CATEGORIES),
      get(ENDPOINTS.CATALOGS.PRIORITIES),
    ]);

    allCategories = Array.isArray(categories) ? categories : [];
    allPriorities = Array.isArray(priorities) ? priorities : [];

    Store.setCategories(allCategories);
    Store.setPriorities(allPriorities);

    populateFilterOptions();
    populateModalSelects();
  } catch (err) {
    console.error('Error al cargar catálogos:', err);
  }
}

/**
 * Llena las opciones de los select de filtro en la barra de herramientas.
 */
function populateFilterOptions() {
  const catFilter = document.getElementById('filter-categoria');
  if (catFilter) {
    catFilter.innerHTML = '<option value="all">Todas las categorías</option>' +
      allCategories.map((c) => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
  }

  const prioFilter = document.getElementById('filter-prioridad');
  if (prioFilter) {
    prioFilter.innerHTML = '<option value="all">Todas las prioridades</option>' +
      allPriorities.map((p) => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('');
  }
}

/**
 * Llena las opciones del modal para creación de tickets.
 */
function populateModalSelects() {
  const catSelect = document.getElementById('modal-category');
  if (catSelect && allCategories.length > 0) {
    catSelect.innerHTML = allCategories
      .map((c) => `<option value="${c.id}">${escapeHtml(c.name)} - ${escapeHtml(c.description || '')}</option>`)
      .join('');
  }

  const prioSelect = document.getElementById('modal-priority');
  if (prioSelect && allPriorities.length > 0) {
    prioSelect.innerHTML = allPriorities
      .map((p) => `<option value="${p.id}">${escapeHtml(p.name)} (SLA: ${p.slaHours || 0}h)</option>`)
      .join('');
  }
}

/**
 * Consulta la lista de tickets autorizados desde GET /api/tickets.
 */
async function fetchTickets() {
  const tableBody = document.getElementById('ticketsTableBody');
  const countText = document.getElementById('ticketCountText');

  // Solo mostrar spinner si la tabla está completamente vacía
  if (tableBody && !tableBody.querySelector('.ticket-row')) {
    tableBody.innerHTML = `
      <tr>
        <td colspan="9" style="text-align: center; padding: 2.5rem; color: var(--on-surface-variant);">
          <div style="display: flex; align-items: center; justify-content: center; gap: 0.5rem;">
            <span class="spinner spinner-primary"></span>
            <span>Sincronizando tickets desde el servidor...</span>
          </div>
        </td>
      </tr>
    `;
  }

  try {
    const data = await get(ENDPOINTS.TICKETS.BASE);
    allTickets = Array.isArray(data) ? data : [];
    Store.setTickets(allTickets);

    // Si viene un parámetro ?q= en la URL, prellenar el campo de búsqueda
    const urlParams = new URLSearchParams(window.location.search);
    const initialQuery = urlParams.get('q');
    const searchInput = document.getElementById('filter-buscar');
    if (initialQuery && searchInput) {
      searchInput.value = initialQuery;
    }

    applyFiltersAndRender();
  } catch (err) {
    console.error('Error al consultar tickets:', err);
    if (tableBody && !tableBody.querySelector('.ticket-row')) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="9" style="text-align: center; padding: 2.5rem; color: var(--error);">
            <div style="display: flex; flex-direction: column; align-items: center; gap: 0.5rem;">
              <span class="material-symbols-outlined" style="font-size: 32px;">error</span>
              <p class="label-md">No se pudieron recuperar los tickets del servidor.</p>
              <span style="font-size: 0.8rem; color: var(--on-surface-variant);">${escapeHtml(err.message || '')}</span>
            </div>
          </td>
        </tr>
      `;
    }
  }
}

/**
 * Filtra los tickets localmente en base a los criterios seleccionados y actualiza la tabla.
 */
function applyFiltersAndRender() {
  const tableBody = document.getElementById('ticketsTableBody');
  const countText = document.getElementById('ticketCountText');
  const searchInput = document.getElementById('filter-buscar');
  const estadoSelect = document.getElementById('filter-estado');
  const prioSelect = document.getElementById('filter-prioridad');
  const catSelect = document.getElementById('filter-categoria');

  if (!tableBody) return;

  const query = searchInput ? searchInput.value.toLowerCase().trim() : '';
  const estado = estadoSelect ? estadoSelect.value.toUpperCase() : 'ALL';
  const priorityId = prioSelect ? prioSelect.value : 'all';
  const categoryId = catSelect ? catSelect.value : 'all';

  const filtered = allTickets.filter((t) => {
    // Filtro por texto libre en código, título, solicitante, técnico o descripción
    const matchesQuery =
      !query ||
      (t.code && t.code.toLowerCase().includes(query)) ||
      (t.title && t.title.toLowerCase().includes(query)) ||
      (t.requesterName && t.requesterName.toLowerCase().includes(query)) ||
      (t.assignedTechnicianName && t.assignedTechnicianName.toLowerCase().includes(query)) ||
      (t.description && t.description.toLowerCase().includes(query));

    // Filtro por estado del ticket
    const matchesEstado = estado === 'ALL' || (t.statusName && t.statusName.toUpperCase() === estado);

    // Filtro por ID de prioridad
    const matchesPriority = priorityId === 'all' || (t.priorityId && String(t.priorityId) === String(priorityId));

    // Filtro por ID de categoría
    const matchesCategory = categoryId === 'all' || (t.categoryId && String(t.categoryId) === String(categoryId));

    return matchesQuery && matchesEstado && matchesPriority && matchesCategory;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  if (currentPage >= totalPages) currentPage = totalPages - 1;
  if (currentPage < 0) currentPage = 0;

  const btnPrev = document.getElementById('btnPrevPage');
  const btnNext = document.getElementById('btnNextPage');
  const pageDisplay = document.getElementById('pageNumberDisplay');

  if (btnPrev) btnPrev.disabled = currentPage === 0;
  if (btnNext) btnNext.disabled = currentPage >= totalPages - 1;
  if (pageDisplay) pageDisplay.textContent = `Página ${currentPage + 1} de ${totalPages}`;

  if (countText) {
    countText.textContent = `${filtered.length} ticket(s) encontrado(s) de un total de ${allTickets.length}`;
  }

  if (filtered.length === 0) {
    if (allTickets.length === 0) {
      tableBody.innerHTML = `
        <tr>
          <td colspan="9" style="text-align: center; padding: 3.5rem 1.5rem; color: var(--on-surface-variant);">
            <div style="display: flex; flex-direction: column; align-items: center; gap: 0.65rem;">
              <span class="material-symbols-outlined" style="font-size: 42px; color: var(--outline-variant);">confirmation_number</span>
              <p class="label-lg" style="font-size: 1.05rem; color: var(--on-surface);">No hay tickets registrados en su bandeja</p>
              <p class="body-sm" style="color: var(--on-surface-variant); max-width: 380px;">Cree una nueva incidencia técnica para iniciar la atención de soporte.</p>
              <button class="btn btn-primary btn-sm" id="btnEmptyCreateTicket" type="button" style="margin-top: 0.25rem;">
                <span class="material-symbols-outlined" style="font-size: 16px;">add</span>
                <span>Crear mi primer ticket</span>
              </button>
            </div>
          </td>
        </tr>
      `;
      const createBtn = document.getElementById('btnEmptyCreateTicket');
      if (createBtn) createBtn.addEventListener('click', () => openModal('newTicketModal'));
    } else {
      tableBody.innerHTML = `
        <tr>
          <td colspan="9" style="text-align: center; padding: 3.5rem 1.5rem; color: var(--on-surface-variant);">
            <div style="display: flex; flex-direction: column; align-items: center; gap: 0.5rem;">
              <span class="material-symbols-outlined" style="font-size: 36px; color: var(--outline-variant);">search_off</span>
              <p class="label-lg">No se encontraron tickets con los criterios de búsqueda aplicados</p>
              <button class="btn btn-secondary btn-sm" id="btnResetFiltersInline">Restablecer filtros</button>
            </div>
          </td>
        </tr>
      `;
      const resetBtn = document.getElementById('btnResetFiltersInline');
      if (resetBtn) resetBtn.addEventListener('click', resetFilters);
    }
    return;
  }

  const pagedItems = filtered.slice(currentPage * pageSize, (currentPage + 1) * pageSize);

  tableBody.innerHTML = pagedItems
    .map(
      (t) => `
      <tr class="ticket-row" onclick="window.location.href='ticket-detail.html?id=${t.id}'" style="cursor: pointer;">
        <td>
          <span style="font-weight: 700; color: var(--primary); font-family: var(--font-family); font-size: 0.95rem;">
            ${escapeHtml(t.code || `TK-${String(t.id).padStart(4, '0')}`)}
          </span>
        </td>
        <td style="max-width: 320px;">
          <div style="font-weight: 600; color: var(--on-surface); line-height: 1.35; font-size: 0.95rem;">
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
}

/**
 * Restablece los filtros de búsqueda a sus valores por defecto.
 */
function resetFilters() {
  const searchInput = document.getElementById('filter-buscar');
  const estadoSelect = document.getElementById('filter-estado');
  const prioSelect = document.getElementById('filter-prioridad');
  const catSelect = document.getElementById('filter-categoria');

  if (searchInput) searchInput.value = '';
  if (estadoSelect) estadoSelect.value = 'all';
  if (prioSelect) prioSelect.value = 'all';
  if (catSelect) catSelect.value = 'all';

  applyFiltersAndRender();
}

/**
 * Configura los event listeners para filtrado reactivo.
 */
function setupFilterListeners() {
  const searchInput = document.getElementById('filter-buscar');
  const estadoSelect = document.getElementById('filter-estado');
  const prioSelect = document.getElementById('filter-prioridad');
  const catSelect = document.getElementById('filter-categoria');
  const clearBtn = document.getElementById('clear-filters-btn');

  const onFilterChange = () => {
    currentPage = 0;
    applyFiltersAndRender();
  };

  if (searchInput) searchInput.addEventListener('input', onFilterChange);
  if (estadoSelect) estadoSelect.addEventListener('change', onFilterChange);
  if (prioSelect) prioSelect.addEventListener('change', onFilterChange);
  if (catSelect) catSelect.addEventListener('change', onFilterChange);
  if (clearBtn) clearBtn.addEventListener('click', resetFilters);
}

/**
 * Configura los botones de paginación anterior y siguiente.
 */
function setupPaginationControls() {
  const btnPrev = document.getElementById('btnPrevPage');
  const btnNext = document.getElementById('btnNextPage');

  if (btnPrev) {
    btnPrev.addEventListener('click', () => {
      if (currentPage > 0) {
        currentPage--;
        applyFiltersAndRender();
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    });
  }

  if (btnNext) {
    btnNext.addEventListener('click', () => {
      currentPage++;
      applyFiltersAndRender();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }
}

/**
 * Configura el modal de registro de tickets.
 */
function setupModal() {
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

        // Recargar tickets actualizados
        await fetchTickets();
      } catch (err) {
        showToast(err.message || 'Error al registrar el ticket en el backend.', 'error');
      } finally {
        if (submitBtn) {
          submitBtn.innerHTML = originalBtnHtml;
          submitBtn.disabled = false;
        }
      }
    });
  }
}
