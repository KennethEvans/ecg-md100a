package net.kenevans.ecg.md100a.model;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import net.kenevans.core.utils.Utils;
import net.kenevans.ecg.md100a.ui.EcgStripViewer;

/*
 * Created on Jun 27, 2012
 * By Kenneth Evans, Jr.
 */

/**
 * ModifyFile is a class used to mofify some data in an existing .cEcg file.
 * 
 * @author Kenneth Evans, Jr.
 */
public class ModifyFile
{
    public static final String DEFAULT_DIR = "C:/Users/evans/AppData/Local/VirtualStore/Program Files (x86)/Keep-it-Easy system/EasyData";
    // private static final String INPUT_FILE_NAME = "DisconnectTest.cEcg";
    // private static final String INPUT_FILE_NAME = "DummyUser2.cEcg";
    private static final String INPUT_FILE_NAME = "VoltageTestOrig.cEcg";
    private static final String OUTPUT_FILE_NAME = "VoltageTest.cEcg";
    // private static final String INPUT_FILE_NAME = "2012-06-21.cEcg";
    public static final String INPUT_FILE_PATH = DEFAULT_DIR + "/"
        + INPUT_FILE_NAME;
    public static final String OUTPUT_FILE_PATH = DEFAULT_DIR + "/"
        + OUTPUT_FILE_NAME;

    private static void modify() {
        String fileName = INPUT_FILE_PATH;
        byte[] data;
        try {
            // Read the data
            data = EcgFileModel.openFile(fileName);
            System.out.println("Read: " + fileName);

            // Modify the data
            int start0 = 0x58A;
            int nVals = 12;
            byte startVal = 68;
            int i = start0;
            int nZeros = 39;
            // These should correspond to 512 as the first value;
            data[i++] = 2;
            data[i++] = 0;
            for(int n = 0; n < nVals; n++) {
                for(int j = 0; j < nZeros; j++) {
                    data[i++] = 0;
                }
                if(n == 0) {
                    data[i++] = startVal;
                    data[i++] = startVal;
                    data[i++] = startVal;
                    data[i++] = startVal;
                    data[i++] = startVal;
                }
                data[i] = 1;
                System.out.println("n=" + n + " i=" + i + " val=" + data[i]);
                i++;
            }

            // Write the data to a stream
            fileName = OUTPUT_FILE_PATH;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.write(data);
            dos.close();
            // Write it to the file
            OutputStream outputStream = new FileOutputStream(fileName);
            baos.writeTo(outputStream);
            System.out.println("Wrote: " + fileName);
        } catch(Exception ex) {
            ex.printStackTrace();
            Utils.excMsg("Error reading " + fileName, ex);
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        modify();
        System.out.println("All done");

        // Run EcgStripViewer
        EcgStripViewer.main(args);
    }

}
