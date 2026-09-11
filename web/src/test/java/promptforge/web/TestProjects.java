package promptforge.web;
import java.nio.file.*;
final class TestProjects {
 static Path create() { try { return Files.createTempDirectory("promptforge-tests-"); } catch(java.io.IOException error) { throw new AssertionError(error); } }
 private TestProjects() { }
}
