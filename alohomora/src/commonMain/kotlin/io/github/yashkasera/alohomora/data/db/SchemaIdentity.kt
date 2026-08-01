package io.github.yashkasera.alohomora.data.db

/**
 * Recognises Room's schema-identity failure, the one open error worth recovering from by deleting
 * the file.
 *
 * Room stores a hash of the compiled schema in `room_master_table` and compares it on open. A
 * mismatch means the entities changed while [AlohomoraDb]'s `version` stayed the same, and it is
 * the one corruption-shaped failure that `fallbackToDestructiveMigration` does **not** cover:
 * that fallback only fires on a version *change*, so same-version-different-schema throws out of
 * every DAO call for the life of the install instead.
 *
 * Recovering automatically is safe here specifically because this database is a disposable capture
 * buffer — the same reasoning that already justifies destructive migration. It is not a general
 * licence to delete on any open error, which is why this matches narrowly rather than catching
 * bare [Exception]: a transient lock or a low-disk failure must keep the developer's captured data.
 *
 * Bumping the version is still the correct fix when the schema changes deliberately. This exists
 * for the case discipline cannot cover: switching between two branches whose entities differ at
 * the same version, which produces this on every switch.
 */
internal fun Throwable.isSchemaIdentityMismatch(): Boolean {
    var cause: Throwable? = this
    // Walk the chain: Room's check runs inside the open callback, so the useful message is
    // routinely wrapped by the driver or by the coroutine that first touched a DAO.
    while (cause != null) {
        val message = cause.message.orEmpty()
        if (message.contains(IDENTITY_HASH_MARKER, ignoreCase = true) ||
            message.contains(INTEGRITY_MARKER, ignoreCase = true)
        ) {
            return true
        }
        cause = cause.cause?.takeIf { it !== cause }
    }
    return false
}

/** Present in every Room version that emits the mismatch, and in no other Room error. */
private const val INTEGRITY_MARKER = "cannot verify the data integrity"

/** Belt and braces: the wording above has been reworded before, the hash label has not. */
private const val IDENTITY_HASH_MARKER = "identity hash"
