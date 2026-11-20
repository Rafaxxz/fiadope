package pe.fiadope.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.fiadope.api.dto.Dtos.TiendaDto;
import pe.fiadope.api.dto.Dtos.TiendaRequest;
import pe.fiadope.api.dto.Dtos.TopesDto;
import pe.fiadope.api.service.ParametroService;
import pe.fiadope.api.service.TiendaService;

import java.util.List;

@Tag(name = "P03 Gestión de tiendas (administrador de plataforma)")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TiendaService tiendas;
    private final ParametroService parametros;

    @GetMapping("/tiendas")
    public List<TiendaDto> listar(@RequestParam(required = false) String q, @RequestParam(required = false) String giro,
                                  @RequestParam(required = false) String estado) {
        return tiendas.listar(q, giro, estado);
    }

    @Operation(summary = "HU03: registra una tienda y su administrador (usuario = RUC)")
    @PostMapping("/tiendas")
    @ResponseStatus(HttpStatus.CREATED)
    public TiendaDto crear(@Valid @RequestBody TiendaRequest r) {
        return tiendas.crear(r);
    }

    @PutMapping("/tiendas/{id}")
    public TiendaDto actualizar(@PathVariable Integer id, @Valid @RequestBody TiendaRequest r) {
        return tiendas.actualizar(id, r);
    }

    @Operation(summary = "HU03: da de baja la tienda; sus usuarios ya no pueden ingresar")
    @PatchMapping("/tiendas/{id}/baja")
    public TiendaDto baja(@PathVariable Integer id) {
        return tiendas.cambiarEstado(id, false);
    }

    @PatchMapping("/tiendas/{id}/reactivar")
    public TiendaDto reactivar(@PathVariable Integer id) {
        return tiendas.cambiarEstado(id, true);
    }

    @Operation(summary = "Topes de tasas del BCRP vigentes (en tanto por uno)")
    @GetMapping("/topes")
    public TopesDto topes() {
        return parametros.topes();
    }

    @PutMapping("/topes")
    public TopesDto actualizarTopes(@RequestBody TopesDto dto) {
        return parametros.actualizar(dto);
    }
}

