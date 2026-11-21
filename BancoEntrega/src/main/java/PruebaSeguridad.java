public class PruebaSeguridad {

    public static void main(String[] args) {
        String codigoPrueba = "PRUEBA123";

        String hash = Seguridad.protegerCodigo(codigoPrueba);

        boolean correcto = Seguridad.verificarCodigo(
                codigoPrueba, hash
        );

        boolean incorrecto = Seguridad.verificarCodigo(
                "OTRO456", hash
        );

        System.out.println("Código correcto aceptado: " + correcto);
        System.out.println("Código incorrecto aceptado: " + incorrecto);
    }
}
