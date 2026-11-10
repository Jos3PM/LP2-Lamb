package pe.edu.upeu.fia.matricula.internal;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static pe.edu.upeu.fia.matricula.internal.MatriculaDtos.*;

@RestController @RequestMapping("/api/v1")
class MatriculaController {
 private final MatriculaService service;
 MatriculaController(MatriculaService service){this.service=service;}
 @GetMapping("/estudiantes/{id}/aprobados") List<Long> aprobados(@PathVariable Long id){return service.aprobados(id);}
 @PostMapping("/estudiantes/{id}/aprobados/{cursoId}") void aprobar(@PathVariable Long id,@PathVariable Long cursoId){service.aprobar(id,cursoId);}
 @GetMapping("/estudiantes") List<EstudianteDto> estudiantes(){return service.estudiantes();}
 @GetMapping("/estudiantes/pagina") Pagina<EstudianteDto> estudiantesPagina(@RequestParam(defaultValue="") String buscar,@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="25") int tamano){validarPagina(pagina,tamano);return service.estudiantesPagina(buscar,pagina,tamano);}
 @PostMapping("/estudiantes") EstudianteDto estudiante(@Valid @RequestBody EstudianteInput i){return service.crearEstudiante(i);}
 @PutMapping("/estudiantes/{id}") EstudianteDto editarEstudiante(@PathVariable Long id,@Valid @RequestBody EstudianteInput i){return service.editarEstudiante(id,i);}
 @DeleteMapping("/estudiantes/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) void borrarEstudiante(@PathVariable Long id){service.borrarEstudiante(id);}
 @GetMapping("/elecciones-carrera") List<EleccionDto> elecciones(){return service.elecciones();}
 @PostMapping("/elecciones-carrera") EleccionDto elegir(@Valid @RequestBody EleccionInput i){return service.elegir(i);}
 @GetMapping("/tarifas-matricula") List<TarifaDto> tarifas(){return service.tarifas();}
 @PutMapping("/tarifas-matricula") TarifaDto tarifa(@Valid @RequestBody TarifaInput i){return service.configurar(i);}
 @GetMapping("/solicitudes-matricula") List<SolicitudDto> solicitudes(){return service.solicitudes();}
 @PostMapping("/solicitudes-matricula/{id}/generar") MatriculaDto generarSolicitud(@PathVariable Long id){return service.generarSolicitud(id);}
 @PostMapping("/solicitudes-matricula/{id}/rechazar") SolicitudDto rechazarSolicitud(@PathVariable Long id){return service.rechazarSolicitud(id);}
 @GetMapping("/matriculas") List<MatriculaDto> matriculas(){return service.matriculas();}
 @GetMapping("/matriculas/pagina") Pagina<MatriculaResumenDto> matriculasPagina(@RequestParam(defaultValue="") String periodo,@RequestParam(defaultValue="") String estado,@RequestParam(required=false) Long planId,@RequestParam(defaultValue="") String buscar,@RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="25") int tamano){validarPagina(pagina,tamano);return service.matriculasPagina(periodo,estado,planId,buscar,pagina,tamano);}
 @GetMapping("/matriculas/{id}") MatriculaDto matricula(@PathVariable Long id){return service.matricula(id);}
 @GetMapping("/planes/{id}/cupos") java.util.Map<Long,Long> cupos(@PathVariable Long id,@RequestParam String periodo){return service.cupos(id,periodo);}
 @PostMapping("/matriculas") MatriculaDto matricular(@Valid @RequestBody MatriculaInput i){return service.matricular(i);}
 @GetMapping("/pagos/pendientes") List<PagoPendienteDto> pagosPendientes(){return service.pagosPendientes();}
 @GetMapping("/matriculas/{id}/pagos/{pagoId}/voucher") org.springframework.http.ResponseEntity<byte[]> voucher(@PathVariable Long id,@PathVariable Long pagoId){var image=service.voucherPago(id,pagoId);return org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType(image.mime())).body(image.bytes());}
 @PostMapping("/matriculas/{id}/pagos/{pagoId}/confirmar") PagoDto confirmarPago(@PathVariable Long id,@PathVariable Long pagoId){return service.confirmarPago(id,pagoId);}
 @PostMapping("/matriculas/{id}/pagos/{pagoId}/rechazar") PagoDto rechazarPago(@PathVariable Long id,@PathVariable Long pagoId,@Valid @RequestBody RevisionPagoInput input){return service.rechazarPago(id,pagoId,input.observacion());}
 @GetMapping("/matriculas/{id}/pagos") List<PagoDto> pagos(@PathVariable Long id){return service.pagos(id);}
 @PostMapping("/matriculas/{id}/pagos") PagoDto pagar(@PathVariable Long id,@Valid @RequestBody PagoInput i){return service.pagar(id,i);}
 static void validarPagina(int pagina,int tamano){if(pagina<0||tamano<1||tamano>100||pagina>Integer.MAX_VALUE/tamano)throw new IllegalArgumentException("La página debe ser mayor o igual a 0, el tamaño debe estar entre 1 y 100 y el desplazamiento debe ser válido");}
}

