import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class Comprobante {
    private Comprobante() { }

    public static String generar(Usuario usuario, Movimiento movimiento) {
        return "BANCO UVG - COMPROBANTE DE OPERACIÓN\n"
                + "Referencia: " + movimiento.getReferencia() + "\n"
                + "Usuario: " + usuario.getNombreUsuario() + "\n"
                + "Titular: " + usuario.getNombreCompleto() + "\n"
                + "Fecha: " + movimiento.getFecha().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                + "\n"
                + "Operación: " + movimiento.getDescripcion() + "\n"
                + "Monto: Q"
                + Dinero.formatear(movimiento.getMonto()) + "\n"
                + "Saldo anterior: Q"
                + Dinero.formatear(movimiento.getSaldoAnterior()) + "\n"
                + "Saldo actual: Q"
                + Dinero.formatear(movimiento.getSaldoResultante()) + "\n"
                        + "Estado: Operación exitosa\n";
                
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

    public static String generarHtml(
            Usuario usuario,
            Movimiento movimiento,
            String numeroCuenta) {

        String fecha = movimiento.getFecha().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        String monto = "Q " + Dinero.formatear(movimiento.getMonto());
        String tipo = movimiento.getDescripcion();
        String contraparte = "";

        if (movimiento instanceof Transferencia) {
            Transferencia transferencia = (Transferencia) movimiento;

            tipo = transferencia.esEntrada()
                    ? "Transferencia recibida"
                    : "Transferencia enviada";

            contraparte = fila(
                    transferencia.esEntrada()
                            ? "Cuenta de origen"
                            : "Cuenta destino",
                    transferencia.getNumeroCuentaContraparte()
            );
        }

        return "<html>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<title>Comprobante Banco UVG</title>"

                + "<style>"
                + "body {"
                + "font-family: Arial, sans-serif;"
                + "font-size: 12pt;"
                + "color: #222222;"
                + "background-color: #ffffff;"
                + "margin: 24px;"
                + "}"
                + "td { padding: 7px; }"
                + "th { padding: 9px; }"
                + "</style>"

                + "</head>"
                + "<body>"

                + "<table width='100%' cellpadding='8' cellspacing='0'>"
                + "<tr>"
                + "<td>"
                + "<font color='#14644E' size='7'>"
                + "<b>BANCO UVG</b>"
                + "</font><br>"
                + "<font color='#555555'>"
                + "Proyecto académico de banca"
                + "</font>"
                + "</td>"
                + "<td align='right'>"
                + "<b>COMPROBANTE</b><br>"
                + "Operación registrada"
                + "</td>"
                + "</tr>"
                + "</table>"

                + "<hr><br>"

                + "<h2 align='center'>"
                + "COMPROBANTE DE OPERACIÓN"
                + "</h2>"

                + "<p align='center'>"
                + escapar(tipo)
                + "</p><br>"

                + "<table width='100%' cellpadding='6' cellspacing='0'>"
                + fila("Titular", usuario.getNombreCompleto())
                + fila("Usuario", usuario.getNombreUsuario())
                + fila("Número de cuenta", numeroCuenta)
                + contraparte
                + fila("Fecha y hora", fecha)

                + "<tr>"
                + "<td valign='top'><b>Referencia</b></td>"
                + "<td>"
                + referenciaHtml(movimiento.getReferencia())
                + "</td>"
                + "</tr>"
                + "</table><br>"

                + "<table width='100%' border='1' "
                + "cellpadding='9' cellspacing='0'>"

                + "<tr bgcolor='#14644E'>"
                + "<th align='left'>"
                + "<font color='white'>CONCEPTO</font>"
                + "</th>"
                + "<th align='right'>"
                + "<font color='white'>VALOR</font>"
                + "</th>"
                + "</tr>"

                + "<tr>"
                + "<td>" + escapar(tipo) + "</td>"
                + "<td align='right'>" + escapar(monto) + "</td>"
                + "</tr>"

                + "<tr bgcolor='#EFF5F2'>"
                + "<td><b>TOTAL DE LA OPERACIÓN</b></td>"
                + "<td align='right'><b>"
                + escapar(monto)
                + "</b></td>"
                + "</tr>"

                + "</table><br>"

                + "<table width='100%' cellpadding='7' cellspacing='0'>"
                + fila(
                        "Saldo anterior",
                        "Q " + Dinero.formatear(
                                movimiento.getSaldoAnterior()))
                + fila(
                        "Saldo después de la operación",
                        "Q " + Dinero.formatear(
                                movimiento.getSaldoResultante()))
                + "</table><br>"

                + "<table width='100%' border='1' "
                + "cellpadding='8' cellspacing='0'>"

                + "<tr bgcolor='#E6E6E6'>"
                + "<th colspan='2'>DATOS DE LA OPERACIÓN</th>"
                + "</tr>"

                + fila("Moneda", "Quetzales (GTQ)")
                + fila("Estado", "Operación exitosa")

                + "</table><br>"

                + "<p align='center'>"
                + "<font color='#14644E'>"
                + "<b>Gracias por utilizar Banco UVG</b>"
                + "</font>"
                + "</p>"

                + "<p align='center'>"
                + "<font size='3' color='#555555'>"
                + "</font>"
                + "</p>"

                + "</body>"
                + "</html>";
    }

    private static String fila(String etiqueta, String valor) {
        return "<tr>"
                + "<td valign='top'><b>"
                + escapar(etiqueta)
                + "</b></td>"
                + "<td>"
                + escapar(valor)
                + "</td>"
                + "</tr>";
    }

    private static String referenciaHtml(String referencia) {
        StringBuilder texto = new StringBuilder();

        for (int i = 0; i < referencia.length(); i += 24) {
            if (i > 0) {
                texto.append("<br>");
            }

            texto.append(escapar(referencia.substring(
                    i,
                    Math.min(i + 24, referencia.length())
            )));
        }

        return texto.toString();
    }

    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }

        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}