package io.github.vinicreis.dht.core.grpc.domain.service

import io.github.vinicreis.dht.core.grpc.domain.strategy.HashStrategy
import io.github.vinicreis.dht.core.grpc.infra.service.DHTServiceGrpcServerImpl
import io.github.vinicreis.dht.core.grpc.infra.strategy.HashStrategyMD5
import io.github.vinicreis.dht.core.grpc.infra.strategy.NodeStubStrategyGrpc
import io.github.vinicreis.dht.core.model.request.FoundRequestOuterClass.FoundRequest
import io.github.vinicreis.dht.core.model.request.GetRequestOuterClass.GetRequest
import io.github.vinicreis.dht.core.model.request.JoinOkRequestOuterClass.JoinOkRequest
import io.github.vinicreis.dht.core.model.request.JoinRequestOuterClass.JoinRequest
import io.github.vinicreis.dht.core.model.request.LeaveRequestOuterClass.LeaveRequest
import io.github.vinicreis.dht.core.model.request.NewNodeRequestOuterClass.NewNodeRequest
import io.github.vinicreis.dht.core.model.request.NodeGoneRequestOuterClass.NodeGoneRequest
import io.github.vinicreis.dht.core.model.request.NotFoundRequestOuterClass.NotFoundRequest
import io.github.vinicreis.dht.core.model.request.SetRequestOuterClass.SetRequest
import io.github.vinicreis.dht.core.model.request.TransferRequestOuterClass.TransferRequest
import io.github.vinicreis.dht.core.model.response.FoundResponseOuterClass.FoundResponse
import io.github.vinicreis.dht.core.model.response.GetResponseOuterClass.GetResponse
import io.github.vinicreis.dht.core.model.response.JoinOkResponseOuterClass.JoinOkResponse
import io.github.vinicreis.dht.core.model.response.JoinResponseOuterClass.JoinResponse
import io.github.vinicreis.dht.core.model.response.LeaveResponseOuterClass.LeaveResponse
import io.github.vinicreis.dht.core.model.response.NewNodeResponseOuterClass.NewNodeResponse
import io.github.vinicreis.dht.core.model.response.NodeGoneResponseOuterClass.NodeGoneResponse
import io.github.vinicreis.dht.core.model.response.NotFoundResponseOuterClass.NotFoundResponse
import io.github.vinicreis.dht.core.model.response.SetResponseOuterClass.SetResponse
import io.github.vinicreis.dht.core.model.response.TransferResponseOuterClass.TransferResponse
import io.github.vinicreis.dht.core.service.domain.DHTService
import io.github.vinicreis.dht.core.service.domain.model.Address
import io.github.vinicreis.dht.core.service.domain.model.Node
import io.github.vinicreis.dht.core.service.domain.model.Port
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext

fun DHTServiceGrpc(
    id: Long,
    address: Address,
    port: Port,
    knownNodes: List<Node>,
    hashStrategy: HashStrategy,
    coroutineContext: CoroutineContext,
): DHTService = DHTServiceGrpcServerImpl(
    info = Node(id, address, port),
    knownNodes = knownNodes,
    coroutineContext = coroutineContext,
    hashStrategy = hashStrategy,
    nodeStubStrategy = NodeStubStrategyGrpc(),
)

interface DHTServiceGrpc {
    suspend fun join(request: JoinRequest): JoinResponse
    suspend fun joinOk(request: JoinOkRequest): JoinOkResponse
    suspend fun newNode(request: NewNodeRequest): NewNodeResponse
    suspend fun nodeGone(request: NodeGoneRequest): NodeGoneResponse
    suspend fun leave(request: LeaveRequest): LeaveResponse
    suspend fun get(request: GetRequest): GetResponse
    suspend fun set(request: SetRequest): SetResponse
    suspend fun found(request: FoundRequest): FoundResponse
    suspend fun notFound(request: NotFoundRequest): NotFoundResponse
    suspend fun transfer(requests: Flow<TransferRequest>): TransferResponse
}
