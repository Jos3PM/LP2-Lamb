package pe.edu.upeu.fia.security;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.beans.factory.annotation.Value;
import pe.edu.upeu.fia.planes.PlanesApi;

@Service
public class TeacherAccountService {
    public record View(String username, Long docenteId, String docente, String codigo, String correo,
                       boolean activo, String creadoPor, String modificadoPor,
                       LocalDateTime creadoEn, LocalDateTime modificadoEn) {}

    private final TeacherAccountRepository accounts;
    private final InMemoryUserDetailsManager users;
    private final PasswordEncoder encoder;
    private final PlanesApi planes;
    private final StudentAccountRepository students;
    private final String adminUsername;

    TeacherAccountService(TeacherAccountRepository accounts, InMemoryUserDetailsManager users,
                          PasswordEncoder encoder, PlanesApi planes, StudentAccountRepository students,
                          @Value("${app.security.admin.username:admin}") String adminUsername) {
        this.accounts = accounts; this.users = users; this.encoder = encoder; this.planes = planes;
        this.students = students; this.adminUsername = adminUsername;
    }

    @PostConstruct void loadAccounts() {
        accounts.findAll().stream().filter(a -> a.activo).forEach(this::upsertLogin);
    }

    @Transactional(readOnly = true) public List<View> list() {
        return accounts.findAll().stream().sorted(Comparator.comparing(a -> a.username)).map(this::view).toList();
    }

    @Transactional(readOnly = true) public View one(String username) {
        return view(accounts.findById(username.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NoSuchElementException("Cuenta de profesor no encontrada")));
    }

    @Transactional(readOnly = true) TeacherAccount active(String username) {
        TeacherAccount account = accounts.findById(username.toLowerCase(Locale.ROOT)).orElseThrow(() -> new AccessDeniedException("Cuenta de profesor no encontrada"));
        if (!account.activo) throw new AccessDeniedException("La cuenta del profesor está desactivada");
        return account;
    }

    @Transactional public synchronized View create(String username, String password, Long docenteId,
                                                   String codigo, String correo, String actor) {
        String name = username.trim().toLowerCase(Locale.ROOT);
        if (users.userExists(name) || accounts.existsById(name) || adminUsername.equalsIgnoreCase(name)
                || students.findAll().stream().anyMatch(a -> a.username.equalsIgnoreCase(name)))
            throw new IllegalArgumentException("El usuario ya existe");
        if (accounts.findByDocenteId(docenteId).isPresent()) throw new IllegalArgumentException("El docente ya tiene cuenta");
        planes.docente(docenteId);
        unique(codigo, correo, null);
        TeacherAccount account = new TeacherAccount();
        account.username = name; account.passwordHash = encoder.encode(password); account.docenteId = docenteId;
        account.codigo = codigo.trim().toUpperCase(Locale.ROOT); account.correo = correo.trim().toLowerCase(Locale.ROOT);
        account.activo = true; account.creadoPor = actor; account.modificadoPor = actor;
        account.creadoEn = LocalDateTime.now(); account.modificadoEn = account.creadoEn;
        accounts.saveAndFlush(account);
        afterCommit(() -> upsertLogin(account));
        return view(account);
    }

    @Transactional public synchronized View update(String username, String codigo, String correo,
                                                   String password, boolean activo, String actor) {
        TeacherAccount account = accounts.findById(username.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NoSuchElementException("Cuenta de profesor no encontrada"));
        unique(codigo, correo, account);
        account.codigo = codigo.trim().toUpperCase(Locale.ROOT); account.correo = correo.trim().toLowerCase(Locale.ROOT);
        if (password != null && !password.isBlank()) account.passwordHash = encoder.encode(password);
        account.activo = activo; account.modificadoPor = actor; account.modificadoEn = LocalDateTime.now();
        accounts.saveAndFlush(account);
        afterCommit(() -> { if (account.activo) upsertLogin(account); else if (users.userExists(account.username)) users.deleteUser(account.username); });
        return view(account);
    }

    private void unique(String codigo, String correo, TeacherAccount current) {
        accounts.findAll().stream().filter(a -> current == null || !a.username.equals(current.username)).forEach(a -> {
            if (a.codigo.equalsIgnoreCase(codigo.trim())) throw new IllegalArgumentException("El código de profesor ya existe");
            if (a.correo.equalsIgnoreCase(correo.trim())) throw new IllegalArgumentException("El correo de profesor ya existe");
        });
    }

    private View view(TeacherAccount account) {
        return new View(account.username, account.docenteId, planes.docente(account.docenteId).name(),
                account.codigo, account.correo, account.activo, account.creadoPor, account.modificadoPor,
                account.creadoEn, account.modificadoEn);
    }

    private void upsertLogin(TeacherAccount account) {
        var user = User.withUsername(account.username).password(account.passwordHash).roles("TEACHER").build();
        if (users.userExists(account.username)) users.updateUser(user); else users.createUser(user);
    }

    private static void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
    }
}
