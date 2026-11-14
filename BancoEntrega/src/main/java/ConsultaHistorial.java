import java.math.BigDecimal;
import java.util.Arrays;

public final class ConsultaHistorial {
    private final Usuario usuario;
    private final BigDecimal saldoInicial;
    private final BigDecimal saldoActual;
    private final Movimiento[] movimientos;

    public ConsultaHistorial(CuentaBancaria cuenta) {
        usuario = cuenta.getUsuario();
        saldoInicial = cuenta.getSaldoInicial();
        saldoActual = cuenta.getSaldo();
        movimientos = cuenta.getMovimientos();
    }
    public Usuario getUsuario() { return usuario; }
    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public BigDecimal getSaldoActual() { return saldoActual; }
    public Movimiento[] getMovimientos() { return Arrays.copyOf(movimientos, movimientos.length); }
    public Movimiento buscarMovimiento(int numero) {
        for (Movimiento movimiento : movimientos) {
            if (movimiento.getNumero() == numero) { return movimiento; }
        }
        throw new IllegalArgumentException("No existe un movimiento con ese número en la cuenta consultada.");
    }
}
