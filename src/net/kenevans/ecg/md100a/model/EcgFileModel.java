package net.kenevans.ecg.md100a.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.kenevans.core.utils.Utils;

/*
 * Created on Jun 24, 2012
 * By Kenneth Evans, Jr.
 */

/**
 * EcgFileModel is a model for data from the MD100A ECG Monitor.
 * 
 * @author Kenneth Evans, Jr.
 */
public class EcgFileModel implements IConstants
{
    private byte[] data;
    private String fileName;
    Header header;
    private Strip[] strips;
    private int nStrips;

    public EcgFileModel(String fileName) {
        this.fileName = fileName;
        try {
            this.data = openFile(fileName);
            byte[] headerBytes = getHeaderBytes(data);
            header = new Header(headerBytes);
            // Make the strips
            nStrips = header.getNStrips();
            strips = new Strip[nStrips];
            for(int i = 0; i < nStrips; i++) {
                strips[i] = new Strip(getStripBytes(i, data));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            Utils.excMsg("Error reading " + fileName, ex);
        }
    }

    private byte[] getHeaderBytes(byte[] data) throws IOException {
        int len = HEADER_LENGTH;
        byte[] bytes = new byte[len];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bais);
        dis.readFully(bytes);
        dis.close();
        return bytes;
    }

    private byte[] getStripBytes(int nStrip, byte[] data) throws IOException {
        int len = STRIP_LENGTH;
        byte[] bytes = new byte[len];
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bais);
        dis.skip(HEADER_LENGTH);
        // Skip over the others
        for(int i = 0; i < nStrip; i++) {
            dis.skip(STRIP_LENGTH);
        }
        // System.out.println("data.length=" + data.length + LS +
        // "STRIP_LENGTH="
        // + STRIP_LENGTH + LS + "dis.available()=" + dis.available());
        dis.readFully(bytes);
        dis.close();
        return bytes;
    }

    private String getInfo() {
        String info = "";
        info += "File name: " + fileName + LS;
        info += "Number of strips : " + header.getNStrips() + LS;
        info += "Name: " + header.getName() + LS;
        info += "Gender: " + header.getGender() + LS;
        info += "Birthdate: " + header.getBirthdate() + LS;
        info += "Height: " + header.getHeight() + LS;
        info += "Weight: " + header.getWeight() + LS;
        info += "Telephone: " + header.getTelephone() + LS;
        info += "Address: " + header.getAddress() + LS;
        info += "Allergies: " + header.getAllergies() + LS;
        info += "Diagnosis: " + header.getDiagnosis() + LS;

        for(int i = 0; i < nStrips; i++) {
            info += LS;
            info += "Strip " + (i + 1) + LS;
            info += "  Date:  " + strips[i].getStringDate() + LS;
        }

        return info;
    }

    /**
     * Reads the file using a LittleEndianDataInputStream.
     * 
     * @param fileName
     * @return The bytes in the file.
     * @throws IOException
     */
    public static byte[] openFile(String fileName) throws IOException {
        File file = new File(fileName);
        int len = (int)file.length();
        byte[] data = new byte[len];
        FileInputStream fis = new FileInputStream(file);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(fis);
        dis.readFully(data);
        dis.close();
        return data;
    }

    /**
     * Writes the file using the given bytes.
     * 
     * @param file
     * @param saveData
     * @throws IOException
     */
    public static void saveFile(File file, byte[] saveData) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.write(saveData);
        dos.close();
        // Write it to the file (This CTOR does not append)
        OutputStream outputStream = new FileOutputStream(file);
        baos.writeTo(outputStream);
    }

    public static String getString(byte[] bytes) throws IOException {
        String string = "";
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bais);
        // Didn't work
        // string = dis.readUTF();
        char ch = 0;
        try {
            while(true) {
                ch = dis.readChar();
                if(ch == 0) {
                    break;
                }
                string += Character.toString(ch);
            }
        } catch(EOFException e) {
            // Do nothing
        }
        dis.close();
        return string;
    }

    /**
     * Inserts the given string into the given array. Does not check for
     * overflow.
     * 
     * @param string The string to insert.
     * @param bytes The array in which to insert it.
     * @param start The index of the array to start inserting.
     * @param maxBytes The maximum number of bytes to write into the array
     *            including the two bytes for the null at the end. Must be even.
     */
    public static void insertString(String string, byte[] bytes, int start,
        int maxBytes) {
        int maxLen = maxBytes / 2 - 1;
        int i;
        char ch;
        int index = start;
        for(i = 0; i < string.length(); i++) {
            if(i >= maxLen) {
                break;
            }
            ch = string.charAt(i);
            insertChar(ch, bytes, index);
            index += 2;
        }
        // Write the null
        insertChar((char)0, bytes, index);
        index += 2;
    }

    /**
     * Inserts the given char into the given array. Does not check for overflow.
     * 
     * @param val The char to insert.
     * @param bytes The array in which to insert it.
     * @param start The index of the array to start inserting.
     */
    public static void insertChar(char val, byte[] bytes, int start) {
        int index = start;
        bytes[index++] = (byte)(val & 0xff);
        bytes[index++] = (byte)((val >> 8) & 0xff);
    }

    /**
     * Inserts the given short into the given array. Does not check for
     * overflow.
     * 
     * @param val The short to insert.
     * @param bytes The array in which to insert it.
     * @param start The index of the array to start inserting.
     */
    public static void insertShort(short val, byte[] bytes, int start) {
        int index = start;
        bytes[index++] = (byte)(val & 0xff);
        bytes[index++] = (byte)((val >> 8) & 0xff);
    }

    /**
     * Inserts the given int into the given array. Does not check for overflow.
     * 
     * @param val The int to insert.
     * @param bytes The array in which to insert it.
     * @param start The index of the array to start inserting.
     */
    public static void insertInt(int val, byte[] bytes, int start) {
        int index = start;
        bytes[index++] = (byte)(val & 0xff);
        bytes[index++] = (byte)((val >> 8) & 0xff);
        bytes[index++] = (byte)((val >> 16) & 0xff);
        bytes[index++] = (byte)((val >> 24) & 0xff);
    }

    /**
     * @return The value of data.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @return The value of fileName.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return The value of header.
     */
    public Header getHeader() {
        return header;
    }

    /**
     * @return The value of strips.
     */
    public Strip[] getStrips() {
        return strips;
    }

    /**
     * @return The value of nStrips.
     */
    public int getNStrips() {
        return nStrips;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("Starting " + EcgFileModel.class.getName());
        EcgFileModel app = new EcgFileModel(FILE_PATH);
        // TODO Auto-generated method stub
        System.out.println(app.getInfo());
        System.out.println();
        System.out.println("All Done");
    }

}
