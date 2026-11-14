import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class Movimiento {
    private final int numero;
    private final LocalDateTime fecha;
    private final BigDecimal monto;
    private final BigDecimal saldoAnterior;
    private final BigDecimal saldoResultante;

    protected Movimiento(int numero, BigDecimal monto, BigDecimal anterior, BigDecimal resultado) {
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo.");
        }
        this.numero = numero;
        this.fecha = LocalDateTime.now();
        this.monto = monto;
        this.saldoAnterior = anterior;
        this.saldoResultante = resultado;
    }
    public abstract String getDescripcion();
    public int getNumero() { return numero; }
    public String getReferencia() { return String.format("UVG-%06d", numero); }
    public LocalDateTime getFecha() { return fecha; }
    public BigDecimal getMonto() { return monto; }
    public BigDecimal getSaldoAnterior() { return saldoAnterior; }
    public BigDecimal getSaldoResultante() { return saldoResultante; }

    @Override
    public String toString() {
        return String.format("%03d  %-16s %-10s Q%10s  Q%10s", numero,
                fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                getDescripcion(), Dinero.formatear(monto), Dinero.formatear(saldoResultante));
    }
}