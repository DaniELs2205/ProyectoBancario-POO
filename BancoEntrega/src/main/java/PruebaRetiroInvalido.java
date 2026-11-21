import java.math.BigDecimal;
import java.sql.SQLException;

public class PruebaRetiroInvalido {

    public static void main(String[] args) {
        OperacionDAO operaciones = new OperacionDAO();

        try {
            operaciones.retirar(
                10001L, new BigDecimal("200.00")
            );

            System.out.println(
                "FALLO: se permitió retirar más del saldo."
            );

        } catch (IllegalArgumentException e) {
            System.out.println(
                "Rechazo esperado: " + e.getMessage()
            );

        } catch (SQLException e) {
            System.out.println(
                "Error de base de datos: " + e.getMessage()
            );
        }
    }
}
