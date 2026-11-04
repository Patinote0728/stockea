export function validarCambioContrasena(contrasenaActual, contrasenaNueva, confirmacion) {
  if (!contrasenaActual || !contrasenaNueva || !confirmacion) {
    return 'Completa todos los campos.';
  }

  if (contrasenaNueva !== confirmacion) {
    return 'La nueva contraseña y su confirmación no coinciden.';
  }

  if (contrasenaNueva.length < 10) {
    return 'La nueva contraseña debe tener al menos 10 caracteres.';
  }

  if (contrasenaActual === contrasenaNueva) {
    return 'La nueva contraseña debe ser diferente de la actual.';
  }

  return null;
}
