/**
 * TechNova IT Helpdesk - Utilidades de Interfaz de Usuario, Notificaciones y Formateo
 */

import { getUser, clearSession, hasRole } from './session.js';

/**
 * Muestra una notificación Toast flotante no intrusiva.
 * @param {string} message - Contenido del mensaje
 * @param {'success'|'error'|'warning'|'info'} [type='info'] - Tipo visual
 * @param {number} [duration=4000] - Tiempo en ms
 */
export function showToast(message, type = 'info', duration = 4000) {
  let container = document.getElementById('technovaToastContainer');
  if (!container) {
    container = document.createElement('div');
    container.id = 'technovaToastContainer';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  const iconName =
    type === 'success'
      ? 'check_circle'
      : type === 'error'
      ? 'error'
      : type === 'warning'
      ? 'warning'
      : 'info';

  toast.innerHTML = `
    <span class="material-symbols-outlined" style="font-size: 20px;">${iconName}</span>
    <span style="flex: 1; word-break: break-word;">${escapeHtml(message)}</span>
  `;

  container.appendChild(toast);

  const removeToast = () => {
    toast.classList.add('toast-hide');
    setTimeout(() => {
      if (toast.parentElement) toast.remove();
    }, 250);
  };

  const timer = setTimeout(removeToast, duration);
  toast.addEventListener('click', () => {
    clearTimeout(timer);
    removeToast();
  });
}

/**
 * Renderiza un contenedor de alerta inline dentro de un elemento.
 * @param {HTMLElement|string} target - ID o elemento destino
 * @param {string} message - Texto del mensaje
 * @param {'error'|'success'|'warning'|'info'} [type='error']
 */
export function renderAlert(target, message, type = 'error') {
  const el = typeof target === 'string' ? document.getElementById(target) : target;
  if (!el) return;

  const iconName =
    type === 'error'
      ? 'error'
      : type === 'success'
      ? 'check_circle'
      : type === 'warning'
      ? 'warning'
      : 'info';

  el.innerHTML = `
    <div class="alert alert-${type}">
      <span class="material-symbols-outlined" style="font-size: 20px;">${iconName}</span>
      <span style="flex: 1;">${escapeHtml(message)}</span>
    </div>
  `;
}

/**
 * Limpia el contenido de un contenedor de alerta.
 * @param {HTMLElement|string} target
 */
export function clearAlert(target) {
  const el = typeof target === 'string' ? document.getElementById(target) : target;
  if (el) el.innerHTML = '';
}

/**
 * Formatea una fecha ISO (ej. 2026-10-14T10:30:00) a formato local legible (DD/MM/YYYY, HH:mm).
 * @param {string|Date} dateVal
 * @returns {string}
 */
export function formatDate(dateVal) {
  if (!dateVal) return 'Sin fecha';
  try {
    const d = new Date(dateVal);
    if (isNaN(d.getTime())) return String(dateVal);
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year}, ${hours}:${minutes}`;
  } catch {
    return String(dateVal);
  }
}

/**
 * Devuelve el HTML de badge para el estado del ticket según la guía de diseño.
 * @param {string} status
 * @returns {string}
 */
export function getStatusBadge(status) {
  if (!status) return '<span class="badge badge-cerrada">Desconocido</span>';
  const clean = status.trim().toUpperCase();
  const label = clean.replace(/_/g, ' ');

  let badgeClass = 'badge-cerrada';
  if (clean === 'ABIERTA') badgeClass = 'badge-abierta';
  else if (clean === 'ASIGNADA') badgeClass = 'badge-asignada';
  else if (clean === 'EN_PROCESO') badgeClass = 'badge-en_proceso';
  else if (clean === 'RESUELTA') badgeClass = 'badge-resuelta';
  else if (clean === 'CERRADA') badgeClass = 'badge-cerrada';

  return `<span class="badge ${badgeClass}">${label}</span>`;
}

/**
 * Devuelve el HTML de badge para la prioridad del ticket.
 * @param {string} priority
 * @returns {string}
 */
export function getPriorityBadge(priority) {
  if (!priority) return '<span class="badge badge-media">Media</span>';
  const clean = priority.trim().toUpperCase();

  let badgeClass = 'badge-media';
  if (clean === 'CRITICA' || clean === 'CRÍTICA') badgeClass = 'badge-critica';
  else if (clean === 'ALTA') badgeClass = 'badge-alta';
  else if (clean === 'MEDIA') badgeClass = 'badge-media';
  else if (clean === 'BAJA') badgeClass = 'badge-baja';

  return `<span class="badge ${badgeClass}">${clean}</span>`;
}

/**
 * Formatea y evalúa visualmente la fecha límite SLA según el estado y tiempo restante.
 * @param {string|Date} dueDate
 * @param {string} statusName
 * @returns {string} HTML formateado
 */
export function formatSla(dueDate, statusName) {
  if (!dueDate) return '<span style="color: var(--outline); font-size: 0.825rem;">Sin SLA</span>';

  const cleanStatus = (statusName || '').trim().toUpperCase();
  if (cleanStatus === 'RESUELTA' || cleanStatus === 'CERRADA') {
    return `<span style="color: #065f46; font-weight: 600; font-size: 0.825rem;">${formatDate(dueDate)}</span>`;
  }

  try {
    const dueTime = new Date(dueDate).getTime();
    const now = Date.now();
    const diffHours = (dueTime - now) / (1000 * 60 * 60);

    if (now > dueTime) {
      return `<span style="color: #b91c1c; font-weight: 700; font-size: 0.825rem; display: inline-flex; align-items: center; gap: 0.2rem;" title="Tiempo SLA superado"><span class="material-symbols-outlined" style="font-size: 14px;">warning</span> Vencido (${formatDate(dueDate)})</span>`;
    } else if (diffHours <= 2) {
      return `<span style="color: #b45309; font-weight: 700; font-size: 0.825rem; display: inline-flex; align-items: center; gap: 0.2rem;" title="Menos de 2 horas para vencer"><span class="material-symbols-outlined" style="font-size: 14px;">schedule</span> En riesgo (${formatDate(dueDate)})</span>`;
    } else {
      return `<span style="color: var(--on-surface-variant); font-size: 0.825rem;">${formatDate(dueDate)}</span>`;
    }
  } catch {
    return `<span style="font-size: 0.825rem; color: var(--on-surface-variant);">${formatDate(dueDate)}</span>`;
  }
}

/**
 * Escapa caracteres HTML para prevenir XSS.
 * @param {string} str
 * @returns {string}
 */
export function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

/**
 * Abre un modal por ID de overlay.
 * @param {string} modalId
 */
export function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.classList.add('active');
}

/**
 * Cierra un modal por ID de overlay.
 * @param {string} modalId
 */
export function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.classList.remove('active');
}

/**
 * Sincroniza la información del usuario autenticado en el sidebar y topbar,
 * gestiona botones de logout y visibilidad basada en roles.
 */
export function syncUserInterface() {
  const user = getUser();
  if (!user) return;

  // Traducir rol principal
  let roleLabel = 'Usuario';
  if (hasRole('ADMIN')) roleLabel = 'Administrador TI';
  else if (hasRole('TECHNICIAN')) roleLabel = 'Técnico de Soporte';
  else if (hasRole('USER')) roleLabel = 'Usuario Solicitante';

  const displayName = user.fullName || user.email;
  const initials = (user.firstName ? user.firstName[0] : '') + (user.lastName ? user.lastName[0] : '') || 'U';

  // Sincronizar elementos en el sidebar y topbar
  document.querySelectorAll('.sidebar-user-name').forEach((el) => {
    el.textContent = displayName;
  });

  document.querySelectorAll('.sidebar-user-role').forEach((el) => {
    el.textContent = roleLabel;
  });

  document.querySelectorAll('.sidebar-user-avatar, .topbar-profile-avatar').forEach((el) => {
    el.textContent = initials;
    if (hasRole('ADMIN')) {
      el.style.backgroundColor = 'var(--primary-container)';
      el.style.color = '#ffffff';
    } else if (hasRole('TECHNICIAN')) {
      el.style.backgroundColor = 'var(--secondary-container)';
      el.style.color = '#ffffff';
    } else {
      el.style.backgroundColor = 'var(--surface-container-high)';
      el.style.color = 'var(--on-surface)';
    }
  });

  // Ocultar enlace a "Reportes y Métricas" para usuarios que NO sean ADMIN
  const reportLink = document.querySelector('a[href="reportes.html"]');
  if (reportLink && !hasRole('ADMIN')) {
    reportLink.style.display = 'none';
  }

  // Sincronizar fecha actual en el header
  document.querySelectorAll('.current-date-text').forEach((el) => {
    const now = new Date();
    const options = { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' };
    const formatted = now.toLocaleDateString('es-ES', options);
    el.textContent = formatted.charAt(0).toUpperCase() + formatted.slice(1);
  });

  // Sincronizar logout global con diálogo modal nativo de Lumina Desk
  const handleLogout = (e) => {
    if (e && e.preventDefault) e.preventDefault();
    showLogoutModal();
  };

  window.logoutUser = handleLogout;

  const topbarProfile = document.getElementById('topbarProfile');
  if (topbarProfile) {
    topbarProfile.style.cursor = 'pointer';
    topbarProfile.addEventListener('click', handleLogout);
  }
}

/**
 * Diálogo modal de confirmación personalizado con diseño de Lumina Desk
 * Reemplaza ventanas emergentes nativas del navegador (window.confirm).
 * @param {Object} options
 */
export function showConfirmModal({
  title = '¿Confirmar acción?',
  message = '¿Está seguro de continuar?',
  icon = 'help_outline',
  iconColor = '#4f46e5',
  iconBg = 'rgba(79, 70, 229, 0.1)',
  confirmText = 'Aceptar',
  confirmColor = '#4f46e5',
  cancelText = 'Cancelar',
  onConfirm,
} = {}) {
  let modalOverlay = document.getElementById('technovaGlobalConfirmModal');
  if (!modalOverlay) {
    modalOverlay = document.createElement('div');
    modalOverlay.id = 'technovaGlobalConfirmModal';
    modalOverlay.className = 'modal-overlay';
    modalOverlay.setAttribute('role', 'dialog');
    modalOverlay.setAttribute('aria-modal', 'true');
    document.body.appendChild(modalOverlay);
  }

  modalOverlay.innerHTML = `
    <div class="modal" style="max-width: 440px; text-align: center; border-radius: var(--radius-2xl); overflow: hidden; box-shadow: var(--shadow-xl); background: var(--surface-container-lowest, #ffffff); border: 1px solid rgba(199, 196, 216, 0.45);">
      <div style="padding: 2.25rem 2rem 1.25rem; display: flex; flex-direction: column; align-items: center;">
        <div style="width: 3.75rem; height: 3.75rem; border-radius: var(--radius-full); background: ${iconBg}; color: ${iconColor}; display: flex; align-items: center; justify-content: center; margin-bottom: 1.25rem;">
          <span class="material-symbols-outlined" style="font-size: 32px;">${icon}</span>
        </div>
        <h3 class="headline-md" style="font-weight: 700; color: #0f172a; margin-bottom: 0.5rem;">${escapeHtml(title)}</h3>
        <p class="body-md" style="color: var(--on-surface-variant); line-height: 1.5; font-size: 0.9375rem;">
          ${escapeHtml(message)}
        </p>
      </div>
      <div class="modal-footer" style="justify-content: center; gap: 0.85rem; background: var(--surface-container-low); padding: 1.25rem 1.75rem; border-top: 1px solid rgba(199, 196, 216, 0.35);">
        <button type="button" class="btn btn-secondary" id="technovaConfirmCancelBtn" style="min-width: 110px; height: 2.75rem;">
          ${escapeHtml(cancelText)}
        </button>
        <button type="button" class="btn btn-primary" id="technovaConfirmAcceptBtn" style="min-width: 140px; height: 2.75rem; background-color: ${confirmColor}; border-color: ${confirmColor}; color: #ffffff;">
          <span>${escapeHtml(confirmText)}</span>
        </button>
      </div>
    </div>
  `;

  const closeDialog = () => {
    modalOverlay.classList.remove('active');
  };

  const cancelBtn = modalOverlay.querySelector('#technovaConfirmCancelBtn');
  if (cancelBtn) cancelBtn.onclick = closeDialog;

  modalOverlay.onclick = (e) => {
    if (e.target === modalOverlay) closeDialog();
  };

  const handleKeydown = (e) => {
    if (e.key === 'Escape' && modalOverlay.classList.contains('active')) {
      closeDialog();
      document.removeEventListener('keydown', handleKeydown);
    }
  };
  document.addEventListener('keydown', handleKeydown);

  const acceptBtn = modalOverlay.querySelector('#technovaConfirmAcceptBtn');
  if (acceptBtn) {
    acceptBtn.onclick = () => {
      closeDialog();
      if (typeof onConfirm === 'function') {
        onConfirm();
      }
    };
  }

  modalOverlay.classList.add('active');
}

/**
 * Muestra el diálogo modal personalizado de confirmación de cierre de sesión
 * con el diseño nativo de TechNova Lumina Desk, evitando ventanas del navegador.
 */
export function showLogoutModal() {
  showConfirmModal({
    title: '¿Cerrar Sesión?',
    message: '¿Estás seguro de que deseas salir del sistema? Para volver a gestionar o consultar incidencias deberás autenticarte nuevamente con tus credenciales corporativas.',
    icon: 'logout',
    iconColor: '#dc2626',
    iconBg: 'rgba(220, 38, 38, 0.1)',
    confirmText: 'Cerrar Sesión',
    confirmColor: '#dc2626',
    cancelText: 'Cancelar',
    onConfirm: () => {
      clearSession();
      window.location.href = 'index.html';
    },
  });
}
