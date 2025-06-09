package application.parsers

import net.sf.jsqlparser.expression.Expression
import net.sf.jsqlparser.expression.operators.conditional.AndExpression
import net.sf.jsqlparser.expression.operators.conditional.OrExpression

class ConditionParser {

    suspend fun splitCondition(expr: Expression): List<Expression> {
        return when (expr) {
            is AndExpression -> {
                splitCondition(expr.leftExpression) + splitCondition(expr.rightExpression)
            }

            is OrExpression -> {
                splitCondition(expr.leftExpression) + splitCondition(expr.rightExpression)
            }

            else -> listOf(expr)
        }
    }
}