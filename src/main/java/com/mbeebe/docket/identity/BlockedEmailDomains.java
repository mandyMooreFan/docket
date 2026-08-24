package com.mbeebe.docket.identity;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * §3.3: public-inbox throwaway domains are blocked as a security rule — a
 * world-readable mailbox is an account anyone can open. Private aliasing services
 * are deliberately allowed and must never appear in this list.
 */
@Component
class BlockedEmailDomains {

    private final Set<String> domains;

    BlockedEmailDomains() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("blocked-email-domains.txt").getInputStream(),
                StandardCharsets.UTF_8))) {
            domains = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    boolean blocks(String email) {
        int at = email.lastIndexOf('@');
        if (at < 0) {
            return false;
        }
        return domains.contains(email.substring(at + 1).toLowerCase(Locale.ROOT));
    }
}
