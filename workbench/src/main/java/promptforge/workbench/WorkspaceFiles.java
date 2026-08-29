package promptforge.workbench;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

/** All paths stay inside one explicitly registered project. */
public final class WorkspaceFiles {
    public static final int MAX_BYTES = 256 * 1024;
    private final Path root;

    public WorkspaceFiles(Path root) {
        try {
            Path absolute = root.toAbsolutePath().normalize();
            Path current = absolute.getRoot();
            for (Path part : absolute) {
                current = current.resolve(part);
                WorkbenchException.require(!Files.isSymbolicLink(current), "專案路徑不可包含符號連結。");
            }
            this.root = absolute.toRealPath();
            WorkbenchException.require(Files.isDirectory(this.root), "專案目錄不存在。");
        } catch (IOException error) { throw new WorkbenchException(FailureKind.INVALID_INPUT, "無法讀取專案目錄。"); }
    }

    public Path root() { return root; }

    public Path resolve(String relative) {
        WorkbenchException.require(relative != null && !relative.isBlank(), "檔案來源不能空白。");
        Path input = Path.of(relative.replace('\\', '/'));
        WorkbenchException.require(!input.isAbsolute() && !relative.contains(":"), "只能使用專案相對路徑。");
        Path candidate = root.resolve(input).normalize();
        WorkbenchException.require(candidate.startsWith(root), "檔案路徑不能離開專案。");
        Path current = root;
        try {
            for (Path part : root.relativize(candidate)) {
                current = current.resolve(part);
                if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                    WorkbenchException.require(!Files.isSymbolicLink(current), "不可使用符號連結：" + relative);
                    WorkbenchException.require(current.toRealPath().startsWith(root), "實際檔案路徑離開專案：" + relative);
                }
            }
            return candidate;
        } catch (IOException error) { throw new WorkbenchException(FailureKind.INVALID_INPUT, "檔案路徑不可用：" + relative); }
    }

    public String read(String relative) {
        Path path = resolve(relative);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new WorkbenchException(FailureKind.NOT_FOUND, "找不到檔案：" + relative);
        try {
            WorkbenchException.require(Files.size(path) <= MAX_BYTES, "檔案超過 256 KiB：" + relative);
            return Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException error) { throw new WorkbenchException(FailureKind.STORAGE_FAILURE, "讀取檔案失敗：" + relative); }
    }

    public List<String> filesUnder(String relative) {
        Path directory = resolve(relative);
        if (!Files.exists(directory)) return List.of();
        WorkbenchException.require(Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS), "來源需要是資料夾。");
        try (var paths = Files.walk(directory)) {
            List<Path> candidates = paths.limit(130).toList();
            WorkbenchException.require(candidates.size() < 130, "資料夾最多 128 個檔案及子目錄。");
            java.util.ArrayList<String> results = new java.util.ArrayList<>();
            long total = 0;
            for (Path candidate : candidates) {
                String source = root.relativize(candidate).toString().replace('\\', '/');
                resolve(source);
                if (Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)) {
                    total += Files.size(candidate);
                    WorkbenchException.require(total <= MAX_BYTES, "資料夾內容超過 256 KiB。");
                    results.add(source);
                }
            }
            results.sort(String::compareTo);
            return List.copyOf(results);
        } catch (IOException error) { throw new WorkbenchException(FailureKind.STORAGE_FAILURE, "無法列出資料夾：" + relative); }
    }
}
