package application.parsers

import domain.Column
import net.sf.jsqlparser.expression.Expression
import net.sf.jsqlparser.statement.select.SelectItem

class SelectItemParser {

    suspend fun parseSelectItems(items: List<SelectItem<Expression>>): MutableList<Column> {
        return items.map { item ->
            val expr = item.expression.toString()
            val alias = item.alias?.name
            Column(expr, alias)
        }.toMutableList()
    }
}