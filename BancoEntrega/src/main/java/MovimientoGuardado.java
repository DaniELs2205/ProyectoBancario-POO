import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoGuardado(
        int numero,
        LocalDateTime fecha,
        String tipo,
        BigDecimal monto,
        BigDecimal saldoAnterior,
        BigDecimal saldoResultante,
        String referencia,
        String cuentaContraparte,
        String usuarioContraparte
) {

    public String descripcion() {
        return switch (tipo) {
            case "DEPOSITO" -> "Depósito";

            case "RETIRO" -> "Retiro";

            case "TRANSFERENCIA_ENTRADA" ->
                "Transferencia recibida de "
                + usuarioContraparte
                + " (" + cuentaContraparte + ")";

            case "TRANSFERENCIA_SALIDA" ->
                "Transferencia enviada a "
                + usuarioContraparte
                + " (" + cuentaContraparte + ")";

            default -> tipo;
        };
    }


}

