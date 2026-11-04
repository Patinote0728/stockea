import test from 'node:test';
import assert from 'node:assert/strict';
import { ejecutarCambioContrasena } from '../aplicacion/cambiarContrasena.js';

test('no consulta puertos externos si la confirmacion es incorrecta', async () => {
  let consultado = false;
  const puertos = {
    autenticacion: { obtenerToken: async () => { consultado = true; return 'token'; } },
    cuenta: { cambiarContrasena: async () => { consultado = true; } },
  };

  await assert.rejects(
    ejecutarCambioContrasena({
      contrasenaActual: 'actual-secreta',
      contrasenaNueva: 'nueva-secreta-segura',
      confirmacion: 'otra-secreta-segura',
    }, puertos),
    /no coinciden/,
  );
  assert.equal(consultado, false);
});

test('solicita el token y entrega al puerto solo las dos contrasenas requeridas', async () => {
  const llamadas = [];
  const puertos = {
    autenticacion: { obtenerToken: async () => 'token-efimero' },
    cuenta: { cambiarContrasena: async (...argumentos) => llamadas.push(argumentos) },
  };

  await ejecutarCambioContrasena({
    contrasenaActual: 'actual-secreta',
    contrasenaNueva: 'nueva-secreta-segura',
    confirmacion: 'nueva-secreta-segura',
  }, puertos);

  assert.deepEqual(llamadas, [[
    'token-efimero',
    { contrasenaActual: 'actual-secreta', contrasenaNueva: 'nueva-secreta-segura' },
  ]]);
});
