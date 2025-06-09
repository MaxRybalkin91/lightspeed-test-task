package application.parsers

import domain.Source
import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.select.*

class FromItemParser(private val queryParser: QueryParser) {

    companion object {
        private const val SUBQUERY_SOURCE_NAME = "subquery"
    }

    suspend fun parseFromItem(fromItem: FromItem?): List<Source> {
        val sources = mutableListOf<Source>()
        when (fromItem) {
            is Table -> {
                sources.add(Source(fromItem.name, fromItem.alias?.name))
            }

            is ParenthesedSelect -> {
                val subQuery = queryParser.parseQuery(fromItem.select.toString())
                val alias = fromItem.alias?.name
                sources.add(Source(SUBQUERY_SOURCE_NAME, alias, subQuery))
            }

            is PlainSelect -> {
                val subQuery = queryParser.parseQuery(fromItem.toString())
                sources.add(Source(SUBQUERY_SOURCE_NAME, null, subQuery))
            }

            is ParenthesedFromItem -> {
                sources.addAll(parseFromItem(fromItem.fromItem))
            }

            is Join -> {
                sources.addAll(parseFromItem(fromItem.fromItem))
                sources.addAll(parseFromItem(fromItem.rightItem))
            }
        }
        return sources
    }
}