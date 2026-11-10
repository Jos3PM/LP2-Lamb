package pe.edu.upeu.fia.infra;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice class Errores {
 @ExceptionHandler(NoSuchElementException.class) ResponseEntity<ProblemDetail> missing(NoSuchElementException e){return error(404,e.getMessage());}
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<ProblemDetail> invalid(IllegalArgumentException e){return error(400,e.getMessage());}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e){return error(400,e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).reduce((a,b)->a+"; "+b).orElse("Datos inválidos"));}
 @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class}) ResponseEntity<ProblemDetail> parse(Exception e){return error(400,"Formato de solicitud inválido");}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ProblemDetail> conflict(Exception e){return error(409,"El registro está relacionado o incumple una restricción de integridad");}
 @org.springframework.beans.factory.annotation.Autowired HttpServletRequest request;
 private ResponseEntity<ProblemDetail> error(int status,String detail){
  ProblemDetail problem=ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status),detail);
  problem.setProperty("requestId",request.getAttribute("requestId"));
  problem.setProperty("accionSugerida",status==404?"Comprueba el identificador y vuelve a consultar.":status==409?"Actualiza la página antes de repetir la operación.":"Revisa los datos y vuelve a intentarlo.");
  return ResponseEntity.status(status).body(problem);
 }
}
