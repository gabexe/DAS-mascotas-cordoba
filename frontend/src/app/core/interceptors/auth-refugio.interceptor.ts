import { HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

/**
 * Interceptor funcional HTTP que inyecta la cabecera 'X-Refugio-Id' en todas las
 * solicitudes dirigidas a la API REST del backend cuando existe un refugio autenticado.
 *
 * Aplica una resolución dual de identidad:
 * 1. Primaria: Señal reactiva `AuthService.currentRefugio()?.id_refugio`.
 * 2. Secundaria (fallback): Lectura de 'mascotas_cba_refugio_user' en localStorage.
 */
export const authRefugioInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authService = inject(AuthService);

  // 1. Obtener id_refugio desde AuthService o fallback de localStorage
  let refugioId: number | null | undefined = authService.currentRefugio()?.id_refugio ?? authService.currentUser()?.id_refugio;

  if (refugioId == null) {
    try {
      const stored = localStorage.getItem('mascotas_cba_refugio_user');
      if (stored) {
        const user = JSON.parse(stored);
        refugioId = user?.refugio?.id_refugio ?? user?.id_refugio ?? null;
      }
    } catch {
      // Ignorar errores de deserialización; refugioId permanece nulo
      refugioId = null;
    }
  }

  // 2. Determinar si la solicitud apunta al backend y no es el endpoint de autenticación inicial
  const isApiRequest = req.url.includes('/api') || (environment.apiUrl && req.url.includes(environment.apiUrl));
  const isAuthLogin = req.url.includes('/auth/login');

  // 3. Inyectar cabecera X-Refugio-Id si aplica
  if (refugioId != null && isApiRequest && !isAuthLogin) {
    const authReq = req.clone({
      setHeaders: {
        'X-Refugio-Id': String(refugioId)
      }
    });
    return next(authReq);
  }

  return next(req);
};
