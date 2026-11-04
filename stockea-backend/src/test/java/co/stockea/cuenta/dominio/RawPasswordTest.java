package co.stockea.cuenta.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RawPasswordTest {

    @Test
    void rejectsNullOrEmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> new ContrasenaPlana(null));
        assertThrows(IllegalArgumentException.class, () -> new ContrasenaPlana(""));
    }

    @Test
    void acceptsNonEmptyValues() {
        assertDoesNotThrow(() -> new ContrasenaPlana("valid-password"));
    }

    @Test
    void doesNotExposeValueInToString() {
        String secret = "never-print-this-password";

        assertFalse(new ContrasenaPlana(secret).toString().contains(secret));
    }
}