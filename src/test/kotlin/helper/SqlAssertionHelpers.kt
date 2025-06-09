package helper

import domain.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColumnAssertions(private val columns: MutableList<Column>) {
    fun hasSize(expected: Int) = assertEquals(expected, columns.size, "Column count mismatch")
    fun column(index: Int, assertions: ColumnAssertion.() -> Unit) {
        ColumnAssertion(columns[index]).assertions()
    }
}

class ColumnAssertion(private val column: Column) {
    fun hasExpression(expected: String) = assertEquals(expected, column.expression, "Expression mismatch")
    fun hasAlias(expected: String) = assertEquals(expected, column.alias, "Alias mismatch")
    fun hasNoAlias() = assertNull(column.alias, "Expected no alias")
}

class SourceAssertions(private val sources: MutableList<Source>) {
    fun hasSize(expected: Int) = assertEquals(expected, sources.size, "Source count mismatch")
    fun source(index: Int, assertions: SourceAssertion.() -> Unit) {
        SourceAssertion(sources[index]).assertions()
    }
}

class SourceAssertion(private val source: Source) {
    fun hasName(expected: String) = assertEquals(expected, source.name, "Source name mismatch")
    fun hasAlias(expected: String) = assertEquals(expected, source.alias, "Source alias mismatch")
    fun hasNoAlias() = assertNull(source.alias, "Expected no alias")
    fun hasSubquery() = assertNotNull(source.subquery, "Expected subquery")
    fun hasNoSubquery() = assertNull(source.subquery, "Expected no subquery")
}

class JoinAssertions(private val joins: MutableList<JoinResult>) {
    fun hasSize(expected: Int) = assertEquals(expected, joins.size, "Join count mismatch")
    fun join(index: Int, assertions: JoinAssertion.() -> Unit) {
        JoinAssertion(joins[index]).assertions()
    }
}

class JoinAssertion(private val join: JoinResult) {
    fun hasType(expected: JoinType) = assertEquals(expected, join.type, "Join type mismatch")
    fun hasSource(expected: String) = assertEquals(expected, join.table.name, "Join source mismatch")
    fun containsCondition(expected: String) = assertTrue(
        join.condition.contains(expected, ignoreCase = true),
        "Join condition should contain '$expected'"
    )
}

class WhereAssertions(private val whereClauses: MutableList<CommonClause>) {
    fun hasSize(expected: Int) = assertEquals(
        expected, whereClauses.size,
        "Where clause count mismatch"
    )

    fun whereClause(index: Int, assertions: WhereAssertion.() -> Unit) {
        WhereAssertion(whereClauses[index]).assertions()
    }
}

class WhereAssertion(private val whereClause: CommonClause) {
    fun containsCondition(expected: String) = assertTrue(
        whereClause.condition.contains(expected, ignoreCase = true),
        "Where condition should contain '$expected'"
    )

    fun hasSubqueries(expected: Int) = assertEquals(
        expected,
        whereClause.subqueries.size,
        "Subquery count mismatch"
    )
}

class HavingAssertions(private val havingClauses: MutableList<CommonClause>) {
    fun hasSize(expected: Int) = assertEquals(
        expected, havingClauses.size, "Having clause count mismatch"
    )

    fun havingClause(index: Int, assertions: HavingAssertion.() -> Unit) {
        HavingAssertion(havingClauses[index]).assertions()
    }

    fun anyClause(assertions: HavingAssertion.() -> Unit) {
        assertTrue(havingClauses.any { clause ->
            try {
                HavingAssertion(clause).assertions()
                true
            } catch (e: AssertionError) {
                false
            }
        }, "No having clause matched the condition")
    }
}

class HavingAssertion(private val havingClause: CommonClause) {
    fun containsCondition(expected: String) = assertTrue(
        havingClause.condition.contains(expected, ignoreCase = true),
        "Having condition should contain '$expected'"
    )
}

class StringListAssertions(private val list: MutableList<String>) {
    fun hasSize(expected: Int) = assertEquals(expected, list.size, "List size mismatch")
    fun contains(expected: String) = assertTrue(
        list.any { it.contains(expected, ignoreCase = true) },
        "List should contain '$expected'"
    )
}

class LimitOffsetAssertions(private val limit: Int, private val offset: Int) {
    fun hasLimit(expected: Int) = assertEquals(expected, limit, "Limit mismatch")
    fun hasOffset(expected: Int) = assertEquals(expected, offset, "Offset mismatch")
}