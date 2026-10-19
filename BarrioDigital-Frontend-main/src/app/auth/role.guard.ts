import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from './auth.service';

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  const expectedRoles: string[] = route.data?.['roles'] || [];
  if (expectedRoles.length === 0) {
    return true;
  }

  const user = authService.getCurrentUser();
  if (user && expectedRoles.some(r => r.toLowerCase() === user.role.toLowerCase())) {
    return true;
  }

  // Usuario autenticado pero sin rol suficiente para esta página
  alert(`Acceso denegado: Esta sección requiere uno de los roles [${expectedRoles.join(', ')}]. Tu rol actual es [${user?.role}].`);
  router.navigate(['/dashboard']);
  return false;
};
