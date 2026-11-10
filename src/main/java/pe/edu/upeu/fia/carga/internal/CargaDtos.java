package pe.edu.upeu.fia.carga.internal;
import java.util.*;
import java.time.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

class CargaDtos {
 record BloqueInput(
    @Min(0) 
    @Max(6) int dia,
    @Min(0) 
    @Max(1439) int inicio,
    @Min(1) 
    @Max(1440) int fin){}
 record DetalleInput(
    @NotNull Long cursoId,Long docenteId,
    @Min(1) int grupo,
    @Min(0) Integer cupos,
    @Size(max=200) String ambiente,
    @Size(max=60) String condicion,
    @Size(max=60) String modalidad,
    @NotNull @Size(max=126) List<@Valid BloqueInput> bloques,
 @Size(max=300) String cursoNombre,
 @Min(1) 
 @Max(10) Integer ciclo,
 @Min(0) 
 @Max(100) Integer creditos,
 @Min(0) 
 @Max(80) Integer ht,
 @Min(0) 
 @Max(80) Integer hp,
 @Min(0) 
 @Max(80) Integer hnp){
 DetalleInput(Long c,Long d,int g,Integer cup,String a,String cond,String mod,List<BloqueInput> b){this(c,d,g,cup,a,cond,mod,b,null,null,null,null,null,null);}
 }
 record Input(@NotNull Long planId,@NotBlank @Size(max=50) String sede,@NotBlank @Size(max=30) String semestre,@NotBlank @Size(max=80) String contrato,@NotEmpty @Size(max=300) List<@Valid DetalleInput> detalles){}
 record DetalleDto(Long id,Long cursoId,Long docenteId,String docenteNombre,int grupo,Integer cupos,String ambiente,String condicion,String modalidad,int horas,List<BloqueInput> bloques,String cursoNombre,int ciclo,int creditos,int ht,int hp,int hnp){}
 record Dto(Long id,Long planId,String sede,String semestre,String contrato,String estado,LocalDateTime fecha,int totalHoras,List<DetalleDto> detalles){}
}
