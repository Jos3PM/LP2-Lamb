package pe.edu.upeu.fia.security;

import java.security.Principal;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.fia.matricula.MatriculaAlumnoApi;

@RestController
@RequestMapping("/api/v1/alumno")
public class AlumnoController {
  private final StudentAccountService accounts; private final MatriculaAlumnoApi matriculas;
  public AlumnoController(StudentAccountService a,MatriculaAlumnoApi m){accounts=a;matriculas=m;}
  public record PerfilInput(@NotBlank @Email @Size(max=180) String correo,
    @NotBlank @Pattern(regexp="[+0-9() -]{7,30}",message="usa entre 7 y 30 caracteres de teléfono") String telefono,
    @NotBlank @Size(max=180) String apoderado) {}
  @GetMapping("/perfil") Object perfil(Principal principal){return matriculas.alumno(accounts.studentId(principal.getName()));}
  @PutMapping("/perfil") Object perfil(@Valid @RequestBody PerfilInput input,Principal principal){return matriculas.perfilAlumno(accounts.studentId(principal.getName()),input.correo(),input.telefono(),input.apoderado());}
  public record PagoInput(java.math.BigDecimal monto,String metodo,String referencia,String modalidad,Integer cuotas,String voucher) {}
  @PostMapping("/matriculas/{id}/pagos") Object pagar(@PathVariable Long id,@RequestBody PagoInput input,Principal principal){return matriculas.pagarAlumno(id,accounts.studentId(principal.getName()),input.monto(),input.metodo(),input.referencia(),input.modalidad(),input.cuotas(),input.voucher());}
  @GetMapping("/matriculas/{id}/pagos") Object pagos(@PathVariable Long id,Principal principal){return matriculas.pagosAlumno(id,accounts.studentId(principal.getName()));}
  @GetMapping("/matriculas/{id}/pagos/{pagoId}/comprobante") Object comprobante(@PathVariable Long id,@PathVariable Long pagoId,Principal principal){return matriculas.comprobanteAlumno(id,pagoId,accounts.studentId(principal.getName()));}
  @GetMapping("/carrera") Object carrera(Principal principal){return Map.of("puedeEditar",accounts.puedeEditarCarrera(principal.getName()),"elecciones",matriculas.eleccionesAlumno(accounts.studentId(principal.getName())));}
  public record CarreraInput(@NotNull Long planId,@NotBlank String periodo) {}
  @PutMapping("/carrera") Object editarCarrera(@Valid @RequestBody CarreraInput input,Principal principal){
   if(!accounts.puedeEditarCarrera(principal.getName()))throw new org.springframework.security.access.AccessDeniedException("El administrador no autorizó editar la carrera");
   return matriculas.editarCarreraAlumno(accounts.studentId(principal.getName()),input.periodo(),input.planId());
  }
  @GetMapping("/aprobados") Object aprobados(Principal principal){return matriculas.aprobados(accounts.studentId(principal.getName()));}
  @GetMapping("/cupos") Object cupos(@RequestParam String periodo,Principal principal){return matriculas.cuposAlumno(accounts.studentId(principal.getName()),periodo);}
  @GetMapping("/solicitudes") Object solicitudes(Principal principal){return matriculas.solicitudesAlumno(accounts.studentId(principal.getName()));}
  public record SolicitudInput(@NotBlank String periodo,@NotEmpty List<@NotNull Long> cursos){}
  @PostMapping("/matriculas") Object matricular(@Valid @RequestBody SolicitudInput input,Principal principal){return matriculas.matricularAlumno(accounts.studentId(principal.getName()),input.periodo(),input.cursos());}
  @PostMapping("/solicitudes") Object solicitar(@Valid @RequestBody SolicitudInput input,Principal principal){return matriculas.solicitarAlumno(accounts.studentId(principal.getName()),input.periodo(),input.cursos());}
  @GetMapping("/matriculas") Object misMatriculas(Principal principal){return matriculas.matriculasAlumno(accounts.studentId(principal.getName()));}
  @GetMapping("/matriculas/{id}/contrato") Object contrato(@PathVariable Long id,Principal principal){return matriculas.contrato(id,accounts.studentId(principal.getName()));}
}
