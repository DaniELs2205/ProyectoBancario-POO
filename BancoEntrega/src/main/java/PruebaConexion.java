import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PruebaConexion {

    public static void main(String[] args) {

        // 1. Abrir la conexión.
        try (Connection conexion = ConexionBD.abrir()) {

            System.out.println("Conexión con MySQL exitosa.");
            System.out.println(
                "Base de datos: " + conexion.getCatalog()
            );

            // 2. Preparar la consulta.
            String sql = "SELECT COUNT(*) AS total FROM usuarios";

            // 3. Ejecutar la consulta.
            try (PreparedStatement consulta =
                        conexion.prepareStatement(sql);
                 ResultSet resultado = consulta.executeQuery()) {

                // 4. Leer el resultado.
                if (resultado.next()) {
                    int total = resultado.getInt("total");
                    System.out.println("Usuarios guardados: " + total);
                }
            }

        } catch (SQLException e) {
            System.out.println(
                "Error al conectar o consultar: " + e.getMessage()
            );
        }
    }
}