package pe.edu.upeu.fia.carga.internal;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "FIA_CARGA_AUDIT")
class CargaAudit {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "carga_audit_seq")
    @SequenceGenerator(name = "carga_audit_seq", sequenceName = "FIA_CARGA_AUDIT_SEQ", allocationSize = 1)
    Long id;
    @Column(name = "carga_id", nullable = false) Long cargaId;
    @Column(nullable = false, length = 20) String accion;
    @Column(nullable = false, length = 80) String actor;
    @Column(nullable = false) LocalDateTime fecha;
    @Column(length = 300) String resumen;
    protected CargaAudit() {}
    CargaAudit(Long cargaId, String accion, String actor, String resumen) {
        this.cargaId = cargaId; this.accion = accion; this.actor = actor; this.fecha = LocalDateTime.now();
        this.resumen = resumen;
    }
}
