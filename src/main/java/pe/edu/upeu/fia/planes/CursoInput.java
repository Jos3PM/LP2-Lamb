package pe.edu.upeu.fia.planes;
import jakarta.validation.constraints.*;

public record CursoInput(
    @NotNull Long planId,
    @Min(1) 
    @Max(10) int cycle,
    @NotBlank 
    @Size(max=300) String name,
    @Min(0) @Max(100) int credits,
    @Min(0) @Max(80) int ht,
    @Min(0) @Max(80) int hp,
    @Min(0) @Max(80) int hnp,
    @Size(max=3000) String prerequisites,@Size(max=300) String horario,@Min(1) @Max(100000) Integer cupos) {
 public CursoInput(Long planId,int cycle,String name,int credits,int ht,int hp,int hnp,String prerequisites){this(planId,cycle,name,credits,ht,hp,hnp,prerequisites,"",30);}
}
