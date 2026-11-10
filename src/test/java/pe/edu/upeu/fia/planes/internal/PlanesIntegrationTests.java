package pe.edu.upeu.fia.planes.internal;
import pe.edu.upeu.fia.planes.*;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @ActiveProfiles("test") class PlanesIntegrationTests {
 @Autowired PlanesService service;
 @Test void mantieneLos37PlanesDocumentados(){assertThat(service.planes()).hasSize(37);}
 @Test void filtraPorPlanCicloYNombre(){var list=service.cursos(37L,4,"Lenguaje","name");assertThat(list).as("Cursos del ciclo: %s", service.cursos(37L,4,null,"name")).hasSize(1);assertThat(list.get(0).name()).isEqualTo("Lenguaje de Programación II");assertThat(list.get(0).total()).isEqualTo(4);}
 @Test void creaActualizaYEliminaCursoManual(){var a=service.guardar(null,new CursoInput(37L,1,"FIXTURE CRUD",2,1,2,0,""));assertThat(a.total()).isEqualTo(3);var b=service.guardar(a.id(),new CursoInput(37L,2,"FIXTURE CRUD EDITADO",3,2,2,0,""));assertThat(b.total()).isEqualTo(4);assertThat(b.cycle()).isEqualTo(2);service.borrar(a.id());assertThatThrownBy(()->service.curso(a.id())).isInstanceOf(java.util.NoSuchElementException.class);}
 @Test void rechazaDuplicadosYProtegePlanConCursos(){
  var plan=service.planes().getFirst();
  var curso=service.cursos(plan.id(),null,null,"cycle").getFirst();
  assertThatThrownBy(()->service.guardarPlan(null,new PlanInput(plan.school(),plan.version(),plan.variant(),"Duplicado")))
    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Ya existe un plan");
  assertThatThrownBy(()->service.guardar(null,new CursoInput(plan.id(),curso.cycle(),curso.name(),curso.credits(),curso.ht(),curso.hp(),curso.hnp(),"")))
    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ya contiene un curso");
  assertThatThrownBy(()->service.borrarPlan(plan.id()))
    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tiene cursos");
 }
}
