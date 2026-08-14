package io.github.yashkasera.alohomora.ui.components.jsonviewer

internal fun preview(
    node: JsonNode,
    tree: JsonTree,
): String {

    val children = tree.children[node.path] ?: return ""
    val isArray = node is JsonArrayNode

    return children
        .take(3) // preview first few properties
        .mapNotNull { path ->

            when (val child = tree.nodes[path]) {

                is JsonValueNode -> {
                    if (isArray)
                        if (child.value is String) {
                            "\"${child.value}\""
                        } else
                            child.value.toString()
                    else
                        "${child.key}:\"${child.value}\""
                }

                is JsonObjectNode ->
                    if (isArray)
                        "{…}"
                    else
                        "${child.key}:{…}"

                is JsonArrayNode ->
                    if (isArray)
                        "[…]"
                    else
                        "${child.key}:[…]"

                else -> null
            }
        }
        .joinToString(", ")
        .replace("\n", " ")
        .replace(Regex("\\s+"), " ")
}
