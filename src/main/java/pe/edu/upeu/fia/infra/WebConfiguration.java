package pe.edu.upeu.fia.infra;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.*;

@Configuration class WebConfiguration implements WebMvcConfigurer {
 @Value("${app.cors.origins}") String origins;
 public void addCorsMappings(CorsRegistry r){r.addMapping("/api/**").allowedOrigins(origins.split(",")).allowedMethods("GET","POST","PUT","DELETE","OPTIONS").allowedHeaders("Content-Type","X-Request-ID").exposedHeaders("X-Request-ID");}
}
