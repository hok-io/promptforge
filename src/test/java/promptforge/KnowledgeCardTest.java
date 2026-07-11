package promptforge;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class KnowledgeCardTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("promptforge-knowledge-");
        Path adr = Files.createDirectories(root.resolve(".promptforge/knowledge/adr"));
        Path policy = Files.createDirectories(root.resolve(".promptforge/knowledge/policy"));
        try {
            Files.writeString(adr.resolve("retry.md"),
                    "# 付款重試\n\n同一交易只可扣款一次。\n", StandardCharsets.UTF_8);
            Files.writeString(policy.resolve("review.md"),
                    "# Review policy\n\n變更前要有測試。\n", StandardCharsets.UTF_8);
            List<KnowledgeCard> cards = new KnowledgeCardLoader().load(root);
            check(cards.size() == 2, "ADR and policy both loaded");
            check(cards.get(0).title().equals("付款重試"), "heading parsed");
            check(cards.get(0).body().contains("扣款一次"), "body parsed");
            check(cards.get(0).source().equals(".promptforge/knowledge/adr/retry.md"), "source is relative");
            Files.writeString(adr.resolve("retry.md"), "沒有標題\n", StandardCharsets.UTF_8);
            try {
                new KnowledgeCardLoader().load(root);
                throw new AssertionError("missing heading should fail");
            } catch (KnowledgeFormatException expected) {
                check(expected.getMessage().contains("retry.md"), "error names source");
            }
            System.out.println("PASS KnowledgeCardTest");
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception e) { throw new RuntimeException(e); }
                });
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
