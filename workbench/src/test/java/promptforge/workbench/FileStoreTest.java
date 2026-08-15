package promptforge.workbench;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class FileStoreTest {
    @TempDir Path root;
    @Test void revisionPreventsOverwriteAndPreservesUnicode() throws Exception {
        RevisionFileStore store = new RevisionFileStore(new WorkspaceFiles(root));
        VersionedText first = store.save(".promptforge/prompts/demo.json", "new", "第一版 🙂");
        assertEquals("第一版 🙂", store.read(".promptforge/prompts/demo.json").text());
        VersionedText second = store.save(".promptforge/prompts/demo.json", first.revision(), "第二版");
        assertNotEquals(first.revision(), second.revision());
        WorkbenchException stale = assertThrows(WorkbenchException.class, () -> store.save(".promptforge/prompts/demo.json", first.revision(), "覆蓋"));
        assertEquals(409, stale.status()); assertEquals("第二版", store.read(".promptforge/prompts/demo.json").text());
    }
    @Test void externalChangesAndTraversalAreRejected() throws Exception {
        WorkspaceFiles files = new WorkspaceFiles(root);
        RevisionFileStore store = new RevisionFileStore(files);
        VersionedText first = store.save(".promptforge/prompts/demo.json", "new", "one");
        Files.writeString(root.resolve(".promptforge/prompts/demo.json"), "external");
        assertEquals(409, assertThrows(WorkbenchException.class, () -> store.save(".promptforge/prompts/demo.json", first.revision(), "mine")).status());
        assertThrows(WorkbenchException.class, () -> files.resolve("../outside"));
        assertThrows(WorkbenchException.class, () -> files.resolve(root.toAbsolutePath().toString()));
    }
    @Test void oversizedWriteDoesNotDestroyOldContent() {
        RevisionFileStore store = new RevisionFileStore(new WorkspaceFiles(root));
        VersionedText first = store.save(".promptforge/prompts/demo.json", "new", "keep");
        assertThrows(WorkbenchException.class, () -> store.save(".promptforge/prompts/demo.json", first.revision(), "x".repeat(300_000)));
        assertEquals("keep", store.read(".promptforge/prompts/demo.json").text());
    }
}
