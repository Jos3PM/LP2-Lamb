package pe.edu.upeu.fia.planes.internal;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Component 
@Order(20) 
@ConditionalOnProperty(name="app.seed.enabled",havingValue="true") 
class CompletarDocentes implements ApplicationRunner {
    private final DocenteRepository repo;CompletarDocentes(DocenteRepository r){repo=r;}
    @Transactional 
    public void run(ApplicationArguments args)throws Exception{
        try(var br=new BufferedReader(new InputStreamReader(new ClassPathResource("docentes.tsv").getInputStream(),StandardCharsets.UTF_8))){for(String line:br.lines().toList()){var v=line.split("\t",-1);Long id=Long.valueOf(v[0]);if(!repo.existsById(id)){var d=new Docente();d.id=id;d.school=v[1];d.name=v[2];d.course=v[3];repo.save(d);}}}}
}
