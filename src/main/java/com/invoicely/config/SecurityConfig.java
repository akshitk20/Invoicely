package com.invoicely.config;

import com.invoicely.model.User;
import com.invoicely.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/subscription/webhook")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/landing", "/css/**", "/js/**", "/images/**", "/icons/**", "/manifest.json", "/sw.js", "/subscription/webhook").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/landing")
                .successHandler(oAuth2SuccessHandler())
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/landing")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oAuth2SuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                String email = oAuth2User.getAttribute("email");
                String name = oAuth2User.getAttribute("name");

                User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .onboardingComplete(false)
                            .build();
                        return userRepository.save(newUser);
                    });

                if (!user.getOnboardingComplete()) {
                    response.sendRedirect("/onboarding");
                } else {
                    SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request, response);
                    if (savedRequest != null) {
                        response.sendRedirect(savedRequest.getRedirectUrl());
                    } else {
                        response.sendRedirect("/dashboard");
                    }
                }
            }
        };
    }
}
