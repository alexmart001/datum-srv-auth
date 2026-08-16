package br.com.datum.auth.service;

import br.com.datum.auth.model.Role;
import br.com.datum.auth.model.Usuario;
import br.com.datum.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test puro (sem contexto Spring, sem banco): UsuarioRepository é
 * mockado.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioDetailsService usuarioDetailsService;

    private Usuario usuario(String username, String password, Role role) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername(username);
        usuario.setPassword(password);
        usuario.setRole(role);
        return usuario;
    }

    @Test
    void usuarioAdmin_carregaComAutoridadeAdminSemPrefixo() {
        usuarioDetailsService = new UsuarioDetailsService(usuarioRepository);
        when(usuarioRepository.findByUsername("admin"))
                .thenReturn(Optional.of(usuario("admin", "hash-da-senha", Role.ADMIN)));

        UserDetails userDetails = usuarioDetailsService.loadUserByUsername("admin");

        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getPassword()).isEqualTo("hash-da-senha");
        assertThat(userDetails.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ADMIN");
    }

    @Test
    void usuarioUser_carregaComAutoridadeUserSemPrefixo() {
        usuarioDetailsService = new UsuarioDetailsService(usuarioRepository);
        when(usuarioRepository.findByUsername("user"))
                .thenReturn(Optional.of(usuario("user", "hash-da-senha", Role.USER)));

        UserDetails userDetails = usuarioDetailsService.loadUserByUsername("user");

        assertThat(userDetails.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("USER");
    }

    @Test
    void usuarioInexistente_lancaUsernameNotFoundException() {
        usuarioDetailsService = new UsuarioDetailsService(usuarioRepository);
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioDetailsService.loadUserByUsername("fantasma"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("fantasma");
    }
}
