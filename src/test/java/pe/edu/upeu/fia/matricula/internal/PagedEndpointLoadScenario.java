package pe.edu.upeu.fia.matricula.internal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Explicit opt-in benchmark: mvn -Dtest=PagedEndpointLoadScenario test. H2 only. */
@SpringBootTest @ActiveProfiles("test")
class PagedEndpointLoadScenario {
    private static final int ROWS = 800;
    private static final String PERIOD = "PERF-ISOLATED-2026";
    @Autowired JdbcTemplate db;
    @Autowired WebApplicationContext context;

    @Test void measurePagedEndpoints() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean("cachePolicy", jakarta.servlet.Filter.class),
                context.getBean("requestLog", jakarta.servlet.Filter.class)).build();
        db.update("DELETE FROM FIA_MATRICULA WHERE periodo=?", PERIOD);
        db.update("DELETE FROM FIA_ESTUDIANTE WHERE codigo LIKE 'PERF-ISO-%'");
        try {
            List<Object[]> students = new ArrayList<>(), enrollments = new ArrayList<>();
            for (int n=1;n<=ROWS;n++) {
                long studentId=-800000L-n, enrollmentId=-900000L-n;
                students.add(new Object[]{studentId,"PERF-ISO-"+n,String.format("Carga pagina %04d",n)});
                enrollments.add(new Object[]{enrollmentId,studentId,37L,PERIOD,n%2==0?"PENDIENTE":"PAGADA",
                    3,new BigDecimal("175.00"),n%2==0?BigDecimal.ZERO:new BigDecimal("175.00"),"PEN",
                    Timestamp.valueOf(LocalDateTime.of(2026,1,1,8,0).plusSeconds(n))});
            }
            db.batchUpdate("INSERT INTO FIA_ESTUDIANTE (id,codigo,nombre) VALUES (?,?,?)", students);
            db.batchUpdate("INSERT INTO FIA_MATRICULA (id,estudiante_id,plan_id,periodo,estado,creditos,monto,pagado,moneda,fecha) VALUES (?,?,?,?,?,?,?,?,?,?)", enrollments);
            long studentCount=db.queryForObject("SELECT COUNT(*) FROM FIA_ESTUDIANTE WHERE codigo LIKE 'PERF-ISO-%'",Long.class);
            long enrollmentCount=db.queryForObject("SELECT COUNT(*) FROM FIA_MATRICULA WHERE periodo=?",Long.class,PERIOD);
            if(studentCount!=ROWS||enrollmentCount!=ROWS)throw new AssertionError("Fixture incomplete");
            String[] paths={
                "/api/v1/estudiantes/pagina?buscar=Carga%20pagina&pagina=3&tamano=25",
                "/api/v1/matriculas/pagina?periodo="+PERIOD+"&estado=PENDIENTE&planId=37&pagina=3&tamano=25",
                "/api/v1/reportes/matriculas?periodo="+PERIOD+"&estado=PENDIENTE&planId=37&pagina=3&tamano=25"};
            for(int k=0;k<paths.length;k++){
                for(int i=0;i<20;i++)mvc.perform(get(paths[k]));
                long[] timings=new long[100];int errors=0;
                for(int i=0;i<timings.length;i++){
                    long start=System.nanoTime();
                    int status=mvc.perform(get(paths[k])).andReturn().getResponse().getStatus();
                    timings[i]=System.nanoTime()-start;
                    if(status!=200)errors++;
                }
                Arrays.sort(timings);
                System.out.printf("BENCH endpoint=%s students=%d enrollments=%d filteredEnrollments=%d requests=%d p50Ms=%.3f p95Ms=%.3f errors=%d%n",
                    new String[]{"students","enrollments","enrollment-report"}[k],studentCount,enrollmentCount,ROWS/2,timings.length,
                    timings[49]/1_000_000.0,timings[94]/1_000_000.0,errors);
                if(errors>0)throw new AssertionError("Request errors: "+errors);
            }
        } finally {
            db.update("DELETE FROM FIA_MATRICULA WHERE periodo=?", PERIOD);
            db.update("DELETE FROM FIA_ESTUDIANTE WHERE codigo LIKE 'PERF-ISO-%'");
        }
    }
}
