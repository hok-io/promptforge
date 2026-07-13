package promptforge;

import java.util.List;

public final class Bm25IndexTest {
    public static void main(String[] args) {
        KnowledgeCard payment = new KnowledgeCard("付款重試", "同一交易只可扣款一次，重試前確認狀態。",
                ".promptforge/knowledge/adr/payment.md");
        KnowledgeCard readme = new KnowledgeCard("README", "更新安裝和使用方式。",
                ".promptforge/knowledge/practice/readme.md");
        Bm25Index index = new Bm25Index(List.of(readme, payment));
        List<SearchHit> hits = index.search("付款重試", 3);
        check(!hits.isEmpty(), "Chinese query has result");
        check(hits.get(0).card().source().equals(payment.source()), "payment ADR ranks first");
        check(hits.stream().noneMatch(h -> h.card().source().equals(readme.source())),
                "unrelated card excluded");
        check(index.search("完全無關", 3).isEmpty(), "no overlap returns no evidence");
        check(index.search(" ", 3).isEmpty(), "blank query returns none");
        KnowledgeCard english = new KnowledgeCard("Payment retry", "Check duplicate charges.", "english.md");
        check(!new Bm25Index(List.of(english)).search("PAYMENT RETRY", 3).isEmpty(),
                "English search ignores case");
        KnowledgeCard a = new KnowledgeCard("Same", "text", "a.md");
        KnowledgeCard b = new KnowledgeCard("Same", "text", "b.md");
        List<SearchHit> ties = new Bm25Index(List.of(b, a)).search("same", 1);
        check(ties.size() == 1 && ties.get(0).card().source().equals("a.md"),
                "ties use stable source order and respect limit");
        System.out.println("PASS Bm25IndexTest");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
