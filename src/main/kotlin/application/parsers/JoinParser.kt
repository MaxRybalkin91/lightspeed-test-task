package application.parsers

import domain.JoinResult
import domain.JoinType
import net.sf.jsqlparser.statement.select.Join

class JoinParser(private val fromItemParser: FromItemParser) {

    companion object {
        private const val EMPTY_CONDITION = ""
    }

    suspend fun parseJoin(join: Join): JoinResult {
        val type = when {
            join.isInner -> JoinType.INNER
            join.isLeft -> JoinType.LEFT
            join.isRight -> JoinType.RIGHT
            join.isFull -> JoinType.FULL
            join.isCross -> JoinType.CROSS
            else -> JoinType.INNER
        }

        val table = fromItemParser.parseFromItem(join.rightItem).first()
        val condition = join.onExpression?.toString() ?: EMPTY_CONDITION

        return JoinResult(type, table, condition)
    }
}