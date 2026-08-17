package promptforge.workbench;

/** Application failure: the web adapter chooses how to display it. */
public final class WorkbenchException extends RuntimeException {
    private final int status;
    public WorkbenchException(int status, String message) { super(message); this.status = status; }
    public int status() { return status; }
    public static void require(boolean condition, String message) {
        if (!condition) throw new WorkbenchException(422, message);
    }
    public static String id(String value) {
        require(value != null && value.matches("[a-z][a-z0-9-]{0,39}"), "名稱 ID 必須以小寫英文字母開始，只含字母、數字及短橫線（最多 40 字）。");
        return value;
    }
}
