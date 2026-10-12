package io.github.marcosaalbanojunior.prontuario.config;

import io.github.marcosaalbanojunior.prontuario.adapter.in.web.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
class WebConfig {

    @Bean
    FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        var registration = new FilterRegistrationBean<>(new TraceIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
