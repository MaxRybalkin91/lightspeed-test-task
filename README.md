# SQL PARSER

SQL parser task for a Java/Kotlin Developer position.

The parser supports constructions in a mandatory way:

- Enumeration of sample fields explicitly (with aliases) or *
- Implicit join of several tables (select * from A, B, C)
- Explicit join of tables (inner, left, right, full join)
- Filter conditions (where a = 1 and b > 100)
- Subqueries (select * from (select * from A) a_alias)
- Grouping by one or several fields (group by)
- Sorting by one or more fields (order by)
- Selection truncation (limit, offset)

It uses the [JSQLParser](https://github.com/JSQLParser/JSqlParser) library to parse the incoming query.

Artifacts like Complementing selections (union and union all), CTE, and Window functions
are ignored.

The application supports only correct SQL queries, meaning the parser works
only with already checked structures and valid SQL-parts order, as well as valid
parenthesis.

To get the parts structured as written above, a user should run the main() func
in the Main.kt class. The response will be returned as a single entity with several collections.

Possible ways to improve:

1) Hashing and storing the response in a collection/DB/file, not to go through the query again
2) Add REST/RPC endpoint
3) Add query validator and exception handling

---

## Algorithm Description

The SQL parser implements a **recursive descent parsing approach** with the following key components:

### 1. Main Parsing Flow
```
Input SQL → CCJSqlParserUtil.parse() → AST Traversal → Query Object Construction
```

### 2. Core Algorithm Steps

1. **Lexical Analysis**: Uses CCJSqlParserUtil to convert SQL string into Abstract Syntax Tree (AST)
2. **Structural Decomposition**: Breaks down the AST into manageable components
3. **Recursive Parsing**: Each parser handles its specific SQL component
4. **Object Construction**: Builds the final Query object with all parsed elements

### 3. Parsing Components

#### SELECT Items Parser
- **Algorithm**: Linear traversal of SELECT expressions
- **Process**: Maps each SelectItem to Column object with expression and alias

#### FROM Items Parser
- **Algorithm**: Recursive traversal with pattern matching
- **Process**: Handles tables, subqueries, and nested FROM clauses recursively

#### JOIN Parser
- **Algorithm**: Sequential processing of JOIN clauses
- **Process**: Determines JOIN type and extracts conditions

#### Condition Parser (WHERE/HAVING)
- **Algorithm**: Recursive binary tree traversal
- **Process**: Splits AND/OR expressions into individual conditions using tree recursion

#### Subquery Parser
- **Algorithm**: Regex-based extraction + recursive parsing
- **Process**: Uses regex to find subqueries, then recursively parses each one

### Overall Time Complexity: **O(n × d × s)**
### Overall Space Complexity: **O(n + d × s)**
Where:
- **n** = Length of SQL string
- **d** = Maximum nesting depth of subqueries
- **s** = Number of subqueries

### Strengths:
1. **Efficient for Simple Queries**: O(n) for basic SELECT statements
2. **Parallel Processing**: `parseQueriesAsync` allows concurrent parsing
3. **Lazy Evaluation**: Only parses sections that exist in SQL

### Bottlenecks:
1. **Subquery Regex**: O(n) scan for each subquery level
2. **Recursive Calls**: Stack depth proportional to nesting
3. **String Operations**: Frequent `.toString()` calls create temporary objects