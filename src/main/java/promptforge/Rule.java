package promptforge;

import java.util.List;
import java.util.Objects;
import java.util.HashSet;

/** A rule is immutable once loaded; its source is a path relative to the project. */
public final class Rule {
    private final String id;
    private final List<String> keywords;
    private final String instruction;
    private final String source;
    private final List<String> conflicts;

    public Rule(String id, List<String> keywords, String instruction, String source) {
        this(id, keywords, instruction, source, List.of());
    }

    public Rule(String id, List<String> keywords, String instruction, String source, List<String> conflicts) {
        if (id == null || id.length() > 80 || !id.matches("[a-z][a-z0-9._-]*")) {
            throw new IllegalArgumentException("id must use lowercase letters, digits, dot, dash or underscore");
        }
        Objects.requireNonNull(keywords, "keywords");
        if (keywords.isEmpty() || keywords.size() > 10 || keywords.stream().anyMatch(k ->
                k == null || k.isBlank() || k.length() > 40 || k.chars().anyMatch(Character::isISOControl))) {
            throw new IllegalArgumentException("keywords must not be empty");
        }
        if (instruction == null || instruction.isBlank() || instruction.length() > 500
                || instruction.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("instruction must be 1-500 printable characters");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must not be empty");
        }
        Objects.requireNonNull(conflicts, "conflicts");
        if (conflicts.size() > 20 || conflicts.stream().anyMatch(c ->
                c == null || c.length() > 80 || !c.matches("[a-z][a-z0-9._-]*") || c.equals(id))
                || new HashSet<>(conflicts).size() != conflicts.size()) {
            throw new IllegalArgumentException("conflicts must contain distinct other rule IDs");
        }
        this.id = id;
        this.keywords = List.copyOf(keywords);
        this.instruction = instruction;
        this.source = source;
        this.conflicts = List.copyOf(conflicts);
    }

    public String id() { return id; }
    public List<String> keywords() { return keywords; }
    public String instruction() { return instruction; }
    public String source() { return source; }
    public List<String> conflicts() { return conflicts; }
}
