import java.math.BigDecimal;
import java.sql.SQLException;

public class PruebaDepositoInvalido {

    public static void main(String[] args) {
        OperacionDAO operaciones = new OperacionDAO();

        try {
            operaciones.depositar(
                10001L, new BigDecimal("-10.00")
            );

            System.out.println(
                "FALLO: se aceptó un depósito negativo."
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
