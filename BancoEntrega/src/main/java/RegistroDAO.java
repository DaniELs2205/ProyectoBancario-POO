import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class RegistroDAO {

    public CuentaSesion registrar(
            String nombreUsuario,
            String nombreCompleto,
            String codigo,
            BigDecimal saldoInicial) throws SQLException {

        // Reutiliza las validaciones de Usuario.
        Usuario usuario = new Usuario(
            nombreUsuario, nombreCompleto, codigo
        );

        if (usuario.getNombreCompleto().length() > 150) {
            throw new IllegalArgumentException(
                "El nombre completo admite hasta 150 caracteres."
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

        if (monto.compareTo(
                new BigDecimal("9999999999999.99")) > 0) {

            throw new IllegalArgumentException(
                "El saldo inicial supera el máximo permitido."
            );
        }

        String hash = Seguridad.protegerCodigo(
            usuario.getCodigo()
        );

        try (Connection conexion = ConexionBD.abrir()) {
            conexion.setAutoCommit(false);

            try {
                long usuarioId;
                long numeroCuenta;

                String insertarUsuario = """
                    INSERT INTO usuarios (
                        nombre_usuario,
                        nombre_completo,
                        codigo_hash
                    )
                    VALUES (?, ?, ?)
                    """;

                try (PreparedStatement consulta =
                        conexion.prepareStatement(
                            insertarUsuario,
                            Statement.RETURN_GENERATED_KEYS
                        )) {

                    consulta.setString(
                        1, usuario.getNombreUsuario()
                    );
                    consulta.setString(
                        2, usuario.getNombreCompleto()
                    );
                    consulta.setString(3, hash);

                    consulta.executeUpdate();

                    try (ResultSet claves =
                            consulta.getGeneratedKeys()) {

                        if (!claves.next()) {
                            throw new SQLException(
                                "No se obtuvo el identificador del usuario."
                            );
                        }

                        usuarioId = claves.getLong(1);
                    }
                }

                String insertarCuenta = """
                    INSERT INTO cuentas (
                        usuario_id,
                        saldo_inicial,
                        saldo
                    )
                    VALUES (?, ?, ?)
                    """;

                try (PreparedStatement consulta =
                        conexion.prepareStatement(
                            insertarCuenta,
                            Statement.RETURN_GENERATED_KEYS
                        )) {

                    consulta.setLong(1, usuarioId);
                    consulta.setBigDecimal(2, monto);
                    consulta.setBigDecimal(3, monto);

                    consulta.executeUpdate();

                    try (ResultSet claves =
                            consulta.getGeneratedKeys()) {

                        if (!claves.next()) {
                            throw new SQLException(
                                "No se obtuvo el número de cuenta."
                            );
                        }

                        numeroCuenta = claves.getLong(1);
                    }
                }

                CuentaSesion nueva = new CuentaSesion(
                    usuarioId,
                    usuario.getNombreUsuario(),
                    usuario.getNombreCompleto(),
                    numeroCuenta,
                    monto,
                    monto
                );

                conexion.commit();
                return nueva;

            } catch (SQLException | RuntimeException error) {
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    error.addSuppressed(errorRollback);
                }

                throw error;
            }
        }
    }
}

