package com.beratyesbek.dslteamprbuddy.service.email

import com.beratyesbek.dslteamprbuddy.entity.EmailMessage
import com.beratyesbek.dslteamprbuddy.entity.User
import com.beratyesbek.dslteamprbuddy.service.AiMessageService
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils

@Component
class WeeklyAssignmentEmailComposer(
    private val aiMessageService: AiMessageService
) {
    fun composeForReviewer(
        reviewer: User,
        devList: List<String>,
        teamName: String,
        assignments: Map<String, List<String>>,
        fromAddress: String,
        fromName: String
    ): EmailMessage {
        val aiMsg = aiMessageService.generateMessage(
            reviewer.name,
            devList,
            teamName,
            reviewer.mainLanguage
        ).orEmpty()

        val aiMessageHtml = HtmlUtils.htmlEscape(aiMsg.trim())
            .replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br>")

        val tableHtml = buildAssignmentsTable(assignments)

        val html = """
            <html>
            <body>
              $aiMessageHtml
              <p>Full assignments for the team:</p>
              $tableHtml
            </body>
            </html>
        """.trimIndent()

        val from = if (fromName.isBlank()) fromAddress else "$fromName <$fromAddress>"

        return EmailMessage(
            from = from,
            to = listOf(reviewer.email),
            subject = "Your PR Review Assignments for This Week",
            htmlBody = html,
            textBody = aiMsg
        )
    }

    private fun buildAssignmentsTable(assignments: Map<String, List<String>>): String =
        buildString {
            append("""<table border="1" style="border-collapse: collapse; width: 50%;">""")
            append("<tr><th>Dev</th><th>Reviewers</th></tr>")
            assignments.toSortedMap().forEach { (dev, reviewers) ->
                append("<tr><td>$dev</td><td>${reviewers.joinToString(", ")}</td></tr>")
            }
            append("</table>")
        }
}