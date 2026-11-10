package pe.edu.upeu.fia.carga.internal;
import org.springframework.data.jpa.repository.*;
interface CargaRepository extends JpaRepository<Carga,Long>,JpaSpecificationExecutor<Carga> {}
interface DetalleCargaRepository extends JpaRepository<DetalleCarga,Long> {
 @EntityGraph(attributePaths={"carga","bloques"})
 java.util.List<DetalleCarga> findByDocenteIdOrderById(Long docenteId);
}
interface CargaAuditRepository extends JpaRepository<CargaAudit,Long> {
 java.util.List<CargaAudit> findByCargaIdOrderByFechaDesc(Long cargaId);
}
