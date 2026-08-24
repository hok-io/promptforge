package promptforge.workbench;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProjectRegistryTest {
    @TempDir Path home;

    @Test void emptyRegistryIsValidAndUnknownProjectsAreNot() throws Exception {
        Path config = home.resolve("projects.json");
        Files.writeString(config, "[]");
        ProjectRegistry registry = ProjectRegistry.load(home, config);
        assertTrue(registry.all().isEmpty());
        assertEquals(404, assertThrows(WorkbenchException.class, () -> registry.require("missing")).status());
    }

    @Test void duplicateIdsAndMissingDirectoriesAreRejected() {
        Project valid = new Project("demo", "Demo", home);
        assertThrows(WorkbenchException.class, () -> new ProjectRegistry(List.of(valid, valid)));
        assertThrows(WorkbenchException.class, () -> new ProjectRegistry(List.of(new Project("demo", "Demo", home.resolve("missing")))));
    }

    @Test void missingRootCannotSilentlyRegisterTheWorkbenchHome() throws Exception {
        Path config = home.resolve("projects.json");
        Files.writeString(config, "[{\"id\":\"demo\",\"name\":\"Demo\"}]");
        assertThrows(WorkbenchException.class, () -> ProjectRegistry.load(home, config));
    }

    @Test void malformedOrOversizedConfigurationIsRejected() throws Exception {
        Path config = home.resolve("projects.json");
        Files.writeString(config, "not json");
        assertThrows(WorkbenchException.class, () -> ProjectRegistry.load(home, config));
        Files.writeString(config, " ".repeat(33 * 1024));
        assertThrows(WorkbenchException.class, () -> ProjectRegistry.load(home, config));
    }
}
