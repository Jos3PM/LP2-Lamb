package pe.edu.upeu.fia.planes.internal;
import org.springframework.boot.ApplicationArguments;import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;import java.nio.charset.StandardCharsets;import java.io.*;

@org.springframework.core.annotation.Order(10) 
@Component 
@ConditionalOnProperty(name="app.seed.enabled",havingValue="true") 
class ImportarFuentes implements ApplicationRunner {
    private final PlanRepository plans;private final CursoRepository courses;
    private final DocenteRepository teachers;
    ImportarFuentes(PlanRepository p,CursoRepository c,DocenteRepository t){plans=p;courses=c;teachers=t;}
    private java.util.List<String[]> lines(String f)throws IOException{try(var r=new BufferedReader(new InputStreamReader(new ClassPathResource(f).getInputStream(),StandardCharsets.UTF_8))){return r.lines().filter(s->!s.isBlank()).map(s->s.split("\t",-1)).toList();}}
    @Transactional public void run(ApplicationArguments args)throws Exception{
    if(plans.count()!=0)return;
    for(var v:lines("planes.tsv")){Plan p=new Plan();
        p.id=Long.valueOf(v[0]);p.school=v[1];p.version=v[2];p.variant=v[3];p.source=v[4];plans.save(p);}
    for(var v:lines("cursos.tsv")){Curso c=new Curso();
        c.plan=plans.getReferenceById(Long.valueOf(v[1]));c.cycle=Integer.parseInt(v[3]);c.name=v[4];c.credits=Integer.parseInt(v[5]);c.ht=Integer.parseInt(v[6]);c.hp=Integer.parseInt(v[7]);c.hnp=Integer.parseInt(v[8]);c.total=Integer.parseInt(v[9]);c.prerequisites=v[10];c.notes=v[11];c.source=v[12];c.page=Integer.parseInt(v[13]);courses.save(c);}
    for(var v:lines("docentes.tsv")){Docente d=new Docente();
        d.id=Long.valueOf(v[0]);d.school=v[1];d.name=v[2];d.course=v[3];teachers.save(d);}
    }
}
