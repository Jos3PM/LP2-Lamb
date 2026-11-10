package pe.edu.upeu.fia.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.fia.carga.CargasProfesorApi;
import pe.edu.upeu.fia.planes.PlanesApi;

@RestController
@RequestMapping("/api/v1/profesores/cuentas")
class TeacherAccountController {
    record CreateInput(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{4,80}") String username,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotNull @Positive Long docenteId,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{2,30}") String codigo,
        @NotBlank @Email @Size(max = 180) String correo) {}
    record UpdateInput(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{2,30}") String codigo,
        @NotBlank @Email @Size(max = 180) String correo,
        @Size(min = 12, max = 72) String password,
        @NotNull Boolean activo) {}

    private final TeacherAccountService service;
    TeacherAccountController(TeacherAccountService service) { this.service = service; }

    @GetMapping List<TeacherAccountService.View> list() { return service.list(); }
    @GetMapping("/{username}") TeacherAccountService.View one(@PathVariable String username) { return service.one(username); }
    @PostMapping ResponseEntity<TeacherAccountService.View> create(@Valid @RequestBody CreateInput input, Principal actor) {
        var result = service.create(input.username(), input.password(), input.docenteId(), input.codigo(), input.correo(), actor.getName());
        return ResponseEntity.created(URI.create("/api/v1/profesores/cuentas/" + result.username())).body(result);
    }
    @PutMapping("/{username}") TeacherAccountService.View update(@PathVariable String username,
            @Valid @RequestBody UpdateInput input, Principal actor) {
        return service.update(username, input.codigo(), input.correo(), input.password(), input.activo(), actor.getName());
    }
}

@RestController
@RequestMapping("/api/v1/profesor")
class TeacherController {
    record Profile(String username, Long docenteId, String codigo, String nombre, String correo,
                   String escuela, String curso, int asignaciones) {}
    private final TeacherAccountService accounts;
    private final PlanesApi planes;
    private final CargasProfesorApi cargas;
    TeacherController(TeacherAccountService accounts, PlanesApi planes, CargasProfesorApi cargas) {
        this.accounts = accounts; this.planes = planes; this.cargas = cargas;
    }
    @GetMapping("/perfil") Profile profile(Principal principal) {
        TeacherAccount a = accounts.active(principal.getName());
        var d = planes.docente(a.docenteId);
        return new Profile(a.username, a.docenteId, a.codigo, d.name(), a.correo, d.school(), d.course(),
                cargas.asignaciones(a.docenteId).size());
    }
    @GetMapping("/asignaciones") List<CargasProfesorApi.Asignacion> assignments(Principal principal) {
        return cargas.asignaciones(accounts.active(principal.getName()).docenteId);
    }
}
