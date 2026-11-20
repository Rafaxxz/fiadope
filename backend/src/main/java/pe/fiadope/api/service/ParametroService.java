package pe.fiadope.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.fiadope.api.domain.Parametro;
import pe.fiadope.api.dto.Dtos.TopesDto;
import pe.fiadope.api.exception.ApiException;
import pe.fiadope.api.repository.ParametroRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ParametroService {

    public static final String TOPE_COMP = "tope_tea_compensatoria";
    public static final String TOPE_MOR = "tope_tea_moratoria";

    private final ParametroRepository repo;
    private final BitacoraService bitacora;

    public BigDecimal valor(String clave) {
        return repo.findById(clave).map(Parametro::getValor)
                .orElseThrow(() -> new IllegalStateException("Falta el parámetro " + clave));
    }

    public TopesDto topes() {
        return new TopesDto(valor(TOPE_COMP), valor(TOPE_MOR));
    }

    @Transactional
    public TopesDto actualizar(TopesDto dto) {
        if (dto.teaCompensatoria() == null || dto.teaCompensatoria().signum() <= 0
                || dto.teaMoratoria() == null || dto.teaMoratoria().signum() <= 0) {
            throw ApiException.badRequest("Los topes deben ser mayores a cero");
        }
        TopesDto antes = topes();
        guardar(TOPE_COMP, dto.teaCompensatoria());
        guardar(TOPE_MOR, dto.teaMoratoria());
        bitacora.registrar("CAMBIO_TOPES", "parametro", null, antes, dto);
        return topes();
    }

    private void guardar(String clave, BigDecimal valor) {
        Parametro p = repo.findById(clave).orElseThrow();
        p.setValor(valor);
        p.setActualizado(LocalDateTime.now());
    }

    public Map<String, BigDecimal> mapa() {
        return Map.of(TOPE_COMP, valor(TOPE_COMP), TOPE_MOR, valor(TOPE_MOR));
    }
}

