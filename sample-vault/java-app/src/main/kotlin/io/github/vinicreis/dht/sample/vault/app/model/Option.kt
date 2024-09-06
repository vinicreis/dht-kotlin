package io.github.vinicreis.dht.sample.vault.app.model

enum class Option {
    GET,
    SET,
    REMOVE,
    EXIT;

    companion object {
        fun fromOrdinal(ordinal: Int): Option? = entries.getOrNull(ordinal)
    }
}