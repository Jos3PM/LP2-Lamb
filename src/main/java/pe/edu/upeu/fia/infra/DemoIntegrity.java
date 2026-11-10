package pe.edu.upeu.fia.infra;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

@Component 
@Profile({"demo","test"}) class DemoIntegrity implements ApplicationRunner {
    private final JdbcTemplate db;DemoIntegrity(JdbcTemplate d){db=d;}
    public void run(ApplicationArguments args){
    db.execute("ALTER TABLE FIA_CARGA_DETALLE ADD CONSTRAINT IF NOT EXISTS FK_DETALLE_CURSO FOREIGN KEY (curso_id) REFERENCES FIA_CURSO(id)");
    db.execute("ALTER TABLE FIA_CARGA ADD CONSTRAINT IF NOT EXISTS FK_CARGA_PLAN FOREIGN KEY (plan_id) REFERENCES FIA_PLAN(id)");
    db.execute("ALTER TABLE FIA_CARGA_DETALLE ADD CONSTRAINT IF NOT EXISTS FK_DETALLE_DOCENTE FOREIGN KEY (docente_id) REFERENCES FIA_DOCENTE_FUENTE(id)");
    }
}
