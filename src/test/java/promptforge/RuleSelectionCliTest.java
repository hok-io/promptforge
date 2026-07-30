package promptforge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RuleSelectionCliTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("promptforge-selection-");
        try {
            Path rules = Files.createDirectories(root.resolve(".promptforge/rules"));
            Files.writeString(rules.resolve("payment.rule"), "id=payment.retry\nkeywords=付款,重試\ninstruction=保留冪等性\nconflicts=payment.fast\n");
            Files.writeString(rules.resolve("fast.rule"), "id=payment.fast\nkeywords=立即,重試\ninstruction=立即重試\n");
            Result list = call(root, "--list-rules");
            check(list.code == 0 && list.out.contains("payment.retry") && list.out.contains("付款,重試")
                    && list.out.contains("保留冪等性") && list.out.contains("payment.rule"), "catalog supports informed selection");
            Result synonym = call(root, "--task", "支付失敗後再試一次", "--rules", "payment.retry");
            check(synonym.code == 0 && synonym.out.contains("保留冪等性"), "explicit ID recovers synonym miss");
            Result none = call(root, "--task", "不改付款重試", "--rules", "");
            check(none.code == 0 && none.out.strip().equals("不改付款重試"), "explicit empty selection overrides auto");
            Result noRules = call(root, "--task", "不改付款重試", "--no-rules");
            check(noRules.code == 0 && noRules.out.strip().equals("不改付款重試"), "no-rules survives native empty argument limitations");
            Result excluded = call(root, "--task", "不改付款重試", "--exclude", "payment.retry");
            check(excluded.code == 0 && excluded.out.strip().equals("不改付款重試"), "exclude removes inappropriate rule");
            Result conflict = call(root, "--task", "支付失敗", "--rules", "payment.retry,payment.fast");
            check(conflict.code == 6 && conflict.out.isEmpty(), "manual selection does not bypass conflicts");
            bad(root, "--task", "支付", "--rules", "missing.id");
            bad(root, "--task", "支付", "--exclude", "missing.id");
            bad(root, "--task", "支付", "--rules", "payment.retry,payment.retry");
            bad(root, "--task", "支付", "--rules", "payment.retry,");
            bad(root, "--task", "支付", "--rules", "payment.retry", "--exclude", "payment.retry");
            bad(root, "--query", "支付", "--rules", "payment.retry");
            bad(root, "--validate", "--exclude", "payment.retry");
            bad(root, "--list-rules", "--task", "支付");
            bad(root, "--task", "支付", "--rules", "payment.retry", "--rules", "payment.fast");
            bad(root, "--task", "支付", "--rules", "payment.retry", "--no-rules");
            Result ordinary = call(root, "--task", "修改付款重試");
            check(ordinary.code == 0 && ordinary.out.contains("保留冪等性"), "legacy default unchanged");
            java.util.List<String> ids = new java.util.ArrayList<>();
            for (int i = 0; i < 20; i++) {
                String id = "extra." + i;
                ids.add(id);
                Files.writeString(rules.resolve(id + ".rule"), "id=" + id + "\nkeywords=limit\ninstruction=邊界規則" + i + "\n");
            }
            Result limit = call(root, "--task", "limit", "--rules", String.join(",", ids));
            check(limit.code == 0 && limit.out.contains("extra.19"), "20 explicit IDs accepted");
            bad(root, "--task", "limit", "--rules", String.join(",", ids) + ",payment.retry");
            System.out.println("PASS RuleSelectionCliTest");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path p : paths.sorted((a,b) -> b.compareTo(a)).collect(java.util.stream.Collectors.toList())) Files.deleteIfExists(p);
            }
        }
    }
    private static void bad(Path root, String... args) throws Exception {
        Result result = call(root, args);
        check(result.code == 2 && result.out.isEmpty(), "invalid selection must fail: " + String.join(" ", args));
    }
    private static Result call(Path root, String... args) throws Exception {
        String[] full = new String[args.length + 2]; full[0]="--project"; full[1]=root.toString();
        System.arraycopy(args,0,full,2,args.length);
        ByteArrayOutputStream out=new ByteArrayOutputStream(), err=new ByteArrayOutputStream();
        int code=Main.run(full,new ByteArrayInputStream(new byte[0]),new PrintStream(out,true,StandardCharsets.UTF_8),new PrintStream(err,true,StandardCharsets.UTF_8));
        return new Result(code,out.toString(StandardCharsets.UTF_8));
    }
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private static final class Result { final int code; final String out; Result(int code,String out) {this.code=code;this.out=out;} }
}
