package br.com.datum.auth.init;

import br.com.datum.auth.model.Role;
import br.com.datum.auth.model.Usuario;
import br.com.datum.auth.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        criarUsuario("admin", "admin123", Role.ADMIN);
        criarUsuario("user", "user123", Role.USER);

        logger.info("Usuários de exemplo criados: admin/admin123 (ADMIN) e user/user123 (USER)");
    }

    private void criarUsuario(String username, String rawPassword, Role role) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(rawPassword));
        usuario.setRole(role);
        usuarioRepository.save(usuario);
    }
}
