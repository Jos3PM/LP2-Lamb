package pe.edu.upeu.fia.infra;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.*;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.web.servlet.HandlerMapping;

@Component class RequestLog extends OncePerRequestFilter {
 private static final Logger log=LoggerFactory.getLogger(RequestLog.class);
 private final MeterRegistry metrics;
 RequestLog(MeterRegistry metrics){this.metrics=metrics;}
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String incoming=req.getHeader("X-Request-ID");
  String id=incoming!=null&&incoming.matches("[A-Za-z0-9_-]{8,64}")?incoming:UUID.randomUUID().toString();
  req.setAttribute("requestId",id);res.setHeader("X-Request-ID",id);long start=System.nanoTime();
  try(MDC.MDCCloseable ignored=MDC.putCloseable("requestId",id)){
   chain.doFilter(req,res);
  }finally{
   long elapsed=System.nanoTime()-start;
   String operation=req.getRequestURI().contains("/pagos")?"pago":req.getRequestURI().startsWith("/api/v1/alumno/matriculas")?"matricula_alumno":"http";
   metrics.timer("fia.request.duration","method",req.getMethod(),"operation",operation,"status",String.valueOf(res.getStatus())).record(elapsed,TimeUnit.NANOSECONDS);
   if(res.getStatus()>=400)metrics.counter("fia.request.errors","operation",operation,"status",String.valueOf(res.getStatus())).increment();
   Object matched=req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
   String route=matched instanceof String pattern?pattern:"/unmatched";
   log.info("request={} method={} route={} status={} durationMs={}",id,req.getMethod(),route,res.getStatus(),elapsed/1000000);
  }
 }
}
