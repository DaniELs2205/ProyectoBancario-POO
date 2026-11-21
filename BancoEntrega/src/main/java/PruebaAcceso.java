import java.sql.SQLException;

public class PruebaAcceso {

    public static void main(String[] args) {
        UsuarioDAO dao = new UsuarioDAO();

        try {
            boolean correcto = dao.verificarAcceso(
                "usuario_prueba", "PRUEBA123"
            );

            boolean codigoIncorrecto = dao.verificarAcceso(
                "usuario_prueba", "OTRO456"
            );

            boolean usuarioInexistente = dao.verificarAcceso(
                "usuario_inexistente", "PRUEBA123"
            );

            System.out.println("Acceso correcto: " + correcto);
            System.out.println(
                "Código incorrecto aceptado: " + codigoIncorrecto
            );
            System.out.println(
                "Usuario inexistente aceptado: " + usuarioInexistente
            );

        } catch (SQLException e) {
            System.out.println(
                "Error al consultar MySQL: " + e.getMessage()
            );
        }
    }
}
