package pe.edu.upeu.fia.security;
import java.util.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import jakarta.annotation.PostConstruct;
import pe.edu.upeu.fia.matricula.MatriculaAlumnoApi;

@Service
public class StudentAccountService {
 private final InMemoryUserDetailsManager users;
 private final PasswordEncoder encoder;
 private final MatriculaAlumnoApi matriculas;
 private final StudentAccountRepository accounts;
 private final TeacherAccountRepository teachers;
 public StudentAccountService(InMemoryUserDetailsManager users,PasswordEncoder encoder,MatriculaAlumnoApi matriculas,StudentAccountRepository accounts,TeacherAccountRepository teachers){this.users=users;this.encoder=encoder;this.matriculas=matriculas;this.accounts=accounts;this.teachers=teachers;}
 @PostConstruct public void loadAccounts(){accounts.findAll().forEach(a->users.createUser(User.withUsername(a.username).password(a.passwordHash).roles("STUDENT").build()));}
 @Transactional public synchronized Map<String,Object> register(String username,String password,String codigo,String nombre,String correo){
  if(username==null||username.isBlank()||password==null||password.length()<8)throw new IllegalArgumentException("Usuario obligatorio y contraseña mínima de 8 caracteres");
  String name=username.trim();
  if(users.userExists(name)||accounts.existsById(name)||teachers.findAll().stream().anyMatch(t->t.username.equalsIgnoreCase(name)))throw new IllegalArgumentException("El usuario ya está registrado");
  Long id=matriculas.registrarAlumnoId(codigo,nombre,correo);
  if(accounts.existsByEstudianteId(id))throw new IllegalArgumentException("El estudiante ya tiene una cuenta de acceso");
  StudentAccount a=new StudentAccount();a.username=name;a.passwordHash=encoder.encode(password);a.estudianteId=id;accounts.saveAndFlush(a);
  afterCommit(()->users.createUser(User.withUsername(a.username).password(a.passwordHash).roles("STUDENT").build()));
  return Map.of("username",name,"role","STUDENT","estudianteId",id);
 }
 public Long studentId(String username){return accounts.findById(username).orElseThrow(()->new IllegalArgumentException("La sesión de alumno no tiene un estudiante asociado")).estudianteId;}
 public boolean puedeEditarCarrera(String username){return accounts.findById(username).orElseThrow(()->new IllegalArgumentException("Cuenta no encontrada")).puedeEditarCarrera==Boolean.TRUE;}
 public List<Map<String,Object>> list(){return accounts.findAll().stream().sorted(Comparator.comparing(a->a.username)).map(a->Map.<String,Object>of("username",a.username,"estudianteId",a.estudianteId,"role","STUDENT","puedeEditarCarrera",Boolean.TRUE.equals(a.puedeEditarCarrera))).toList();}
 @Transactional public synchronized Map<String,Object> edit(String username,String newUsername,String password,Boolean puedeEditarCarrera){
  StudentAccount old=accounts.findById(username).orElseThrow(()->new NoSuchElementException("Cuenta no encontrada"));
  String name=newUsername==null||newUsername.isBlank()?username:newUsername.trim();
  if(!name.equals(username)&&(users.userExists(name)||accounts.existsById(name)||teachers.findAll().stream().anyMatch(t->t.username.equalsIgnoreCase(name))))throw new IllegalArgumentException("El usuario ya está registrado");
  String hash=password==null||password.isBlank()?old.passwordHash:encoder.encode(password);
  if(password!=null&&!password.isBlank()&&password.length()<8)throw new IllegalArgumentException("Contraseña mínima de 8 caracteres");
  if(!name.equals(username)){accounts.delete(old);accounts.flush();old.username=name;}
  old.passwordHash=hash;if(puedeEditarCarrera!=null)old.puedeEditarCarrera=puedeEditarCarrera;accounts.saveAndFlush(old);
  String previous=username;
  afterCommit(()->{if(!name.equals(previous)&&users.userExists(previous))users.deleteUser(previous);var user=User.withUsername(name).password(hash).roles("STUDENT").build();if(users.userExists(name))users.updateUser(user);else users.createUser(user);});
  return Map.of("username",name,"estudianteId",old.estudianteId,"role","STUDENT","puedeEditarCarrera",Boolean.TRUE.equals(old.puedeEditarCarrera));
 }
 private static void afterCommit(Runnable action){TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){action.run();}});}
}
