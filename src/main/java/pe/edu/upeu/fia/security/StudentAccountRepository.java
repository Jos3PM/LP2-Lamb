package pe.edu.upeu.fia.security;
import org.springframework.data.jpa.repository.JpaRepository;
interface StudentAccountRepository extends JpaRepository<StudentAccount,String> { boolean existsByEstudianteId(Long estudianteId); }
