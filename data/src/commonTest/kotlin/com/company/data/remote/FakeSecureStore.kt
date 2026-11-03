package com.company.data.remote

/** Secure storage in memory for tests. */
internal class FakeSecureStore : SecureStore {
    val values = mutableMapOf<String, String>()

    /** When true, each write fails, as on a locked Keychain. */
    var failWrites = false

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String) {
        check(!failWrites) { "The storage is locked" }
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
