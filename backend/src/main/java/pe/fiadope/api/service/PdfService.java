package pe.fiadope.api.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import pe.fiadope.api.domain.Cliente;
import pe.fiadope.api.domain.EstadoCuenta;
import pe.fiadope.api.domain.EstadoCuentaItem;
import pe.fiadope.api.dto.Dtos.CronogramaDto;
import pe.fiadope.api.dto.Dtos.FilaCronogramaDto;
import pe.fiadope.api.finanzas.MotorFinanciero;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Reportes A4 descargables: estado de cuenta y cronograma de cuotas. */
@Service
public class PdfService {

    private static final Color MAGENTA = new Color(0xB9, 0x00, 0x64);
    private static final Color AMARILLO = new Color(0xFD, 0xE4, 0x00);
    private static final Color GRIS = new Color(0xF0, 0xED, 0xEC);
    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, MAGENTA);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10);
    private static final Font PEQUENA = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);

    /**
     * Estado de cuenta. Si {@code moraAl} no es nulo y el estado está pendiente, agrega la mora calculada a esa fecha.
     */
    public byte[] estadoCuenta(EstadoCuenta e, LocalDate moraAl) {
        Cliente c = e.getCliente();
        return documento(doc -> {
            encabezado(doc, "Estado de cuenta", c.getTienda().getRazonSocial() + " · RUC " + c.getTienda().getRuc());
            PdfPTable datos = new PdfPTable(new float[]{1, 1});
            datos.setWidthPercentage(100);
            celdaDato(datos, "Cliente", c.getNombres() + " · DNI " + c.getDni());
            celdaDato(datos, "Línea de crédito", "S/ " + c.getLimiteCredito());
            celdaDato(datos, "Fecha de corte", e.getFechaCorte().format(F));
            celdaDato(datos, "Fecha de pago", e.getFechaPago().format(F));
            celdaDato(datos, "TEA compensatoria", Mapper.pct(MotorFinanciero.tea(c.ted()), 4) + " %");
            celdaDato(datos, "TEA moratoria", Mapper.pct(MotorFinanciero.tea(c.tedMora()), 4) + " %");
            doc.add(datos);
            doc.add(new Paragraph(" "));

            PdfPTable t = new PdfPTable(new float[]{1.2f, 3.5f, 0.8f, 1.3f, 1.2f, 1.3f});
            t.setWidthPercentage(100);
            for (String h : new String[]{"Fecha", "Detalle", "Días", "Monto", "Interés", "Total"}) {
                cabecera(t, h);
            }
            BigDecimal total = BigDecimal.ZERO;
            for (EstadoCuentaItem i : e.getItems()) {
                celda(t, i.getFecha().format(F), false);
                celda(t, ("MORA".equals(i.getTipo()) ? "Intereses por mora · " : "") + i.getDetalle(), false);
                celda(t, String.valueOf(i.getDias()), true);
                celda(t, "MORA".equals(i.getTipo()) ? "—" : i.getMonto().toPlainString(), true);
                celda(t, i.getInteres().toPlainString(), true);
                celda(t, i.getTotal().toPlainString(), true);
                total = total.add(i.getTotal());
            }
            if (moraAl != null && e.pendiente()) {
                var m = MotorFinanciero.pagoConMora(e.getTotalPeriodo(), c.ted(), c.tedMora(), e.getFechaPago(), moraAl);
                if (m.diasMora() > 0) {
                    BigDecimal im = m.interesCompensatorio().add(m.interesMoratorio());
                    celda(t, moraAl.format(F), false);
                    celda(t, "Intereses por mora al " + moraAl.format(F) + " (compensatorio " + m.interesCompensatorio()
                            + " + moratorio " + m.interesMoratorio() + ")", false);
                    celda(t, String.valueOf(m.diasMora()), true);
                    celda(t, "—", true);
                    celda(t, im.toPlainString(), true);
                    celda(t, im.toPlainString(), true);
                    total = total.add(im);
                }
            }
            PdfPCell etiqueta = new PdfPCell(new Phrase("TOTAL A PAGAR (" + c.getMoneda() + ")", NEGRITA));
            etiqueta.setColspan(5);
            etiqueta.setBackgroundColor(AMARILLO);
            etiqueta.setHorizontalAlignment(Element.ALIGN_RIGHT);
            etiqueta.setPadding(6);
            t.addCell(etiqueta);
            PdfPCell valor = new PdfPCell(new Phrase(MotorFinanciero.redondear(total).toPlainString(), NEGRITA));
            valor.setBackgroundColor(AMARILLO);
            valor.setHorizontalAlignment(Element.ALIGN_RIGHT);
            valor.setPadding(6);
            t.addCell(valor);
            doc.add(t);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Estado: " + e.getEstado() + ". Solo se acepta el pago total; no se aceptan pagos parciales. "
                    + "Si pagas después de la fecha de pago se cobran intereses compensatorio y moratorio por cada día de atraso, "
                    + "que se muestran en el ítem «Intereses por mora».", PEQUENA));
        });
    }

    public byte[] cronograma(CronogramaDto c) {
        return documento(doc -> {
            encabezado(doc, "Cronograma de cuotas", c.cliente() + " · DNI " + c.dni());
            PdfPTable datos = new PdfPTable(new float[]{1, 1});
            datos.setWidthPercentage(100);
            celdaDato(datos, "Producto", c.compra().producto());
            celdaDato(datos, "Fecha de compra", c.compra().fechaHora().toLocalDate().format(F));
            celdaDato(datos, "Precio (C)", "S/ " + c.capital());
            celdaDato(datos, "Días de gracia", c.diasGracia() + " días (gracia total)");
            celdaDato(datos, "Capital ajustado (C')", "S/ " + c.capitalAjustado());
            celdaDato(datos, "Cuota fija (R)", "S/ " + c.cuota());
            celdaDato(datos, "TEA aplicada", c.teaPorcentaje() + " %");
            celdaDato(datos, "TEM", c.temPorcentaje() + " %");
            doc.add(datos);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("¿Cómo se calculó la gracia? C' = C × (1 + TED)^días = " + c.capital() + " → "
                    + c.capitalAjustado() + ". Los intereses de los días de gracia se suman al capital antes de calcular la cuota "
                    + "con el método francés vencido ordinario.", PEQUENA));
            doc.add(new Paragraph(" "));
            PdfPTable t = new PdfPTable(new float[]{0.6f, 1.3f, 1.3f, 1.1f, 1.3f, 1.1f, 1.3f});
            t.setWidthPercentage(100);
            for (String h : new String[]{"N°", "Vencimiento", "Saldo inicial", "Interés", "Amortización", "Cuota", "Saldo final"}) {
                cabecera(t, h);
            }
            for (FilaCronogramaDto f : c.filas()) {
                celda(t, String.valueOf(f.numero()), true);
                celda(t, f.vencimiento().format(F), false);
                celda(t, f.saldoInicial().toPlainString(), true);
                celda(t, f.interes().toPlainString(), true);
                celda(t, f.amortizacion().toPlainString(), true);
                celda(t, f.cuota().toPlainString(), true);
                celda(t, f.saldoFinal().toPlainString(), true);
            }
            PdfPCell tot = new PdfPCell(new Phrase("Totales", NEGRITA));
            tot.setColspan(3);
            tot.setBackgroundColor(AMARILLO);
            tot.setPadding(5);
            t.addCell(tot);
            for (BigDecimal v : new BigDecimal[]{c.totalInteres(), c.totalAmortizacion(), c.totalVentaPlazo()}) {
                PdfPCell x = new PdfPCell(new Phrase(v.toPlainString(), NEGRITA));
                x.setBackgroundColor(AMARILLO);
                x.setHorizontalAlignment(Element.ALIGN_RIGHT);
                x.setPadding(5);
                t.addCell(x);
            }
            PdfPCell vacio = new PdfPCell(new Phrase("", NEGRITA));
            vacio.setBackgroundColor(AMARILLO);
            t.addCell(vacio);
            doc.add(t);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Total venta a plazo: S/ " + c.totalVentaPlazo() + " · Interés total (incluye la gracia): S/ "
                    + c.interesTotal(), NEGRITA));
        });
    }

    // ------------------------------------------------------------------ util
    private interface Contenido {
        void escribir(Document doc) throws DocumentException;
    }

    private byte[] documento(Contenido contenido) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, out);
        doc.open();
        try {
            contenido.escribir(doc);
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el PDF", ex);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    private void encabezado(Document doc, String titulo, String subtitulo) throws DocumentException {
        Paragraph marca = new Paragraph("FIADOPE · Deja el cuaderno, cobra con orden", new Font(Font.HELVETICA, 9, Font.BOLD, new Color(0xA0, 0x41, 0x00)));
        doc.add(marca);
        doc.add(new Paragraph(titulo, TITULO));
        doc.add(new Paragraph(subtitulo, NORMAL));
        doc.add(new Paragraph("Emitido el " + LocalDate.now().format(F), PEQUENA));
        doc.add(new Paragraph(" "));
    }

    private void cabecera(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
        c.setBackgroundColor(MAGENTA);
        c.setPadding(5);
        t.addCell(c);
    }

    private void celda(PdfPTable t, String texto, boolean derecha) {
        PdfPCell c = new PdfPCell(new Phrase(texto, NORMAL));
        c.setPadding(4);
        c.setBorderColor(GRIS);
        if (derecha) {
            c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        }
        t.addCell(c);
    }

    private void celdaDato(PdfPTable t, String etiqueta, String valor) {
        Phrase p = new Phrase();
        p.add(new Chunk(etiqueta + ": ", NEGRITA));
        p.add(new Chunk(valor, NORMAL));
        PdfPCell c = new PdfPCell(p);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(3);
        t.addCell(c);
    }
}
