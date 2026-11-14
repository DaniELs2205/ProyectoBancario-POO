import java.util.Locale;

public class Usuario {
    private final String nombreUsuario;
    private final String nombreCompleto;
    private final String codigo;

    public Usuario(String nombreUsuario, String nombreCompleto) {
        this(nombreUsuario, nombreCompleto, nombreUsuario);
    }
    public Usuario(String nombreUsuario, String nombreCompleto, String codigo) {
        if (nombreUsuario == null || !nombreUsuario.trim().matches("[A-Za-z0-9_]{3,20}")) {
            throw new IllegalArgumentException("El usuario debe tener de 3 a 20 letras, números o guion bajo.");
        }
        if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del titular es obligatorio.");
        }
        this.codigo = normalizarCodigo(codigo);
        this.nombreUsuario = nombreUsuario.trim();
        this.nombreCompleto = nombreCompleto.trim();
    }
    public static String normalizarCodigo(String codigo) {
        if (codigo == null || !codigo.trim().matches("[A-Za-z0-9]{3,12}")) {
            throw new IllegalArgumentException("El código debe tener de 3 a 12 letras o números, sin espacios.");
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getCodigo() { return codigo; }
}
