package promptforge.workbench;

import java.nio.file.Path;

public record Project(String id, String name, Path root) {
    public Project { WorkbenchException.id(id); WorkbenchException.require(name != null && !name.isBlank(), "專案名稱不能空白。"); }
}
