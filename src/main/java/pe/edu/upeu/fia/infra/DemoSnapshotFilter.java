package pe.edu.upeu.fia.infra;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Persists demo H2 mutations as an SQL snapshot that can be loaded on the next start. */
@Component
@ConditionalOnProperty(name="app.demo.sql-persistence",havingValue="true")
public class DemoSnapshotFilter extends OncePerRequestFilter {
 private final JdbcTemplate jdbc;
 private final Object snapshotLock=new Object();
 public DemoSnapshotFilter(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Override protected boolean shouldNotFilter(HttpServletRequest request){
  String method=request.getMethod();String path=request.getRequestURI();
  return !(path.startsWith("/api/v1/")&&(method.equals("POST")||method.equals("PUT")||method.equals("PATCH")||method.equals("DELETE")))
   ||path.equals("/api/v1/auth/login")||path.equals("/api/v1/auth/logout");
 }
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  ContentCachingResponseWrapper wrapped=new ContentCachingResponseWrapper(response);
  chain.doFilter(request,wrapped);
  if(wrapped.getStatus()<400){
   try { synchronized(snapshotLock){jdbc.execute("SCRIPT TO './data/fia-v3-backup.sql'");} }
   catch(Exception error){wrapped.resetBuffer();wrapped.sendError(500,"No se pudo guardar la base de demostración");}
  }
  wrapped.copyBodyToResponse();
 }
}
