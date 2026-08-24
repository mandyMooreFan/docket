package com.mbeebe.docket.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
class MailerConfig {

    private static final Logger log = LoggerFactory.getLogger(MailerConfig.class);

    /**
     * SMTP when spring.mail.* is configured; otherwise mail lands in the console —
     * the dev loop needs no mail account (build map note on #26).
     */
    @Bean
    Mailer mailer(ObjectProvider<JavaMailSender> senders,
                  @Value("${docket.mail-from:no-reply@localhost}") String from) {
        JavaMailSender sender = senders.getIfAvailable();
        if (sender == null) {
            return (to, subject, body) ->
                    log.info("mail (console — no SMTP configured)\nTo: {}\nSubject: {}\n{}",
                            to, subject, body);
        }
        return (to, subject, body) -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        };
    }
}
