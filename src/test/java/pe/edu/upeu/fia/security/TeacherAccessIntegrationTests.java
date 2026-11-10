package pe.edu.upeu.fia.security;

import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pe.edu.upeu.fia.planes.PlanesApi;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.security.enabled=true", "app.security.admin.password=TestAdminPassword2026!"})
@ActiveProfiles("test")
class TeacherAccessIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired TeacherAccountService accounts;
    @Autowired TeacherAccountRepository repository;
    @Autowired PlanesApi planes;
    MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("springSecurityFilterChain", Filter.class),
                        context.getBean("requestLog", Filter.class)).build();
    }

    private record Session(MockHttpSession session, String csrf) {}
    private Session login(String username, String password) throws Exception {
        var status = mvc.perform(get("/api/v1/auth/status")).andExpect(status().isOk()).andReturn();
        var csrfCookie = status.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        var result = mvc.perform(post("/api/v1/auth/login").cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue()).contentType("application/json")
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        var current = mvc.perform(get("/api/v1/auth/status").session(session)).andExpect(status().isOk()).andReturn();
        return new Session(session, current.getResponse().getCookie("XSRF-TOKEN").getValue());
    }

    @Test void teacherSeLimitaASuPerfilYNoPuedeConsultarAdministracion() throws Exception {
        String name = "profe" + UUID.randomUUID().toString().substring(0, 8);
        String password = "ProfesorTemporal2026!";
        var docente = planes.docentes("").getFirst();
        accounts.create(name, password, docente.id(), "P" + name, name + "@example.test", "admin-test");
        Session teacher = login(name, password);

        mvc.perform(get("/api/v1/profesor/perfil").session(teacher.session()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(name))
            .andExpect(jsonPath("$.docenteId").value(docente.id()));
        mvc.perform(get("/api/v1/profesor/asignaciones").session(teacher.session()))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/profesores/cuentas").session(teacher.session()))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/docentes").session(teacher.session()))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/alumno/perfil").session(teacher.session()))
            .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics").session(teacher.session()))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/cargas/1/auditoria").session(teacher.session()))
            .andExpect(status().isForbidden());
    }

    @Test void validaCamposYDesactivaAcceso() throws Exception {
        Session admin = login("admin", "TestAdminPassword2026!");
        mvc.perform(post("/api/v1/profesores/cuentas").session(admin.session()).cookie(new Cookie("XSRF-TOKEN", admin.csrf()))
                .header("X-XSRF-TOKEN", admin.csrf()).contentType("application/json")
                .content("{\"username\":\"x\",\"password\":\"corta\",\"docenteId\":1,\"codigo\":\"!\",\"correo\":\"invalido\"}"))
            .andExpect(status().isBadRequest());

        String name = "profe" + UUID.randomUUID().toString().substring(0, 8);
        String password = "ProfesorTemporal2026!";
        var docente = planes.docentes("").get(1);
        accounts.create(name, password, docente.id(), "P" + name, name + "@example.test", "admin-test");
        assertThat(repository.findById(name).orElseThrow().passwordHash).isNotEqualTo(password).startsWith("$2");
        accounts.update(name, "P" + name, name + "@example.test", null, false, "admin-test");
        assertThatThrownBy(() -> accounts.active(name)).isInstanceOf(AccessDeniedException.class);
        assertThat(accounts.one(name).activo()).isFalse();
    }
}
