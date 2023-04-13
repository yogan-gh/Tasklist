import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.*
import java.io.File

val TABLE_HEAD: String = """
    +----+------------+-------+---+---+--------------------------------------------+
    | N  |    Date    | Time  | P | D |                   Task                     |
    +----+------------+-------+---+---+--------------------------------------------+
""".trimIndent()
val TABLE_SUBLIME: String = """
    |    |            |       |   |   |TEXT_LINE|
""".trimIndent()
val TABLE_SEPARATOR: String = "+----+------------+-------+---+---+--------------------------------------------+"

enum class Priority(val symbol: String, val colorSymbol: String) {
    Critical("C", "\u001B[101m \u001B[0m"),
    High("H", "\u001B[103m \u001B[0m"),
    Normal("N", "\u001B[102m \u001B[0m"),
    Low("L", "\u001B[104m \u001B[0m"),
    NULL("NULL", "NULL");
    override fun toString(): String {
        return colorSymbol
    }
}
enum class Field(val symbol: String) {
    Priority("priority"),
    Date("date"),
    Time("time"),
    Task("task");

    override fun toString(): String {
        return symbol
    }
}

enum class State(val symbol: String, val colorSymbol: String) {
    InTime("I", "\u001B[102m \u001B[0m"),
    Today("T", "\u001B[103m \u001B[0m"),
    Overdue("O", "\u001B[101m \u001B[0m");

    override fun toString(): String {
        return colorSymbol
    }
}

fun findField(symbol: String): Field? {
    for (field in Field.values()) {
        if (symbol == field.symbol) return field
    }
    return null
}
fun findPriority(symbol: String): Priority? {
    for (priority in Priority.values()) {
        if (symbol == priority.symbol) return priority
    }
    return null
}
class Task(var date: String, var time: String, var priority: Priority, var text: String) {
    operator fun component1(): String = date
    operator fun component2(): String = time
    operator fun component3(): Priority = priority
    operator fun component4(): State = state
    operator fun component5(): String = text
    constructor(): this("", "", Priority.NULL, "")
    val dateLD: LocalDate
        get() {
            val dateArr = date.split("-").map { it.toInt() }
            return LocalDate(dateArr[0], dateArr[1], dateArr[2])
        }
    val state: State get() {
        val taskDate = dateLD
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
        val numberOfDays = currentDate.daysUntil(taskDate)
        return when {
            numberOfDays > 0 -> State.InTime
            numberOfDays < 0 -> State.Overdue
            else -> State.Today
        }
    }
    fun build(): Task {
        priority = setPriority()
        date = setDate()
        time = setTime()
        setText()
        return this
    }
    fun setPriority(): Priority {
        while (true) {
            println("Input the task priority (C, H, N, L):")
            val priority = findPriority(readln().uppercase())
            if (priority != null) {
                return priority
            }
        }
    }
    fun set(msg: String, substring: (String) -> String): String {
        while (true) {
            println(msg)
            val input = toDateTimeOrNull(dateStr = readln())
            if (input != null) {
                return substring(input.toString())
            }
            else println("The input date is invalid")
        }
    }
    fun setDate(): String {
        while (true) {
            println("Input the date (yyyy-mm-dd):")
            val inputDate = toDateTimeOrNull(dateStr = readln())
            if (inputDate != null) {
                return inputDate.toString().substringBefore('T')
            }
            else println("The input date is invalid")
        }
    }
    fun setTime(): String {
        while (true) {
            println("Input the time (hh:mm):")
            val inputTime = toDateTimeOrNull(timeStr = readln())
            if (inputTime != null) {
                return inputTime.toString().substringAfter('T')
            }
            else println("The input time is invalid")
        }
    }
    fun setText() {
        println("Input a new task (enter a blank line to end):")
        text = ""
        while (true) {
            val text = readln().trim()
            if (text.isNotEmpty())
                this.addLn(text)
            else
                break
        }
    }
    fun addLn(text: String) {
        this.text += (if (this.text == "") "" else "\n") + text
    }
    fun edit() {
        val editedField: Field
        while (true) {
            println("Input a field to edit (priority, date, time, task):")
            val inputField = findField(readln().lowercase())
            if (inputField != null) {
                editedField = inputField
                break
            }
            else println("Invalid field")
        }
        when (editedField) {
            Field.Priority -> priority = setPriority()
            Field.Date -> date = setDate()
            Field.Time -> time = setTime()
            Field.Task -> setText()
        }
        println("The task is changed")
    }
    fun isEmpty(): Boolean = (text == "")
    override fun toString(): String {
        return "$date $time $priority $state\n$text"
    }
}

class TaskList {
    var list = mutableListOf<Task>()
    val jsonAdapter: JsonAdapter<MutableList<Task>>
        get() { return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter<MutableList<Task>>(Types.newParameterizedType(List::class.java, Task::class.java))}
    init {
        load()
    }
    fun add(task: Task) = list.add(task)
    fun edit(index: Int) {
        list[index].edit()
    }
    fun delete(index: Int) {
        list.removeAt(index)
        println("The task is deleted")
    }
    fun isEmpty(): Boolean = (list.isEmpty())
    fun selectIndex(): Int {
        while (true) {
            println("Input the task number (1-${list.size}):")
            val index = try { readln().toInt() - 1 } catch (e: NumberFormatException) { -1 }
            if (index in 0..list.lastIndex)
                return index
            else println("Invalid task number")
        }
    }
    fun textAdoption(oldText: String, width: Int = 44): List<String> {
        val listStr = mutableListOf<String>()
        for (line in oldText.lines()) {
            listStr.addAll(line.chunked(width))
            listStr[listStr.lastIndex] += " ".repeat(width - listStr[listStr.lastIndex].length)
        }
        return listStr
    }

    fun save(fileName: String = "tasklist.json") {
        val text = jsonAdapter.toJson(list)
        val file = File(fileName)
        file.writeText(text)
    }
    fun load(fileName: String = "tasklist.json") {
        val file = File(fileName)
        if (file.exists())
            list = jsonAdapter.fromJson(file.readText())?:return
    }
    override fun toString(): String {
        val table: StringBuilder = StringBuilder(TABLE_HEAD)
        for ((index, task) in list.withIndex()) {
            val (date, time, priority, state, text) = task
            val tableIndex = "${index + 1}${if (index < 9) " " else ""}"
            val tableText = textAdoption(text)
            table.append('\n')
            table.append("| $tableIndex | $date | $time | $priority | $state |${tableText[0]}|")
            repeat(tableText.size - 1) {
                table.append('\n')
                table.append(TABLE_SUBLIME.replace("TEXT_LINE", tableText[it + 1]))
            }

            table.append('\n')
            table.append(TABLE_SEPARATOR)
        }
        return table.toString()
    }

}

fun toDateTimeOrNull(dateStr: String = "2000-01-01", timeStr: String = "00:00"): LocalDateTime? {
    return try {
        val dateArr = dateStr.split("-").map { it.toInt() }
        val timeArr = timeStr.split(":").map { it.toInt() }
        LocalDateTime(dateArr[0], dateArr[1], dateArr[2], timeArr[0], timeArr[1])
    }
    catch (e: Exception) {
        null
    }
}