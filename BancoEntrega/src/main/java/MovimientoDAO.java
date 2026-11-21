import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MovimientoDAO {

    public List<MovimientoGuardado> listarPorCuenta(long numeroCuenta)
            throws SQLException {

        String sql = """
            SELECT
                numero_movimiento,
                fecha,
                tipo,
                monto,
                saldo_anterior,
                saldo_resultante,
                referencia,
                cuenta_contraparte,
                usuario_contraparte
            FROM movimientos
            WHERE numero_cuenta = ?
            ORDER BY numero_movimiento
            """;

        List<MovimientoGuardado> movimientos = new ArrayList<>();

        try (Connection conexion = ConexionBD.abrir();
             PreparedStatement consulta =
                     conexion.prepareStatement(sql)) {

            consulta.setLong(1, numeroCuenta);

            try (ResultSet resultado = consulta.executeQuery()) {

                while (resultado.next()) {
                    MovimientoGuardado movimiento =
                            new MovimientoGuardado(
                                resultado.getInt("numero_movimiento"),
                                resultado.getTimestamp("fecha")
                                         .toLocalDateTime(),
                                resultado.getString("tipo"),
                                resultado.getBigDecimal("monto"),
                                resultado.getBigDecimal("saldo_anterior"),
                                resultado.getBigDecimal("saldo_resultante"),
                                resultado.getString("referencia"),
                                resultado.getString("cuenta_contraparte"),
                                resultado.getString("usuario_contraparte")
                            );

                    movimientos.add(movimiento);
                }
            }
        }

        return movimientos;
    }
}


