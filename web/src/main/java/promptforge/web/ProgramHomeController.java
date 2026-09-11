package promptforge.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** New Skill manager is the human-facing home; older flows remain reachable separately. */
@Controller
public final class ProgramHomeController {
    @GetMapping("/")
    public String home() { return "forward:/program.html"; }
}
