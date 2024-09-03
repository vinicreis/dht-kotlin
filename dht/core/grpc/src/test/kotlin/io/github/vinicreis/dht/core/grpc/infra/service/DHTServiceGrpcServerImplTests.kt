package io.github.vinicreis.dht.core.grpc.infra.service

import io.github.vinicreis.dht.core.grpc.domain.strategy.HashStrategy
import io.github.vinicreis.dht.core.grpc.infra.fixture.NodeStrategyStubManyMock
import io.github.vinicreis.dht.core.grpc.infra.fixture.NodeStrategyStubSingleMock
import io.github.vinicreis.dht.core.grpc.infra.mapper.asGrpc
import io.github.vinicreis.dht.core.grpc.infra.strategy.HashStrategyMD5
import io.github.vinicreis.dht.core.model.ResultOuterClass
import io.github.vinicreis.dht.core.model.request.JoinRequestOuterClass
import io.github.vinicreis.dht.core.model.request.SetRequestOuterClass.SetRequest
import io.github.vinicreis.dht.core.model.request.joinOkRequest
import io.github.vinicreis.dht.core.model.request.newNodeRequest
import io.github.vinicreis.dht.core.model.response.joinOkResponse
import io.github.vinicreis.dht.core.model.response.joinResponse
import io.github.vinicreis.dht.core.model.response.newNodeResponse
import io.github.vinicreis.dht.core.model.response.setResponse
import io.github.vinicreis.dht.core.model.response.transferResponse
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub
import io.github.vinicreis.dht.core.service.domain.DHTService
import io.github.vinicreis.dht.core.service.domain.model.Address
import io.github.vinicreis.dht.core.service.domain.model.Node
import io.github.vinicreis.dht.core.service.domain.model.Port
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.logging.Logger

@OptIn(ExperimentalCoroutinesApi::class)
class DHTServiceGrpcServerImplTests {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, t ->
        fail("Caught $t in $coroutineContext", t)
    }
    private val testDispatcher = StandardTestDispatcher()
    private val hashStrategy = HashStrategyMD5(4L)
    private lateinit var sut: DHTServiceGrpcServerImpl

    @AfterEach
    fun tearDown() {
        sut.shutdown()
    }

    @Nested
    inner class StartTests {
        @Test
        fun `Should send join requests to all known hosts on start until one succeeds`() = runTest(testDispatcher) {
            val events = mutableListOf<DHTService.Event>()
            val stub1: DHTServiceCoroutineStub = mockk {
                coEvery { join(any(), any()) } throws StatusException(Status.CANCELLED)
            }
            val stub2: DHTServiceCoroutineStub = mockk {
                coEvery { join(any(), any()) } returns joinResponse { result = ResultOuterClass.Result.SUCCESS }
            }
            val stub3: DHTServiceCoroutineStub = mockk {
                coEvery { join(any(), any()) } answers { fail("Should not be called!") }
            }

            backgroundScope.launch { sut.events.toList(events) }

            sut = DHTServiceGrpcServerImpl(
                info = Node(
                    id = 0,
                    address = Address("localhost"),
                    port = Port(10090),
                ),
                knownNodes = listOf(
                    Node(0, Address("localhost"), Port(10090)),
                    Node(1, Address("localhost"), Port(10091)),
                    Node(2, Address("localhost"), Port(10092)),
                    Node(3, Address("localhost"), Port(10093)),
                ),
                nodeStubStrategy = NodeStrategyStubManyMock(stub1, stub2, stub3),
                logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.java.simpleName),
                hashStrategy = hashStrategy,
                coroutineContext = testDispatcher,
            )

            sut.start()

            runCurrent()

            coVerify(exactly = 1) { stub1.join(any(), any()) }
            coVerify(exactly = 1) { stub2.join(any(), any()) }
            coVerify(exactly = 0) { stub3.join(any(), any()) }
            assertEquals(listOf(DHTService.Event.JoinStarted, DHTService.Event.Joined), events)
        }

        @Test
        fun `Should consider if this is the first node on network`() = runTest(testDispatcher) {
            val events = mutableListOf<DHTService.Event>()
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { join(any(), any()) } throws StatusException(Status.CANCELLED)
            }

            backgroundScope.launch { sut.events.toList(events) }

            sut = DHTServiceGrpcServerImpl(
                info = Node(
                    id = 0,
                    address = Address("localhost"),
                    port = Port(10090),
                ),
                knownNodes = listOf(
                    Node(0, Address("localhost"), Port(10090)),
                    Node(1, Address("localhost"), Port(10091)),
                    Node(2, Address("localhost"), Port(10092)),
                    Node(3, Address("localhost"), Port(10093)),
                ),
                nodeStubStrategy = NodeStrategyStubSingleMock(stub),
                logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.java.simpleName),
                hashStrategy = hashStrategy,
                coroutineContext = testDispatcher,
            )

            sut.start()

            runCurrent()

            coVerify(exactly = 3) { stub.join(any(), any()) }
            assertEquals(listOf(DHTService.Event.JoinStarted, DHTService.Event.Joined), events)
        }
    }

    @Nested
    inner class JoinTests {
        @Test
        fun `Should send join ok and transfer data to the new node`() = runTest(testDispatcher) {
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { joinOk(any(), any()) } returns joinOkResponse { result = ResultOuterClass.Result.SUCCESS }
                coEvery { transfer(any(), any()) } returns transferResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceGrpcServerImpl(
                info = Node(
                    id = 0,
                    address = Address("localhost"),
                    port = Port(10090),
                ),
                knownNodes = listOf(
                    Node(0, Address("localhost"), Port(10090)),
                    Node(1, Address("localhost"), Port(10091)),
                    Node(2, Address("localhost"), Port(10092)),
                    Node(3, Address("localhost"), Port(10093)),
                ),
                nodeStubStrategy = NodeStrategyStubSingleMock(stub),
                logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.java.simpleName),
                hashStrategy = hashStrategy,
                coroutineContext = testDispatcher,
            )

            sut.join(JoinRequestOuterClass.JoinRequest.getDefaultInstance())

            runCurrent()

            coVerify(exactly = 1) { stub.joinOk(any(), any()) }
            coVerify(exactly = 1) { stub.transfer(any(), any()) }
        }

        @Test
        fun `Should send a new node request to the previous node on join ok`() = runTest(testDispatcher) {
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { newNode(any(), any()) } returns newNodeResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceGrpcServerImpl(
                info = Node(
                    id = 0,
                    address = Address("localhost"),
                    port = Port(10090),
                ),
                knownNodes = listOf(
                    Node(0, Address("localhost"), Port(10090)),
                    Node(1, Address("localhost"), Port(10091)),
                    Node(2, Address("localhost"), Port(10092)),
                    Node(3, Address("localhost"), Port(10093)),
                ),
                nodeStubStrategy = NodeStrategyStubSingleMock(stub),
                logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.java.simpleName),
                hashStrategy = hashStrategy,
                coroutineContext = testDispatcher,
            )

            sut.joinOk(
                joinOkRequest {
                    next = Node(2, Address("localhost"), Port(10092)).asGrpc
                    previous = Node(1, Address("localhost"), Port(10091)).asGrpc
                }
            )

            runCurrent()

            coVerify(exactly = 1) { stub.newNode(any(), any()) }
        }

        @Test
        fun `Should set next node when receiving a new node request`() = runTest(testDispatcher) {

        }
    }

    @Nested
    inner class LeaveTests {
        @Test
        fun `Should set next node when receiving a node gone request`() = runTest(testDispatcher) {

        }

        @Test
        fun `Should set previous node when receiving a leave request`() = runTest(testDispatcher) {

        }
    }

    @Nested
    inner class SetTests {
        @Test
        fun `Should fail in case no next node exists and node is not responsible for key set`() = runTest(
            testDispatcher
        ) {
            var exceptionCaught: Throwable? = null
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { set(any(), any()) } returns setResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceGrpcServerImpl(
                info = Node(
                    id = 0,
                    address = Address("localhost"),
                    port = Port(10090),
                ),
                knownNodes = listOf(
                    Node(0, Address("localhost"), Port(10090)),
                    Node(1, Address("localhost"), Port(10091)),
                    Node(2, Address("localhost"), Port(10092)),
                    Node(3, Address("localhost"), Port(10093)),
                ),
                nodeStubStrategy = NodeStrategyStubSingleMock(stub),
                logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.java.simpleName),
                hashStrategy = object : HashStrategy {
                    override fun invoke(key: String): Long = 4L
                },
                coroutineContext = testDispatcher + CoroutineExceptionHandler { _, throwable ->
                    exceptionCaught = throwable
                },
            )

            sut.set(SetRequest.getDefaultInstance())

            runCurrent()

            coVerify(exactly = 0) { stub.set(any(), any()) }
            assertNotNull(exceptionCaught)
            assertInstanceOf(IllegalStateException::class.java, exceptionCaught)
        }

        @Test
        fun `Should redirect call to next node when a set request for key not responsible for`() = runTest(
            testDispatcher
        ) {
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { set(any(), any()) } returns setResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceGrpcServerImpl(
                info = Node(
                    id = 0,
                    address = Address("localhost"),
                    port = Port(10090),
                ),
                knownNodes = listOf(
                    Node(0, Address("localhost"), Port(10090)),
                    Node(1, Address("localhost"), Port(10091)),
                    Node(2, Address("localhost"), Port(10092)),
                    Node(3, Address("localhost"), Port(10093)),
                ),
                nodeStubStrategy = NodeStrategyStubSingleMock(stub),
                logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.java.simpleName),
                hashStrategy = object : HashStrategy {
                    override fun invoke(key: String): Long = 4L
                },
                coroutineContext = testDispatcher + coroutineExceptionHandler,
            )

            sut.newNode(newNodeRequest { node = Node(1, Address("localhost"), Port(10091)).asGrpc })
            sut.set(SetRequest.getDefaultInstance())

            runCurrent()

            coVerify(exactly = 1) { stub.set(any(), any()) }
        }

        @Test
        fun `Should save data queue node is responsible for`() = runTest(testDispatcher) {
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { set(any(), any()) } returns setResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceGrpcServerImpl(
                info = Node(
                    id = 0,
                    address = Address("localhost"),
                    port = Port(10090),
                ),
                knownNodes = listOf(
                    Node(0, Address("localhost"), Port(10090)),
                    Node(1, Address("localhost"), Port(10091)),
                    Node(2, Address("localhost"), Port(10092)),
                    Node(3, Address("localhost"), Port(10093)),
                ),
                nodeStubStrategy = NodeStrategyStubSingleMock(stub),
                logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.java.simpleName),
                hashStrategy = object : HashStrategy { override fun invoke(key: String): Long = 0L },
                coroutineContext = testDispatcher + coroutineExceptionHandler,
            )

            sut.newNode(newNodeRequest { node = Node(1, Address("localhost"), Port(10091)).asGrpc })
            sut.set(SetRequest.getDefaultInstance())

            runCurrent()

            coVerify(exactly = 0) { stub.set(any(), any()) }
            // TODO: Validate that data is on memory
        }
    }

    @Nested
    inner class GetTests {
        @Test
        fun `Should fail in case no next node exists and node is not responsible for key set`() = runTest(
            testDispatcher
        ) {
            var exceptionCaught: Throwable? = null
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { set(any(), any()) } returns setResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceGrpcServerImpl(
                info = Node(
                    id = 0,
                    address = Address("localhost"),
                    port = Port(10090),
                ),
                knownNodes = listOf(
                    Node(0, Address("localhost"), Port(10090)),
                    Node(1, Address("localhost"), Port(10091)),
                    Node(2, Address("localhost"), Port(10092)),
                    Node(3, Address("localhost"), Port(10093)),
                ),
                nodeStubStrategy = NodeStrategyStubSingleMock(stub),
                logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.java.simpleName),
                hashStrategy = object : HashStrategy {
                    override fun invoke(key: String): Long = 4L
                },
                coroutineContext = testDispatcher + CoroutineExceptionHandler { _, throwable ->
                    exceptionCaught = throwable
                },
            )

            sut.set(SetRequest.getDefaultInstance())

            runCurrent()

            coVerify(exactly = 0) { stub.set(any(), any()) }
            assertNotNull(exceptionCaught)
            assertInstanceOf(IllegalStateException::class.java, exceptionCaught)
        }

        @Test
        fun `Should redirect call to next node when a set request for key not responsible for`() = runTest(
            testDispatcher
        ) {
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { set(any(), any()) } returns setResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceGrpcServerImpl(
                info = Node(
                    id = 0,
                    address = Address("localhost"),
                    port = Port(10090),
                ),
                knownNodes = listOf(
                    Node(0, Address("localhost"), Port(10090)),
                    Node(1, Address("localhost"), Port(10091)),
                    Node(2, Address("localhost"), Port(10092)),
                    Node(3, Address("localhost"), Port(10093)),
                ),
                nodeStubStrategy = NodeStrategyStubSingleMock(stub),
                logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.java.simpleName),
                hashStrategy = object : HashStrategy {
                    override fun invoke(key: String): Long = 4L
                },
                coroutineContext = testDispatcher + coroutineExceptionHandler,
            )

            sut.newNode(newNodeRequest { node = Node(1, Address("localhost"), Port(10091)).asGrpc })
            sut.set(SetRequest.getDefaultInstance())

            runCurrent()

            coVerify(exactly = 1) { stub.set(any(), any()) }
        }

        @Test
        fun `Should save data queue node is responsible for`() = runTest(testDispatcher) {
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { set(any(), any()) } returns setResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceGrpcServerImpl(
                info = Node(
                    id = 0,
                    address = Address("localhost"),
                    port = Port(10090),
                ),
                knownNodes = listOf(
                    Node(0, Address("localhost"), Port(10090)),
                    Node(1, Address("localhost"), Port(10091)),
                    Node(2, Address("localhost"), Port(10092)),
                    Node(3, Address("localhost"), Port(10093)),
                ),
                nodeStubStrategy = NodeStrategyStubSingleMock(stub),
                logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.java.simpleName),
                hashStrategy = object : HashStrategy { override fun invoke(key: String): Long = 0L },
                coroutineContext = testDispatcher + coroutineExceptionHandler,
            )

            sut.newNode(newNodeRequest { node = Node(1, Address("localhost"), Port(10091)).asGrpc })
            sut.set(SetRequest.getDefaultInstance())

            runCurrent()

            coVerify(exactly = 0) { stub.set(any(), any()) }
            // TODO: Validate that data is on memory
        }
    }
}
