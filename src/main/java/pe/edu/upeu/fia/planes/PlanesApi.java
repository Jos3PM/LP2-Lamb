package pe.edu.upeu.fia.planes;
import java.util.List;
public interface PlanesApi {
 List<PlanDto> planes(); List<CursoDto> cursos(Long planId,Integer ciclo,String buscar,String orden);
 List<DocenteDto> docentes(String buscar); CursoDto curso(Long id); PlanDto plan(Long id); DocenteDto docente(Long id);
 void bloquearCursos(List<Long> ids);
}
