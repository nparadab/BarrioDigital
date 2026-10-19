import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  let clonedReq = req;

  // Si la petición va dirigida al BFF/API y tenemos un token activo, adjuntar header Bearer
  if (token && (req.url.includes('/api') || req.url.includes('localhost:8080'))) {
    // Si el token ya expiró y no es una llamada pública de autenticación, forzar cierre de sesión
    if (authService.isTokenExpired(token) && !req.url.includes('/api/auth/')) {
      console.warn('[authInterceptor] Token expirado detectado antes de enviar solicitud a:', req.url);
      authService.logout(true);
      return throwError(() => new Error('Sesión expirada'));
    }

    clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(clonedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Si el servidor BFF rechaza la autenticación (HTTP 401)
      if (error.status === 401 && !req.url.includes('/api/auth/')) {
        console.warn('[authInterceptor] HTTP 401 Unauthorized recibido desde el BFF. Limpiando credenciales obsoletas...');
        authService.logout(true);
      }
      return throwError(() => error);
    })
  );
};
