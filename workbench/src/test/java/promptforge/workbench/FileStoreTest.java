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
        assertEquals(FailureKind.CONFLICT, stale.kind()); assertEquals("第二版", store.read(".promptforge/prompts/demo.json").text());
    }
    @Test void externalChangesAndTraversalAreRejected() throws Exception {
        WorkspaceFiles files = new WorkspaceFiles(root);
        RevisionFileStore store = new RevisionFileStore(files);
        VersionedText first = store.save(".promptforge/prompts/demo.json", "new", "one");
        Files.writeString(root.resolve(".promptforge/prompts/demo.json"), "external");
        assertEquals(FailureKind.CONFLICT, assertThrows(WorkbenchException.class, () -> store.save(".promptforge/prompts/demo.json", first.revision(), "mine")).kind());
        assertThrows(WorkbenchException.class, () -> files.resolve("../outside"));
        assertThrows(WorkbenchException.class, () -> files.resolve(root.toAbsolutePath().toString()));
    }
    @Test void oversizedWriteDoesNotDestroyOldContent() {
        RevisionFileStore store = new RevisionFileStore(new WorkspaceFiles(root));
        VersionedText first = store.save(".promptforge/prompts/demo.json", "new", "keep");
        assertThrows(WorkbenchException.class, () -> store.save(".promptforge/prompts/demo.json", first.revision(), "x".repeat(300_000)));
        assertEquals("keep", store.read(".promptforge/prompts/demo.json").text());
    }
    @Test void concurrentEditorsCannotBothReplaceTheSameRevision() throws Exception {
        RevisionFileStore store = new RevisionFileStore(new WorkspaceFiles(root));
        VersionedText initial = store.save(".promptforge/prompts/race.json", "new", "initial");
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var attempts = java.util.stream.Stream.of("first", "second").map(text -> pool.submit(() -> {
                start.await();
                try { store.save(".promptforge/prompts/race.json", initial.revision(), text); return "saved"; }
                catch (WorkbenchException conflict) { return conflict.kind(); }
            })).toList();
            start.countDown();
            var outcomes = new java.util.ArrayList<Object>();
            for (var attempt : attempts) outcomes.add(attempt.get(10, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(2, outcomes.size());
            assertTrue(outcomes.contains("saved"));
            assertTrue(outcomes.contains(FailureKind.CONFLICT));
            assertTrue(java.util.List.of("first", "second").contains(store.read(".promptforge/prompts/race.json").text()));
        }
    }
}
