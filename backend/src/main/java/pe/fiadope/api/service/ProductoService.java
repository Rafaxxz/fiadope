package pe.fiadope.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.fiadope.api.config.AppProperties;
import pe.fiadope.api.domain.Producto;
import pe.fiadope.api.dto.Dtos.ProductoDto;
import pe.fiadope.api.dto.Dtos.ProductoRequest;
import pe.fiadope.api.exception.ApiException;
import pe.fiadope.api.repository.ProductoRepository;
import pe.fiadope.api.repository.TiendaRepository;
import pe.fiadope.api.security.UsuarioActual;
import pe.fiadope.api.util.Validaciones;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** HU05: catálogo de productos con precio contado, precio de lista y modalidades. */
@Service
@RequiredArgsConstructor
public class ProductoService {

    public static final Set<String> CATEGORIAS = Set.of("Abarrotes y granel", "Lácteos y panadería",
            "Bebidas y gaseosas", "Electro y hogar", "Otro");

    private final ProductoRepository productos;
    private final TiendaRepository tiendas;
    private final BitacoraService bitacora;
    private final AppProperties props;

    @Transactional(readOnly = true)
    public List<ProductoDto> listar(String categoria, String q, String estado) {
        return productos.buscar(UsuarioActual.tienda(), Validaciones.vacioANull(categoria), Validaciones.vacioANull(q),
                Validaciones.vacioANull(estado)).stream().map(Mapper::producto).toList();
    }

    @Transactional
    public ProductoDto crear(ProductoRequest r) {
        Integer t = UsuarioActual.tienda();
        if (productos.existsByTiendaIdAndSkuIgnoreCase(t, r.sku().trim())) {
            throw ApiException.campo("sku", "El SKU ya existe en tu catálogo");
        }
        validar(r);
        Producto p = new Producto();
        p.setTienda(tiendas.getReferenceById(t));
        copiar(r, p);
        productos.save(p);
        ProductoDto dto = Mapper.producto(p);
        bitacora.registrar("ALTA_PRODUCTO", "producto", p.getId(), null, dto);
        return dto;
    }

    @Transactional
    public ProductoDto actualizar(Integer id, ProductoRequest r) {
        Producto p = buscar(id);
        if (!p.getSku().equalsIgnoreCase(r.sku().trim()) && productos.existsByTiendaIdAndSkuIgnoreCase(UsuarioActual.tienda(), r.sku().trim())) {
            throw ApiException.campo("sku", "El SKU ya existe en tu catálogo");
        }
        validar(r);
        ProductoDto antes = Mapper.producto(p);
        copiar(r, p);
        ProductoDto dto = Mapper.producto(p);
        bitacora.registrar("EDITA_PRODUCTO", "producto", p.getId(), antes, dto);
        return dto;
    }

    @Transactional
    public ProductoDto cambiarEstado(Integer id, boolean activar) {
        Producto p = buscar(id);
        String antes = p.getEstado();
        p.setEstado(activar ? "ACTIVO" : "BAJA");
        bitacora.registrar(activar ? "REACTIVA_PRODUCTO" : "BAJA_PRODUCTO", "producto", p.getId(),
                Map.of("estado", antes), Map.of("estado", p.getEstado()));
        return Mapper.producto(p);
    }

    /** Sube la foto del producto (solo JPG/PNG de hasta 2 MB) y devuelve su URL pública. */
    public String subirImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw ApiException.campo("imagen", "Selecciona una imagen");
        }
        if (archivo.getSize() > 2 * 1024 * 1024) {
            throw ApiException.campo("imagen", "Solo imágenes JPG o PNG de hasta 2 MB");
        }
        String ext;
        try (InputStream in = archivo.getInputStream()) {
            byte[] cabecera = in.readNBytes(8);
            ext = tipoImagen(cabecera);
        } catch (IOException e) {
            throw ApiException.campo("imagen", "No se pudo leer la imagen");
        }
        if (ext == null) {
            throw ApiException.campo("imagen", "Solo imágenes JPG o PNG de hasta 2 MB");
        }
        try {
            Path dir = Path.of(props.uploads().directorio(), "productos").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String nombre = UUID.randomUUID() + "." + ext;
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, dir.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/productos/" + nombre;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen", e);
        }
    }

    private static String tipoImagen(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
        }
        return null;
    }

    private void validar(ProductoRequest r) {
        if (!CATEGORIAS.contains(r.categoria())) {
            throw ApiException.campo("categoria", "Categoría no válida");
        }
        if (r.precioLista().compareTo(r.precioContado()) < 0) {
            throw ApiException.campo("precioLista", "El precio de lista debe ser mayor o igual al precio contado");
        }
        if (!r.permiteFinMes() && !r.permiteCuotas()) {
            throw ApiException.campo("modalidades", "Marca al menos una modalidad: fin de mes o cuotas");
        }
    }

    private void copiar(ProductoRequest r, Producto p) {
        p.setNombre(r.nombre().trim());
        p.setCategoria(r.categoria());
        p.setSku(r.sku().trim().toUpperCase());
        p.setMarca(Validaciones.vacioANull(r.marca()));
        p.setProveedor(Validaciones.vacioANull(r.proveedor()));
        p.setUnidad(r.unidad() == null || r.unidad().isBlank() ? "UND" : r.unidad().trim().toUpperCase());
        p.setPrecioContado(r.precioContado());
        p.setPrecioLista(r.precioLista());
        p.setPermiteFinMes(r.permiteFinMes());
        p.setPermiteCuotas(r.permiteCuotas());
        p.setImagenUrl(Validaciones.vacioANull(r.imagenUrl()));
    }

    Producto buscar(Integer id) {
        return productos.findByIdAndTiendaId(id, UsuarioActual.tienda())
                .orElseThrow(() -> ApiException.noEncontrado("Producto no encontrado"));
    }
}

