package ar.portal.autoridadmesa.config;

/** Un dato inválido: se informa cuál es (campo) y por qué. */
public class ValidacionException extends RuntimeException {
    private final String campo;

    public ValidacionException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String campo() { return campo; }
}
