package promptforge;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class RuleConflictFieldTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("promptforge-conflict-field-");
        Path rules = Files.createDirectories(root.resolve(".promptforge/rules"));
        Path file = rules.resolve("one.rule");
        try {
            Files.writeString(file, "id=one\nkeywords=付款\ninstruction=Do A\nconflicts=two,three\n",
                    StandardCharsets.UTF_8);
            Rule rule = new RuleFileLoader().load(root).get(0);
            check(rule.conflicts().equals(List.of("two", "three")), "conflict ids loaded");
            try {
                rule.conflicts().add("four");
                throw new AssertionError("conflicts should be immutable");
            } catch (UnsupportedOperationException expected) { /* immutable by contract */ }
            Files.writeString(file, "id=one\nkeywords=付款\ninstruction=Do A\nconflicts=one\n",
                    StandardCharsets.UTF_8);
            expectInvalid(root, "conflicts");
            Files.writeString(file, "id=one\nkeywords=付款\ninstruction=Do A\nconflicts=two,two\n",
                    StandardCharsets.UTF_8);
            expectInvalid(root, "conflicts");
            System.out.println("PASS RuleConflictFieldTest");
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception e) { throw new RuntimeException(e); }
                });
            }
        }
    }

    private static void expectInvalid(Path root, String detail) throws Exception {
        try {
            new RuleFileLoader().load(root);
            throw new AssertionError("invalid conflicts should fail");
        } catch (RuleFormatException expected) {
            check(expected.getMessage().contains(detail), "error identifies conflicts");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
