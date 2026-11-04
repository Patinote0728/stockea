package co.stockea.cuenta.dominio;

public enum InfraccionPoliticaContrasenas {
    DEMASIADO_CORTA,
    IGUAL_A_LA_ACTUAL,
    EN_HISTORIAL_DE_CONTRASENAS,
    CONTIENE_NUMERO_DOCUMENTO,
    CONTIENE_EMAIL
}