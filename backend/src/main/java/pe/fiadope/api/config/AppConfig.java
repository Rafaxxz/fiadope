package pe.fiadope.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class AppConfig implements WebMvcConfigurer {

    private final AppProperties props;

    public AppConfig(AppProperties props) {
        this.props = props;
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(props.zonaHoraria()));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ruta = Path.of(props.uploads().directorio()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(ruta);
    }

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("FiadoPe API").version("1.0.0")
                        .description("Cuaderno digital de fiados: tiendas, catálogo, clientes, compras, cortes, pagos y bitácora."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}

