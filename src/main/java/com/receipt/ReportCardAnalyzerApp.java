package com.receipt;


import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.regex.*;


public class ReportCardAnalyzerApp extends Frame {


    private TextArea textArea;
    private Button uploadButton, saveButton;
    private Panel chartContainer;


    public ReportCardAnalyzerApp() {
        setTitle("Student Report Card Scanner & Performance Analyzer");
        setSize(1000, 600);
        setLayout(new BorderLayout());


        Panel contentPanel = new Panel(new GridLayout(1, 2)); // Split layout: text and chart
        textArea = new TextArea();
        contentPanel.add(textArea);


        chartContainer = new Panel(new BorderLayout());
        chartContainer.setPreferredSize(new Dimension(400, 400));
        contentPanel.add(chartContainer);


        add(contentPanel, BorderLayout.CENTER);


        Panel buttonPanel = new Panel(new FlowLayout());
        uploadButton = new Button("Upload Report Card");
        saveButton = new Button("Save as PDF");


        buttonPanel.add(uploadButton);
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);


        uploadButton.addActionListener(e -> chooseFile());
        saveButton.addActionListener(e -> saveReportToFile());


        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });


        setVisible(true);
    }


    private void chooseFile() {
        FileDialog dialog = new FileDialog(this, "Choose a report card image", FileDialog.LOAD);
        dialog.setVisible(true);


        String directory = dialog.getDirectory();
        String filename = dialog.getFile();


        if (filename != null) {
            File selectedFile = new File(directory, filename);


            try {
                Tesseract tesseract = new Tesseract();
                tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
                tesseract.setLanguage("eng");


                String rawText = tesseract.doOCR(selectedFile);


                HashMap<String, String> studentDetails = extractStudentDetails(rawText);
                HashMap<String, Integer> marksMap = extractMarks(rawText);


                if (marksMap.isEmpty()) {
                    textArea.setText(" No valid subject-mark pairs found.");
                    return;
                }


                displayFormattedResult(studentDetails, marksMap);
                showPieChart(marksMap);


            } catch (TesseractException e) {
                textArea.setText(" OCR failed: " + e.getMessage());
            }
        }
    }


    // Method to extract student details (like name, year, department) from raw text
    private HashMap<String, String> extractStudentDetails(String rawText) {
        HashMap<String, String> details = new HashMap<>();
        String[] lines = rawText.split("\\n");


        Pattern namePattern = Pattern.compile("(?i)\\bname\\b\\s*[:\\-]\\s*(.+)");
        Pattern yearPattern = Pattern.compile("(?i)\\byear\\b\\s*[:\\-]\\s*(\\d{4})");
        Pattern deptPattern = Pattern.compile("(?i)(course|department|stream|program)\\s*[:\\-]\\s*(.+)");


        for (String line : lines) {
            line = line.trim();
            Matcher m;


            m = namePattern.matcher(line);
            if (m.find() && !details.containsKey("Name") && !line.toLowerCase().contains("teacher")) {
                details.put("Name", m.group(1).trim());
            }


            m = yearPattern.matcher(line);
            if (m.find()) {
                details.put("Year", m.group(1).trim());
            }


            m = deptPattern.matcher(line);
            if (m.find()) {
                details.put("Department", m.group(2).trim());
            }
        }


        return details;
    }


    // Method to extract marks (subject-wise) from the raw text
    private HashMap<String, Integer> extractMarks(String rawText) {
        HashMap<String, Integer> marksMap = new HashMap<>();
        String[] lines = rawText.split("\\n");


        Pattern markPattern = Pattern.compile("([A-Za-z &]+)\\s+(\\d{1,3})");


        for (String line : lines) {
            line = line.trim();


            if (line.toLowerCase().contains("report card") || line.length() < 3
                    || line.toLowerCase().contains("teacher") || line.toLowerCase().contains("student")
                    || line.toLowerCase().contains("name") || line.toLowerCase().contains("year")
                    || line.toLowerCase().contains("department") || line.toLowerCase().contains("course"))
                continue;


            Matcher matcher = markPattern.matcher(line);
            if (matcher.find()) {
                String subject = matcher.group(1).trim();
                try {
                    int marks = Integer.parseInt(matcher.group(2));
                    if (marks <= 100 && marks >= 20)
                        marksMap.put(subject, marks);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return marksMap;
    }


    // Method to format and display the extracted results
    private void displayFormattedResult(HashMap<String, String> details, HashMap<String, Integer> marksMap) {
        StringBuilder result = new StringBuilder();


        result.append("📋 STUDENT REPORT ANALYSIS\n");
        result.append("=================================\n");
       // System.out.println("added for github");

        result.append("👤 Student Details:\n");
        result.append("------------------------\n");
        result.append("Name           : ").append(details.getOrDefault("Name", "N/A")).append("\n");
        if (details.containsKey("Department"))
            result.append("Department     : ").append(details.get("Department")).append("\n");
        if (details.containsKey("Year"))
            result.append("Year           : ").append(details.get("Year")).append("\n");


        result.append("\n🎯 SUBJECT-WISE MARKS\n");
        result.append("---------------------------------\n");


        int total = 0, highest = Integer.MIN_VALUE, lowest = Integer.MAX_VALUE;
        String topSubject = "", lowSubject = "";


        for (Map.Entry<String, Integer> entry : marksMap.entrySet()) {
            String subject = entry.getKey();
            int mark = entry.getValue();
            result.append(String.format("%-25s : %3d\n", subject, mark));
            total += mark;


            if (mark > highest) {
                highest = mark;
                topSubject = subject;
            }
            if (mark < lowest) {
                lowest = mark;
                lowSubject = subject;
            }
        }


        double average = (double) total / marksMap.size();
        String grade;
        if (average >= 90) grade = "A (Excellent)";
        else if (average >= 75) grade = "B (Good)";
        else if (average >= 60) grade = "C (Average)";
        else grade = "D (Needs Improvement)";


        result.append("\n---------------------------------\n");
        result.append(String.format(" Total Marks       : %d\n", total));
        result.append(String.format(" Average Score     : %.2f\n", average));
        result.append(String.format(" Final Grade       : %s\n", grade));
        result.append(String.format(" Top Subject       : %s (%d)\n", topSubject, highest));
        result.append(String.format(" Lowest Subject    : %s (%d)\n", lowSubject, lowest));
        result.append("\n Remarks           : ").append(generateRemark(average));


        textArea.setText(result.toString());
    }



    private String generateRemark(double average) {
        if (average >= 90) return "Outstanding performance! Keep it up.";
        else if (average >= 75) return "Great job! Aim for excellence.";
        else if (average >= 60) return "Good effort, but there's room for improvement.";
        else return "Needs serious attention. Work harder and seek help.";
    }


    // Method to show a pie chart based on subject-wise marks
    private void showPieChart(HashMap<String, Integer> marksMap) {
        chartContainer.removeAll();


        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Integer> entry : marksMap.entrySet()) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }


        JFreeChart chart = ChartFactory.createPieChart(
                "Subject-wise Marks Distribution",
                dataset, true, true, false);


        ChartPanel chartPanel = new ChartPanel(chart);
        chartContainer.add(chartPanel, BorderLayout.CENTER);


        chartContainer.revalidate();
        chartContainer.repaint();
    }


    // Method to convert the pie chart to an image
    private BufferedImage chartToImage() {
        try {
            ChartPanel chartPanel = (ChartPanel) chartContainer.getComponent(0);
            JFreeChart chart = chartPanel.getChart();
            return chart.createBufferedImage(500, 500);
        } catch (Exception e) {
            return null;
        }
    }


    // Method to save the report as a PDF file
    private void saveReportToFile() {
        FileDialog dialog = new FileDialog(this, "Save Report as PDF", FileDialog.SAVE);
        dialog.setFile("report.pdf");
        dialog.setVisible(true);


        String directory = dialog.getDirectory();
        String filename = dialog.getFile();


        if (filename != null) {
            if (!filename.toLowerCase().endsWith(".pdf")) {
                filename += ".pdf";
            }


            String filePath = directory + filename;
            Document document = new Document();


            try {
                PdfWriter.getInstance(document, new FileOutputStream(filePath));
                document.open();
                document.add(new Paragraph(textArea.getText()));


                BufferedImage image = chartToImage();
                if (image != null) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    ImageIO.write(image, "PNG", byteArrayOutputStream);
                    Image chartImage = Image.getInstance(byteArrayOutputStream.toByteArray());
                    chartImage.scaleToFit(500, 500);
                    document.add(chartImage);
                }


                document.close();
                textArea.append("\n\n✅ PDF saved to: " + filePath);
            } catch (DocumentException | IOException e) {
                textArea.append("\n❌ Error saving PDF: " + e.getMessage());
            }
        }
    }


    public static void main(String[] args) {
        new ReportCardAnalyzerApp();
    }
}



















