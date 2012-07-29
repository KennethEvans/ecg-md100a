package net.kenevans.ecg.md100a.model;

import net.kenevans.ecg.md100a.utils.MathUtils;

/*
 * Created on Jul 29, 2012
 * By Kenneth Evans, Jr.
 */

/**
 * EcgFilterModel handles filters and data modes.
 * 
 * @author Kenneth Evans, Jr.
 */
public class EcgFilterModel implements IConstants
{
    /** The default window to use for the median filter. */
    public static final int MEDIAN_FILTER_WINDOW_DEFAULT = 50;
    /** The window to use for the median filter. */
    private int medianFilterWindow = MEDIAN_FILTER_WINDOW_DEFAULT;
    /** The default cutoff to use for the Butterworth low pass filter. */
    public static final double BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT = 8;
    /** The cutoff to use for the Butterworth low pass filter. */
    private double butterworthLowPassCutoff = BUTTERWORTH_LP_FILTER_CUTOFF_DEFAULT;

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
            public double[] process(EcgFilterModel viewer, double[] data) {
                double[] result = new double[data.length];
                for(int i = 0; i < result.length; i++) {
                    result[i] = data[i];
                }
                return result;
            }
        },
        MEDIAN_SUBTRACTED("Median Subtracted") {
            @Override
            public double[] process(EcgFilterModel viewer, double[] data) {
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
            public double[] process(EcgFilterModel viewer, double[] data) {
                return MathUtils.medianFilter(data, viewer.medianFilterWindow);
            }
        },
        BUTTERWORTH("Amperor Butterworth") {
            @Override
            public double[] process(EcgFilterModel viewer, double[] data) {
                return MathUtils.butterworth_6_05_75(data);
            }
        },
        BUTTERWORTH_LOW_PASS("Butterworth Low Pass") {
            @Override
            public double[] process(EcgFilterModel viewer, double[] data) {
                return MathUtils.butterworthLowPass2Pole(SAMPLE_RATE,
                    viewer.butterworthLowPassCutoff, data);
            }
        },
        MEDIAN_SUBTRACTED_BUTTERWORTH_LOW_PASS(
            "Median Subtracted Butterworth Low Pass Scaled") {
            @Override
            public double[] process(EcgFilterModel viewer, double[] data) {
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

        private String name;

        DataMode(String name) {
            this.name = name;
        }

        /**
         * Method that processes the data for this mode. It must return a new
         * array and not change the input.
         * 
         * @param viewer The EcgFilterModel. Used to access instance variables.
         * @param data The input data.
         * @return
         */
        public abstract double[] process(EcgFilterModel viewer, double[] data);

        /**
         * @return The value of name.
         */
        public String getName() {
            return name;
        }

    };

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
     * @return The value of medianFilterWindow.
     */
    public int getMedianFilterWindow() {
        return medianFilterWindow;
    }

    /**
     * @param medianFilterWindow The new value for medianFilterWindow.
     */
    public void setMedianFilterWindow(int medianFilterWindow) {
        this.medianFilterWindow = medianFilterWindow;
    }

    /**
     * @return The value of butterworthLowPassCutoff.
     */
    public double getButterworthLowPassCutoff() {
        return butterworthLowPassCutoff;
    }

    /**
     * @param butterworthLowPassCutoff The new value for
     *            butterworthLowPassCutoff.
     */
    public void setButterworthLowPassCutoff(double butterworthLowPassCutoff) {
        this.butterworthLowPassCutoff = butterworthLowPassCutoff;
    }

    /**
     * @return The value of dataMode.
     */
    public DataMode getDataMode() {
        return dataMode;
    }

    /**
     * @param dataMode The new value for dataMode.
     */
    public void setDataMode(DataMode dataMode) {
        this.dataMode = dataMode;
    }

}
