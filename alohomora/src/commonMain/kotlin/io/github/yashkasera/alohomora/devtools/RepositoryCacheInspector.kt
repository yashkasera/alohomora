package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.domain.repository.CacheRepository

/**
 * Serves the desktop's cache view from the same [CacheRepository] the on-device console reads.
 *
 * Replaces a per-platform pair of inspectors that answered the same question differently, and the Android
 * one answered it wrongly: it read a single hardcoded `"${packageName}_preferences"` file, while
 * `CacheRepositoryImpl` enumerates every `*.xml` in `shared_prefs/`. An app that writes to any other
 * store name — which is the normal case — showed its keys on the phone and nothing at all on the desktop.
 * That is the "two implementations of one definition" split that already made the two consoles disagree
 * about an error row's title, so the fix is to delete the second implementation rather than patch it.
 *
 * There is no `expect`/`actual` here on purpose: [CacheRepository] is already the platform seam, so both
 * platforms get one code path and cannot drift again.
 *
 * **Known gap.** Android keys are not qualified by store, so two preference files holding the same key
 * collapse to one row on the desktop and [getValue] answers with whichever the repository sorted first.
 * The on-device console distinguishes them via `CacheEntry.storeName`; closing the gap properly means
 * carrying that field on the wire, which is an additive protocol change and not attempted here.
 */
internal class RepositoryCacheInspector(
    private val repository: CacheRepository,
) : DevToolsCacheInspector {

    /**
     * Re-scans, because this runs once per connect and is what the desktop's whole cache view is built
     * from. [CacheRepository.getAllPreferences] would serve a list cached from an earlier session.
     *
     * The scan also primes the repository's cache, which is what keeps [getValue] cheap.
     */
    override suspend fun getAllKeys(): List<String> =
        repository.refresh().map { it.key }.distinct()

    /**
     * Reads the cache primed by [getAllKeys] rather than re-scanning.
     *
     * Load-bearing for cost, not just tidiness: the desktop requests one value per key, so a re-scan here
     * would turn a connect into a full store scan per key — quadratic in the number of preferences. The
     * price is that a value cannot be read fresher than the connect-time scan, which is why the desktop
     * offers no per-key refresh.
     */
    override suspend fun getValue(key: String): String? =
        repository.getAllPreferences().firstOrNull { it.key == key }?.value
}
