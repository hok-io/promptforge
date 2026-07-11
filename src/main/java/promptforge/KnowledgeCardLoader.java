package promptforge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Reads small Markdown cards without following symbolic links. */
public final class KnowledgeCardLoader {
    public List<KnowledgeCard> load(Path projectRoot) throws IOException {
        Path config = projectRoot.resolve(".promptforge");
        Path knowledge = config.resolve("knowledge");
        if (Files.isSymbolicLink(config) || Files.isSymbolicLink(knowledge)) {
            throw new KnowledgeFormatException("symbolic link knowledge directory is not allowed");
        }
        if (!Files.isDirectory(knowledge)) return List.of();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(knowledge)) {
            files = walk.filter(p -> p.getFileName().toString().endsWith(".md"))
                    .sorted(Comparator.comparing(p -> knowledge.relativize(p).toString()))
                    .collect(Collectors.toList());
        }
        List<KnowledgeCard> cards = new java.util.ArrayList<>();
        for (Path file : files) {
            if (Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                throw new KnowledgeFormatException("symbolic link or non-file knowledge card: " + file);
            }
            if (Files.size(file) > 8192) throw new KnowledgeFormatException("knowledge card too large: " + file);
            String source = projectRoot.relativize(file).toString().replace('\\', '/');
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
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
