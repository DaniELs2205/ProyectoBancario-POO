import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public class CuentaBancaria {
    private final Usuario usuario;
    private final BigDecimal saldoInicial;
    private BigDecimal saldo;
    private Movimiento[] movimientos = new Movimiento[10];
    private int cantidad;

    public CuentaBancaria(Usuario usuario, BigDecimal saldoInicial) {
        if (usuario == null) { throw new IllegalArgumentException("Registra un usuario."); }
        BigDecimal inicial = validarDinero(saldoInicial);
        if (inicial.signum() < 0) { throw new IllegalArgumentException("El saldo inicial no puede ser negativo."); }
        this.usuario = usuario;
        this.saldoInicial = inicial;
        this.saldo = inicial;
    }
    private static BigDecimal validarDinero(BigDecimal monto) {
        if (monto == null) { throw new IllegalArgumentException("Ingresa un monto."); }
        try { return monto.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Usa como máximo dos decimales."); }
    }
    private static BigDecimal validarPositivo(BigDecimal monto) {
        BigDecimal valor = validarDinero(monto);
        if (valor.signum() <= 0) { throw new IllegalArgumentException("El monto debe ser mayor que cero."); }
        return valor;
    }
    public Movimiento depositar(BigDecimal monto) {
        BigDecimal valor = validarPositivo(monto);
        Movimiento movimiento = new Deposito(cantidad + 1, valor, saldo, saldo.add(valor));
        registrar(movimiento);
        saldo = movimiento.getSaldoResultante();
        return movimiento;
    }
    public Movimiento retirar(BigDecimal monto) {
        BigDecimal valor = validarPositivo(monto);
        if (valor.compareTo(saldo) > 0) { throw new IllegalArgumentException("Fondos insuficientes."); }
        Movimiento movimiento = new Retiro(cantidad + 1, valor, saldo, saldo.subtract(valor));
        registrar(movimiento);
        saldo = movimiento.getSaldoResultante();
        return movimiento;
    }
    public Movimiento retirarTodo() {
        if (saldo.signum() == 0) { throw new IllegalArgumentException("No tienes saldo disponible para retirar."); }
        return retirar(saldo);
    }
    private void registrar(Movimiento movimiento) {
        if (cantidad == movimientos.length) {
            movimientos = Arrays.copyOf(movimientos, movimientos.length * 2);
        }
        movimientos[cantidad++] = movimiento;
    }
    public Movimiento buscarMovimiento(int numero) {
        for (int i = 0; i < cantidad; i++) {
            if (movimientos[i].getNumero() == numero) { return movimientos[i]; }
        }
        throw new IllegalArgumentException("No existe un movimiento con ese número.");
    }
    public BigDecimal totalDepositos() { return sumar("Depósito"); }
    public BigDecimal totalRetiros() { return sumar("Retiro"); }
    private BigDecimal sumar(String tipo) {
        BigDecimal total = new BigDecimal("0.00");
        for (int i = 0; i < cantidad; i++) {
            if (movimientos[i].getDescripcion().equals(tipo)) { total = total.add(movimientos[i].getMonto()); }
        }
        return total;
    }
    public Usuario getUsuario() { return usuario; }
    public BigDecimal getSaldo() { return saldo; }
    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public Movimiento[] getMovimientos() { return Arrays.copyOf(movimientos, cantidad); }
}
