package pe.edu.upeu.fia.matricula.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pe.edu.upeu.fia.planes.PlanesApi;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static pe.edu.upeu.fia.matricula.internal.MatriculaDtos.*;

@SpringBootTest
@ActiveProfiles("test")
class MatriculaIntegrationTests {
 @Autowired MatriculaService service;
 @Autowired PlanesApi planes;
 @Autowired org.springframework.jdbc.core.JdbcTemplate db;
 private pe.edu.upeu.fia.planes.CursoDto cursoProgramado(Long planId){
  var curso=planes.cursos(planId,1,null,"name").stream().filter(c->c.credits()>0).findFirst().orElseThrow();
  if(curso.horario()==null||curso.horario().isBlank())db.update("UPDATE FIA_CURSO SET horario=? WHERE id=?","Lun 08:00-09:00",curso.id());
  return planes.curso(curso.id());
 }

 @Test void guardaLaMatriculaCalculaElSaldoYProtegeLaEleccion() {
  String suffix=UUID.randomUUID().toString().substring(0,8);
  String periodo="PRUEBA-"+suffix;
  var estudiante=service.crearEstudiante(new EstudianteInput("FIX-MAT-"+suffix,"Estudiante de prueba",null));
  var plan=planes.planes().getFirst();
  var otroPlan=planes.planes().stream().filter(p->!p.id().equals(plan.id())).findFirst().orElseThrow();
  var curso=cursoProgramado(plan.id());
  service.elegir(new EleccionInput(estudiante.id(),plan.id(),periodo));
  service.configurar(new TarifaInput(periodo,new BigDecimal("100.00"),new BigDecimal("25.00"),"PEN"));

  var matricula=service.matricular(new MatriculaInput(estudiante.id(),plan.id(),periodo,List.of(curso.id())));
  BigDecimal total=new BigDecimal("100.00").add(new BigDecimal("25.00").multiply(BigDecimal.valueOf(curso.credits())));
  assertThat(matricula.monto()).isEqualByComparingTo(total);
  assertThat(matricula.saldo()).isEqualByComparingTo(total);
  assertThat(matricula.cursos()).extracting(DetalleDto::cursoId).containsExactly(curso.id());

  service.pagar(matricula.id(),new PagoInput(new BigDecimal("30.00"),"Transferencia","PRUEBA-001"));
  var parcial=service.matriculas().stream().filter(m->m.id().equals(matricula.id())).findFirst().orElseThrow();
  assertThat(parcial.estado()).isEqualTo("PARCIAL");
  assertThat(parcial.saldo()).isEqualByComparingTo(total.subtract(new BigDecimal("30.00")));
  assertThat(service.pagos(matricula.id())).hasSize(1);
  assertThatThrownBy(()->service.pagar(matricula.id(),new PagoInput(total,"Transferencia",null)))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("supera el saldo");
  assertThatThrownBy(()->service.elegir(new EleccionInput(estudiante.id(),otroPlan.id(),periodo)))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("No se puede cambiar el plan");

  service.pagar(matricula.id(),new PagoInput(total.subtract(new BigDecimal("30.00")),"Efectivo",null));
  var pagada=service.matriculas().stream().filter(m->m.id().equals(matricula.id())).findFirst().orElseThrow();
  assertThat(pagada.estado()).isEqualTo("PAGADA");
  assertThat(pagada.saldo()).isEqualByComparingTo(BigDecimal.ZERO);
  assertThat(service.pagos(matricula.id())).hasSize(2);
 }
 @Test void paginaSinCargarDetalleYNoPersistePagoRechazado() {
  String suffix=UUID.randomUUID().toString().substring(0,8);
  String periodo="PAG-"+suffix;
  var estudiante=service.crearEstudiante(new EstudianteInput("FIX-PAG-"+suffix,"Alumno pagina "+suffix,null));
  var plan=planes.planes().getFirst();
  var curso=cursoProgramado(plan.id());
  service.elegir(new EleccionInput(estudiante.id(),plan.id(),periodo));
  service.configurar(new TarifaInput(periodo,new BigDecimal("100.00"),new BigDecimal("25.00"),"PEN"));
  var matricula=service.matricular(new MatriculaInput(estudiante.id(),plan.id(),periodo,List.of(curso.id())));
  var page=service.matriculasPagina(periodo,"",null,"Alumno pagina",0,1);
  assertThat(page.total()).isEqualTo(1);
  assertThat(page.items()).extracting(MatriculaResumenDto::id).containsExactly(matricula.id());
  assertThat(service.matricula(matricula.id()).cursos()).hasSize(1);
  assertThatThrownBy(()->service.pagar(matricula.id(),new PagoInput(matricula.monto().add(BigDecimal.ONE),"Efectivo",null)))
      .isInstanceOf(IllegalArgumentException.class);
  assertThat(service.pagos(matricula.id())).isEmpty();
  assertThat(service.matricula(matricula.id()).pagado()).isEqualByComparingTo(BigDecimal.ZERO);
 }
 @Test void alumnoMatriculaPagaYAdminConfirmaAntesDelContrato(){
  String suffix=UUID.randomUUID().toString().substring(0,8), periodo="E2E-"+suffix;
  var estudiante=service.crearEstudiante(new EstudianteInput("E2E-"+suffix,"Alumno flujo "+suffix,"flujo@example.test"));
  service.perfilAlumno(estudiante.id(),"flujo@example.test","987654321","Apoderado de prueba");
  var plan=planes.planes().getFirst();var curso=cursoProgramado(plan.id());
  service.elegir(new EleccionInput(estudiante.id(),plan.id(),periodo));
  service.configurar(new TarifaInput(periodo,new BigDecimal("100.00"),new BigDecimal("25.00"),"PEN"));
  long cuposAntes=service.cupos(plan.id(),periodo).get(curso.id());
  var matricula=service.matricularAlumno(estudiante.id(),periodo,List.of(curso.id()));
  assertThat(service.cupos(plan.id(),periodo).get(curso.id())).isEqualTo(cuposAntes-1);
  var declarado=service.pagarAlumno(matricula.id(),estudiante.id(),matricula.monto(),"Efectivo",null,"CONTADO",null,null);
  assertThat(declarado.estado()).isEqualTo("PENDIENTE_VERIFICACION");
  assertThat(service.matricula(matricula.id()).pagado()).isEqualByComparingTo(BigDecimal.ZERO);
  assertThatThrownBy(()->service.contrato(matricula.id(),estudiante.id())).isInstanceOf(IllegalArgumentException.class);
  service.confirmarPago(matricula.id(),declarado.id());
  assertThat(service.matricula(matricula.id()).saldo()).isEqualByComparingTo(BigDecimal.ZERO);
  assertThat(service.contrato(matricula.id(),estudiante.id())).containsKey("cursos");
  assertThat(service.comprobanteAlumno(matricula.id(),declarado.id(),estudiante.id())).containsKey("monto");
 }
 @Test void reporteSumaTodoElFiltroAunqueLaPaginaMuestreUnaFila(){
  String suffix=UUID.randomUUID().toString().substring(0,8),periodo="REP-"+suffix;
  var plan=planes.planes().getFirst();var curso=cursoProgramado(plan.id());
  service.configurar(new TarifaInput(periodo,new BigDecimal("100"),new BigDecimal("25"),"PEN"));
  for(int n=1;n<=2;n++){
   var alumno=service.crearEstudiante(new EstudianteInput("REP-"+suffix+n,"Filtro alumno "+suffix+" "+n,null));
   service.elegir(new EleccionInput(alumno.id(),plan.id(),periodo));
   service.matricular(new MatriculaInput(alumno.id(),plan.id(),periodo,List.of(curso.id())));
  }
  var page=service.matriculasPagina(periodo,"PENDIENTE",plan.id(),"Filtro alumno",0,1);
  var report=service.reporteMatriculas(periodo,"PENDIENTE",plan.id(),"Filtro alumno",0,1);
  assertThat(page.total()).isEqualTo(2);assertThat(page.items()).hasSize(1);
  assertThat(report.get("total")).isEqualTo(2L);
  assertThat((BigDecimal)report.get("monto")).isEqualByComparingTo(page.items().getFirst().monto().multiply(BigDecimal.valueOf(2)));
  assertThat((BigDecimal)report.get("saldo")).isEqualByComparingTo((BigDecimal)report.get("monto"));
 }
 @Test void filtrosOrdenMetadatosYTotalesPersistenEntrePaginas(){
  String suffix=UUID.randomUUID().toString().substring(0,8),periodo="MULTI-"+suffix;
  var plan=planes.planes().getFirst();var curso=cursoProgramado(plan.id());
  service.configurar(new TarifaInput(periodo,new BigDecimal("100"),new BigDecimal("25"),"PEN"));
  for(int n=1;n<=9;n++){
   var alumno=service.crearEstudiante(new EstudianteInput("MULTI-"+suffix+"-"+n,"Paginacion "+suffix+" "+String.format("%02d",n),null));
   if(n<=7){service.elegir(new EleccionInput(alumno.id(),plan.id(),periodo));service.matricular(new MatriculaInput(alumno.id(),plan.id(),periodo,List.of(curso.id())));}
  }
  var students0=service.estudiantesPagina("Paginacion "+suffix,0,4);
  var students1=service.estudiantesPagina("Paginacion "+suffix,1,4);
  var students2=service.estudiantesPagina("Paginacion "+suffix,2,4);
  assertThat(students0.total()).isEqualTo(9);assertThat(students2.paginas()).isEqualTo(3);
  assertThat(students0.items()).hasSize(4);assertThat(students1.items()).hasSize(4);assertThat(students2.items()).hasSize(1);
  assertThat(java.util.stream.Stream.of(students0,students1,students2).flatMap(p->p.items().stream()).map(EstudianteDto::nombre).toList()).isSorted();
  var p0=service.matriculasPagina(periodo,"PENDIENTE",plan.id(),suffix,0,3);
  var p1=service.matriculasPagina(periodo,"PENDIENTE",plan.id(),suffix,1,3);
  var p2=service.matriculasPagina(periodo,"PENDIENTE",plan.id(),suffix,2,3);
  assertThat(p0.total()).isEqualTo(7);assertThat(p1.total()).isEqualTo(7);assertThat(p2.paginas()).isEqualTo(3);
  assertThat(p0.items()).hasSize(3);assertThat(p1.items()).hasSize(3);assertThat(p2.items()).hasSize(1);
  var ids=java.util.stream.Stream.of(p0,p1,p2).flatMap(p->p.items().stream()).map(MatriculaResumenDto::id).toList();
  assertThat(ids).doesNotHaveDuplicates();assertThat(ids).isSortedAccordingTo(java.util.Comparator.reverseOrder());
  var report=service.reporteMatriculas(periodo,"PENDIENTE",plan.id(),suffix,2,3);
  assertThat(report.get("total")).isEqualTo(7L);assertThat(report.get("paginas")).isEqualTo(3);
  assertThat((BigDecimal)report.get("monto")).isEqualByComparingTo(p0.items().getFirst().monto().multiply(BigDecimal.valueOf(7)));
  assertThat(service.matriculasPagina(periodo,"PAGADA",plan.id(),suffix,0,3).total()).isZero();
 }
}
