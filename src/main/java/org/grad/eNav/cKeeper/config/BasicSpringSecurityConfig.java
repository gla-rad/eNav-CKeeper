/*
 * Copyright (c) 2021 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grad.eNav.cKeeper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * The Web Security Configuration.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(value = "app.security.basic.enabled")
class BasicSpringSecurityConfig {

    /**
     * The default application name.
     */
    @Value("${spring.application.name:cKeeper}")
    private String appName;

    /**
     * The default application name.
     */
    @Value("${gla.rad.ckeeper.resources.open:/,/login,/index,/webjars/**,/css/**,/lib/**,/images/**,/src/**}")
    private String[] openResources;

    /**
     * The user name.
     */
    @Value("${spring.security.user.name:user}")
    private String username;

    /**
     * The user password.
     */
    @Value("${spring.security.user.password:password}")
    private String password;

    /**
     * The user roles.
     */
    @Value("${spring.security.user.roles:user}")
    private String[] roles;

    /**
     * The user details manager definitions. In Springboot 3, this seems to
     * be required manually, so we use the previous user details and manually
     * import tham into a memory user details service.
     *
     * @param passwordEncoder   the password encoder
     * @return the user details manager
     */
    @Bean
    public InMemoryUserDetailsManager userDetailsManager(PasswordEncoder passwordEncoder) {
        UserDetails user = User.withUsername(this.username)
                .password(passwordEncoder.encode(this.password))
                .roles(this.roles)
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    /**
     * The password encoder.
     *
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Define a slightly more flexible HTTP Firewall configuration that allows
     * characters like semicolons, slashes and percentages.
     */
    @Bean
    protected HttpFirewall securityHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowSemicolon(true);
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowUrlEncodedPercent(true);
        return firewall;
    }

    /**
     * Defines the security web-filter chains.
     *
     * Allows open access to the health and info actuator endpoints.
     * All other actuator endpoints are only available for the actuator role.
     * Finally, all other exchanges need to be authenticated.
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Authenticate through Basic Auth
        http.httpBasic(withDefaults())
                .formLogin();
        // Also, logout using Basic Auth
        http.logout()
                .deleteCookies("JSESSIONID")
                .logoutSuccessUrl("/");
        // Require authentication for specific requests
        http.httpBasic(withDefaults())
                .authorizeHttpRequests()
                .requestMatchers(EndpointRequest.to(
                        InfoEndpoint.class,     //info endpoints
                        HealthEndpoint.class    //health endpoints
                )).permitAll()
                .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ACTUATOR")
                .requestMatchers(this.openResources).permitAll()
                .anyRequest().authenticated();

        // Disable the CSRF
        http.csrf().disable();
        return http.build();
    }

}
