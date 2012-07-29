package net.kenevans.ecg.md100a.ui;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.kenevans.core.utils.Utils;
import net.kenevans.ecg.md100a.model.EcgFilterModel;
import net.kenevans.ecg.md100a.model.IConstants;

/**
 * FilterOptionsDialog
 * 
 * @author Kenneth Evans, Jr.
 */
public class SaveFileDialog extends JDialog implements IConstants
{
    private static final long serialVersionUID = 1L;

    private EcgStripViewer viewer;

    JTextField fileNameText;
    JTextField stripSpecText;
    JTextField idText;
    JComboBox dataModeCombo;

    /**
     * Constructor
     */
    public SaveFileDialog(EcgStripViewer mainFrame) {
        super();
        this.viewer = mainFrame;
        init();
        reset();
        // Locate it on the screen
        this.setLocation(425, 490);
    }

    /**
     * This method initializes this dialog
     * 
     * @return void
     */
    private void init() {
        this.setTitle("Save Options");
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new GridBagLayout());

        GridBagConstraints gbcDefault = new GridBagConstraints();
        gbcDefault.insets = new Insets(2, 2, 2, 2);
        gbcDefault.weightx = 100;
        gbcDefault.gridx = 1;
        gbcDefault.anchor = GridBagConstraints.WEST;
        gbcDefault.fill = GridBagConstraints.NONE;
        // gbcDefault.fill = GridBagConstraints.HORIZONTAL;
        GridBagConstraints gbc = null;

        // Filename panel
        gbc = (GridBagConstraints)gbcDefault.clone();
        JPanel fileNamePanel = new JPanel();
        contentPane.add(fileNamePanel, gbc);

        JLabel label = new JLabel("Filename:");
        label.setToolTipText("The name of the file to be written.");
        fileNamePanel.add(label);

        fileNameText = new JTextField(30);
        fileNameText.setToolTipText("The name of the file to be written.");
        fileNamePanel.add(fileNameText);

        JButton button = new JButton();
        button.setText("Browse");
        button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                if(viewer.defaultSavePath != null) {
                    chooser
                        .setCurrentDirectory(new File(viewer.defaultOpenPath));
                }
                int result = chooser.showOpenDialog(viewer);
                if(result == JFileChooser.APPROVE_OPTION) {
                    // Save the selected path for next time
                    viewer.defaultSavePath = chooser.getSelectedFile()
                        .getParentFile().getPath();
                    // Process the file
                    File file = chooser.getSelectedFile();
                    // Set the cursor in case it takes a long time
                    // This isn't working. The cursor apparently doesn't get set
                    // until after it is done.
                    Cursor oldCursor = getCursor();
                    try {
                        setCursor(Cursor
                            .getPredefinedCursor(Cursor.WAIT_CURSOR));
                        fileNameText.setText(file.getPath());
                    } finally {
                        setCursor(oldCursor);
                    }
                }
            }
        });
        fileNamePanel.add(button);

        // Id panel
        JPanel idPanel = new JPanel();
        gbc = (GridBagConstraints)gbcDefault.clone();
        contentPane.add(idPanel, gbc);

        label = new JLabel("Patient ID:");
        label.setToolTipText("The patient ID (1 - " + MAX_PATIENT_ID + ".");
        idPanel.add(label);

        idText = new JTextField(5);
        idText.setToolTipText("The patient ID (1 - " + MAX_PATIENT_ID + ".");
        idPanel.add(idText);

        // Strip specification panel
        JPanel stripScecPanel = new JPanel();
        gbc = (GridBagConstraints)gbcDefault.clone();
        contentPane.add(stripScecPanel, gbc);

        label = new JLabel("Strips to use:");
        label.setToolTipText("Comma-separated list of valid strip numbers. "
            + "Example: 1-2, 3, 9-10");
        stripScecPanel.add(label);

        stripSpecText = new JTextField(15);
        stripSpecText
            .setToolTipText("Comma-separated list of valid strip numbers. "
                + "Example: 1-2, 3, 9-10");
        stripScecPanel.add(stripSpecText);

        // Data mode panel
        JPanel dataModePanel = new JPanel();
        gbc = (GridBagConstraints)gbcDefault.clone();
        contentPane.add(dataModePanel, gbc);

        label = new JLabel("Data Mode:");
        label.setToolTipText("The data mode to use for conversion.");
        dataModePanel.add(label);

        String[] comboItems = new String[EcgFilterModel.dataModeList.length];
        for(int i = 0; i < comboItems.length; i++) {
            comboItems[i] = EcgFilterModel.dataModeList[i].getName();
        }
        dataModeCombo = new JComboBox(comboItems);
        dataModeCombo.setToolTipText("The data mode to use for conversion.");
        dataModePanel.add(dataModeCombo);

        // Button panel
        JPanel buttonPanel = new JPanel();
        gbc = (GridBagConstraints)gbcDefault.clone();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        contentPane.add(buttonPanel, gbc);

        button = new JButton();
        button.setText("OK");
        button.setToolTipText("Check validity and if valid, save the file, "
            + "and close the dialog.");
        button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                boolean result = apply();
                if(result) {
                    SaveFileDialog.this.setVisible(false);
                }
            }
        });
        buttonPanel.add(button);

        button = new JButton();
        button.setText("Reset");
        button.setToolTipText("Reset the options to the original values.");
        button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                reset();
            }
        });
        buttonPanel.add(button);

        button = new JButton();
        button.setText("Cancel");
        button.setToolTipText("Close the dialog and do nothing.");
        button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                SaveFileDialog.this.setVisible(false);
            }
        });
        buttonPanel.add(button);

        // isModified = false;
        // this.setName("saveFileDialog");
        // this.setModal(false);
        // this.setTitle("Save File");
        // this.setContentPane(getJContentPane());
        pack();
    }

    /**
     * Resets the internal state from the EcgStripViewer. Can also be used to
     * initialize the dialog.
     */
    public void reset() {
        String text;
        if(fileNameText != null) {
            text = viewer.defaultSavePath + "/" + DEFAULT_SAVE_FILE_NAME;
            fileNameText.setText(text);
        }
        if(idText != null) {
            // TODO Perhaps there is a better default
            text = "2";
            idText.setText(text);
        }
        if(stripSpecText != null) {
            int len = viewer.strips.length;
            if(len == 0) {
                text = "No strips available";
            } else if(len == 1) {
                text = "" + len;
            } else {
                text = "1-" + len;
            }
            stripSpecText.setText(text);
        }
        if(dataModeCombo != null) {
            dataModeCombo.setSelectedItem(viewer.getFilterModel().getDataMode()
                .getName());
        }
    }

    /**
     * Collects the values of the components, and if they are valid, then calls
     * the saveFile method in the viewer.
     * 
     * @return True on success to close the dialog or false otherwise to leave
     *         the dialog up.
     */
    public boolean apply() {
        // Filename
        String text;
        text = fileNameText.getText();
        File file = new File(text);
        if(file.exists()) {
            int result = JOptionPane.showConfirmDialog(this, "File exists:"
                + LS + file.getPath() + LS + "OK to overwrite?", "File Exists",
                JOptionPane.OK_CANCEL_OPTION);
            if(result != JOptionPane.OK_OPTION) {
                return false;
            }
        }

        // Patient Id
        String id = idText.getText();
        long longVal = -1;
        try {
            longVal = Long.parseLong(id);
        } catch(Exception ex) {
            Utils.excMsg("Invalid value for Patient ID", ex);
            return false;
        }
        if(longVal < 1 || longVal > MAX_PATIENT_ID) {
            Utils.errMsg("Invalid Patient ID: " + id);
            return false;
        }

        // Strip specification
        text = stripSpecText.getText().replaceAll("\\s", "");
        String[] tokens = text.split(",");
        int maxVal = viewer.strips.length;
        String msg = "Invalid strip specification or invalid index" + LS
            + "(Check the tooltip)";
        // Collect the indices in a HashSet to remove duplicates
        Set<Integer> noDupsList = new HashSet<Integer>();
        String[] startEnd;
        int start, end;
        for(String token : tokens) {
            startEnd = token.split("-");
            if(startEnd.length == 1) {
                start = isValidPositiveInteger(startEnd[0], maxVal);
                if(start < 0) {
                    Utils.errMsg(msg);
                    return false;
                }
                noDupsList.add(start - 1);
            } else if(startEnd.length == 2) {
                start = isValidPositiveInteger(startEnd[0], maxVal);
                if(start < 0) {
                    Utils.errMsg(msg);
                    return false;
                }
                end = isValidPositiveInteger(startEnd[1], maxVal);
                if(end < 0) {
                    Utils.errMsg(msg);
                    return false;
                }
                for(int i = start; i <= end; i++) {
                    noDupsList.add(i - 1);
                }
            } else {
                Utils.errMsg(msg);
                return false;
            }
        }
        // Make it an ArrayList so we can sort it
        List<Integer> stripList = new ArrayList<Integer>(noDupsList);
        Collections.sort(stripList);

        // Data mode
        int index = dataModeCombo.getSelectedIndex();
        if(index == -1) {
            Utils.errMsg("Nothing selected for Data Mode");
            return false;
        }
        if(index < 0 || index >= EcgFilterModel.dataModeList.length) {
            Utils.errMsg("Got invalid index for Data Mode: " + index);
            return false;
        }
        EcgFilterModel.DataMode dataMode = EcgFilterModel.dataModeList[index];

        // Call the saveFile method of the viewer
        viewer.saveFile(file, id, stripList, dataMode);
        // Return true so the dialog will go away
        return true;
    }

    /**
     * Checks if the given string represents a positive integer
     * 
     * @param stringVal The String to check;
     * @param maxVal The maximum value the integer can have.
     * @return The integer on success or -1 on failure.
     */
    private static int isValidPositiveInteger(String stringVal, int maxVal) {
        int intVal;
        try {
            intVal = Integer.parseInt(stringVal);
        } catch(Exception ex) {
            return -1;
        }
        if(intVal > 0 && intVal <= maxVal) {
            return intVal;
        } else {
            return -1;
        }
    }

}
