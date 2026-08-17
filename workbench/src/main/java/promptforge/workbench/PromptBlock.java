package promptforge.workbench;

public record PromptBlock(String name, String text) {
    public PromptBlock {
        WorkbenchException.require(name != null && !name.isBlank() && name.length() <= 80, "區塊名稱需要 1–80 字。");
        WorkbenchException.require(text != null && text.length() <= 4000, "每個區塊最多 4000 字。");
    }
}
