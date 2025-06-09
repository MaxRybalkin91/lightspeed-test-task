package application

import application.parsers.QueryParser
import domain.Query
import kotlinx.coroutines.*

class SqlParserService(private val context: CoroutineDispatcher = Dispatchers.Default) {

    private val queryParser = QueryParser(context)

    suspend fun parseQueries(queries: List<String>): List<Query?> = coroutineScope {
        queries.map { sql ->
            async(context) {
                runCatching { queryParser.parseQuery(sql) }.getOrNull()
            }
        }.awaitAll()
    }

    suspend fun parseQuery(sql: String): Query = queryParser.parseQuery(sql)
}