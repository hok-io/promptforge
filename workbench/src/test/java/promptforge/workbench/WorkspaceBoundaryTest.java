package promptforge.workbench;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceBoundaryTest {
    @TempDir Path fixture;

    @Test void directoryLinkCannotExposeOrModifyOutsideProject() throws Exception {
        Path project = Files.createDirectory(fixture.resolve("project"));
        Path outside = Files.createDirectory(fixture.resolve("outside"));
        Files.writeString(outside.resolve("private.txt"), "untouched");
        Path link = project.resolve("linked");
        boolean windows = System.getProperty("os.name").toLowerCase().contains("windows");
        if (windows) {
            // Junctions need no symlink privilege, so Windows must exercise this boundary too.
            Process create = new ProcessBuilder("cmd.exe", "/c", "mklink", "/J", link.toString(), outside.toString())
                    .redirectErrorStream(true).start();
            assertTrue(create.waitFor(10, TimeUnit.SECONDS), "junction creation timed out");
            assertEquals(0, create.exitValue(), new String(create.getInputStream().readAllBytes()));
        } else {
            Files.createSymbolicLink(link, outside);
        }
        try {
            WorkspaceFiles files = new WorkspaceFiles(project);
            assertThrows(WorkbenchException.class, () -> files.read("linked/private.txt"));
            assertThrows(WorkbenchException.class, () -> files.filesUnder("linked"));
            assertThrows(WorkbenchException.class, () -> new RevisionFileStore(files).save("linked/private.txt", "new", "overwrite"));
            assertEquals("untouched", Files.readString(outside.resolve("private.txt")));
        } finally {
            // Unlink the junction itself before @TempDir cleanup; never walk its target.
            Files.delete(link);
        }
    }

    @Test void listingStopsAtTheEntryAndAggregateByteLimits() throws Exception {
        WorkspaceFiles files = new WorkspaceFiles(fixture);
        Path many = Files.createDirectory(fixture.resolve("many"));
        for (int index = 0; index < 129; index++) Files.writeString(many.resolve(index + ".txt"), "x");
        assertThrows(WorkbenchException.class, () -> files.filesUnder("many"));
        Path large = Files.createDirectory(fixture.resolve("large"));
        Files.writeString(large.resolve("a.txt"), "x".repeat(140_000));
        Files.writeString(large.resolve("b.txt"), "x".repeat(140_000));
        assertThrows(WorkbenchException.class, () -> files.filesUnder("large"));
    }
}
