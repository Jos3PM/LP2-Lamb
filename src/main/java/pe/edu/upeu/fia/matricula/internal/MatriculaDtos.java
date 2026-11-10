package pe.edu.upeu.fia.matricula.internal;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

final class MatriculaDtos {
 private MatriculaDtos(){}
 record EstudianteInput(@NotBlank @Size(max=30) String codigo,@NotBlank @Size(max=180) String nombre,@Email @Size(max=180) String correo){}
 record EstudianteDto(Long id,String codigo,String nombre,String correo,String telefono,String apoderado){}
 record EleccionInput(@NotNull Long estudianteId,@NotNull Long planId,@NotBlank @Size(max=30) String periodo){}
 record EleccionDto(Long id,Long estudianteId,String estudiante,Long planId,String carrera,String periodo,LocalDateTime fecha){}
 record TarifaInput(@NotBlank @Size(max=30) String periodo,@NotNull @DecimalMin("0.00") BigDecimal base,@NotNull @DecimalMin("0.00") BigDecimal porCredito,@NotBlank @Pattern(regexp="[A-Z]{3}") String moneda){}
 record TarifaDto(String periodo,BigDecimal base,BigDecimal porCredito,String moneda,LocalDateTime actualizado){}
 record MatriculaInput(@NotNull Long estudianteId,@NotNull Long planId,@NotBlank @Size(max=30) String periodo,@NotEmpty List<@NotNull Long> cursos){}
 record DetalleDto(Long cursoId,String curso,int creditos){}
 record MatriculaDto(Long id,Long estudianteId,String estudiante,Long planId,String carrera,String periodo,String estado,int creditos,BigDecimal monto,BigDecimal pagado,BigDecimal saldo,String moneda,LocalDateTime fecha,List<DetalleDto> cursos){}
 record MatriculaResumenDto(Long id,Long estudianteId,String estudiante,Long planId,String carrera,String periodo,String estado,int creditos,BigDecimal monto,BigDecimal pagado,BigDecimal saldo,String moneda,LocalDateTime fecha){}
 record Pagina<T>(List<T> items,long total,int pagina,int tamano,int paginas){}
 record SolicitudInput(@NotBlank String periodo,@NotEmpty List<@NotNull Long> cursos){}
 record SolicitudDto(Long id,Long estudianteId,String estudiante,Long planId,String carrera,String periodo,List<Long> cursos,int creditos,BigDecimal monto,String moneda,String estado,LocalDateTime fecha){}
 record PagoInput(@NotNull @DecimalMin(value="0.01") BigDecimal monto,@NotBlank @Size(max=30) String metodo,@Size(max=100) String referencia){}
 record PagoDto(Long id,Long matriculaId,BigDecimal monto,String metodo,String referencia,LocalDateTime fecha,String estado,boolean tieneVoucher,String observacion){}
 record PagoPendienteDto(Long id,Long matriculaId,String estudiante,String codigo,String carrera,String periodo,BigDecimal monto,String moneda,String metodo,String referencia,LocalDateTime fecha,boolean tieneVoucher){}
 record RevisionPagoInput(@Size(max=300) String observacion){}
}
