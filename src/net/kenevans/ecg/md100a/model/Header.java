package net.kenevans.ecg.md100a.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import net.kenevans.core.utils.Utils;

/*
 * Created on Jun 24, 2012
 * By Kenneth Evans, Jr.
 */

/**
 * Header is a class to manage the header from the MD100A ECG Monitor output
 * file.
 * 
 * @author Kenneth Evans, Jr.
 */
public class Header implements IConstants
{
    private int nStrips;
    private String id;
    private String name;
    private String gender;
    private String birthdate;
    private String height;
    private String weight;
    private String telephone;
    private String address;
    private String allergies;
    private String diagnosis;

    Header(byte[] data) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bais);
        byte[] stringData = null;
        int stringBytes = 0;
        try {
            nStrips = dis.readInt();

            stringBytes = 32;
            stringData = new byte[stringBytes];
            dis.readFully(stringData);
            id = EcgFileModel.getString(stringData);

            stringBytes = 82;
            stringData = new byte[stringBytes];
            dis.readFully(stringData);
            name = EcgFileModel.getString(stringData);

            stringBytes = 20;
            stringData = new byte[stringBytes];
            dis.readFully(stringData);
            gender = EcgFileModel.getString(stringData);

            stringBytes = 112;
            stringData = new byte[stringBytes];
            dis.readFully(stringData);
            birthdate = EcgFileModel.getString(stringData);

            stringBytes = 28;
            stringData = new byte[stringBytes];
            dis.readFully(stringData);
            height = EcgFileModel.getString(stringData);

            stringBytes = 30;
            stringData = new byte[stringBytes];
            dis.readFully(stringData);
            weight = EcgFileModel.getString(stringData);

            stringBytes = 130;
            stringData = new byte[stringBytes];
            dis.readFully(stringData);
            telephone = EcgFileModel.getString(stringData);

            stringBytes = 210;
            stringData = new byte[stringBytes];
            dis.readFully(stringData);
            address = EcgFileModel.getString(stringData);

            stringBytes = 130;
            stringData = new byte[stringBytes];
            dis.readFully(stringData);
            allergies = EcgFileModel.getString(stringData);

            stringBytes = 614; // To end of header, must be at least 542
            stringData = new byte[stringBytes];
            dis.readFully(stringData);
            diagnosis = EcgFileModel.getString(stringData);

            dis.close();
        } catch(IOException ex) {
            Utils.excMsg("Error getting header", ex);
        }
    }

    public Header(int nStrips, String id, String name, String gender,
        String birthdate, String height, String weight, String telephone,
        String address, String allergies, String diagnosis) {
        this.nStrips = nStrips;
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.birthdate = birthdate;
        this.height = height;
        this.weight = weight;
        this.telephone = telephone;
        this.address = address;
        this.allergies = allergies;
        this.diagnosis = diagnosis;
    }

    /**
     * Gets the patient info.
     * 
     * @return
     */
    public String getInfo() {
        String info = "";
        info += "Name: " + getName() + LS;
        info += "Number of strips: " + getNStrips() + LS;
        info += "ID: " + getId() + LS;
        info += "Gender: " + getGender() + LS;
        info += "Birthdate :" + getBirthdate() + LS;
        info += "Height: " + getHeight() + LS;
        info += "Weight: " + getWeight() + LS;
        info += "Telephone: " + getTelephone() + LS;
        info += "Address: " + getAddress() + LS;
        info += "Allergies: " + getAllergies() + LS;
        info += "Diagnosis: " + getDiagnosis() + LS;
        return info;
    }

    public byte[] getData() {
        byte[] data = new byte[HEADER_LENGTH];
        int index = 0;
        EcgFileModel.insertInt(nStrips, data, index);
        index += 4;
        EcgFileModel.insertString(id, data, index, 32);
        index += 32;
        EcgFileModel.insertString(name, data, index, 82);
        index += 82;
        EcgFileModel.insertString(gender, data, index, 20);
        index += 20;
        EcgFileModel.insertString(birthdate, data, index, 112);
        index += 112;
        EcgFileModel.insertString(height, data, index, 28);
        index += 28;
        EcgFileModel.insertString(weight, data, index, 30);
        index += 30;
        EcgFileModel.insertString(telephone, data, index, 130);
        index += 130;
        EcgFileModel.insertString(address, data, index, 210);
        index += 210;
        EcgFileModel.insertString(allergies, data, index, 130);
        index += 130;
        EcgFileModel.insertString(diagnosis, data, index, 614);
        index += 614;

        return data;
    }

    public Header clone() {
        return new Header(nStrips, id, name, gender, birthdate, height, weight,
            telephone, address, allergies, diagnosis);
    }

    /**
     * @return The value of nStrips.
     */
    public int getNStrips() {
        return nStrips;
    }

    /**
     * @param nStrips The new value for nStrips.
     */
    public void setNStrips(int nStrips) {
        this.nStrips = nStrips;
    }

    /**
     * @return The value of id.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id The new value for id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return The value of name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The new value for name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return The value of gender.
     */
    public String getGender() {
        return gender;
    }

    /**
     * @param gender The new value for gender.
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * @return The value of birthdate.
     */
    public String getBirthdate() {
        return birthdate;
    }

    /**
     * @param birthdate The new value for birthdate.
     */
    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    /**
     * @return The value of height.
     */
    public String getHeight() {
        return height;
    }

    /**
     * @param height The new value for height.
     */
    public void setHeight(String height) {
        this.height = height;
    }

    /**
     * @return The value of weight.
     */
    public String getWeight() {
        return weight;
    }

    /**
     * @param weight The new value for weight.
     */
    public void setWeight(String weight) {
        this.weight = weight;
    }

    /**
     * @return The value of telephone.
     */
    public String getTelephone() {
        return telephone;
    }

    /**
     * @param telephone The new value for telephone.
     */
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    /**
     * @return The value of address.
     */
    public String getAddress() {
        return address;
    }

    /**
     * @param address The new value for address.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * @return The value of allergies.
     */
    public String getAllergies() {
        return allergies;
    }

    /**
     * @param allergies The new value for allergies.
     */
    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    /**
     * @return The value of diagnosis.
     */
    public String getDiagnosis() {
        return diagnosis;
    }

    /**
     * @param diagnosis The new value for diagnosis.
     */
    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

}
