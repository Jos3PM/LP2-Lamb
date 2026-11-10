package pe.edu.upeu.fia.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface TeacherAccountRepository extends JpaRepository<TeacherAccount, String> {
    Optional<TeacherAccount> findByDocenteId(Long docenteId);
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCorreoIgnoreCase(String correo);
}
