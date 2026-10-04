package ar.portal.autoridadmesa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * RNF-02: público = ver charlas, estado de la convocatoria, registrarse como postulante.
 * RNF-03: todo lo demás de /api requiere el rol ADMIN (HTTP Basic).
 * CSRF desactivado: la API no usa cookies de sesión, solo credenciales Basic.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.GET, "/api/charlas", "/api/convocatoria", "/api/distritos").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/postulantes").permitAll()
                .requestMatchers("/api/**").hasRole("ADMIN")
                .anyRequest().permitAll())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService usuarios(PasswordEncoder encoder,
                                @Value("${portal.admin.usuario}") String usuario,
                                @Value("${portal.admin.password}") String password) {
        return new InMemoryUserDetailsManager(
                User.withUsername(usuario).password(encoder.encode(password)).roles("ADMIN").build());
    }
}
