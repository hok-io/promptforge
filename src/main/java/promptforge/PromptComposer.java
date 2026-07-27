package promptforge;

import java.util.List;

/** Combines a rule frame and full, explicitly chosen knowledge; never truncates evidence. */
public final class PromptComposer {
    public String compose(TaskFrame frame, List<KnowledgeCard> cards) {
        StringBuilder text = new StringBuilder(frame.text());
        if (!cards.isEmpty()) text.append("\n\n選定的專案知識：");
        for (KnowledgeCard card : cards) {
            text.append("\n\n## ").append(card.title()).append("\n來源：").append(card.source())
                    .append("\n\n").append(card.body());
        }
        String result = text.toString();
        if (result.length() > TaskFrameCompiler.MAX_FRAME_CHARACTERS) {
            throw new FrameBudgetException("combined prompt exceeds 6000 characters; select fewer rules or smaller cards");
        }
        return result;
    }
}
