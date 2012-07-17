package net.kenevans.ecg.md100a.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.kenevans.core.utils.AboutBoxPanel;
import net.kenevans.core.utils.Utils;
import net.kenevans.ecg.md100a.model.EcgFileModel;
import net.kenevans.ecg.md100a.model.Header;
import net.kenevans.ecg.md100a.model.IConstants;
import net.kenevans.ecg.md100a.model.Strip;
import net.kenevans.ecg.md100a.utils.MathUtils;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * EcgStripViewer is a viewer to view ECG strips from the MD100A ECG Monitor.
 * 
 * @author Kenneth Evans, Jr.
 */
public class EcgStripViewer extends JFrame implements IConstants
{
    /**
     * Use this to determine if a file is loaded initially. Useful for
     * development. Not so good for deployment.
     */
    public static final boolean USE_START_FILE_NAME = true;
    private static final long serialVersionUID = 1L;
    public static final String LS = System.getProperty("line.separator");

    /** The frame title. */
    private static final String title = "ECG Strip Viewer";
    /** The frame width. */
    private static final int WIDTH = 1200;
    /** The frame height. */
    private static final int HEIGHT = 600;
    /** The divider location. */
    private static final int MAIN_PANE_DIVIDER_LOCATION = 2 * HEIGHT / 3;

    /** Default value for the range maximum. */
    private static final double YMAX = 160;
    /** Value for the domain maximum. */
    private static final double XMAX = 30;

    /** Keeps the last-used path for the file open dialog. */
    private String defaultPath = DEFAULT_DIR;

    /**
     * Determines how much of the 256-byte plot range is shown in the sub plot.
     * The height of the sub-plot area is 256 / DATA_SCALE_DEFAULT. Choosing
     * larger values makes the curve larger.
     */
    private static final double DATA_SCALE_DEFAULT = 1;
    /** The dataScale to use */
    private double dataScale = DATA_SCALE_DEFAULT;

    /** The color for the ECG strip. */
    private Paint stripColor = Color.RED;
    /** The color for the RSA curve. */
    private Paint rsaColor = new Color(0, 153, 255);
    /** The color for the RSA base line. */
    private Paint rsaBaseLineColor = new Color(0, 0, 255);

    /** The model for this user interface. */
    private EcgFileModel model;

    // User interface controls (Many do not need to be global)
    private Container contentPane = this.getContentPane();
    private JPanel listPanel = new JPanel();
    private DefaultListModel listModel = new DefaultListModel();
    private JList list = new JList(listModel);
    private JScrollPane listScrollPane;
    private JPanel displayPanel = new JPanel();
    private ChartPanel chartPanel;
    private JPanel mainPanel = new JPanel();
    private JSplitPane mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        displayPanel, listPanel);
    private JMenuBar menuBar;

    /** Array of Strips for the viewer. */
    private Strip[] strips;
    /** The currently selected Strip. */
    private Strip curStrip;

    /** The XYSeriesCollection used in the plot. It is filled out as needed. */
    private XYSeriesCollection dataset = new XYSeriesCollection();

    /** Used to retain the domain maximum for resetting the plot. */
    private double defaultXMax;
    /** Used to retain the range maximum for resetting the plot. */
    private double defaultYMax;
    /** Whether to show markers in the plot. */
    private boolean showMarkers = false;
    /** Whether to show RSA data in the plot. */
    private boolean showRSA = true;
    /** The default window to use for the median filter. */
    private static final int MEDIAN_FILTER_WINDOW_DEFAULT = 50;
    /** The window to use for the median filter. */
    private int medianFilterWindow = MEDIAN_FILTER_WINDOW_DEFAULT;
    /** The default cutoff to use for the Butterworth low pass filter. */
    private static final double BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT = 8;
    /** The cutoff to use for the Butterworth low pass filter. */
    private double butterworthLowPassCutoff = BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT;
    /** The number of sub-plots to use. */
    private int nSubPlots = 3;

    /** List of dataModes to handle. */
    private static final DataMode[] dataModeList = {DataMode.DEFAULT,
        DataMode.MEDIAN_SUBTRACTED, DataMode.MEDIAN, DataMode.BUTTERWORTH,
        DataMode.BUTTERWORTH_LOW_PASS,
        DataMode.MEDIAN_SUBTRACTED_BUTTERWORTH_LOW_PASS,};
    /** The DataMode to use. */
    private DataMode dataMode = DataMode.DEFAULT;

    /**
     * DataMode represents the various modes for displaying the data on the
     * plot. Each DataMode has a name for use in menus and the like and a
     * Process which defines how the original data is processed in this mode.
     */
    private static enum DataMode {
        DEFAULT("Default") {
            @Override
            double[] process(EcgStripViewer viewer, double[] data) {
                // TODO Auto-generated method stub
                return data;
            }
        },
        MEDIAN_SUBTRACTED("Median Subtracted") {
            @Override
            double[] process(EcgStripViewer viewer, double[] data) {
                double[] result = MathUtils.medianFilter(data,
                    viewer.medianFilterWindow);
                for(int i = 0; i < data.length; i++) {
                    result[i] = data[i] - result[i];
                }
                return result;
            }
        },
        MEDIAN("Median") {
            @Override
            double[] process(EcgStripViewer viewer, double[] data) {
                return MathUtils.medianFilter(data, viewer.medianFilterWindow);
            }
        },
        BUTTERWORTH("Amperor Butterworth") {
            @Override
            double[] process(EcgStripViewer viewer, double[] data) {
                return MathUtils.butterworth_6_05_75(data);
            }
        },
        BUTTERWORTH_LOW_PASS("Butterworth Low Pass") {
            @Override
            double[] process(EcgStripViewer viewer, double[] data) {
                return MathUtils.butterworthLowPass2Pole(SAMPLE_RATE,
                    viewer.butterworthLowPassCutoff, data);
            }
        },
        MEDIAN_SUBTRACTED_BUTTERWORTH_LOW_PASS(
            "Median Subtracted Butterworth Low Pass") {
            @Override
            double[] process(EcgStripViewer viewer, double[] data) {
                double[] temp = DataMode.MEDIAN_SUBTRACTED
                    .process(viewer, data);
                return MathUtils.butterworthLowPass2Pole(SAMPLE_RATE,
                    viewer.butterworthLowPassCutoff, temp);
            }
        };

        public String name;

        DataMode(String name) {
            this.name = name;
        }

        /**
         * Method that processes the data for this mode.
         * 
         * @param viewer The EcgStripViewer. Used to access instance variables.
         * @param data The input data.
         * @return
         */
        abstract double[] process(EcgStripViewer viewer, double[] data);
    };

    /**
     * EcgStripViewer constructor.
     */
    public EcgStripViewer() {
        uiInit();
    }

    /**
     * Initializes the user interface.
     */
    void uiInit() {
        this.setLayout(new BorderLayout());

        // Display panel
        displayPanel.setLayout(new BorderLayout());
        displayPanel.setPreferredSize(new Dimension(WIDTH, HEIGHT / 2));
        createChart();
        chartPanel.setPreferredSize(new Dimension(600, 270));
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(true);
        javax.swing.border.CompoundBorder compoundborder = BorderFactory
            .createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createEtchedBorder());
        chartPanel.setBorder(compoundborder);
        displayPanel.add(chartPanel);

        // List panel
        listScrollPane = new JScrollPane(list);
        listPanel.setLayout(new BorderLayout());
        listPanel.add(listScrollPane, BorderLayout.CENTER);
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent ev) {
                // Internal implementation
                onListItemSelected(ev);
            }
        });

        list.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
                JLabel label = (JLabel)super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);
                // Use the date from the strip as the label
                Strip strip = (Strip)value;
                label.setText((index + 1) + " " + strip.getStringDate() + " "
                    + strip.getStringTime(false) + " " + strip.getHeartRate()
                    + " bpm" + " " + strip.getDiagnosisString());
                return label;
            }
        });

        // Main split pane
        mainPane.setContinuousLayout(true);
        mainPane.setDividerLocation(MAIN_PANE_DIVIDER_LOCATION);
        if(false) {
            mainPane.setOneTouchExpandable(true);
        }

        // Main panel
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(mainPane, BorderLayout.CENTER);

        // Content pane
        // For the drag behavior to work correctly, the tool bar must be in a
        // container that uses the BorderLayout layout manager. The component
        // that
        // the tool bar affects is generally in the center of the container. The
        // tool bar must be the only other component in the container, and it
        // must
        // not be in the center.
        contentPane.setLayout(new BorderLayout());
        contentPane.add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Initializes the menus.
     */
    private void initMenus() {
        // Menu
        menuBar = new JMenuBar();

        // File
        JMenu menu = new JMenu();
        menu.setText("File");
        menuBar.add(menu);

        // File Open
        JMenuItem menuItem = new JMenuItem();
        menuItem.setText("Open...");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                open();
            }
        });
        menu.add(menuItem);

        // File Save As
        menuItem = new JMenuItem();
        menuItem.setText("Save As...");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                saveAs();
            }
        });
        menu.add(menuItem);

        JSeparator separator = new JSeparator();
        menu.add(separator);

        // File Exit
        menuItem = new JMenuItem();
        menuItem.setText("Exit");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                quit();
            }
        });
        menu.add(menuItem);

        // Tools
        menu = new JMenu();
        menu.setText("Tools");
        menuBar.add(menu);

        menuItem = new JMenuItem();
        menuItem.setText("Patient Info...");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showInfo();
            }
        });
        menu.add(menuItem);

        // Help
        menu = new JMenu();
        menu.setText("Help");
        menuBar.add(menu);

        menuItem = new JMenuItem();
        menuItem.setText("About");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JOptionPane.showMessageDialog(null, new AboutBoxPanel(title,
                    "Written by Kenneth Evans, Jr.", "kenevans.net",
                    "Copyright (c) 2012 Kenneth Evans"), "About",
                    JOptionPane.PLAIN_MESSAGE);
            }
        });
        menu.add(menuItem);
    }

    /**
     * Puts the panel in a JFrame and runs the JFrame.
     */
    public void run() {
        try {
            // Create and set up the window.
            // JFrame.setDefaultLookAndFeelDecorated(true);
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            this.setTitle(title);
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            // frame.setLocationRelativeTo(null);

            // Has to be done here. The menus are not part of the JPanel.
            initMenus();
            this.setJMenuBar(menuBar);

            // Display the window
            this.setBounds(20, 20, WIDTH, HEIGHT);
            this.setVisible(true);
            if(USE_START_FILE_NAME) {
                File file = new File(FILE_PATH);
                if(file.exists()) {
                    loadFile(file);
                }
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Brings up a JFileChooser to open a file.
     */
    private void open() {
        JFileChooser chooser = new JFileChooser();
        if(defaultPath != null) {
            chooser.setCurrentDirectory(new File(defaultPath));
        }
        int result = chooser.showOpenDialog(this);
        if(result == JFileChooser.APPROVE_OPTION) {
            // Save the selected path for next time
            defaultPath = chooser.getSelectedFile().getParentFile().getPath();
            // Process the file
            File file = chooser.getSelectedFile();
            // Set the cursor in case it takes a long time
            // This isn't working. The cursor apparently doesn't get set
            // until after it is done.
            Cursor oldCursor = getCursor();
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                loadFile(file);
            } finally {
                setCursor(oldCursor);
            }
        }
    }

    /**
     * Brings up a JFileChooser to save a file.
     */
    private void saveAs() {
        JFileChooser chooser = new JFileChooser();
        if(defaultPath != null) {
            chooser.setCurrentDirectory(new File(defaultPath));
        }
        int result = chooser.showOpenDialog(this);
        if(result == JFileChooser.APPROVE_OPTION) {
            // Save the selected path for next time
            defaultPath = chooser.getSelectedFile().getParentFile().getPath();
            // Process the file
            File file = chooser.getSelectedFile();
            // Set the cursor in case it takes a long time
            // This isn't working. The cursor apparently doesn't get set
            // until after it is done.
            Cursor oldCursor = getCursor();
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                saveFile(file);
            } finally {
                setCursor(oldCursor);
            }
        }
    }

    /**
     * Loads a new file.
     * 
     * @param fileName
     */
    private void loadFile(final File file) {
        // this.file = file;
        if(file == null) {
            Utils.errMsg("File is null");
            return;
        }

        // Needs to be done this way to allow the text to change before reading
        // the image.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    model = new EcgFileModel(file.getPath());
                    strips = model.getStrips();

                    // DEBUG
                    // System.out.println("First Shorts in Strip");
                    // for(Strip strip : strips) {
                    // strip.printFirstShorts(14);
                    // }
                    // System.out.println();

                    // DEBUG
                    // System.out.println("Control Points in Strip");
                    // for(Strip strip : strips) {
                    // strip.printControlPoints();
                    // }
                    // System.out.println();

                    populateList();
                    if(strips.length > 0) {
                        list.setSelectedValue(strips[0], false);
                    }

                    // Show the selected info
                    setTitle(file.getName());
                } catch(Exception ex) {
                    String msg = "Error loading file: " + file.getPath();
                    Utils.excMsg(msg, ex);
                } catch(Error err) {
                    String msg = "Error loading file: " + file.getPath();
                    Utils.excMsg(msg, err);
                }
            }
        });
    }

    /**
     * Loads a new file.
     * 
     * @param fileName
     */
    private void saveFile(final File file) {
        // this.file = file;
        if(file == null) {
            Utils.errMsg("File is null");
            return;
        }
        try {
            int nStrips = 1;
            int totalSize = HEADER_LENGTH + nStrips * STRIP_LENGTH;
            byte[] data = new byte[totalSize];
            Header header = model.getHeader().clone();
            header.setNStrips(nStrips);
            header.setId("2");
            byte[] headerData = header.getData();
            byte[] stripData;
            int start = 0;
            int end = HEADER_LENGTH;
            for(int i = 0; i < end; i++) {
                data[i] = headerData[i];
            }
            for(int n = 0; n < nStrips; n++) {
                stripData = model.getStrips()[n].getData();
                start += end;
                end = STRIP_LENGTH;
                for(int i = 0; i < end; i++) {
                    data[start + i] = stripData[i];
                }
            }
            EcgFileModel.saveFile(file, data);
        } catch(Exception ex) {
            Utils.excMsg("Error saving file:" + LS + file.getPath(), ex);
            ex.printStackTrace();
        }
    }

    /**
     * Populates the list from the list of profiles.
     */
    private void populateList() {
        list.setEnabled(false);
        listModel.removeAllElements();
        for(Strip strip : strips) {
            listModel.addElement(strip);
        }
        list.validate();
        mainPane.validate();
        list.setEnabled(true);
    }

    /**
     * Creates the JFreeChart and ChartPanel. Sets the XYDataSet in it but does
     * nothing with it otherwise.
     * 
     * @return The chart created.
     */
    private JFreeChart createChart() {
        // Generate the graph
        JFreeChart chart = ChartFactory.createXYLineChart(
            "30-sec ECG Measurement", "sec (25 mm / sec)", "mV (10 mm / mV)",
            null, PlotOrientation.VERTICAL, // BasicImagePlot
            // Orientation
            false, // Show Legend
            false, // Use tooltips
            false // Configure chart to generate URLs?
            );
        // Change the axis limits
        chart.getXYPlot().getRangeAxis().setRange(-YMAX, YMAX);
        chart.getXYPlot().getRangeAxis().setTickLabelsVisible(false);
        chart.getXYPlot().getDomainAxis().setRange(0, XMAX);
        XYPlot plot = chart.getXYPlot();
        // Set the dataset. We will mostly deal with the dataset later.
        plot.setDataset(dataset);

        // Define the chartPanel before extending the popup menu
        chartPanel = new ChartPanel(chart);

        // Add to the popup menu
        extendPopupMenu();

        return chart;
    }

    /**
     * Adds to the plot pop-up menu.
     */
    private void extendPopupMenu() {
        JPopupMenu menu = chartPanel.getPopupMenu();
        if(menu == null) return;

        JSeparator separator = new JSeparator();
        menu.add(separator);

        JMenuItem item = new JMenuItem();
        item.setText("Toggle RSA");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showRSA = !showRSA;
                clearPlot();
                addStripToChart(curStrip);
            }
        });
        menu.add(item);

        JMenu menu1 = new JMenu("Data Plotted");
        menu.add(menu1);

        ButtonGroup bgroup = new ButtonGroup();

        // Loop over the dataModes
        JRadioButtonMenuItem radioButtonItem;
        for(final DataMode mode : dataModeList) {
            radioButtonItem = new JRadioButtonMenuItem();
            radioButtonItem.setText(mode.name);
            radioButtonItem.setSelected(dataMode == mode);
            radioButtonItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    dataMode = mode;
                    if(curStrip != null) {
                        clearPlot();
                        addStripToChart(curStrip);
                    }
                }
            });
            menu1.add(radioButtonItem);
            bgroup.add(radioButtonItem);
        }

        menu1 = new JMenu("Settings");
        menu.add(menu1);

        item = new JMenuItem();
        item.setText("Data Scale");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                double oldDataScale = dataScale;
                String value = (String)JOptionPane.showInputDialog(null,
                    "Enter the data scale factor (non-negative floating point value)"
                        + LS + "or -1 to restore the default",
                    "Data Scale Factor", JOptionPane.PLAIN_MESSAGE, null, null,
                    dataScale);
                if(value == null) {
                    return;
                }
                try {
                    dataScale = Double.parseDouble(value);
                } catch(NumberFormatException ex) {
                    Utils.errMsg("Invalid value for the data scale factor,"
                        + " using default (" + DATA_SCALE_DEFAULT + ")");
                    dataScale = DATA_SCALE_DEFAULT;
                }
                if(dataScale < 0) {
                    Utils.errMsg("Invalid value for the data scale factor,"
                        + " using default (" + DATA_SCALE_DEFAULT + ")");
                    dataScale = DATA_SCALE_DEFAULT;
                }
                // Redraw if it has changed
                if(dataScale != oldDataScale) {
                    clearPlot();
                    addStripToChart(curStrip);
                }
            }
        });
        menu1.add(item);

        item = new JMenuItem();
        item.setText("Number of Sub-Plots");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int oldNSubPlots = nSubPlots;
                String value = (String)JOptionPane.showInputDialog(null,
                    "Enter the number of sub-plots desired", "Sub-Plots",
                    JOptionPane.PLAIN_MESSAGE, null, new String[] {"1", "2",
                        "3"}, String.valueOf(nSubPlots));
                if(value == null) {
                    return;
                }
                try {
                    nSubPlots = Integer.parseInt(value);
                } catch(NumberFormatException ex) {
                    Utils.errMsg("Invalid value for the number of sub-plots,"
                        + " using default (3)");
                    nSubPlots = 3;
                }
                if(nSubPlots < 0) {
                    Utils.errMsg("Invalid value for the number of sub-plots,"
                        + " using default (3)");
                    nSubPlots = 3;
                }
                // Redraw if we are showing one of those modes
                if(nSubPlots != oldNSubPlots) {
                    clearPlot();
                    addStripToChart(curStrip);
                }
            }
        });
        menu1.add(item);

        item = new JMenuItem();
        item.setText("Median Filter Window");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String value = (String)JOptionPane.showInputDialog(null,
                    "Enter window (non-negative integer)" + LS
                        + "or -1 to restore the default",
                    "Median Filter Window", JOptionPane.PLAIN_MESSAGE, null,
                    null, medianFilterWindow);
                if(value == null) {
                    return;
                }
                try {
                    medianFilterWindow = Integer.parseInt(value);
                } catch(NumberFormatException ex) {
                    Utils.errMsg("Invalid value for the Median Filter Window,"
                        + " using default (" + MEDIAN_FILTER_WINDOW_DEFAULT
                        + ")");
                    medianFilterWindow = MEDIAN_FILTER_WINDOW_DEFAULT;
                }
                if(medianFilterWindow < 0) {
                    Utils.errMsg("Invalid value for the Median Filter Window,"
                        + " using default (" + MEDIAN_FILTER_WINDOW_DEFAULT
                        + ")");
                    medianFilterWindow = MEDIAN_FILTER_WINDOW_DEFAULT;
                }
                // Redraw if we are showing one of those modes
                if(dataMode == DataMode.MEDIAN
                    || dataMode == DataMode.MEDIAN_SUBTRACTED) {
                    clearPlot();
                    addStripToChart(curStrip);
                }
            }
        });
        menu1.add(item);

        item = new JMenuItem();
        item.setText("Butterworth Low Pass Cutoff");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String value = (String)JOptionPane.showInputDialog(null,
                    "Enter cutoff (non-negative floating point value)" + LS
                        + "or -1 to restore the default",
                    "Butterworth Low Pass Cutoff", JOptionPane.PLAIN_MESSAGE,
                    null, null, butterworthLowPassCutoff);
                if(value == null) {
                    return;
                }
                try {
                    butterworthLowPassCutoff = Double.parseDouble(value);
                } catch(NumberFormatException ex) {
                    Utils
                        .errMsg("Invalid value for the Butterworth Low Pass Cutoff,"
                            + " using default ("
                            + BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT + ")");
                    butterworthLowPassCutoff = BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT;
                }
                if(medianFilterWindow < 0) {
                    Utils
                        .errMsg("Invalid value for the Butterworth Low Pass Cutoff,"
                            + " using default ("
                            + BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT + ")");
                    butterworthLowPassCutoff = BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT;
                }
                // Redraw if we are showing one of those modes
                if(dataMode == DataMode.BUTTERWORTH_LOW_PASS) {
                    clearPlot();
                    addStripToChart(curStrip);
                }
            }
        });
        menu1.add(item);

        item = new JMenuItem();
        item.setText("Reset");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFreeChart chart = chartPanel.getChart();
                chart.getXYPlot().getRangeAxis().setRange(0, defaultYMax);
                chart.getXYPlot().getDomainAxis().setRange(0, defaultXMax);
                if(showMarkers) {
                    showMarkers = false;
                    // Get the renderer
                    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)chartPanel
                        .getChart().getXYPlot().getRenderer();
                    // Change for the first 3 series
                    for(int i = 0; i < 3; i++) {
                        renderer.setSeriesShapesVisible(i, showMarkers);
                    }
                }
            }
        });
        menu.add(item);

        item = new JMenuItem();
        item.setText("Reset Axes");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFreeChart chart = chartPanel.getChart();
                chart.getXYPlot().getRangeAxis().setRange(0, defaultYMax);
                chart.getXYPlot().getDomainAxis().setRange(0, defaultXMax);
            }
        });
        menu.add(item);

        item = new JMenuItem();
        item.setText("Toggle Markers");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showMarkers = !showMarkers;
                // Get the renderer
                XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)chartPanel
                    .getChart().getXYPlot().getRenderer();
                // Change for the first 3 series
                for(int i = 0; i < 3; i++) {
                    renderer.setSeriesShapesVisible(i, showMarkers);
                }
            }
        });
        menu.add(item);
    }

    /**
     * Removes all series from the plot.
     */
    private void clearPlot() {
        try {
            dataset.removeAllSeries();
        } catch(Exception ex) {
            Utils.excMsg("Error clearing plot", ex);
        }
    }

    /**
     * Fills in the chart with the data from the given strip.
     * 
     * @param strip
     */
    // TODO
    private void addStripToChart(Strip strip) {
        double[] stripData, data;
        try {
            stripData = strip.getDataAsBytes();
            int nDataPoints = stripData.length;
            // Work with a copy of the data so we don't modify the original
            data = new double[nDataPoints];
            for(int i = 0; i < nDataPoints; i++) {
                data[i] = stripData[i];
            }
            // Process according to the dataMode
            data = dataMode.process(this, data);

            // DEBUG
            boolean debug = false;
            if(debug) {
                // Find the max and min of the data
                double max = -Double.MAX_VALUE;
                double min = Double.MAX_VALUE;
                int maxIndex = -1;
                int minIndex = -1;
                for(int i = 0; i < nDataPoints; i++) {
                    if(data[i] > max) {
                        maxIndex = i;
                        max = data[i];
                    }
                    if(data[i] < min) {
                        minIndex = i;
                        min = data[i];
                    }
                }
                System.out.println();
                System.out.println("addStripToChart: min=" + min + " @ "
                    + minIndex + " max=" + max + " @ " + maxIndex);
            }

            // Determine the 3 plot areas
            double scale = nSubPlots * dataScale;
            double dataHeight = 1024;
            double totalHeight = nSubPlots * dataHeight;
            totalHeight /= scale;

            // Set the axis limits
            double xMax = XMAX / nSubPlots;

            // Use for resetting the plot, otherwise it will auto-scale
            defaultXMax = xMax;
            defaultYMax = totalHeight;

            // Generate the x values
            // TODO Would need 7501 values to have i = 0 correspond to 0 sec
            // and i= nDataPoints-1 to correspond to 30 sec
            // This gives an error of .004 (or less)
            int nPoints = nDataPoints / nSubPlots;
            double[] xVals = new double[nPoints];
            for(int n = 0; n < nPoints; n++) {
                xVals[n] = xMax * n / (nPoints - 1);
            }

            // Set the axis limits in the plot
            JFreeChart chart = chartPanel.getChart();
            chart.getXYPlot().getRangeAxis().setRange(0, totalHeight);
            chart.getXYPlot().getDomainAxis().setRange(0, xMax);

            // Hard-coded values
            boolean doRSABaseLines = true;
            boolean doBaseLines = true;

            // Plot the data
            plot("Segment", stripColor, nSubPlots, totalHeight, .5, xVals, data);

            // Add RSA values
            if(showRSA) {
                if(strip.getPeakIndices().length > 2) {
                    // Get the peak index values
                    double[] rsaVals = strip.getPeakIndicesArray();
                    // Scale them
                    double subPlotHeight = .25 * totalHeight / nSubPlots;
                    double secMax = .1;
                    for(int i = 0; i < rsaVals.length; i++) {
                        // Scale
                        rsaVals[i] *= subPlotHeight / secMax;
                        // lastPeakVal = 0;
                        // lastPeakVal = nextPeak;
                    }

                    plot("RSA", rsaColor, nSubPlots, totalHeight, .25, xVals,
                        rsaVals);

                    // Create some RSA zero lines
                    if(doRSABaseLines) {
                        plot("RSA Base Line", rsaBaseLineColor, nSubPlots,
                            totalHeight, .25, xVals, new double[] {0});
                    }
                }
            }

            // Create some zero lines
            if(doBaseLines) {
                plot("Base Line", Color.BLACK, nSubPlots, totalHeight, .5,
                    xVals, new double[] {0});
            }

            // Create some test lines
            // DEBUG
            if(debug) {
                plot("Base Line", Color.GREEN, nSubPlots, totalHeight, .5,
                    xVals, new double[] {25});
            }

        } catch(Exception ex) {
            Utils.excMsg("Error adding profile to plot", ex);
            ex.printStackTrace();
        }
    }

    /**
     * General routine to add one or more series to a plot.
     * 
     * @param seriesName The series name will be this value with the sub-plot
     *            number appended.
     * @param paint A Paint representing the color of the series.
     * @param nSubPlots The number of sub-plots to use.
     * @param totalHeight The total height of the chart. Each sub-plot height
     *            will be this value divided by nSubPlots.
     * @param originFraction What fraction of the sub-plot area to use as the
     *            origin. Measured from the bottom. .5 is the middle, and .2 is
     *            below the middle. The useful range is 0 to 1.
     * @param xVals The array of x values.
     * @param yVals The array of y values. If the length of this array is 1, it
     *            is used as a constant value for all x values. Otherwise it
     *            should be nSeries times as long as the length of the x values.
     *            If it is shorter, then null will be used for the remaining
     *            plot values.
     */
    private void plot(String seriesName, Paint paint, int nSubPlots,
        double totalHeight, double originFraction, double[] xVals,
        double[] yVals) {
        int index;
        int nPoints = xVals.length;
        int nDataPoints = yVals.length;
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)chartPanel
            .getChart().getXYPlot().getRenderer();
        double offset;
        for(int i = 0; i < nSubPlots; i++) {
            XYSeries series = new XYSeries(seriesName + " " + (i + 1));
            offset = totalHeight - (i + 1 - originFraction) * totalHeight
                / nSubPlots;
            for(int n = 0; n < nPoints; n++) {
                if(nDataPoints == 1) {
                    series.add(xVals[n], yVals[0] + offset);
                } else {
                    index = i * nPoints + n;
                    // In case yVals does not fill the segment
                    if(index > nDataPoints - 1) {
                        series.add(xVals[n], null);
                    } else {
                        series.add(xVals[n], yVals[index] + offset);
                    }
                }
            }
            dataset.addSeries(series);
            renderer.setSeriesPaint(dataset.indexOf(series), paint);
        }
    }

    /**
     * Handler for the list. Toggles the checked state.
     * 
     * @param ev
     */
    private void onListItemSelected(ListSelectionEvent ev) {
        if(ev.getValueIsAdjusting()) return;
        Strip strip = (Strip)list.getSelectedValue();
        if(strip == null) {
            return;
        }
        if(strip == null) return;
        list.clearSelection();
        clearPlot();
        addStripToChart(strip);
        curStrip = strip;
    }

    /**
     * Shows patient information
     */
    private void showInfo() {
        if(model != null && model.getHeader() != null) {
            String info = model.getHeader().getInfo();
            JOptionPane.showMessageDialog(null, info, "Patient Information",
                JOptionPane.PLAIN_MESSAGE);
        }
    }

    /**
     * Quits the application
     */
    private void quit() {
        System.exit(0);
    }

    /**
     * Main method.
     * 
     * @param args
     */
    public static void main(String[] args) {
        final EcgStripViewer app = new EcgStripViewer();

        // Make the job run in the AWT thread
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if(app != null) {
                    app.run();
                }
            }
        });
    }
}
