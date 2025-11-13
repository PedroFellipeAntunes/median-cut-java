package Windows;

import Data.ConfigData;
import Data.Models.OperationEnum;
import MedianCut.Operations;

import javax.swing.*;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * DropDownWindow
 *
 * - config is created inline with defaults here (source of truth).
 * - components are initialized using config values directly (no existence checks).
 * - OperationOrderManager will use config as source.
 */
public class DropDownWindow {
    // Interface components
    private JFrame frame;
    private JLabel dropLabel;

    // Operation + three order combos
    private JComboBox<OperationEnum> operationCombo;
    private JComboBox<Enum<?>> firstCombo;
    private JComboBox<Enum<?>> secondCombo;
    private JComboBox<Enum<?>> thirdCombo;

    // Buckets slider + text field
    private final int MIN_SLIDER = 1, MAX_SLIDER = 256;
    private JSlider bucketsSlider;
    private JTextField bucketsField;

    // Initial states
    private final ConfigData config = new ConfigData(
        OperationEnum.GRAYSCALE,
        new Enum<?>[]{ null, null, null },
        16
    );

    private boolean loading = false;
    private final Font defaultFont = UIManager.getDefaults().getFont("Label.font");

    public DropDownWindow() {
        initFrame();
        initDropLabel();
        initTypeAndOpTypeComboBoxes();
        initSlidersAndControls();
        finalizeFrame();
    }

    private void initFrame() {
        frame = new JFrame("Median Cut");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
    }

    private void initDropLabel() {
        dropLabel = new JLabel("Drop IMAGE files here", SwingConstants.CENTER);
        dropLabel.setPreferredSize(new Dimension(300, 200));
        dropLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        dropLabel.setForeground(Color.WHITE);
        dropLabel.setOpaque(true);
        dropLabel.setBackground(Color.BLACK);
        dropLabel.setTransferHandler(createTransferHandler());

        frame.add(dropLabel, BorderLayout.CENTER);
    }

    private TransferHandler createTransferHandler() {
        return new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return !loading && support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) support.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);

                    for (File file : files) {
                        String name = file.getName().toLowerCase();

                        if (!name.endsWith(".png") && !name.endsWith(".jpg") && !name.endsWith(".jpeg")) {
                            showError("Incorrect image format, use: png, jpg or jpeg");
                            return false;
                        }
                    }

                    processFiles(files);

                    return true;
                } catch (UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        };
    }

    private void setLoadingState(boolean state) {
        loading = state;

        toggleControls(!state);

        frame.repaint();
    }

    private void toggleControls(boolean enabled) {
        // minimal: no null checks; if components missing JVM will throw
        operationCombo.setEnabled(enabled);
        firstCombo.setEnabled(enabled && firstCombo.getItemCount() > 0);
        secondCombo.setEnabled(enabled && secondCombo.getItemCount() > 0);
        thirdCombo.setEnabled(enabled && thirdCombo.getItemCount() > 0);

        bucketsSlider.setEnabled(enabled);
        bucketsField.setEnabled(enabled);
    }

    private void processFiles(List<File> files) {
        final int total = files.size();
        dropLabel.setText("LOADING (1/" + total + ")");

        setLoadingState(true);

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws InterruptedException, InvocationTargetException {
                Operations op;

                try {
                    op = new Operations(config);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    
                    SwingUtilities.invokeAndWait(() -> {
                        showError("Configuration error: " + ex.getMessage());
                    });
                    
                    return null;
                }

                for (int i = 0; i < total; i++) {
                    final File file = files.get(i);
                    final int num = i + 1;

                    try {
                        op.startProcess(file.getPath());
                    } catch (Exception ex) {
                        ex.printStackTrace();

                        SwingUtilities.invokeAndWait(() -> {
                            showError("Error processing file (" + num + "/" + total + "): " + file.getName());
                        });

                        break;
                    }

                    // If the user chose to skip displaying and not save, stop processing further files
                    if (op.save == false && op.skip == true) {
                        break;
                    }

                    publish(i + 2);
                }

                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int done = chunks.get(chunks.size() - 1);
                dropLabel.setText("LOADING (" + done + "/" + total + ")");
            }

            @Override
            protected void done() {
                onProcessingComplete();
            }
        };

        worker.execute();
    }

    private void onProcessingComplete() {
        dropLabel.setText("Images Generated");

        Timer resetTimer = new Timer(1000, e -> {
            dropLabel.setText("Drop IMAGE files here");
            setLoadingState(false);
        });

        resetTimer.setRepeats(false);
        resetTimer.start();
    }

    private void initTypeAndOpTypeComboBoxes() {
        operationCombo = new JComboBox<>(OperationEnum.values());

        // always use config.operation (no existence checks)
        operationCombo.setSelectedItem(config.operation);

        operationCombo.setBackground(Color.BLACK);
        operationCombo.setForeground(Color.WHITE);
        operationCombo.setBorder(BorderFactory.createLineBorder(Color.WHITE));

        operationCombo.addActionListener(e -> {
            if (!loading) {
                config.operation = (OperationEnum) operationCombo.getSelectedItem();
                // OperationOrderManager will handle updating order combos via its listener
            }
        });

        customizeComboBoxUI(operationCombo);

        // Order ComboBoxes (first/second/third)
        firstCombo = new JComboBox<>();
        secondCombo = new JComboBox<>();
        thirdCombo = new JComboBox<>();

        customizeComboBoxUI(firstCombo);
        customizeComboBoxUI(secondCombo);
        customizeComboBoxUI(thirdCombo);

        // Panel to hold operation on TOP and orders container BOTTOM
        JPanel comboPanel = new JPanel();
        comboPanel.setBackground(Color.BLACK);
        comboPanel.setLayout(new BorderLayout(0, 8)); // vertical gap

        // TOP: operation
        JPanel topOpPanel = new JPanel(new BorderLayout());
        topOpPanel.setBackground(Color.BLACK);
        topOpPanel.add(operationCombo, BorderLayout.CENTER);

        // BOTTOM: three labeled subpanels horizontally
        JPanel ordersContainer = new JPanel(new GridLayout(1, 3, 6, 0));
        ordersContainer.setBackground(Color.BLACK);
        ordersContainer.add(makeLabeledComboPanel("First", firstCombo));
        ordersContainer.add(makeLabeledComboPanel("Second", secondCombo));
        ordersContainer.add(makeLabeledComboPanel("Third", thirdCombo));

        comboPanel.add(topOpPanel, BorderLayout.NORTH);
        comboPanel.add(ordersContainer, BorderLayout.CENTER);

        frame.add(comboPanel, BorderLayout.NORTH);

        new OperationOrderManager(operationCombo, firstCombo, secondCombo, thirdCombo, config);
    }

    private JPanel makeLabeledComboPanel(String labelText, JComboBox<Enum<?>> combo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel label = new JLabel(labelText, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        combo.setPreferredSize(new Dimension(160, 28));
        combo.setBackground(Color.BLACK);
        combo.setForeground(Color.WHITE);
        combo.setBorder(BorderFactory.createLineBorder(Color.WHITE));

        panel.add(label, BorderLayout.NORTH);
        panel.add(combo, BorderLayout.CENTER);

        return panel;
    }

    private void initSlidersAndControls() {
        // ----- Buckets Slider (1..256) -----
        bucketsSlider = new JSlider(JSlider.HORIZONTAL, MIN_SLIDER, MAX_SLIDER, config.buckets);
        bucketsSlider.setMajorTickSpacing(MAX_SLIDER / 5);
        bucketsSlider.setMinorTickSpacing(MAX_SLIDER / 15);
        bucketsSlider.setPaintTicks(true);
        bucketsSlider.setPaintLabels(true);
        bucketsSlider.setBackground(Color.BLACK);
        bucketsSlider.setForeground(Color.WHITE);

        // field initialized directly from config.buckets (no checks)
        bucketsField = new JTextField(String.valueOf(config.buckets));
        bucketsField.setForeground(Color.WHITE);
        bucketsField.setBackground(Color.BLACK);
        bucketsField.setFont(defaultFont);
        bucketsField.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        bucketsField.setPreferredSize(new Dimension(60, 20));
        bucketsField.setHorizontalAlignment(JTextField.CENTER);

        bucketsSlider.addChangeListener(e -> {
            if (!loading) {
                int v = bucketsSlider.getValue();
                bucketsField.setText(String.valueOf(v));
                config.buckets = v;
            }
        });

        bucketsField.addActionListener(e -> {
            if (!loading) {
                parseAndSetBucketsFromField();
                bucketsField.transferFocus();
            }
        });

        bucketsField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                parseAndSetBucketsFromField();
            }
        });

        JLabel bucketsLabel = new JLabel("Buckets (1 - 256)", SwingConstants.LEFT);
        bucketsLabel.setForeground(Color.WHITE);
        bucketsLabel.setBackground(Color.BLACK);
        bucketsLabel.setOpaque(true);

        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.setBackground(Color.BLACK);
        sliderPanel.add(bucketsLabel, BorderLayout.NORTH);

        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(Color.BLACK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // slider: weightx 0.85
        gbc.gridx = 0;
        gbc.weightx = 0.85;
        gbc.gridwidth = 1;
        row.add(bucketsSlider, gbc);

        // input: weightx 0.15
        gbc.gridx = 1;
        gbc.weightx = 0.15;
        gbc.gridwidth = 1;
        row.add(bucketsField, gbc);

        sliderPanel.add(row, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        controlPanel.setBackground(Color.BLACK);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        controlPanel.add(sliderPanel);

        frame.add(controlPanel, BorderLayout.SOUTH);
    }

    private void parseAndSetBucketsFromField() {
        String text = bucketsField.getText().trim();
        if (text.isEmpty()) {
            bucketsField.setText(String.valueOf(bucketsSlider.getValue()));
            return;
        }

        try {
            int value = Integer.parseInt(text);
            value = Math.max(1, Math.min(256, value));
            bucketsSlider.setValue(value);
            bucketsField.setText(String.valueOf(value));
            config.buckets = value;
        } catch (NumberFormatException ex) {
            bucketsField.setText(String.valueOf(bucketsSlider.getValue()));
        }
    }

    private void customizeComboBoxUI(JComboBox<?> comboBox) {
        comboBox.setUI(new BasicComboBoxUI() {
            @Override
            protected ComboBoxEditor createEditor() {
                ComboBoxEditor editor = super.createEditor();
                Component editorComponent = editor.getEditorComponent();
                editorComponent.setBackground(Color.BLACK);
                editorComponent.setForeground(Color.WHITE);

                return editor;
            }

            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup popup = (BasicComboPopup) super.createPopup();
                popup.getList().setBackground(Color.BLACK);
                popup.getList().setForeground(Color.WHITE);
                popup.getList().setSelectionBackground(Color.WHITE);
                popup.getList().setSelectionForeground(Color.BLACK);
                popup.setBorder(BorderFactory.createLineBorder(Color.WHITE));

                JScrollPane scrollPane = (JScrollPane) popup.getComponent(0);
                JScrollBar bar = scrollPane.getVerticalScrollBar();
                bar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.WHITE));

                bar.setUI(new BasicScrollBarUI() {
                    @Override
                    protected JButton createDecreaseButton(int orientation) {
                        return createArrowButton(SwingConstants.NORTH);
                    }

                    @Override
                    protected JButton createIncreaseButton(int orientation) {
                        return createArrowButton(SwingConstants.SOUTH);
                    }

                    @Override
                    protected void configureScrollBarColors() {
                        this.thumbColor = Color.WHITE;
                        this.trackColor = Color.BLACK;
                    }
                });

                return popup;
            }

            @Override
            protected JButton createArrowButton() {
                BasicArrowButton arrow = new BasicArrowButton(
                        SwingConstants.SOUTH,
                        Color.BLACK,
                        Color.WHITE,
                        Color.WHITE,
                        Color.BLACK
                );

                arrow.setBorder(BorderFactory.createEmptyBorder());
                return arrow;
            }

            protected JButton createArrowButton(int direction) {
                BasicArrowButton arrow = new BasicArrowButton(
                        direction,
                        Color.BLACK,
                        Color.WHITE,
                        Color.WHITE,
                        Color.BLACK
                );

                arrow.setBorder(BorderFactory.createEmptyBorder());
                return arrow;
            }
        });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void finalizeFrame() {
        frame.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int xPos = (screenSize.width - frame.getWidth()) / 2;
        int yPos = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(xPos, yPos);
        frame.setVisible(true);
    }
}