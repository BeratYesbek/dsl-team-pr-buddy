package com.beratyesbek.dslteamprbuddy.service.email.config

import EmailProperties
import com.beratyesbek.dslteamprbuddy.service.email.EmailSender
import com.beratyesbek.dslteamprbuddy.service.email.impl.MailjetEmailSender
import com.beratyesbek.dslteamprbuddy.service.email.impl.NoopEmailSender
import com.beratyesbek.dslteamprbuddy.service.email.impl.RecipientOverrideEmailSender
import com.google.cloud.spring.secretmanager.SecretManagerTemplate
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(EmailProperties::class)
class EmailConfig {
    private val log = LoggerFactory.getLogger(EmailConfig::class.java)

    @Bean
    fun emailSender(
        props: EmailProperties,
        secretManagerTemplate: SecretManagerTemplate
    ): EmailSender {
        // choose the base provider
        val base: EmailSender = when (props.provider) {
            EmailProperties.Provider.MAILJET -> {
                log.info("Using MailjetEmailSender (keys from Secret Manager)")
                MailjetEmailSender(props, secretManagerTemplate)
            }
            EmailProperties.Provider.NOOP -> {
                log.warn("Using NoopEmailSender (emails will NOT be sent)")
                NoopEmailSender()
            }
            EmailProperties.Provider.SES,
            EmailProperties.Provider.SENDGRID -> {
                log.warn("Selected ${props.provider}, but no implementation is provided yet. Falling back to NOOP.")
                NoopEmailSender()
            }
        }

        // wrap with recipient override for local testing
        val override = props.recipientOverride?.trim().orEmpty()
        return if (override.isNotEmpty()) {
            log.warn("Recipient override ENABLED -> all emails will go to {}", override)
            RecipientOverrideEmailSender(base, override)
        } else {
            base
        }
    }
}