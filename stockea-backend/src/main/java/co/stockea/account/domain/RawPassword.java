package co.stockea.account.domain;

public final class RawPassword {

    private final String value;

    public RawPassword(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede ser nula ni vacía");
        }
        this.value = value;
    }

    String value() {
        return value;
    }

    @Override
    public String toString() {
        return "RawPassword[REDACTED]";
    }
}