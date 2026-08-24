package com.mbeebe.docket.images;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The dev/placeholder implementation: permits everything. Wiring the real CSAM
 * hash-match provider and the s.20A perceptual-hash blocklist is deployment work
 * (§10.4, §14.2) — it replaces this bean by contributing an {@link ImageChecks}
 * of its own; nothing else in the product changes.
 */
@Configuration
class PlaceholderImageChecks {

    @Bean
    @ConditionalOnMissingBean(ImageChecks.class)
    ImageChecks permitEverything() {
        return image -> true;
    }
}
