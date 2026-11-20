package pe.fiadope.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.fiadope.api.dto.Dtos.CronogramaDto;
import pe.fiadope.api.dto.Dtos.MiCuentaDto;
import pe.fiadope.api.service.MiCuentaService;

@Tag(name = "P11 Mi cuenta (cliente)")
@RestController
@RequestMapping("/api/mi-cuenta")
@RequiredArgsConstructor
public class MiCuentaController {

    private final MiCuentaService servicio;

    @GetMapping
    public MiCuentaDto miCuenta() {
        return servicio.miCuenta();
    }

    @GetMapping("/compras/{id}/cronograma")
    public CronogramaDto cronograma(@PathVariable Integer id) {
        return servicio.cronograma(id);
    }

    @GetMapping("/compras/{id}/cronograma/pdf")
    public ResponseEntity<byte[]> cronogramaPdf(@PathVariable Integer id) {
        return Archivos.pdf(servicio.pdfCronograma(id), "cronograma-" + id + ".pdf");
    }

    @GetMapping("/estado-cuenta/pdf")
    public ResponseEntity<byte[]> estadoPdf(@RequestParam(required = false) Integer idEstado) {
        return Archivos.pdf(servicio.pdfEstadoCuenta(idEstado), "mi-estado-de-cuenta.pdf");
    }
}

