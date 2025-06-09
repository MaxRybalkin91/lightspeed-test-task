package application

import domain.JoinType
import domain.Query
import helper.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SqlParserTest {
    companion object {
        // Simple queries
        private const val QUERY_SIMPLE = "SELECT * FROM book"

        private const val QUERY_WITH_COLUMNS = """
            SELECT name, author, price 
            FROM book 
            WHERE price > 100
        """

        // Medium complexity queries
        private const val QUERY_WITH_JOIN_AND_AGGREGATION = """
            SELECT author.name, COUNT(book.id) as book_count, SUM(book.cost) as total_cost
            FROM author 
            LEFT JOIN book ON author.id = book.author_id 
            GROUP BY author.name 
            HAVING COUNT(*) > 1 AND SUM(book.cost) > 500
            ORDER BY total_cost DESC
            LIMIT 10
        """

        private const val QUERY_WITH_ALIASES_AND_OFFSET = """
            SELECT a.name as author_name, b.title as book_title, b.cost as price
            FROM author a
            INNER JOIN book b ON a.id = b.author_id
            WHERE b.status = 'PUBLISHED'
            ORDER BY b.cost DESC, a.name ASC
            LIMIT 10 OFFSET 20
        """

        // Complex queries with subqueries
        private const val QUERY_WITH_SUBQUERY_IN_WHERE = """
            SELECT a.name as author_name, b.title as book_title, b.cost as price
            FROM author a
            FULL JOIN book b ON a.id = b.author_id
            WHERE b.id IN (
                SELECT book_id 
                FROM shop_inventory 
                WHERE status = 'AVAILABLE' AND quantity > 5
                ORDER BY book_id 
                LIMIT 100
            )
            ORDER BY b.cost DESC
        """

        private const val QUERY_WITH_NESTED_SUBQUERIES = """
            SELECT 
            main.author_name as author_name,
            main.total_books as total_books,
            main.avg_price as avg_price,
            (SELECT COUNT(*) FROM reviews r WHERE r.author_id = main.author_id) AS review_count
            FROM (
                SELECT 
                    a.id AS author_id,
                    a.name AS author_name,
                    COUNT(b.id) AS total_books,
                    AVG(b.price) AS avg_price
                FROM author a
                LEFT JOIN book b ON a.id = b.author_id
                WHERE a.country IN (
                    SELECT country_code 
                    FROM countries 
                    WHERE active = true AND population > 1000000
                )
                GROUP BY a.id, a.name
                HAVING COUNT(b.id) > 2
            ) AS main
            WHERE main.avg_price > (
                SELECT price * 1.2 
                FROM book 
                WHERE publication_year >= 2020
                LIMIT 1
            )
            ORDER BY main.total_books DESC, main.avg_price ASC
            LIMIT 50
        """

        private const val QUERY_WITH_MULTIPLE_JOINS_AND_COMPLEX_CONDITIONS = """
            SELECT DISTINCT
                a.name as author_name,
                b.title as book_title,
                p.name as publisher_name,
                c.name as category_name,
                b.price,
                CASE 
                    WHEN b.price < 20 THEN 'Cheap'
                    WHEN b.price < 50 THEN 'Medium'
                    ELSE 'Expensive'
                END as price_category
            FROM author a
            INNER JOIN book b ON a.id = b.author_id
            LEFT JOIN publisher p ON b.publisher_id = p.id
            RIGHT JOIN book_category bc ON b.id = bc.book_id
            INNER JOIN category c ON bc.category_id = c.id
            CROSS JOIN popularity_metrics pm ON pm.book_id = b.id
            WHERE b.publication_date BETWEEN '2020-01-01' AND '2024-12-31'
                AND (b.price > 25 OR c.name IN ('Fiction', 'Science', 'Technology'))
                AND a.country = 'USA'
                AND EXISTS (
                    SELECT 1 
                    FROM book_awards ba 
                    WHERE ba.book_id = b.id AND ba.award_year >= 2022
                )
            GROUP BY a.id, a.name, b.id, b.title, p.name, c.name, b.price
            HAVING AVG(pm.rating) > 4.0
            ORDER BY 
                c.name ASC,
                b.price DESC,
                a.name ASC
            LIMIT 100 OFFSET 50
        """

        private val exceptions = arrayOf("columns", "fromSources")
    }

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var parser: SqlParserService

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        parser = SqlParserService(testDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should parse simple SELECT query with single table`() = testScope.runTest {
        val result = parser.parseQuery(QUERY_SIMPLE)

        assertColumns(result) {
            hasSize(1)
            column(0) {
                hasExpression("*")
                hasNoAlias()
            }
        }

        assertFromSources(result) {
            hasSize(1)
            source(0) {
                hasName("book")
                hasNoAlias()
                hasNoSubquery()
            }
        }

        assertEmptyClausesExcept(result)
    }

    @Test
    fun `should parse query with specific columns and WHERE clause`() = testScope.runTest {
        val result = parser.parseQuery(QUERY_WITH_COLUMNS.trimIndent())

        assertColumns(result) {
            hasSize(3)
            column(0) { hasExpression("name") }
            column(1) { hasExpression("author") }
            column(2) { hasExpression("price") }
        }

        assertFromSources(result) {
            hasSize(1)
            source(0) { hasName("book") }
        }

        assertWhereClauses(result) {
            hasSize(1)
            whereClause(0) { containsCondition("PRICE > 100") }
        }
    }

    @Test
    fun `should parse complex query with JOINs, GROUP BY, HAVING, and ORDER BY`() = testScope.runTest {
        val result = parser.parseQuery(QUERY_WITH_JOIN_AND_AGGREGATION.trimIndent())

        assertColumns(result) {
            hasSize(3)
            column(0) { hasExpression("author.name") }
            column(1) {
                hasExpression("COUNT(book.id)")
                hasAlias("book_count")
            }
            column(2) {
                hasExpression("SUM(book.cost)")
                hasAlias("total_cost")
            }
        }

        assertJoins(result) {
            hasSize(1)
            join(0) {
                hasType(JoinType.LEFT)
                hasSource("book")
                containsCondition("author.id = book.author_id")
            }
        }

        assertGroupByColumns(result) {
            hasSize(1)
            contains("author.name")
        }

        assertHavingClauses(result) {
            hasSize(2)
            anyClause { containsCondition("COUNT(*) > 1") }
            anyClause { containsCondition("SUM(book.cost) > 500") }
        }

        assertOrderByColumns(result) {
            hasSize(1)
            contains("total_cost DESC")
        }

        assertLimitAndOffset(result) {
            hasLimit(10)
            hasOffset(0)
        }
    }

    @Test
    fun `should parse query with table aliases and OFFSET`() = testScope.runTest {
        val result = parser.parseQuery(QUERY_WITH_ALIASES_AND_OFFSET.trimIndent())

        assertFromSources(result) {
            hasSize(1)
            source(0) {
                hasName("author")
                hasAlias("a")
            }
        }

        assertJoins(result) {
            hasSize(1)
            join(0) {
                hasType(JoinType.INNER)
                hasSource("book")
            }
        }

        assertLimitAndOffset(result) {
            hasLimit(10)
            hasOffset(20)
        }
    }

    @Test
    fun `should parse complex query with subquery in WHERE clause`() = testScope.runTest {
        val result = parser.parseQuery(QUERY_WITH_SUBQUERY_IN_WHERE.trimIndent())

        assertColumns(result) {
            hasSize(3)
            column(0) { hasAlias("author_name") }
            column(1) { hasAlias("book_title") }
            column(2) { hasAlias("price") }
        }

        assertJoins(result) {
            hasSize(1)
            join(0) { hasType(JoinType.FULL) }
        }

        assertWhereClauses(result) {
            hasSize(1)
            whereClause(0) {
                containsCondition("b.id IN")
                hasSubqueries(1)
            }
        }
    }

    @Test
    fun `should parse highly complex query with nested subqueries and multiple clauses`() = testScope.runTest {
        val result = parser.parseQuery(QUERY_WITH_NESTED_SUBQUERIES.trimIndent())

        assertColumns(result) {
            hasSize(4)
            column(0) { hasAlias("author_name") }
            column(1) { hasAlias("total_books") }
            column(2) { hasAlias("avg_price") }
            column(3) { hasAlias("review_count") }
        }

        assertFromSources(result) {
            hasSize(1)
            source(0) {
                hasAlias("main")
                hasSubquery()
            }
        }

        assertWhereClauses(result) {
            hasSize(1)
            whereClause(0) {
                containsCondition("MAIN.AVG_PRICE >")
                hasSubqueries(1)
            }
        }

        assertLimitAndOffset(result) {
            hasLimit(50)
            hasOffset(0)
        }
    }

    @Test
    fun `should parse query with multiple JOIN types and complex conditions`() = testScope.runTest {
        val result = parser.parseQuery(QUERY_WITH_MULTIPLE_JOINS_AND_COMPLEX_CONDITIONS.trimIndent())

        assertColumns(result) {
            hasSize(6)
            column(0) { hasAlias("author_name") }
            column(1) { hasAlias("book_title") }
            column(2) { hasAlias("publisher_name") }
            column(3) { hasAlias("category_name") }
            column(4) { hasExpression("b.price") }
            column(5) { hasAlias("price_category") }
        }

        assertJoins(result) {
            hasSize(5)
            join(0) { hasType(JoinType.INNER) }
            join(1) { hasType(JoinType.LEFT) }
            join(2) { hasType(JoinType.RIGHT) }
            join(3) { hasType(JoinType.INNER) }
            join(4) { hasType(JoinType.CROSS) }
        }

        assertWhereClauses(result) {
            hasSize(4)
            whereClause(0) {
                containsCondition("b.publication_date BETWEEN")
            }
        }

        assertGroupByColumns(result) {
            hasSize(7)
        }

        assertHavingClauses(result) {
            hasSize(1)
            havingClause(0) { containsCondition("AVG(PM.RATING) > 4.0") }
        }

        assertOrderByColumns(result) {
            hasSize(3)
            contains("C.NAME ASC")
            contains("B.PRICE DESC")
            contains("A.NAME ASC")
        }

        assertLimitAndOffset(result) {
            hasLimit(100)
            hasOffset(50)
        }
    }

    private fun assertColumns(result: Query, assertions: ColumnAssertions.() -> Unit) {
        ColumnAssertions(result.columns).assertions()
    }

    private fun assertFromSources(result: Query, assertions: SourceAssertions.() -> Unit) {
        SourceAssertions(result.fromSources).assertions()
    }

    private fun assertJoins(result: Query, assertions: JoinAssertions.() -> Unit) {
        JoinAssertions(result.joins).assertions()
    }

    private fun assertWhereClauses(result: Query, assertions: WhereAssertions.() -> Unit) {
        WhereAssertions(result.whereClauses).assertions()
    }

    private fun assertGroupByColumns(result: Query, assertions: StringListAssertions.() -> Unit) {
        StringListAssertions(result.groupByColumns).assertions()
    }

    private fun assertHavingClauses(result: Query, assertions: HavingAssertions.() -> Unit) {
        HavingAssertions(result.havingClauses).assertions()
    }

    private fun assertOrderByColumns(result: Query, assertions: StringListAssertions.() -> Unit) {
        StringListAssertions(
            result.orderByColumns
                .map { "${it.column} ${it.direction}" }
                .toMutableList())
            .assertions()
    }

    private fun assertLimitAndOffset(result: Query, assertions: LimitOffsetAssertions.() -> Unit) {
        LimitOffsetAssertions(result.limit, result.offset).assertions()
    }

    private fun assertEmptyClausesExcept(result: Query) {
        val exceptSet = exceptions.toSet()
        if ("joins" !in exceptSet) assertTrue(result.joins.isEmpty(), "Expected joins to be empty")
        if ("whereClauses" !in exceptSet) assertTrue(
            result.whereClauses.isEmpty(),
            "Expected whereClauses to be empty",
        )
        if ("groupByColumns" !in exceptSet) assertTrue(
            result.groupByColumns.isEmpty(),
            "Expected groupByColumns to be empty"
        )
        if ("havingClauses" !in exceptSet) assertTrue(
            result.havingClauses.isEmpty(),
            "Expected havingClauses to be empty"
        )
        if ("orderByColumns" !in exceptSet) assertTrue(
            result.orderByColumns.isEmpty(),
            "Expected orderByColumns to be empty"
        )
        if ("limit" !in exceptSet) assertEquals(0, result.limit, "Expected limit to be 0")
        if ("offset" !in exceptSet) assertEquals(0, result.offset, "Expected offset to be 0")
    }
}