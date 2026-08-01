package io.github.yashkasera.alohomora.utils.share

import androidx.core.content.FileProvider

/**
 * Dedicated [FileProvider] subclass for the library.
 *
 * The Android manifest merger keys `<provider>` elements by their `android:name`.
 * A host app almost always declares its own `androidx.core.content.FileProvider`,
 * so reusing that class here collapses both providers into one merged node and the
 * conflicting `android.support.FILE_PROVIDER_PATHS` meta-data fails the merge. A
 * unique class name keeps the library's provider as a distinct node with its own
 * authority and file-paths resource.
 */
class AlohomoraFileProvider : FileProvider()
