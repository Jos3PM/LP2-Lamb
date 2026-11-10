package pe.edu.upeu.fia.carga.internal;
import jakarta.persistence.*;
import org.springframework.data.jpa.repository.*;

@Entity 
@Table(name="FIA_AGENDA_LOCK") 
class AgendaLock {@Id Long id;protected AgendaLock(){} AgendaLock(Long i){id=i;}}
interface AgendaLockRepository extends JpaRepository<AgendaLock,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @org.springframework.data.jpa.repository.Query("select a from AgendaLock a where a.id=1") AgendaLock bloquear();
}

