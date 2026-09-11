package promptforge.web;
import org.springframework.web.bind.annotation.*;
import promptforge.workbench.*;
import java.util.Map;
@RestController
@RequestMapping("/api/verification")
public class VerificationController {
    private final ConnectionVerification verification;
    private final SkillLibraryController library;
    public VerificationController(ConnectionVerification verification, SkillLibraryController library) { this.verification = verification; this.library = library; }
    @PostMapping public Map<String,Object> create(@RequestBody Request request) {
        if (!library.get(request.skill()).generatedCurrent()) throw new WorkbenchException(FailureKind.INVALID_INPUT,"Skill 尚未生成或與大綱不同步。請先到「管理 Skill」保存並生成，再開始驗證。");
        return verification.create(request.skill());
    }
    @GetMapping("/{code}") public Map<String,Object> status(@PathVariable String code) { return verification.status(code); }
    public record Request(String skill) { }
}
