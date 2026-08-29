package promptforge.workbench;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registration is local configuration, never an arbitrary root received over HTTP. */
public final class ProjectRegistry {
    private final Map<String, Project> projects = new LinkedHashMap<>();
    public ProjectRegistry(List<Project> projects) {
        for (Project project : projects) {
            new WorkspaceFiles(project.root());
            WorkbenchException.require(this.projects.putIfAbsent(project.id(), project) == null, "重複專案 ID：" + project.id());
        }
    }
    public List<Project> all() { return List.copyOf(projects.values()); }
    public Project require(String id) {
        Project project = projects.get(id);
        if (project == null) throw new WorkbenchException(FailureKind.NOT_FOUND, "找不到已登記的專案：" + id);
        return project;
    }
    public static ProjectRegistry load(Path home, Path configuration) {
        try {
            WorkbenchException.require(Files.size(configuration) <= 32 * 1024, "專案設定檔超過 32 KiB。");
            var array = new ObjectMapper().readTree(Files.readString(configuration));
            WorkbenchException.require(array.isArray() && array.size() <= 20, "設定需是最多 20 個專案的陣列。");
            java.util.ArrayList<Project> entries = new java.util.ArrayList<>();
            for (var item : array) {
                WorkbenchException.require(item.isObject() && item.path("root").isTextual()
                        && !item.path("root").asText().isBlank(), "每個專案必須明確提供非空 root 路徑。");
                entries.add(new Project(item.path("id").asText(), item.path("name").asText(),
                        home.resolve(item.path("root").asText()).normalize()));
            }
            return new ProjectRegistry(entries);
        } catch (java.io.IOException error) { throw new WorkbenchException(FailureKind.INVALID_INPUT, "專案設定無法讀取：" + configuration); }
    }
}
