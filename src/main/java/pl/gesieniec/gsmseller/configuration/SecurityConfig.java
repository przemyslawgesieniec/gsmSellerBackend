package pl.gesieniec.gsmseller.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService users(PasswordEncoder passwordEncoder) {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();

        manager.createUser(User.withUsername("seller1")
            .password(passwordEncoder.encode("password1"))
            .roles("SELLER")
            .build());

        manager.createUser(User.withUsername("seller2")
            .password(passwordEncoder.encode("password2"))
            .roles("SELLER")
            .build());

        return manager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) //TODO fixme
            .headers(headers -> headers.frameOptions(frame -> frame.disable())) //TODO remove with H2
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login",       // endpoint POST logowania
                    "/login.html",  // strona logowania
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/h2-console/**"
                ).permitAll()
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login.html")     // wyświetlana strona logowania
                .loginProcessingUrl("/login") // endpoint do POST logowania
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html")
                .permitAll()
            );

        return http.build();
    }
}
