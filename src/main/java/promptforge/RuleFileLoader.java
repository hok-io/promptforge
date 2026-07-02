package promptforge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Loads human-editable rule files in a predictable order. */
public final class RuleFileLoader {
    private static final Set<String> KEYS = Set.of("id", "keywords", "instruction");

    public List<Rule> load(Path projectRoot) throws IOException {
        Path rulesDir = projectRoot.resolve(".promptforge/rules");
        if (!Files.isDirectory(rulesDir)) return List.of();
        List<Path> files;
        try (Stream<Path> entries = Files.list(rulesDir)) {
            files = entries.filter(p -> p.getFileName().toString().endsWith(".rule"))
                    .sorted(Comparator.comparing(Path::toString)).collect(Collectors.toList());
        }
        List<Rule> result = new ArrayList<>();
        Map<String, String> sources = new HashMap<>();
        for (Path file : files) {
            Rule rule = parse(projectRoot, file);
            String firstSource = sources.putIfAbsent(rule.id(), rule.source());
            if (firstSource != null) {
                throw new RuleFormatException("duplicate id " + rule.id() + " in " + firstSource + " and " + rule.source());
            }
            result.add(rule);
        }
        return List.copyOf(result);
    }

    private Rule parse(Path projectRoot, Path file) throws IOException {
        String source = projectRoot.relativize(file).toString().replace('\\', '/');
        Map<String, String> values = new HashMap<>();
        int lineNumber = 0;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            lineNumber++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int equals = trimmed.indexOf('=');
            if (equals < 1) throw new RuleFormatException(source + ":" + lineNumber + " expected key=value");
            String key = trimmed.substring(0, equals).trim();
            if (!KEYS.contains(key)) throw new RuleFormatException(source + ":" + lineNumber + " unknown key " + key);
            if (values.putIfAbsent(key, trimmed.substring(equals + 1).trim()) != null) {
                throw new RuleFormatException(source + ":" + lineNumber + " duplicate key " + key);
            }
        }
        for (String key : KEYS) {
            if (!values.containsKey(key) || values.get(key).isBlank()) {
                throw new RuleFormatException(source + " missing " + key);
            }
        }
        List<String> keywords = Stream.of(values.get("keywords").split(",", -1))
                .map(String::trim).collect(Collectors.toList());
        try {
            return new Rule(values.get("id"), keywords, values.get("instruction"), source);
        } catch (IllegalArgumentException e) {
            throw new RuleFormatException(source + " " + e.getMessage());
        }
    }
}
