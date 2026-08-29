package promptforge.workbench;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class SkillParser {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory()).enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    record Header(String name, String description) {}
    static Header parse(String raw) {
        WorkbenchException.require(raw != null && raw.getBytes(StandardCharsets.UTF_8).length <= 32 * 1024, "SKILL.md 不可超過 32 KiB。");
        String normalized = raw.startsWith("\uFEFF") ? raw.substring(1) : raw;
        normalized = normalized.replace("\r\n", "\n").replace('\r', '\n');
        WorkbenchException.require(normalized.startsWith("---\n"), "SKILL.md 需要以 YAML frontmatter 開始。");
        int end = normalized.indexOf("\n---\n", 4);
        if (end < 0 && normalized.endsWith("\n---")) end = normalized.length() - 4;
        WorkbenchException.require(end >= 0, "SKILL.md frontmatter 缺少結尾 ---。");
        try {
            Map<?, ?> values = YAML.readValue(normalized.substring(4, end), Map.class);
            Object name = values.get("name"), description = values.get("description");
            WorkbenchException.require(name instanceof String && !((String) name).isBlank(), "frontmatter 需要非空 name。");
            WorkbenchException.require(description instanceof String && !((String) description).isBlank(), "frontmatter 需要非空 description。");
            WorkbenchException.require(((String) name).length() <= 64, "frontmatter name 最多 64 字元。");
            WorkbenchException.require(((String) description).length() <= 1024, "frontmatter description 最多 1024 字元。");
            return new Header((String) name, (String) description);
        } catch (WorkbenchException ex) { throw ex; }
        catch (Exception ex) { throw new WorkbenchException(FailureKind.INVALID_INPUT, "SKILL.md frontmatter YAML 無效。"); }
    }
}
