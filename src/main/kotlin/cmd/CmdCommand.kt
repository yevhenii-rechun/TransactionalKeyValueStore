package cmd

enum class Command(val id: String) {
    Set("SET"),
    Get("GET"),
    Delete("DELETE"),
    Count("COUNT"),
    Begin("BEGIN"),
    Commit("COMMIT"),
    Rollback("ROLLBACK"),
    Help("HELP"),
    Exit("EXIT");

    companion object
}

fun Command.Companion.parse(request: String): CommandWithArgs {
    val all = request.split(" ")
    if (all.isEmpty()) throw IllegalCommandException()
    val id = all.first()
    val args = all.drop(1)

    val command = Command.values().firstOrNull { it.id.equals(id, ignoreCase = true) }
        ?: throw IllegalCommandException(id)

    val argument = when (command) {
        Command.Set -> Argument.parseArgument<Argument.KeyValue>(args)
        Command.Get, Command.Delete, Command.Count -> Argument.parseArgument<Argument.Key>(args)
        Command.Begin, Command.Commit, Command.Rollback -> Argument.parseArgument<Argument.NoArg>(args)
        Command.Help, Command.Exit -> Argument.parseArgument<Argument.AnyArg>(args)
    }

    return command to argument
}

class IllegalCommandException(id: String? = null) : IllegalArgumentException(
    if (id == null) "Request command is empty" else "Command $id is not supported, use HELP to print list of available commands"
)

typealias CommandWithArgs = Pair<Command, Argument>
