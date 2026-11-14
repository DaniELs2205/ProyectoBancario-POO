import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class Main extends JFrame {
    private static final Color VERDE = new Color(20, 100, 78);
    private static final Color FONDO = new Color(242, 246, 249);
    private final Banco banco = new Banco();
    private final JTabbedPane paginas = new JTabbedPane();
    private static final int TAB_CUENTAS = 0;
    private static final int TAB_INGRESAR = 1;
    private static final int TAB_CREAR = 2;
    private static final int TAB_OPERACIONES = 3;
    private static final int TAB_HISTORIAL = 4;
    private final java.awt.CardLayout tarjetasHistorial = new java.awt.CardLayout();
    private final JPanel panelHistorial = new JPanel(tarjetasHistorial);
    private final JTextField codigoHistorial = new JTextField(22);
    private boolean historialDesbloqueado;
    private ConsultaHistorial consultaHistorial;
    private final JLabel titularHistorial = new JLabel();
    private final JLabel saldoHistorial = new JLabel();
    private final JTextField codigoIngreso = new JTextField(22);
    private final JButton cerrarSesion = new JButton("Cerrar sesión");
    private final JLabel detalleCuenta = new JLabel();
    private final JLabel saldo = new JLabel();
    private final JLabel depositado = new JLabel();
    private final JLabel retirado = new JLabel();
    private final JLabel operaciones = new JLabel();
    private final JLabel titular = new JLabel();
    private final JLabel estado = new JLabel(" ");
    private final JComboBox<String> filtro = new JComboBox<>(new String[]{"Todos", "Depósitos", "Retiros"});
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"N.º", "Fecha y hora", "Operación", "Monto (Q)", "Saldo resultante (Q)"}, 0) {
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final DefaultTableModel modeloUsuarios = new DefaultTableModel(new String[]{"Usuario", "Código"}, 0) {
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tablaUsuarios = new JTable(modeloUsuarios);
    private CuentaBancaria cuenta;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo tema : UIManager.getInstalledLookAndFeels()) {
                    if (tema.getName().equals("Nimbus")) {
                        UIManager.setLookAndFeel(tema.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                // El tema predeterminado de Java permite iniciar igualmente.
            }
            UIManager.put("control", FONDO);
            new Main().setVisible(true);
        });
    }

    public Main() {
        super("Banco UVG | Usuarios, cuentas y comprobantes");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { salir(); }
        });
        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(new EmptyBorder(20, 24, 16, 24));
        contenido.setBackground(FONDO);
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        JLabel nombre = new JLabel("BANCO UVG");
        nombre.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        nombre.setForeground(VERDE);
        cabecera.add(nombre, BorderLayout.NORTH);
        JPanel accesoSesion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        accesoSesion.setOpaque(false);
        cerrarSesion.setFont(cerrarSesion.getFont().deriveFont(Font.BOLD));
        cerrarSesion.setPreferredSize(new Dimension(145, 36));
        cerrarSesion.setToolTipText("Cerrar la cuenta o consulta actual y volver a la lista de cuentas");
        cerrarSesion.addActionListener(e -> cerrarSesion());
        accesoSesion.add(cerrarSesion);
        cabecera.add(accesoSesion, BorderLayout.SOUTH);
        contenido.add(cabecera, BorderLayout.NORTH);
        paginas.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        paginas.addTab("Cuentas", crearListaCuentas());
        paginas.addTab("Ingresar", crearIngreso());
        paginas.addTab("Crear usuario", crearRegistro());
        paginas.addTab("Operaciones", crearCuenta());
        paginas.addTab("Movimientos e historial", panelHistorial);
        habilitarCuenta(false);
        paginas.addChangeListener(e -> {
            bloquearHistorial();
            if (paginas.getSelectedIndex() == TAB_CUENTAS) { actualizarUsuarios(); }
            if (paginas.getSelectedIndex() == TAB_HISTORIAL) { codigoHistorial.requestFocusInWindow(); }
            if (paginas.getSelectedIndex() == TAB_INGRESAR) { codigoIngreso.requestFocusInWindow(); }
        });
        contenido.add(paginas, BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        JPanel salida = new JPanel(new FlowLayout(FlowLayout.RIGHT)); salida.setOpaque(false);
        salida.add(boton("Salir", e -> salir()));
        pie.add(salida, BorderLayout.EAST);

        contenido.add(pie, BorderLayout.SOUTH);
        setContentPane(contenido);
        setMinimumSize(new Dimension(950, 680));
        setSize(1100, 740);
        setLocationRelativeTo(null);
    }

    private void habilitarCuenta(boolean habilitado) {
        for (int i = TAB_OPERACIONES; i < paginas.getTabCount(); i++) {
            paginas.setEnabledAt(i, i == TAB_HISTORIAL || habilitado);
        }
        actualizarEstadoSesion();
    }

    private void actualizarEstadoSesion() {
        boolean consultaAbierta = consultaHistorial != null && historialDesbloqueado;
        cerrarSesion.setEnabled(cuenta != null || consultaAbierta);
    }

    private JPanel crearListaCuentas() {
        JPanel panel = new JPanel(new BorderLayout(0, 16)); panel.setOpaque(false);
        JPanel cabecera = new JPanel(new BorderLayout(0, 8)); cabecera.setOpaque(false);
        JLabel titulo = new JLabel("Cuentas registradas");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22)); titulo.setForeground(VERDE);
        cabecera.add(titulo, BorderLayout.NORTH);
        cabecera.add(new JLabel("Consulta los usuarios y códigos. Para abrir una cuenta, utiliza la pestaña Ingresar."), BorderLayout.SOUTH);
        panel.add(cabecera, BorderLayout.NORTH);
        tablaUsuarios.setRowHeight(32); tablaUsuarios.setFillsViewportHeight(true);
        tablaUsuarios.setAutoCreateRowSorter(true);
        tablaUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaUsuarios.getTableHeader().setReorderingAllowed(false);
        panel.add(new JScrollPane(tablaUsuarios), BorderLayout.CENTER);
        panel.add(new JLabel("Los usuarios permanecen en esta lista mientras el programa esté abierto."), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearIngreso() {
        JPanel marco = new JPanel(new java.awt.GridBagLayout()); marco.setOpaque(false);
        JPanel formulario = new JPanel(new BorderLayout(0, 18));
        formulario.setBackground(Color.WHITE); formulario.setBorder(new EmptyBorder(30, 34, 30, 34));
        JLabel titulo = new JLabel("Ingresa a tu cuenta");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24)); titulo.setForeground(VERDE);
        formulario.add(titulo, BorderLayout.NORTH);
        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 10)); campos.setOpaque(false);
        campos.add(new JLabel("Código de cuenta")); campos.add(codigoIngreso);
        formulario.add(campos, BorderLayout.CENTER);
        JButton entrar = boton("Ingresar", e -> {
            try {
                CuentaBancaria encontrada = banco.ingresar(codigoIngreso.getText());
                cuenta = encontrada;
                limpiarCamposOperacion();
                filtro.setSelectedIndex(0);
                actualizar(); habilitarCuenta(true);
                estado.setText("Bienvenido. Las operaciones se aplican a esta cuenta.");
                paginas.setSelectedIndex(TAB_OPERACIONES);
                codigoIngreso.setText("");
            } catch (IllegalArgumentException error) { mostrarError(error.getMessage()); }
        });
        codigoIngreso.addActionListener(e -> entrar.doClick());
        formulario.add(entrar, BorderLayout.SOUTH); marco.add(formulario);
        return marco;
    }

    private JPanel crearAccesoHistorial() {
        JPanel marco = new JPanel(new java.awt.GridBagLayout()); marco.setOpaque(false);
        JPanel formulario = new JPanel(new BorderLayout(0, 18));
        formulario.setBackground(Color.WHITE); formulario.setBorder(new EmptyBorder(28, 32, 28, 32));
        JLabel titulo = new JLabel("Consultar movimientos e historial");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        formulario.add(titulo, BorderLayout.NORTH);
        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 10)); campos.setOpaque(false);
        campos.add(new JLabel("Escribe el código de la cuenta para consultar su historial y saldo."));
        campos.add(new JLabel("Código de cuenta")); campos.add(codigoHistorial);
        formulario.add(campos, BorderLayout.CENTER);
        JButton consultar = boton("Consultar historial", e -> {
            try {
                consultaHistorial = banco.consultarHistorial(codigoHistorial.getText());
                historialDesbloqueado = true;
                Usuario usuarioConsultado = consultaHistorial.getUsuario();
                titularHistorial.setText("Titular: " + usuarioConsultado.getNombreCompleto()
                        + " | Usuario: " + usuarioConsultado.getNombreUsuario()
                        + " | Código: " + usuarioConsultado.getCodigo());
                saldoHistorial.setText("Saldo disponible: Q" + Dinero.formatear(consultaHistorial.getSaldoActual())
                        + "     |     Saldo inicial: Q" + Dinero.formatear(consultaHistorial.getSaldoInicial())
                        + "     |     Movimientos: " + consultaHistorial.getMovimientos().length);
                codigoHistorial.setText("");
                filtro.setSelectedIndex(0);
                actualizarTabla();
                tarjetasHistorial.show(panelHistorial, "datos");
                actualizarEstadoSesion();
            } catch (IllegalArgumentException error) {
                bloquearHistorial();
                mostrarError(error.getMessage());
                codigoHistorial.requestFocusInWindow();
            }
        });
        codigoHistorial.addActionListener(e -> consultar.doClick());
        formulario.add(consultar, BorderLayout.SOUTH); marco.add(formulario);
        return marco;
    }

    private void bloquearHistorial() {
        historialDesbloqueado = false;
        consultaHistorial = null;
        titularHistorial.setText(""); saldoHistorial.setText("");
        codigoHistorial.setText("");
        tabla.clearSelection();
        modelo.setRowCount(0);
        tabla.getAccessibleContext().setAccessibleDescription("");
        tarjetasHistorial.show(panelHistorial, "codigo");
        actualizarEstadoSesion();
    }

    private JPanel crearRegistro() {
        JPanel marco = new JPanel(new java.awt.GridBagLayout()); marco.setOpaque(false);
        JPanel formulario = new JPanel(new BorderLayout(0, 14));
        formulario.setBackground(Color.WHITE); formulario.setBorder(new EmptyBorder(20, 28, 20, 28));
        JLabel titulo = new JLabel("Crear usuario y agregar cuenta");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        formulario.add(titulo, BorderLayout.NORTH);
        JTextField nombre = new JTextField(28);
        JTextField usuario = new JTextField(28);
        JTextField codigo = new JTextField(28);
        JTextField inicial = new JTextField("0.00", 28);
        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 5)); campos.setOpaque(false);
        campos.add(new JLabel("Nombre completo del titular")); campos.add(nombre);
        campos.add(new JLabel("Nombre de usuario")); campos.add(usuario);
        campos.add(new JLabel("Código único para ingresar")); campos.add(codigo);
        campos.add(new JLabel("Saldo inicial (Q)")); campos.add(inicial);
        campos.add(new JLabel("Recuerda tu código. Los datos se conservan mientras este programa esté abierto."));
        formulario.add(campos, BorderLayout.CENTER);
        JButton crear = boton("Registrar usuario y crear cuenta", e -> {
            try {
                CuentaBancaria nueva = banco.registrar(usuario.getText().trim(), nombre.getText(),
                        codigo.getText(), leerDinero(inicial.getText()));
                String asignado = nueva.getUsuario().getCodigo();
                JOptionPane.showMessageDialog(this, "Usuario registrado correctamente.\nCódigo: " + asignado
                        + "\nSaldo inicial: Q" + Dinero.formatear(nueva.getSaldo())
                        + "\nAhora ingresa con ese código en la pestaña Ingresar.", "Cuenta creada", JOptionPane.INFORMATION_MESSAGE);
                nombre.setText(""); usuario.setText(""); codigo.setText(""); inicial.setText("0.00");
                cerrarSesion();
                paginas.setSelectedIndex(TAB_INGRESAR);
                codigoIngreso.setText(asignado); codigoIngreso.requestFocusInWindow();
            } catch (IllegalArgumentException error) { mostrarError(error.getMessage()); }
        });
        inicial.addActionListener(e -> crear.doClick());
        formulario.add(crear, BorderLayout.SOUTH); marco.add(formulario);
        return marco;
    }

    private final JTextField montoOperacion = new JTextField(18);

    private void limpiarCamposOperacion() {
        bloquearHistorial();
        montoOperacion.setText("");
        tabla.clearSelection();
    }
    private void cerrarSesion() {
        cuenta = null;
        limpiarCamposOperacion();
        modelo.setRowCount(0);
        actualizarUsuarios();
        // Eliminar también los textos de la cuenta anterior en las pestañas bloqueadas.
        saldo.setText(""); depositado.setText(""); retirado.setText(""); operaciones.setText("");
        titular.setText(""); detalleCuenta.setText(""); estado.setText(" ");
        tabla.getAccessibleContext().setAccessibleDescription("");
        codigoIngreso.setText("");
        paginas.setSelectedIndex(TAB_CUENTAS); habilitarCuenta(false);
        codigoIngreso.requestFocusInWindow();
    }

    private JPanel crearCuenta() {
        JPanel panel = new JPanel(new BorderLayout(0, 14)); panel.setOpaque(false);
        JPanel superior = new JPanel(new BorderLayout(0, 12)); superior.setOpaque(false);
        titular.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        superior.add(titular, BorderLayout.NORTH);
        JPanel resumen = new JPanel(new GridLayout(1, 4, 10, 0)); resumen.setOpaque(false);
        resumen.add(tarjeta("Saldo disponible", saldo));
        resumen.add(tarjeta("Total depositado", depositado));
        resumen.add(tarjeta("Total retirado", retirado));
        resumen.add(tarjeta("Operaciones", operaciones));
        superior.add(resumen, BorderLayout.CENTER);
        panel.add(superior, BorderLayout.NORTH);

        JPanel historial = new JPanel(new BorderLayout(0, 8)); historial.setOpaque(false);
        JPanel herramientas = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); herramientas.setOpaque(false);
        herramientas.add(new JLabel("Historial de movimientos"));
        herramientas.add(filtro);
        filtro.addActionListener(e -> { if (consultaHistorial != null) { actualizarTabla(); } });
        herramientas.add(boton("Buscar por número", e -> buscarMovimiento()));
        herramientas.add(boton("Generar comprobante", e -> comprobanteSeleccionado()));
        herramientas.add(boton("Consultar otro código", e -> { bloquearHistorial(); codigoHistorial.requestFocusInWindow(); }));
        JPanel cabeceraHistorial = new JPanel(new GridLayout(0, 1, 0, 8)); cabeceraHistorial.setOpaque(false);
        titularHistorial.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        saldoHistorial.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        saldoHistorial.setForeground(VERDE);
        cabeceraHistorial.add(titularHistorial);
        cabeceraHistorial.add(saldoHistorial);
        cabeceraHistorial.add(herramientas);
        historial.add(cabeceraHistorial, BorderLayout.NORTH);
        tabla.setRowHeight(30); tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true); tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer derecha = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object valor) {
                super.setValue(valor instanceof BigDecimal ? Dinero.formatear((BigDecimal) valor) : valor);
            }
        };
        derecha.setHorizontalAlignment(SwingConstants.RIGHT);
        tabla.getColumnModel().getColumn(3).setCellRenderer(derecha);
        tabla.getColumnModel().getColumn(4).setCellRenderer(derecha);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(45);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(180);
        historial.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JLabel ayuda = new JLabel("Consulta de solo lectura. Si no hay movimientos, se muestra el saldo de la cuenta. Selecciona una fila para ver su comprobante.");
        historial.add(ayuda, BorderLayout.SOUTH);
        panelHistorial.setOpaque(false);
        panelHistorial.add(crearAccesoHistorial(), "codigo");
        panelHistorial.add(historial, "datos");
        tarjetasHistorial.show(panelHistorial, "codigo");
        JPanel informacion = new JPanel(new BorderLayout(0, 18)); informacion.setOpaque(false);
        detalleCuenta.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        informacion.add(detalleCuenta, BorderLayout.NORTH);
        JPanel operacionesCuenta = new JPanel(new java.awt.GridBagLayout()); operacionesCuenta.setOpaque(false);
        JPanel formulario = new JPanel(new BorderLayout(0, 18));
        formulario.setBackground(Color.WHITE); formulario.setBorder(new EmptyBorder(28, 30, 28, 30));
        JLabel titulo = new JLabel("Operaciones de la cuenta");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        formulario.add(titulo, BorderLayout.NORTH);
        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 10)); campos.setOpaque(false);
        campos.add(new JLabel("Monto (Q) para depositar o retirar")); campos.add(montoOperacion);
        campos.add(new JLabel("Usa un monto positivo con hasta dos decimales. Ejemplo: 150.50"));
        formulario.add(campos, BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT)); botones.setOpaque(false);
        botones.add(boton("Depositar", e -> operar(true)));
        botones.add(boton("Retirar", e -> operar(false)));
        botones.add(boton("Retirar todo el saldo", e -> retirarTodo()));
        formulario.add(botones, BorderLayout.SOUTH); operacionesCuenta.add(formulario);
        informacion.add(operacionesCuenta, BorderLayout.CENTER);
        informacion.add(new JLabel("Consulta Movimientos e historial para ver las operaciones o generar sus comprobantes."), BorderLayout.SOUTH);
        panel.add(informacion, BorderLayout.CENTER);
        estado.setForeground(VERDE);
        panel.add(estado, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel tarjeta(String nombre, JLabel valor) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE); panel.setBorder(new EmptyBorder(12, 14, 12, 14));
        valor.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21)); valor.setForeground(VERDE);
        panel.add(new JLabel(nombre), BorderLayout.NORTH); panel.add(valor, BorderLayout.CENTER);
        return panel;
    }
    private JButton boton(String texto, java.awt.event.ActionListener accion) {
        JButton boton = new JButton(texto); boton.addActionListener(accion); return boton;
    }
    private BigDecimal leerDinero(String texto) {
        String valor = texto.trim();
        if (!valor.matches("[+-]?[0-9]+(\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException("Escribe un número con máximo dos decimales, por ejemplo 150.50.");
        }
        return new BigDecimal(valor);
    }
    private void operar(boolean esDeposito) {
        try {
            BigDecimal valor = leerDinero(montoOperacion.getText());
            Movimiento movimiento = esDeposito ? cuenta.depositar(valor) : cuenta.retirar(valor);
            montoOperacion.setText("");
            operacionExitosa(movimiento);
        } catch (IllegalArgumentException error) { mostrarError(error.getMessage()); }
    }
    private void retirarTodo() {
        if (cuenta.getSaldo().signum() == 0) { mostrarError("No tienes saldo disponible para retirar."); return; }
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Retirar Q" + Dinero.formatear(cuenta.getSaldo()) + " y dejar el saldo en cero?",
                "Confirmar retiro total", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opcion == JOptionPane.YES_OPTION) {
            try { operacionExitosa(cuenta.retirarTodo()); }
            catch (IllegalArgumentException e) { mostrarError(e.getMessage()); }
        } else { estado.setText("Retiro cancelado. Tu saldo no cambió."); }
    }
    private void operacionExitosa(Movimiento movimiento) {
        actualizar();
        estado.setText(movimiento.getDescripcion() + " registrado: Q" + Dinero.formatear(movimiento.getMonto())
                + " · " + movimiento.getReferencia());
        int opcion = JOptionPane.showConfirmDialog(this,
                "Operación exitosa. Saldo actual: Q" + Dinero.formatear(cuenta.getSaldo())
                        + "\n¿Quieres generar el comprobante?", "Operación registrada", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) { mostrarComprobante(movimiento); }
    }
    private void actualizar() {
        titular.setText(cuenta.getUsuario().getNombreCompleto() + "  |  Usuario: " + cuenta.getUsuario().getNombreUsuario());
        saldo.setText("Q" + Dinero.formatear(cuenta.getSaldo()));
        depositado.setText("Q" + Dinero.formatear(cuenta.totalDepositos()));
        retirado.setText("Q" + Dinero.formatear(cuenta.totalRetiros()));
        operaciones.setText(String.valueOf(cuenta.getMovimientos().length));
        detalleCuenta.setText("Código: " + cuenta.getUsuario().getCodigo()
                + "     |     Saldo inicial: Q" + Dinero.formatear(cuenta.getSaldoInicial()));
        actualizarUsuarios();
        actualizarTabla();
    }
    private void actualizarUsuarios() {
        modeloUsuarios.setRowCount(0);
        for (Usuario usuario : banco.getUsuarios()) {
            modeloUsuarios.addRow(new Object[]{usuario.getNombreUsuario(), usuario.getCodigo()});
        }
    }
    private void actualizarTabla() {
        modelo.setRowCount(0);
        if (consultaHistorial == null || !historialDesbloqueado) { return; }
        int tipo = filtro.getSelectedIndex();
        tabla.getAccessibleContext().setAccessibleDescription(formatearArreglo(consultaHistorial.getMovimientos()));
        for (Movimiento m : consultaHistorial.getMovimientos()) {
            if ((tipo == 1 && !(m instanceof Deposito)) || (tipo == 2 && !(m instanceof Retiro))) { continue; }
            modelo.addRow(new Object[]{m.getNumero(), m.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    m.getDescripcion(), m.getMonto(), m.getSaldoResultante()});
        }
    }
    private void buscarMovimiento() {
        String numero = JOptionPane.showInputDialog(this, "Número de movimiento:", "Buscar movimiento", JOptionPane.QUESTION_MESSAGE);
        if (numero == null) { return; }
        try {
            Movimiento movimiento = consultaHistorial.buscarMovimiento(Integer.parseInt(numero.trim()));
            filtro.setSelectedIndex(0);
            for (int i = 0; i < modelo.getRowCount(); i++) {
                if (((Integer) modelo.getValueAt(i, 0)) == movimiento.getNumero()) {
                    int fila = tabla.convertRowIndexToView(i);
                    tabla.setRowSelectionInterval(fila, fila);
                    tabla.scrollRectToVisible(tabla.getCellRect(fila, 0, true));
                    estado.setText("Movimiento encontrado: " + movimiento.getReferencia());
                    break;
                }
            }
        } catch (NumberFormatException e) { mostrarError("Escribe un número de movimiento válido."); }
        catch (IllegalArgumentException e) { mostrarError(e.getMessage()); }
    }
    private void comprobanteSeleccionado() {
        int seleccion = tabla.getSelectedRow();
        if (seleccion < 0) { mostrarError("Selecciona primero una operación en la tabla."); return; }
        int fila = tabla.convertRowIndexToModel(seleccion);
        mostrarComprobante(consultaHistorial.getUsuario(), consultaHistorial.buscarMovimiento((Integer) modelo.getValueAt(fila, 0)));
    }
    private void mostrarComprobante(Movimiento movimiento) {
        mostrarComprobante(cuenta.getUsuario(), movimiento);
    }
    private void mostrarComprobante(Usuario usuarioComprobante, Movimiento movimiento) {
        JDialog dialogo = new JDialog(this, "Comprobante " + movimiento.getReferencia(), true);
        JTextArea texto = new JTextArea(Comprobante.generar(usuarioComprobante, movimiento));
        texto.setEditable(false); texto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        texto.setBorder(new EmptyBorder(14, 16, 14, 16));
        dialogo.add(new JScrollPane(texto), BorderLayout.CENTER);
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controles.add(boton("Guardar como TXT", e -> guardarComprobante(usuarioComprobante, movimiento, dialogo)));
        controles.add(boton("Cerrar", e -> dialogo.dispose()));
        dialogo.add(controles, BorderLayout.SOUTH);
        dialogo.setSize(560, 430); dialogo.setLocationRelativeTo(this); dialogo.setVisible(true);
    }
    private void guardarComprobante(Usuario usuarioComprobante, Movimiento movimiento, JDialog padre) {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar comprobante");
        selector.setSelectedFile(new java.io.File(usuarioComprobante.getCodigo() + "-" + movimiento.getReferencia() + ".txt"));
        if (selector.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) { return; }
        Path archivo = selector.getSelectedFile().toPath();
        if (!archivo.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".txt")) {
            archivo = archivo.resolveSibling(archivo.getFileName() + ".txt");
        }
        if (Files.exists(archivo) && JOptionPane.showConfirmDialog(padre,
                "El archivo existe. ¿Deseas reemplazarlo?", "Confirmar guardado", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) { return; }
        try {
            Files.writeString(archivo, Comprobante.generar(usuarioComprobante, movimiento), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(padre, "Comprobante guardado en:\n" + archivo.toAbsolutePath());
        } catch (IOException | SecurityException e) {
            JOptionPane.showMessageDialog(padre, "No se pudo guardar: " + e.getMessage()
                    + "\nLa operación ya fue registrada; no la repitas.", "Error al guardar", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Revisar datos", JOptionPane.WARNING_MESSAGE);
    }
    private void salir() {
        if (banco.getCantidadUsuarios() == 0 || JOptionPane.showConfirmDialog(this,
                "Los usuarios, saldos e historiales no se conservan al cerrar el programa.\nLos comprobantes guardados sí permanecen. ¿Salir?",
                "Cerrar Banco UVG", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { dispose(); }
    }
    // Se conserva el método genérico como evidencia de los temas del curso.
    public static <T> String formatearArreglo(T[] datos) {
        if (datos.length == 0) { return "No hay movimientos para mostrar.\n"; }
        StringBuilder texto = new StringBuilder();
        for (T dato : datos) { texto.append(dato).append(System.lineSeparator()); }
        return texto.toString();
    }
}