package staniz.gui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Verifies that resources required during JavaFX startup are packaged.
 */
class GuiResourcesTest {

    @Test
    void guiResources_areAvailableFromClasspath() {
        assertAll(
                () -> assertNotNull(Main.class.getResource("/view/MainWindow.fxml")),
                () -> assertNotNull(DialogBox.class.getResource("/view/DialogBox.fxml")),
                () -> assertNotNull(Main.class.getResource("/view/styles.css")));
    }
}
