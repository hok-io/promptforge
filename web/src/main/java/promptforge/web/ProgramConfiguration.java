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
            .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, error) -> {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                if (error instanceof org.springframework.security.web.csrf.CsrfException)
                    response.getWriter().write("{\"code\":\"CSRF_EXPIRED\",\"message\":\"保存連線已更新，請再試一次；輸入內容仍保留。\"}");
                else response.getWriter().write("{\"code\":\"ACCESS_DENIED\",\"message\":\"這次操作未獲允許，輸入內容仍保留。\"}");
            }))
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'")))
            .build();
    }
}
