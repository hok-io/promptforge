package promptforge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Shared path checks for project content read by loaders. */
final class ProjectPaths {
    private ProjectPaths() { }

    static Path realProjectRoot(Path projectRoot, String kind) throws IOException {
        Path absolute = projectRoot.toAbsolutePath().normalize();
        Path current = absolute.getRoot();
        for (Path part : absolute) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) {
                throw unsafe(kind, "symbolic link in project path: " + current);
            }
        }
        if (!Files.isDirectory(absolute, LinkOption.NOFOLLOW_LINKS)) {
            throw unsafe(kind, "project path is not a directory: " + absolute);
        }
        return absolute.toRealPath();
    }

    static Path checkedPath(Path projectRoot, Path path, String kind) throws IOException {
        Path normalizedRoot = projectRoot.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (!normalizedPath.startsWith(normalizedRoot)) {
            throw unsafe(kind, "path leaves project: " + path);
        }
        Path current = normalizedRoot;
        for (Path part : normalizedRoot.relativize(normalizedPath)) {
            current = current.resolve(part);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                return current;
            }
            if (Files.isSymbolicLink(current)) {
                throw unsafe(kind, "symbolic link in project content path: " + current);
            }
        }
        Path real = normalizedPath.toRealPath();
        if (!real.startsWith(normalizedRoot.toRealPath())) {
            throw unsafe(kind, "real path leaves project: " + path);
        }
        return real;
    }

    private static RuntimeException unsafe(String kind, String message) {
        if ("rule".equals(kind)) return new RuleFormatException(message);
        return new KnowledgeFormatException(message);
    }
}

