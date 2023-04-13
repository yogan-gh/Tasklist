fun main() {
    val taskList = TaskList()
    while (true) {
        println("Input an action (add, print, edit, delete, end):")
        when (val inputCom = readln().trim().lowercase()) {
            "add" -> {
                val task = Task().build()
                if (task.isEmpty())
                    println("The task is blank")
                else
                    taskList.add(task)
            }
            "end" -> {
                taskList.save()
                println("Tasklist exiting!")
                break
            }
            "test" -> taskList.load()
            "print", "edit", "delete" ->
                if (taskList.isEmpty())
                    println("No tasks have been input")
                else
                    when (inputCom) {
                        "print" -> println(taskList)
                        "edit" -> {
                            println(taskList)
                            taskList.edit(taskList.selectIndex())
                        }
                        "delete" -> {
                            println(taskList)
                            taskList.delete(taskList.selectIndex())
                        }
                    }
            else -> println("The input action is invalid")
        }
    }
}
