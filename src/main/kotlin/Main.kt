import application.SqlParserService
import kotlinx.serialization.json.Json
import java.util.*

suspend fun main() {
    val sqlParser = SqlParserService()
    val scanner = Scanner(System.`in`)

    while (true) {
        val incomingString = buildString {
            while (true) {
                val line = scanner.nextLine()
                if (line.isEmpty()) {
                    break
                }
                append(line).append(" ")
            }
        }

        val parsedQuery = sqlParser.parseQuery(incomingString)
        val jsonString = Json.encodeToString(parsedQuery)
        println(jsonString)
    }
}