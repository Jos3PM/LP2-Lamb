package pe.edu.upeu.fia;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
class ModularityTests {
 @Test void verificaLimitesEntreModulos(){ApplicationModules.of(FiaApplication.class).verify();}
}
