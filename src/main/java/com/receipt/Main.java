package com.receipt;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class Main extends Frame {

    private TextArea textArea;
    private Button uploadButton;

    public Main() {
        setTitle("Receipt Scanner & Analyzer");
        setSize(600, 400);
        setLayout(new BorderLayout());

        // TextArea for extracted text
        textArea = new TextArea();
        add(textArea, BorderLayout.CENTER);

        // Upload button
        uploadButton = new Button("Upload Receipt");
        add(uploadButton, BorderLayout.SOUTH);

        uploadButton.addActionListener(e -> chooseFile());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        setVisible(true);
    }

    private void chooseFile() {
        FileDialog dialog = new FileDialog(this, "Choose a receipt image", FileDialog.LOAD);
        dialog.setVisible(true);

        String directory = dialog.getDirectory();
        String filename = dialog.getFile();

        if (filename != null) {
            File selectedFile = new File(directory, filename);
            textArea.setText("Selected: " + selectedFile.getAbsolutePath());
            // OCR and processing will happen here in later steps
        }
    }

    public static void main(String[] args) {
        new Main();
    }
}
