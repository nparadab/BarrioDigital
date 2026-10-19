import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, firstValueFrom } from 'rxjs';
import { MsalService } from '@azure/msal-angular';
import { AuthenticationResult } from '@azure/msal-browser';
import { environment } from '../../environments/environment';

export interface UserProfile {
  username: string;
  name: string;
  email: string;
  role: 'Admin' | 'Funcionario' | 'Vecino' | 'Auditor' | string;
  token: string;
  isMicrosoftAuth: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject = new BehaviorSubject<UserProfile | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private msalService: MsalService,
    private http: HttpClient,
    private router: Router
  ) {
    this.restoreSession();
  }

  public async initMsal(): Promise<void> {
    try {
      await this.msalService.instance.initialize();
      const result: AuthenticationResult | null = await this.msalService.instance.handleRedirectPromise();
      if (result) {
        const claims = (result.idTokenClaims || {}) as Record<string, any>;
        const username = (result.account?.username || 'usuario@duocuc.cl').toLowerCase();
        const displayName = result.account?.name || 'Usuario Microsoft';

        let role = 'Vecino';

        // 1. Extraer rol si viene en roles claim de Azure AD
        if (claims['roles'] && Array.isArray(claims['roles']) && claims['roles'].length > 0) {
          const rawRole = String(claims['roles'][0]).toLowerCase();
          if (rawRole === 'administrador' || rawRole === 'admin') {
            role = 'Admin';
          } else if (rawRole === 'auditor') {
            role = 'Auditor';
          } else if (rawRole === 'funcionario') {
            role = 'Funcionario';
          } else {
            role = String(claims['roles'][0]);
          }
        } else if (username.includes('bustos') || displayName.toLowerCase().includes('bustos')) {
          // Administrador del proyecto (Cristóbal Bustos)
          role = 'Admin';
        } else if (username.includes('parada') || displayName.toLowerCase().includes('parada')) {
          // Auditor del proyecto (Nicolás Parada)
          role = 'Auditor';
        }

        const user: UserProfile = {
          username: username,
          name: displayName,
          email: username,
          role: role,
          token: result.idToken || result.accessToken,
          isMicrosoftAuth: true
        };

        this.setCurrentUser(user);
        this.router.navigate(['/dashboard']);
      }
    } catch (e) {
      console.warn('MSAL init / handleRedirectPromise:', e);
    }
  }

  public isTokenExpired(token: string | null | undefined): boolean {
    if (!token) return true;
    try {
      const parts = token.split('.');
      if (parts.length !== 3) return false; // Not a standard JWT, let it pass
      const payloadBase64 = parts[1];
      const decodedJson = atob(payloadBase64.replace(/-/g, '+').replace(/_/g, '/'));
      const payload = JSON.parse(decodedJson);
      if (payload && payload.exp) {
        // Expirado si tiempo actual >= expiración (con 15 segundos de margen)
        return (Date.now() / 1000) >= (payload.exp - 15);
      }
      return false;
    } catch (e) {
      return false;
    }
  }

  private async restoreSession(): Promise<void> {
    const saved = localStorage.getItem('barriodigital_user');
    if (saved) {
      try {
        const user = JSON.parse(saved) as UserProfile;
        if (this.isTokenExpired(user.token)) {
          console.warn('[AuthService] Token en caché expirado. Intentando renovación silenciosa...');
          if (user.isMicrosoftAuth) {
            const renewed = await this.trySilentTokenRefresh();
            if (renewed) return;
          }
          console.warn('[AuthService] No se pudo renovar token expirado. Cerrando sesión.');
          this.logout(true);
          return;
        }
        this.currentUserSubject.next(user);
      } catch (e) {
        localStorage.removeItem('barriodigital_user');
      }
    }
  }

  public async trySilentTokenRefresh(): Promise<boolean> {
    try {
      await this.msalService.instance.initialize();
      const accounts = this.msalService.instance.getAllAccounts();
      if (accounts && accounts.length > 0) {
        const silentResult = await this.msalService.instance.acquireTokenSilent({
          scopes: environment.azure.scopes,
          account: accounts[0]
        });
        const currentUser = this.currentUserSubject.value;
        if (currentUser && silentResult) {
          currentUser.token = silentResult.idToken || silentResult.accessToken;
          this.setCurrentUser(currentUser);
          console.log('[AuthService] Token renovado exitosamente vía MSAL silent refresh.');
          return true;
        }
      }
    } catch (err) {
      console.warn('[AuthService] Error en renovación silenciosa de token MSAL:', err);
    }
    return false;
  }

  public async loginWithMicrosoft(): Promise<void> {
    try {
      await this.msalService.instance.initialize();
      await this.msalService.loginRedirect({
        scopes: environment.azure.scopes
      });
    } catch (error) {
      console.error('Error durante autenticación con Microsoft Azure AD:', error);
      throw error;
    }
  }

  public async loginWithDemoRole(role: string, name?: string, email?: string): Promise<UserProfile> {
    const targetName = name || (
      role === 'Admin' ? 'Cristobal Bustos Reyes' :
      role === 'Auditor' ? 'Nicolas Fabian Parada Bahamondes' :
      role === 'Funcionario' ? 'Funcionario Municipal' : 'Vecino Solicitante'
    );
    const targetEmail = email || (
      role === 'Admin' ? 'c.bustosr@duocuc.cl' :
      role === 'Auditor' ? 'n.paradab@duocuc.cl' :
      role === 'Funcionario' ? 'funcionario@barriodigital.cl' : 'vecino@barriodigital.cl'
    );

    try {
      const url = `/api/auth/demo-token?role=${encodeURIComponent(role)}&name=${encodeURIComponent(targetName)}&email=${encodeURIComponent(targetEmail)}`;
      const resp: any = await firstValueFrom(this.http.get(url));

      const user: UserProfile = {
        username: targetEmail,
        name: targetName,
        email: targetEmail,
        role: resp.role || role,
        token: resp.accessToken,
        isMicrosoftAuth: false
      };

      this.setCurrentUser(user);
      this.router.navigate(['/dashboard']);
      return user;
    } catch (err) {
      console.error('Error al generar token de demostración:', err);
      throw err;
    }
  }

  public setCurrentUser(user: UserProfile): void {
    localStorage.setItem('barriodigital_user', JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  public logout(isExpired = false): void {
    const current = this.currentUserSubject.value;
    localStorage.removeItem('barriodigital_user');
    this.currentUserSubject.next(null);

    if (current?.isMicrosoftAuth && !isExpired) {
      try {
        this.msalService.logoutRedirect();
        return;
      } catch (e) {
        console.warn('MSAL logout redirect error:', e);
      }
    }
    this.router.navigate(['/login'], { queryParams: isExpired ? { expired: 'true' } : {} });
  }

  public getCurrentUser(): UserProfile | null {
    return this.currentUserSubject.value;
  }

  public getToken(): string | null {
    const current = this.currentUserSubject.value;
    if (!current) return null;
    return current.token || null;
  }

  public isAuthenticated(): boolean {
    return !!this.currentUserSubject.value;
  }

  public hasRole(role: string): boolean {
    const current = this.currentUserSubject.value;
    if (!current) return false;
    return current.role.toLowerCase() === role.toLowerCase();
  }

  public hasAnyRole(roles: string[]): boolean {
    const current = this.currentUserSubject.value;
    if (!current) return false;
    return roles.some(r => r.toLowerCase() === current.role.toLowerCase());
  }
}
