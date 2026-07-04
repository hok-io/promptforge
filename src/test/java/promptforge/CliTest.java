package promptforge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CliTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("promptforge-cli-");
        Path rules = Files.createDirectories(root.resolve(".promptforge/rules"));
        Files.writeString(rules.resolve("payment.rule"),
                "id=payment.retry\nkeywords=付款,重試\ninstruction=保留冪等性\n", StandardCharsets.UTF_8);
        try {
            Result matched = call(new String[]{"--project", root.toString(), "--task", "修改付款重試"}, "");
            check(matched.code == 0, "matching task succeeds");
            check(matched.out.contains("保留冪等性"), "matching rule shown");
            check(matched.out.contains("payment.rule"), "source shown");
            check(matched.err.isEmpty(), "success has no stderr");
            Result bypass = call(new String[]{"--project", root.toString(), "--stdin"}, "更新 README");
            check(bypass.code == 0 && bypass.out.strip().equals("更新 README"), "stdin bypass");
            Result blank = call(new String[]{"--project", root.toString(), "--task", "  "}, "");
            check(blank.code == 2 && blank.err.contains("task"), "blank task error");
            Files.writeString(rules.resolve("payment.rule"), "id=bad\n", StandardCharsets.UTF_8);
            Result invalid = call(new String[]{"--project", root.toString(), "--task", "付款重試"}, "");
            check(invalid.code == 3 && invalid.err.contains("payment.rule"), "invalid rule named");
            System.out.println("PASS CliTest");
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception e) { throw new RuntimeException(e); }
                });
            }
        }
    }

    private static Result call(String[] args, String stdin) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int code = Main.run(args, new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(errors, true, StandardCharsets.UTF_8));
        return new Result(code, output.toString(StandardCharsets.UTF_8), errors.toString(StandardCharsets.UTF_8));
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
