package application.parsers

import domain.CommonClause
import domain.OrderBy
import domain.Query
import domain.SortDirection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sf.jsqlparser.expression.Expression
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.select.SelectItem

class QueryParser(val context: CoroutineDispatcher = Dispatchers.Default) {
    companion object {
        private const val DEFAULT_LIMIT = 0
        private const val DEFAULT_OFFSET = 0
    }

    private val selectItemParser = SelectItemParser()
    private val fromItemParser = FromItemParser(this)
    private val joinParser = JoinParser(fromItemParser)
    private val conditionParser = ConditionParser()
    private val subqueryParser = SubqueryParser(this)

    suspend fun parseQuery(sql: String): Query = withContext(context) {
        val statement = CCJSqlParserUtil.parse(sql) as Select
        val selectBody = statement.selectBody as PlainSelect

        val result = Query()

        result.columns.addAll(
            selectItemParser.parseSelectItems(
                selectBody.selectItems?.filterIsInstance<SelectItem<Expression>>() ?: emptyList()
            )
        )

        result.fromSources.addAll(fromItemParser.parseFromItem(selectBody.fromItem))

        selectBody.joins?.forEach { join ->
            result.joins.add(joinParser.parseJoin(join))
        }

        selectBody.where?.let { expr ->
            conditionParser.splitCondition(expr).forEach { part ->
                result.whereClauses.add(
                    CommonClause(
                        part.toString(),
                        subqueryParser.parseSubqueries(part.toString())
                    )
                )
            }
        }

        selectBody.groupBy?.groupByExpressionList?.forEach { expr ->
            result.groupByColumns.add(expr.toString())
        }

        selectBody.having?.let { expr ->
            conditionParser.splitCondition(expr).forEach { part ->
                result.havingClauses.add(
                    CommonClause(
                        part.toString(),
                        subqueryParser.parseSubqueries(part.toString())
                    )
                )
            }
        }

        selectBody.orderByElements?.forEach { ob ->
            result.orderByColumns.add(
                OrderBy(
                    ob.expression.toString(),
                    if (ob.isAsc) SortDirection.ASC else SortDirection.DESC
                )
            )
        }

        selectBody.limit?.let { limit ->
            result.limit = when {
                limit.rowCount != null -> limit.rowCount.toString().toIntOrNull() ?: DEFAULT_LIMIT
                limit.isLimitAll -> Int.MAX_VALUE
                else -> DEFAULT_LIMIT
            }
        }

        selectBody.offset?.let { limit ->
            result.offset = limit.offset?.toString()?.toIntOrNull() ?: DEFAULT_OFFSET
        }

        result
    }
}