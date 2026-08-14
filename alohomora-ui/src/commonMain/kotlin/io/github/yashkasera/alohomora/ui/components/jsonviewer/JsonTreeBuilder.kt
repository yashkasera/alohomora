package io.github.yashkasera.alohomora.ui.components.jsonviewer

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

internal object JsonTreeBuilder {

    fun build(element: JsonElement): JsonTree {

        val nodes = mutableMapOf<Path, JsonNode>()
        val children = mutableMapOf<Path, List<Path>>()
        val search = SearchIndex()

        fun walk(el: JsonElement, path: Path, key: String?) {

            when (el) {

                is JsonObject -> {

                    val childPaths = el.entries.map { "$path.${it.key}" }

                    nodes[path] = JsonObjectNode(path, key, childPaths)
                    children[path] = childPaths

                    el.entries.forEach { (k, v) ->
                        walk(v, "$path.$k", k)
                        search.insert(k.lowercase(), "$path.$k")
                    }
                }

                is JsonArray -> {

                    val childPaths = el.indices.map { "$path[$it]" }

                    nodes[path] = JsonArrayNode(path, key, childPaths)
                    children[path] = childPaths

                    el.forEachIndexed { i, v ->
                        walk(v, "$path[$i]", i.toString())
                    }
                }

                is JsonPrimitive -> {

                    val value: Any =
                        when {
                            el.isString -> el.content
                            el.booleanOrNull != null -> el.boolean
                            el.longOrNull != null -> el.long
                            el.doubleOrNull != null -> el.double
                            else -> el.content
                        }

                    nodes[path] = JsonValueNode(path, key, value)

                    search.insert(value.toString().lowercase(), path)
                }
            }
        }

        walk(element, "$", null)

        return JsonTree(
            root = "$",
            nodes = nodes,
            children = children,
            searchIndex = search,
        )
    }
}
