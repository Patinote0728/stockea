package co.stockea.cuenta.dominio.excepcion;

import co.stockea.cuenta.dominio.InfraccionPoliticaContrasenas;

import java.util.List;

public final class PoliticaContrasenaIncumplidaExcepcion extends RuntimeException {

    private final List<InfraccionPoliticaContrasenas> violaciones;

    public PoliticaContrasenaIncumplidaExcepcion(List<InfraccionPoliticaContrasenas> infracciones) {
        super(mensaje(infracciones));
        this.violaciones = List.copyOf(infracciones);
    }

    public List<InfraccionPoliticaContrasenas> violaciones() {
        return violaciones;
    }

    private static String mensaje(List<InfraccionPoliticaContrasenas> infracciones) {
        if (infracciones == null || infracciones.isEmpty()) {
            throw new IllegalArgumentException("La lista de infracciones no puede ser nula ni vacía");
        }
        return "La contraseña incumple estas reglas: " + String.join(", ",
                infracciones.stream().map(Enum::name).toList());
    }
}
