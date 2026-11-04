import Keycloak from 'keycloak-js';

const autenticacion = new Keycloak({
  url: import.meta.env.VITE_IDP_URL || 'https://keycloak.localhost',
  realm: 'stockea',
  clientId: 'stockea-web',
});

export async function iniciarSesion() {
  return autenticacion.init({
    onLoad: 'login-required',
    pkceMethod: 'S256',
    checkLoginIframe: false,
    enableLogging: false,
  });
}

export async function obtenerTokenAcceso() {
  if (!autenticacion.authenticated) {
    throw new Error('La sesión no está autenticada.');
  }

  await autenticacion.updateToken(30);
  if (!autenticacion.token) {
    throw new Error('No se pudo renovar la sesión.');
  }

  return autenticacion.token;
}

export const puertoAutenticacion = {
  obtenerToken: obtenerTokenAcceso,
};

export function cerrarSesion() {
  return autenticacion.logout({ redirectUri: window.location.origin });
}
