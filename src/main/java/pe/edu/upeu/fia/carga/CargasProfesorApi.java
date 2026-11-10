package pe.edu.upeu.fia.carga;

import java.util.List;

/** Vista de solo lectura de la carga académica para la cuenta del profesor. */
public interface CargasProfesorApi {
    record Bloque(int dia, int inicio, int fin) {}
    record Asignacion(Long cargaId, Long detalleId, Long planId, Long cursoId, String curso,
                      String semestre, String sede, int grupo, String ambiente,
                      String modalidad, int cupos, int horas, List<Bloque> bloques) {}
    List<Asignacion> asignaciones(Long docenteId);
}
