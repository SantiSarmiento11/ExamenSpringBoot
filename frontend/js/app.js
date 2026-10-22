/**
 * TechNova IT Helpdesk - Inicializador Global y Navegación
 */

import { requireAuth } from './session.js';
import { syncUserInterface, showToast } from './ui.js';

document.addEventListener('DOMContentLoaded', () => {
  // Validar autenticación obligatoria para páginas internas
  const path = window.location.pathname.split('/').pop() || 'index.html';
  if (path !== 'index.html' && path !== '') {
    if (!requireAuth()) return;
  }

  // Sincronizar estado visual de navegación
  syncActiveNavigation();

  // Sincronizar datos del usuario autenticado en la interfaz
  syncUserInterface();

  // Notificaciones no intrusivas
  setupNotifications();

  // Accesibilidad y atajos de teclado globales
  setupKeyboardShortcuts();

  // Detección de conectividad de red y reconexión
  setupNetworkStatusListeners();

  // Navegación responsive para móviles
  setupMobileSidebar();
});

function setupMobileSidebar() {
  const sidebar = document.querySelector('.sidebar');
  if (!sidebar) return;

  let backdrop = document.querySelector('.sidebar-backdrop');
  if (!backdrop) {
    backdrop = document.createElement('div');
    backdrop.className = 'sidebar-backdrop';
    document.body.appendChild(backdrop);
  }

  const topbar = document.querySelector('.topbar');
  if (topbar && !document.getElementById('mobileMenuBtn')) {
    const menuBtn = document.createElement('button');
    menuBtn.id = 'mobileMenuBtn';
    menuBtn.className = 'mobile-menu-btn';
    menuBtn.type = 'button';
    menuBtn.title = 'Abrir menú de navegación';
    menuBtn.innerHTML = '<span class="material-symbols-outlined">menu</span>';
    topbar.insertBefore(menuBtn, topbar.firstChild);

    menuBtn.addEventListener('click', () => {
      const isOpen = sidebar.classList.toggle('open');
      backdrop.classList.toggle('active', isOpen);
    });
  }

  backdrop.addEventListener('click', () => {
    sidebar.classList.remove('open');
    backdrop.classList.remove('active');
  });

  sidebar.querySelectorAll('.sidebar-link').forEach((link) => {
    link.addEventListener('click', () => {
      sidebar.classList.remove('open');
      backdrop.classList.remove('active');
    });
  });
}

function setupNetworkStatusListeners() {
  window.addEventListener('offline', () => {
    showToast('Conexión con la red interrumpida. Las operaciones con el backend no estarán disponibles.', 'warning');
  });

  window.addEventListener('online', () => {
    showToast('Conexión de red restablecida exitosamente.', 'success');
  });
}

function setupKeyboardShortcuts() {
  document.addEventListener('keydown', (e) => {
    // Escape cierra cualquier modal activo
    if (e.key === 'Escape') {
      const activeModals = document.querySelectorAll('.modal-overlay.active');
      activeModals.forEach((modal) => {
        modal.classList.remove('active');
      });
    }

    // Ctrl+K o Cmd+K para enfocar el buscador principal si existe
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
      const searchInput = document.getElementById('filter-buscar') || document.querySelector('input[type="search"]');
      if (searchInput) {
        e.preventDefault();
        searchInput.focus();
        if (typeof searchInput.select === 'function') searchInput.select();
      }
    }
  });
}

function syncActiveNavigation() {
  const currentPath = window.location.pathname.split('/').pop() || 'index.html';
  const navLinks = document.querySelectorAll('.sidebar-nav .sidebar-link');

  navLinks.forEach((link) => {
    const href = link.getAttribute('href');
    if (href === currentPath || (currentPath === '' && href === 'dashboard.html')) {
      link.classList.add('active');
    } else {
      link.classList.remove('active');
    }
  });
}

function setupNotifications() {
  const notifBtn = document.getElementById('topbarNotifBtn');
  if (notifBtn) {
    notifBtn.addEventListener('click', () => {
      showToast('No hay alertas críticas pendientes en el sistema.', 'info');
      const badge = notifBtn.querySelector('.topbar-badge');
      if (badge) badge.style.display = 'none';
    });
  }
}
