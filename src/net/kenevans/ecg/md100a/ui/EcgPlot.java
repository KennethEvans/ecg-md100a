package net.kenevans.ecg.md100a.ui;

import java.awt.Color;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;

import net.kenevans.core.utils.Utils;
import net.kenevans.ecg.md100a.model.EcgFilterModel;
import net.kenevans.ecg.md100a.model.IConstants;
import net.kenevans.ecg.md100a.model.Strip;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/*
 * Created on Jul 29, 2012
 * By Kenneth Evans, Jr.
 */

/**
 * EcgPlot handles plotting for the EcgStripViewer.
 * 
 * @author Kenneth Evans, Jr.
 */
public class EcgPlot implements IConstants
{
    /** Default value for the range maximum. */
    private static final double YMAX = 160;
    /** Value for the domain maximum. */
    private static final double XMAX = 30;

    /** The XYSeriesCollection used in the plot. It is filled out as needed. */
    private XYSeriesCollection dataset = new XYSeriesCollection();

    /** Used to retain the domain limits for resetting the plot. */
    private double defaultXMax;
    /** Used to retain the range limits for resetting the plot. */
    private double defaultYMax;
    /** Whether to show markers in the plot. */
    private boolean showMarkers = false;
    /** Whether to show RSA data in the plot. */
    private boolean showRSA = true;
    /** Whether to get the RSA values from the default or the current data mode */
    private boolean useDefaultAsRsaSource = true;

    /** The number of sub-plots to use. */
    private int nSubPlots = 1;

    /**
     * Determines the default value for determining the total range. Choosing
     * larger values makes the data look larger.
     */
    private static final double DATA_SCALE_DEFAULT = 1;
    /**
     * Determines the default scale factor for converting RSA values in seconds
     * to mm on the plot. The units are mm/sec.
     */
    private static final double RSA_SCALE_DEFAULT = 100;

    /** The dataScale to use */
    private double dataScale = DATA_SCALE_DEFAULT;
    /** The rsaScale to use */
    private double rsaScale = RSA_SCALE_DEFAULT;

    /** The color for the ECG strip. */
    private Paint stripColor = Color.RED;
    /** The color for the RSA curve. */
    private Paint rsaColor = new Color(0, 153, 255);
    /** The color for the RSA base line. */
    private Paint rsaBaseLineColor = new Color(0, 0, 255);

    /** The ChartPanel for the chart. */
    private ChartPanel chartPanel;

    /** The EcgStripViewer that contains this plot. */
    private EcgStripViewer viewer;

    public EcgPlot(EcgStripViewer viewer) {
        this.viewer = viewer;
    }

    /**
     * Creates the JFreeChart and ChartPanel. Sets the XYDataSet in it but does
     * nothing with it otherwise.
     * 
     * @return The chart created.
     */
    public JFreeChart createChart() {
        // Generate the graph
        JFreeChart chart = ChartFactory.createXYLineChart(
            "30-sec ECG Measurement", "sec (25 mm / sec)", "mm (10 mm / mV)",
            null, PlotOrientation.VERTICAL, // BasicImagePlot
            // Orientation
            false, // Show Legend
            false, // Use tooltips
            false // Configure chart to generate URLs?
            );
        // Change the axis limits
        chart.getXYPlot().getRangeAxis().setRange(-YMAX, YMAX);
        // chart.getXYPlot().getRangeAxis().setTickLabelsVisible(false);
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
                addStripToChart(viewer.getCurStrip());
            }
        });
        menu.add(item);

        JMenu menu1 = new JMenu("Data Plotted");
        menu.add(menu1);

        ButtonGroup bgroup = new ButtonGroup();

        // Loop over the dataModes
        JRadioButtonMenuItem radioButtonItem;
        for(final EcgFilterModel.DataMode mode : EcgFilterModel.dataModeList) {
            radioButtonItem = new JRadioButtonMenuItem();
            radioButtonItem.setText(mode.getName());
            radioButtonItem
                .setSelected(viewer.getFilterModel().getDataMode() == mode);
            radioButtonItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    viewer.getFilterModel().setDataMode(mode);
                    if(viewer.getCurStrip() != null) {
                        clearPlot();
                        addStripToChart(viewer.getCurStrip());
                        viewer.updateBeatText(viewer.getCurStrip());
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
                    addStripToChart(viewer.getCurStrip());
                }
            }
        });
        menu1.add(item);

        item = new JMenuItem();
        item.setText("RSA Scale...");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                double oldRsaScale = rsaScale;
                String value = (String)JOptionPane.showInputDialog(null,
                    "Enter the RSA scale factor in mm/sec"
                        + "(non-negative floating point value)" + LS
                        + "or -1 to restore the default", "RSA Scale Factor",
                    JOptionPane.PLAIN_MESSAGE, null, null, rsaScale);
                if(value == null) {
                    return;
                }
                try {
                    rsaScale = Double.parseDouble(value);
                } catch(NumberFormatException ex) {
                    Utils.errMsg("Invalid value for the RSA scale factor,"
                        + " using default (" + RSA_SCALE_DEFAULT + ")");
                    rsaScale = RSA_SCALE_DEFAULT;
                }
                if(rsaScale < 0) {
                    Utils.errMsg("Invalid value for the RSA scale factor,"
                        + " using default (" + RSA_SCALE_DEFAULT + ")");
                    rsaScale = RSA_SCALE_DEFAULT;
                }
                // Redraw if it has changed
                if(rsaScale != oldRsaScale) {
                    clearPlot();
                    addStripToChart(viewer.getCurStrip());
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
                    addStripToChart(viewer.getCurStrip());
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
                    null, viewer.getFilterModel().getMedianFilterWindow());
                if(value == null) {
                    return;
                }
                int medianFilterWindow = EcgFilterModel.MEDIAN_FILTER_WINDOW_DEFAULT;
                try {
                    medianFilterWindow = Integer.parseInt(value);
                } catch(NumberFormatException ex) {
                    Utils.errMsg("Invalid value for the Median Filter Window,"
                        + " using default ("
                        + EcgFilterModel.MEDIAN_FILTER_WINDOW_DEFAULT + ")");
                    medianFilterWindow = EcgFilterModel.MEDIAN_FILTER_WINDOW_DEFAULT;
                }
                if(medianFilterWindow < 0) {
                    Utils.errMsg("Invalid value for the Median Filter Window,"
                        + " using default ("
                        + EcgFilterModel.MEDIAN_FILTER_WINDOW_DEFAULT + ")");
                    medianFilterWindow = EcgFilterModel.MEDIAN_FILTER_WINDOW_DEFAULT;
                }
                viewer.getFilterModel().setMedianFilterWindow(
                    medianFilterWindow);
                // Redraw if we are showing one of those modes
                if(viewer.getFilterModel().getDataMode() == EcgFilterModel.DataMode.MEDIAN
                    || viewer.getFilterModel().getDataMode() == EcgFilterModel.DataMode.MEDIAN_SUBTRACTED) {
                    clearPlot();
                    addStripToChart(viewer.getCurStrip());
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
                    null, null, viewer.getFilterModel()
                        .getButterworthLowPassCutoff());
                if(value == null) {
                    return;
                }
                double butterworthLowPassCutoff = EcgFilterModel.BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT;
                try {
                    butterworthLowPassCutoff = Double.parseDouble(value);
                } catch(NumberFormatException ex) {
                    Utils
                        .errMsg("Invalid value for the Butterworth Low Pass Cutoff,"
                            + " using default ("
                            + EcgFilterModel.BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT
                            + ")");
                    butterworthLowPassCutoff = EcgFilterModel.BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT;
                }
                if(butterworthLowPassCutoff < 0) {
                    Utils
                        .errMsg("Invalid value for the Butterworth Low Pass Cutoff,"
                            + " using default ("
                            + EcgFilterModel.BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT
                            + ")");
                    butterworthLowPassCutoff = EcgFilterModel.BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT;
                }
                viewer.getFilterModel().setButterworthLowPassCutoff(
                    butterworthLowPassCutoff);
                // Redraw if we are showing one of those modes
                if(viewer.getFilterModel().getDataMode() == EcgFilterModel.DataMode.BUTTERWORTH_LOW_PASS) {
                    clearPlot();
                    addStripToChart(viewer.getCurStrip());
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
                if(viewer.getCurStrip() != null) {
                    clearPlot();
                    addStripToChart(viewer.getCurStrip());
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
                if(viewer.getCurStrip() != null) {
                    clearPlot();
                    addStripToChart(viewer.getCurStrip());
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
                chart.getXYPlot().getRangeAxis()
                    .setRange(-.5 * defaultYMax, .5 * defaultYMax);
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
                chart.getXYPlot().getRangeAxis()
                    .setRange(-.5 * defaultYMax, .5 * defaultYMax);
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
    public void clearPlot() {
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
    public void addStripToChart(Strip strip) {
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
            double[] data = viewer.getFilterModel().getDataMode()
                .process(viewer.getFilterModel(), stripData);

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
            // The totalHeight should be divisible by 2, 3, and 5
            double totalHeight = 60;
            totalHeight /= dataScale;

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
            chart.getXYPlot().getRangeAxis()
                .setRange(-.5 * totalHeight, .5 * totalHeight);
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
                    for(int i = 0; i < rsaVals.length; i++) {
                        // Scale according to the RSA scale
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
                    xVals, new double[] {5});
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
            offset = .5 * totalHeight
                - ((i + 1 - originFraction) * totalHeight) / nSubPlots;
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
     * @return The value of chartPanel.
     */
    public ChartPanel getChartPanel() {
        return chartPanel;
    }

}
