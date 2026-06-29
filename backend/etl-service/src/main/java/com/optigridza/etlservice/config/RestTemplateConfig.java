package com.optigridza.etlservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    // RestTemplate is the HTTP client we use to call
    // OpenWeatherMap API, EskomSePush, and any other external API

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
