import java.sql.SQLException;

public class PruebaSegundoUsuario {

    public static void main(String[] args) {
        try {
            Usuario usuario = new Usuario(
                "usuario_destino",
                "Persona Destino",
                "DESTINO456"
            );

            String hash = Seguridad.protegerCodigo(
                usuario.getCodigo()
            );

            UsuarioDAO dao = new UsuarioDAO();
            dao.guardar(usuario, hash);

            System.out.println("Segundo usuario guardado.");

        } catch (SQLException e) {
            System.out.println(
                "No se pudo guardar: " + e.getMessage()
            );
        }
    }
}
