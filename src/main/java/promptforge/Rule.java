package promptforge;

import java.util.List;
import java.util.Objects;

/** A rule is immutable once loaded; its source is a path relative to the project. */
public final class Rule {
    private final String id;
    private final List<String> keywords;
    private final String instruction;
    private final String source;

    public Rule(String id, List<String> keywords, String instruction, String source) {
        if (id == null || !id.matches("[a-z][a-z0-9._-]*")) {
            throw new IllegalArgumentException("id must use lowercase letters, digits, dot, dash or underscore");
        }
        Objects.requireNonNull(keywords, "keywords");
        if (keywords.isEmpty() || keywords.stream().anyMatch(k -> k == null || k.isBlank())) {
            throw new IllegalArgumentException("keywords must not be empty");
        }
        if (instruction == null || instruction.isBlank()) {
            throw new IllegalArgumentException("instruction must not be empty");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must not be empty");
        }
        this.id = id;
        this.keywords = List.copyOf(keywords);
        this.instruction = instruction;
        this.source = source;
    }

    public String id() { return id; }
    public List<String> keywords() { return keywords; }
    public String instruction() { return instruction; }
    public String source() { return source; }
}
