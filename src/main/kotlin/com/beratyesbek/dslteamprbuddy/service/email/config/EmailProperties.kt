import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.email")
data class EmailProperties(
    var provider: Provider = Provider.MAILJET,
    var fromAddress: String = "",
    var fromName: String = "",
    var recipientOverride: String? = null,
    var mailjet: Mailjet = Mailjet()
) {
    enum class Provider { NOOP, MAILJET, SES, SENDGRID }

    data class Mailjet(
        var apiKeyPublicSecretName: String = "",
        var apiKeyPrivateSecretName: String = "",
        var baseUrl: String? = null,
        var connectTimeoutMs: Long = 10_000,
        var readTimeoutMs: Long = 20_000,
        var writeTimeoutMs: Long = 20_000,
    )
}