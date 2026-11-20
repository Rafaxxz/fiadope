package pe.fiadope.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final Map<String, String> errores;

    public ApiException(HttpStatus status, String mensaje) {
        this(status, mensaje, Map.of());
    }

    public ApiException(HttpStatus status, String mensaje, Map<String, String> errores) {
        super(mensaje);
        this.status = status;
        this.errores = errores;
    }

    public static ApiException badRequest(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje);
    }

    public static ApiException campo(String campo, String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje, Map.of(campo, mensaje));
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, mensaje);
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, mensaje);
    }
}

