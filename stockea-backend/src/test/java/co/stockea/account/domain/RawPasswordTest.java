package co.stockea.account.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RawPasswordTest {

    @Test
    void rejectsNullOrEmptyValues() {
        assertThrows(IllegalArgumentException.class, () -> new RawPassword(null));
        assertThrows(IllegalArgumentException.class, () -> new RawPassword(""));
    }

    @Test
    void acceptsNonEmptyValues() {
        assertDoesNotThrow(() -> new RawPassword("valid-password"));
    }

    @Test
    void doesNotExposeValueInToString() {
        String secret = "never-print-this-password";

        assertFalse(new RawPassword(secret).toString().contains(secret));
    }
}