import java.math.BigDecimal;

public record ResumenCuenta(
        BigDecimal saldo,
        BigDecimal totalDepositos,
        BigDecimal totalRetiros,
        long cantidadMovimientos
) {
}
