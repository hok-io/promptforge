package promptforge.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** All former workbench bookmarks now lead to the Skill manager. */
@Controller
public final class ProgramHomeController {
    @GetMapping("/")
    public String home() { return "forward:/program.html"; }
    @GetMapping({"/legacy.html", "/workbench.html", "/guide.html", "/index.html"})
    public String retired() { return "redirect:/program.html"; }
}
