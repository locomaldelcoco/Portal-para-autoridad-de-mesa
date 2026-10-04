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
 * Público: ver charlas, inscribirse y las páginas estáticas.
 * Admin (HTTP Basic): crear/editar/borrar charlas y ver inscriptos.
 * CSRF se desactiva porque la API es stateless y usa Basic Auth enviado por el frontend.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.GET, "/api/charlas", "/api/charlas/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/inscripciones").permitAll()
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
