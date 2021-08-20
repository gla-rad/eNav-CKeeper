/*
 * Copyright (c) 2021 GLA UK Research and Development Directive
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.grad.eNav.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Feign Bean Factory Configuration.
 *
 * Apparently there is a small issue with the feign client during shutdown.
 * This doesn't cause major issue but this configuration avoids the ugly
 * exception messages.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Configuration
public class FeignBeanFactoryConfig {

    /**
     * The Feign Bean Factory post processor.
     *
     * @return The Feign Bean Factory post processor
     */
    @Bean
    FeignBeanFactoryPostProcessor feignBeanFactoryPostProcessor () {
        return new FeignBeanFactoryPostProcessor();
    }

    /**
     * The Feign Bean Factory post processor that defines the bean dependencies.
     *
     * It links both the Feign context and the Spring Client Factory with the
     * registration listeners.
     */
    public class FeignBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
            BeanDefinition bd1 = configurableListableBeanFactory.getBeanDefinition("feignContext");
            bd1.setDependsOn("registrationListener");

            BeanDefinition bd2 = configurableListableBeanFactory.getBeanDefinition("springClientFactory");
            bd2.setDependsOn("registrationListener");
        }
    }
}
