package promptforge.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** MCP has read-only tools, so its JSON-RPC POST does not need a browser CSRF token. */
@Configuration
public class ProgramMcpSecurity {
    @Bean
    @Order(1)
    SecurityFilterChain mcpSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher("/mcp")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'none'; frame-ancestors 'none'")))
                .build();
    }
}
