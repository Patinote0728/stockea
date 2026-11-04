import React from 'react';
import { createRoot } from 'react-dom/client';
import Aplicacion from './Aplicacion.jsx';
import { iniciarSesion } from '../infraestructura/autenticacionKeycloak.js';
import './estilos.css';

const raiz = createRoot(document.getElementById('root'));

iniciarSesion()
  .then(() => raiz.render(<React.StrictMode><Aplicacion /></React.StrictMode>))
  .catch(() => raiz.render(
    <main className="estado-inicial" role="alert">
      <section className="tarjeta">
        <Marca />
        <h1>No pudimos iniciar tu sesión</h1>
        <p>Comprueba tu conexión e inténtalo de nuevo.</p>
        <button className="boton-secundario" onClick={() => window.location.reload()}>
          Reintentar
        </button>
      </section>
    </main>,
  ));

function Marca() {
  return <div className="marca" aria-label="STOCKEA"><span aria-hidden="true">S</span> STOCKEA</div>;
}
