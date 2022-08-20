package cmd

sealed interface Argument {
    data class Key(val key: String) : Argument
    data class KeyValue(val key: String, val value: String) : Argument
    object NoArg : Argument
    object AnyArg : Argument

    companion object {

        inline fun <reified T : Argument> parseArgument(args: List<String>): T {
            val arguments = when (T::class.java) {
                KeyValue::class.java -> {
                    if (args.size == 2) {
                        val (key, value) = args
                        KeyValue(key, value)
                    } else throw WrongArgumentsException(args, T::class.java)
                }
                Key::class.java -> {
                    if (args.size == 1) {
                        Key(args.single())
                    } else throw WrongArgumentsException(args, T::class.java)
                }
                NoArg::class.java -> if (args.isEmpty()) NoArg else throw WrongArgumentsException(args, T::class.java)
                AnyArg::class.java -> AnyArg
                else -> throw WrongArgumentsException(args, T::class.java)
            }
            return arguments as T
        }
    }
}

class WrongArgumentsException(args: List<String>, argsType: Class<out Argument>) :
    IllegalArgumentException("Wrong args $args for expected type of: ${argsType.simpleName}")
