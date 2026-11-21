import java.math.BigDecimal;
import java.sql.SQLException;

public class PruebaCrearCuenta {

    public static void main(String[] args) {
        CuentaDAO dao = new CuentaDAO();

        try {
            dao.crear(1L, new BigDecimal("100.00"));

            System.out.println(
                "Cuenta guardada con saldo inicial de Q100.00."
            );

        } catch (SQLException e) {
            System.out.println(
                "No se pudo crear la cuenta: " + e.getMessage()
            );
        }
    }
}
