package application.parsers

import domain.Query

class SubqueryParser(private val queryParser: QueryParser) {

    companion object {
        private val SUBQUERY_PATTERN = """(?s)\(\s*(SELECT\b.*?)\)"""
            .toRegex(RegexOption.IGNORE_CASE)
        private const val SQL_GROUP_INDEX = 1
    }

    suspend fun parseSubqueries(expr: String): MutableList<Query> {
        val subqueries = mutableListOf<Query>()

        SUBQUERY_PATTERN.findAll(expr).forEach { match ->
            val subquerySql = match.groupValues[SQL_GROUP_INDEX].trim()
            try {
                val subQuery = queryParser.parseQuery(subquerySql)
                subqueries.add(subQuery)
            } catch (e: Exception) {
                throw RuntimeException("Failed to parse subquery: $subquerySql. Error: ${e.message}")
            }
        }

        return subqueries
    }
}