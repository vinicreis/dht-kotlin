package io.github.vinicreis.dht.core.grpc.domain.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.vinicreis.dht.core.grpc.domain.strategy.HashStrategy
import io.github.vinicreis.dht.core.grpc.infra.service.DHTServiceGrpcServerImpl
import io.github.vinicreis.dht.core.grpc.infra.strategy.NodeStubStrategyGrpc
import io.github.vinicreis.dht.core.model.request.GetRequestOuterClass.GetRequest
import io.github.vinicreis.dht.core.model.request.JoinOkRequestOuterClass.JoinOkRequest
import io.github.vinicreis.dht.core.model.request.JoinRequestOuterClass.JoinRequest
import io.github.vinicreis.dht.core.model.request.LeaveRequestOuterClass.LeaveRequest
import io.github.vinicreis.dht.core.model.request.NewNodeRequestOuterClass.NewNodeRequest
import io.github.vinicreis.dht.core.model.request.NodeGoneRequestOuterClass.NodeGoneRequest
import io.github.vinicreis.dht.core.model.request.SetRequestOuterClass.SetRequest
import io.github.vinicreis.dht.core.model.request.TransferRequestOuterClass.TransferRequest
import io.github.vinicreis.dht.core.model.response.GetResponseOuterClass.GetResponse
import io.github.vinicreis.dht.core.model.response.JoinOkResponseOuterClass.JoinOkResponse
import io.github.vinicreis.dht.core.model.response.JoinResponseOuterClass.JoinResponse
import io.github.vinicreis.dht.core.model.response.LeaveResponseOuterClass.LeaveResponse
import io.github.vinicreis.dht.core.model.response.NewNodeResponseOuterClass.NewNodeResponse
import io.github.vinicreis.dht.core.model.response.NodeGoneResponseOuterClass.NodeGoneResponse
import io.github.vinicreis.dht.core.model.response.SetResponseOuterClass.SetResponse
import io.github.vinicreis.dht.core.model.response.TransferResponseOuterClass.TransferResponse
import io.github.vinicreis.dht.core.service.domain.DHTService
import io.github.vinicreis.dht.model.service.Address
import io.github.vinicreis.dht.model.service.Node
import io.github.vinicreis.dht.model.service.Port
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

fun DHTServiceGrpc(
    info: Node,
    hashStrategy: HashStrategy,
    coroutineContext: CoroutineContext,
    knownNodes: List<Node>,
    logger: Logger,
): DHTService = DHTServiceGrpcServerImpl(
    info = info,
    knownNodes = knownNodes,
    coroutineContext = coroutineContext,
    hashStrategy = hashStrategy,
    nodeStubStrategy = NodeStubStrategyGrpc(),
    logger = logger,
)

interface DHTServiceGrpc {
    suspend fun join(request: JoinRequest): JoinResponse
    suspend fun joinOk(request: JoinOkRequest): JoinOkResponse
    suspend fun newNode(request: NewNodeRequest): NewNodeResponse
    suspend fun nodeGone(request: NodeGoneRequest): NodeGoneResponse
    suspend fun leave(request: LeaveRequest): LeaveResponse
    suspend fun get(request: GetRequest): GetResponse
    suspend fun set(request: SetRequest): SetResponse
    suspend fun transfer(requests: Flow<TransferRequest>): TransferResponse
}
