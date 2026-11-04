package co.stockea.cuenta.dominio.excepcion;

import co.stockea.cuenta.dominio.InfraccionPoliticaContrasenas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoliticaContrasenaIncumplidaExcepcionTest {

    @Test
    void conservaLasInfraccionesYNoPermiteModificarlas() {
        var excepcion = new PoliticaContrasenaIncumplidaExcepcion(
                List.of(InfraccionPoliticaContrasenas.DEMASIADO_CORTA));

        assertEquals(List.of(InfraccionPoliticaContrasenas.DEMASIADO_CORTA), excepcion.violaciones());
        assertThrows(UnsupportedOperationException.class,
                () -> excepcion.violaciones().add(InfraccionPoliticaContrasenas.CONTIENE_CORREO));
    }

    @Test
    void rechazaUnaListaNulaOVacia() {
        assertThrows(IllegalArgumentException.class,
                () -> new PoliticaContrasenaIncumplidaExcepcion(null));
        assertThrows(IllegalArgumentException.class,
                () -> new PoliticaContrasenaIncumplidaExcepcion(List.of()));
    }

    @Test
    void elMensajeMencionaElCodigoDeInfraccion() {
        var excepcion = new PoliticaContrasenaIncumplidaExcepcion(
                List.of(InfraccionPoliticaContrasenas.DEMASIADO_CORTA));

        assertTrue(excepcion.getMessage().contains("DEMASIADO_CORTA"));
    }
}
