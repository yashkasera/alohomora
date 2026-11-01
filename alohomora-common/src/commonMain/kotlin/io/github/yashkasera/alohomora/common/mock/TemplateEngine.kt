package io.github.yashkasera.alohomora.common.mock

object TemplateEngine {

    private val pattern = Regex("""\{\{(\w+)(?:\(([^)]*)\))?\}\}""")

    /**
     * Replaces every `{{generator(args)}}` in [template] with a fresh value.
     *
     * Never throws. It runs inside the host app's request (`NetworkRuleEngine.findMatch`, from the
     * OkHttp interceptors and the iOS URLProtocol), where an exception is an app crash: OkHttp
     * rethrows non-IO exceptions on its dispatcher thread, and a Kotlin exception crossing into
     * Objective-C terminates the process. So a generator that fails leaves its own placeholder as
     * literal text, the way an unknown name does, and the rest of the body still resolves.
     */
    fun resolve(template: String): String {
        if (!template.contains("{{")) return template
        return pattern.replace(template) { match ->
            val name = match.groupValues[1].lowercase()
            val args = match.groupValues[2]
                .takeIf { it.isNotBlank() }
                ?.split(",")
                ?.map { it.trim() }
                ?: emptyList()
            runCatching { generate(name, args) }.getOrNull() ?: match.value
        }
    }

    private fun generate(name: String, args: List<String>): String? = when (name) {
        "uuid" -> MockGenerators.uuid()
        "name" -> MockGenerators.name()
        "firstname" -> MockGenerators.firstName()
        "lastname" -> MockGenerators.lastName()
        "email" -> MockGenerators.email()
        "bool" -> MockGenerators.bool()
        "timestamp" -> MockGenerators.timestamp()
        "int" -> {
            val min = args.getOrNull(0)?.toIntOrNull() ?: 0
            val max = args.getOrNull(1)?.toIntOrNull() ?: 100
            MockGenerators.int(min, max)
        }

        "float" -> {
            val min = args.getOrNull(0)?.toFiniteDoubleOrNull() ?: 0.0
            val max = args.getOrNull(1)?.toFiniteDoubleOrNull() ?: 1.0
            MockGenerators.float(min, max)
        }

        "amount" -> {
            val min = args.getOrNull(0)?.toFiniteDoubleOrNull() ?: 0.0
            val max = args.getOrNull(1)?.toFiniteDoubleOrNull() ?: 1000.0
            MockGenerators.amount(min, max)
        }

        "date" -> {
            val direction = args.getOrNull(0) ?: "past"
            val offset = args.getOrNull(1)?.toIntOrNull() ?: 30
            MockGenerators.date(direction, offset)
        }

        "oneof" -> MockGenerators.oneOf(args)
        else -> null
    }

    // "NaN" and "Infinity" parse as doubles but are not a range, so they get the default too.
    private fun String.toFiniteDoubleOrNull(): Double? = toDoubleOrNull()?.takeIf { it.isFinite() }
}
