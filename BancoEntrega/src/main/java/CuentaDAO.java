import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CuentaDAO {

    public void crear(long usuarioId, BigDecimal saldoInicial)
    
            throws SQLException {

        if (usuarioId <= 0) {
            throw new IllegalArgumentException(
                "El identificador del usuario debe ser positivo."
            );
        }

        if (saldoInicial == null || saldoInicial.signum() < 0) {
            throw new IllegalArgumentException(
                "El saldo inicial no puede ser nulo ni negativo."
            );
        }

        BigDecimal monto;

        try {
            monto = saldoInicial.setScale(
                2, RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                "Usa como máximo dos decimales."
            );
        }

        String sql = """
            INSERT INTO cuentas
                (usuario_id, saldo_inicial, saldo)
            VALUES (?, ?, ?)
            """;

        try (Connection conexion = ConexionBD.abrir();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setLong(1, usuarioId);
            consulta.setBigDecimal(2, monto);
            consulta.setBigDecimal(3, monto);

            consulta.executeUpdate();
        }
    }

    public BigDecimal consultarSaldo(long numeroCuenta)
        throws SQLException {

    String sql = """
        SELECT saldo
        FROM cuentas
        WHERE numero_cuenta = ?
        """;

    try (Connection conexion = ConexionBD.abrir();
         PreparedStatement consulta =
                 conexion.prepareStatement(sql)) {

        consulta.setLong(1, numeroCuenta);

        try (ResultSet resultado = consulta.executeQuery()) {
            if (!resultado.next()) {
                throw new SQLException(
                    "No existe la cuenta " + numeroCuenta
                );
            }

            return resultado.getBigDecimal("saldo");
        }
    }
}

public CuentaSesion buscarPorUsuario(String nombreUsuario)
        throws SQLException {

    if (nombreUsuario == null || nombreUsuario.isBlank()) {
        throw new IllegalArgumentException(
            "El nombre de usuario es obligatorio."
        );
    }

    String sql = """
        SELECT
            u.id AS usuario_id,
            u.nombre_usuario,
            u.nombre_completo,
            c.numero_cuenta,
            c.saldo_inicial,
            c.saldo
        FROM usuarios u
        JOIN cuentas c ON c.usuario_id = u.id
        WHERE u.nombre_usuario = ?
        """;

    try (Connection conexion = ConexionBD.abrir();
         PreparedStatement consulta =
                 conexion.prepareStatement(sql)) {

        consulta.setString(1, nombreUsuario.trim());

        try (ResultSet resultado = consulta.executeQuery()) {
            if (!resultado.next()) {
                throw new SQLException(
                    "No se encontró una cuenta para ese usuario."
                );
            }

            return new CuentaSesion(
                resultado.getLong("usuario_id"),
                resultado.getString("nombre_usuario"),
                resultado.getString("nombre_completo"),
                resultado.getLong("numero_cuenta"),
                resultado.getBigDecimal("saldo_inicial"),
                resultado.getBigDecimal("saldo")
            );
        }
    }
}

public ResumenCuenta consultarResumen(long numeroCuenta)
        throws SQLException {

    String sql = """
        SELECT
            c.saldo,
            COALESCE(SUM(
                CASE WHEN m.tipo = 'DEPOSITO'
                     THEN m.monto ELSE 0 END
            ), 0) AS total_depositos,
            COALESCE(SUM(
                CASE WHEN m.tipo = 'RETIRO'
                     THEN m.monto ELSE 0 END
            ), 0) AS total_retiros,
            COUNT(m.id) AS cantidad_movimientos
        FROM cuentas c
        LEFT JOIN movimientos m
            ON m.numero_cuenta = c.numero_cuenta
        WHERE c.numero_cuenta = ?
        GROUP BY c.numero_cuenta, c.saldo
        """;

    try (Connection conexion = ConexionBD.abrir();
         PreparedStatement consulta =
                 conexion.prepareStatement(sql)) {

        consulta.setLong(1, numeroCuenta);

        try (ResultSet resultado = consulta.executeQuery()) {
            if (!resultado.next()) {
                throw new SQLException(
                    "No existe la cuenta " + numeroCuenta
                );
            }

            return new ResumenCuenta(
                resultado.getBigDecimal("saldo"),
                resultado.getBigDecimal("total_depositos"),
                resultado.getBigDecimal("total_retiros"),
                resultado.getLong("cantidad_movimientos")
            );
        }
    }
}

public List<CuentaDirectorio> listarDirectorio()
        throws SQLException {

    String sql = """
        SELECT
            u.nombre_usuario,
            u.nombre_completo,
            c.numero_cuenta
        FROM cuentas c
        JOIN usuarios u ON u.id = c.usuario_id
        ORDER BY c.numero_cuenta
        """;

    List<CuentaDirectorio> cuentas = new ArrayList<>();

    try (Connection conexion = ConexionBD.abrir();
         PreparedStatement consulta =
                 conexion.prepareStatement(sql);
         ResultSet resultado = consulta.executeQuery()) {

        while (resultado.next()) {
            cuentas.add(
                new CuentaDirectorio(
                    resultado.getString("nombre_usuario"),
                    resultado.getString("nombre_completo"),
                    resultado.getLong("numero_cuenta")
                )
            );
        }
    }

    return cuentas;
}

}

