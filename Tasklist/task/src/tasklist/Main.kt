package tasklist

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.datetime.*
import java.io.File

private const val INPUT_ACTION_MESSAGE ="Input an action (add, print, edit, delete, end):"
private const val INPUT_PRIORITY_MESSAGE = "Input the task priority (C, H, N, L):"
private const val INPUT_DATE_MESSAGE = "Input the date (yyyy-mm-dd):"
private const val INPUT_TIME_MESSAGE = "Input the time (hh:mm):"
private const val INPUT_TASKS_MESSAGE = "Input a new task (enter a blank line to end):"
private const val INPUT_FIELD_MESSAGE = "Input a field to edit (priority, date, time, task):"
private const val TASK_CHANGED_MESSAGE = "The task is changed"
private const val TASK_DELETED_MESSAGE = "The task is deleted"
private const val END_MESSAGE = "Tasklist exiting!"

private const val INVALID_ACTION_MESSAGE = "The input action is invalid"
private const val INVALID_DATE_MESSAGE = "The input date is invalid"
private const val INVALID_TIME_MESSAGE = "The input time is invalid"
private const val BLANK_TASK_MESSAGE = "The task is blank"
private const val NO_TASKS_MESSAGE = "No tasks have been input"
private const val INVALID_INDEX_MESSAGE = "Invalid task number"
private const val INVALID_FIELD_MESSAGE = "Invalid field"

private const val ROW_SEPARATOR = "+----+------------+-------+---+---+--------------------------------------------+"
private const val HEADERS =       "| N  |    Date    | Time  | P | D |                   Task                     |"
private const val ROW_EMPTY =     "|    |            |       |   |  "

private const val REGEX_DATE = "([0-9]{4})\\-((0?[1-9])|(1[0-2]))\\-(([0-2]?[0-9])|(3[01]))"
private const val REGEX_TIME = "(([01]?[0-9])|2[0-3]):([0-5]?[0-9])"

private const val JSON_FILEPATH = "tasklist.json"
private const val BLANK = ' '
private const val NO_VALID_VALUE = ""
private const val NO_INDEX = -1
private const val MAX_LINE_LENGTH = 44
private const val MIN_NUMBER_LENGTH = 2

fun main() {
    val taskList = readTaskListFile()
    var action = ""
    while (action != "end") {
        println(INPUT_ACTION_MESSAGE)
        action = readln()
        when(action){
            "add" -> inputTasks(taskList)
            "print" -> outputTasks(taskList)
            "edit" -> inputEditTask(taskList)
            "delete" -> inputDeleteTask(taskList)
            "end" -> println(END_MESSAGE)
            else -> println(INVALID_ACTION_MESSAGE)
        }
    }
    saveTaskListFile(taskList)
}

private fun readTaskListFile(): MutableList<Task> =
    if(File(JSON_FILEPATH).exists()){
        jsonAdapter().fromJson(File(JSON_FILEPATH).readText())!!.toMutableList()
    } else mutableListOf()

private fun saveTaskListFile(taskList: List<Task>) {
    File(JSON_FILEPATH).writeText(jsonAdapter().toJson(taskList))
}

private fun jsonAdapter(): JsonAdapter<List<Task>> {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val type = Types.newParameterizedType(List::class.java, Task::class.java)
    return moshi.adapter(type)!!
}

private fun inputTasks(taskList: MutableList<Task>) {
    val priority = inputPriority()
    val date = inputDate()
    val time = inputTime()
    val taskLines = inputTaskLines()
    if(taskLines.isEmpty()){
        println(BLANK_TASK_MESSAGE)
    }else{
        taskList.add(Task(priority, date, time, taskLines))
    }
}

private fun inputPriority(): Char {
    var priority = BLANK
    while (priority == BLANK) {
        println(INPUT_PRIORITY_MESSAGE)
        priority = getPriority(readln())
    }
    return priority
}

private fun getPriority(priority: String): Char = when(priority.trim()) {
    "C", "c" -> 'C'
    "H", "h" -> 'H'
    "N", "n" -> 'N'
    "L", "l" -> 'L'
    else -> BLANK
}

private fun inputDate(): String {
    var date = NO_VALID_VALUE
    while (date == NO_VALID_VALUE) {
        println(INPUT_DATE_MESSAGE)
        date = getValidDate(readln().trim())
    }
    return date
}

private fun getValidDate(date: String) =
    if(REGEX_DATE.toRegex().matches(date)){
        val (year, month, day) = date.split("-").map { it.padStart(MIN_NUMBER_LENGTH, '0') }
        if(day.toInt() > 28) {
           try {
               "${year}-${month}-${day}T00:00:00Z".toInstant().toString().split("T")[0]
           }catch (e: RuntimeException){
               println(INVALID_DATE_MESSAGE)
               NO_VALID_VALUE
           }
        } else {
           "${year}-${month}-${day}"
        }
    } else {
        println(INVALID_DATE_MESSAGE)
        NO_VALID_VALUE
    }

private fun inputTime(): String {
    var time = NO_VALID_VALUE
    while(time == NO_VALID_VALUE){
        println(INPUT_TIME_MESSAGE)
        time = getValidTime(readln().trim())
    }
    return time
}

private fun getValidTime(time: String) =
    if (REGEX_TIME.toRegex().matches(time)) {
    val (hour, minute) = time.split(":").map { it.padStart(MIN_NUMBER_LENGTH, '0') }
    "$hour:$minute"
} else {
    println(INVALID_TIME_MESSAGE)
    NO_VALID_VALUE
}

private fun inputTaskLines(): List<String> {
    println(INPUT_TASKS_MESSAGE)
    val taskLines = mutableListOf<String>()
    var task = readln().trim()
    while (task.isNotEmpty()) {
        taskLines.addAll(splitTaskLines(task))
        task = readln().trim()
    }
    return taskLines
}

private fun splitTaskLines(inputTask: String): List<String> {
    val lines = mutableListOf<String>()
    if(inputTask.length > MAX_LINE_LENGTH) {
        lines.add(inputTask.substring(0, MAX_LINE_LENGTH))
        lines.addAll(splitTaskLines(inputTask.substring(MAX_LINE_LENGTH)))
    } else {
        lines.add(inputTask)
    }
    return lines
}

private fun outputTasks(taskList: MutableList<Task>) {
    if(taskList.isEmpty()){
        println(NO_TASKS_MESSAGE)
    } else {
        println(ROW_SEPARATOR)
        println(HEADERS)
        println(ROW_SEPARATOR)
        for ((index, task) in taskList.withIndex()) {
            val taskNum = (index + 1).toString().padEnd(MIN_NUMBER_LENGTH, BLANK)
            val (priority, date, time, taskLines) = task
            println("| $taskNum | $date | $time | ${getTagColor(priority)} | " +
                    "${getTagColor(getOverdueTag(date))} |${taskLines.first().padEnd(MAX_LINE_LENGTH, BLANK)}|")
            for (i in 1 .. taskLines.lastIndex){
                println("$ROW_EMPTY |${taskLines[i].padEnd(MAX_LINE_LENGTH, BLANK)}|")
            }
            println(ROW_SEPARATOR)
        }
    }
}

private fun getTagColor(tag: Char) = when (tag){
    'C', 'O' -> "\u001B[101m \u001B[0m"
    'H', 'T' -> "\u001B[103m \u001B[0m"
    'N', 'I' -> "\u001B[102m \u001B[0m"
    'L' -> "\u001B[104m \u001B[0m"
    else -> " "
}

private fun getOverdueTag(date: String): Char {
    val daysLeft = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        .daysUntil(date.toLocalDate())
    return when  {
        daysLeft > 0 -> 'I'
        daysLeft < 0 -> 'O'
        else -> 'T'
    }
}

private fun inputEditTask(taskList: MutableList<Task>) {
    outputTasks(taskList)
    if(taskList.isNotEmpty()){
        val index = inputIndex(taskList)
        when (inputField()) {
            "priority" -> taskList[index].priority = inputPriority()
            "date" -> taskList[index].date = inputDate()
            "time" -> taskList[index].time = inputTime()
            "task" -> taskList[index].taskLines = inputTaskLines()
        }
        println(TASK_CHANGED_MESSAGE)
    }
}

private fun inputField(): String {
    var field = NO_VALID_VALUE
    while (field == NO_VALID_VALUE) {
        println(INPUT_FIELD_MESSAGE)
        val inputField = readln().trim()
        field = when(inputField){
            "priority", "date", "time", "task" -> inputField
            else -> {
                println(INVALID_FIELD_MESSAGE)
                NO_VALID_VALUE
            }
        }
    }
    return field
}

private fun inputDeleteTask(taskList: MutableList<Task>) {
    outputTasks(taskList)
    if(taskList.isNotEmpty()) {
        taskList.removeAt(inputIndex(taskList))
        println(TASK_DELETED_MESSAGE)
    }
}

private fun inputIndex(taskList: List<Task>): Int {
    var index = NO_INDEX
    while (index == NO_INDEX) {
        println("Input the task number (1-${taskList.size}):")
        index = getValidIndex(readln(), taskList)
    }
    return index
}

private fun getValidIndex(inputIndex: String, taskList: List<Task>): Int =
    try {
        val index = inputIndex.toInt() - 1
        if (index in taskList.indices ) index else {
            println(INVALID_INDEX_MESSAGE)
            NO_INDEX
        }
    }catch (e: NumberFormatException) {
        println(INVALID_INDEX_MESSAGE)
        NO_INDEX
    }

data class Task(var priority: Char, var date: String, var time: String, var taskLines: List<String>)