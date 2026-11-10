package pe.edu.upeu.fia.infra;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@RestController class EstadoController {
    private final Environment env;private final JdbcTemplate db;EstadoController(Environment e,JdbcTemplate j){env=e;db=j;}
 @GetMapping("/api/v1/estado") Map<String,Object> estado(){Integer ping=db.queryForObject("SELECT 1 FROM DUAL",Integer.class);return Map.of("estado",ping==1?"OK":"ERROR","perfil",String.join(",",env.getActiveProfiles().length==0?env.getDefaultProfiles():env.getActiveProfiles()));}
}
