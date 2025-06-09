package domain

import kotlinx.serialization.Serializable

@Serializable
data class Query(
    var columns: MutableList<Column> = mutableListOf(),
    var fromSources: MutableList<Source> = mutableListOf(),
    var joins: MutableList<JoinResult> = mutableListOf(),
    var whereClauses: MutableList<CommonClause> = mutableListOf(),
    var groupByColumns: MutableList<String> = mutableListOf(),
    var havingClauses: MutableList<CommonClause> = mutableListOf(),
    var orderByColumns: MutableList<OrderBy> = mutableListOf(),
    var limit: Int = 0,
    var offset: Int = 0,
)

@Serializable
data class Column(
    var expression: String,
    var alias: String? = null,
)

@Serializable
data class Source(
    var name: String,
    var alias: String? = null,
    var subquery: Query? = null,
)

@Serializable
data class JoinResult(
    var type: JoinType,
    var table: Source,
    var condition: String,
)

@Serializable
data class CommonClause(
    var condition: String,
    var subqueries: MutableList<Query> = mutableListOf(),
)

@Serializable
data class OrderBy(
    var column: String,
    var direction: SortDirection = SortDirection.ASC,
)

@Serializable
enum class SortDirection {
    ASC, DESC
}

@Serializable
enum class JoinType {
    INNER, LEFT, RIGHT, FULL, CROSS
}
