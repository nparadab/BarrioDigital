import { IPublicClientApplication, PublicClientApplication, InteractionType, BrowserCacheLocation, LogLevel } from '@azure/msal-browser';
import { MsalGuardConfiguration, MsalInterceptorConfiguration, ProtectedResourceScopes } from '@azure/msal-angular';
import { environment } from '../../environments/environment';

export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.azure.clientId,
      authority: environment.azure.authority,
      redirectUri: environment.azure.redirectUri,
      postLogoutRedirectUri: environment.azure.postLogoutRedirectUri
    },
    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage
    },
    system: {
      loggerOptions: {
        loggerCallback: (level, message, containsPii) => {
          if (!containsPii) {
            console.log(`[MSAL ${LogLevel[level]}]: ${message}`);
          }
        },
        logLevel: LogLevel.Warning
      }
    }
  });
}

export function MSALGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: environment.azure.scopes
    },
    loginFailedRoute: '/login'
  };
}

export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string | ProtectedResourceScopes> | null>();
  // Cualquier llamada al backend BFF incluirá el Bearer access token
  protectedResourceMap.set(environment.apiUrl, environment.azure.scopes);
  protectedResourceMap.set('http://localhost:8080/api', environment.azure.scopes);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}
