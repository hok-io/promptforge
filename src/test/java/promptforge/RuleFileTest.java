package promptforge;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class RuleFileTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("promptforge-rules-");
        Path rules = Files.createDirectories(root.resolve(".promptforge/rules"));
        try {
            Files.writeString(rules.resolve("payment.rule"),
                    "# Payment rule\nid=payment.retry\nkeywords=付款,重試\ninstruction=保留冪等性\n",
                    StandardCharsets.UTF_8);
            List<Rule> loaded = new RuleFileLoader().load(root);
            check(loaded.size() == 1, "one rule loaded");
            check(loaded.get(0).id().equals("payment.retry"), "id preserved");
            check(loaded.get(0).keywords().equals(List.of("付款", "重試")), "keywords preserved");
            check(loaded.get(0).source().equals(".promptforge/rules/payment.rule"), "portable source");
            expectFailure(() -> Files.writeString(rules.resolve("payment.rule"),
                    "id=payment.retry\nkeywords=付款\n", StandardCharsets.UTF_8),
                    root, "instruction");
            expectFailure(() -> Files.writeString(rules.resolve("payment.rule"),
                    "id=payment.retry\nkeywords=付款\ninstruction=OK\nunknown=x\n", StandardCharsets.UTF_8),
                    root, "unknown");
            Files.writeString(rules.resolve("payment.rule"),
                    "id=payment.retry\nkeywords=付款\ninstruction=OK\n", StandardCharsets.UTF_8);
            Files.writeString(rules.resolve("duplicate.rule"),
                    "id=payment.retry\nkeywords=退款\ninstruction=Different\n", StandardCharsets.UTF_8);
            expectLoadFailure(root, "payment.retry");
            System.out.println("PASS RuleFileTest");
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception e) { throw new RuntimeException(e); }
                });
            }
        }
    }

    private static void expectFailure(ThrowingRunnable change, Path root, String detail) throws Exception {
        change.run();
        expectLoadFailure(root, detail);
    }

    private static void expectLoadFailure(Path root, String detail) throws Exception {
        try {
            new RuleFileLoader().load(root);
            throw new AssertionError("Expected rule error containing " + detail);
        } catch (RuleFormatException expected) {
            check(expected.getMessage().contains(detail), "error names " + detail);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }
}
