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

    private static final int TAB_CUENTAS = 0;
    private static final int TAB_INGRESAR = 1;
    private static final int TAB_OPERACIONES = 3;
    private static final int TAB_HISTORIAL = 4;

    private final Banco banco = new Banco();
    private final JTabbedPane paginas = new JTabbedPane();

    private final java.awt.CardLayout tarjetasHistorial =
            new java.awt.CardLayout();

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

    private final JTextField montoOperacion = campoNumerico();
    private final JTextField numeroDestino = campoNumerico();

    private final JComboBox<String> filtro = new JComboBox<>(
            new String[]{
                "Todos",
                "Depósitos",
                "Retiros",
                "Transferencias"
            }
    );

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{
                "N.º",
                "Fecha y hora",
                "Operación",
                "Monto (Q)",
                "Saldo resultante (Q)"
            }, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    private final JTable tabla = new JTable(modelo);

    private final DefaultTableModel modeloUsuarios = new DefaultTableModel(
            new String[]{
                "Usuario",
                "Código de acceso",
                "Número de cuenta"
            }, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    private final JTable tablaUsuarios = new JTable(modeloUsuarios);

    private CuentaBancaria cuenta;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo tema
                        : UIManager.getInstalledLookAndFeels()) {

                    if (tema.getName().equals("Nimbus")) {
                        UIManager.setLookAndFeel(tema.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                // Se conserva el tema predeterminado.
            }

            Font fuente = new Font(Font.SANS_SERIF, Font.PLAIN, 14);

            String[] claves = {
                "Label.font",
                "Button.font",
                "TextField.font",
                "TabbedPane.font",
                "ComboBox.font",
                "Table.font",
                "TableHeader.font"
            };

            for (String clave : claves) {
                UIManager.put(clave, fuente);
            }

            UIManager.put("control", FONDO);

            new Main().setVisible(true);
        });
    }

    public Main() {
        super("Banco UVG | Usuarios, cuentas y comprobantes");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                salir();
            }
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

        JPanel accesoSesion = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 0)
        );
        accesoSesion.setOpaque(false);

        cerrarSesion.setFont(
                cerrarSesion.getFont().deriveFont(Font.BOLD)
        );
        cerrarSesion.setPreferredSize(new Dimension(145, 36));
        cerrarSesion.setToolTipText(
                "Cerrar la cuenta o consulta actual"
        );
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

            if (paginas.getSelectedIndex() == TAB_CUENTAS) {
                actualizarUsuarios();
            }

            if (paginas.getSelectedIndex() == TAB_HISTORIAL) {
                codigoHistorial.requestFocusInWindow();
            }

            if (paginas.getSelectedIndex() == TAB_INGRESAR) {
                codigoIngreso.requestFocusInWindow();
            }
        });

        contenido.add(paginas, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);

        JPanel salida = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );
        salida.setOpaque(false);
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
            paginas.setEnabledAt(
                    i,
                    i == TAB_HISTORIAL || habilitado
            );
        }

        actualizarEstadoSesion();
    }

    private void actualizarEstadoSesion() {
        boolean consultaAbierta =
                consultaHistorial != null && historialDesbloqueado;

        cerrarSesion.setEnabled(cuenta != null || consultaAbierta);
    }

    private JPanel crearListaCuentas() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);

        JPanel cabecera = new JPanel(new BorderLayout(0, 8));
        cabecera.setOpaque(false);

        JLabel titulo = new JLabel("Cuentas registradas");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        titulo.setForeground(VERDE);

        cabecera.add(titulo, BorderLayout.NORTH);

        cabecera.add(
                new JLabel(
                        "Ingresa con tu código de acceso. "
                        + "Para transferir, usa el número de cuenta destino."
                ),
                BorderLayout.SOUTH
        );

        panel.add(cabecera, BorderLayout.NORTH);

        tablaUsuarios.setRowHeight(36);

        tablaUsuarios.getColumnModel().getColumn(0)
                .setPreferredWidth(280);

        tablaUsuarios.getColumnModel().getColumn(1)
                .setPreferredWidth(220);

        tablaUsuarios.getColumnModel().getColumn(2)
                .setPreferredWidth(280);

        tablaUsuarios.setFillsViewportHeight(true);
        tablaUsuarios.setAutoCreateRowSorter(true);
        tablaUsuarios.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        tablaUsuarios.getTableHeader().setReorderingAllowed(false);

        panel.add(
                new JScrollPane(tablaUsuarios),
                BorderLayout.CENTER
        );

        panel.add(
                new JLabel(
                        "Los usuarios permanecen en esta lista "
                        + "mientras el programa esté abierto."
                ),
                BorderLayout.SOUTH
        );

        return panel;
    }

    private JPanel crearIngreso() {
        JPanel marco = new JPanel(new java.awt.GridBagLayout());
        marco.setOpaque(false);

        JPanel formulario = new JPanel(new BorderLayout(0, 18));
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(new EmptyBorder(30, 34, 30, 34));

        JLabel titulo = new JLabel("Ingresa a tu cuenta");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        titulo.setForeground(VERDE);

        formulario.add(titulo, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 10));
        campos.setOpaque(false);

        campos.add(new JLabel("Código de acceso"));
        campos.add(codigoIngreso);

        formulario.add(campos, BorderLayout.CENTER);

        JButton entrar = boton("Ingresar", e -> {
            try {
                CuentaBancaria encontrada =
                        banco.ingresar(codigoIngreso.getText());

                cuenta = encontrada;

                limpiarCamposOperacion();
                filtro.setSelectedIndex(0);
                actualizar();
                habilitarCuenta(true);

                estado.setText(
                        "Bienvenido. Las operaciones se aplican a esta cuenta."
                );

                paginas.setSelectedIndex(TAB_OPERACIONES);
                codigoIngreso.setText("");

            } catch (IllegalArgumentException error) {
                mostrarError(error.getMessage());
            }
        });

        codigoIngreso.addActionListener(e -> entrar.doClick());

        formulario.add(entrar, BorderLayout.SOUTH);
        marco.add(formulario);

        return conDesplazamiento(marco);
    }

    private JPanel crearAccesoHistorial() {
        JPanel marco = new JPanel(new java.awt.GridBagLayout());
        marco.setOpaque(false);

        JPanel formulario = new JPanel(new BorderLayout(0, 18));
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(new EmptyBorder(28, 32, 28, 32));

        JLabel titulo = new JLabel("Consultar movimientos e historial");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));

        formulario.add(titulo, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 10));
        campos.setOpaque(false);

        campos.add(
                new JLabel(
                        "Escribe el código de acceso para consultar "
                        + "el historial y saldo."
                )
        );

        campos.add(new JLabel("Código de acceso"));
        campos.add(codigoHistorial);

        formulario.add(campos, BorderLayout.CENTER);

        JButton consultar = boton("Consultar historial", e -> {
            try {
                consultaHistorial =
                        banco.consultarHistorial(codigoHistorial.getText());

                historialDesbloqueado = true;

                Usuario usuarioConsultado =
                        consultaHistorial.getUsuario();

                String numeroCuenta = banco.ingresar(
                        usuarioConsultado.getCodigo()
                ).getNumeroCuenta();

                titularHistorial.setText(
                        "Titular: " + usuarioConsultado.getNombreCompleto()
                        + " | Usuario: "
                        + usuarioConsultado.getNombreUsuario()
                        + " | Cuenta: " + numeroCuenta
                );

                saldoHistorial.setText(
                        "Saldo disponible: Q"
                        + Dinero.formatear(
                                consultaHistorial.getSaldoActual()
                        )
                        + "     |     Saldo inicial: Q"
                        + Dinero.formatear(
                                consultaHistorial.getSaldoInicial()
                        )
                        + "     |     Movimientos: "
                        + consultaHistorial.getMovimientos().length
                );

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

        formulario.add(consultar, BorderLayout.SOUTH);
        marco.add(formulario);

        return conDesplazamiento(marco);
    }

    private void bloquearHistorial() {
        historialDesbloqueado = false;
        consultaHistorial = null;

        titularHistorial.setText("");
        saldoHistorial.setText("");
        codigoHistorial.setText("");

        tabla.clearSelection();
        modelo.setRowCount(0);
        tabla.getAccessibleContext().setAccessibleDescription("");

        tarjetasHistorial.show(panelHistorial, "codigo");
        actualizarEstadoSesion();
    }

    private JPanel crearRegistro() {
        JPanel marco = new JPanel(new java.awt.GridBagLayout());
        marco.setOpaque(false);

        JPanel formulario = new JPanel(new BorderLayout(0, 14));
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(new EmptyBorder(20, 28, 20, 28));

        JLabel titulo = new JLabel("Crear usuario y agregar cuenta");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));

        formulario.add(titulo, BorderLayout.NORTH);

        JTextField nombre = new JTextField(28);
        JTextField usuario = new JTextField(28);
        JTextField codigo = new JTextField(28);

        JTextField inicial = campoNumerico();
        inicial.setText("0.00");

        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 5));
        campos.setOpaque(false);

        campos.add(new JLabel("Nombre completo del titular"));
        campos.add(nombre);

        campos.add(new JLabel("Nombre de usuario"));
        campos.add(usuario);

        campos.add(new JLabel("Código de acceso (lo eliges tú)"));
        campos.add(codigo);

        campos.add(new JLabel("Saldo inicial (Q)"));
        campos.add(inicial);

        campos.add(
                new JLabel(
                        "El número de cuenta se genera automáticamente "
                        + "al registrar."
                )
        );

        campos.add(
                new JLabel(
                        "Los datos se conservan mientras este programa "
                        + "esté abierto."
                )
        );

        formulario.add(campos, BorderLayout.CENTER);

        JButton crear = boton("Registrar usuario y crear cuenta", e -> {
            try {
                CuentaBancaria nueva = banco.registrar(
                        usuario.getText().trim(),
                        nombre.getText(),
                        codigo.getText(),
                        leerDinero(inicial.getText())
                );

                String asignado = nueva.getUsuario().getCodigo();

                JOptionPane.showMessageDialog(
                        this,
                        "Usuario registrado correctamente."
                        + "\nCódigo de acceso: " + asignado
                        + "\nNúmero de cuenta: " + nueva.getNumeroCuenta()
                        + "\nSaldo inicial: Q"
                        + Dinero.formatear(nueva.getSaldo())
                        + "\nAhora ingresa con tu código de acceso.",
                        "Cuenta creada",
                        JOptionPane.INFORMATION_MESSAGE
                );

                nombre.setText("");
                usuario.setText("");
                codigo.setText("");
                inicial.setText("0.00");

                cerrarSesion();
                paginas.setSelectedIndex(TAB_INGRESAR);

                codigoIngreso.setText(asignado);
                codigoIngreso.requestFocusInWindow();

            } catch (IllegalArgumentException error) {
                mostrarError(error.getMessage());
            }
        });

        inicial.addActionListener(e -> crear.doClick());

        formulario.add(crear, BorderLayout.SOUTH);
        marco.add(formulario);

        return conDesplazamiento(marco);
    }

    private void limpiarCamposOperacion() {
        bloquearHistorial();
        montoOperacion.setText("");
        numeroDestino.setText("");
        tabla.clearSelection();
    }

    private void cerrarSesion() {
        cuenta = null;

        limpiarCamposOperacion();
        modelo.setRowCount(0);
        actualizarUsuarios();

        saldo.setText("");
        depositado.setText("");
        retirado.setText("");
        operaciones.setText("");
        titular.setText("");
        detalleCuenta.setText("");
        estado.setText(" ");

        tabla.getAccessibleContext().setAccessibleDescription("");
        codigoIngreso.setText("");

        paginas.setSelectedIndex(TAB_CUENTAS);
        habilitarCuenta(false);
        codigoIngreso.requestFocusInWindow();
    }

    private JPanel crearCuenta() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        JPanel superior = new JPanel(new BorderLayout(0, 12));
        superior.setOpaque(false);

        titular.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        superior.add(titular, BorderLayout.NORTH);

        JPanel resumen = new JPanel(new GridLayout(1, 4, 10, 0));
        resumen.setOpaque(false);

        resumen.add(tarjeta("Saldo disponible", saldo));
        resumen.add(tarjeta("Total depositado", depositado));
        resumen.add(tarjeta("Total retirado", retirado));
        resumen.add(tarjeta("Operaciones", operaciones));

        superior.add(resumen, BorderLayout.CENTER);
        panel.add(superior, BorderLayout.NORTH);

        JPanel historial = new JPanel(new BorderLayout(0, 8));
        historial.setOpaque(false);

        JPanel herramientas = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 0)
        );
        herramientas.setOpaque(false);

        herramientas.add(new JLabel("Historial de movimientos"));
        herramientas.add(filtro);

        filtro.addActionListener(e -> {
            if (consultaHistorial != null) {
                actualizarTabla();
            }
        });

        herramientas.add(
                boton("Buscar por número", e -> buscarMovimiento())
        );

        herramientas.add(
                boton(
                        "Generar comprobante",
                        e -> comprobanteSeleccionado()
                )
        );

        herramientas.add(
                boton("Consultar otro código", e -> {
                    bloquearHistorial();
                    codigoHistorial.requestFocusInWindow();
                })
        );

        JPanel cabeceraHistorial = new JPanel(
                new GridLayout(0, 1, 0, 8)
        );
        cabeceraHistorial.setOpaque(false);

        titularHistorial.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 15)
        );

        saldoHistorial.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 17)
        );
        saldoHistorial.setForeground(VERDE);

        cabeceraHistorial.add(titularHistorial);
        cabeceraHistorial.add(saldoHistorial);
        cabeceraHistorial.add(herramientas);

        historial.add(cabeceraHistorial, BorderLayout.NORTH);

        tabla.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.setRowHeight(36);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);
        tabla.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer derecha =
                new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object valor) {
                super.setValue(
                        valor instanceof BigDecimal
                                ? Dinero.formatear((BigDecimal) valor)
                                : valor
                );
            }
        };

        derecha.setHorizontalAlignment(SwingConstants.RIGHT);

        tabla.getColumnModel().getColumn(3)
                .setCellRenderer(derecha);

        tabla.getColumnModel().getColumn(4)
                .setCellRenderer(derecha);

        tabla.getColumnModel().getColumn(0).setPreferredWidth(45);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(180);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(340);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(190);

        historial.add(
                new JScrollPane(tabla),
                BorderLayout.CENTER
        );

        historial.add(
                new JLabel(
                        "Selecciona una operación para generar su comprobante."
                ),
                BorderLayout.SOUTH
        );

        panelHistorial.setOpaque(false);
        panelHistorial.add(crearAccesoHistorial(), "codigo");
        panelHistorial.add(historial, "datos");

        tarjetasHistorial.show(panelHistorial, "codigo");

        JPanel informacion = new JPanel(new BorderLayout(0, 18));
        informacion.setOpaque(false);

        detalleCuenta.setFont(
                new Font(Font.SANS_SERIF, Font.PLAIN, 16)
        );

        informacion.add(detalleCuenta, BorderLayout.NORTH);

        JPanel operacionesCuenta = new JPanel(
                new java.awt.GridBagLayout()
        );
        operacionesCuenta.setOpaque(false);

        JPanel formulario = new JPanel(new BorderLayout(0, 18));
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel titulo = new JLabel("Operaciones de la cuenta");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));

        formulario.add(titulo, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 6));
        campos.setOpaque(false);

        campos.add(
                new JLabel(
                        "Monto (Q) para depositar, retirar o transferir"
                )
        );

        campos.add(montoOperacion);

        campos.add(
                new JLabel(
                        "Usa hasta dos decimales. Ejemplo: 150.50"
                )
        );

        campos.add(
                new JLabel(
                        "Número de cuenta destino (solo para transferir)"
                )
        );

        campos.add(numeroDestino);

        formulario.add(campos, BorderLayout.CENTER);

        JPanel botones = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );
        botones.setOpaque(false);

        botones.add(boton("Depositar", e -> operar(true)));
        botones.add(boton("Retirar", e -> operar(false)));
        botones.add(boton("Transferir", e -> transferir()));

        botones.add(
                boton("Retirar todo el saldo", e -> retirarTodo())
        );

        formulario.add(botones, BorderLayout.SOUTH);
        operacionesCuenta.add(formulario);

        informacion.add(operacionesCuenta, BorderLayout.CENTER);

        informacion.add(
                new JLabel(
                        "Consulta Movimientos e historial "
                        + "para ver tus operaciones."
                ),
                BorderLayout.SOUTH
        );

        JScrollPane desplazamiento = new JScrollPane(informacion);
        desplazamiento.setBorder(null);
        desplazamiento.getVerticalScrollBar().setUnitIncrement(18);

        panel.add(desplazamiento, BorderLayout.CENTER);

        estado.setForeground(VERDE);
        panel.add(estado, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel conDesplazamiento(JPanel contenido) {
        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private static JTextField campoNumerico() {
        JTextField campo = new JTextField(22);

        campo.setFont(
                new Font(Font.MONOSPACED, Font.PLAIN, 18)
        );

        campo.setMargin(
                new java.awt.Insets(6, 10, 6, 10)
        );

        campo.setPreferredSize(new Dimension(360, 38));
        campo.setMinimumSize(new Dimension(160, 38));

        return campo;
    }

    private JPanel tarjeta(String nombre, JLabel valor) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));

        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        valor.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 21)
        );
        valor.setForeground(VERDE);

        panel.add(new JLabel(nombre), BorderLayout.NORTH);
        panel.add(valor, BorderLayout.CENTER);

        return panel;
    }

    private JButton boton(
            String texto,
            java.awt.event.ActionListener accion) {

        JButton boton = new JButton(texto);
        boton.addActionListener(accion);

        return boton;
    }

    private BigDecimal leerDinero(String texto) {
        String valor = texto.trim();

        if (!valor.matches("[+-]?[0-9]+(\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException(
                    "Escribe un número con máximo dos decimales, "
                    + "por ejemplo 150.50."
            );
        }

        return new BigDecimal(valor);
    }

    private void operar(boolean esDeposito) {
        try {
            BigDecimal valor = leerDinero(montoOperacion.getText());

            Movimiento movimiento = esDeposito
                    ? cuenta.depositar(valor)
                    : cuenta.retirar(valor);

            montoOperacion.setText("");
            operacionExitosa(movimiento);

        } catch (IllegalArgumentException error) {
            mostrarError(error.getMessage());
        }
    }

    private void transferir() {
        if (cuenta == null) {
            mostrarError("Primero ingresa a una cuenta.");
            return;
        }

        try {
            BigDecimal monto = leerDinero(montoOperacion.getText());

            CuentaBancaria destino = banco.buscarPorNumeroCuenta(
                    numeroDestino.getText().trim()
            );

            if (destino == cuenta) {
                throw new IllegalArgumentException(
                        "Selecciona una cuenta diferente a la tuya."
                );
            }

            if (monto.signum() <= 0) {
                throw new IllegalArgumentException(
                        "El monto debe ser mayor que cero."
                );
            }

            if (monto.compareTo(cuenta.getSaldo()) > 0) {
                throw new IllegalArgumentException(
                        "Fondos insuficientes."
                );
            }

            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    "¿Transferir Q" + Dinero.formatear(monto)
                    + "\nA: " + destino.getUsuario().getNombreCompleto()
                    + "\nUsuario: "
                    + destino.getUsuario().getNombreUsuario()
                    + "\nNúmero de cuenta: "
                    + destino.getNumeroCuenta() + "?",
                    "Confirmar transferencia",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (opcion != JOptionPane.YES_OPTION) {
                estado.setText(
                        "Transferencia cancelada. Los saldos no cambiaron."
                );
                return;
            }

            Movimiento movimiento = cuenta.transferirA(destino, monto);

            montoOperacion.setText("");
            numeroDestino.setText("");

            bloquearHistorial();
            operacionExitosa(movimiento);

        } catch (IllegalArgumentException error) {
            mostrarError(error.getMessage());
        }
    }

    private void retirarTodo() {
        if (cuenta.getSaldo().signum() == 0) {
            mostrarError("No tienes saldo disponible para retirar.");
            return;
        }

        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Retirar Q" + Dinero.formatear(cuenta.getSaldo())
                + " y dejar el saldo en cero?",
                "Confirmar retiro total",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (opcion == JOptionPane.YES_OPTION) {
            try {
                operacionExitosa(cuenta.retirarTodo());

            } catch (IllegalArgumentException e) {
                mostrarError(e.getMessage());
            }
        } else {
            estado.setText("Retiro cancelado. Tu saldo no cambió.");
        }
    }

    private void operacionExitosa(Movimiento movimiento) {
        actualizar();

        String tipo = movimiento instanceof Transferencia
                ? "Transferencia"
                : movimiento.getDescripcion();

        estado.setText(
                tipo + " registrada por Q"
                + Dinero.formatear(movimiento.getMonto())
                + ". Consulta el historial para ver el detalle."
        );

        int opcion = JOptionPane.showConfirmDialog(
                this,
                "Operación exitosa. Saldo actual: Q"
                + Dinero.formatear(cuenta.getSaldo())
                + "\n¿Quieres generar el comprobante?",
                "Operación registrada",
                JOptionPane.YES_NO_OPTION
        );

        if (opcion == JOptionPane.YES_OPTION) {
            mostrarComprobante(movimiento);
        }
    }

    private void actualizar() {
        titular.setText(
                cuenta.getUsuario().getNombreCompleto()
                + "  |  Usuario: "
                + cuenta.getUsuario().getNombreUsuario()
        );

        saldo.setText(
                "Q" + Dinero.formatear(cuenta.getSaldo())
        );

        depositado.setText(
                "Q" + Dinero.formatear(cuenta.totalDepositos())
        );

        retirado.setText(
                "Q" + Dinero.formatear(cuenta.totalRetiros())
        );

        operaciones.setText(
                String.valueOf(cuenta.getMovimientos().length)
        );

        detalleCuenta.setText(
                "Número de cuenta: " + cuenta.getNumeroCuenta()
                + "     |     Saldo inicial: Q"
                + Dinero.formatear(cuenta.getSaldoInicial())
        );

        actualizarUsuarios();
        actualizarTabla();
    }

    private void actualizarUsuarios() {
        modeloUsuarios.setRowCount(0);

        for (CuentaBancaria registrada : banco.getCuentas()) {
            Usuario usuario = registrada.getUsuario();

            modeloUsuarios.addRow(new Object[]{
                usuario.getNombreUsuario(),
                usuario.getCodigo(),
                registrada.getNumeroCuenta()
            });
        }
    }

    private void actualizarTabla() {
        modelo.setRowCount(0);

        if (consultaHistorial == null || !historialDesbloqueado) {
            return;
        }

        int tipo = filtro.getSelectedIndex();

        tabla.getAccessibleContext().setAccessibleDescription(
                formatearArreglo(consultaHistorial.getMovimientos())
        );

        for (Movimiento m : consultaHistorial.getMovimientos()) {
            if ((tipo == 1 && !(m instanceof Deposito))
                    || (tipo == 2 && !(m instanceof Retiro))
                    || (tipo == 3 && !(m instanceof Transferencia))) {
                continue;
            }

            modelo.addRow(new Object[]{
                m.getNumero(),
                m.getFecha().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                ),
                m.getDescripcion(),
                m.getMonto(),
                m.getSaldoResultante()
            });
        }
    }

    private void buscarMovimiento() {
        String numero = JOptionPane.showInputDialog(
                this,
                "Número de movimiento:",
                "Buscar movimiento",
                JOptionPane.QUESTION_MESSAGE
        );

        if (numero == null) {
            return;
        }

        try {
            Movimiento movimiento = consultaHistorial.buscarMovimiento(
                    Integer.parseInt(numero.trim())
            );

            filtro.setSelectedIndex(0);

            for (int i = 0; i < modelo.getRowCount(); i++) {
                if (((Integer) modelo.getValueAt(i, 0))
                        == movimiento.getNumero()) {

                    int fila = tabla.convertRowIndexToView(i);

                    tabla.setRowSelectionInterval(fila, fila);

                    tabla.scrollRectToVisible(
                            tabla.getCellRect(fila, 0, true)
                    );

                    estado.setText(
                            "Movimiento encontrado: "
                            + movimiento.getReferencia()
                    );

                    break;
                }
            }

        } catch (NumberFormatException e) {
            mostrarError("Escribe un número de movimiento válido.");

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    private void comprobanteSeleccionado() {
        int seleccion = tabla.getSelectedRow();

        if (seleccion < 0) {
            mostrarError("Selecciona primero una operación en la tabla.");
            return;
        }

        int fila = tabla.convertRowIndexToModel(seleccion);

        mostrarComprobante(
                consultaHistorial.getUsuario(),
                consultaHistorial.buscarMovimiento(
                        (Integer) modelo.getValueAt(fila, 0)
                )
        );
    }

    private void mostrarComprobante(Movimiento movimiento) {
        mostrarComprobante(cuenta.getUsuario(), movimiento);
    }

    private void mostrarComprobante(
            Usuario usuarioComprobante,
            Movimiento movimiento) {

        String numeroCuenta = banco.ingresar(
                usuarioComprobante.getCodigo()
        ).getNumeroCuenta();

        String html = Comprobante.generarHtml(
                usuarioComprobante,
                movimiento,
                numeroCuenta
        );

        JDialog dialogo = new JDialog(
                this,
                "Comprobante de operación",
                true
        );

        JEditorPane vista = new JEditorPane("text/html", html);
        vista.setEditable(false);
        vista.setBackground(Color.WHITE);
        vista.setCaretPosition(0);

        dialogo.add(
                new JScrollPane(vista),
                BorderLayout.CENTER
        );

        JPanel controles = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        controles.add(
                boton(
                        "Guardar recibo",
                        e -> guardarReciboHtml(html, dialogo)
                )
        );

        controles.add(
                boton("Imprimir / PDF", e -> {
                    try {
                        vista.print();

                    } catch (java.awt.print.PrinterException
                            | SecurityException error) {

                        JOptionPane.showMessageDialog(
                                dialogo,
                                "No se pudo imprimir: "
                                + error.getMessage()
                                + "\nLa operación ya fue registrada; "
                                + "no la repitas.",
                                "Imprimir comprobante",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                })
        );

        controles.add(
                boton(
                        "Guardar TXT",
                        e -> guardarComprobante(
                                usuarioComprobante,
                                movimiento,
                                dialogo
                        )
                )
        );

        controles.add(
                boton("Cerrar", e -> dialogo.dispose())
        );

        dialogo.add(controles, BorderLayout.SOUTH);
        dialogo.setSize(850, 680);
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }

    private void guardarReciboHtml(String html, JDialog padre) {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar recibo con diseño");

        selector.setSelectedFile(
                new java.io.File(
                        "comprobante-"
                        + java.util.UUID.randomUUID()
                                .toString().substring(0, 8)
                        + ".html"
                )
        );

        selector.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter(
                        "Recibo HTML (*.html)",
                        "html"
                )
        );

        if (selector.showSaveDialog(padre)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path archivo = selector.getSelectedFile().toPath();

        if (!archivo.getFileName().toString()
                .toLowerCase(java.util.Locale.ROOT)
                .endsWith(".html")) {

            archivo = archivo.resolveSibling(
                    archivo.getFileName() + ".html"
            );
        }

        if (Files.exists(archivo)
                && JOptionPane.showConfirmDialog(
                        padre,
                        "El archivo existe. ¿Deseas reemplazarlo?",
                        "Confirmar guardado",
                        JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Files.writeString(
                    archivo,
                    html,
                    StandardCharsets.UTF_8
            );

            JOptionPane.showMessageDialog(
                    padre,
                    "Recibo guardado. Puedes abrirlo en tu navegador:\n"
                    + archivo.toAbsolutePath()
            );

        } catch (IOException | SecurityException error) {
            JOptionPane.showMessageDialog(
                    padre,
                    "No se pudo guardar: " + error.getMessage()
                    + "\nLa operación ya fue registrada; no la repitas.",
                    "Guardar comprobante",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void guardarComprobante(
            Usuario usuarioComprobante,
            Movimiento movimiento,
            JDialog padre) {

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar comprobante TXT");

        selector.setSelectedFile(
                new java.io.File(
                        usuarioComprobante.getCodigo()
                        + "-"
                        + movimiento.getReferencia()
                        + ".txt"
                )
        );

        if (selector.showSaveDialog(padre)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path archivo = selector.getSelectedFile().toPath();

        if (!archivo.getFileName().toString()
                .toLowerCase(java.util.Locale.ROOT)
                .endsWith(".txt")) {

            archivo = archivo.resolveSibling(
                    archivo.getFileName() + ".txt"
            );
        }

        if (Files.exists(archivo)
                && JOptionPane.showConfirmDialog(
                        padre,
                        "El archivo existe. ¿Deseas reemplazarlo?",
                        "Confirmar guardado",
                        JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Files.writeString(
                    archivo,
                    textoComprobante(usuarioComprobante, movimiento),
                    StandardCharsets.UTF_8
            );

            JOptionPane.showMessageDialog(
                    padre,
                    "Comprobante guardado en:\n"
                    + archivo.toAbsolutePath()
            );

        } catch (IOException | SecurityException e) {
            JOptionPane.showMessageDialog(
                    padre,
                    "No se pudo guardar: " + e.getMessage()
                    + "\nLa operación ya fue registrada; no la repitas.",
                    "Error al guardar",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String textoComprobante(
            Usuario usuarioComprobante,
            Movimiento movimiento) {

        String numero = banco.ingresar(
                usuarioComprobante.getCodigo()
        ).getNumeroCuenta();

        return "Número de cuenta: " + numero + "\n"
                + Comprobante.generar(usuarioComprobante, movimiento)
                        .replace(
                                "Código de cuenta: ",
                                "Código de acceso: "
                        );
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(
                this,
                mensaje,
                "Revisar datos",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void salir() {
        if (banco.getCantidadUsuarios() == 0
                || JOptionPane.showConfirmDialog(
                        this,
                        "Los usuarios, saldos e historiales no se conservan "
                        + "al cerrar el programa.\n"
                        + "Los comprobantes guardados sí permanecen. ¿Salir?",
                        "Cerrar Banco UVG",
                        JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION) {

            dispose();
        }
    }

    public static <T> String formatearArreglo(T[] datos) {
        if (datos.length == 0) {
            return "No hay movimientos para mostrar.\n";
        }

        StringBuilder texto = new StringBuilder();

        for (T dato : datos) {
            texto.append(dato).append(System.lineSeparator());
        }

        return texto.toString();
    }
}