package net.kenevans.ecg.md100a.model;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.kenevans.core.utils.Utils;
import net.kenevans.ecg.md100a.utils.MathUtils;

/*
 * Created on Jun 24, 2012
 * By Kenneth Evans, Jr.
 */

/**
 * Strip is a class to manage one 30 second strip from the MD100A ECG Monitor.
 * 
 * @author Kenneth Evans, Jr.
 */
public class Strip implements IConstants
{
    public static final String LS = System.getProperty("line.separator");
    // DEBUG
    private static boolean FIRST = true;

    private String year;
    private String month;
    private String day;
    private String hour;
    private String min;
    private String sec;
    private String diagnostic;
    private String heartRate;

    private byte[] data;
    private double[] vals;
    private int[] peakIndices;

    /**
     * Strip constructor.
     * 
     * @param bytes The bytes comprising this strip.
     */
    Strip(byte[] bytes) {
        int stripLength = bytes.length;
        // Copy it
        this.data = new byte[stripLength];
        for(int i = 0; i < stripLength; i++) {
            this.data[i] = bytes[i];
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bais);
        short intVal;
        int nRead = 0;
        int skip = -1;
        try {
            intVal = dis.readShort();
            nRead += 2;

            intVal = dis.readShort();
            nRead += 2;
            year = Integer.toString(intVal);

            intVal = dis.readShort();
            nRead += 2;
            month = Integer.toString(intVal);

            intVal = dis.readShort();
            nRead += 2;

            intVal = dis.readShort();
            nRead += 2;
            day = Integer.toString(intVal);

            intVal = dis.readShort();
            nRead += 2;
            hour = Integer.toString(intVal);

            intVal = dis.readShort();
            nRead += 2;
            min = Integer.toString(intVal);

            intVal = dis.readShort();
            nRead += 2;
            sec = Integer.toString(intVal);

            intVal = dis.readShort();
            nRead += 2;

            intVal = dis.readShort();
            nRead += 2;
            diagnostic = Integer.toString(intVal);

            intVal = dis.readShort();
            nRead += 2;
            heartRate = Integer.toString(intVal);

            // skip = STRIP_DATA_START - nRead;
            dis.skip(skip);
            dis.close();
        } catch(EOFException ex) {
            String available = "Unknown";
            try {
                // This throws an IOException
                available = Integer.toString(dis.available());
            } catch(Exception ex1) {
                // Do nothing
            }
            String msg = "EOF getting strip" + LS + "Data length="
                + data.length + LS + "nRead=" + nRead + LS + "dataStart="
                + STRIP_START + LS + "skip=" + skip + LS + "dis.available()="
                + available;
            Utils.excMsg(msg, ex);
        } catch(IOException ex) {
            Utils.excMsg("I/O Error getting strip", ex);
        } catch(Exception ex) {
            Utils.excMsg("Error getting strip", ex);
        }
    }

    /**
     * Get the data as an array of data points starting reading at the beginning
     * plus a hard-coded skip value. A skip value of 26 to starts reading just
     * after the 55AA byte.
     * 
     * @return
     */
    public double[] getDataAsBytes() {
        // Calculate the first time, then use the stored values
        if(this.vals != null) {
            return this.vals;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bais);
        // We want to get nVals bytes starting at the end
        int nVals = STRIP_N_DATA_VALS;
        // These should be in sections of 501 bytes;
        int nSegs = STRIP_N_DATA_SEGMENTS;
        // Number of bytes to skip to go to the location after 55AA.
        int skip = STRIP_DATA_START;
        double[] vals = new double[nVals];

        // DEBUG
        // System.out.println("getDataAsBytes");
        // System.out.println("  data.length=" + data.length);
        // System.out.println("  nVals=" + nVals);
        // System.out.println("  nSegs=" + nSegs);
        // System.out.println("  skip=" + skip);
        // int startOffset = STRIP_START + skip;
        // System.out.printf("Starting at location : %x" + LS, startOffset);

        // DEBUG Extra Information
        // printFirstBytes(32);

        int seg = -1;
        try {
            dis.skip(skip);
            for(seg = 0; seg < nSegs; seg++) {
                boolean res = readSegmentBytes(dis, vals, seg
                    * SEGMENT_N_DATA_VALS);
                if(!res) {
                    Utils.errMsg("Error reading segment " + seg);
                }
            }
            // Subtract 512 so 0 corresponds to 0 mV
            // TODO Could convert to mV here
            for(int i = 0; i < vals.length; i++) {
                vals[i] -= 512;
            }
        } catch(EOFException ex) {
            String msg = "EOF getting strip at seg=" + seg + LS
                + "Data length=" + data.length + LS + "nVals=" + nVals + LS
                + "skip=" + skip;
            Utils.excMsg(msg, ex);
            return null;
        } catch(IOException ex) {
            Utils.excMsg("I/O Error getting strip at seg=" + seg, ex);
            return null;
        } catch(Exception ex) {
            Utils.excMsg("Error getting strip at seg=" + seg, ex);
            return null;
        } finally {
            if(dis != null) {
                try {
                    dis.close();
                } catch(IOException ex) {
                    // Do nothing
                }
            }
        }
        // Only set the internal values if we got to here.
        this.vals = vals;
        return vals;
    }

    public byte[] getConvertedBytes(double[] vals) {
        byte[] bytes = new byte[STRIP_LENGTH];
        // Duplicate the header
        for(int i = 0; i < STRIP_DATA_START; i++) {
            bytes[i] = data[i];
        }

        int nextIndex = STRIP_DATA_START;
        int nextVal = 0;
        short val = 0;
        for(int seg = 0; seg < STRIP_N_DATA_SEGMENTS; seg++) {
            for(int i = 0; i < SEGMENT_LENGTH; i++) {
                if(i == 0) {
                    val = (short)Math.round(vals[nextVal] + 512);
                    bytes[nextIndex++] = (byte)((val >> 8) & 0xff);
                } else if(i == 1) {
                    bytes[nextIndex++] = (byte)(val & 0xff);
                    nextVal++;
                } else {
                    val = (short)(Math.round(vals[nextVal]));
                    val -= (short)(Math.round(vals[nextVal - 1]));
                    nextVal++;
                    // This logic is necessary because Java's byte is unsigned
                    if(val >= 0) {
                        bytes[nextIndex++] = (byte)(val & 0xff);
                    } else {
                        bytes[nextIndex++] = (byte)((-(val + 128)) & 0xff);
                    }
                }
                // // DEBUG
                // if(seg == 0 && i < 5) {
                // byte bVal = bytes[nextIndex - 1];
                // System.out.println("getConvertedBytes "
                // + String.format(
                // "%d | val: %d %4x | 128-val: %d %4x | -(val + 128): %d %4x | bVal: %d %2x",
                // i, val, val, 128 - val, 128 - val, -(val + 128), -(val +
                // 128), bVal, bVal));
                // }
            }
        }
        return bytes;
    }

    public boolean readSegmentBytes(LittleEndianDataInputStream dis,
        double[] vals, int startIndex) throws IOException {
        int j = startIndex;
        int bval = 0;
        for(int i = 0; i < SEGMENT_LENGTH; i++) {
            // Fill the data with NaN if there are more data points than bytes
            if(dis.available() == 0) {
                if(i != 0) {
                    vals[j++] = Double.NaN;
                }
                continue;
            }
            bval = dis.readUnsignedByte();
            if(i == 0) {
                // Same as bval << 8
                vals[j] = bval * 256;
            } else if(i == 1) {
                vals[j] += bval;
            } else {
                if(bval < 128) {
                    vals[j] = vals[j - 1] + bval;
                } else {
                    vals[j] = vals[j - 1] - bval + 128;
                }
            }
            // DEBUG
            // if(startIndex == 0 * SEGMENT_N_DATA_VALS) {
            // System.out.println("i=" + i + " j=" + j + " sec=" + (j * .004)
            // + " bval=" + bval + " vals[j]=" + vals[j]);
            // }

            // // DEBUG
            // if(startIndex == 0 && i < 5) {
            // System.out.println("readSegmentBytes " + i + ": " + vals[j]
            // + " " + bval + " " + String.format("%2x", bval));
            // }
            if(i != 0) {
                j++;
            }
        }
        return true;
    }

    /**
     * Finds the peak indices for the array of values in this instance minus a
     * median filter of the values. Stores the result the first time it is
     * called and uses that value afterward.
     * 
     * @return
     */
    public int[] getPeakIndices() {
        // Calculate the first time, then use the stored values
        if(this.peakIndices != null) {
            return this.peakIndices;
        }
        this.peakIndices = getPeakIndices(this.vals);
        return this.peakIndices;
    }

    /**
     * Finds the peak indices for the given array of values minus a median
     * filter of the values.
     * 
     * @param vals The array of values to use.
     * @return
     */
    public static int[] getPeakIndices(double[] vals) {
        int window = 50;
        if(vals == null) {
            return null;
        }
        int nVals = vals.length;
        if(nVals == 0) {
            return new int[0];
        }

        // Look for the peaks in the vals minus a median filter of the vals
        // The median filter eliminates noise and subtracting eliminates
        // baseline variation
        double[] wVals = MathUtils.medianFilter(vals, window);
        for(int i = 0; i < nVals; i++) {
            wVals[i] = vals[i] - wVals[i];
        }

        List<Integer> indicesList = new ArrayList<Integer>();
        for(int i = 0; i < nVals; i++) {
            if(isPeak(wVals, i)) {
                indicesList.add(new Integer(i));
            }
        }
        int nIndices = indicesList.size();
        int[] peakIndices = new int[nIndices];
        int i = 0;
        for(Integer integer : indicesList) {
            // DEBUG
            // System.out.println(integer);
            peakIndices[i++] = integer;
        }
        return peakIndices;
    }

    /**
     * Determines if this index is an R peak.
     * 
     * @param fVals The array to use for finding peaks. Typically this array
     *            will have been processed first and be normalized so zero
     *            corresponds to 0 mV.
     * @param index The index to check.
     * @return
     */
    public static boolean isPeak(double[] fVals, int index) {
        // DEBUG
        // if(index == 600) {
        // System.out.println("Stop here");
        // }
        // Hard-coded flag to check if there are adjacent negative values
        boolean checkNegative = true;
        // Hard-coded threshold (value must be greater than this)
        double threshold = 25;
        // Must be above threshold
        if(fVals[index] < threshold) {
            return false;
        }
        // Cannot be preceded by the same value
        // In the case of two equal values at the top, take the first
        if(index > 0 && fVals[index - 1] == fVals[index]) {
            return false;
        }
        // Must be greater than surrounding values
        int delta = 10;
        int len = fVals.length;

        int iMin = index - delta;
        if(iMin < 0) {
            iMin = 0;
        }
        int iMax = index + delta;
        if(iMax > len) {
            iMax = len;
        }
        for(int i = iMin; i < iMax; i++) {
            if(fVals[i] > fVals[index]) {
                return false;
            }
        }
        if(!checkNegative) {
            return true;
        }
        // Must be preceded by a negative value within minCheck indices
        boolean possible = false;
        int minCheck = 10;
        iMin = index - minCheck;
        if(iMin < 0) {
            // Are not enough indices to check
            possible = true;
        } else {
            for(int i = iMin; i < index; i++) {
                if(fVals[i] < 0) {
                    possible = true;
                    break;
                }
            }
        }
        if(!possible) {
            return false;
        }
        // It has passed the check for being preceded by a negative value
        // Must be followed by a negative value within minCheck indices
        iMax = index + minCheck;
        if(iMax > len) {
            // Are not enough indices to check
            return true;
        }
        for(int i = index + 1; i < iMax; i++) {
            if(fVals[i] < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the average value at the peaks eliminating outliers that fall
     * outside (1 +/- the fractionOfAverage) of the true average. Equivalent to
     * getAveragePeakValue(fractionOfAverage, this.vals).
     * 
     * @param fractionOfAverage The fraction of the original average to use in
     *            determining outliers.
     * @param vals Array of values to use.
     * @return
     * @see #getAveragePeakValue(double fractionOfAverage, double[] vals)
     */
    public double getAveragePeakValue(double fractionOfAverage) {
        return getAveragePeakValue(this.vals, fractionOfAverage);
    }

    /**
     * Gets the average value of the given array at the peaks eliminating
     * outliers that fall outside (1 +/- the fractionOfAverage) of the true
     * average.
     * 
     * @param fractionOfAverage The fraction of the original average to use in
     *            determining outliers.
     * @param vals Array of values to use.
     * @return
     */
    public double getAveragePeakValue(double[] vals, double fractionOfAverage) {
        if(peakIndices == null) {
            getPeakIndices();
        }
        int nPeaks = peakIndices.length;
        // DEBUG
        // for(int i = 0; i < peakIndices.length; i++) {
        // System.out.println(i + " " + peakIndices[i]);
        // }
        // Find the average
        double sum = 0;
        int count = 0;
        for(int i = 0; i < nPeaks; i++) {
            sum += vals[i];
            count++;
        }
        double avg = count > 0 ? sum / count : 0;

        // Redo the average eliminating outliers
        sum = 0;
        count = 0;
        double val;
        for(int i = 1; i < nPeaks; i++) {
            val = vals[i];
            if(val < (1 + fractionOfAverage) * avg
                && val > (1 - fractionOfAverage) * avg) {
                sum += val;
                count++;
            }
        }
        avg = count > 0 ? sum / count : 0;
        return avg;
    }

    /**
     * Gets the average interval between peaks eliminating outliers that fall
     * outside (1 +/- the fractionOfAverage) of the true average.
     * 
     * @param peakIndices The array of peak indices to use.
     * @param fractionOfAverage The fraction of the original average to use in
     *            determining outliers.
     * @return
     */
    public static double getAveragePeakInterval(int[] peakIndices,
        double fractionOfAverage) {
        int nPeaks = peakIndices.length;
        // DEBUG
        // for(int i = 0; i < peakIndices.length; i++) {
        // System.out.println(i + " " + peakIndices[i]);
        // }
        // Find the average skipping the first point
        double sum = 0;
        int count = 0;
        double delta;
        for(int i = 1; i < nPeaks; i++) {
            delta = peakIndices[i] - peakIndices[i - 1];
            sum += delta;
            count++;
        }
        double avg = count > 0 ? sum / count : 0;

        // Redo the average eliminating outliers
        sum = 0;
        count = 0;
        for(int i = 1; i < nPeaks; i++) {
            delta = peakIndices[i] - peakIndices[i - 1];
            if(delta < (1 + fractionOfAverage) * avg
                && delta > (1 - fractionOfAverage) * avg) {
                sum += delta;
                count++;
            }
        }
        avg = count > 0 ? sum / count : 0;
        return avg;
    }

    /**
     * Calculates an array of values of the time between peaks at the peaks in
     * this instance. The array is a step function. The steps represent the
     * interval between the last peak and the one before it. The steps start at
     * the second peak and NEGATIVE_INFINITY is used for times before that. The
     * values are the difference from the average. The average is calculated not
     * including outliers in the time difference.<br>
     * <br>
     * 
     * This version uses the instance values for the peak indices and the
     * values.
     * 
     * @param fractionOfAverage The fraction of the original average to use in
     *            determining outliers as used in getAveragePeakInterval().
     * @return
     * @see #getAveragePeakInterval
     */
    public double[] getRsaArray(double fractionOfAverage) {
        if(this.peakIndices == null) {
            getPeakIndices();
        }
        return getRsaArray(this.peakIndices, this.vals, fractionOfAverage);
    }

    /**
     * Calculates an array of values of the time between peaks at the peaks in
     * this instance. The array is a step function. The steps represent the
     * interval between the last peak and the one before it. The steps start at
     * the second peak and NEGATIVE_INFINITY is used for times before that. The
     * values are the difference from the average. The average is calculated not
     * including outliers in the time difference.<br>
     * <br>
     * 
     * This version uses the given vals to determine the peak indices.
     * 
     * @param vals The array of vals to use.
     * @param fractionOfAverage The fraction of the original average to use in
     *            determining outliers as used in getAveragePeakInterval().
     * @return
     * @see #getAveragePeakInterval
     */
    public static double[] getRsaArray(double[] vals, double fractionOfAverage) {
        return getRsaArray(getPeakIndices(vals), vals, fractionOfAverage);
    }

    /**
     * Calculates an array of values of the time between peaks at the peaks in
     * this instance. The array is a step function. The steps represent the
     * interval between the last peak and the one before it. The steps start at
     * the second peak and NEGATIVE_INFINITY is used for times before that. The
     * values are the difference from the average. The average is calculated not
     * including outliers in the time difference.<br>
     * <br>
     * 
     * This is a generalized version and probably should not be called directly.
     * 
     * @param peakIndices The array of peak indices to use.
     * @param vals The array of vals to use.
     * @param fractionOfAverage The fraction of the original average to use in
     *            determining outliers as used in getAveragePeakInterval().
     * @return
     * @see #getAveragePeakInterval
     */
    public static double[] getRsaArray(int[] peakIndices, double[] vals,
        double fractionOfAverage) {
        int nPeaks = peakIndices.length;
        double avg = getAveragePeakInterval(peakIndices, fractionOfAverage);
        // DEBUG
        // for(int i = 1; i < nPeaks; i++) {
        // System.out.println(i + " " + (peakIndices[i] - peakIndices[i - 1])
        // * INDEX_TO_SEC);
        // }

        // Calculate the array
        double lastPeakVal = Double.NEGATIVE_INFINITY;
        int nDataPoints = vals.length;
        int nextPeak = 0;
        double[] rsaVals = new double[nDataPoints];
        for(int i = 0; i < rsaVals.length; i++) {
            if(i == peakIndices[nextPeak]) {
                // Don't plot anything until the second peak
                if(nextPeak >= 1 && nextPeak < nPeaks) {
                    // Difference from average in seconds
                    lastPeakVal = INDEX_TO_SEC
                        * (peakIndices[nextPeak] - peakIndices[nextPeak - 1] - avg);
                }
                if(nextPeak < nPeaks - 1) {
                    nextPeak++;
                }
            }
            if(lastPeakVal == Double.NEGATIVE_INFINITY) {
                rsaVals[i] = Double.NaN;
            } else {
                rsaVals[i] = lastPeakVal;
            }
        }

        // DEBUG
        // System.out.println(LS + "Peak Indices for Strip " + getStringDate()
        // + " " + getStringTime(false));
        // for(int i = 0; i < peakIndices.length; i++) {
        // System.out.printf("%2d %4d %5.2f %9.4f \n", i, peakIndices[i],
        // getTimeForIndex(peakIndices[i]), rsaVals[peakIndices[i]]);
        // }

        return rsaVals;
    }

    /**
     * Original version, which is not correct. Get the data as an array of data
     * points starting reading at the beginning plus a hard-coded skip value. A
     * skip value of 26 to starts reading just after the 55AA byte.
     * 
     * @return
     */
    @Deprecated
    public double[] getDataAsBytes1() {
        // Calculate the first time, then use the stored values
        if(this.vals != null) {
            return this.vals;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bais);
        // We want to read nVals bytes starting at the end
        int nVals = 7500;
        // Number of bytes to skip (26 goes to the location after 55AA).
        int skip = 26;
        double[] vals = new double[nVals];

        // DEBUG
        System.out.println("getDataAsBytes1");
        System.out.println("  data.length=" + data.length);
        System.out.println("  nVals=" + nVals);
        System.out.println("  skip=" + skip);
        int startOffset = STRIP_START + skip;
        System.out.printf("Starting at location : %x" + LS, startOffset);

        int method = 3;
        byte valRead = 0;
        // i is the location in the vile, j is the location in the data
        int i = -1, j = -1;
        try {
            dis.skip(skip);
            for(i = 0,j = 0; j < nVals; i++,j++) {
                if(skip + i >= data.length) {
                    Utils.errMsg("Trying to read past the end of the data" + LS
                        + "i=" + i + LS + "j=" + j + LS + "i + skip="
                        + (i + skip) + LS + "data.length=" + data.length + LS
                        + "nVals=" + nVals + LS + "skip=" + skip);
                    break;
                }
                // Handle control bytes
                if(i != 1 && (i == 0 || i % 501 == 1)) {
                    j--;

                    // DEBUG
                    // byte controlByte = dis.readByte();
                    // startOffset = STRIP_START + skip + i;
                    // System.out.printf("Control byte at location %x i=%d j=%d:"
                    // + " byte read: %02x byte value: %d" + LS, startOffset,
                    // i, j, controlByte, controlByte);

                    continue;
                }
                switch(method) {
                case 1:
                    vals[i] = valRead = dis.readByte();
                    break;
                case 2:
                    // This has the same result as case 4
                    vals[j] = valRead = dis.readByte();
                    if(vals[j] < 0) {
                        vals[j] = -(128 + vals[j]);
                    }
                    break;
                case 3:
                    // Assumes the device writes the absolute value of the
                    // number and adds a sign bit
                    valRead = dis.readByte();
                    vals[j] = 0x7f & valRead;
                    if(valRead < 0) {
                        vals[j] = -vals[j];
                    }
                    break;
                case 5:
                    vals[j] = dis.readUnsignedByte();
                    valRead = 0;
                    break;
                default:
                    vals[j] = 0;
                }
                // DEBUG
                // if(i < 15 || i == nVals - 1) {
                // startOffset = STRIP_START + skip + i;
                // // DEBUG
                // System.out.printf(
                // "Val at location %x: hex value: %04x byte value:"
                // + " %d used value: " + vals[j] + LS, startOffset,
                // valRead, valRead);
                // }

                // DEBUG
                if(vals[j] > 120 || vals[j] < -120) {
                    startOffset = STRIP_START + skip + i;
                    // DEBUG
                    System.out.printf("Outlier val at location %x index %d:"
                        + " value read: %02x byte value: %d used value: "
                        + vals[j] + LS, startOffset, i, valRead, valRead);

                }

                // Handle controlPoint ?
                // vals[j] += controlByte;
            }
            dis.close();
        } catch(EOFException ex) {
            String msg = "EOF getting strip at i=" + i + " j=" + j + LS
                + "Data length=" + data.length + LS + "nVals=" + nVals + LS
                + "skip=" + skip;
            Utils.excMsg(msg, ex);
            return null;
        } catch(IOException ex) {
            Utils.excMsg("I/O Error getting strip at i=" + i + " j=" + j, ex);
            return null;
        } catch(Exception ex) {
            Utils.excMsg("Error getting strip at i=" + i + " j=" + j, ex);
            return null;
        }
        // DEBUG
        // printTestTable();
        // Only set the internal values if we got to here.
        this.vals = vals;
        return vals;
    }

    /**
     * Get the data assuming it is an array of bytes ending at the end. The
     * number of bytes to read and the offset back from the end are hard coded.
     * This is the original version and gets all the data.
     * 
     * @return
     */
    @Deprecated
    public double[] getAllDataAsBytes() {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bais);
        // We want to read nVals bytes starting at the end
        int nVals = 7500;
        // With a possible offset in bytes toward the beginning
        int offset = 15;
        // The number of bytes we will be using
        int dataSize = 1 * nVals;
        // We need to skip to the start of the values we are using
        int skip = STRIP_LENGTH - dataSize - offset;
        double vals[] = new double[nVals];
        // DEBUG
        System.out.println("getAllDataAsBytes");
        System.out.println("  data.length=" + data.length);
        System.out.println("  nVals=" + nVals);
        System.out.println("  dataSize=" + dataSize);
        System.out.println("  offset=" + offset);
        System.out.println("  skip=" + skip);
        int method = 3;
        byte valRead = 0;
        try {
            dis.skip(skip);
            for(int i = 0; i < nVals; i++) {
                switch(method) {
                case 1:
                    vals[i] = valRead = dis.readByte();
                    break;
                case 2:
                    // This has the same result as case 4
                    vals[i] = valRead = dis.readByte();
                    if(vals[i] < 0) {
                        vals[i] = -(128 + vals[i]);
                    }
                    break;
                case 3:
                    // Assumes the device writes the absolute value of the
                    // number and adds a sign bit
                    valRead = dis.readByte();
                    vals[i] = 0x7f & valRead;
                    if(valRead < 0) {
                        vals[i] = -vals[i];
                    }
                    break;
                case 5:
                    vals[i] = dis.readUnsignedByte();
                    valRead = 0;
                    break;
                default:
                    vals[i] = 0;
                }
                if(i < 15 || i == nVals - 1) {
                    int startOffset = STRIP_START + skip + i;
                    System.out.printf(
                        "Val at location %x: hex value: %04x byte value:"
                            + " %d used value: " + vals[i] + LS, startOffset,
                        valRead, valRead);

                }
                if(vals[i] > 120 || vals[i] < -120) {
                    int startOffset = STRIP_START + skip + i;
                    System.out.printf("Outlier val at location %x index %d:"
                        + " value read: %02x byte value: %d used value: "
                        + vals[i] + LS, startOffset, i, valRead, valRead);

                }
            }
            dis.close();
        } catch(EOFException ex) {
            String msg = "EOF getting strip" + LS + "Data length="
                + data.length + LS + "dataSize=" + dataSize + LS + "skip="
                + skip;
            Utils.excMsg(msg, ex);
            return null;
        } catch(IOException ex) {
            Utils.excMsg("I/O Error getting strip", ex);
            return null;
        } catch(Exception ex) {
            Utils.excMsg("Error getting strip", ex);
            return null;
        }
        // DEBUG
        // printTestTable();
        return vals;
    }

    /**
     * Get the data assuming it is an array of shorts ending at the end. (This
     * is not a correct assumption, and this method should not be used except
     * for testing.)
     * 
     * @return
     */
    @Deprecated
    public double[] getDataAsShorts() {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bais);
        // We want to read nVals shorts starting at the end
        int nVals = 3757;
        // With a possible offset in bytes toward the beginning
        int offset = 0;
        // The number of bytes we will be using
        int dataSize = 2 * nVals;
        // We need to skip to the start of the values we are using
        int skip = STRIP_LENGTH - dataSize - offset;
        double vals[] = new double[nVals];
        // DEBUG
        System.out.println("getDataAsShorts");
        System.out.println("  data.length=" + data.length);
        System.out.println("  nVals=" + nVals);
        System.out.println("  dataSize=" + dataSize);
        System.out.println("  offset=" + offset);
        System.out.println("  skip=" + skip);
        try {
            dis.skip(skip);
            short valRead = 0;
            int method = 9;
            for(int i = 0; i < nVals; i++) {
                switch(method) {
                case 1:
                    vals[i] = dis.readShort();
                    break;
                case 2:
                    vals[i] = dis.readUnsignedShort();
                    break;
                case 3:
                    vals[i] = dis.readBigEndianShort();
                    break;
                case 4:
                    vals[i] = dis.readBigEndianUnsignedShort();
                    break;
                case 8:
                    vals[i] = dis.readShort();
                    if(vals[i] < 0) {
                        vals[i] = 0;
                    }
                    // if(vals[i] < 0) {
                    // vals[i] = vals[i] + 32768;
                    // }
                    break;
                case 9:
                    valRead = dis.readShort();
                    vals[i] = valRead;
                    if(vals[i] < 0) {
                        vals[i] = -(32768 + vals[i]);
                    }
                    break;
                case 10:
                    // Average two bytes
                    byte[] w = new byte[2];
                    dis.readFully(w, 0, 2);
                    vals[i] = .5 * (w[0] + w[1]);
                    break;
                default:
                    vals[i] = 0;
                }
                if(i < 15 || i == nVals - 1) {
                    int startOffset = STRIP_START + skip + 2 * i;
                    System.out.printf("Val at location %x: hex value:"
                        + " %04x short value: %d used value: " + vals[i] + LS,
                        startOffset, valRead, valRead);

                }
            }
            dis.close();
        } catch(EOFException ex) {
            String msg = "EOF getting strip" + LS + "Data length="
                + data.length + LS + "dataSize=" + dataSize + LS + "dataStart="
                + "skip=" + skip;
            Utils.excMsg(msg, ex);
            return null;
        } catch(IOException ex) {
            Utils.excMsg("I/O Error getting strip", ex);
            return null;
        } catch(Exception ex) {
            Utils.excMsg("Error getting strip", ex);
            return null;
        }
        return vals;
    }

    /**
     * Gets a string description of the diagnostic code.
     * 
     * @return
     */
    public String getDiagnosisString() {
        int type = Integer.parseInt(diagnostic);
        switch(type) {
        case 13: // Verified
            // return "Stable waveform";
            return "";
        case 1:
            return "Suspected sinus halted beat";
        case 2:
            return "Suspected fast beat";
        case 3:
            return "Suspected slow beat";
        case 4: // Verified
            return "Suspected missing beat";
        case 5:
            return "Suspected repeating early beat";
        case 6:
            return "Suspected trigemeny";
        case 7:
            return "Suspected bigemeny";
        case 8: // Verified
            return "Suspected R wave on T wave";
        case 9:
            return "Suspected twin early beat";
        case 10:
            return "Suspected early beat";
        case 11: // Verified
            return "Suspected other arrhythmia";
        case 12:
            return "Poor signal";
        default:
            return "Unrecognized diagnostic: " + type;
        }
    }

    /**
     * Gets the date as a String.
     * 
     * @return
     */
    public String getStringDate() {
        return month + "/" + day + "/" + year;
    }

    /**
     * Gets the date as a String.
     * 
     * @param twelveHour Whether to use 12-hour or 24-hour clock
     * @return
     */
    public String getStringTime(Boolean twelveHour) {
        int ihour = Integer.parseInt(hour);
        int imin = Integer.parseInt(min);
        int isec = Integer.parseInt(sec);
        if(twelveHour) {
            String ampm = "AM";
            if(ihour >= 13) {
                ihour -= 12;
                ampm = "PM";
            }
            return String.format("%d:%02d:%02d %s", ihour, imin, isec, ampm);
        } else {
            return String.format("%d:%02d:%02d", ihour, imin, isec);
        }
    }

    /**
     * Prints the first nBytes values from the data.
     * 
     * @param nBytes
     */
    public void printControlPoints() {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bais);
        // We want to read nVals bytes starting at the end
        int nVals = 7500;
        // Number of bytes to skip (26 goes to the location after 55AA).
        int skip = 26;
        int i = -1, j = -1;
        byte controlByte;
        String info1 = "";
        String info2 = "";
        try {
            dis.skip(skip);
            for(i = 0,j = 0; j < nVals; i++,j++) {
                if(skip + i >= data.length) {
                    Utils.errMsg("printControlPoints: "
                        + "Trying to read past the end of the data" + LS + "i="
                        + i + LS + "j=" + j + LS + "i + skip=" + (i + skip)
                        + LS + "data.length=" + data.length + LS + "nVals="
                        + nVals + LS + "skip=" + skip);
                    break;
                }
                // Handle control bytes
                if(i != 1 && (i == 0 || i % 501 == 1)) {
                    j--;
                    controlByte = dis.readByte();
                    int startOffset = STRIP_START + skip + i;
                    info1 += String.format("%4x ", startOffset);
                    info2 += String.format("%4d ", controlByte);
                    continue;
                }
                dis.readByte();
            }
            dis.close();
            if(FIRST) {
                System.out.println(info1);
                FIRST = false;
            }
            System.out.println(info2);
        } catch(EOFException ex) {
            String msg = "EOF getting control points at i=" + i + " j=" + j
                + LS + "Data length=" + data.length + LS + "nVals=" + nVals
                + LS + "skip=" + skip;
            Utils.excMsg(msg, ex);
        } catch(IOException ex) {
            Utils.excMsg("I/O Error getting control points at i=" + i + " j="
                + j, ex);
        } catch(Exception ex) {
            Utils.excMsg("Error getting control points at i=" + i + " j=" + j,
                ex);
        }

    }

    /**
     * Prints the first short values from the data.
     * 
     * @param nShorts
     */
    public void printFirstShorts(int nShorts) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bais);
        short intVal;
        int nRead = 0;
        String info = "";
        try {
            for(int i = 0; i < nShorts; i++) {
                intVal = dis.readShort();
                nRead += 2;
                info += String.format("%5d ", intVal);
            }
            dis.close();
            System.out.println(info);
        } catch(EOFException ex) {
            String available = "Unknown";
            try {
                // This throws an IOException
                available = Integer.toString(dis.available());
            } catch(Exception ex1) {
                // Do nothing
            }
            String msg = "EOF printing first shorts " + LS + "Data length="
                + data.length + LS + "nRead=" + nRead + LS + "dataStart="
                + STRIP_START + LS + "dis.available()=" + available;
            Utils.excMsg(msg, ex);
        } catch(IOException ex) {
            Utils.excMsg("I/O printing first shorts", ex);
        } catch(Exception ex) {
            Utils.excMsg("Error printing first shorts", ex);
        }
    }

    /**
     * Prints the first short values from the data.
     * 
     * @param nBytes
     */
    public void printFirstBytes(int nBytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bais);
        byte val;
        int nRead = 0;
        String info = "";
        try {
            for(int i = 0; i < nBytes; i++) {
                val = dis.readByte();
                nRead++;
                info += String.format("%02X ", val);
            }
            dis.close();
            System.out.println(info);
        } catch(EOFException ex) {
            String available = "Unknown";
            try {
                // This throws an IOException
                available = Integer.toString(dis.available());
            } catch(Exception ex1) {
                // Do nothing
            }
            String msg = "EOF printing first bytes " + LS + "Data length="
                + data.length + LS + "nRead=" + nRead + LS + "dataStart="
                + STRIP_START + LS + "dis.available()=" + available;
            Utils.excMsg(msg, ex);
        } catch(IOException ex) {
            Utils.excMsg("I/O printing first bytes", ex);
        } catch(Exception ex) {
            Utils.excMsg("Error printing first bytes", ex);
        }
    }

    public static void printTestTable() {
        byte val;
        int intVal;
        int intVal2;
        int intVal3;
        int calcVal;
        byte byte1;
        for(int i = 127; i >= -128; i--) {
            val = (byte)i;
            intVal = val;
            intVal2 = (int)(val & 0xFF);
            calcVal = val;
            if(intVal < 0) {
                calcVal = -(129 + val);
            }
            byte1 = (byte)(0xff & val);
            intVal3 = intVal - 127;
            System.out.printf(
                "%3d: %02x %4d | %08x %4d | %08x %4d | %4d %4d %4d" + LS, i,
                val, val, intVal, intVal, intVal2, intVal2, calcVal, byte1,
                intVal3);
        }

    }

    public static void printTestTable1() {
        byte val;
        int intVal;
        int intVal2;
        int intVal3;
        int calcVal;
        byte byte1;
        for(int i = 255; i >= 0; i--) {
            val = (byte)i;
            intVal = val;
            intVal2 = (int)(val & 0xFF);
            calcVal = val;
            if(intVal < 0) {
                calcVal = -(129 + val);
            }
            byte1 = (byte)(0xff & val);
            intVal3 = intVal - 127;
            System.out.printf(
                "%3d: %02x %4d | %08x %4d | %08x %4d | %4d %4d %4d" + LS, i,
                val, val, intVal, intVal, intVal2, intVal2, calcVal, byte1,
                intVal3);
        }

    }

    public static double getTimeForIndex(int index) {
        return .004 * index;
    }

    /**
     * @return The value of year.
     */
    public String getYear() {
        return year;
    }

    /**
     * @return The value of month.
     */
    public String getMonth() {
        return month;
    }

    /**
     * @return The value of day.
     */
    public String getDay() {
        return day;
    }

    /**
     * @return The value of data.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @return The value of hour.
     */
    public String getHour() {
        return hour;
    }

    /**
     * @return The value of min.
     */
    public String getMin() {
        return min;
    }

    /**
     * @return The value of sec.
     */
    public String getSec() {
        return sec;
    }

    /**
     * @return The value of diagnostic.
     */
    public String getDiagnostic() {
        return diagnostic;
    }

    /**
     * @return The value of heartRate.
     */
    public String getHeartRate() {
        return heartRate;
    }

}
