package pe.edu.upeu.fia.security;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    public record LoginInput(String username, String password) {}
    private final AuthenticationManager manager;
    private final StudentAccountService students;
    private final SecurityContextRepository repository = new HttpSessionSecurityContextRepository();
    public AuthController(AuthenticationManager manager, StudentAccountService students) { this.manager = manager; this.students = students; }

    public record RegisterInput(String username,String password,String codigo,String nombre,String correo) {}

    @PostMapping("/register")
    public Map<String,Object> register(@Valid @RequestBody RegisterInput input){return students.register(input.username(),input.password(),input.codigo(),input.nombre(),input.correo());}

    @GetMapping("/accounts") public Object accounts(){return students.list();}
    public record AccountEditInput(String username,String password,Boolean puedeEditarCarrera) {}
    @PutMapping("/accounts/{username}") public Object editAccount(@PathVariable String username,@RequestBody AccountEditInput input){return students.edit(username,input.username(),input.password(),input.puedeEditarCarrera());}

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginInput input, HttpServletRequest request, HttpServletResponse response) {
        if (input.username() == null || input.password() == null) throw new BadCredentialsException("Usuario o contraseña inválidos");
        Authentication authentication = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(input.username(), input.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        repository.saveContext(context, request, response);
        String role=authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(x->x.startsWith("ROLE_")).map(x->x.substring(5)).findFirst().orElse("USER");
        return Map.of("username", authentication.getName(), "role", role);
    }

    @GetMapping("/status")
    public Map<String, Object> status(HttpServletRequest request) {
        // Materializa el token diferido y entrega la cookie XSRF-TOKEN a la SPA.
        Object csrf = request.getAttribute(CsrfToken.class.getName());
        if (csrf instanceof CsrfToken token) token.getToken();
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = a != null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken);
        String role=authenticated ? a.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(x->x.startsWith("ROLE_")).map(x->x.substring(5)).findFirst().orElse("USER") : "";
        return Map.of("authenticated", authenticated, "username", authenticated ? a.getName() : "", "role", role);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}







