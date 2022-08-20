package cmd

import Readme

fun main() {
    println("Welcome to Transactional Key-Value store!")
    println("Enter first command to start working with the app.")
    println()

    Readme.print()

    val store = CmdStoreWrapper()

    var request = readlnOrNull()
    while (request != null) {
        if (request.isEmpty()) println("Please, enter command") else {
            try {
                val (command, arg) = Command.parse(request)
                when {
                    command == Command.Set && arg is Argument.KeyValue -> store.set(arg.key, arg.value)
                    command == Command.Get && arg is Argument.Key -> store.get(arg.key)
                    command == Command.Delete && arg is Argument.Key -> store.delete(arg.key)
                    command == Command.Count && arg is Argument.Key -> store.count(arg.key)
                    command == Command.Begin -> store.beginTransaction()
                    command == Command.Commit -> store.commitTransaction()
                    command == Command.Rollback -> store.rollbackTransaction()
                    command == Command.Help -> Readme.print()
                    command == Command.Exit -> {
                        println("Exiting...")
                        return
                    }
                    else -> throw RuntimeException("Wrong command to arguments relation, command: $command, args: $arg. This must not happen!")
                }
            } catch (e: IllegalCommandException) {
                println(e.message)
            } catch (e: WrongArgumentsException) {
                println(e.message)
            }
        }
        request = readlnOrNull()
    }
}
