package pe.edu.upeu.fia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class HttpIntegrationTests {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void configureMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("cachePolicy", jakarta.servlet.Filter.class),
                context.getBean("requestLog", jakarta.servlet.Filter.class))
            .build();
    }

    @Test
    void validaDtoYDevuelve400() throws Exception {
        mvc.perform(post("/api/v1/cursos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\":37,\"cycle\":0,\"name\":\"\",\"credits\":-1,\"ht\":0,\"hp\":0,\"hnp\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(header().exists("X-Request-ID"));
    }

    @Test void rechazaDesplazamientoDePaginaQueDesbordaEntero() throws Exception {
        mvc.perform(get("/api/v1/matriculas/pagina?pagina=2147483647&tamano=100"))
            .andExpect(status().isBadRequest());
    }

    @Test void logNoIncluyeParametroPersonalNiRutaConcreta() throws Exception {
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger("pe.edu.upeu.fia.infra.RequestLog");
        var appender=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();logger.addAppender(appender);
        try {
            mvc.perform(get("/api/v1/estudiantes/pagina").param("buscar","correo-personal@example.test"))
                .andExpect(status().isOk());
            String messages=appender.list.stream().map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage)
                .collect(java.util.stream.Collectors.joining("\n"));
            org.assertj.core.api.Assertions.assertThat(messages).contains("route=/api/v1/estudiantes/pagina")
                .doesNotContain("correo-personal@example.test");
        } finally {logger.detachAppender(appender);appender.stop();}
    }

    @Test
    void permiteCorsDelOrigenConfigurado() throws Exception {
        mvc.perform(options("/api/v1/cursos")
                .header("Origin", "http://localhost:8080")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"));
    }

    @Test
    void rechazaOrigenNoConfigurado() throws Exception {
        mvc.perform(options("/api/v1/cursos")
                .header("Origin", "https://otro.example")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());
    }

    @Test
    void entregaInterfazYEstado() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/portal/"));
        mvc.perform(get("/portal/")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/estado"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("OK")));
    }

    @Test void cacheaSoloBundlesVersionados() throws Exception {
        String bundle;
        try (var files = java.nio.file.Files.list(java.nio.file.Path.of("src/main/resources/static/portal"))) {
            bundle = files.map(p -> p.getFileName().toString())
                .filter(name -> name.matches("main-[A-Za-z0-9_-]{8,}\\.js"))
                .findFirst().orElseThrow();
        }
        mvc.perform(get("/portal/index.html"))
            .andExpect(header().string("Cache-Control", "no-store, max-age=0"));
        mvc.perform(get("/portal/" + bundle))
            .andExpect(header().string("Cache-Control", "public, max-age=31536000, immutable"));
        mvc.perform(get("/api/v1/estado"))
            .andExpect(header().string("Cache-Control", "no-store, max-age=0"));
    }
}
