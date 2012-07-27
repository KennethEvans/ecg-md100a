package net.kenevans.ecg.md100a.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
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
import javax.swing.JTextArea;
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
    private static final int HEIGHT = 825;
    /** The divider location for the main split pane. */
    private static final int MAIN_PANE_DIVIDER_LOCATION = 5 * HEIGHT / 8;
    /** The divider location for the lower split pane. */
    private static final int LOWER_PANE_DIVIDER_LOCATION = WIDTH / 2;

    /** Default value for the range maximum. */
    private static final double YMAX = 160;
    /** Value for the domain maximum. */
    private static final double XMAX = 30;

    /** Keeps the last-used path for the file open dialog. */
    public String defaultOpenPath = DEFAULT_DIR;
    /** Keeps the last-used path for the file save dialog. */
    public String defaultSavePath = DEFAULT_DIR;

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
    private JPanel lowerPanel = new JPanel();
    private DefaultListModel listModel = new DefaultListModel();
    private JList list = new JList(listModel);
    private JScrollPane listScrollPane;
    private JTextArea beatTextArea;
    private JPanel displayPanel = new JPanel();
    private ChartPanel chartPanel;
    private JPanel mainPanel = new JPanel();
    private JSplitPane mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        displayPanel, lowerPanel);
    private JMenuBar menuBar;

    /** Array of Strips for the viewer. */
    public Strip[] strips;
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
    /** The fraction to use in determining the average baseline for RSA values. */
    private double RSA_AVG_OUTLIER_FRACTION = .2;
    /** Whether to get the RSA values from the default or the current data mode */
    private boolean useDefaultAsRsaSource = true;
    /** The default window to use for the median filter. */
    private static final int MEDIAN_FILTER_WINDOW_DEFAULT = 50;
    /** The window to use for the median filter. */
    private int medianFilterWindow = MEDIAN_FILTER_WINDOW_DEFAULT;
    /** The default cutoff to use for the Butterworth low pass filter. */
    private static final double BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT = 8;
    /** The cutoff to use for the Butterworth low pass filter. */
    private double butterworthLowPassCutoff = BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT;
    /** The number of sub-plots to use. */
    private int nSubPlots = 1;

    /** List of dataModes to handle. */
    public static final DataMode[] dataModeList = {DataMode.DEFAULT,
        DataMode.MEDIAN_SUBTRACTED, DataMode.MEDIAN, DataMode.BUTTERWORTH,
        DataMode.BUTTERWORTH_LOW_PASS,
        DataMode.MEDIAN_SUBTRACTED_BUTTERWORTH_LOW_PASS,};
    /** The DataMode to use. */
    public DataMode dataMode = DataMode.DEFAULT;

    /**
     * DataMode represents the various modes for displaying the data on the
     * plot. Each DataMode has a name for use in menus and the like and a
     * Process which defines how the original data is processed in this mode.
     */
    public static enum DataMode {
        DEFAULT("Default") {
            @Override
            double[] process(EcgStripViewer viewer, double[] data) {
                double[] result = new double[data.length];
                for(int i = 0; i < result.length; i++) {
                    result[i] = data[i];
                }
                return result;
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
            "Median Subtracted Butterworth Low Pass Scaled") {
            @Override
            double[] process(EcgStripViewer viewer, double[] data) {
                // Hard-coded. Get averages above mean + nSigma times sigma.
                double nSigma = 1.0;
                double[] temp = DataMode.MEDIAN_SUBTRACTED
                    .process(viewer, data);
                // Get the average of the higher points
                double avg1 = findPeakAverage(temp, nSigma);
                temp = MathUtils.butterworthLowPass2Pole(SAMPLE_RATE,
                    viewer.butterworthLowPassCutoff, temp);
                // Get the average of the higher points
                double avg2 = findPeakAverage(temp, nSigma);
                // Scale the results so the averages are the same
                double factor = avg2 != 0 ? avg1 / avg2 : 1;
                // // DEBUG
                // System.out.println("avg1=" + avg1 + " avg2=" + avg2
                // + " factor=" + factor);
                for(int i = 0; i < temp.length; i++) {
                    temp[i] = factor * temp[i];
                }
                return temp;
            }
        };

        public String name;

        DataMode(String name) {
            this.name = name;
        }

        /**
         * Method that processes the data for this mode. It must return a new
         * array and not change the input.
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

        // Chart panel
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

        // BeatPanel
        JPanel beatPanel = new JPanel();
        beatPanel.setLayout(new BorderLayout());

        // Beat test area
        beatTextArea = new JTextArea();
        beatTextArea.setEditable(false);
        beatTextArea.setColumns(40);
        JScrollPane beatScrollPane = new JScrollPane(beatTextArea);
        beatPanel.add(beatScrollPane, BorderLayout.CENTER);

        // Lower split pane
        JSplitPane lowerPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            listPanel, beatPanel);
        lowerPane.setContinuousLayout(true);
        lowerPane.setDividerLocation(LOWER_PANE_DIVIDER_LOCATION);

        // Main split pane
        mainPane.setContinuousLayout(true);
        mainPane.setDividerLocation(MAIN_PANE_DIVIDER_LOCATION);
        if(false) {
            mainPane.setOneTouchExpandable(true);
        }

        // Lower panel
        lowerPanel.setLayout(new BorderLayout());
        lowerPanel.add(lowerPane, BorderLayout.CENTER);

        // Main panel
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(mainPane, BorderLayout.CENTER);

        // Content pane
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
                SaveFileDialog dialog = new SaveFileDialog(EcgStripViewer.this);
                dialog.setVisible(true);
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

        menuItem = new JMenuItem();
        menuItem.setText("Heart Beat Info...");
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showHeartBeatInfo();
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
        if(defaultOpenPath != null) {
            chooser.setCurrentDirectory(new File(defaultOpenPath));
        }
        int result = chooser.showOpenDialog(this);
        if(result == JFileChooser.APPROVE_OPTION) {
            // Save the selected path for next time
            defaultOpenPath = chooser.getSelectedFile().getParentFile()
                .getPath();
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
     * Loads a new file.
     * 
     * @param fileName
     */
    private void loadFile(final File file) {
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
    public void saveFile(File file, String id, List<Integer> stripList,
        DataMode dataMode) {
        if(file == null) {
            Utils.errMsg("File is null");
            return;
        }
        try {
            int nStrips = stripList.size();
            int totalSize = HEADER_LENGTH + nStrips * STRIP_LENGTH;
            byte[] data = new byte[totalSize];

            // Do Header
            Header header = model.getHeader().clone();
            header.setNStrips(nStrips);
            header.setId(id);
            byte[] headerData = header.getData();
            int start = 0;
            int end = HEADER_LENGTH;
            for(int i = 0; i < end; i++) {
                data[i] = headerData[i];
            }

            // Do strips
            double[] vals;
            byte[] stripData;
            for(int n : stripList) {
                // stripData = model.getStrips()[n].getData();
                vals = model.getStrips()[n].getDataAsBytes();
                vals = dataMode.process(this, vals);
                stripData = model.getStrips()[n].getConvertedBytes(vals);
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
                        updateBeatText(curStrip);
                    }
                }
            });
            menu1.add(radioButtonItem);
            bgroup.add(radioButtonItem);
        }

        menu1 = new JMenu("Settings");
        menu.add(menu1);

        item = new JMenuItem();
        item.setText("Data Scale...");
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
        item.setText("Number of Sub-Plots...");
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
        item.setText("Median Filter Window...");
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
        item.setText("Butterworth Low Pass Cutoff...");
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

        JMenu menu2 = new JMenu("RSA");
        menu1.add(menu2);

        bgroup = new ButtonGroup();

        // Select RSA source
        radioButtonItem = new JRadioButtonMenuItem();
        radioButtonItem.setText("From Default");
        radioButtonItem.setSelected(true);
        radioButtonItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                useDefaultAsRsaSource = true;
                if(curStrip != null) {
                    clearPlot();
                    addStripToChart(curStrip);
                }
            }
        });
        menu2.add(radioButtonItem);
        bgroup.add(radioButtonItem);

        radioButtonItem = new JRadioButtonMenuItem();
        radioButtonItem.setText("From Data Plotted");
        radioButtonItem.setSelected(true);
        radioButtonItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                useDefaultAsRsaSource = false;
                if(curStrip != null) {
                    clearPlot();
                    addStripToChart(curStrip);
                }
            }
        });
        menu2.add(radioButtonItem);
        bgroup.add(radioButtonItem);

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
        double[] stripData;
        try {
            stripData = strip.getDataAsBytes();
            int nDataPoints = stripData.length;
            // // Work with a copy of the data so we don't modify the original
            // double[] data = new double[nDataPoints];
            // for(int i = 0; i < nDataPoints; i++) {
            // data[i] = stripData[i];
            // }
            // // Process according to the dataMode
            // data = dataMode.process(this, data);

            // process() should return a new array
            double[] data = dataMode.process(this, stripData);

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
                int[] peakIndices;
                if(useDefaultAsRsaSource) {
                    peakIndices = strip.getPeakIndices();
                } else {
                    peakIndices = Strip.getPeakIndices(data);
                }
                if(peakIndices.length > 2) {
                    // Get the peak index values
                    double[] rsaVals = Strip.getRsaArray(peakIndices, data,
                        RSA_AVG_OUTLIER_FRACTION);
                    // DEBUG
                    // for(int i = 0; i < rsaVals.length; i++) {
                    // rsaVals[i] = (i/250);
                    // rsaVals[i] *= .02;
                    // }
                    // Scale them
                    double subPlotHeight = .25 * totalHeight / nSubPlots;
                    System.out.println(subPlotHeight);
                    // Determine scale factor to get a specified number of sec
                    // per tick mark
                    double secPerTick = .05;
                    double unitsPerTick = 50;
                    double rsaScale = unitsPerTick / secPerTick;
                    for(int i = 0; i < rsaVals.length; i++) {
                        // Scale
                        rsaVals[i] *= rsaScale;
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
     * Finds the average of points that are above the mean + nSigma * sigma of
     * the given array, where mean is the full mean, and sigma is the full
     * standard deviation.
     * 
     * @param array The array to use.
     * @param nSigma The multiplier of the standard deviation to use.
     * @return
     */
    public static double findPeakAverage(double[] array, double nSigma) {
        int nPoints = array.length;
        if(nPoints < 1) return Double.NaN;

        // Get the mean and standard deviation
        // double max = -Double.MAX_VALUE;
        // double min = Double.MAX_VALUE;
        double sum = 0.0;
        double sumsq = 0.0;
        for(int i = 0; i < nPoints; i++) {
            double val = array[i];
            // if(val > max) {
            // max = val;
            // }
            // if(val < min) {
            // min = val;
            // }
            sum += val;
            sumsq += val * val;
        }
        double mean = sum / nPoints;
        double sigma = (sumsq - nPoints * mean * mean) / (nPoints - 1);
        sigma = Math.sqrt(sigma);

        // Redo the mean using only points above the mean + nSigma * sigma
        sum = 0;
        int count = 0;
        for(int i = 0; i < nPoints; i++) {
            double val = array[i];
            if(val > mean + nSigma * sigma) {
                sum += val;
                count++;
            }
        }
        return count > 0 ? sum / count : Double.NaN;
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
        list.clearSelection();
        clearPlot();
        addStripToChart(strip);
        curStrip = strip;
        updateBeatText(strip);
    }

    /**
     * Shows patient information
     */
    private void showInfo() {
        if(model != null && model.getHeader() != null) {
            String info = model.getHeader().getInfo();
            // JOptionPane.showMessageDialog(null, info, "Patient Information",
            // JOptionPane.PLAIN_MESSAGE);
            scrolledTextMsg(null, info, "Patient Info", 600, 400);
        }
    }

    /**
     * Updates the beat information in the beat text area.
     * 
     * @param strip
     */
    private void updateBeatText(Strip strip) {
        String info = "Heartbeat Information" + LS;
        info += strip.getStringDate() + " " + strip.getStringTime(false) + " "
            + strip.getHeartRate() + " bpm" + " " + strip.getDiagnosisString()
            + LS + LS;
        info += "Data Mode is " + dataMode.name + LS;
        info += getHeartBeatInfo(dataMode);
        beatTextArea.setText(info);
        beatTextArea.setCaretPosition(0);
    }

    /**
     * Shows heart-beat information in a dialog.
     */
    private void showHeartBeatInfo() {
        String info = "";
        if(curStrip != null) {
            info += curStrip.getStringDate() + " "
                + curStrip.getStringTime(false) + " " + curStrip.getHeartRate()
                + " bpm" + " " + curStrip.getDiagnosisString() + LS + LS;
        } else {
            info = "Error getting current strip";
            scrolledTextMsg(null, info, "Heart Beat Info", 600, 400);
        }

        // Default
        info += "Default" + LS;
        info += getHeartBeatInfo(DataMode.DEFAULT);
        if(dataMode == DataMode.DEFAULT) {
            scrolledTextMsg(null, info, "Heart Beat Info", 600, 400);
            return;
        }

        // Current data mode
        info += LS;
        info += dataMode.name + LS;
        info += getHeartBeatInfo(dataMode);
        scrolledTextMsg(null, info, "Heart Beat Info", 600, 400);
    }

    /**
     * Gets information about the heartbeats for the given data mode.
     * 
     * @param dataMode
     * @return
     */
    private String getHeartBeatInfo(DataMode dataMode) {
        String info = "";
        if(curStrip == null) {
            info = "  There is no strip";
            return info;
        }
        if(model == null || model.getHeader() == null) {
            info = "  There is no strip model or it is invalid";
            return info;
        }

        double[] stripData = null;
        double[] vals = null;
        double[] array = null;
        double max, min, mean, sigma, sum, sumsq, val, rsaBaseLine;
        int maxIndex, minIndex;

        stripData = curStrip.getDataAsBytes();
        vals = dataMode.process(this, stripData);
        int[] peakIndices = Strip.getPeakIndices(vals);
        int nPeaks = peakIndices.length;
        int nIntervals = nPeaks - 1;

        // Get the statistics
        if(nPeaks == 0) {
            info += "  No R peaks found" + LS;
        } else if(nPeaks == 1) {
            info += "  Only one R peak found, not enough to calculate intervals"
                + LS;
        } else {
            rsaBaseLine = Strip.getAveragePeakInterval(peakIndices,
                RSA_AVG_OUTLIER_FRACTION);
            array = new double[nPeaks];
            for(int i = 1; i < nPeaks; i++) {
                array[i] = 60. / INDEX_TO_SEC
                    / (peakIndices[i] - peakIndices[i - 1]);
            }
            maxIndex = -1;
            minIndex = -1;
            max = -Double.MAX_VALUE;
            min = Double.MAX_VALUE;
            sum = 0.0;
            sumsq = 0.0;
            for(int i = 1; i < nPeaks; i++) {
                val = array[i];
                if(val > max) {
                    max = val;
                    maxIndex = i;
                }
                if(val < min) {
                    min = val;
                    minIndex = i;
                }
                sum += val;
                sumsq += val * val;
            }
            mean = sum / nIntervals;
            sigma = (sumsq - nIntervals * mean * mean) / (nIntervals - 1);
            sigma = Math.sqrt(sigma);
            info += "  Number of R peaks: " + nPeaks + LS;
            info += "  Number of intervals: " + nIntervals + LS;
            info += "  Mean BPM: " + String.format("%.2f", mean) + LS;
            info += "  BPM Standard Deviation: " + String.format("%.2f", sigma)
                + LS;
            info += String.format("  Max BPM: %.2f @ %.2f sec", max,
                peakIndices[maxIndex] * INDEX_TO_SEC) + LS;
            info += String.format("  Min BPM: %.2f @ %.2f sec", min,
                peakIndices[minIndex] * INDEX_TO_SEC) + LS;
            if(true) {
                info += String.format("  Min Interval: %.2f sec @ %.2f sec",
                    60. / max, peakIndices[maxIndex] * INDEX_TO_SEC) + LS;
                info += String.format("  Max Interval: %.2f sec @ %.2f sec",
                    60. / min, peakIndices[minIndex] * INDEX_TO_SEC) + LS;
            }
            info += "  RSA Baseline: "
                + String.format("%.2f BPM = %.2f sec", 60. / INDEX_TO_SEC
                    / rsaBaseLine, rsaBaseLine * INDEX_TO_SEC) + LS;
        }
        return info;
    }

    /**
     * Displays a scrolled text dialog with the given message.
     * 
     * @param message
     */
    public static void scrolledTextMsg(Frame parent, String message,
        String title, int width, int height) {
        final JDialog dialog = new JDialog(parent);

        // Message
        JPanel jPanel = new JPanel();
        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        jPanel.add(scrollPane, BorderLayout.CENTER);
        dialog.getContentPane().add(scrollPane);

        // Close button
        jPanel = new JPanel();
        JButton button = new JButton("OK");
        jPanel.add(button);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
            }

        });
        dialog.getContentPane().add(jPanel, BorderLayout.SOUTH);

        // Settings
        dialog.setTitle(title);
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dialog.setSize(width, height);
        // Has to be done after set size
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
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
