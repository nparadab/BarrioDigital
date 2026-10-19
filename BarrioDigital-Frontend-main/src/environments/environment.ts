export const environment = {
  production: false,
  apiUrl: '/api',
  azure: {
    clientId: 'dca1a06c-9b36-484d-8909-ae95c3f26301',
    tenantId: '5970cdb9-bbea-48dd-ad49-c3aacc2c1241',
    authority: 'https://login.microsoftonline.com/5970cdb9-bbea-48dd-ad49-c3aacc2c1241',
    redirectUri: typeof window !== 'undefined' ? window.location.origin : 'http://localhost:4200',
    postLogoutRedirectUri: typeof window !== 'undefined' ? window.location.origin + '/login' : 'http://localhost:4200/login',
    apiEndpoint: 'https://vtk53qn89d.execute-api.us-east-1.amazonaws.com/api',
    scopes: [
      'openid',
      'profile',
      'email'
    ]
  }
};
