package io.github.vinicreis.dht.model.service

@JvmInline
value class Address(val value: String) {
    init {
        require(value.isNotBlank()) { "Address could not be blank" }
    }

    override fun toString(): String = value
}
