import java.math.BigDecimal;
import java.sql.SQLException;

public class PruebaConsultarSaldo {

    public static void main(String[] args) {
        CuentaDAO dao = new CuentaDAO();

        try {
            BigDecimal saldo = dao.consultarSaldo(10001L);

            System.out.println("Cuenta: 10001");
            System.out.println("Saldo guardado: Q" + saldo);

        } catch (SQLException e) {
            System.out.println(
                "No se pudo consultar: " + e.getMessage()
            );
        }
    }
}
