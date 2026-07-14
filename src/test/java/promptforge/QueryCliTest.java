package promptforge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class QueryCliTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("promptforge-query-");
        Path knowledge = Files.createDirectories(root.resolve(".promptforge/knowledge/adr"));
        try {
            Files.writeString(knowledge.resolve("payment.md"),
                    "# 付款重試\n\n同一交易只可扣款一次。\n", StandardCharsets.UTF_8);
            Result found = call("--project", root.toString(), "--query", "付款重試");
            check(found.code == 0, "query succeeds");
            check(found.out.contains("付款重試"), "title shown");
            check(found.out.contains("同一交易只可扣款一次"), "excerpt shown");
            check(found.out.contains(".promptforge/knowledge/adr/payment.md"), "source shown");
            Result missing = call("--project", root.toString(), "--query", "完全無關");
            check(missing.code == 0 && missing.out.contains("沒有相關本地資料"), "no-hit is honest");
            Result both = call("--project", root.toString(), "--query", "付款", "--task", "修改");
            check(both.code == 2, "query and task are mutually exclusive");
            System.out.println("PASS QueryCliTest");
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception e) { throw new RuntimeException(e); }
                });
            }
        }
    }

    private static Result call(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = Main.run(args, new ByteArrayInputStream(new byte[0]),
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Result(code, out.toString(StandardCharsets.UTF_8));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Result {
        final int code;
        final String out;
        Result(int code, String out) { this.code = code; this.out = out; }
    }
}
