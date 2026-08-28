package promptforge.workbench;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Keeps the learning boundary explicit: ordinary Java below the HTTP adapter. */
class LearningArchitectureTest {
    private Path root() {
        Path path = Path.of("").toAbsolutePath();
        while (path != null && !Files.isDirectory(path.resolve("workbench/src/main/java"))) {
            path = path.getParent();
        }
        return java.util.Objects.requireNonNull(path, "Repository root");
    }

    @Test void coreAndWorkbenchDoNotImportWebFrameworks() throws Exception {
        for (String directory : new String[] {"src/main/java", "workbench/src/main/java"}) {
            try (var files = Files.walk(root().resolve(directory))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    assertFalse(source.matches("(?s).*import\\s+(org\\.springframework|jakarta\\.servlet|promptforge\\.web)\\..*"), file.toString());
                }
            }
        }
    }

    @Test void promptServiceOffersOperationsWithoutExposingStorage() {
        for (var method : PromptService.class.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                assertFalse(PromptRepository.class.isAssignableFrom(method.getReturnType()), method.toString());
            }
        }
    }
}
