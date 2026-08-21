package promptforge.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import promptforge.FrameService;
import promptforge.workbench.*;
import java.nio.file.Path;

/** Composition root: Spring constructs objects; domain classes have no Spring annotations. */
@Configuration
public class WorkbenchConfiguration {
    @Bean ProjectRegistry projectRegistry(@Value("${promptforge.home:.}") String home,
            @Value("${promptforge.registry:workbench-projects.json}") String registry,
            @Value("${server.address:127.0.0.1}") String address) {
        if (!address.equals("127.0.0.1")) throw new IllegalArgumentException("PromptForge Web only binds to 127.0.0.1");
        Path root = Path.of(home).toAbsolutePath().normalize();
        return ProjectRegistry.load(root, root.resolve(registry));
    }
    @Bean PromptService prompts(ProjectRegistry projects) { return new PromptService(projects, new PromptPreviewService(new FrameService())); }
    @Bean SkillService skills(ProjectRegistry projects) { return new SkillService(projects); }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'")))
                .build(); // CSRF stays enabled; there is no login or remote access.
    }
}
