import cmd.Command

object Readme {

    private val serviceCommands = setOf(Command.Help, Command.Exit)
    private val storeManageCommands = Command.values().filterNot(serviceCommands::contains)

    fun print() {
        println("Key-Value store commands:")
        storeManageCommands.map(Command::comment).forEach(::println)
        println("")
        println("Service commands:")
        serviceCommands.map(Command::comment).forEach(::println)
        println()
    }
}

fun Command.comment(): String {
    return when (this) {
        Command.Set -> "SET <key> <value> # store the value for key"
        Command.Get -> "GET <key>         # return the current value for key"
        Command.Delete -> "DELETE <key>      # remove the entry for key"
        Command.Count -> "COUNT <value>     # return the number of keys that have the given value"
        Command.Begin -> "BEGIN             # start a new transaction"
        Command.Commit -> "COMMIT            # complete the current transaction"
        Command.Rollback -> "ROLLBACK          # revert to state prior to BEGIN call"
        Command.Help -> "HELP              # prints list of available commands"
        Command.Exit -> "EXIT              # terminates program"
    }
}
