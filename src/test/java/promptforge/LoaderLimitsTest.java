package promptforge;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LoaderLimitsTest {
    private static final int MAX_CARDS = 100;
    private static final long MAX_TOTAL_BYTES = 256L * 1024L;

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("loader-limits-");
        try {
            testCardCount(root);
            cleanPromptforge(root);
            testTotalBytes(root);
            cleanPromptforge(root);
            testAncestorSymlink(root);
            cleanPromptforge(root);
            testProjectRootSymlink(root);
            cleanPromptforge(root);
            testRealPathEscape(root);
            cleanPromptforge(root);
            testMissingRuleOrder(root);
            System.out.println("PASS LoaderLimitsTest");
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception e) { throw new RuntimeException(e); }
                });
            }
        }
    }

    private static void testCardCount(Path root) throws Exception {
        Path knowledge = Files.createDirectories(root.resolve(".promptforge/knowledge"));
        for (int i = 0; i < MAX_CARDS + 1; i++) {
            Files.writeString(knowledge.resolve(String.format("%03d.md", i)), "# Card " + i + "\n\nBody\n");
        }
        expectKnowledgeError(root, "100");
    }

    private static void testTotalBytes(Path root) throws Exception {
        Path knowledge = Files.createDirectories(root.resolve(".promptforge/knowledge"));
        byte[] payload = "# X\n\n".getBytes(StandardCharsets.UTF_8);
        byte[] padding = new byte[2620];
        java.util.Arrays.fill(padding, (byte) 'x');
        byte[] card = new byte[payload.length + padding.length];
        System.arraycopy(payload, 0, card, 0, payload.length);
        System.arraycopy(padding, 0, card, payload.length, padding.length);
        for (int i = 0; i < MAX_CARDS; i++) Files.write(knowledge.resolve(String.format("%03d.md", i)), card);
        long total;
        try (var files = Files.list(knowledge)) { total = files.mapToLong(p -> {
            try { return Files.size(p); } catch (Exception e) { throw new RuntimeException(e); }
        }).sum(); }
        check(total > MAX_TOTAL_BYTES, "test fixture exceeds the aggregate limit while each card remains below 8 KiB");
        expectKnowledgeError(root, "256");
    }

    private static void testAncestorSymlink(Path root) throws Exception {
        Path external = Files.createDirectories(root.resolve("outside/.promptforge/rules"));
        Files.writeString(external.resolve("link.rule"), "id=link\nkeywords=x\ninstruction=Do it\n");
        try {
            Files.createSymbolicLink(root.resolve("alias"), root.resolve("outside"));
            try {
                new RuleFileLoader().load(root.resolve("alias"));
                throw new AssertionError("project-root symlink ancestor should fail");
            } catch (RuleFormatException expected) {
                check(expected.getMessage().toLowerCase().contains("link"), "rule error identifies unsafe path");
            }
        } catch (UnsupportedOperationException | java.io.IOException e) {
            if (!System.getProperty("os.name").toLowerCase().contains("windows")) throw e;
            System.out.println("SKIP ancestor symlink test: Windows privilege unavailable");
        }
    }

    private static void testProjectRootSymlink(Path root) throws Exception {
        Path realRoot = Files.createDirectories(root.resolve("real-project/.promptforge/knowledge"));
        Files.writeString(realRoot.resolve("x.md"), "# X\n\nBody\n");
        try {
            Files.createSymbolicLink(root.resolve("project-link"), root.resolve("real-project"));
            expectKnowledgeError(root.resolve("project-link"), "link");
        } catch (UnsupportedOperationException | java.io.IOException e) {
            if (!System.getProperty("os.name").toLowerCase().contains("windows")) throw e;
            System.out.println("SKIP project-root symlink test: Windows privilege unavailable");
        }
    }

    private static void testRealPathEscape(Path root) throws Exception {
        Path outside = Files.createDirectories(root.resolve("real-outside/knowledge"));
        Files.writeString(outside.resolve("x.md"), "# X\n\nBody\n");
        Path internal = Files.createDirectories(root.resolve("inside/.promptforge"));
        try {
            Files.createSymbolicLink(internal.resolve("knowledge"), outside);
            expectKnowledgeError(root.resolve("inside"), "link");
        } catch (UnsupportedOperationException | java.io.IOException e) {
            if (!System.getProperty("os.name").toLowerCase().contains("windows")) throw e;
            System.out.println("SKIP real-path escape test: Windows privilege unavailable");
        }
    }

    private static void testMissingRuleOrder(Path root) throws Exception {
        Path rules = Files.createDirectories(root.resolve(".promptforge/rules"));
        Files.writeString(rules.resolve("missing.rule"), "id=x\n");
        try {
            new RuleFileLoader().load(root);
            throw new AssertionError("incomplete rule should fail");
        } catch (RuleFormatException expected) {
            check(expected.getMessage().contains("missing keywords"), "missing-field order is fixed: keywords before instruction");
        }
    }

    private static void expectKnowledgeError(Path root, String detail) throws Exception {
        try {
            new KnowledgeCardLoader().load(root);
            throw new AssertionError("Expected knowledge error containing " + detail);
        } catch (KnowledgeFormatException expected) {
            check(expected.getMessage().toLowerCase().contains(detail.toLowerCase()), "knowledge error explains " + detail + ": " + expected.getMessage());
        }
    }

    private static void cleanPromptforge(Path root) throws Exception {
        Path config = root.resolve(".promptforge");
        if (Files.exists(config)) try (var paths = Files.walk(config)) {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception e) { throw new RuntimeException(e); }
            });
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

