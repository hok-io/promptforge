package promptforge.workbench;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PromptTemplateCompiler {
    private static final Pattern VARIABLE = Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9_]{0,39})}");
    public String compile(PromptDocument document) {
        StringBuilder text = new StringBuilder(document.task());
        if (document.mode().equals("template")) {
            for (PromptBlock block : document.blocks()) {
                if (!block.text().isBlank()) text.append("\n\n").append(block.name()).append("：\n").append(block.text());
            }
        }
        Matcher matcher = VARIABLE.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = document.variables().get(matcher.group(1));
            WorkbenchException.require(value != null, "缺少變數：" + matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        WorkbenchException.require(!result.toString().isBlank(), "請填寫任務或至少一個非空區塊。");
        WorkbenchException.require(result.length() <= 4000, "編譯後的任務／區塊最多 4000 字，請縮小內容。");
        return result.toString();
    }
}
