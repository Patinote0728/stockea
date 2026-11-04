import test from 'node:test';
import assert from 'node:assert/strict';
import { validarCambioContrasena } from '../dominio/validarCambioContrasena.js';

test('rechaza campos vacios', () => {
  assert.equal(validarCambioContrasena('', 'una-clave-larga', 'una-clave-larga'),
    'Completa todos los campos.');
});

test('rechaza contrasenas que no coinciden', () => {
  assert.equal(validarCambioContrasena('clave-anterior', 'clave-nueva-segura', 'otra-clave-segura'),
    'La nueva contraseña y su confirmación no coinciden.');
});

test('rechaza una contrasena nueva demasiado corta', () => {
  assert.equal(validarCambioContrasena('clave-anterior', '123456789', '123456789'),
    'La nueva contraseña debe tener al menos 10 caracteres.');
});

test('exige que la contrasena nueva sea diferente de la actual', () => {
  assert.equal(validarCambioContrasena('misma-clave-larga', 'misma-clave-larga', 'misma-clave-larga'),
    'La nueva contraseña debe ser diferente de la actual.');
});

test('acepta una solicitud valida', () => {
  assert.equal(validarCambioContrasena('actual-secreta', 'nueva-secreta-segura', 'nueva-secreta-segura'), null);
});
