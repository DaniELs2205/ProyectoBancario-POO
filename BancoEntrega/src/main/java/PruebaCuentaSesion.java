import java.sql.SQLException;

public class PruebaCuentaSesion {

    public static void main(String[] args) {
        CuentaDAO dao = new CuentaDAO();

        try {
            CuentaSesion cuenta =
                    dao.buscarPorUsuario("usuario_prueba");

            System.out.println("Usuario: " + cuenta.nombreUsuario());
            System.out.println("Titular: " + cuenta.nombreCompleto());
            System.out.println("Cuenta: " + cuenta.numeroCuenta());
            System.out.println("Saldo: Q" + cuenta.saldo());

        } catch (SQLException e) {
            System.out.println(
                "No se pudo consultar la cuenta: " + e.getMessage()
            );
        }
    }
}