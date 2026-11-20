package pe.fiadope.api.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class Red {

    private Red() {
    }

    /** IP de la petición actual (respeta X-Forwarded-For detrás del balanceador/WAF). */
    public static String ipActual() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return "127.0.0.1";
        }
        HttpServletRequest req = attrs.getRequest();
        String xff = req.getHeader("X-Forwarded-For");
        String ip = xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : req.getRemoteAddr();
        return ip == null || ip.isBlank() ? "127.0.0.1" : ip;
    }
}

