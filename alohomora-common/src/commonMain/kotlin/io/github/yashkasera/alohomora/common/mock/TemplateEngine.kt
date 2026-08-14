package io.github.yashkasera.alohomora.common.mock

object TemplateEngine {

    private val pattern = Regex("""\{\{(\w+)(?:\(([^)]*)\))?\}\}""")

    fun resolve(template: String): String {
        if (!template.contains("{{")) return template
        return pattern.replace(template) { match ->
            val name = match.groupValues[1].lowercase()
            val args = match.groupValues[2]
                .takeIf { it.isNotBlank() }
                ?.split(",")
                ?.map { it.trim() }
                ?: emptyList()
            generate(name, args) ?: match.value
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
            val min = args.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val max = args.getOrNull(1)?.toDoubleOrNull() ?: 1.0
            MockGenerators.float(min, max)
        }

        "amount" -> {
            val min = args.getOrNull(0)?.toDoubleOrNull() ?: 0.0
            val max = args.getOrNull(1)?.toDoubleOrNull() ?: 1000.0
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
}
