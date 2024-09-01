import MyDatabase
import Tasks
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver

fun main() {
    // SQLiteドライバーの設定
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    MyDatabase.Schema.create(driver)
    val database = MyDatabase(driver)
    val queries = database.tasksQueries

    // タスクの挿入
    queries.insertTask("Learn SQLDelight", 0)
    queries.insertTask("Write Kotlin code", 1)

    // タスクの選択
    val tasks: List<Tasks> = queries.selectAllTasks().executeAsList()
    tasks.forEach { println("${it.id}: ${it.title} (completed: ${it.completed})") }

    // タスクの更新
    queries.updateTaskStatus(1, tasks[0].id)
    val updatedTasks = queries.selectAllTasks().executeAsList()
    updatedTasks.forEach { println("${it.id}: ${it.title} (completed: ${it.completed})") }
}
