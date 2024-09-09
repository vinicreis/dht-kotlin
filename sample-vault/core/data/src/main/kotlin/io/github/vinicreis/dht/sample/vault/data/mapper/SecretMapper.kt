package io.github.vinicreis.dht.sample.vault.data.mapper

import io.github.vinicreis.dht.sample.vault.data.model.SecretDTO
import io.github.vinicreis.dht.sample.vault.model.Secret

internal val SecretDTO.asDomain: Secret
    get() = Secret(
        key = key,
        value = value,
    )

internal val Secret.asDto: SecretDTO
    get() = SecretDTO(
        key = key,
        value = value,
    )
