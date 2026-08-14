package br.com.datum.auth.service;

import br.com.datum.auth.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        // Autoridade sem prefixo "ROLE_": o claim "roles" do JWT carrega o nome
        // puro do papel (ex.: "ADMIN"); quem adiciona o prefixo "ROLE_" é o
        // resource server, ao converter o JWT em Authentication (ver
        // JwtAuthenticationConverter no datum-srv-clientes).
        return User.withUsername(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(usuario.getRole().name())
                .build();
    }
}
