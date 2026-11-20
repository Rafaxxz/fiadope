package pe.fiadope.api.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Captcha de 4 dígitos generado como SVG distorsionado. Cada captcha se usa una sola vez y vence a los 5 minutos. */
@Service
public class CaptchaService {

    private record Reto(String codigo, Instant expira) {
    }

    public record Captcha(String captchaId, String imagen) {
    }

    private final Map<String, Reto> retos = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public Captcha generar() {
        String codigo = String.format("%04d", random.nextInt(10_000));
        String id = UUID.randomUUID().toString();
        retos.put(id, new Reto(codigo, Instant.now().plusSeconds(300)));
        String svg = dibujar(codigo);
        return new Captcha(id, "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes()));
    }

    public boolean validar(String id, String respuesta) {
        if (id == null || respuesta == null) {
            return false;
        }
        Reto r = retos.remove(id);
        return r != null && r.expira().isAfter(Instant.now()) && r.codigo().equals(respuesta.trim());
    }

    private String dibujar(String codigo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns='http://www.w3.org/2000/svg' width='160' height='56' viewBox='0 0 160 56'>");
        sb.append("<rect width='160' height='56' fill='#fde400'/>");
        for (int i = 0; i < 6; i++) {
            sb.append(String.format("<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='%s' stroke-width='2' opacity='0.55'/>",
                    random.nextInt(160), random.nextInt(56), random.nextInt(160), random.nextInt(56),
                    i % 2 == 0 ? "#b90064" : "#a04100"));
        }
        for (int i = 0; i < codigo.length(); i++) {
            int x = 22 + i * 32 + random.nextInt(6);
            int y = 38 + random.nextInt(8);
            int rot = random.nextInt(40) - 20;
            sb.append(String.format("<text x='%d' y='%d' transform='rotate(%d %d %d)' font-family='Arial Black,Arial' font-size='30' font-weight='900' fill='#1c1b1b'>%c</text>",
                    x, y, rot, x, y, codigo.charAt(i)));
        }
        for (int i = 0; i < 40; i++) {
            sb.append(String.format("<circle cx='%d' cy='%d' r='1.4' fill='#1c1b1b' opacity='0.5'/>", random.nextInt(160), random.nextInt(56)));
        }
        sb.append("</svg>");
        return sb.toString();
    }

    @Scheduled(fixedRate = 120_000)
    void limpiar() {
        Instant ahora = Instant.now();
        retos.entrySet().removeIf(e -> e.getValue().expira().isBefore(ahora));
    }
}

