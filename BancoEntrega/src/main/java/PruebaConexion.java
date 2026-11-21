import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PruebaConexion {

    public static void main(String[] args) {

        
        try (Connection conexion = ConexionBD.abrir()) {

            System.out.println("Conexión con MySQL exitosa.");
            System.out.println(
                "Base de datos: " + conexion.getCatalog()
            );

            
            String sql = "SELECT COUNT(*) AS total FROM usuarios";

            
            try (PreparedStatement consulta =
                        conexion.prepareStatement(sql);
                 ResultSet resultado = consulta.executeQuery()) {

                //
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