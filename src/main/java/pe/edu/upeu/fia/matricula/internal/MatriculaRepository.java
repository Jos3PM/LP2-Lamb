package pe.edu.upeu.fia.matricula.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

interface EstudianteRepo extends JpaRepository<Estudiante,Long>{Optional<Estudiante> findByCodigoIgnoreCase(String codigo);boolean existsByCodigoIgnoreCase(String codigo);
 Page<Estudiante> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(String nombre,String codigo,Pageable page);
}
interface EleccionRepo extends JpaRepository<EleccionCarrera,Long>{Optional<EleccionCarrera> findByEstudianteIdAndPeriodo(Long estudianteId,String periodo);List<EleccionCarrera> findAllByOrderByFechaDesc();List<EleccionCarrera> findAllByEstudianteId(Long estudianteId);}
interface TarifaRepo extends JpaRepository<Tarifa,Long>{Optional<Tarifa> findByPeriodoIgnoreCase(String periodo);}
interface MatriculaRepo extends JpaRepository<Matricula,Long>{Optional<Matricula> findByEstudianteIdAndPeriodo(Long estudianteId,String periodo);List<Matricula> findAllByOrderByFechaDesc();List<Matricula> findByEstudianteIdOrderByFechaDesc(Long estudianteId);boolean existsByEstudianteId(Long estudianteId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select m from Matricula m where m.id=:id") Optional<Matricula> findLockedById(@Param("id") Long id);}
interface MatriculaDetalleRepo extends JpaRepository<MatriculaDetalle,Long>{List<MatriculaDetalle> findByMatriculaId(Long matriculaId);
 @Query("select count(d) from MatriculaDetalle d join Matricula m on m.id=d.matriculaId where d.cursoId=:cursoId and lower(m.periodo)=lower(:periodo)") long ocupados(@Param("cursoId") Long cursoId,@Param("periodo") String periodo);
}
interface PagoRepo extends JpaRepository<Pago,Long>{List<Pago> findByMatriculaIdOrderByFechaDesc(Long matriculaId);}


interface AprobadoRepo extends JpaRepository<CursoAprobado,Long>{List<CursoAprobado> findAllByEstudianteId(Long estudianteId);boolean existsByEstudianteIdAndCursoId(Long estudianteId,Long cursoId);}

interface SolicitudRepo extends JpaRepository<SolicitudMatricula,Long>{Optional<SolicitudMatricula> findByEstudianteIdAndPeriodo(Long estudianteId,String periodo);List<SolicitudMatricula> findAllByOrderByFechaDesc();List<SolicitudMatricula> findAllByEstudianteId(Long estudianteId);
 @Query("select s from SolicitudMatricula s where s.estado='PENDIENTE' and lower(s.periodo)=lower(:periodo)") List<SolicitudMatricula> pendientesPeriodo(@Param("periodo") String periodo);
}
