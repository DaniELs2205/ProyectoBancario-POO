import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class OperacionDAO {

    public void depositar(long numeroCuenta, BigDecimal monto)
            throws SQLException {

        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException(
                "El monto debe ser mayor que cero."
            );
        }

        BigDecimal valor;

        try {
            valor = monto.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                "Usa como máximo dos decimales."
            );
        }

        try (Connection conexion = ConexionBD.abrir()) {

            // Los cambios se confirmarán juntos al final.
            conexion.setAutoCommit(false);

            try {
                BigDecimal saldoAnterior;

                // Bloquea esta cuenta mientras se realiza la operación.
                String buscarCuenta = """
                    SELECT saldo
                    FROM cuentas
                    WHERE numero_cuenta = ?
                    FOR UPDATE
                    """;

                try (PreparedStatement consulta =
                        conexion.prepareStatement(buscarCuenta)) {

                    consulta.setLong(1, numeroCuenta);

                    try (ResultSet resultado = consulta.executeQuery()) {
                        if (!resultado.next()) {
                            throw new SQLException("La cuenta no existe.");
                        }

                        saldoAnterior = resultado.getBigDecimal("saldo");
                    }
                }

                BigDecimal saldoNuevo = saldoAnterior.add(valor);
                int siguienteNumero;

                String buscarNumero = """
                    SELECT COALESCE(MAX(numero_movimiento), 0) + 1
                        AS siguiente
                    FROM movimientos
                    WHERE numero_cuenta = ?
                    """;

                try (PreparedStatement consulta =
                        conexion.prepareStatement(buscarNumero)) {

                    consulta.setLong(1, numeroCuenta);

                    try (ResultSet resultado = consulta.executeQuery()) {
                        resultado.next();
                        siguienteNumero = resultado.getInt("siguiente");
                    }
                }

                String actualizarSaldo = """
                    UPDATE cuentas
                    SET saldo = ?
                    WHERE numero_cuenta = ?
                    """;

                try (PreparedStatement consulta =
                        conexion.prepareStatement(actualizarSaldo)) {

                    consulta.setBigDecimal(1, saldoNuevo);
                    consulta.setLong(2, numeroCuenta);
                    consulta.executeUpdate();
                }

                String registrarMovimiento = """
                    INSERT INTO movimientos (
                        numero_cuenta,
                        numero_movimiento,
                        tipo,
                        monto,
                        saldo_anterior,
                        saldo_resultante,
                        referencia
                    )
                    VALUES (?, ?, 'DEPOSITO', ?, ?, ?, ?)
                    """;

                try (PreparedStatement consulta =
                        conexion.prepareStatement(registrarMovimiento)) {

                    consulta.setLong(1, numeroCuenta);
                    consulta.setInt(2, siguienteNumero);
                    consulta.setBigDecimal(3, valor);
                    consulta.setBigDecimal(4, saldoAnterior);
                    consulta.setBigDecimal(5, saldoNuevo);
                    consulta.setString(
                        6, "UVG-DP-" + UUID.randomUUID()
                    );

                    consulta.executeUpdate();
                }

                // Confirma el saldo y el movimiento juntos.
                conexion.commit();

            } catch (SQLException | RuntimeException e) {
                // Deshace los cambios si ocurre un error.
                conexion.rollback();
                throw e;
            }
        }
    }

    public void retirar(long numeroCuenta, BigDecimal monto)
        throws SQLException {

    if (monto == null || monto.signum() <= 0) {
        throw new IllegalArgumentException(
            "El monto debe ser mayor que cero."
        );
    }

    BigDecimal valor;

    try {
        valor = monto.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException e) {
        throw new IllegalArgumentException(
            "Usa como máximo dos decimales."
        );
    }

    try (Connection conexion = ConexionBD.abrir()) {

        conexion.setAutoCommit(false);

        try {
            BigDecimal saldoAnterior;

            String buscarCuenta = """
                SELECT saldo
                FROM cuentas
                WHERE numero_cuenta = ?
                FOR UPDATE
                """;

            try (PreparedStatement consulta =
                    conexion.prepareStatement(buscarCuenta)) {

                consulta.setLong(1, numeroCuenta);

                try (ResultSet resultado = consulta.executeQuery()) {
                    if (!resultado.next()) {
                        throw new SQLException("La cuenta no existe.");
                    }

                    saldoAnterior = resultado.getBigDecimal("saldo");
                }
            }

            // No permite retirar más del saldo disponible.
            if (valor.compareTo(saldoAnterior) > 0) {
                throw new IllegalArgumentException(
                    "Fondos insuficientes."
                );
            }

            BigDecimal saldoNuevo = saldoAnterior.subtract(valor);
            int siguienteNumero;

            String buscarNumero = """
                SELECT COALESCE(MAX(numero_movimiento), 0) + 1
                    AS siguiente
                FROM movimientos
                WHERE numero_cuenta = ?
                """;

            try (PreparedStatement consulta =
                    conexion.prepareStatement(buscarNumero)) {

                consulta.setLong(1, numeroCuenta);

                try (ResultSet resultado = consulta.executeQuery()) {
                    resultado.next();
                    siguienteNumero = resultado.getInt("siguiente");
                }
            }

            String actualizarSaldo = """
                UPDATE cuentas
                SET saldo = ?
                WHERE numero_cuenta = ?
                """;

            try (PreparedStatement consulta =
                    conexion.prepareStatement(actualizarSaldo)) {

                consulta.setBigDecimal(1, saldoNuevo);
                consulta.setLong(2, numeroCuenta);
                consulta.executeUpdate();
            }

            String registrarMovimiento = """
                INSERT INTO movimientos (
                    numero_cuenta,
                    numero_movimiento,
                    tipo,
                    monto,
                    saldo_anterior,
                    saldo_resultante,
                    referencia
                )
                VALUES (?, ?, 'RETIRO', ?, ?, ?, ?)
                """;

            try (PreparedStatement consulta =
                    conexion.prepareStatement(registrarMovimiento)) {

                consulta.setLong(1, numeroCuenta);
                consulta.setInt(2, siguienteNumero);
                consulta.setBigDecimal(3, valor);
                consulta.setBigDecimal(4, saldoAnterior);
                consulta.setBigDecimal(5, saldoNuevo);
                consulta.setString(
                    6, "UVG-RT-" + UUID.randomUUID()
                );

                consulta.executeUpdate();
            }

            conexion.commit();

        } catch (SQLException | RuntimeException e) {
            conexion.rollback();
            throw e;
        }
    }
}

private BigDecimal bloquearYConsultarSaldo(
        Connection conexion, long numeroCuenta)
        throws SQLException {

    String sql = """
        SELECT saldo
        FROM cuentas
        WHERE numero_cuenta = ?
        FOR UPDATE
        """;

    try (PreparedStatement consulta =
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

public void transferir(
        long cuentaOrigen,
        long cuentaDestino,
        BigDecimal monto) throws SQLException {

    if (cuentaOrigen == cuentaDestino) {
        throw new IllegalArgumentException(
            "No puedes transferir a la misma cuenta."
        );
    }

    if (monto == null || monto.signum() <= 0) {
        throw new IllegalArgumentException(
            "El monto debe ser mayor que cero."
        );
    }

    BigDecimal valor;

    try {
        valor = monto.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException e) {
        throw new IllegalArgumentException(
            "Usa como máximo dos decimales."
        );
    }

    try (Connection conexion = ConexionBD.abrir()) {
        conexion.setAutoCommit(false);

        try {
            // Bloquear las cuentas siempre en el mismo orden.
            long primera = Math.min(cuentaOrigen, cuentaDestino);
            long segunda = Math.max(cuentaOrigen, cuentaDestino);

            BigDecimal saldoPrimera =
                    bloquearYConsultarSaldo(conexion, primera);

            BigDecimal saldoSegunda =
                    bloquearYConsultarSaldo(conexion, segunda);

            BigDecimal saldoOrigen =
                    cuentaOrigen == primera
                        ? saldoPrimera : saldoSegunda;

            BigDecimal saldoDestino =
                    cuentaDestino == primera
                        ? saldoPrimera : saldoSegunda;

            if (valor.compareTo(saldoOrigen) > 0) {
                throw new IllegalArgumentException(
                    "Fondos insuficientes."
                );
            }

            BigDecimal nuevoOrigen = saldoOrigen.subtract(valor);
            BigDecimal nuevoDestino = saldoDestino.add(valor);

            String actualizar = """
                UPDATE cuentas
                SET saldo = ?
                WHERE numero_cuenta = ?
                """;

            try (PreparedStatement consulta =
                    conexion.prepareStatement(actualizar)) {

                consulta.setBigDecimal(1, nuevoOrigen);
                consulta.setLong(2, cuentaOrigen);
                consulta.executeUpdate();

                consulta.setBigDecimal(1, nuevoDestino);
                consulta.setLong(2, cuentaDestino);
                consulta.executeUpdate();
            }

            String referencia = "UVG-TR-" + UUID.randomUUID();

            // Registrar la salida de dinero.
            registrarTransferencia(
                conexion,
                cuentaOrigen,
                cuentaDestino,
                false,
                valor,
                saldoOrigen,
                nuevoOrigen,
                referencia
            );

            // Registrar la entrada de dinero.
            registrarTransferencia(
                conexion,
                cuentaDestino,
                cuentaOrigen,
                true,
                valor,
                saldoDestino,
                nuevoDestino,
                referencia
            );

            conexion.commit();

        } catch (SQLException | RuntimeException e) {
            conexion.rollback();
            throw e;
        }
    }
}

private void registrarTransferencia(
        Connection conexion,
        long numeroCuenta,
        long cuentaContraparte,
        boolean entrada,
        BigDecimal monto,
        BigDecimal saldoAnterior,
        BigDecimal saldoNuevo,
        String referencia) throws SQLException {

    int siguienteNumero;

    String buscarNumero = """
        SELECT COALESCE(MAX(numero_movimiento), 0) + 1
            AS siguiente
        FROM movimientos
        WHERE numero_cuenta = ?
        """;

    try (PreparedStatement consulta =
            conexion.prepareStatement(buscarNumero)) {

        consulta.setLong(1, numeroCuenta);

        try (ResultSet resultado = consulta.executeQuery()) {
            resultado.next();
            siguienteNumero = resultado.getInt("siguiente");
        }
    }

    String buscarUsuario = """
        SELECT u.nombre_usuario
        FROM usuarios u
        JOIN cuentas c ON c.usuario_id = u.id
        WHERE c.numero_cuenta = ?
        """;

    String usuarioContraparte;

    try (PreparedStatement consulta =
            conexion.prepareStatement(buscarUsuario)) {

        consulta.setLong(1, cuentaContraparte);

        try (ResultSet resultado = consulta.executeQuery()) {
            if (!resultado.next()) {
                throw new SQLException(
                    "No existe la cuenta contraparte."
                );
            }

            usuarioContraparte =
                    resultado.getString("nombre_usuario");
        }
    }



    String sql = """
        INSERT INTO movimientos (
            numero_cuenta,
            numero_movimiento,
            tipo,
            monto,
            saldo_anterior,
            saldo_resultante,
            referencia,
            cuenta_contraparte,
            usuario_contraparte
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    try (PreparedStatement consulta =
            conexion.prepareStatement(sql)) {

        consulta.setLong(1, numeroCuenta);
        consulta.setInt(2, siguienteNumero);
        consulta.setString(
            3,
            entrada
                ? "TRANSFERENCIA_ENTRADA"
                : "TRANSFERENCIA_SALIDA"
        );
        consulta.setBigDecimal(4, monto);
        consulta.setBigDecimal(5, saldoAnterior);
        consulta.setBigDecimal(6, saldoNuevo);
        consulta.setString(7, referencia);
        consulta.setLong(8, cuentaContraparte);
        consulta.setString(9, usuarioContraparte);

        consulta.executeUpdate();
    }
}

public BigDecimal retirarTodo(long numeroCuenta)
        throws SQLException {

    try (Connection conexion = ConexionBD.abrir()) {
        conexion.setAutoCommit(false);

        try {
            BigDecimal saldoAnterior =
                    bloquearYConsultarSaldo(conexion, numeroCuenta);

            if (saldoAnterior.signum() <= 0) {
                throw new IllegalArgumentException(
                    "No tienes saldo disponible para retirar."
                );
            }

            int siguienteNumero;

            String buscarNumero = """
                SELECT COALESCE(MAX(numero_movimiento), 0) + 1
                    AS siguiente
                FROM movimientos
                WHERE numero_cuenta = ?
                """;

            try (PreparedStatement consulta =
                    conexion.prepareStatement(buscarNumero)) {

                consulta.setLong(1, numeroCuenta);

                try (ResultSet resultado = consulta.executeQuery()) {
                    resultado.next();
                    siguienteNumero = resultado.getInt("siguiente");
                }
            }

            BigDecimal saldoNuevo = new BigDecimal("0.00");

            String actualizarSaldo = """
                UPDATE cuentas
                SET saldo = ?
                WHERE numero_cuenta = ?
                """;

            try (PreparedStatement consulta =
                    conexion.prepareStatement(actualizarSaldo)) {

                consulta.setBigDecimal(1, saldoNuevo);
                consulta.setLong(2, numeroCuenta);
                consulta.executeUpdate();
            }

            String registrarMovimiento = """
                INSERT INTO movimientos (
                    numero_cuenta,
                    numero_movimiento,
                    tipo,
                    monto,
                    saldo_anterior,
                    saldo_resultante,
                    referencia
                )
                VALUES (?, ?, 'RETIRO', ?, ?, ?, ?)
                """;

            try (PreparedStatement consulta =
                    conexion.prepareStatement(registrarMovimiento)) {

                consulta.setLong(1, numeroCuenta);
                consulta.setInt(2, siguienteNumero);
                consulta.setBigDecimal(3, saldoAnterior);
                consulta.setBigDecimal(4, saldoAnterior);
                consulta.setBigDecimal(5, saldoNuevo);
                consulta.setString(
                    6, "UVG-RT-" + UUID.randomUUID()
                );

                consulta.executeUpdate();
            }

            conexion.commit();

            // Devuelve el monto que realmente se retiró.
            return saldoAnterior;

        } catch (SQLException | RuntimeException e) {
            conexion.rollback();
            throw e;
        }
    }
}

}
