package io.github.vinicreis.dht.core.grpc.infra.service

import com.google.protobuf.ByteString
import io.github.vinicreis.dht.core.grpc.domain.strategy.ClientStubStrategy
import io.github.vinicreis.dht.core.grpc.domain.extensions.asByteString
import io.github.vinicreis.dht.core.model.DataTypeOuterClass
import io.github.vinicreis.dht.core.model.data
import io.github.vinicreis.dht.core.model.request.foundRequest
import io.github.vinicreis.dht.core.model.request.notFoundRequest
import io.github.vinicreis.dht.core.service.domain.DHTServiceClientStub
import io.github.vinicreis.dht.model.service.Node
import kotlinx.coroutines.withContext
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

internal class DHTServiceClientStubImpl(
    private val coroutineContext: CoroutineContext,
    private val stubStrategy: ClientStubStrategy,
    private val logger: Logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName)
) :
    DHTServiceClientStub,
    ClientStubStrategy by stubStrategy
{
    override suspend fun Node.found(key: String, data: ByteArray) {
        withContext(coroutineContext) {
            logger.fine("Sending FOUND to $id")

            withStub {
                found(
                    foundRequest {
                        this.key = key.asByteString
                        this.data = data {
                            type = DataTypeOuterClass.DataType.BYTE
                            content = ByteString.copyFrom(data)
                        }
                    }
                )
            }
        }
    }

    override suspend fun Node.notFound(key: String) {
        withContext(coroutineContext) {
            logger.fine("Sending NOT_FOUND to $id")

            withStub {
                notFound(
                    notFoundRequest {
                        this.key = key.asByteString
                    }
                )
            }
        }
    }
}