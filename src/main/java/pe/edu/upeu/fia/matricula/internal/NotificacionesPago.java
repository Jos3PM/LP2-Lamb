package pe.edu.upeu.fia.matricula.internal;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Puerto sustituible por correo o mensajería; la demo registra el intento sin enviar datos fuera. */
interface CanalNotificacion { String enviar(String destino,String asunto,String mensaje); }

@Component class CanalNotificacionDemo implements CanalNotificacion {
 public String enviar(String destino,String asunto,String mensaje){return "SIMULADO";}
}

record PagoRevisado(Long pagoId,Long matriculaId,String estado,String correo) {}

@Entity @Table(name="FIA_NOTIFICACION_INTENTO")
class NotificacionIntento {
 @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="fia_notif_seq")
 @SequenceGenerator(name="fia_notif_seq",sequenceName="FIA_NOTIF_SEQ",allocationSize=1) Long id;
 @Column(name="pago_id",nullable=false) Long pagoId;
 @Column(name="matricula_id",nullable=false) Long matriculaId;
 @Column(nullable=false,length=180) String destino;
 @Column(nullable=false,length=30) String canal;
 @Column(nullable=false,length=30) String estado;
 @Column(length=300) String resultado;
 @Column(nullable=false) LocalDateTime fecha;
 protected NotificacionIntento(){}
}

interface NotificacionIntentoRepo extends JpaRepository<NotificacionIntento,Long> {}

@Component class NotificacionesPago {
 private final NotificacionIntentoRepo intentos; private final CanalNotificacion canal;
 NotificacionesPago(NotificacionIntentoRepo intentos,CanalNotificacion canal){this.intentos=intentos;this.canal=canal;}
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void pagoRevisado(PagoRevisado event){
  NotificacionIntento intento=new NotificacionIntento();intento.pagoId=event.pagoId();intento.matriculaId=event.matriculaId();
  intento.destino=event.correo()==null||event.correo().isBlank()?"sin-correo":event.correo();intento.canal="DEMO";intento.fecha=LocalDateTime.now();
  try{intento.resultado=canal.enviar(intento.destino,"Estado del pago",event.estado());intento.estado="REGISTRADO";}
  catch(RuntimeException error){intento.estado="FALLIDO";intento.resultado=error.getClass().getSimpleName();}
  intentos.save(intento);
 }
}
