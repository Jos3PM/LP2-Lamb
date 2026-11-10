package pe.edu.upeu.fia.carga.internal;
import pe.edu.upeu.fia.planes.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static pe.edu.upeu.fia.carga.internal.CargaDtos.*;
@SpringBootTest @ActiveProfiles("test") class AgendaIntegrationTests {
 @Autowired CargaService service; @Autowired PlanesApi planes;
 DetalleInput row(int group,String room,Long teacher,int start,int cupos){return new DetalleInput(planes.cursos(37L,1,null,"name").get(0).id(),teacher,group,cupos,room,"Titular","Teórico",List.of(new BloqueInput(1,start,start+50)),"Curso editable",1,3,1,0,0);}
 Input input(String semester,DetalleInput... rows){return new Input(37L,"Filial Juliaca",semester,"Pregrado",List.of(rows));}
 @Test void generaCincoCarrerasSinCrucesYEsIdempotente(){
  var cargas=service.generar();assertThat(cargas).hasSize(5);assertThat(service.generar()).extracting(Dto::id).containsExactlyElementsOf(cargas.stream().map(Dto::id).toList());
  for(var carga:cargas){assertThat(carga.detalles()).isNotEmpty();for(var a:carga.detalles()){assertThat(a.docenteId()).isNull();assertThat(a.bloques()).hasSize(a.ht()+a.hp());for(var b:carga.detalles())if(!a.id().equals(b.id())&&a.ciclo()==b.ciclo())for(var x:a.bloques())for(var y:b.bloques())assertThat(x.dia()==y.dia()&&x.inicio()<y.fin()&&y.inicio()<x.fin()).isFalse();}}
 }
 @Test void rechazaChoqueAmbienteEntreCargas(){service.crear(input("ROOM",row(1,"A-1",null,450,20)));assertThatThrownBy(()->service.crear(input("ROOM",row(2,"a 1",null,450,30)))).hasMessageContaining("Choque de ambiente");}
 @Test void rechazaChoqueDocenteEntreCargas(){Long teacher=planes.docentes(null).get(0).id();service.crear(input("TEACHER",row(1,"A",teacher,450,20)));assertThatThrownBy(()->service.crear(input("TEACHER",row(2,"B",teacher,450,20)))).hasMessageContaining("Choque de docente");}
 @Test void rechazaChoqueCursosDelMismoGrupo(){service.crear(input("COHORT",row(1,"A",null,450,20)));assertThatThrownBy(()->service.crear(input("COHORT",row(1,"B",null,450,20)))).hasMessageContaining("cursos del ciclo");}
 @Test void actualizaFilaYCuposYRevierteCambioConChoque(){var a=service.crear(input("EDIT",row(1,"A",null,450,20)));var b=service.crear(input("EDIT",row(2,"A",null,500,20)));var updated=service.actualizar(b.id(),input("EDIT",row(2,"A",null,500,45)));assertThat(updated.detalles().get(0).cupos()).isEqualTo(45);assertThatThrownBy(()->service.actualizar(b.id(),input("EDIT",row(2,"A",null,450,99)))).hasMessageContaining("Choque");assertThat(service.uno(b.id()).detalles().get(0).cupos()).isEqualTo(45);assertThat(service.uno(a.id()).detalles().get(0).bloques().get(0).inicio()).isEqualTo(450);}
 @Test void buscaDocentesGlobalesSinDependerDelCurso(){var teachers=planes.docentes(null);assertThat(teachers.size()).isGreaterThan(50);var teacher=teachers.get(teachers.size()-1);assertThat(planes.docentes(teacher.name())).extracting(DocenteDto::id).contains(teacher.id());assertThat(service.crear(input("ANY-TEACHER",row(1,"A",teacher.id(),450,25))).detalles().get(0).docenteId()).isEqualTo(teacher.id());}
}
