package application;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;

public class BibleReaderController {
    @FXML private ComboBox<String> bookSelector;
    @FXML private ComboBox<Integer> chapterSelector;
    @FXML private TextArea verseDisplay;

    private final Map<String, List<Element>> bookChapterMap = new LinkedHashMap<>();
    private final File progressFile = new File("progress.properties");
    private boolean isLoading = false;

    @FXML
    public void initialize() {
        try {
            // Load and parse XML
            InputStream is = getClass().getResourceAsStream("/EnglishESVBible.xml");
            if (is == null) throw new RuntimeException("EnglishESVBible.xml not found in resources.");

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            // Populate book/chapter map
            NodeList books = doc.getElementsByTagName("book");
            for (int i = 0; i < books.getLength(); i++) {
                Element book = (Element) books.item(i);
                String bookNum = book.getAttribute("number");
                NodeList chapters = book.getElementsByTagName("chapter");

                List<Element> chapterList = new ArrayList<>();
                for (int j = 0; j < chapters.getLength(); j++) {
                    chapterList.add((Element) chapters.item(j));
                }

                String bookLabel = "Book " + bookNum;
                bookChapterMap.put(bookLabel, chapterList);
            }

            // Setup UI components
            bookSelector.getItems().addAll(bookChapterMap.keySet());
            bookSelector.setOnAction(e -> updateChapters());
            chapterSelector.setOnAction(e -> displayChapter());

            // Load saved progress
            loadProgress();
        } catch (Exception e) {
            verseDisplay.setText("Error initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateChapters() {
        String selectedBook = bookSelector.getValue();
        chapterSelector.getItems().clear();
        if (selectedBook != null) {
            int totalChapters = bookChapterMap.get(selectedBook).size();
            for (int i = 1; i <= totalChapters; i++) {
                chapterSelector.getItems().add(i);
            }
            chapterSelector.getSelectionModel().selectFirst();
            
            // Only auto-display when not loading saved progress
            if (!isLoading) displayChapter();
        }
    }

    private void displayChapter() {
        String selectedBook = bookSelector.getValue();
        Integer selectedChapter = chapterSelector.getValue();
        if (selectedBook == null || selectedChapter == null) return;

        Element chapter = bookChapterMap.get(selectedBook).get(selectedChapter - 1);
        NodeList verses = chapter.getElementsByTagName("verse");

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < verses.getLength(); i++) {
            Element verse = (Element) verses.item(i);
            String number = verse.getAttribute("number");
            String text = verse.getTextContent().trim();
            content.append(number).append(". ").append(text).append("\n\n");
        }

        verseDisplay.setText(content.toString());
        saveProgress();
    }

    private void saveProgress() {
    	System.out.println("Saving progress to: " + progressFile.getAbsolutePath());
        String book = bookSelector.getValue();
        Integer chapter = chapterSelector.getValue();
        if (book == null || chapter == null) return;

        Properties props = new Properties();
        props.setProperty("book", book);
        props.setProperty("chapter", chapter.toString());

        try (FileOutputStream out = new FileOutputStream(progressFile)) {
            props.store(out, "Bible Reading Progress");
        } catch (Exception e) {
            System.err.println("Failed to save progress: " + e.getMessage());
        }
    }

    private void loadProgress() {
        if (!progressFile.exists()) return;
        
        isLoading = true;
        Properties props = new Properties();
        
        try (FileInputStream in = new FileInputStream(progressFile)) {
            props.load(in);
            String book = props.getProperty("book");
            String chapterStr = props.getProperty("chapter");

            if (book != null && chapterStr != null && bookChapterMap.containsKey(book)) {
                bookSelector.getSelectionModel().select(book);
                updateChapters(); // Populates chapters
                chapterSelector.getSelectionModel().select(Integer.valueOf(chapterStr));
                displayChapter();
            }
        } catch (Exception e) {
            System.err.println("Failed to load progress: " + e.getMessage());
        } finally {
            isLoading = false;
        }
    }
    
    @FXML
    private void resetProgress() {
        // This delete the progress file to reset progress hehe
        if (progressFile.exists()) {
            if (progressFile.delete()) {
                // Reset UI to default state
                bookSelector.getSelectionModel().clearSelection();
                chapterSelector.getItems().clear();
                verseDisplay.clear();
                showAlert("Progress Reset", "Your reading progress has been reset.");
            } else {
                showAlert("Error", "Failed to reset progress.");
            }
        } else {
            showAlert("Info", "No progress to reset.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}