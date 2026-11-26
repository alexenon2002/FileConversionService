package ru.shim.FileConversionService.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;
import org.springframework.kafka.listener.CommonErrorHandler;

@Configuration
public class KafkaListenerConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        return new CommonContainerStoppingErrorHandler();
    }
}
