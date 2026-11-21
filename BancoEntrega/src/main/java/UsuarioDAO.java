import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class UsuarioDAO {

    
    public void guardar(Usuario usuario, String codigoHash)
            throws SQLException {

        if (usuario == null) {
            throw new IllegalArgumentException(
                "El usuario es obligatorio."
            );
        }

        if (codigoHash == null || codigoHash.isBlank()) {
            throw new IllegalArgumentException(
                "El código protegido es obligatorio."
            );
        }

        String sql = """
            INSERT INTO usuarios
                (nombre_usuario, nombre_completo, codigo_hash)
            VALUES (?, ?, ?)
            """;

        try (Connection conexion = ConexionBD.abrir();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setString(1, usuario.getNombreUsuario());
            consulta.setString(2, usuario.getNombreCompleto());
            consulta.setString(3, codigoHash);

            consulta.executeUpdate();
        }

    }

    
    public boolean verificarAcceso(String nombreUsuario, String codigo)
            throws SQLException {

        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            return false;
        }

        String sql = """
            SELECT codigo_hash
            FROM usuarios
            WHERE nombre_usuario = ?
            """;

        try (Connection conexion = ConexionBD.abrir();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setString(1, nombreUsuario.trim());

            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    return false;
                }

                String hashGuardado =
                        resultado.getString("codigo_hash");

                return Seguridad.verificarCodigo(
                    codigo, hashGuardado
                );
            }
        }

    }

} 