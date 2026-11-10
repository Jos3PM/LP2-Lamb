package pe.edu.upeu.fia.security;
import jakarta.persistence.*;
@Entity @Table(name="FIA_ALUMNO_CUENTA")
class StudentAccount {
 @Id @Column(length=80) String username;
 @Column(nullable=false,length=100) String passwordHash;
 @Column(nullable=false) Long estudianteId;
 @Column Boolean puedeEditarCarrera=false;
 protected StudentAccount() {}
}

