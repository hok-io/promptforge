package promptforge;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;

class TaskInputReaderTest {
    @Test void boundedAndStrictUnicodeInput() throws Exception {
        TaskInputReader reader = new TaskInputReader();
        assertEquals("進🙂", reader.read(new ByteArrayInputStream("進🙂\n".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        assertEquals(16000, reader.read(new ByteArrayInputStream(new byte[16000])).length());
        assertThrows(IllegalArgumentException.class, () -> reader.read(new ByteArrayInputStream(new byte[16001])));
        assertThrows(IllegalArgumentException.class, () -> reader.read(new ByteArrayInputStream(new byte[]{(byte) 0xff})));
    }
}
