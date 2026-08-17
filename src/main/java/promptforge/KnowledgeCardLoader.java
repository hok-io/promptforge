package promptforge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Reads small Markdown cards without following symbolic links. */
public final class KnowledgeCardLoader {
    private static final int MAX_CARDS = 100;
    private static final long MAX_TOTAL_BYTES = 256L * 1024L;

    public List<KnowledgeCard> load(Path projectRoot) throws IOException {
        Path root = ProjectPaths.realProjectRoot(projectRoot, "knowledge");
        Path knowledge = ProjectPaths.checkedPath(root, root.resolve(".promptforge/knowledge"), "knowledge");
        if (!Files.isDirectory(knowledge, LinkOption.NOFOLLOW_LINKS)) return List.of();
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(knowledge, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                ProjectPaths.checkedPath(root, dir, "knowledge");
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path safeFile = ProjectPaths.checkedPath(root, file, "knowledge");
                if (safeFile.getFileName().toString().endsWith(".md")) {
                    if (files.size() >= MAX_CARDS) {
                        throw new KnowledgeFormatException("knowledge card count exceeds 100 card limit");
                    }
                    files.add(safeFile);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        files.sort(Comparator.comparing(p -> knowledge.relativize(p).toString()));
        List<KnowledgeCard> cards = new ArrayList<>();
        long totalBytes = 0;
        for (Path file : files) {
            Path safeFile = ProjectPaths.checkedPath(root, file, "knowledge");
            if (!Files.isRegularFile(safeFile, LinkOption.NOFOLLOW_LINKS)) {
                throw new KnowledgeFormatException("symbolic link or non-file knowledge card: " + file);
            }
            long fileBytes = Files.size(safeFile);
            if (fileBytes > 8192) throw new KnowledgeFormatException("knowledge card too large: " + file);
            totalBytes += fileBytes;
            if (totalBytes > MAX_TOTAL_BYTES) {
                throw new KnowledgeFormatException("knowledge cards exceed 256 KiB total limit");
            }
            if (cards.size() >= MAX_CARDS) {
                throw new KnowledgeFormatException("knowledge card count exceeds 100 card limit");
            }
            String source = root.relativize(safeFile).toString().replace('\\', '/');
            List<String> lines = Files.readAllLines(safeFile, StandardCharsets.UTF_8);
            int heading = 0;
            while (heading < lines.size() && lines.get(heading).isBlank()) heading++;
            if (heading >= lines.size() || !lines.get(heading).startsWith("# ")
                    || lines.get(heading).substring(2).isBlank()) {
                throw new KnowledgeFormatException(source + " needs a '# title' heading");
            }
            String body = String.join("\n", lines.subList(heading + 1, lines.size())).trim();
            if (body.isBlank()) throw new KnowledgeFormatException(source + " needs body text");
            cards.add(new KnowledgeCard(lines.get(heading).substring(2).trim(), body, source));
        }
        return List.copyOf(cards);
    }
}
