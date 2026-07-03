package promptforge;

import java.util.List;

public final class TaskFrameTest {
    public static void main(String[] args) {
        Rule payment = new Rule("payment.retry", List.of("付款", "重試"),
                "保留冪等性並檢查重複扣款", ".promptforge/rules/payment.rule");
        TaskFrameCompiler compiler = new TaskFrameCompiler();
        String task = "請修改付款重試邏輯，保持既有測試通過";
        TaskFrame frame = compiler.compile(task, List.of(payment));
        check(!frame.bypassed(), "matching rule is included");
        check(frame.text().startsWith(task), "original task preserved");
        check(frame.text().contains("保留冪等性並檢查重複扣款"), "instruction included");
        check(frame.text().contains("payment.retry"), "rule id included");
        check(frame.text().contains(".promptforge/rules/payment.rule"), "source included");
        check(!frame.text().contains("你是需求整理"), "no generic role boilerplate");
        TaskFrame bypass = compiler.compile("更新 README", List.of(payment));
        check(bypass.bypassed(), "irrelevant rules bypassed");
        check(bypass.text().equals("更新 README"), "bypass returns unchanged task");
        try {
            compiler.compile("  ", List.of(payment));
            throw new AssertionError("blank task should fail");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("task"), "blank error explains field");
        }
        System.out.println("PASS TaskFrameTest");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
