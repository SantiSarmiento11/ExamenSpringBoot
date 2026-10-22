/**
 * TechNova IT Helpdesk - Módulo de Autenticación
 * Gestiona el inicio de sesión contra el endpoint REST POST /api/auth/login
 * y consulta el perfil del usuario mediante GET /api/auth/me.
 */

import { post, get, ApiError } from './api.js';
import { ENDPOINTS } from './config.js';
import { saveSession, isLoggedIn, clearSession } from './session.js';
import { renderAlert, clearAlert } from './ui.js';

document.addEventListener('DOMContentLoaded', () => {
  const loginForm = document.getElementById('loginForm');
  const emailInput = document.getElementById('work-email');
  const passwordInput = document.getElementById('work-password');
  const togglePasswordBtn = document.getElementById('togglePasswordBtn');
  const alertContainer = document.getElementById('alertContainer');

  // Si ya tiene sesión activa y no viene con parámetro de expirado, ir al dashboard
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('expired') === '1') {
    clearSession();
    renderAlert(alertContainer, 'Su sesión ha expirado o el token no es válido. Por favor ingrese de nuevo.', 'warning');
  } else if (isLoggedIn()) {
    window.location.replace('dashboard.html');
    return;
  }

  // Asegurar reproducción fluida y continua del video de animación del ticket
  const ticketVideo = document.querySelector('.login-ticket-video');
  if (ticketVideo) {
    ticketVideo.muted = true;
    const playPromise = ticketVideo.play();
    if (playPromise !== undefined) {
      playPromise.catch(() => {
        document.addEventListener('click', () => ticketVideo.play().catch(() => {}), { once: true });
      });
    }
  }

  // Alternar visibilidad de contraseña
  if (togglePasswordBtn && passwordInput) {
    togglePasswordBtn.addEventListener('click', () => {
      const isPassword = passwordInput.type === 'password';
      passwordInput.type = isPassword ? 'text' : 'password';
      togglePasswordBtn.textContent = isPassword ? 'visibility_off' : 'visibility';
    });
  }

  // Prellenar credenciales con un clic si se seleccionan cuentas de prueba
  document.querySelectorAll('.demo-account-chip').forEach((chip) => {
    chip.addEventListener('click', () => {
      const email = chip.getAttribute('data-email');
      if (email && emailInput) emailInput.value = email;
      if (passwordInput) passwordInput.value = 'admin123';
      clearAlert(alertContainer);
    });
  });

  // Procesar envío del formulario
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      clearAlert(alertContainer);

      const email = emailInput ? emailInput.value.trim() : '';
      const password = passwordInput ? passwordInput.value : '';

      if (!email || !password) {
        renderAlert(alertContainer, 'Por favor ingrese su correo corporativo y contraseña.', 'warning');
        return;
      }

      const submitBtn = loginForm.querySelector('button[type="submit"]');
      const originalBtnHtml = submitBtn ? submitBtn.innerHTML : 'Iniciar Sesión';

      try {
        if (submitBtn) {
          submitBtn.innerHTML = '<span class="spinner"></span> <span>Autenticando...</span>';
          submitBtn.disabled = true;
        }

        // 1. Invocar endpoint REST del backend: POST /api/auth/login
        const loginResponse = await post(
          ENDPOINTS.AUTH.LOGIN,
          { email, password },
          { auth: false }
        );

        if (!loginResponse || !loginResponse.token) {
          throw new Error('Respuesta de autenticación incompleta del servidor.');
        }

        // Guardar token preliminar
        saveSession(loginResponse);

        // 2. Consultar perfil completo del usuario autenticado: GET /api/auth/me
        try {
          const profile = await get(ENDPOINTS.AUTH.ME);
          saveSession(loginResponse, profile);
        } catch {
          // Si /me falla o no está disponible, mantener datos del loginResponse
        }

        // Redirección exitosa al panel principal
        window.location.replace('dashboard.html');
      } catch (err) {
        let errorMsg = 'Error al iniciar sesión. Inténtelo nuevamente.';
        if (err instanceof ApiError) {
          if (err.status === 401 || err.status === 400) {
            errorMsg = 'Credenciales inválidas. Verifique su correo corporativo y contraseña.';
          } else {
            errorMsg = err.message || errorMsg;
          }
        } else if (err.message) {
          errorMsg = err.message;
        }
        renderAlert(alertContainer, errorMsg, 'error');
      } finally {
        if (submitBtn) {
          submitBtn.innerHTML = originalBtnHtml;
          submitBtn.disabled = false;
        }
      }
    });
  }
});
