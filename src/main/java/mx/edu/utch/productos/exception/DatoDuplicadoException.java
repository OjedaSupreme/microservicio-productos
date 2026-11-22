package mx.edu.utch.productos.exception;

/**
 * Se lanza cuando se intenta registrar un valor que debe ser único (HTTP 409).
 */
public class DatoDuplicadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DatoDuplicadoException(String campo, String valor) {
        super(String.format("Ya existe un registro con %s '%s'", campo, valor));
    }
}
