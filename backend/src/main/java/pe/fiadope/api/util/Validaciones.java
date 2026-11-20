package pe.fiadope.api.util;

import java.util.regex.Pattern;

public final class Validaciones {

    /** 8 a 64 caracteres, con al menos una mayúscula, un número y un símbolo. */
    public static final Pattern CLAVE_SEGURA = Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$");

    private Validaciones() {
    }

    public static boolean claveSegura(String clave) {
        return clave != null && CLAVE_SEGURA.matcher(clave).matches();
    }

    public static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public static String ocultarCorreo(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        String usuario = email.substring(0, at);
        String visible = usuario.substring(0, Math.min(2, usuario.length()));
        return visible + "*".repeat(Math.max(3, usuario.length() - 2)) + email.substring(at);
    }
}
