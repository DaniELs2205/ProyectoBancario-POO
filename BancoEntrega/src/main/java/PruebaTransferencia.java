import java.math.BigDecimal;
import java.sql.SQLException;

public class PruebaTransferencia {

    public static void main(String[] args) {
        OperacionDAO operaciones = new OperacionDAO();

        try {
            operaciones.transferir(
                10001L,
                10002L,
                new BigDecimal("30.00")
            );

            System.out.println("Transferencia realizada.");

        } catch (SQLException | IllegalArgumentException e) {
            System.out.println(
                "No se pudo transferir: " + e.getMessage()
            );
        }
    }
}
