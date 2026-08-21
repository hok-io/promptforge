package promptforge.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
public class PromptForgeWeb {
    public static void main(String[] args) { SpringApplication.run(PromptForgeWeb.class, args); }
}
