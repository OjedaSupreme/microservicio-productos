package mx.edu.utch.productos.exception;

/**
 * Se lanza cuando el recurso solicitado no existe (HTTP 404).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecursoNoEncontradoException(String recurso, String id) {
        super(String.format("%s con id '%s' no encontrado", recurso, id));
    }
}
