package co.stockea.cuenta.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContrasenaPlanaTest {

    @Test
    void rechazaValoresNulosOVacios() {
        assertThrows(IllegalArgumentException.class, () -> new ContrasenaPlana(null));
        assertThrows(IllegalArgumentException.class, () -> new ContrasenaPlana(""));
    }

    @Test
    void aceptaValoresNoVacios() {
        assertDoesNotThrow(() -> new ContrasenaPlana("contrasena-valida"));
    }

    @Test
    void noExponeElValorEnToString() {
        String secreto = "nunca-imprimir-esta-contrasena";

        assertFalse(new ContrasenaPlana(secreto).toString().contains(secreto));
    }
}
