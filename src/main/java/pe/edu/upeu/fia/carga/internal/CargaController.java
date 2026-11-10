package pe.edu.upeu.fia.carga.internal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import java.time.*;
import static pe.edu.upeu.fia.carga.internal.CargaDtos.*;
@RestController 
@RequestMapping("/api/v1/cargas") 
class CargaController {
 private final CargaService service;CargaController(CargaService s){service=s;}
 @PostMapping ResponseEntity<Dto> crear(
    @Valid 
    @RequestBody Input i){var d=service.crear(i);return ResponseEntity.created(URI.create("/api/v1/cargas/"+d.id())).body(d);}
 @PutMapping("/{id}") Dto actualizar(
    @PathVariable Long id,
    @Valid 
    @RequestBody Input i){return service.actualizar(id,i);}
 @PostMapping("/generar") List<Dto> generar(){return service.generar();}
 @GetMapping List<Dto> listar(
    @RequestParam(required=false) Long planId,@RequestParam(required=false) String semestre,@RequestParam(required=false) LocalDate desde,@RequestParam(required=false) LocalDate hasta){return service.listar(planId,semestre,desde,hasta);}
 @GetMapping("/{id}") Dto uno(
    @PathVariable Long id){return service.uno(id);}
 @GetMapping("/{id}/auditoria") List<CargaService.AuditDto> auditoria(@PathVariable Long id){return service.auditoria(id);}
 @GetMapping("/resumen") Map<String,Object> resumen(
    @RequestParam(required=false) Long planId,
    @RequestParam(required=false) String semestre,
    @RequestParam(required=false) LocalDate desde,
    @RequestParam(required=false) LocalDate hasta){var list=service.listar(planId,semestre,desde,hasta);return Map.of("cargas",list.size(),"detalles",list.stream().mapToInt(c->c.detalles().size()).sum(),"horas",list.stream().mapToInt(Dto::totalHoras).sum());}
}
