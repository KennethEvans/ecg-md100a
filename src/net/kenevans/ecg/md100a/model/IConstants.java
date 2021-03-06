package net.kenevans.ecg.md100a.model;

/*
 * Created on Jul 9, 2012
 * By Kenneth Evans, Jr.
 */

/**
 * Provides constants for classes related to the MD100A ECG Monitor.
 * 
 * @author Kenneth Evans, Jr.
 */
public interface IConstants
{
    public static final String LS = System.getProperty("line.separator");

    public static final String DEFAULT_DIR = "C:/Users/evans/AppData/Local/VirtualStore/Program Files (x86)/Keep-it-Easy system/EasyData";
    // public static final String FILE_NAME = "DisconnectTest.cEcg";
    // public static final String FILE_NAME = "2012-06-21.cEcg";
    // public static final String FILE_NAME = "DummyUser2.cEcg";
    // public static final String FILE_NAME = "DummyUser3.cEcg";
    // public static final String FILE_NAME = "VoltageTest.cEcg";
    // public static final String FILE_NAME = "2012-06-28-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2012-07-09-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2012-07-13-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2012-07-17-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2012-07-20-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2012-07-30-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2012-08-01-UsingLeads.cEcg";
    // public static final String FILE_NAME = "2012-08-01-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2012-08-07-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2012-09-24-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2013-10-11-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2013-12-13-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2014-05-18-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2014-06-06-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2014-06-23-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2014-06-30-AllFromDevice.cEcg";
    // public static final String FILE_NAME = "2014-07-20-AllFromDevice.cEcg";
    public static final String FILE_NAME = "2015-02-08-AllFromDevice.cEcg";

    public static final String FILE_PATH = DEFAULT_DIR + "/" + FILE_NAME;
    public static final String DEFAULT_SAVE_FILE_NAME = "SavedFile.cEcg";

    // A file consists of a header and 1 or more strips. The strips consist of a
    // header and data. The data are broken up into segments. Each segment has
    // one more byte than the number of data points it represents.

    /** Length of the header. */
    public static int HEADER_LENGTH = 1392;
    /** Length of a strip waveform. */
    public static int STRIP_LENGTH = 7541;
    /** Number of data points in a strip waveform. */
    public static int STRIP_N_DATA_VALS = 7500;
    /** Number of bytes in a strip waveform. */
    public static int STRIP_N_DATA_BYTES = 7515;
    /** Number of segments in a strip waveform. */
    public static int STRIP_N_DATA_SEGMENTS = 15;
    /** Number of data points in a strip segment. */
    public static int SEGMENT_N_DATA_VALS = 500;
    /** Length of a single segment in a strip waveform. */
    public static int SEGMENT_LENGTH = 501;
    /** Start byte of the first strip. */
    public static int STRIP_START = 1392;
    /** Sample rate in Hz. */
    public static int SAMPLE_RATE = 250;
    /** Strip Sample time in seconds. */
    public static int STRIP_SAMPLE_TIME = 30;
    /**
     * Start byte of the data block in the strip. This is after the 55AA bytes
     * indicating the start of the data. Skip this number of bytes in the strip
     * to get to the start of the data.
     */
    public static int STRIP_DATA_START = 26;

    /** The maximum patient ID. */
    public final long MAX_PATIENT_ID = 999999999999999L;

    /** Conversion from index to seconds. */
    public static final double INDEX_TO_SEC = (double)STRIP_SAMPLE_TIME
        / STRIP_N_DATA_VALS;

    /** Conversion from bytes to mm */
    public static final double mmPerUnit = .0525;

    /** The fraction to use in determining the average baseline for RSA values. */
    public static double RSA_AVG_OUTLIER_FRACTION = .2;

}
