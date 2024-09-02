package io.github.vinicreis.dht.core.grpc.infra.extensions

import com.google.protobuf.ByteString

val String.asByteString: ByteString
    get() = ByteString.copyFromUtf8(this)
