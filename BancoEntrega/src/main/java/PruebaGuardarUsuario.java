import java.sql.SQLException;

public class PruebaGuardarUsuario {

    public static void main(String[] args) {
        try {
            Usuario usuario = new Usuario(
                "usuario_prueba",
                "Persona de Prueba",
                "PRUEBA123"
            );

            String hash = Seguridad.protegerCodigo(
                usuario.getCodigo()
            );

            UsuarioDAO dao = new UsuarioDAO();
            dao.guardar(usuario, hash);

            System.out.println("Usuario guardado en MySQL.");

        } catch (SQLException e) {
            System.out.println(
                "No se pudo guardar: " + e.getMessage()
            );
        }
    }
}
