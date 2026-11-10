package pe.edu.upeu.fia.carga.internal;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.boot.*;
import org.springframework.beans.factory.annotation.Value;

@Component 
@Order(40) 
class IniciarAgenda implements ApplicationRunner {
 private final AgendaLockRepository locks;private final CargaService service;private final boolean generate;
 IniciarAgenda(AgendaLockRepository l,CargaService s,@Value("${app.agenda.generar:false}") boolean g){locks=l;service=s;generate=g;}
 public void run(ApplicationArguments args){if(!locks.existsById(1L))locks.saveAndFlush(new AgendaLock(1L));if(generate)service.generar();}
}
