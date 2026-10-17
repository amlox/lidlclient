package net.lidl.lidlclient.size;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SizeCodeTest {
    private final UUID id = UUID.fromString("12345678-1234-1234-1234-123456789abc");

    @Test void roundTripsIdentityAndSliderEndpoints() {
        for (double scale : new double[]{0.25, 0.5, 1, 1.75, 3}) {
            SizeCode code = new SizeCode(id, scale);
            assertEquals(code, SizeCode.decode("  " + code.encode() + "  "));
        }
    }

    @Test void rejectsNonfiniteOutOfRangeAndMalformedCodes() {
        for (String value : new String[]{"NaN", "Infinity", "-Infinity", "0", "-1", "3.01", "0.24"}) {
            assertThrows(IllegalArgumentException.class, () -> SizeCode.decode("LIDL-SIZE:1:" + id + ":" + value));
        }
        for (String value : new String[]{"", "LIDL-SIZE:2:" + id + ":1",
                "LIDL-SIZE:1:1-1-1-1-1:1", "LIDL-SIZE:1:Alex:1", "LIDL-SIZE:1:" + id + ":1:extra"}) {
            assertThrows(IllegalArgumentException.class, () -> SizeCode.decode(value));
        }
    }
}
