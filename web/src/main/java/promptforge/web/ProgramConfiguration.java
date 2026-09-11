package promptforge.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ProgramConfiguration {
    @Bean SecurityFilterChain security(HttpSecurity http, @Value("${server.address:127.0.0.1}") String address) throws Exception {
        if (!address.equals("127.0.0.1")) throw new IllegalArgumentException("PromptForge Web only binds to 127.0.0.1");
        return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'")))
            .build();
    }
}
