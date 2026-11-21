import java.math.BigDecimal;

public record CuentaSesion(
        long usuarioId,
        String nombreUsuario,
        String nombreCompleto,
        long numeroCuenta,
        BigDecimal saldoInicial,
        BigDecimal saldo
) {
}


