    import java.math.BigDecimal;
    import java.util.Arrays;

    public class Banco {
        private CuentaBancaria[] cuentas = new CuentaBancaria[10];
        private int cantidad;

        public CuentaBancaria registrar(String nombreUsuario, String titular,
                                    String codigo, BigDecimal inicial) {
            Usuario nuevo = new Usuario(nombreUsuario, titular, codigo);

            for (int i = 0; i < cantidad; i++) {
                Usuario existente = cuentas[i].getUsuario();

                if (existente.getCodigo().equals(nuevo.getCodigo())) {
                    throw new IllegalArgumentException(
                            "Ese código ya está registrado. Elige uno diferente.");
                }

                if (existente.getNombreUsuario()
                        .equalsIgnoreCase(nuevo.getNombreUsuario())) {
                    throw new IllegalArgumentException(
                            "Ese nombre de usuario ya está registrado.");
                }
            }

            CuentaBancaria cuenta = new CuentaBancaria(nuevo, inicial);

            if (cantidad == cuentas.length) {
                cuentas = Arrays.copyOf(cuentas, cuentas.length * 2);
            }

            cuentas[cantidad++] = cuenta;
            return cuenta;
        }

        public CuentaBancaria ingresar(String codigo) {
            String normalizado = Usuario.normalizarCodigo(codigo);

            for (int i = 0; i < cantidad; i++) {
                if (cuentas[i].getUsuario().getCodigo().equals(normalizado)) {
                    return cuentas[i];
                }
            }

            throw new IllegalArgumentException(
                    "Código no registrado. Revisa el código o crea tu usuario.");
        }

        public CuentaBancaria buscarPorNumeroCuenta(String numero) {
            if (numero == null || numero.isBlank()) {
                throw new IllegalArgumentException(
                        "Ingresa el número de cuenta destino.");
            }

            String normalizado = numero.trim();

            for (int i = 0; i < cantidad; i++) {
                if (cuentas[i].getNumeroCuenta().equals(normalizado)) {
                    return cuentas[i];
                }
            }

            throw new IllegalArgumentException(
                    "Número de cuenta no registrado. "
                    + "Usa el número de cuenta, no el código de acceso.");
        }

        public ConsultaHistorial consultarHistorial(String codigo) {
            return new ConsultaHistorial(ingresar(codigo));
        }

        public CuentaBancaria[] getCuentas() {
            return Arrays.copyOf(cuentas, cantidad);
        }

        public Usuario[] getUsuarios() {
            Usuario[] usuarios = new Usuario[cantidad];

            for (int i = 0; i < cantidad; i++) {
                usuarios[i] = cuentas[i].getUsuario();
            }

            return usuarios;
        }

        public int getCantidadUsuarios() {
            return cantidad;
        }
    }