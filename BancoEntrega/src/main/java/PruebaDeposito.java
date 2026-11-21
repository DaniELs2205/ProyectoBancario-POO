import java.math.BigDecimal;
import java.sql.SQLException;

public class PruebaDeposito {

    public static void main(String[] args) {
        OperacionDAO operaciones = new OperacionDAO();

        try {
            operaciones.depositar(
                10001L, new BigDecimal("25.00")
            );

            System.out.println("Depósito realizado.");

            CuentaDAO cuentas = new CuentaDAO();
            System.out.println(
                "Saldo actual: Q" + cuentas.consultarSaldo(10001L)
            );

        } catch (SQLException | IllegalArgumentException e) {
            System.out.println(
                "Error durante la prueba: " + e.getMessage()
            );
        }
    }
}
