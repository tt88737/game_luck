package com.gameluck.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class PaymentProviderConfiguration {

    @Bean
    public Clock paymentClock() {
        return Clock.systemUTC();
    }
}
