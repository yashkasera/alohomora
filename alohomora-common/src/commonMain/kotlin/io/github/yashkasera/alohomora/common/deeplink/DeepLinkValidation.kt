package io.github.yashkasera.alohomora.common.deeplink

/** The outcome of building a concrete URL: [url] is non-null exactly when [errors] is empty. */
data class DeepLinkBuildResult(val url: String?, val errors: List<String>)

/**
 * Per-field structural problems with a [DeepLinkDef], used to gate saving and to drive inline field
 * errors in the editor. [isValid] is what blocks a write — [warnings] are advisory (a template using
 * `{placeholder}` with no declared param, or an example that doesn't fit) and never block a save.
 */
data class DeepLinkFieldErrors(
    val name: String? = null,
    val module: String? = null,
    val uriTemplate: String? = null,
    /** Keyed by param index, so the editor can mark the offending row. */
    val params: Map<Int, String> = emptyMap(),
    val warnings: List<String> = emptyList(),
) {
    val isValid: Boolean
        get() = name == null && module == null && uriTemplate == null && params.isEmpty()

    /** Blocking errors only, flattened for a summary count/badge. */
    val blockingCount: Int
        get() = listOfNotNull(name, module, uriTemplate).size + params.size
}

/**
 * Validates the *shape* of a definition — the fields that must be present for it to be a usable,
 * shareable deep link — independent of whether its examples happen to match. A blank name/module/
 * template, a nameless or duplicated param, or an ENUM with no allowed values all block a save; an
 * undeclared `{placeholder}` and any [validateExamples] problem surface as advisory warnings.
 */
fun DeepLinkDef.validateStructure(): DeepLinkFieldErrors {
    val nameError = if (name.isBlank()) "Name is required." else null
    val moduleError = if (module.isBlank()) "Module is required." else null
    val templateError = when {
        uriTemplate.isBlank() -> "URI template is required."
        !uriTemplate.contains("://") -> "Include a scheme, e.g. app://module/path."
        else -> null
    }

    val paramErrors = mutableMapOf<Int, String>()
    val seenNames = mutableSetOf<String>()
    params.forEachIndexed { index, param ->
        val trimmed = param.name.trim()
        when {
            trimmed.isEmpty() -> paramErrors[index] = "Parameter name is required."
            !seenNames.add(trimmed) -> paramErrors[index] = "Duplicate parameter '$trimmed'."
            param.type == ParamType.ENUM && param.allowedValues.isEmpty() ->
                paramErrors[index] = "ENUM '$trimmed' needs allowed values."
        }
    }

    val declared = params.map { it.name }.toSet()
    val warnings = buildList {
        placeholders().filter { it !in declared }
            .forEach { add("Template uses {$it} with no declared parameter.") }
        addAll(validateExamples())
    }

    return DeepLinkFieldErrors(nameError, moduleError, templateError, paramErrors, warnings)
}

private val PLACEHOLDER = Regex("""\{(\w+)}""")
private val UUID_REGEX =
    Regex("""[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}""")

/** The `{name}` placeholders in the template, in order, deduplicated. */
fun DeepLinkDef.placeholders(): List<String> =
    PLACEHOLDER.findAll(uriTemplate).map { it.groupValues[1] }.distinct().toList()

/**
 * Builds a concrete URL by substituting [args] into [DeepLinkDef.uriTemplate], validating each against
 * its declared param. A missing required arg or a type/enum violation is an error rather than a
 * silently malformed URL — the whole point of typing the params.
 */
fun DeepLinkDef.buildUri(args: Map<String, String>): DeepLinkBuildResult {
    val errors = mutableListOf<String>()
    val byName = params.associateBy { it.name }

    for (param in params) {
        val value = args[param.name]
        if (value.isNullOrEmpty()) {
            if (param.required) errors += "Missing required parameter '${param.name}'."
            continue
        }
        validateValue(param, value)?.let { errors += it }
    }

    val url = PLACEHOLDER.replace(uriTemplate) { match ->
        val name = match.groupValues[1]
        val value = args[name]
        if (value.isNullOrEmpty()) {
            // A placeholder with no declared param and no arg is left unresolved and flagged.
            if (byName[name] == null) errors += "No value for placeholder '{$name}'."
            match.value
        } else {
            value
        }
    }

    return if (errors.isEmpty()) DeepLinkBuildResult(url, emptyList()) else DeepLinkBuildResult(null, errors)
}

/**
 * Validates that every [DeepLinkDef.examples] URL fits the template and its param types. Returns a list
 * of human-readable problems; empty means the definition is self-consistent.
 */
fun DeepLinkDef.validateExamples(): List<String> {
    val problems = mutableListOf<String>()
    val names = placeholders()
    // A regex that matches the template literally, capturing each placeholder.
    val pattern = buildString {
        var last = 0
        for (match in PLACEHOLDER.findAll(uriTemplate)) {
            append(Regex.escape(uriTemplate.substring(last, match.range.first)))
            append("(.+?)")
            last = match.range.last + 1
        }
        append(Regex.escape(uriTemplate.substring(last)))
    }
    val regex = Regex("^$pattern$")
    val byName = params.associateBy { it.name }

    for (example in examples) {
        val match = regex.matchEntire(example)
        if (match == null) {
            problems += "Example does not fit the template: $example"
            continue
        }
        names.forEachIndexed { index, name ->
            val value = match.groupValues.getOrNull(index + 1) ?: return@forEachIndexed
            byName[name]?.let { param -> validateValue(param, value)?.let { problems += "$example: $it" } }
        }
    }
    return problems
}

private fun validateValue(param: DeepLinkParam, value: String): String? = when (param.type) {
    ParamType.STRING -> null
    ParamType.INT -> if (value.toIntOrNull() == null) "'${param.name}' must be an integer, was '$value'." else null
    ParamType.BOOL ->
        if (value != "true" && value != "false") "'${param.name}' must be true or false, was '$value'." else null
    ParamType.UUID -> if (!UUID_REGEX.matches(value)) "'${param.name}' must be a UUID, was '$value'." else null
    ParamType.ENUM ->
        if (value !in param.allowedValues) {
            "'${param.name}' must be one of ${param.allowedValues}, was '$value'."
        } else {
            null
        }
}
