package promptforge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ValidateCliTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("promptforge-validate-");
        Path rules = Files.createDirectories(root.resolve(".promptforge/rules"));
        Path knowledge = Files.createDirectories(root.resolve(".promptforge/knowledge/adr"));
        try {
            Files.writeString(rules.resolve("one.rule"),
                    "id=one\nkeywords=付款\ninstruction=Keep idempotency\n", StandardCharsets.UTF_8);
            Files.writeString(knowledge.resolve("one.md"),
                    "# 付款決策\n\n每筆交易只扣款一次。\n", StandardCharsets.UTF_8);
            Result valid = call("--project", root.toString(), "--validate");
            check(valid.code == 0 && valid.out.contains("1 rules") && valid.out.contains("1 knowledge cards"),
                    "valid project counts both sources");
            Result mixed = call("--project", root.toString(), "--validate", "--task", "付款");
            check(mixed.code == 2, "validate and task are exclusive");
            Files.writeString(knowledge.resolve("one.md"), "no title\n", StandardCharsets.UTF_8);
            Result invalid = call("--project", root.toString(), "--validate");
            check(invalid.code == 3 && invalid.err.contains("one.md"), "invalid card source named");
            System.out.println("PASS ValidateCliTest");
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
        return new Result(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Result {
        final int code;
        final String out;
        final String err;
        Result(int code, String out, String err) { this.code = code; this.out = out; this.err = err; }
    }
}
