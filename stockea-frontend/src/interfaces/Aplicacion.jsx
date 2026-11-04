import { useState } from 'react';
import { ejecutarCambioContrasena } from '../aplicacion/cambiarContrasena.js';
import { cerrarSesion, puertoAutenticacion } from '../infraestructura/autenticacionKeycloak.js';
import { puertoCuenta } from '../infraestructura/clienteApiCuenta.js';

export default function Aplicacion() {
  const [contrasenaActual, setContrasenaActual] = useState('');
  const [contrasenaNueva, setContrasenaNueva] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [mensaje, setMensaje] = useState('');
  const [tipoMensaje, setTipoMensaje] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function enviarSolicitud(evento) {
    evento.preventDefault();
    setMensaje('');

    if (!navigator.onLine) {
      setTipoMensaje('error');
      setMensaje('Necesitas conexión a internet para cambiar la contraseña.');
      return;
    }

    setEnviando(true);
    try {
      await ejecutarCambioContrasena(
        { contrasenaActual, contrasenaNueva, confirmacion },
        { autenticacion: puertoAutenticacion, cuenta: puertoCuenta },
      );
      setTipoMensaje('exito');
      setMensaje('Tu contraseña se actualizó correctamente.');
      setContrasenaActual('');
      setContrasenaNueva('');
      setConfirmacion('');
    } catch (error) {
      setTipoMensaje('error');
      setMensaje(error instanceof Error
        ? error.message
        : 'No es posible procesar la solicitud en este momento.');
      setContrasenaActual('');
      setContrasenaNueva('');
      setConfirmacion('');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="pagina">
      <header className="barra-superior">
        <a className="marca" href="/" aria-label="STOCKEA, inicio">
          <span aria-hidden="true">S</span> STOCKEA
        </a>
        <button className="boton-sesion" type="button" onClick={() => cerrarSesion()}>
          Cerrar sesión
        </button>
      </header>

      <section className="contenido">
        <div className="panel-intro">
          <div className="icono-candado" aria-hidden="true">⌑</div>
          <p className="eyebrow">SEGURIDAD DE LA CUENTA</p>
          <h1>Cambia tu contraseña</h1>
          <p className="subtitulo">
            Mantén tu cuenta protegida. El cambio requiere conexión y no se guarda en este dispositivo.
          </p>
        </div>

        <form className="tarjeta formulario" onSubmit={enviarSolicitud} noValidate>
          <label htmlFor="contrasena-actual">Contraseña actual</label>
          <input
            id="contrasena-actual"
            name="contrasena-actual"
            type="password"
            autoComplete="current-password"
            value={contrasenaActual}
            onChange={(evento) => setContrasenaActual(evento.target.value)}
            maxLength={1024}
            required
          />

          <label htmlFor="contrasena-nueva">Nueva contraseña</label>
          <input
            id="contrasena-nueva"
            name="contrasena-nueva"
            type="password"
            autoComplete="new-password"
            value={contrasenaNueva}
            onChange={(evento) => setContrasenaNueva(evento.target.value)}
            maxLength={1024}
            required
          />
          <p className="ayuda">Usa al menos 10 caracteres y evita incluir tu correo o nombre de usuario.</p>

          <label htmlFor="confirmacion">Confirma la nueva contraseña</label>
          <input
            id="confirmacion"
            name="confirmacion"
            type="password"
            autoComplete="new-password"
            value={confirmacion}
            onChange={(evento) => setConfirmacion(evento.target.value)}
            maxLength={1024}
            required
          />

          {mensaje && (
            <p className={`mensaje ${tipoMensaje}`} role="status" aria-live="polite">
              {mensaje}
            </p>
          )}

          <button className="boton-principal" type="submit" disabled={enviando}>
            {enviando ? 'Actualizando…' : 'Actualizar contraseña'}
          </button>
        </form>

        <p className="nota-seguridad">
          <span aria-hidden="true">●</span> Tu contraseña se envía únicamente al servicio seguro de STOCKEA.
        </p>
      </section>
    </main>
  );
}
