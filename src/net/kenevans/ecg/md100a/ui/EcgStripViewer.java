package net.kenevans.ecg.md100a.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
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
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.kenevans.core.utils.AboutBoxPanel;
import net.kenevans.core.utils.ImageUtils;
import net.kenevans.core.utils.Utils;
import net.kenevans.ecg.md100a.model.EcgFileModel;
import net.kenevans.ecg.md100a.model.EcgFilterModel;
import net.kenevans.ecg.md100a.model.Header;
import net.kenevans.ecg.md100a.model.IConstants;
import net.kenevans.ecg.md100a.model.Strip;

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

    /** Keeps the last-used path for the file open dialog. */
    public String defaultOpenPath = DEFAULT_DIR;
    /** Keeps the last-used path for the file save dialog. */
    public String defaultSavePath = DEFAULT_DIR;

    /** The model for this user interface. */
    private EcgFileModel model;

    /** The plot for this user interface. */
    private EcgPlot plot;

    /** The data ecgFilterModel model for this user interface. */
    private EcgFilterModel ecgFilterModel;

    // User interface controls (Many do not need to be global)
    private Container contentPane = this.getContentPane();
    private JPanel listPanel = new JPanel();
    private JPanel lowerPanel = new JPanel();
    private DefaultListModel listModel = new DefaultListModel();
    private JList list = new JList(listModel);
    private JScrollPane listScrollPane;
    private JTextArea beatTextArea;
    private JPanel displayPanel = new JPanel();
    private JPanel mainPanel = new JPanel();
    private JSplitPane mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        displayPanel, lowerPanel);
    private JMenuBar menuBar;

    /** Array of Strips for the viewer. */
    public Strip[] strips;
    /** The currently selected Strip. */
    private Strip curStrip;

    /**
     * EcgStripViewer constructor.
     */
    public EcgStripViewer() {
        ecgFilterModel = new EcgFilterModel();
        plot = new EcgPlot(this);
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
        plot.createChart();
        plot.getChartPanel().setPreferredSize(new Dimension(600, 270));
        plot.getChartPanel().setDomainZoomable(true);
        plot.getChartPanel().setRangeZoomable(true);
        javax.swing.border.CompoundBorder compoundborder = BorderFactory
            .createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createEtchedBorder());
        plot.getChartPanel().setBorder(compoundborder);
        displayPanel.add(plot.getChartPanel());

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

            // Set the icon
            ImageUtils.setIconImageFromResource(this,
                "/resources/HeartMonitor.36x36.png");

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
        EcgFilterModel.DataMode dataMode) {
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
                vals = dataMode.process(ecgFilterModel, vals);
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
        plot.clearPlot();
        plot.addStripToChart(strip);
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
    public void updateBeatText(Strip strip) {
        String info = "Heartbeat Information" + LS;
        info += strip.getStringDate() + " " + strip.getStringTime(false) + " "
            + strip.getHeartRate() + " bpm" + " " + strip.getDiagnosisString()
            + LS + LS;
        info += "Data Mode is " + ecgFilterModel.getDataMode().getName() + LS;
        info += getHeartBeatInfo(ecgFilterModel.getDataMode());
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
        info += getHeartBeatInfo(EcgFilterModel.DataMode.DEFAULT);
        if(ecgFilterModel.getDataMode() == EcgFilterModel.DataMode.DEFAULT) {
            scrolledTextMsg(null, info, "Heart Beat Info", 600, 400);
            return;
        }

        // Current data mode
        info += LS;
        info += ecgFilterModel.getDataMode().getName() + LS;
        info += getHeartBeatInfo(ecgFilterModel.getDataMode());
        scrolledTextMsg(null, info, "Heart Beat Info", 600, 400);
    }

    /**
     * Gets information about the heartbeats for the given data mode.
     * 
     * @param dataMode
     * @return
     */
    private String getHeartBeatInfo(EcgFilterModel.DataMode dataMode) {
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
        vals = ecgFilterModel.getDataMode().process(ecgFilterModel, stripData);
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
     * @return The value of curStrip.
     */
    public Strip getCurStrip() {
        return curStrip;
    }

    /**
     * @return The value of ecgFilterModel.
     */
    public EcgFilterModel getFilterModel() {
        return ecgFilterModel;
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
