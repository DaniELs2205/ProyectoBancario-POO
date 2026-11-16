import java.math.BigDecimal;

public final class Transferencia extends Movimiento {
    private final boolean entrada;
    private final String numeroCuentaContraparte;
    private final String usuarioContraparte;
    private final String referencia;

    public Transferencia(int numero, BigDecimal monto,
                         BigDecimal anterior, BigDecimal resultado,
                         boolean entrada, CuentaBancaria contraparte,
                         String referencia) {
        super(numero, monto, anterior, resultado);

        this.entrada = entrada;
        this.numeroCuentaContraparte = contraparte.getNumeroCuenta();
        this.usuarioContraparte =
                contraparte.getUsuario().getNombreUsuario();
        this.referencia = referencia;
    }

    public boolean esEntrada() {
        return entrada;
    }

    public String getNumeroCuentaContraparte() {
        return numeroCuentaContraparte;
    }

    @Override
    public String getReferencia() {
        return referencia;
    }

    @Override
    public String getDescripcion() {
        return (entrada
                ? "Transferencia recibida de "
                : "Transferencia enviada a ")
                + usuarioContraparte
                + " (" + numeroCuentaContraparte + ")";
    }
}