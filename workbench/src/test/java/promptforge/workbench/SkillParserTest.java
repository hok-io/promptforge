package promptforge.workbench;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkillParserTest {
    @Test void bomAndCrLfAreAcceptedWithoutChangingProvidedText() {
        String raw = "\uFEFF---\r\nname: demo\r\ndescription: Demo\r\nunknown: keep\r\n---\r\nbody";
        var header = SkillParser.parse(raw);
        assertEquals("demo", header.name());
        assertEquals("Demo", header.description());
        assertTrue(raw.contains("unknown: keep"));
    }

    @Test void duplicateKeysAndLongHeadersAreRejected() {
        assertThrows(WorkbenchException.class, () -> SkillParser.parse("---\nname: a\nname: b\ndescription: d\n---\n"));
        assertThrows(WorkbenchException.class, () -> SkillParser.parse("---\nname: " + "a".repeat(65) + "\ndescription: d\n---\n"));
        assertThrows(WorkbenchException.class, () -> SkillParser.parse("---\nname: a\ndescription: d\n---\n" + "x".repeat(32 * 1024)));
    }
}
