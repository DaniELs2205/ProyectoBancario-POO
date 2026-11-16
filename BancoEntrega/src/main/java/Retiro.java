import java.math.BigDecimal;

public class Retiro extends Movimiento {
    public Retiro(int numero, BigDecimal monto,
                  BigDecimal anterior, BigDecimal resultado) {
        super(numero, monto, anterior, resultado);
    }

    @Override
    public String getDescripcion() {
        return "Retiro";
    }
}