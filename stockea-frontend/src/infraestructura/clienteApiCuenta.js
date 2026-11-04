export async function cambiarContrasena(token, solicitud) {
  const respuesta = await fetch('/api/v1/cuenta/contrasena', {
    method: 'POST',
    cache: 'no-store',
    credentials: 'omit',
    referrerPolicy: 'no-referrer',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify({
      contrasenaActual: solicitud.contrasenaActual,
      contrasenaNueva: solicitud.contrasenaNueva,
    }),
  });

  if (respuesta.status === 204) {
    return;
  }

  let codigo = '';
  try {
    const cuerpo = await respuesta.json();
    codigo = typeof cuerpo.codigo === 'string' ? cuerpo.codigo : '';
  } catch {
    // No se expone al usuario el cuerpo no estructurado del proveedor.
  }

  throw new Error(mensajeParaCodigo(respuesta.status, codigo));
}

export const puertoCuenta = { cambiarContrasena };

function mensajeParaCodigo(estado, codigo) {
  if (codigo === 'CREDENCIALES_INVALIDAS') {
    return 'No fue posible cambiar la contraseña. Verifica los datos ingresados.';
  }
  if (codigo === 'POLITICA_CONTRASENA') {
    return 'La nueva contraseña no cumple la política requerida.';
  }
  if (codigo === 'LIMITE_INTENTOS' || estado === 429) {
    return 'Superaste el límite de intentos. Intenta nuevamente más tarde.';
  }
  if (estado === 401 || codigo === 'TOKEN_INVALIDO') {
    return 'Tu sesión venció. Inicia sesión nuevamente.';
  }
  return 'No es posible procesar la solicitud en este momento. Intenta más tarde.';
}
