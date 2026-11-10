package pe.edu.upeu.fia.planes;
import java.util.List;
public record CursoDto(Long id,Long planId,String school,int cycle,String name,int credits,int ht,int hp,int hnp,int total,String prerequisites,String notes,String source,int page,List<DocenteDto> teachers,String horario,int cupos) {}
