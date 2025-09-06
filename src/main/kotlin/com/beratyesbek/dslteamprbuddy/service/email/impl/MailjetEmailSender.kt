package com.beratyesbek.dslteamprbuddy.service.email.impl

import EmailProperties
import com.beratyesbek.dslteamprbuddy.entity.EmailMessage
import com.beratyesbek.dslteamprbuddy.entity.EmailSendResult
import com.beratyesbek.dslteamprbuddy.service.email.EmailSender
import com.google.cloud.spring.secretmanager.SecretManagerTemplate
import com.mailjet.client.ClientOptions
import com.mailjet.client.MailjetClient
import com.mailjet.client.MailjetRequest
import com.mailjet.client.MailjetResponse
import com.mailjet.client.errors.MailjetException
import com.mailjet.client.resource.Emailv31
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Mailjet Send API v3.1 with credentials fetched from Google Secret Manager.
 * Works on Cloud Run Jobs using the service account's IAM permissions.
 */
class MailjetEmailSender(
    private val props: EmailProperties,
    private val secrets: SecretManagerTemplate
) : EmailSender {

    private val log = LoggerFactory.getLogger(MailjetEmailSender::class.java)

    @Volatile private var clientRef: MailjetClient? = null

    override fun send(message: EmailMessage): EmailSendResult {
        val client = clientRef ?: synchronized(this) {
            clientRef ?: buildClient().also { clientRef = it }
        }

        return try {
            val (fromName, fromEmail) = parseFrom(message.from)

            val fromJson = JSONObject().put("Email", fromEmail).apply {
                if (!fromName.isNullOrBlank()) put("Name", fromName)
            }

            val toArr = toCcBccArray(message.to)
            val ccArr = toCcBccArray(message.cc)
            val bccArr = toCcBccArray(message.bcc)

            val msg = JSONObject().apply {
                put(Emailv31.Message.FROM, fromJson)
                put(Emailv31.Message.TO, toArr)
                if (ccArr.length() > 0) put(Emailv31.Message.CC, ccArr)
                if (bccArr.length() > 0) put(Emailv31.Message.BCC, bccArr)
                put(Emailv31.Message.SUBJECT, message.subject)
                message.htmlBody.takeIf { it.isNotBlank() }?.let {
                    put(Emailv31.Message.HTMLPART, it)
                }
                message.textBody?.takeIf { it.isNotBlank() }?.let {
                    put(Emailv31.Message.TEXTPART, it)
                }
                message.replyTo?.takeIf { it.isNotBlank() }?.let { reply ->
                    put(Emailv31.Message.HEADERS, JSONObject().put("Reply-To", reply))
                }
            }

            val payload = JSONArray().put(msg)
            val request = MailjetRequest(Emailv31.resource)
                .property(Emailv31.MESSAGES, payload)

            val response: MailjetResponse = client.post(request)
            val ok = response.status in 200..299 && responseIsSuccess(response)
            val messageId = extractMessageId(response)

            if (ok) {
                log.debug("Mailjet send OK status={}, id={}", response.status, messageId)
                EmailSendResult(provider = "mailjet", messageId = messageId, ok = true)
            } else {
                val err = response.data?.toString() ?: "Unknown Mailjet error"
                log.error("Mailjet send FAILED status={}, error={}", response.status, err)
                EmailSendResult(provider = "mailjet", messageId = messageId, ok = false, error = err)
            }
        } catch (ex: MailjetException) {
            log.error("Mailjet exception", ex)
            EmailSendResult(provider = "mailjet", ok = false, error = ex.message)
        } catch (ex: Exception) {
            log.error("Mailjet unexpected exception", ex)
            EmailSendResult(provider = "mailjet", ok = false, error = ex.message)
        }
    }

    /*
    ==== internals ====
    */

    private fun buildClient(): MailjetClient {
        val mj = props.mailjet

        fun fetch(name: String, label: String): String {
            require(name.isNotBlank()) { "$label Secret Manager name is blank" }
            return secrets.getSecretString(name)
                ?: error("Secret '$name' not found or empty")
        }

        val apiKeyPublic = fetch(mj.apiKeyPublicSecretName, "Mailjet public API key")
        val apiKeyPrivate = fetch(mj.apiKeyPrivateSecretName, "Mailjet private API key")

        val http = OkHttpClient.Builder()
            .connectTimeout(mj.connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(mj.readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(mj.writeTimeoutMs, TimeUnit.MILLISECONDS)
            .build()

        val options = ClientOptions.builder()
            .apiKey(apiKeyPublic)
            .apiSecretKey(apiKeyPrivate)
            .okHttpClient(http)
            .apply { mj.baseUrl?.takeIf { it.isNotBlank() }?.let { baseUrl(it) } }
            .build()

        return MailjetClient(options)
    }

    private fun toCcBccArray(emails: List<String>): JSONArray =
        JSONArray().apply {
            emails.filter { it.isNotBlank() }.forEach { email ->
                put(JSONObject().put("Email", email))
            }
        }

    private fun parseFrom(from: String): Pair<String?, String> {
        val trimmed = from.trim()
        val m = Regex("""^\s*(?:(.+?)\s*)?<\s*([^>]+)\s*>\s*$""").matchEntire(trimmed)
        return if (m != null) {
            val name = m.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
            val email = m.groupValues.getOrNull(2) ?: trimmed
            name to email
        } else {
            null to trimmed // only email provided
        }
    }

    private fun responseIsSuccess(response: MailjetResponse): Boolean {
        return try {
            // Expecting: { "Messages": [ { "Status": "success", ... } ] }
            val top = response.data.optJSONObject(0) ?: return response.status in 200..299
            val messages = top.optJSONArray("Messages") ?: return response.status in 200..299
            if (messages.length() == 0) return response.status in 200..299
            val first = messages.optJSONObject(0) ?: return response.status in 200..299
            first.optString("Status").equals("success", ignoreCase = true)
        } catch (_: Exception) {
            response.status in 200..299
        }
    }

    private fun extractMessageId(response: MailjetResponse): String? {
        return try {
            val top = response.data.optJSONObject(0) ?: return null
            val messages = top.optJSONArray("Messages") ?: return null
            if (messages.length() == 0) return null
            val first = messages.optJSONObject(0) ?: return null
            val toArr = first.optJSONArray("To") ?: return null
            if (toArr.length() == 0) return null
            val firstTo = toArr.optJSONObject(0) ?: return null

            val uuid = firstTo.optString("MessageUUID")
            if (uuid.isNullOrBlank()) {
                firstTo.opt("MessageID")?.toString()
            } else {
                uuid
            }
        } catch (_: Exception) {
            null
        }
    }
}