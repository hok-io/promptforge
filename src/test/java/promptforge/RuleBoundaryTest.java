package promptforge;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RuleBoundaryTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("promptforge-boundary-");
        Path rules = Files.createDirectories(root.resolve(".promptforge/rules"));
        try {
            Path rule = rules.resolve("large.rule");
            Files.writeString(rule, "id=large\nkeywords=test\ninstruction=" + "x".repeat(8200), StandardCharsets.UTF_8);
            expectRuleError(root, "large.rule");
            Files.writeString(rule, "id=large\nkeywords=test\ninstruction=" + "x".repeat(501), StandardCharsets.UTF_8);
            expectRuleError(root, "instruction");
            Files.delete(rule);
            Path outside = Files.createTempFile("external-rule-", ".rule");
            try {
                Files.writeString(outside, "id=external\nkeywords=test\ninstruction=Do something\n", StandardCharsets.UTF_8);
                try {
                    Files.createSymbolicLink(rules.resolve("external.rule"), outside);
                    expectRuleError(root, "symbolic link");
                } catch (UnsupportedOperationException | java.io.IOException e) {
                    if (!System.getProperty("os.name").toLowerCase().contains("windows")) throw e;
                    System.out.println("SKIP symlink test: Windows privilege unavailable");
                }
            } finally {
                Files.deleteIfExists(outside);
            }
            try {
                new TaskFrameCompiler().compile("x".repeat(4001), java.util.List.of());
                throw new AssertionError("long task should fail");
            } catch (IllegalArgumentException expected) {
                check(expected.getMessage().contains("task"), "task length error");
            }
            java.util.List<Rule> many = new java.util.ArrayList<>();
            for (int i = 0; i < 15; i++) {
                many.add(new Rule("rule." + i, java.util.List.of("x"),
                        "y".repeat(500), ".promptforge/rules/" + i + ".rule"));
            }
            try {
                new TaskFrameCompiler().compile("x", many);
                throw new AssertionError("oversized frame should fail");
            } catch (FrameBudgetException expected) {
                check(expected.getMessage().contains("frame"), "frame length error");
            }
            System.out.println("PASS RuleBoundaryTest");
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception e) { throw new RuntimeException(e); }
                });
            }
        }
    }

    private static void expectRuleError(Path root, String detail) throws Exception {
        try {
            new RuleFileLoader().load(root);
            throw new AssertionError("Expected rule error containing " + detail);
        } catch (RuleFormatException expected) {
            check(expected.getMessage().contains(detail), "error explains " + detail);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
