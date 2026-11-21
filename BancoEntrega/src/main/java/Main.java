import java.awt.*;
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

    private static final Color VERDE = new Color(0, 107, 63);
    private static final Color FONDO = new Color(243, 246, 244);
    private static final Color TEXTO = new Color(36, 51, 43);
    private static final Color SUAVE = new Color(225, 242, 230);

    private static final int TAB_CUENTAS = 0;
    private static final int TAB_INGRESAR = 1;
    private static final int TAB_OPERACIONES = 3;
    private static final int TAB_HISTORIAL = 4;

    private final Banco banco = new Banco();
    private final JTabbedPane paginas = new JTabbedPane();
    private final JButton[] menu = new JButton[5];

    private final JLabel ruta = new JLabel("Inicio / Cuentas");
    private final JLabel totalCuentas =
            new JLabel("Directorio de cuentas (0)");

    private final CardLayout tarjetasHistorial = new CardLayout();
    private final JPanel panelHistorial =
            new JPanel(tarjetasHistorial);

    private final JTextField codigoHistorial = new JTextField(22);
    private final JTextField usuarioIngreso = new JTextField(22);
    private final JTextField codigoIngreso = new JTextField(22);
    private final JTextField montoOperacion = campoNumerico();
    private final JTextField numeroDestino = campoNumerico();

    private final JButton cerrarSesion = new JButton("Ingresar");

    private final JLabel titularHistorial = new JLabel();
    private final JLabel saldoHistorial = new JLabel();
    private final JLabel detalleCuenta = new JLabel();
    private final JLabel saldo = new JLabel();
    private final JLabel depositado = new JLabel();
    private final JLabel retirado = new JLabel();
    private final JLabel operaciones = new JLabel();
    private final JLabel titular = new JLabel();
    private final JLabel estado = new JLabel(" ");

    private boolean historialDesbloqueado;
    private ConsultaHistorial consultaHistorial;
    private CuentaBancaria cuenta;
    private CuentaSesion cuentaSesion;
    private boolean operacionEnCurso;
    private boolean registroEnCurso;
    private java.util.List<MovimientoGuardado> movimientosSesion =
        new java.util.ArrayList<>();

    private final JComboBox<String> filtro = new JComboBox<>(
            new String[]{
                "Todos", "Depósitos", "Retiros", "Transferencias"
            }
    );

    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{
                "N.º", "Fecha y hora", "Operación",
                "Monto (Q)", "Saldo resultante (Q)"
            }, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    private final JTable tabla = new JTable(modelo);

    private final DefaultTableModel modeloUsuarios =
            new DefaultTableModel(
                    new String[]{
                        "Usuario", "Titular", "Número de cuenta"
                    }, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };

    private final JTable tablaUsuarios = new JTable(modeloUsuarios);

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
                "Label.font", "Button.font", "TextField.font",
                "TabbedPane.font", "ComboBox.font",
                "Table.font", "TableHeader.font"
            };

            for (String clave : claves) {
                UIManager.put(clave, fuente);
            }

            UIManager.put("control", FONDO);
            new Main().setVisible(true);
        });
    }

    public Main() {
        super("WillBAC | Banca educativa");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                salir();
            }
        });

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(FONDO);

        JPanel cabecera = new JPanel(new BorderLayout(20, 0));
        cabecera.setBackground(VERDE);
        cabecera.setBorder(new EmptyBorder(18, 28, 18, 28));

        JPanel marca = new JPanel(new GridLayout(2, 1, 0, 4));
        marca.setOpaque(false);

        JLabel nombre = new JLabel("WillBAC");
        nombre.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        nombre.setForeground(Color.WHITE);

        JLabel universidad = new JLabel(
                "Universidad del Valle de Guatemala · Altiplano"
        );
        universidad.setForeground(Color.WHITE);
        universidad.setFont(
                new Font(Font.SANS_SERIF, Font.PLAIN, 15)
        );

        marca.add(nombre);
        marca.add(universidad);
        cabecera.add(marca, BorderLayout.CENTER);

        cerrarSesion.setUI(
                new javax.swing.plaf.basic.BasicButtonUI()
        );
        cerrarSesion.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 15)
        );
        cerrarSesion.setForeground(Color.WHITE);
        cerrarSesion.setBackground(VERDE);
        cerrarSesion.setFocusPainted(false);

        cerrarSesion.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                Color.WHITE, 1, true
                        ),
                        new EmptyBorder(10, 18, 10, 18)
                )
        );

        cerrarSesion.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        cerrarSesion.addActionListener(e -> {
            if (cuentaSesion != null || cuenta != null || historialDesbloqueado) {
                cerrarSesion();
            } else {
                paginas.setSelectedIndex(TAB_INGRESAR);
            }
        });

        JPanel acceso = new JPanel(new GridBagLayout());
        acceso.setOpaque(false);
        acceso.add(cerrarSesion);

        cabecera.add(acceso, BorderLayout.EAST);
        raiz.add(cabecera, BorderLayout.NORTH);

        // La navegación se controla desde el menú lateral.
        paginas.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(
                    int colocacion, int filas, int alto) {
                return 0;
            }

            @Override
            protected void paintTabArea(
                    Graphics g, int colocacion, int seleccion) {
            }

            @Override
            protected void paintContentBorder(
                    Graphics g, int colocacion, int seleccion) {
            }
        });
 
        paginas.setBorder(null);
        paginas.setOpaque(false);

        paginas.addTab("Cuentas", crearListaCuentas());
        paginas.addTab("Ingresar", crearIngreso());
        paginas.addTab("Crear usuario", crearRegistro());
        paginas.addTab("Operaciones", crearCuenta());
        paginas.addTab("Historial", panelHistorial);

        JPanel lateral = new JPanel(new BorderLayout());
        lateral.setBackground(Color.WHITE);
        lateral.setPreferredSize(new Dimension(205, 0));
        lateral.setBorder(new EmptyBorder(25, 12, 20, 12));

        JPanel opciones = new JPanel(new GridLayout(5, 1, 0, 10));
        opciones.setOpaque(false);

        String[] nombres = {
            "Cuentas", "Ingresar", "Crear usuario",
            "Operaciones", "Historial"
        };

        for (int i = 0; i < nombres.length; i++) {
            final int indice = i;

            JButton opcion = boton(
                    nombres[i],
                    e -> paginas.setSelectedIndex(indice)
            );

            opcion.setHorizontalAlignment(SwingConstants.LEFT);
            opcion.setIcon(new IconoMenu(i));
            opcion.setIconTextGap(14);
            opcion.setPreferredSize(new Dimension(180, 49));
            opcion.setFont(
                    new Font(Font.SANS_SERIF, Font.BOLD, 15)
            );

            menu[i] = opcion;
            opciones.add(opcion);
        }

        lateral.add(opciones, BorderLayout.NORTH);

        JLabel pieMenu = new JLabel("Cuenta Universitaria");
        pieMenu.setForeground(new Color(100, 112, 105));
        pieMenu.setFont(
                new Font(Font.SANS_SERIF, Font.PLAIN, 13)
        );

        lateral.add(pieMenu, BorderLayout.SOUTH);
        raiz.add(lateral, BorderLayout.WEST);

        JPanel espacio = new JPanel(new BorderLayout(0, 14));
        espacio.setOpaque(false);
        espacio.setBorder(new EmptyBorder(20, 24, 16, 24));

        ruta.setForeground(VERDE);
        ruta.setFont(
                new Font(Font.SANS_SERIF, Font.PLAIN, 14)
        );

        espacio.add(ruta, BorderLayout.NORTH);
        espacio.add(paginas, BorderLayout.CENTER);

        JPanel pie = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 0)
        );
        pie.setOpaque(false);
        pie.add(boton("Salir", e -> salir()));

        espacio.add(pie, BorderLayout.SOUTH);
        raiz.add(espacio, BorderLayout.CENTER);

        paginas.addChangeListener(e -> {
            bloquearHistorial();
            actualizarMenu();

            if (paginas.getSelectedIndex() == TAB_CUENTAS) {
                actualizarUsuarios();
            }

            if (paginas.getSelectedIndex() == TAB_HISTORIAL) {
                cargarHistorialSesion();
            }

            if (paginas.getSelectedIndex() == TAB_INGRESAR) {
                codigoIngreso.requestFocusInWindow();
            }
        });

        habilitarCuenta(false);
        actualizarUsuarios();

        setContentPane(raiz);

        Rectangle pantalla = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();

        setMinimumSize(new Dimension(
                Math.min(1040, pantalla.width),
                Math.min(680, pantalla.height)
        ));

        setSize(
                Math.min(1280, pantalla.width),
                Math.min(820, pantalla.height)
        );

        setLocationRelativeTo(null);
    }

    private void actualizarMenu() {
        for (int i = 0; i < menu.length; i++) {
            if (menu[i] == null) {
                continue;
            }

            boolean activo = paginas.getSelectedIndex() == i;

            menu[i].setEnabled(paginas.isEnabledAt(i));
            menu[i].setBackground(activo ? SUAVE : Color.WHITE);
            menu[i].setForeground(
                    menu[i].isEnabled() ? VERDE : Color.GRAY
            );
        }

        ruta.setText(
                "Inicio / "
                + paginas.getTitleAt(paginas.getSelectedIndex())
        );
    }

    private void habilitarCuenta(boolean habilitado) {
        for (int i = TAB_OPERACIONES; i < paginas.getTabCount(); i++) {
            paginas.setEnabledAt(
                    i,
                    i == TAB_HISTORIAL || habilitado
            );
        }

        actualizarMenu();
        actualizarEstadoSesion();
    }

        private void actualizarEstadoSesion() {
    boolean consultaAbierta =
            consultaHistorial != null && historialDesbloqueado;

    boolean sesionAbierta =
            cuentaSesion != null
            || cuenta != null
            || consultaAbierta;

    cerrarSesion.setEnabled(true);
    cerrarSesion.setText(
        sesionAbierta ? "Cerrar sesión" : "Ingresar"
    );
}

    private JPanel crearListaCuentas() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);

        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 4));
        titulos.setOpaque(false);

        JLabel titulo = new JLabel("Cuentas registradas");
        titulo.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 28)
        );
        titulo.setForeground(TEXTO);

        JLabel subtitulo = new JLabel(
                "Consulta las cuentas y sus números "
                + "para realizar transferencias."
        );
        subtitulo.setForeground(new Color(95, 109, 100));

        titulos.add(titulo);
        titulos.add(subtitulo);

        panel.add(titulos, BorderLayout.NORTH);

        JPanel directorio = new PanelRedondo();
        directorio.setLayout(new BorderLayout(0, 16));
        directorio.setBorder(new EmptyBorder(20, 20, 20, 20));

        totalCuentas.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 21)
        );
        totalCuentas.setForeground(TEXTO);

        directorio.add(totalCuentas, BorderLayout.NORTH);

        prepararTabla(tablaUsuarios);

        tablaUsuarios.setDefaultRenderer(
                Object.class,
                new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object valor,
                    boolean seleccionada, boolean foco,
                    int fila, int columna) {

                super.getTableCellRendererComponent(
                        t, valor, seleccionada, foco, fila, columna
                );

                setHorizontalAlignment(
                        columna == 0
                                ? SwingConstants.LEFT
                                : SwingConstants.CENTER
                );

                setBorder(new EmptyBorder(8, 12, 8, 12));

                setBackground(
                        seleccionada
                                ? new Color(198, 230, 207)
                                : fila % 2 == 0
                                        ? Color.WHITE
                                        : new Color(239, 247, 241)
                );

                setForeground(TEXTO);

                setFont(new Font(
                        Font.SANS_SERIF,
                        columna == 0 ? Font.BOLD : Font.PLAIN,
                        16
                ));

                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(tablaUsuarios);

        scroll.setBorder(
                BorderFactory.createLineBorder(
                        new Color(222, 230, 224)
                )
        );
        scroll.getViewport().setBackground(Color.WHITE);

        directorio.add(scroll, BorderLayout.CENTER);

        JLabel ayuda = new JLabel(
                "Ingresa con tu código de acceso. "
                + "Transfiere con el número de cuenta."
        );
        ayuda.setForeground(new Color(95, 109, 100));

        directorio.add(ayuda, BorderLayout.SOUTH);
        panel.add(directorio, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearIngreso() {
        JPanel marco = new JPanel(new GridBagLayout());
        marco.setOpaque(false);

        JPanel formulario = new JPanel(new BorderLayout(0, 18));
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(new EmptyBorder(30, 34, 30, 34));

        JLabel titulo = new JLabel("Ingresa a tu cuenta");
        titulo.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 24)
        );
        titulo.setForeground(VERDE);

        JPanel encabezado = new JPanel(new BorderLayout(0, 15));
encabezado.setOpaque(false);

java.net.URL rutaImagen =
        Main.class.getResource("/IMG/banco-ingreso.jpg");

if (rutaImagen != null) {
    ImageIcon original = new ImageIcon(rutaImagen);

    if (original.getIconWidth() > 0
            && original.getIconHeight() > 0) {

        double escala = Math.min(
                360.0 / original.getIconWidth(),
                180.0 / original.getIconHeight()
        );

        int ancho = Math.max(
                1, (int) (original.getIconWidth() * escala)
        );
        int alto = Math.max(
                1, (int) (original.getIconHeight() * escala)
        );

        Image imagen = original.getImage().getScaledInstance(
                ancho, alto, Image.SCALE_SMOOTH
        );

        JLabel ilustracion = new JLabel(new ImageIcon(imagen));
        ilustracion.setHorizontalAlignment(SwingConstants.CENTER);

        encabezado.add(ilustracion, BorderLayout.CENTER);
    }
}

encabezado.add(titulo, BorderLayout.SOUTH);
formulario.add(encabezado, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 10));
        campos.setOpaque(false);

        campos.add(new JLabel("Nombre de usuario"));
        campos.add(usuarioIngreso);

        campos.add(new JLabel("Código de acceso"));
        campos.add(codigoIngreso);

        formulario.add(campos, BorderLayout.CENTER);

        JButton entrar = boton("Ingresar", e -> {
    String nombreUsuario = usuarioIngreso.getText().trim();
    String codigo = codigoIngreso.getText();

    if (nombreUsuario.isBlank()) {
        mostrarError("Escribe tu nombre de usuario.");
        return;
    }

    try {
        Usuario.normalizarCodigo(codigo);
    } catch (IllegalArgumentException error) {
        mostrarError(error.getMessage());
        return;
    }

    JButton botonIngreso = (JButton) e.getSource();
    botonIngreso.setEnabled(false);
    botonIngreso.setText("Comprobando...");

    SwingWorker<CuentaSesion, Void> tarea =
            new SwingWorker<CuentaSesion, Void>() {

        @Override
        protected CuentaSesion doInBackground() throws Exception {
            UsuarioDAO usuarios = new UsuarioDAO();

            if (!usuarios.verificarAcceso(nombreUsuario, codigo)) {
                return null;
            }

            CuentaDAO cuentas = new CuentaDAO();
            return cuentas.buscarPorUsuario(nombreUsuario);
        }

        @Override
        protected void done() {
            try {
                CuentaSesion datos = get();

                if (datos == null) {
                    mostrarError("Usuario o código incorrectos.");
                    return;
                }
                cuentaSesion = datos;
                limpiarCamposOperacion();
                habilitarCuenta(true);
                actualizarResumenSesion();
                paginas.setSelectedIndex(TAB_OPERACIONES);

                JOptionPane.showMessageDialog(
                    Main.this,
                    "Acceso verificado en MySQL."
                    + "\nTitular: " + datos.nombreCompleto()
                    + "\nCuenta: " + datos.numeroCuenta()
                    + "\nSaldo: Q" + Dinero.formatear(datos.saldo()),
                    "Verificación de acceso",
                    JOptionPane.INFORMATION_MESSAGE
                );

            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                mostrarError("La verificación fue interrumpida.");

            } catch (java.util.concurrent.ExecutionException error) {
                mostrarError(
                    "No se pudo completar la consulta: "
                    + error.getCause().getMessage()
                );

            } finally {
                codigoIngreso.setText("");
                botonIngreso.setEnabled(true);
                botonIngreso.setText("Ingresar");
            }
        }
    };

    tarea.execute();
});

        codigoIngreso.addActionListener(e -> entrar.doClick());

        formulario.add(entrar, BorderLayout.SOUTH);
        marco.add(formulario);

        return conDesplazamiento(marco);
    }

    private JPanel crearAccesoHistorial() {
        JPanel marco = new JPanel(new GridBagLayout());
        marco.setOpaque(false);

        JPanel formulario = new JPanel(new BorderLayout(0, 18));
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(new EmptyBorder(28, 32, 28, 32));

        JLabel titulo = new JLabel("Consultar movimientos e historial");
        titulo.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 22)
        );

        formulario.add(titulo, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 10));
        campos.setOpaque(false);

        campos.add(new JLabel(
                "Escribe el código de acceso "
                + "para consultar el historial y saldo."
        ));
        campos.add(new JLabel("Código de acceso"));
        campos.add(codigoHistorial);

        formulario.add(campos, BorderLayout.CENTER);

        JButton consultar = boton("Consultar historial", e -> {
            try {
                consultaHistorial = banco.consultarHistorial(
                        codigoHistorial.getText()
                );

                historialDesbloqueado = true;

                Usuario usuarioConsultado =
                        consultaHistorial.getUsuario();

                String numeroCuenta = banco.ingresar(
                        usuarioConsultado.getCodigo()
                ).getNumeroCuenta();

                titularHistorial.setText(
                        "Titular: "
                        + usuarioConsultado.getNombreCompleto()
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
    JPanel marco = new JPanel(new GridBagLayout());
    marco.setOpaque(false);

    JPanel formulario = new JPanel(new BorderLayout(0, 14));
    formulario.setBackground(Color.WHITE);
    formulario.setBorder(new EmptyBorder(20, 28, 20, 28));

    JLabel titulo = new JLabel("Crear usuario y agregar cuenta");
    titulo.setFont(
        new Font(Font.SANS_SERIF, Font.BOLD, 22)
    );
    titulo.setForeground(VERDE);

    formulario.add(titulo, BorderLayout.NORTH);

    JTextField nombre = new JTextField(28);
    JTextField usuario = new JTextField(28);
    JTextField codigo = new JTextField(28);
    JTextField inicial = campoNumerico();

    inicial.setText("0.00");

    JPanel campos = new JPanel();
    campos.setLayout(
        new BoxLayout(campos, BoxLayout.Y_AXIS)
    );
    campos.setOpaque(false);

    campos.add(new JLabel("Nombre completo del titular"));
    campos.add(nombre);

    campos.add(new JLabel("Nombre de usuario"));
    campos.add(usuario);

    campos.add(new JLabel("Código de acceso (lo eliges tú)"));
    campos.add(codigo);

    campos.add(new JLabel("Saldo inicial (Q)"));
    campos.add(inicial);

    campos.add(new JLabel(
        "El número de cuenta se genera automáticamente al registrar."
    ));

    campos.add(new JLabel(
        "Los datos se guardan en MySQL y permanecen al cerrar el programa."
    ));

    formulario.add(campos, BorderLayout.CENTER);
    JButton crear = boton("Registrar usuario y crear cuenta", e -> {
    if (registroEnCurso || operacionEnCurso) {
        mostrarError("Espera a que termine el proceso actual.");
        return;
    }
    final String nombreUsuario = usuario.getText().trim();
    final String nombreCompleto = nombre.getText().trim();
    final String codigoAcceso = codigo.getText();
    final BigDecimal saldoInicial;

    try {
        saldoInicial = leerDinero(inicial.getText());
    } catch (IllegalArgumentException error) {
        mostrarError(error.getMessage());
        return;
    }

    JButton botonRegistro = (JButton) e.getSource();

    registroEnCurso = true;
    botonRegistro.setEnabled(false);
    botonRegistro.setText("Registrando...");

    nombre.setEnabled(false);
    usuario.setEnabled(false);
    codigo.setEnabled(false);
    inicial.setEnabled(false);

    SwingWorker<CuentaSesion, Void> tarea =
            new SwingWorker<CuentaSesion, Void>() {

        @Override
        protected CuentaSesion doInBackground() throws Exception {
            RegistroDAO dao = new RegistroDAO();

            return dao.registrar(
                nombreUsuario,
                nombreCompleto,
                codigoAcceso,
                saldoInicial
            );
        }

        @Override
        protected void done() {
            try {
                CuentaSesion nueva = get();

                JOptionPane.showMessageDialog(
                    Main.this,
                    "Usuario y cuenta guardados en MySQL."
                    + "\nUsuario: " + nueva.nombreUsuario()
                    + "\nNúmero de cuenta: " + nueva.numeroCuenta()
                    + "\nSaldo inicial: Q"
                    + Dinero.formatear(nueva.saldoInicial())
                    + "\nAhora ingresa con tu usuario y código.",
                    "Cuenta creada",
                    JOptionPane.INFORMATION_MESSAGE
                );

                nombre.setText("");
                usuario.setText("");
                codigo.setText("");
                inicial.setText("0.00");

                cerrarSesion();
                paginas.setSelectedIndex(TAB_INGRESAR);

                usuarioIngreso.setText(nueva.nombreUsuario());
                codigoIngreso.setText("");
                codigoIngreso.requestFocusInWindow();

            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();

                mostrarError(
                    "Se interrumpió la comprobación del registro."
                    + "\nRevisa el directorio antes de repetir."
                );

            } catch (java.util.concurrent.ExecutionException error) {
                Throwable causa = error.getCause();

                if (causa instanceof IllegalArgumentException) {
                    mostrarError(causa.getMessage());

                } else if (causa instanceof java.sql.SQLException
                        && ((java.sql.SQLException) causa)
                                .getErrorCode() == 1062) {

                    mostrarError(
                        "Ese nombre de usuario ya está registrado."
                        + "\nElige otro nombre o inicia sesión."
                    );

                } else {
                    mostrarError(
                        "No se pudo confirmar el registro: "
                        + causa.getMessage()
                        + "\nRevisa el directorio antes de repetir."
                    );
                }

            } finally {
                registroEnCurso = false;
                botonRegistro.setEnabled(true);
                botonRegistro.setText(
                    "Registrar usuario y crear cuenta"
                );

                nombre.setEnabled(true);
                usuario.setEnabled(true);
                codigo.setEnabled(true);
                inicial.setEnabled(true);
            }
        }
    };

    tarea.execute();
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
        cuentaSesion = null;
        usuarioIngreso.setText("");

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

        titular.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 15)
        );
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
        herramientas.add(
            boton("Buscar por número", e -> buscarMovimiento())
        );
        herramientas.add(
            boton(
                "Generar comprobante",
                e -> comprobanteSeleccionado()
            )
        );

        filtro.addActionListener(e -> {
        if (cuentaSesion != null && historialDesbloqueado) {
           actualizarTabla();
          }
        });


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

        prepararTabla(tabla);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.setRowHeight(36);

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
                        "Selecciona una operación "
                        + "para generar su comprobante."
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

        JPanel operacionesCuenta = new JPanel(new GridBagLayout());
        operacionesCuenta.setOpaque(false);

        JPanel formulario = new JPanel(new BorderLayout(0, 18));
        formulario.setBackground(Color.WHITE);
        formulario.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel titulo = new JLabel("Operaciones de la cuenta");
        titulo.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 22)
        );
        formulario.add(titulo, BorderLayout.NORTH);

        JPanel campos = new JPanel(new GridLayout(0, 1, 0, 6));
        campos.setOpaque(false);

        campos.add(new JLabel(
                "Monto (Q) para depositar, retirar o transferir"
        ));
        campos.add(montoOperacion);
        campos.add(new JLabel(
                "Usa hasta dos decimales. Ejemplo: 150.50"
        ));
        campos.add(new JLabel(
                "Número de cuenta destino (solo para transferir)"
        ));
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
                        "Consulta el historial para ver tus operaciones."
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
        campo.setMargin(new Insets(6, 10, 6, 10));
        campo.setPreferredSize(new Dimension(360, 38));
        campo.setMinimumSize(new Dimension(160, 38));

        return campo;
    }

    private JPanel tarjeta(String nombre, JLabel valor) {
        JPanel panel = new PanelRedondo();
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(17, 18, 17, 18));

        valor.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 23)
        );
        valor.setForeground(VERDE);

        JLabel etiqueta = new JLabel(nombre);
        etiqueta.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 13)
        );
        etiqueta.setForeground(TEXTO);

        panel.add(etiqueta, BorderLayout.NORTH);
        panel.add(valor, BorderLayout.CENTER);

        return panel;
    }

    private JButton boton(
            String texto,
            java.awt.event.ActionListener accion) {

        JButton boton = new JButton(texto);

        boton.setUI(
                new javax.swing.plaf.basic.BasicButtonUI()
        );
        boton.setBackground(VERDE);
        boton.setForeground(Color.WHITE);
        boton.setFont(
                new Font(Font.SANS_SERIF, Font.BOLD, 14)
        );
        boton.setFocusPainted(false);
        boton.setBorder(new EmptyBorder(10, 16, 10, 16));
        boton.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        boton.addActionListener(accion);

        return boton;
    }

    private void prepararTabla(JTable t) {
        t.setRowHeight(46);
        t.setFont(
                new Font(Font.SANS_SERIF, Font.PLAIN, 15)
        );
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setShowVerticalLines(false);
        t.setGridColor(new Color(222, 230, 224));

        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(
                new Dimension(0, 42)
        );

        t.getTableHeader().setDefaultRenderer(
                new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tabla, Object valor,
                    boolean seleccionada, boolean foco,
                    int fila, int columna) {

                super.getTableCellRendererComponent(
                        tabla, valor, seleccionada,
                        foco, fila, columna
                );

                setBackground(VERDE);
                setForeground(Color.WHITE);
                setFont(
                        new Font(Font.SANS_SERIF, Font.BOLD, 14)
                );
                setBorder(new EmptyBorder(8, 12, 8, 12));

                setHorizontalAlignment(
                        columna == 0
                                ? SwingConstants.LEFT
                                : SwingConstants.CENTER
                );

                return this;
            }
        });
    }

    private static class PanelRedondo extends JPanel {

        PanelRedondo() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D dibujo = (Graphics2D) g.create();

            dibujo.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            dibujo.setColor(Color.WHITE);
            dibujo.fillRoundRect(
                    0, 0, getWidth() - 1, getHeight() - 1,
                    18, 18
            );

            dibujo.setColor(new Color(220, 228, 222));
            dibujo.drawRoundRect(
                    0, 0, getWidth() - 1, getHeight() - 1,
                    18, 18
            );

            dibujo.dispose();
            super.paintComponent(g);
        }
    }

    private static class IconoMenu implements Icon {
        private final int tipo;

        IconoMenu(int tipo) {
            this.tipo = tipo;
        }

        @Override
        public int getIconWidth() {
            return 24;
        }

        @Override
        public int getIconHeight() {
            return 24;
        }

        @Override
        public void paintIcon(
                Component c, Graphics g, int x, int y) {

            Graphics2D d = (Graphics2D) g.create();
            d.translate(x, y);

            d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            d.setColor(
                    c.isEnabled() ? c.getForeground() : Color.GRAY
            );
            d.setStroke(new BasicStroke(1.8f));

            switch (tipo) {
                case 0:
                    d.drawRoundRect(2, 4, 20, 16, 4, 4);
                    d.drawLine(2, 9, 22, 9);
                    d.drawLine(6, 15, 13, 15);
                    break;

                case 1:
                    d.drawLine(3, 12, 17, 12);
                    d.drawLine(12, 7, 17, 12);
                    d.drawLine(12, 17, 17, 12);
                    d.drawLine(17, 3, 22, 3);
                    d.drawLine(22, 3, 22, 21);
                    d.drawLine(17, 21, 22, 21);
                    break;

                case 2:
                    d.drawOval(5, 2, 8, 8);
                    d.drawArc(1, 12, 16, 17, 0, 180);
                    d.drawLine(20, 10, 20, 18);
                    d.drawLine(16, 14, 24, 14);
                    break;

                case 3:
                    d.drawLine(2, 7, 21, 7);
                    d.drawLine(16, 2, 21, 7);
                    d.drawLine(16, 12, 21, 7);
                    d.drawLine(22, 17, 3, 17);
                    d.drawLine(8, 12, 3, 17);
                    d.drawLine(8, 22, 3, 17);
                    break;

                default:
                    d.drawRoundRect(4, 2, 16, 20, 3, 3);
                    d.drawLine(8, 7, 16, 7);
                    d.drawLine(8, 12, 16, 12);
                    d.drawLine(8, 17, 14, 17);
            }

            d.dispose();
        }
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
    if (cuentaSesion == null) {
        mostrarError("Primero inicia sesión.");
        return;
    }

    if (operacionEnCurso) {
        return;
    }

    final BigDecimal monto;

    try {
        monto = leerDinero(montoOperacion.getText());

        if (monto.signum() <= 0) {
            throw new IllegalArgumentException(
                "El monto debe ser mayor que cero."
            );
        }

    } catch (IllegalArgumentException error) {
        mostrarError(error.getMessage());
        return;
    }

    final CuentaSesion sesionOperacion = cuentaSesion;
    final long numeroCuenta = sesionOperacion.numeroCuenta();
    final String tipo = esDeposito ? "Depósito" : "Retiro";

    operacionEnCurso = true;
    estado.setText("Procesando " + tipo.toLowerCase() + "...");

    SwingWorker<Void, Void> tarea = new SwingWorker<Void, Void>() {

        @Override
        protected Void doInBackground() throws Exception {
            OperacionDAO dao = new OperacionDAO();

            if (esDeposito) {
                dao.depositar(numeroCuenta, monto);
            } else {
                dao.retirar(numeroCuenta, monto);
            }

            return null;
        }

        @Override
        protected void done() {
            try {
                get();

                // No actualizar otra sesión si el usuario cambió.
                if (cuentaSesion != sesionOperacion) {
                    return;
                }

                montoOperacion.setText("");
                estado.setText(tipo + " registrado en MySQL.");
                actualizarResumenSesion();

                JOptionPane.showMessageDialog(
                    Main.this,
                    tipo + " registrado por Q"
                    + Dinero.formatear(monto)
                    + "\nCuenta: " + numeroCuenta,
                    "Operación registrada",
                    JOptionPane.INFORMATION_MESSAGE
                );

            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();

                mostrarError(
                    "Se interrumpió la comprobación del resultado."
                    + "\nConsulta el historial antes de repetir."
                );

            } catch (java.util.concurrent.ExecutionException error) {
                Throwable causa = error.getCause();

                if (causa instanceof IllegalArgumentException) {
                    mostrarError(causa.getMessage());
                } else {
                    mostrarError(
                        "No se pudo confirmar la operación: "
                        + causa.getMessage()
                        + "\nConsulta el historial antes de repetir."
                    );
                }

            } finally {
                operacionEnCurso = false;
            }
        }
    };

    tarea.execute();
}

        private void transferir() {
    if (cuentaSesion == null) {
        mostrarError("Primero inicia sesión.");
        return;
    }

    if (operacionEnCurso) {
        return;
    }

    final BigDecimal monto;
    final long destino;

    try {
        monto = leerDinero(montoOperacion.getText());
        destino = Long.parseLong(numeroDestino.getText().trim());

        if (monto.signum() <= 0) {
            throw new IllegalArgumentException(
                "El monto debe ser mayor que cero."
            );
        }

        if (destino <= 0) {
            throw new IllegalArgumentException(
                "El número de cuenta destino debe ser positivo."
            );
        }

        if (destino == cuentaSesion.numeroCuenta()) {
            throw new IllegalArgumentException(
                "No puedes transferir a tu misma cuenta."
            );
        }

    } catch (NumberFormatException error) {
        mostrarError("Escribe un número de cuenta destino válido.");
        return;

    } catch (IllegalArgumentException error) {
        mostrarError(error.getMessage());
        return;
    }

    final CuentaSesion sesionOperacion = cuentaSesion;
    final long origen = sesionOperacion.numeroCuenta();

    int respuesta = JOptionPane.showConfirmDialog(
        this,
        "¿Transferir Q" + Dinero.formatear(monto)
        + "\nDesde la cuenta: " + origen
        + "\nHacia la cuenta: " + destino + "?",
        "Confirmar transferencia",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE
    );

    if (respuesta != JOptionPane.YES_OPTION) {
        estado.setText("Transferencia cancelada.");
        return;
    }

    operacionEnCurso = true;
    estado.setText("Procesando transferencia...");

    SwingWorker<Void, Void> tarea = new SwingWorker<Void, Void>() {

        @Override
        protected Void doInBackground() throws Exception {
            OperacionDAO dao = new OperacionDAO();
            dao.transferir(origen, destino, monto);
            return null;
        }

        @Override
        protected void done() {
            try {
                get();

                if (cuentaSesion != sesionOperacion) {
                    return;
                }

                montoOperacion.setText("");
                numeroDestino.setText("");

                estado.setText("Transferencia registrada en MySQL.");
                actualizarResumenSesion();

                JOptionPane.showMessageDialog(
                    Main.this,
                    "Transferencia registrada por Q"
                    + Dinero.formatear(monto)
                    + "\nCuenta destino: " + destino,
                    "Transferencia realizada",
                    JOptionPane.INFORMATION_MESSAGE
                );

            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();

                mostrarError(
                    "Se interrumpió la comprobación del resultado."
                    + "\nConsulta el historial antes de repetir."
                );

            } catch (java.util.concurrent.ExecutionException error) {
                Throwable causa = error.getCause();

                if (causa instanceof IllegalArgumentException) {
                    mostrarError(causa.getMessage());
                } else {
                    mostrarError(
                        "No se pudo confirmar la transferencia: "
                        + causa.getMessage()
                        + "\nConsulta el historial antes de repetir."
                    );
                }

            } finally {
                operacionEnCurso = false;
            }
        }
    };

    tarea.execute();
}

    private void retirarTodo() {
    if (cuentaSesion == null) {
        mostrarError("Primero inicia sesión.");
        return;
    }

    if (operacionEnCurso) {
        return;
    }

    final CuentaSesion sesionOperacion = cuentaSesion;
    final long numeroCuenta = sesionOperacion.numeroCuenta();

    int respuesta = JOptionPane.showConfirmDialog(
        this,
        "¿Retirar todo el saldo disponible de la cuenta "
        + numeroCuenta + "?"
        + "\nSe retirará el saldo existente al procesar la operación.",
        "Confirmar retiro total",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE
    );

    if (respuesta != JOptionPane.YES_OPTION) {
        estado.setText("Retiro total cancelado.");
        return;
    }

    operacionEnCurso = true;
    estado.setText("Procesando retiro total...");

    SwingWorker<BigDecimal, Void> tarea =
            new SwingWorker<BigDecimal, Void>() {

        @Override
        protected BigDecimal doInBackground() throws Exception {
            OperacionDAO dao = new OperacionDAO();
            return dao.retirarTodo(numeroCuenta);
        }

        @Override
        protected void done() {
            try {
                BigDecimal retirado = get();

                if (cuentaSesion != sesionOperacion) {
                    return;
                }

                montoOperacion.setText("");
                estado.setText("Retiro total registrado en MySQL.");
                actualizarResumenSesion();

                JOptionPane.showMessageDialog(
                    Main.this,
                    "Se retiraron Q" + Dinero.formatear(retirado)
                    + "\nCuenta: " + numeroCuenta,
                    "Retiro realizado",
                    JOptionPane.INFORMATION_MESSAGE
                );

            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();

                mostrarError(
                    "Se interrumpió la comprobación del resultado."
                    + "\nConsulta el historial antes de repetir."
                );

            } catch (java.util.concurrent.ExecutionException error) {
                Throwable causa = error.getCause();

                if (causa instanceof IllegalArgumentException) {
                    mostrarError(causa.getMessage());
                } else {
                    mostrarError(
                        "No se pudo confirmar el retiro: "
                        + causa.getMessage()
                        + "\nConsulta el historial antes de repetir."
                    );
                }

            } finally {
                operacionEnCurso = false;
            }
        }
    };

    tarea.execute();
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
    actualizarEstadoSesion();
    modeloUsuarios.setRowCount(0);
    totalCuentas.setText("Cargando cuentas...");

    SwingWorker<java.util.List<CuentaDirectorio>, Void> tarea =
            new SwingWorker<java.util.List<CuentaDirectorio>, Void>() {

        @Override
        protected java.util.List<CuentaDirectorio> doInBackground()
                throws Exception {

            CuentaDAO dao = new CuentaDAO();
            return dao.listarDirectorio();
        }

        @Override
        protected void done() {
            try {
                java.util.List<CuentaDirectorio> cuentas = get();

                modeloUsuarios.setRowCount(0);

                for (CuentaDirectorio registrada : cuentas) {
                    modeloUsuarios.addRow(new Object[]{
                        registrada.nombreUsuario(),
                        registrada.nombreCompleto(),
                        registrada.numeroCuenta()
                    });
                }

                totalCuentas.setText(
                    "Directorio de cuentas (" + cuentas.size() + ")"
                );

            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                totalCuentas.setText("Consulta interrumpida.");

            } catch (java.util.concurrent.ExecutionException error) {
                totalCuentas.setText("No se pudieron cargar las cuentas.");

                mostrarError(
                    "Error al consultar el directorio: "
                    + error.getCause().getMessage()
                );
            }
        }
    };

    tarea.execute();
}


private void actualizarTabla() {
    modelo.setRowCount(0);

    if (cuentaSesion == null || !historialDesbloqueado) {
        return;
    }

    int filtroSeleccionado = filtro.getSelectedIndex();

    for (MovimientoGuardado movimiento : movimientosSesion) {

        boolean mostrar = switch (filtroSeleccionado) {
            case 1 -> movimiento.tipo().equals("DEPOSITO");

            case 2 -> movimiento.tipo().equals("RETIRO");

            case 3 ->
                movimiento.tipo().equals("TRANSFERENCIA_ENTRADA")
                || movimiento.tipo().equals("TRANSFERENCIA_SALIDA");

            default -> true;
        };

        if (!mostrar) {
            continue;
        }

        modelo.addRow(new Object[]{
            movimiento.numero(),
            movimiento.fecha().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            ),
            movimiento.descripcion(),
            movimiento.monto(),
            movimiento.saldoResultante()
        });
    }

    tabla.getAccessibleContext().setAccessibleDescription(
        "Historial de la cuenta "
        + cuentaSesion.numeroCuenta()
        + ". Movimientos mostrados: "
        + modelo.getRowCount()
    );
}

private void buscarMovimiento() {
    if (cuentaSesion == null || !historialDesbloqueado) {
        mostrarError("Primero abre el historial de tu cuenta.");
        return;
    }

    String texto = JOptionPane.showInputDialog(
        this,
        "Número de movimiento:",
        "Buscar movimiento",
        JOptionPane.QUESTION_MESSAGE
    );

    if (texto == null) {
        return;
    }

    final int numero;

    try {
        numero = Integer.parseInt(texto.trim());

        if (numero <= 0) {
            throw new NumberFormatException();
        }

    } catch (NumberFormatException error) {
        mostrarError("Escribe un número entero mayor que cero.");
        return;
    }

    MovimientoGuardado encontrado = null;

    for (MovimientoGuardado movimiento : movimientosSesion) {
        if (movimiento.numero() == numero) {
            encontrado = movimiento;
            break;
        }
    }

    if (encontrado == null) {
        mostrarError(
            "No existe ese movimiento en el historial cargado."
        );
        return;
    }

    // Mostrar todos para que el filtro no oculte el resultado.
    filtro.setSelectedIndex(0);
    actualizarTabla();

    for (int filaModelo = 0;
            filaModelo < modelo.getRowCount();
            filaModelo++) {

        int numeroFila =
                ((Number) modelo.getValueAt(filaModelo, 0)).intValue();

        if (numeroFila == numero) {
            int filaVisible =
                    tabla.convertRowIndexToView(filaModelo);

            if (filaVisible >= 0) {
                tabla.setRowSelectionInterval(
                    filaVisible, filaVisible
                );

                tabla.scrollRectToVisible(
                    tabla.getCellRect(filaVisible, 0, true)
                );
            }

            break;
        }
    }
}

private void comprobanteSeleccionado() {
    if (cuentaSesion == null || !historialDesbloqueado) {
        mostrarError("Primero abre el historial de tu cuenta.");
        return;
    }

    int filaVisible = tabla.getSelectedRow();

    if (filaVisible < 0) {
        mostrarError("Selecciona un movimiento de la tabla.");
        return;
    }

    int filaModelo = tabla.convertRowIndexToModel(filaVisible);

    int numero = ((Number) modelo.getValueAt(
        filaModelo, 0
    )).intValue();

    MovimientoGuardado seleccionado = null;

    for (MovimientoGuardado movimiento : movimientosSesion) {
        if (movimiento.numero() == numero) {
            seleccionado = movimiento;
            break;
        }
    }

    if (seleccionado == null) {
        mostrarError(
            "No se encontró el movimiento."
            + "\nVuelve a abrir el historial."
        );
        return;
    }

    // Preparar ambos formatos con los mismos datos.
    final String html =
            Comprobante.generarHtml(cuentaSesion, seleccionado);

    final String texto =
            Comprobante.generar(cuentaSesion, seleccionado);

    final String nombreArchivo =
            "comprobante-"
            + cuentaSesion.numeroCuenta()
            + "-" + seleccionado.numero() + ".txt";

    JDialog dialogo = new JDialog(
        this,
        "Comprobante del movimiento " + seleccionado.numero(),
        true
    );

    dialogo.setDefaultCloseOperation(
        WindowConstants.DISPOSE_ON_CLOSE
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

    // Reutiliza el método de guardado HTML que ya existe.
    controles.add(
        boton("Guardar HTML", e ->
            guardarReciboHtml(html, dialogo)
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
                    "No se pudo imprimir: " + error.getMessage()
                    + "\nEl movimiento guardado no se modifica.",
                    "Error de impresión",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        })
    );

    controles.add(
        boton("Guardar TXT", e -> {
            JFileChooser selector = new JFileChooser();
            selector.setDialogTitle("Guardar comprobante TXT");

            selector.setSelectedFile(
                new java.io.File(nombreArchivo)
            );

            selector.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter(
                    "Archivo de texto (*.txt)", "txt"
                )
            );

            if (selector.showSaveDialog(dialogo)
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

            if (Files.exists(archivo)) {
                int respuesta = JOptionPane.showConfirmDialog(
                    dialogo,
                    "El archivo ya existe. ¿Deseas reemplazarlo?",
                    "Confirmar guardado",
                    JOptionPane.YES_NO_OPTION
                );

                if (respuesta != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try {
                Files.writeString(
                    archivo, texto, StandardCharsets.UTF_8
                );

                JOptionPane.showMessageDialog(
                    dialogo,
                    "Comprobante guardado en:\n"
                    + archivo.toAbsolutePath()
                );

            } catch (IOException | SecurityException error) {
                JOptionPane.showMessageDialog(
                    dialogo,
                    "No se pudo guardar: " + error.getMessage()
                    + "\nEl movimiento guardado no se modifica.",
                    "Error al guardar",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        })
    );

    controles.add(
        boton("Cerrar", e -> dialogo.dispose())
    );

    dialogo.add(controles, BorderLayout.SOUTH);

    Rectangle pantalla = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getMaximumWindowBounds();

    dialogo.setSize(
        Math.min(850, pantalla.width),
        Math.min(680, pantalla.height)
    );

    dialogo.setLocationRelativeTo(this);
    dialogo.setVisible(true);
}

    private void mostrarComprobante(Movimiento movimiento) {
        mostrarComprobante(
                cuenta.getUsuario(), movimiento
        );
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
                this, "Comprobante de operación", true
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
                        "Recibo HTML (*.html)", "html"
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
                    archivo, html, StandardCharsets.UTF_8
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
                        + "-" + movimiento.getReferencia()
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
                    textoComprobante(
                            usuarioComprobante, movimiento
                    ),
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
                + Comprobante.generar(
                        usuarioComprobante, movimiento
                ).replace(
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
    if (operacionEnCurso || registroEnCurso) {
        mostrarError(
            "Hay un proceso en curso."
            + "\nEspera a que termine antes de cerrar."
        );
        return;
    }

    int respuesta = JOptionPane.showConfirmDialog(
        this,
        "¿Deseas cerrar Banco UVG?"
        + "\nLos datos guardados en MySQL se conservarán.",
        "Cerrar aplicación",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE
    );

    if (respuesta == JOptionPane.YES_OPTION) {
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

    private void actualizarResumenSesion() {
    if (cuentaSesion == null) {
        return;
    }

    final CuentaSesion sesionConsultada = cuentaSesion;

    SwingWorker<ResumenCuenta, Void> tarea =
            new SwingWorker<ResumenCuenta, Void>() {

        @Override
        protected ResumenCuenta doInBackground() throws Exception {
            CuentaDAO dao = new CuentaDAO();

            return dao.consultarResumen(
                sesionConsultada.numeroCuenta()
            );
        }

        @Override
        protected void done() {
            try {
                ResumenCuenta resumen = get();

                // Evita mostrar datos si cambió o terminó la sesión.
                if (cuentaSesion != sesionConsultada) {
                    return;
                }

                titular.setText(
                    sesionConsultada.nombreCompleto()
                    + " | Usuario: "
                    + sesionConsultada.nombreUsuario()
                );

                detalleCuenta.setText(
                    "Número de cuenta: "
                    + sesionConsultada.numeroCuenta()
                    + " | Saldo inicial: Q"
                    + Dinero.formatear(
                        sesionConsultada.saldoInicial()
                    )
                );

                saldo.setText(
                    "Q" + Dinero.formatear(resumen.saldo())
                );

                depositado.setText(
                    "Q" + Dinero.formatear(resumen.totalDepositos())
                );

                retirado.setText(
                    "Q" + Dinero.formatear(resumen.totalRetiros())
                );

                operaciones.setText(
                    String.valueOf(resumen.cantidadMovimientos())
                );

            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();

            } catch (java.util.concurrent.ExecutionException error) {
    Throwable causa = error.getCause();
    causa.printStackTrace();

    if (cuentaSesion == sesionConsultada) {
        mostrarError(
            "No se pudo actualizar el resumen."
            + "\nDetalle: " + causa.getMessage()
        );
    }
}
        }
    };

    tarea.execute();
}

private void cargarHistorialSesion() {
    if (cuentaSesion == null) {
        mostrarError("Primero inicia sesión.");
        paginas.setSelectedIndex(TAB_INGRESAR);
        return;
    }

    final CuentaSesion sesionConsultada = cuentaSesion;

    historialDesbloqueado = false;
    movimientosSesion.clear();
    modelo.setRowCount(0);

    titularHistorial.setText(
        "Titular: " + sesionConsultada.nombreCompleto()
        + " | Cuenta: " + sesionConsultada.numeroCuenta()
    );

    saldoHistorial.setText("Cargando movimientos...");
    tarjetasHistorial.show(panelHistorial, "datos");

    SwingWorker<java.util.List<MovimientoGuardado>, Void> tarea =
            new SwingWorker<java.util.List<MovimientoGuardado>, Void>() {

        @Override
        protected java.util.List<MovimientoGuardado> doInBackground()
                throws Exception {

            MovimientoDAO dao = new MovimientoDAO();

            return dao.listarPorCuenta(
                sesionConsultada.numeroCuenta()
            );
        }

        @Override
        protected void done() {
            // No mostrar datos de otra sesión ni de otra pantalla.
            if (cuentaSesion != sesionConsultada
                    || paginas.getSelectedIndex() != TAB_HISTORIAL) {
                return;
            }

            try {
                movimientosSesion = get();
                historialDesbloqueado = true;

                saldoHistorial.setText(
                    "Movimientos registrados: "
                    + movimientosSesion.size()
                );

                filtro.setSelectedIndex(0);
                actualizarTabla();

            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                saldoHistorial.setText("Carga interrumpida.");

            } catch (java.util.concurrent.ExecutionException error) {
                saldoHistorial.setText("No se pudo cargar el historial.");

                mostrarError(
                    "Error al consultar los movimientos: "
                    + error.getCause().getMessage()
                );
            }
        }
    };

    tarea.execute();
}

}