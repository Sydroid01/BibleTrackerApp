module org.openjfx.BibleReadingTrackerFX {
    requires javafx.controls;
    requires javafx.fxml;
	requires java.xml;

    opens org.openjfx.BibleReadingTrackerFX to javafx.fxml;
    exports org.openjfx.BibleReadingTrackerFX;
}
