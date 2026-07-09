package promptforge;

import java.util.List;

public final class RuleConflictTest {
    public static void main(String[] args) {
        Rule a = new Rule("payment.fast", List.of("付款", "重試"), "儘速重試",
                ".promptforge/rules/fast.rule", List.of("payment.safe"));
        Rule b = new Rule("payment.safe", List.of("付款", "重試"), "先檢查交易狀態",
                ".promptforge/rules/safe.rule");
        TaskFrameCompiler compiler = new TaskFrameCompiler();
        try {
            compiler.compile("修改付款重試", List.of(a, b));
            throw new AssertionError("conflicting rules must not produce a frame");
        } catch (RuleConflictException expected) {
            check(expected.getMessage().contains("payment.fast"), "first ID named");
            check(expected.getMessage().contains("payment.safe"), "second ID named");
            check(expected.getMessage().contains("fast.rule"), "first source named");
            check(expected.getMessage().contains("safe.rule"), "second source named");
        }
        TaskFrame one = compiler.compile("修改付款重試", List.of(a));
        check(one.rules().size() == 1, "unselected conflict target does not block");
        TaskFrame unrelated = compiler.compile("更新 README", List.of(a, b));
        check(unrelated.bypassed(), "unrelated task bypasses conflicts");
        System.out.println("PASS RuleConflictTest");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
