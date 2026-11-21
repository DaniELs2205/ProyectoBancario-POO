import java.math.BigDecimal;
import java.sql.SQLException;

public class PruebaCuentaDestino {

    public static void main(String[] args) {
        CuentaDAO dao = new CuentaDAO();

        try {
            dao.crear(2L, new BigDecimal("0.00"));

            System.out.println("Cuenta destino creada.");

        } catch (SQLException e) {
            System.out.println(
                "No se pudo crear: " + e.getMessage()
            );
        }
    }
}
