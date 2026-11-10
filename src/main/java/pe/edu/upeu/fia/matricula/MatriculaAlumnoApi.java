package pe.edu.upeu.fia.matricula;

import java.math.BigDecimal;
import java.util.List;

/** Contrato público del módulo de matrícula para el portal y las cuentas de alumnos. */
public interface MatriculaAlumnoApi {
    Long registrarAlumnoId(String codigo, String nombre, String correo);
    Object alumno(Long estudianteId);
    Object perfilAlumno(Long estudianteId, String correo, String telefono, String apoderado);
    Object eleccionesAlumno(Long estudianteId);
    Object editarCarreraAlumno(Long estudianteId, String periodo, Long planId);
    Object aprobados(Long estudianteId);
    Object cuposAlumno(Long estudianteId, String periodo);
    Object solicitudesAlumno(Long estudianteId);
    Object matricularAlumno(Long estudianteId, String periodo, List<Long> cursos);
    Object solicitarAlumno(Long estudianteId, String periodo, List<Long> cursos);
    Object matriculasAlumno(Long estudianteId);
    Object contrato(Long matriculaId, Long estudianteId);
    Object pagarAlumno(Long matriculaId, Long estudianteId, BigDecimal monto, String metodo,
                      String referencia, String modalidad, Integer cuotas, String voucher);
    Object pagosAlumno(Long matriculaId, Long estudianteId);
    Object comprobanteAlumno(Long matriculaId, Long pagoId, Long estudianteId);
}
