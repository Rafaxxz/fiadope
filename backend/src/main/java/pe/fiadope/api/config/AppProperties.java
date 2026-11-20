package pe.fiadope.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String zonaHoraria, Jwt jwt, Seguridad seguridad, Mfa mfa, Mail mail, Cors cors,
                            Uploads uploads, Seed seed) {

    public record Jwt(String secret, int horasValidez) {
    }

    public record Seguridad(int maxIntentos, int minutosBloqueo, int minutosMfa, int maxIntentosMfa,
                            int minutosInactividad) {
    }

    public record Mfa(boolean mostrarCodigo) {
    }

    public record Mail(String remitente) {
    }

    public record Cors(List<String> origenes) {
    }

    public record Uploads(String directorio) {
    }

    public record Seed(boolean habilitado) {
    }
}

