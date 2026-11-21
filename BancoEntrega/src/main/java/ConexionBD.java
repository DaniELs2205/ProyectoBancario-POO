import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    public static Connection abrir() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/banco_proyecto";
        String usuario = "root";
        String contrasena = System.getenv("BANCO_DB_PASSWORD");

        if (contrasena == null || contrasena.isEmpty()) {
            throw new SQLException(
                "Falta configurar BANCO_DB_PASSWORD."
            );
        }

        return DriverManager.getConnection(url, usuario, contrasena);
    }
}

