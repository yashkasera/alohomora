package io.github.yashkasera.alohomora.ui.components.jsonviewer

internal typealias Path = String

internal sealed interface JsonNode {

    val path: Path
    val key: String?
}

internal data class JsonObjectNode(
    override val path: Path,
    override val key: String?,
    val children: List<Path>,
) : JsonNode

internal data class JsonArrayNode(
    override val path: Path,
    override val key: String?,
    val children: List<Path>,
) : JsonNode

internal data class JsonValueNode(
    override val path: Path,
    override val key: String?,
    val value: Any?,
) : JsonNode

internal data class JsonTree(
    val root: Path,
    val nodes: Map<Path, JsonNode>,
    val children: Map<Path, List<Path>>,
    val searchIndex: SearchIndex,
)
