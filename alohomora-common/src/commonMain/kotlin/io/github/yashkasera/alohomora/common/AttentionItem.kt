package io.github.yashkasera.alohomora.common

sealed class AttentionItem : Comparable<AttentionItem> {
    abstract val timestamp: Long

    data class FailedTraffic(val entry: TrafficEntry) : AttentionItem() {
        override val timestamp: Long get() = entry.time ?: 0L
    }

    data class UnviewedError(val error: Error) : AttentionItem() {
        override val timestamp: Long get() = error.time
    }

    override fun compareTo(other: AttentionItem): Int =
        other.timestamp.compareTo(timestamp)
}

fun mergeAttentionItems(
    errors: List<Error>,
    traffic: List<TrafficEntry>,
): List<AttentionItem> {
    val errorItems = errors
        .filter { !it.isViewed }
        .map { AttentionItem.UnviewedError(it) }
    val trafficItems = traffic
        .filter { !it.isViewed && !it.isSuccessful() }
        .map { AttentionItem.FailedTraffic(it) }
    return (errorItems + trafficItems).sorted()
}
