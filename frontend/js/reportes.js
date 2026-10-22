/**
 * TechNova IT Helpdesk - Módulo de Reportes Ejecutivos y Métricas
 * Consume el endpoint exclusivo para ADMIN: GET /api/admin/dashboard (RF-07)
 */

import { get } from './api.js';
import { ENDPOINTS } from './config.js';
import { requireRole, hasRole } from './session.js';
import { Store } from './store.js';
import { escapeHtml, showToast } from './ui.js';

document.addEventListener('DOMContentLoaded', async () => {
  // Proteger la vista: solo usuarios con rol ADMIN tienen acceso
  if (!hasRole('ADMIN')) {
    renderAccessDenied();
    return;
  }

  setupRefreshHandler();

  // 1. Carga inmediata desde el almacén de cliente
  const cached = Store.getMetrics();
  if (cached && cached.totalTickets !== undefined) {
    renderHeaderMetrics(cached);
    renderStatusDistribution(cached);
    renderCategoryDistribution(cached);
    renderTechnicianWorkloads(cached.technicianWorkloads || []);
  }

  // 2. Consulta y sincronización en segundo plano
  await loadDashboardReport();
});

/**
 * Configura el botón de refresco de métricas ejecutivas.
 */
function setupRefreshHandler() {
  const btn = document.getElementById('btnRefreshReports');
  if (btn) {
    btn.addEventListener('click', async () => {
      btn.disabled = true;
      const icon = btn.querySelector('.material-symbols-outlined');
      if (icon) icon.classList.add('spinner-primary');
      try {
        await loadDashboardReport();
        showToast('Métricas del sistema actualizadas.', 'info', 2000);
      } catch (err) {
        showToast('Error al refrescar métricas.', 'error');
      } finally {
        btn.disabled = false;
        if (icon) icon.classList.remove('spinner-primary');
      }
    });
  }
}

/**
 * Muestra pantalla de acceso restringido para usuarios sin rol ADMIN.
 */
function renderAccessDenied() {
  const main = document.querySelector('.page-container');
  if (main) {
    main.innerHTML = `
      <div style="padding: 4rem 2rem; text-align: center; max-width: 600px; margin: 0 auto;">
        <div style="width: 4rem; height: 4rem; border-radius: var(--radius-full); background: #fee2e2; color: #b91c1c; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.5rem;">
          <span class="material-symbols-outlined" style="font-size: 32px;">lock</span>
        </div>
        <h1 class="headline-xl" style="margin-bottom: 0.75rem;">Acceso Restringido</h1>
        <p class="body-md" style="color: var(--on-surface-variant); margin-bottom: 2rem;">
          El módulo de Reportes y Métricas Operacionales está reservado exclusivamente para usuarios con rol de <strong>Administrador (ADMIN)</strong>.
        </p>
        <a href="dashboard.html" class="btn btn-primary" style="display: inline-flex;">
          <span class="material-symbols-outlined">arrow_back</span>
          <span>Regresar al Panel Principal</span>
        </a>
      </div>
    `;
  }
}

/**
 * Consulta y renderiza las métricas ejecutivas desde GET /api/admin/dashboard
 */
async function loadDashboardReport() {
  try {
    const data = await get(ENDPOINTS.ADMIN.DASHBOARD);
    if (!data) throw new Error('No se recibieron datos de métricas del servidor.');

    Store.setMetrics(data);
    renderHeaderMetrics(data);
    renderStatusDistribution(data);
    renderCategoryDistribution(data);
    renderTechnicianWorkloads(data.technicianWorkloads || []);
  } catch (err) {
    console.error('Error al cargar métricas de dashboard:', err);
    showToast(err.message || 'Error al obtener métricas del backend.', 'error');
  }
}

/**
 * Renderiza métricas clave en el encabezado.
 */
function renderHeaderMetrics(data) {
  const totalEl = document.getElementById('report-total-tickets');
  const slaEl = document.getElementById('report-sla-percentage');
  const mttrEl = document.getElementById('report-mttr');

  if (totalEl) totalEl.textContent = data.totalTickets ?? 0;
  if (slaEl) {
    const sla = data.slaCompliancePercentage != null ? Number(data.slaCompliancePercentage).toFixed(1) : '100.0';
    slaEl.textContent = `${sla}%`;
  }
  if (mttrEl) {
    const mttr = data.averageResolutionTimeHours != null ? Number(data.averageResolutionTimeHours).toFixed(1) : '0.0';
    mttrEl.textContent = `${mttr}h`;
  }
}

/**
 * Renderiza la distribución de tickets por estado.
 */
function renderStatusDistribution(data) {
  const total = data.totalTickets || 1;
  const badgeContainer = document.getElementById('statusBadgesContainer');
  const barsContainer = document.getElementById('statusBarsContainer');

  const openCount = data.openTickets || 0;
  const assignedCount = data.assignedTickets || 0;
  const inProgressCount = data.inProgressTickets || 0;
  const resolvedCount = data.resolvedTickets || 0;
  const closedCount = data.closedTickets || 0;

  if (badgeContainer) {
    badgeContainer.innerHTML = `
      <div class="badge badge-abierta">Abiertas (${openCount})</div>
      <div class="badge badge-asignada">Asignadas (${assignedCount})</div>
      <div class="badge badge-en_proceso">En Proceso (${inProgressCount})</div>
      <div class="badge badge-resuelta">Resueltas (${resolvedCount})</div>
      <div class="badge badge-cerrada">Cerradas (${closedCount})</div>
    `;
  }

  if (barsContainer) {
    const openPct = Math.round((openCount / total) * 100);
    const assignedPct = Math.round((assignedCount / total) * 100);
    const inProgPct = Math.round((inProgressCount / total) * 100);
    const resPct = Math.round((resolvedCount / total) * 100);
    const closedPct = Math.round((closedCount / total) * 100);

    barsContainer.innerHTML = `
      <div style="display: flex; flex-direction: column; gap: 1.25rem; width: 100%; padding: 1rem 0;">
        <div>
          <div style="display: flex; justify-content: space-between; font-size: 0.85rem; margin-bottom: 0.35rem;">
            <span style="font-weight: 600;">Resueltas y Cerradas</span>
            <span style="font-weight: 700; color: #065f46;">${resolvedCount + closedCount} (${resPct + closedPct}%)</span>
          </div>
          <div style="height: 10px; background: var(--surface-container); border-radius: var(--radius-full); overflow: hidden;">
            <div style="width: ${resPct + closedPct}%; height: 100%; background: #059669; border-radius: var(--radius-full); transition: width 0.5s;"></div>
          </div>
        </div>

        <div>
          <div style="display: flex; justify-content: space-between; font-size: 0.85rem; margin-bottom: 0.35rem;">
            <span style="font-weight: 600;">En Proceso / Asignadas</span>
            <span style="font-weight: 700; color: var(--primary);">${inProgressCount + assignedCount} (${inProgPct + assignedPct}%)</span>
          </div>
          <div style="height: 10px; background: var(--surface-container); border-radius: var(--radius-full); overflow: hidden;">
            <div style="width: ${inProgPct + assignedPct}%; height: 100%; background: var(--primary); border-radius: var(--radius-full); transition: width 0.5s;"></div>
          </div>
        </div>

        <div>
          <div style="display: flex; justify-content: space-between; font-size: 0.85rem; margin-bottom: 0.35rem;">
            <span style="font-weight: 600;">Abiertas (Sin Asignar)</span>
            <span style="font-weight: 700; color: #1d4ed8;">${openCount} (${openPct}%)</span>
          </div>
          <div style="height: 10px; background: var(--surface-container); border-radius: var(--radius-full); overflow: hidden;">
            <div style="width: ${openPct}%; height: 100%; background: #3b82f6; border-radius: var(--radius-full); transition: width 0.5s;"></div>
          </div>
        </div>
      </div>
    `;
  }
}

/**
 * Renderiza el gráfico de dona y la leyenda de categorías con datos del backend.
 */
function renderCategoryDistribution(data) {
  const list = data.categoryDistribution || [];
  const legendGrid = document.getElementById('categoryLegendGrid');
  const donutCenterText = document.getElementById('donutTotalCount');
  const donutSvg = document.getElementById('categoryDonutSvg');

  const total = data.totalTickets || 0;
  if (donutCenterText) donutCenterText.textContent = total;

  if (list.length === 0) {
    if (legendGrid) {
      legendGrid.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; color: var(--on-surface-variant); padding: 1rem;">
          No hay incidencias clasificadas aún.
        </div>
      `;
    }
    return;
  }

  const palette = ['#4F46E5', '#316BF3', '#005338', '#B4C5FF', '#F59E0B', '#10B981'];

  // Renderizar segmentos de la dona SVG
  if (donutSvg) {
    const circumference = 2 * Math.PI * 62; // r=62 -> ~389.5
    let currentOffset = 0;

    let circlesHtml = `<circle cx="80" cy="80" fill="transparent" r="62" stroke="#E5EEFF" stroke-width="18"></circle>`;

    list.forEach((item, index) => {
      const pct = (item.percentage || 0) / 100;
      const strokeLen = pct * circumference;
      const color = palette[index % palette.length];

      circlesHtml += `
        <circle cx="80" cy="80" fill="transparent" r="62" stroke="${color}"
          stroke-dasharray="${strokeLen.toFixed(1)} ${circumference.toFixed(1)}"
          stroke-dashoffset="${(-currentOffset).toFixed(1)}"
          stroke-width="18"></circle>
      `;
      currentOffset += strokeLen;
    });

    donutSvg.innerHTML = circlesHtml;
  }

  // Renderizar tarjetas de la leyenda
  if (legendGrid) {
    legendGrid.innerHTML = list
      .map((item, index) => {
        const color = palette[index % palette.length];
        const pct = item.percentage != null ? Number(item.percentage).toFixed(1) : '0';
        return `
        <div class="legend-card">
          <span class="legend-dot" style="background: ${color};"></span>
          <div>
            <div class="label-sm" style="color: var(--on-surface); font-weight: 600;">${escapeHtml(item.categoryName || 'GENERAL')}</div>
            <div class="code-sm" style="color: var(--on-surface-variant);">${item.count || 0} (${pct}%)</div>
          </div>
        </div>
      `;
      })
      .join('');
  }
}

/**
 * Renderiza la tabla de carga de trabajo de técnicos especialistas: technicianWorkloads
 */
function renderTechnicianWorkloads(workloads) {
  const tbody = document.getElementById('technicianWorkloadsBody');
  const countBadge = document.getElementById('techniciansCountBadge');

  if (countBadge) {
    countBadge.textContent = `${workloads.length} técnicos registrados`;
  }

  if (!tbody) return;

  if (workloads.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="4" style="text-align: center; padding: 2rem; color: var(--on-surface-variant);">
          No se registraron técnicos de soporte en el sistema.
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = workloads
    .map((tech) => {
      const active = tech.activeTickets || 0;
      const resolved = tech.resolvedTickets || 0;
      const initials = (tech.technicianName || 'T').substring(0, 2).toUpperCase();

      return `
      <tr>
        <td>
          <div style="display: flex; align-items: center; gap: 0.75rem;">
            <div class="sidebar-user-avatar" style="background: var(--secondary-fixed); color: var(--secondary); display: flex; align-items: center; justify-content: center; font-weight: 700;">
              ${escapeHtml(initials)}
            </div>
            <div>
              <div class="label-md" style="font-weight: 700;">${escapeHtml(tech.technicianName || 'Técnico')}</div>
              <div class="code-sm" style="color: var(--on-surface-variant); font-size: 0.775rem;">${escapeHtml(tech.email || '')}</div>
            </div>
          </div>
        </td>
        <td style="text-align: center; font-weight: 700; color: var(--primary);">
          ${active}
        </td>
        <td style="text-align: center; font-weight: 700; color: #065f46;">
          ${resolved}
        </td>
        <td style="text-align: center;">
          <span class="badge ${active > 3 ? 'badge-alta' : 'badge-resuelta'}">
            ${active > 3 ? 'Carga Alta' : 'Operativo'}
          </span>
        </td>
      </tr>
    `;
    })
    .join('');
}
