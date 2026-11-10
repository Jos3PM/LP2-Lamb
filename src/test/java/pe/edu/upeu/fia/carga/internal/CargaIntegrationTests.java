package pe.edu.upeu.fia.carga.internal;
import pe.edu.upeu.fia.planes.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static pe.edu.upeu.fia.carga.internal.CargaDtos.*;
@SpringBootTest @ActiveProfiles("test") class CargaIntegrationTests {
 @Autowired CargaService service; @Autowired PlanesApi planes; @Autowired JdbcTemplate db;
 CursoDto curso(){return planes.cursos(37L,1,null,"name").get(0);}
 Input input(List<DetalleInput> ds){return new Input(37L,"Filial Juliaca","PRUEBA-TRANSACCION","Escuela Profesional Pregrado",ds);}
 DetalleInput detalle(Long id,int grupo){return new DetalleInput(id,null,grupo,null,null,null,null,List.of());}
 long count(String table){return db.queryForObject("SELECT COUNT(*) FROM "+table,Long.class);}
 @Test void registraCabeceraDetallesYCalculaHoras(){var c=curso();var result=service.crear(input(List.of(detalle(c.id(),1),detalle(c.id(),2))));assertThat(result.id()).isNotNull();assertThat(result.detalles()).hasSize(2);assertThat(result.totalHoras()).isEqualTo(c.total()*2);assertThat(service.uno(result.id()).detalles()).hasSize(2);assertThat(service.auditoria(result.id())).extracting(CargaService.AuditDto::accion).contains("CREAR");}
 @Test void falloEnSegundoDetalleRevierteCabeceraYPrimerDetalle(){long h=count("FIA_CARGA"),d=count("FIA_CARGA_DETALLE"),a=count("FIA_CARGA_AUDIT");assertThatThrownBy(()->service.crear(input(List.of(detalle(curso().id(),1),detalle(Long.MAX_VALUE,1))))).isInstanceOf(NoSuchElementException.class);assertThat(count("FIA_CARGA")).isEqualTo(h);assertThat(count("FIA_CARGA_DETALLE")).isEqualTo(d);assertThat(count("FIA_CARGA_AUDIT")).isEqualTo(a);}
 @Test void rechazaCursoDeOtroPlan(){var otro=planes.cursos(1L,1,null,"name").get(0);assertThatThrownBy(()->service.crear(input(List.of(detalle(otro.id(),1))))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no pertenece");}
 @Test void rechazaGrupoDuplicado(){var c=curso();long h=count("FIA_CARGA");assertThatThrownBy(()->service.crear(input(List.of(detalle(c.id(),1),detalle(c.id(),1))))).isInstanceOf(IllegalArgumentException.class);assertThat(count("FIA_CARGA")).isEqualTo(h);}
 @Test void rechazaBloquesSuperpuestos(){var c=curso();var d=new DetalleInput(c.id(),null,1,null,null,null,null,List.of(new BloqueInput(1,450,510),new BloqueInput(1,500,550)));assertThatThrownBy(()->service.crear(input(List.of(d)))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("superpuestos");}
 @Test void filtraCargasPorPlanSemestreYFecha(){var c=curso();var r=service.crear(input(List.of(detalle(c.id(),1))));assertThat(service.listar(37L,"PRUEBA-TRANSACCION",r.fecha().toLocalDate(),r.fecha().toLocalDate())).extracting(Dto::id).contains(r.id());assertThat(service.listar(1L,"PRUEBA-TRANSACCION",null,null)).isEmpty();}
 @Test void separaAsignacionesPorDocente(){var c=curso();var teachers=planes.docentes("");var a=teachers.get(0);var b=teachers.get(1);var r=service.crear(input(List.of(new DetalleInput(c.id(),a.id(),1,null,null,null,null,List.of()))));assertThat(service.asignaciones(a.id())).extracting(pe.edu.upeu.fia.carga.CargasProfesorApi.Asignacion::cargaId).contains(r.id());assertThat(service.asignaciones(b.id())).extracting(pe.edu.upeu.fia.carga.CargasProfesorApi.Asignacion::cargaId).doesNotContain(r.id());}
 @Test void auditaCreacionYEdicionConActorFechaYCambio(){
  var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("auditor-prueba",null,List.of());
  org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
  try {
   var c=curso();var r=service.crear(input(List.of(detalle(c.id(),1))));service.actualizar(r.id(),input(List.of(detalle(c.id(),1),detalle(c.id(),2))));
   var events=service.auditoria(r.id());assertThat(events).extracting(CargaService.AuditDto::accion).contains("CREAR","EDITAR");
   assertThat(events).allSatisfy(e->{assertThat(e.actor()).isEqualTo("auditor-prueba");assertThat(e.fecha()).isNotNull();assertThat(e.resumen()).isNotBlank();});
   assertThat(events.stream().filter(e->e.accion().equals("EDITAR")).findFirst().orElseThrow().resumen()).contains("detalles 1→2");
  } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
 }
}
