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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/**
 * The Web Security Configuration.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(value = "app.security.basic.enabled")
class BasicWebSecurityConfig extends WebSecurityConfigurerAdapter {

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
     * Override this method to configure {@link WebSecurity} so that we ignore
     * certain requests like swagger, css etc.
     *
     * @param webSecurity The web security
     * @throws Exception Exception thrown while configuring the security
     */
    @Override
    public void configure(WebSecurity webSecurity) throws Exception {
        super.configure(webSecurity);
        webSecurity
                // Set some alternative firewall rules to allow extra characters
                .httpFirewall(securityHttpFirewall())
                //This will not attempt to authenticate these end points.
                //Saves on validation requests.
                .ignoring()
                .antMatchers(
                        "/webjars/**",  //bootstrap
                        "/css/**",          //css files
                        "/lib/**",          //js files
                        "/images/**",       //the images
                        "/src/**",          //the javascript sources
                        "/api/**"           //the api requests
                );
    }

}
