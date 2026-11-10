package pe.edu.upeu.fia.matricula.internal;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import static pe.edu.upeu.fia.matricula.internal.MatriculaDtos.*;

@RestController
@RequestMapping("/api/v1/reportes")
class ReportesController {
    private final MatriculaService service;
    ReportesController(MatriculaService service){this.service=service;}

    @GetMapping("/matriculas")
    Map<String,Object> matriculas(@RequestParam(required=false) String periodo,
                                   @RequestParam(required=false) String estado,
                                   @RequestParam(required=false) Long planId,
                                   @RequestParam(required=false) String buscar,
                                   @RequestParam(defaultValue="0") int pagina,
                                   @RequestParam(defaultValue="25") int tamano){
        MatriculaController.validarPagina(pagina,tamano);
        return service.reporteMatriculas(periodo,estado,planId,buscar,pagina,tamano);
    }
}
