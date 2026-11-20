package pe.fiadope.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pe.fiadope.api.config.AppProperties;

/** Envío de correos (código MFA y estados de cuenta). Sin SMTP configurado, solo deja constancia en el log. */
@Slf4j
@Service
public class CorreoService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String remitente;

    public CorreoService(ObjectProvider<JavaMailSender> mailSender, @Value("${spring.mail.host:}") String host,
                         AppProperties props) {
        this.mailSender = mailSender;
        this.host = host;
        this.remitente = props.mail().remitente();
    }

    public boolean habilitado() {
        return host != null && !host.isBlank() && mailSender.getIfAvailable() != null;
    }

    public void enviar(String para, String asunto, String html, byte[] adjunto, String nombreAdjunto) {
        if (!habilitado()) {
            log.info("[correo simulado] para={} asunto='{}'", para, asunto);
            return;
        }
        try {
            var sender = mailSender.getObject();
            var msg = sender.createMimeMessage();
            var helper = new MimeMessageHelper(msg, adjunto != null, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(para);
            helper.setSubject(asunto);
            helper.setText(html, true);
            if (adjunto != null) {
                helper.addAttachment(nombreAdjunto, new org.springframework.core.io.ByteArrayResource(adjunto));
            }
            sender.send(msg);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo a {}: {}", para, e.getMessage());
        }
    }

    public void enviarCodigoMfa(String para, String nombre, String codigo) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px">
                  <h2 style="color:#b90064">FiadoPe · Código de verificación</h2>
                  <p>Hola %s, tu código para ingresar es:</p>
                  <p style="font-size:32px;font-weight:900;letter-spacing:8px;background:#fde400;padding:12px;text-align:center">%s</p>
                  <p>Vence en 5 minutos. Si no fuiste tú, cambia tu contraseña.</p>
                </div>""".formatted(nombre, codigo);
        enviar(para, "Tu código de acceso a FiadoPe", html, null, null);
        if (!habilitado()) {
            log.info("[MFA] código para {}: {}", para, codigo);
        }
    }
}

