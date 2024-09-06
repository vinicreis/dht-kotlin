package io.github.vinicreis.dht.core.grpc.infra.service

import com.google.protobuf.ByteString
import io.github.vinicreis.dht.core.grpc.domain.strategy.NodeStubStrategy
import io.github.vinicreis.dht.core.grpc.infra.extensions.asByteString
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
    private val nodeStubStrategy: NodeStubStrategy,
    private val logger: Logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.java.simpleName)
) :
    DHTServiceClientStub,
    NodeStubStrategy by nodeStubStrategy
{
    override suspend fun Node.found(key: String, data: ByteArray) {
        withContext(coroutineContext) {
            logger.info("Sending FOUND to $id")

            withClientStub {
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
            logger.info("Sending NOT_FOUND to $id")

            withClientStub {
                notFound(
                    notFoundRequest {
                        this.key = key.asByteString
                    }
                )
            }
        }
    }
}