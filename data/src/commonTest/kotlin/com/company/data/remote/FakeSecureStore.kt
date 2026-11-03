package com.company.data.remote

/** Secure storage in memory for tests. */
internal class FakeSecureStore : SecureStore {
    val values = mutableMapOf<String, String>()

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
