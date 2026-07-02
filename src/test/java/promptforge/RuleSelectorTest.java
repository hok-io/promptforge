package promptforge;

import java.util.List;
import java.util.stream.Collectors;

public final class RuleSelectorTest {
    public static void main(String[] args) {
        Rule payment = new Rule("payment.retry", List.of("付款", "重試"),
                "保留冪等性", ".promptforge/rules/payment.rule");
        Rule readme = new Rule("docs.readme", List.of("README"),
                "附上使用例子", ".promptforge/rules/readme.rule");
        Rule english = new Rule("payment.english", List.of("payment", "retry"),
                "Check duplicate charges", ".promptforge/rules/english.rule");
        RuleSelector selector = new RuleSelector();
        check(ids(selector.select("請修改付款重試邏輯", List.of(readme, payment)))
                .equals(List.of("payment.retry")), "payment task selects payment rule only");
        check(ids(selector.select("更新 README", List.of(payment, readme)))
                .equals(List.of("docs.readme")), "README task does not select payment rule");
        check(ids(selector.select("Fix PAYMENT RETRY", List.of(english)))
                .equals(List.of("payment.english")), "English case ignored");
        check(selector.select("付款退款", List.of(payment)).isEmpty(),
                "all keywords required");
        check(selector.select("   ", List.of(payment)).isEmpty(), "blank task selects nothing");
        check(ids(selector.select("README 與付款重試", List.of(payment, readme)))
                .equals(ids(selector.select("README 與付款重試", List.of(readme, payment)))),
                "file order does not change result");
        System.out.println("PASS RuleSelectorTest");
    }

    private static List<String> ids(List<Rule> rules) {
        return rules.stream().map(Rule::id).collect(Collectors.toList());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
