<<<<<<< HEAD
import java.math.BigDecimal;
import java.util.Arrays;


public class Banco {
    private CuentaBancaria[] cuentas = new CuentaBancaria[10];
    private int cantidad;

    public CuentaBancaria registrar(String nombreUsuario, String titular, String codigo, BigDecimal inicial) {
        Usuario nuevo = new Usuario(nombreUsuario, titular, codigo);
        for (int i = 0; i < cantidad; i++) {
            Usuario existente = cuentas[i].getUsuario();
            if (existente.getCodigo().equals(nuevo.getCodigo())) {
                throw new IllegalArgumentException("Ese código ya está registrado. Elige uno diferente.");
            }
            if (existente.getNombreUsuario().equalsIgnoreCase(nuevo.getNombreUsuario())) {
                throw new IllegalArgumentException("Ese nombre de usuario ya está registrado.");
            }
        }
        
        CuentaBancaria cuenta = new CuentaBancaria(nuevo, inicial);
        if (cantidad == cuentas.length) { cuentas = Arrays.copyOf(cuentas, cuentas.length * 2); }
        cuentas[cantidad++] = cuenta;
        return cuenta;
    }
    public CuentaBancaria ingresar(String codigo) {
        String normalizado = Usuario.normalizarCodigo(codigo);
        for (int i = 0; i < cantidad; i++) {
            if (cuentas[i].getUsuario().getCodigo().equals(normalizado)) { return cuentas[i]; }
        }
        throw new IllegalArgumentException("Código no registrado. Revisa el código o crea tu usuario.");
    }
    public ConsultaHistorial consultarHistorial(String codigo) {
        // Buscar por código no modifica la cuenta elegida para operar en la interfaz.
        return new ConsultaHistorial(ingresar(codigo));
    }

    
    public Usuario[] getUsuarios() {
        Usuario[] usuarios = new Usuario[cantidad];
        for (int i = 0; i < cantidad; i++) { usuarios[i] = cuentas[i].getUsuario(); }
        return usuarios;
    }
    public int getCantidadUsuarios() { return cantidad; }
}
=======
package BancoEntrega.src.main.java;









>>>>>>> 7d469c6bb62c2bf01f59f4f267bab01867554cfb
