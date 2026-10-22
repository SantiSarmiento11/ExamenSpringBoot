/**
 * TechNova IT Helpdesk - Gestor de Caché y Estado Persistente en Cliente
 * Almacena los datos reales del backend en sessionStorage para que permanezcan
 * activos y disponibles inmediatamente entre transiciones de página, eliminando
 * recargas en blanco y llamadas redundantes al servidor.
 */

const CACHE_KEYS = {
  TICKETS: 'technova_cache_tickets',
  METRICS: 'technova_cache_metrics',
  CATEGORIES: 'technova_cache_categories',
  PRIORITIES: 'technova_cache_priorities',
  TECHNICIANS: 'technova_cache_technicians',
  TICKET_PREFIX: 'technova_cache_ticket_',
};

export const Store = {
  /**
   * Obtiene la lista de tickets en caché.
   * @returns {Array|null}
   */
  getTickets() {
    try {
      const data = sessionStorage.getItem(CACHE_KEYS.TICKETS);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  },

  /**
   * Guarda la lista de tickets en caché.
   * @param {Array} tickets
   */
  setTickets(tickets) {
    try {
      if (Array.isArray(tickets)) {
        sessionStorage.setItem(CACHE_KEYS.TICKETS, JSON.stringify(tickets));
      }
    } catch (e) {
      console.warn('Error al guardar tickets en caché:', e);
    }
  },

  /**
   * Obtiene las métricas del dashboard en caché.
   * @returns {Object|null}
   */
  getMetrics() {
    try {
      const data = sessionStorage.getItem(CACHE_KEYS.METRICS);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  },

  /**
   * Guarda las métricas del dashboard en caché.
   * @param {Object} metrics
   */
  setMetrics(metrics) {
    try {
      if (metrics && typeof metrics === 'object') {
        sessionStorage.setItem(CACHE_KEYS.METRICS, JSON.stringify(metrics));
      }
    } catch (e) {
      console.warn('Error al guardar métricas en caché:', e);
    }
  },

  /**
   * Obtiene el catálogo de categorías en caché.
   * @returns {Array|null}
   */
  getCategories() {
    try {
      const data = sessionStorage.getItem(CACHE_KEYS.CATEGORIES);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  },

  /**
   * Guarda el catálogo de categorías en caché.
   * @param {Array} cats
   */
  setCategories(cats) {
    try {
      if (Array.isArray(cats)) {
        sessionStorage.setItem(CACHE_KEYS.CATEGORIES, JSON.stringify(cats));
      }
    } catch {}
  },

  /**
   * Obtiene el catálogo de prioridades en caché.
   * @returns {Array|null}
   */
  getPriorities() {
    try {
      const data = sessionStorage.getItem(CACHE_KEYS.PRIORITIES);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  },

  /**
   * Guarda el catálogo de prioridades en caché.
   * @param {Array} prios
   */
  setPriorities(prios) {
    try {
      if (Array.isArray(prios)) {
        sessionStorage.setItem(CACHE_KEYS.PRIORITIES, JSON.stringify(prios));
      }
    } catch {}
  },

  /**
   * Obtiene la lista de técnicos en caché.
   * @returns {Array|null}
   */
  getTechnicians() {
    try {
      const data = sessionStorage.getItem(CACHE_KEYS.TECHNICIANS);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  },

  /**
   * Guarda la lista de técnicos en caché.
   * @param {Array} techs
   */
  setTechnicians(techs) {
    try {
      if (Array.isArray(techs)) {
        sessionStorage.setItem(CACHE_KEYS.TECHNICIANS, JSON.stringify(techs));
      }
    } catch {}
  },

  /**
   * Obtiene el detalle de un ticket específico en caché.
   * @param {string|number} id
   * @returns {Object|null}
   */
  getTicket(id) {
    try {
      const direct = sessionStorage.getItem(CACHE_KEYS.TICKET_PREFIX + id);
      if (direct) return JSON.parse(direct);

      // Búsqueda en la lista general en caché
      const list = this.getTickets();
      if (Array.isArray(list)) {
        const found = list.find(
          (t) =>
            t.id === Number(id) ||
            (t.code && t.code.toUpperCase() === String(id).toUpperCase())
        );
        if (found) return found;
      }
      return null;
    } catch {
      return null;
    }
  },

  /**
   * Guarda el detalle de un ticket en caché y lo sincroniza con la lista general.
   * @param {string|number} id
   * @param {Object} ticket
   */
  setTicket(id, ticket) {
    try {
      if (ticket && typeof ticket === 'object') {
        sessionStorage.setItem(CACHE_KEYS.TICKET_PREFIX + id, JSON.stringify(ticket));
        if (ticket.id) {
          sessionStorage.setItem(CACHE_KEYS.TICKET_PREFIX + ticket.id, JSON.stringify(ticket));
        }

        // Sincronizar en la lista de tickets
        const list = this.getTickets();
        if (Array.isArray(list)) {
          const idx = list.findIndex((t) => t.id === ticket.id);
          if (idx >= 0) {
            list[idx] = { ...list[idx], ...ticket };
            this.setTickets(list);
          } else {
            list.unshift(ticket);
            this.setTickets(list);
          }
        }
      }
    } catch (e) {
      console.warn('Error al guardar ticket en caché:', e);
    }
  },

  /**
   * Actualiza un ticket en caché y en la lista global de tickets.
   * @param {Object} ticket
   */
  updateTicketInCache(ticket) {
    if (ticket && ticket.id) {
      this.setTicket(ticket.id, ticket);
    }
  },

  /**
   * Limpia toda la caché del cliente.
   */
  clearAll() {
    try {
      sessionStorage.removeItem(CACHE_KEYS.TICKETS);
      sessionStorage.removeItem(CACHE_KEYS.METRICS);
      sessionStorage.removeItem(CACHE_KEYS.CATEGORIES);
      sessionStorage.removeItem(CACHE_KEYS.PRIORITIES);
      sessionStorage.removeItem(CACHE_KEYS.TECHNICIANS);

      Object.keys(sessionStorage).forEach((k) => {
        if (k.startsWith(CACHE_KEYS.TICKET_PREFIX)) {
          sessionStorage.removeItem(k);
        }
      });
    } catch {}
  },
};
