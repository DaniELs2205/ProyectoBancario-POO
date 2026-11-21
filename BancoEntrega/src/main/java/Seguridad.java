import at.favre.lib.crypto.bcrypt.BCrypt;

public class Seguridad {

    public static String protegerCodigo(String codigo) {
        String normalizado = Usuario.normalizarCodigo(codigo);

        return BCrypt.withDefaults()
                .hashToString(12, normalizado.toCharArray());
    }

    public static boolean verificarCodigo(
            String codigo, String hashGuardado) {

        String normalizado = Usuario.normalizarCodigo(codigo);

        return BCrypt.verifyer()
                .verify(normalizado.toCharArray(), hashGuardado)
                .verified;
    }
}
