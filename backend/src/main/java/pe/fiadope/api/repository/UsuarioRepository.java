package pe.fiadope.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.fiadope.api.domain.Rol;
import pe.fiadope.api.domain.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByLogin(String login);

    Optional<Usuario> findByMfaToken(String mfaToken);

    boolean existsByLogin(String login);

    boolean existsByEmailIgnoreCase(String email);

    List<Usuario> findByTiendaId(Integer idTienda);

    Optional<Usuario> findFirstByTiendaIdAndRol(Integer idTienda, Rol rol);
}

