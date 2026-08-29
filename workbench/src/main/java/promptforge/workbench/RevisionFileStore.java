package promptforge.workbench;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

public final class RevisionFileStore {
    private static final ConcurrentHashMap<Path, Object> LOCKS = new ConcurrentHashMap<>();
    private final WorkspaceFiles files;
    public RevisionFileStore(WorkspaceFiles files) { this.files = files; }

    public static String hash(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    public VersionedText read(String relative) {
        String text = files.read(relative);
        return new VersionedText(hash(text), text);
    }

    public VersionedText readOrNew(String relative) {
        return Files.exists(files.resolve(relative)) ? read(relative) : new VersionedText("new", "");
    }

    public VersionedText save(String relative, String expectedRevision, String text) {
        WorkbenchException.require(text != null && text.getBytes(StandardCharsets.UTF_8).length <= WorkspaceFiles.MAX_BYTES, "保存內容超過 256 KiB。");
        WorkbenchException.require(expectedRevision != null, "保存需要舊 revision，新增請用 new。");
        Path destination = files.resolve(relative);
        synchronized (LOCKS.computeIfAbsent(destination, key -> new Object())) {
            if (!readOrNew(relative).revision().equals(expectedRevision)) throw new WorkbenchException(FailureKind.CONFLICT, "檔案已改變，請重新載入後比較，沒有覆蓋。");
            Path temporary = null;
            try {
                Files.createDirectories(destination.getParent());
                files.resolve(relative);
                temporary = Files.createTempFile(destination.getParent(), ".promptforge-", ".tmp");
                Files.writeString(temporary, text, StandardCharsets.UTF_8);
                if (!readOrNew(relative).revision().equals(expectedRevision)) throw new WorkbenchException(FailureKind.CONFLICT, "檔案保存前已被修改，沒有覆蓋。");
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                return new VersionedText(hash(text), text);
            } catch (IOException error) { throw new WorkbenchException(FailureKind.STORAGE_FAILURE, "無法原子保存檔案，原檔案保留。"); }
            finally {
                if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* old content is never replaced by a partial write */ }
            }
        }
    }
}
