import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class Comprobante {
    private Comprobante() { }

    public static String generar(Usuario usuario, Movimiento movimiento) {
        return "\nBANCO UVG - COMPROBANTE DE OPERACIÓN\n"
                + "Referencia: " + movimiento.getReferencia() + "\n"
                + "Usuario: " + usuario.getNombreUsuario() + "\n"
                + "Titular: " + usuario.getNombreCompleto() + "\n"
                + "Código de cuenta: " + usuario.getCodigo() + "\n"
                + "Fecha: " + movimiento.getFecha().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n"
                + "Operación: " + movimiento.getDescripcion() + "\n"
                + "Monto: Q" + Dinero.formatear(movimiento.getMonto()) + "\n"
                + "Saldo anterior: Q"
                + Dinero.formatear(movimiento.getSaldoAnterior()) + "\n"
                + "Saldo actual: Q"
                + Dinero.formatear(movimiento.getSaldoResultante()) + "\n"
                + "Estado: Operación exitosa\n"
                + "Simulación académica. Sin validez bancaria.\n";
    }

    public static Path guardar(Usuario usuario, Movimiento movimiento)
            throws IOException {
        Path carpeta = Path.of("comprobantes");
        Files.createDirectories(carpeta);

        String nombre = usuario.getNombreUsuario()
                + "-" + movimiento.getReferencia()
                + "-" + UUID.randomUUID().toString().substring(0, 8)
                + ".txt";

        return Files.writeString(
                carpeta.resolve(nombre),
                generar(usuario, movimiento),
                StandardCharsets.UTF_8
        );
    }
}