package org.openjfx.BibleReadingTrackerFX;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.stream.IntStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BibleReaderController {
    @FXML private ComboBox<String> bookSelector;
    @FXML private ComboBox<Integer> chapterSelector;
    @FXML private TextArea verseDisplay;
    @FXML private Label currentBookChapterLabel;
    @FXML private ProgressBar overallProgressBar;
    @FXML private Label overallProgressLabel;
    @FXML private ProgressBar bookProgressBar;
    @FXML private Label bookProgressLabel;
    @FXML private Label chaptersReadLabel;
    @FXML private Label booksCompletedLabel;
    @FXML private Label lastReadLabel;

    private final Map<String, List<Element>> bookChapterMap = new LinkedHashMap<>();
    private String currentUser = null;
    private File progressFile;
    private File bookmarksFile;
    private Set<String> readChapters = new HashSet<>();
    private static final int TOTAL_CHAPTERS = 1189; // Total chapters of bible (ESV) :P
    private File progressDataFile;
    
    private void saveReadingData() {
        if (currentUser == null) return;
        
        Properties props = new Properties();
        props.setProperty("readChapters", String.join(",", readChapters));
        props.setProperty("lastRead", LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        
        try (FileOutputStream out = new FileOutputStream(progressDataFile)) {
            props.store(out, "Bible Reading Progress Data");
        } catch (Exception e) {
            System.err.println("Failed to save reading data: " + e.getMessage());
        }
    }

    private void loadReadingData() {
        if (progressDataFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(progressDataFile)) {
                props.load(in);
                // read chapters
                String readChaptersStr = props.getProperty("readChapters", "");
                if (!readChaptersStr.isEmpty()) {
                    readChapters = new HashSet<>(Arrays.asList(readChaptersStr.split(",")));
                }
                
                // last read date
                String lastRead = props.getProperty("lastRead");
                if (lastRead != null) {
                    lastReadLabel.setText("Last Read: " + lastRead);
                }
            } catch (Exception e) {
                System.err.println("Error loading reading data: " + e.getMessage());
            }
        }
    }
    
    private void updateProgressUI() {
        // Update overall progress :)
        int chaptersRead = readChapters.size();
        double overallProgress = (double) chaptersRead / TOTAL_CHAPTERS;
        overallProgressBar.setProgress(overallProgress);
        overallProgressLabel.setText(String.format("%.1f%% (%d/%d chapters)", 
            overallProgress * 100, chaptersRead, TOTAL_CHAPTERS));
        
        // Update book progress if a book is selected :3
        if (bookSelector.getValue() != null) {
            String currentBook = bookSelector.getValue();
            List<Element> chapters = bookChapterMap.get(currentBook);
            int bookChapterCount = chapters.size();
            
            // Count read chapters in current book :3
            int bookChaptersRead = 0;
            for (int i = 1; i <= bookChapterCount; i++) {
                if (readChapters.contains(currentBook + "-" + i)) {
                    bookChaptersRead++;
                }
            }
            
            double bookProgress = (double) bookChaptersRead / bookChapterCount;
            bookProgressBar.setProgress(bookProgress);
            bookProgressLabel.setText(String.format("%.1f%% (%d/%d chapters)", 
                bookProgress * 100, bookChaptersRead, bookChapterCount));
        }
        
        // Update progress labels :P
        chaptersReadLabel.setText("Chapters Read: " + chaptersRead);
        
        // To count the number of completed books :3
        int completedBooks = 0;
        for (String book : bookChapterMap.keySet()) {
            int bookChapterCount = bookChapterMap.get(book).size();
            boolean bookCompleted = true;
            
            for (int i = 1; i <= bookChapterCount; i++) {
                if (!readChapters.contains(book + "-" + i)) {
                    bookCompleted = false;
                    break;
                }
            }
            
            if (bookCompleted) completedBooks++;
        }
        
        booksCompletedLabel.setText("Books Completed: " + completedBooks);
        
        String lastRead = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        lastReadLabel.setText("Last Read: " + lastRead);
    }
    
    @FXML
    private void markChapterRead() {
        String book = bookSelector.getValue();
        Integer chapter = chapterSelector.getValue();
        if (book == null || chapter == null) return;
        
        String chapterKey = book + "-" + chapter;
        if (!readChapters.contains(chapterKey)) {
            readChapters.add(chapterKey);
            saveReadingData();
            updateProgressUI();
            showAlert("Chapter Marked", "Chapter " + chapter + " of " + book + " marked as read.");
        } else {
            showAlert("Already Read", "This chapter is already marked as read.");
        }
    }
        
        @FXML
        private void markChapterUnread() {
            String book = bookSelector.getValue();
            Integer chapter = chapterSelector.getValue();
            if (book == null || chapter == null) return;
            
            String chapterKey = book + "-" + chapter;
            if (readChapters.contains(chapterKey)) {
                readChapters.remove(chapterKey);
                saveReadingData();
                updateProgressUI();
                showAlert("Chapter Unmarked", "Chapter " + chapter + " of " + book + " marked as unread.");
            } else {
                showAlert("Not Marked", "This chapter is not marked as read.");
            }
        }
        
    
    public void setUser(String username) {
        this.currentUser = username;
        File userDir = new File("users/" + username);
        if (!userDir.exists()) {
            userDir.mkdirs();
        }
        progressFile = new File(userDir, "progress.properties");
        bookmarksFile = new File(userDir, "bookmarks.properties");
        progressDataFile = new File(userDir, "readingData.properties");
        
        loadProgress();
        loadReadingData();
        updateProgressUI();
    }
    
    @FXML
    public void initialize() {
        try {
            // To load the Bible XML file from resources
            InputStream is = getClass().getResourceAsStream("/EnglishESVBible.xml");
            if (is == null) throw new RuntimeException("EnglishESVBible.xml not found in resources.");

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            // To load chapters and books from the XML
            NodeList books = doc.getElementsByTagName("book");
            for (int i = 0; i < books.getLength(); i++) {
                Element book = (Element) books.item(i);
                String bookNum = book.getAttribute("number");
                NodeList chapters = book.getElementsByTagName("chapter");

                List<Element> chapterList = new ArrayList<>();
                for (int j = 0; j < chapters.getLength(); j++) {
                    chapterList.add((Element) chapters.item(j));
                }

                String bookLabel = " " + bookNum;
                bookChapterMap.put(bookLabel, chapterList);
            }

            // To initialize the UI components
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

        } catch (Exception e) {
            verseDisplay.setText("Error initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // To load the user's last reading progress
    private void loadProgress() {
        if (progressFile != null && progressFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(progressFile)) {
                props.load(in);
                String book = props.getProperty("book");
                String chapter = props.getProperty("chapter");
                if (book != null && chapter != null) {
                    safeBookmarkNavigation(book, Integer.parseInt(chapter));
                }
            } catch (Exception e) {
                verseDisplay.setText("Error loading progress: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // To safely navigate to a bookmarked chapter
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

    // To display the selected chapter directly
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
            currentBookChapterLabel.setText(book + " - Chapter " + chapter);
            verseDisplay.setText(content.toString());
            saveProgress();
        } catch (Exception e) {
            verseDisplay.setText("Error displaying chapter: " + e.getMessage());
        }
        if (!readChapters.contains(book + "-" + chapter)) {
            readChapters.add(book + "-" + chapter);
            saveReadingData();
            updateProgressUI();
        }
    }

    // The three methods below handle chapter navigation
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

    // To update the selection in the book and chapter selectors
    private void updateSelection(String book, int chapter) {
        if (!book.equals(bookSelector.getValue())) {
            bookSelector.getSelectionModel().select(book);
            Platform.runLater(() -> chapterSelector.getSelectionModel().select(chapter - 1));
        } else {
            chapterSelector.getSelectionModel().select(chapter - 1);
        }
    }

    // To add a bookmark for the current chapter
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

    // To view and manage bookmarks (this incldes deleting bookmarks)
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

    // To save the user's current reading progress
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

    // To reset the user's reading progress
    @FXML
    private void resetProgress() {
        if (progressFile.exists()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Reset");
            confirmAlert.setHeaderText("Reset Reading Progress");
            confirmAlert.setContentText("Are you sure you want to reset your reading progress? This will also clear all your chapter reading marks.");
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (progressFile.delete()) {
                    // Also reset reading data
                    readChapters.clear();
                    if (progressDataFile.exists()) progressDataFile.delete();
                    
                    bookSelector.getSelectionModel().clearSelection();
                    chapterSelector.getItems().clear();
                    verseDisplay.clear();
                    showAlert("Progress Reset", "Your reading progress has been reset.");
                    currentBookChapterLabel.setText("");
                    updateProgressUI();
                } else {
                    showAlert("Error", "Failed to reset progress.");
                }
            }
        } else {
            showAlert("Info", "No progress to reset.");
        }
    }

    // To show an alert dialog with a message
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // To delete the user's account and all associated data
    @FXML
    private void deleteUser() {
        if (currentUser == null) {
            showAlert("Error", "No user is currently logged in.");
            return;
        }

        // Confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete User Account");
        confirmAlert.setHeaderText("Delete User Account?");
        confirmAlert.setContentText("Are you absolutely sure you want to delete your account?\n\n" +
                "This will permanently delete:\n" +
                "• All your reading progress\n" +
                "• All your bookmarks\n" +
                "• Your account credentials\n\n" +
                "This action cannot be undone!");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Delete user directory
                File userDir = new File("users/" + currentUser);
                if (userDir.exists()) {
                    deleteDirectory(userDir);
                }
                
                // Delete credentials file
                File credentialsFile = new File("users/" + currentUser + ".properties");
                if (credentialsFile.exists() && !credentialsFile.delete()) {
                    throw new IOException("Failed to delete credentials file");
                }
                
                // Return to login screen
                returnToLoginScreen();
                
                showAlert("Account Deleted", "Your account has been successfully deleted.");
            } catch (IOException e) {
                showAlert("Error", "Failed to delete user account: " + e.getMessage());
            }
        }
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Delete user directory
                File userDir = new File("users/" + currentUser);
                if (userDir.exists()) {
                    deleteDirectory(userDir);
                }
                
                // Delete credentials file
                File credentialsFile = new File("users/" + currentUser + ".properties");
                if (credentialsFile.exists() && !credentialsFile.delete()) {
                    throw new IOException("Failed to delete credentials file");
                }
                
                // Return to login screen
                returnToLoginScreen();
                
                showAlert("Account Deleted", "Your account has been successfully deleted.");
            } catch (IOException e) {
                showAlert("Error", "Failed to delete user account: " + e.getMessage());
            }
        }
    }
    
    // To delete a directory and all its contents (when user deletes their account)
    private void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (!file.delete()) {
                            throw new IOException("Failed to delete file: " + file.getAbsolutePath());
                        }
                    }
                }
            }
            if (!directory.delete()) {
                throw new IOException("Failed to delete directory: " + directory.getAbsolutePath());
            }
        }
    }
    
    // To return to the login screen after deleting the user account
    private void returnToLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) bookSelector.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Bible Reader - Login");
        } catch (IOException e) {
            showAlert("Error", "Failed to return to login screen: " + e.getMessage());
        }
    }
    
    @FXML
    private void logout() {
		Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
		confirmAlert.setTitle("Confirm Logout");
		confirmAlert.setHeaderText("Logout Confirmation");
		confirmAlert.setContentText("Are you sure you want to logout?");

		Optional<ButtonType> result = confirmAlert.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			returnToLoginScreen();
		}
	}
}