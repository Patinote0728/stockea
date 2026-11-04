import { validarCambioContrasena } from '../dominio/validarCambioContrasena.js';

export async function ejecutarCambioContrasena(solicitud, puertos) {
  const infraccion = validarCambioContrasena(
    solicitud.contrasenaActual,
    solicitud.contrasenaNueva,
    solicitud.confirmacion,
  );
  if (infraccion) {
    throw new Error(infraccion);
  }

  const token = await puertos.autenticacion.obtenerToken();
  await puertos.cuenta.cambiarContrasena(token, {
    contrasenaActual: solicitud.contrasenaActual,
    contrasenaNueva: solicitud.contrasenaNueva,
  });
}
