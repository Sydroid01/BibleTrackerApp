package org.openjfx.BibleReadingTrackerFX;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

public class BibleReaderController {
    @FXML private ComboBox<String> bookSelector;
    @FXML private ComboBox<Integer> chapterSelector;
    @FXML private TextArea verseDisplay;

    private final Map<String, List<Element>> bookChapterMap = new LinkedHashMap<>();
    private final File progressFile = new File("progress.properties");
    private final File bookmarksFile = new File("bookmarks.properties");

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
            bookSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    chapterSelector.getItems().clear();
                    int chapters = bookChapterMap.get(newVal).size();
                    IntStream.rangeClosed(1, chapters).forEach(chapterSelector.getItems()::add);
                }
            });

            chapterSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && bookSelector.getValue() != null) {
                    displayChapterDirectly(bookSelector.getValue(), newVal);
                }
            });

            // Load saved progress
            if (progressFile.exists()) {
                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(progressFile)) {
                    props.load(in);
                    String book = props.getProperty("book");
                    String chapter = props.getProperty("chapter");
                    if (book != null && chapter != null) {
                        safeBookmarkNavigation(book, Integer.parseInt(chapter));
                    }
                }
            }
        } catch (Exception e) {
            verseDisplay.setText("Error initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void safeBookmarkNavigation(String book, int chapter) {
        Platform.runLater(() -> {
            bookSelector.getSelectionModel().select(book);
            chapterSelector.getItems().clear();
            int chapterCount = bookChapterMap.get(book).size();
            for (int i = 1; i <= chapterCount; i++) {
                chapterSelector.getItems().add(i);
            }
            chapterSelector.getSelectionModel().select(chapter - 1);

            displayChapterDirectly(book, chapter);
        });
    }

    private void displayChapterDirectly(String book, int chapter) {
        try {
            Element chapterElement = bookChapterMap.get(book).get(chapter - 1);
            StringBuilder content = new StringBuilder();
            
            NodeList verses = chapterElement.getElementsByTagName("verse");
            for (int i = 0; i < verses.getLength(); i++) {
                Element verse = (Element) verses.item(i);
                content.append(verse.getAttribute("number"))
                       .append(". ")
                       .append(verse.getTextContent().trim())
                       .append("\n\n");
            }
            
            verseDisplay.setText(content.toString());
            saveProgress();
        } catch (Exception e) {
            verseDisplay.setText("Error displaying chapter: " + e.getMessage());
        }
    }

    @FXML
    private void previousChapter() {
        navigateChapter(-1);
    }

    @FXML
    private void nextChapter() {
        navigateChapter(1);
    }

    private void navigateChapter(int direction) {
        String currentBook = bookSelector.getValue();
        Integer currentChapter = chapterSelector.getValue();
        
        if (currentBook == null || currentChapter == null) return;
        
        List<String> books = new ArrayList<>(bookChapterMap.keySet());
        int currentBookIndex = books.indexOf(currentBook);
        List<Element> currentBookChapters = bookChapterMap.get(currentBook);
        
        int newChapter = currentChapter + direction;
        
        if (newChapter >= 1 && newChapter <= currentBookChapters.size()) {
            updateSelection(currentBook, newChapter);
        } else {
            int newBookIndex = currentBookIndex + direction;
            if (newBookIndex >= 0 && newBookIndex < books.size()) {
                String newBook = books.get(newBookIndex);
                List<Element> newBookChapters = bookChapterMap.get(newBook);
                int targetChapter = (direction > 0) ? 1 : newBookChapters.size();
                updateSelection(newBook, targetChapter);
            }
        }
    }

    private void updateSelection(String book, int chapter) {
        if (!book.equals(bookSelector.getValue())) {
            bookSelector.getSelectionModel().select(book);
            Platform.runLater(() -> chapterSelector.getSelectionModel().select(chapter - 1));
        } else {
            chapterSelector.getSelectionModel().select(chapter - 1);
        }
    }


    @FXML
    private void addBookmark() {
        String book = bookSelector.getValue();
        Integer chapter = chapterSelector.getValue();
        if (book == null || chapter == null) return;

        Properties props = new Properties();
        if (bookmarksFile.exists()) {
            try (FileInputStream in = new FileInputStream(bookmarksFile)) {
                props.load(in);
            } catch (Exception e) {
                System.err.println("Failed to load bookmarks: " + e.getMessage());
            }
        }

        String key = book + " - Chapter " + chapter;
        props.setProperty(key, System.currentTimeMillis() + "");

        try (FileOutputStream out = new FileOutputStream(bookmarksFile)) {
            props.store(out, "Bible Bookmarks");
            showAlert("Bookmark Added", "Current chapter has been bookmarked.");
        } catch (Exception e) {
            showAlert("Error", "Failed to save bookmark: " + e.getMessage());
        }
    }

    @FXML
    private void viewBookmarks() {
        if (!bookmarksFile.exists()) {
            showAlert("No Bookmarks", "You haven't saved any bookmarks yet.");
            return;
        }

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(bookmarksFile)) {
            props.load(in);
        } catch (Exception e) {
            showAlert("Error", "Failed to load bookmarks: " + e.getMessage());
            return;
        }

        if (props.isEmpty()) {
            showAlert("No Bookmarks", "You haven't saved any bookmarks yet.");
            return;
        }

        ListView<String> bookmarkList = new ListView<>();
        List<Map.Entry<String, String>> sortedBookmarks = new ArrayList<>();
        
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            sortedBookmarks.add(new AbstractMap.SimpleEntry<>(
                entry.getKey().toString(),
                entry.getValue().toString()
            ));
        }

        sortedBookmarks.sort((a, b) -> 
            Long.compare(Long.parseLong(b.getValue()), Long.parseLong(a.getValue()))
        );

        for (Map.Entry<String, String> entry : sortedBookmarks) {
            bookmarkList.getItems().add(entry.getKey());
        }

        Button deleteButton = new Button("Delete Selected");
        deleteButton.setOnAction(e -> {
            String selected = bookmarkList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                props.remove(selected);
                try (FileOutputStream out = new FileOutputStream(bookmarksFile)) {
                    props.store(out, "Bible Bookmarks");
                    bookmarkList.getItems().remove(selected);
                    showAlert("Success", "Bookmark removed successfully.");
                } catch (Exception ex) {
                    showAlert("Error", "Failed to remove bookmark: " + ex.getMessage());
                }
            } else {
                showAlert("No Selection", "Please select a bookmark to delete.");
            }
        });

        VBox vbox = new VBox(10, bookmarkList, deleteButton);
        vbox.setPadding(new Insets(10));

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Your Bookmarks");
        alert.setHeaderText("Select a bookmark to jump to (double-click to select):");
        alert.getDialogPane().setContent(vbox);
        
        bookmarkList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = bookmarkList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    String[] parts = selected.split(" - Chapter ");
                    if (parts.length == 2) {
                        safeBookmarkNavigation(parts[0], Integer.parseInt(parts[1]));
                    }
                    alert.close();
                }
            }
        });

        alert.showAndWait();
    }

    private void saveProgress() {
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

    @FXML
    private void resetProgress() {
        if (progressFile.exists()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Reset");
            confirmAlert.setHeaderText("Reset Reading Progress");
            confirmAlert.setContentText("Are you sure you want to reset your reading progress? This action cannot be undone.");
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (progressFile.delete()) {
                    bookSelector.getSelectionModel().clearSelection();
                    chapterSelector.getItems().clear();
                    verseDisplay.clear();
                    showAlert("Progress Reset", "Your reading progress has been reset.");
                } else {
                    showAlert("Error", "Failed to reset progress.");
                }
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