import java.math.BigDecimal;

public class Deposito extends Movimiento {
    public Deposito(int numero, BigDecimal monto, BigDecimal anterior, BigDecimal resultado) {
        super(numero, monto, anterior, resultado);
    }
    @Override
    public String getDescripcion() { return "Depósito"; }
}
