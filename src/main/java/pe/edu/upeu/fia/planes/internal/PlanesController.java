package pe.edu.upeu.fia.planes.internal;
import pe.edu.upeu.fia.planes.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
@RestController 
@RequestMapping("/api/v1") class PlanesController {
 
private final PlanesService service; private final HorariosService horarios; PlanesController(PlanesService s,HorariosService h){service=s;horarios=h;}
    @PostMapping("/planes/generar-horarios") Map<String,Object> generarHorarios(){return horarios.generarTodos();}
    @PostMapping("/planes/{id}/generar-horarios") Map<String,Object> generarHorariosPlan(@PathVariable Long id){service.plan(id);return horarios.generarPlan(id);}
    record CuposInput(int incremento){}
    @PostMapping("/cursos/{id}/aumentar-cupos") CursoDto aumentarCupos(@PathVariable Long id,@RequestBody CuposInput input){horarios.aumentarCupos(id,input.incremento());return service.curso(id);}
    @GetMapping("/docentes") List<DocenteDto> docentes(
        @RequestParam(defaultValue="") String buscar,
        @RequestParam(defaultValue="") String escuela,
        @RequestParam(defaultValue="") String curso){return service.docentes(buscar,escuela,curso);}
    @GetMapping("/planes") List<PlanDto> planes(){return service.planes();}    @PostMapping("/planes") ResponseEntity<PlanDto> crearPlan(@Valid @RequestBody PlanInput i){var p=service.guardarPlan(null,i);return ResponseEntity.created(URI.create("/api/v1/planes/"+p.id())).body(p);}
    @PutMapping("/planes/{id}") PlanDto editarPlan(@PathVariable Long id,@Valid @RequestBody PlanInput i){return service.guardarPlan(id,i);}
    @DeleteMapping("/planes/{id}") ResponseEntity<Void> borrarPlan(@PathVariable Long id){service.borrarPlan(id);return ResponseEntity.noContent().build();}
    @PostMapping("/docentes") ResponseEntity<DocenteDto> crearDocente(@Valid @RequestBody DocenteInput i){var d=service.guardarDocente(null,i);return ResponseEntity.created(URI.create("/api/v1/docentes/"+d.id())).body(d);}
    @PutMapping("/docentes/{id}") DocenteDto editarDocente(@PathVariable Long id,@Valid @RequestBody DocenteInput i){return service.guardarDocente(id,i);}
    @DeleteMapping("/docentes/{id}") ResponseEntity<Void> borrarDocente(@PathVariable Long id){service.borrarDocente(id);return ResponseEntity.noContent().build();}
    @GetMapping("/cursos") List<CursoDto> cursos(
        @RequestParam(required=false) Long planId,
        @RequestParam(required=false) Integer ciclo,
        @RequestParam(required=false) String buscar,
        @RequestParam(defaultValue="cycle") String orden){return service.cursos(planId,ciclo,buscar,orden);}
    @GetMapping("/cursos/{id}") CursoDto curso(
        @PathVariable Long id){return service.curso(id);}
    @PostMapping("/cursos") ResponseEntity<CursoDto> crear(
        @Valid 
        @RequestBody CursoInput i){var c=service.guardar(null,i);return ResponseEntity.created(URI.create("/api/v1/cursos/"+c.id())).body(c);}
    @PutMapping("/cursos/{id}") CursoDto editar(
        @PathVariable Long id,
        @Valid 
        @RequestBody CursoInput i){return service.guardar(id,i);}
    @DeleteMapping("/cursos/{id}") ResponseEntity<Void> borrar(
        @PathVariable Long id){service.borrar(id);return ResponseEntity.noContent().build();}
    @GetMapping("/cursos/resumen") Map<String,Object> resumen(
        @RequestParam Long planId){var cs=service.cursos(planId,null,null,"cycle");return Map.of("cursos",cs.size(),"creditos",cs.stream().mapToInt(CursoDto::credits).sum(),"horas",cs.stream().mapToInt(CursoDto::total).sum());}
}


