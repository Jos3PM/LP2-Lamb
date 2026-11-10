package pe.edu.upeu.fia.planes.internal;
import org.springframework.data.jpa.repository.*;

interface PlanRepository extends JpaRepository<Plan,Long> {}
interface CursoRepository extends JpaRepository<Curso,Long>,JpaSpecificationExecutor<Curso> {
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select c from Curso c where c.id=:id")
 java.util.Optional<Curso> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
}
interface DocenteRepository extends JpaRepository<Docente,Long> {}
