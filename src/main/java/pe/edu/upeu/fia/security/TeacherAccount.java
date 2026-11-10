package pe.edu.upeu.fia.security;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "FIA_PROFESOR_CUENTA", uniqueConstraints = {
    @UniqueConstraint(name = "UK_FIA_PROF_DOCENTE", columnNames = "docente_id"),
    @UniqueConstraint(name = "UK_FIA_PROF_CODIGO", columnNames = "codigo"),
    @UniqueConstraint(name = "UK_FIA_PROF_CORREO", columnNames = "correo")
})
class TeacherAccount {
    @Id @Column(length = 80) String username;
    @Column(name = "password_hash", nullable = false, length = 100) String passwordHash;
    @Column(name = "docente_id", nullable = false) Long docenteId;
    @Column(nullable = false, length = 30) String codigo;
    @Column(nullable = false, length = 180) String correo;
    @Column(nullable = false) boolean activo = true;
    @Column(name = "creado_por", nullable = false, length = 80) String creadoPor;
    @Column(name = "modificado_por", nullable = false, length = 80) String modificadoPor;
    @Column(name = "creado_en", nullable = false) LocalDateTime creadoEn;
    @Column(name = "modificado_en", nullable = false) LocalDateTime modificadoEn;
    protected TeacherAccount() {}
}
