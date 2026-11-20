package pe.fiadope.api.security;

import pe.fiadope.api.domain.Rol;

/** Datos del usuario autenticado que viajan en el JWT. */
public record UsuarioPrincipal(Integer idUsuario, String login, String nombre, Rol rol, Integer idTienda,
                               String jti) {
}

