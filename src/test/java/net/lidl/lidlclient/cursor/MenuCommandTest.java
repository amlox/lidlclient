package net.lidl.lidlclient.cursor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MenuCommandTest {
    @Test void acceptsSlashOrBareCommandAndRetainsArguments() {
        assertEquals("pets",MenuCommand.normalize(" /pets "));
        assertEquals("storage 3",MenuCommand.normalize("storage 3"));
        assertEquals("",MenuCommand.normalize("  "));
    }
    @Test void rejectsEmptySlashMultipleSlashesControlsAndOversize() {
        for (String value:new String[]{"/","//pets","pets\nwarp hub","x".repeat(257)})
            assertThrows(IllegalArgumentException.class,() -> MenuCommand.normalize(value));
    }
}
