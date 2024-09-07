package io.github.vinicreis.dht.core.grpc.infra.service

import io.github.vinicreis.dht.core.grpc.domain.strategy.HashStrategy
import io.github.vinicreis.dht.core.grpc.domain.extensions.asByteString
import io.github.vinicreis.dht.core.grpc.infra.fixture.ClientStrategyStubSingleMock
import io.github.vinicreis.dht.core.grpc.infra.fixture.ServerStrategyStubManyMock
import io.github.vinicreis.dht.core.grpc.infra.fixture.ServerStrategyStubSingleMock
import io.github.vinicreis.dht.core.grpc.infra.mapper.asGrpc
import io.github.vinicreis.dht.core.grpc.infra.strategy.HashStrategyMD5
import io.github.vinicreis.dht.core.model.ResultOuterClass
import io.github.vinicreis.dht.core.model.data
import io.github.vinicreis.dht.core.model.request.SetRequestOuterClass.SetRequest
import io.github.vinicreis.dht.core.model.request.joinOkRequest
import io.github.vinicreis.dht.core.model.request.joinRequest
import io.github.vinicreis.dht.core.model.request.leaveRequest
import io.github.vinicreis.dht.core.model.request.newNodeRequest
import io.github.vinicreis.dht.core.model.request.setRequest
import io.github.vinicreis.dht.core.model.request.transferRequest
import io.github.vinicreis.dht.core.model.response.joinOkResponse
import io.github.vinicreis.dht.core.model.response.joinResponse
import io.github.vinicreis.dht.core.model.response.leaveResponse
import io.github.vinicreis.dht.core.model.response.newNodeResponse
import io.github.vinicreis.dht.core.model.response.nodeGoneResponse
import io.github.vinicreis.dht.core.model.response.setResponse
import io.github.vinicreis.dht.core.model.response.transferResponse
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub
import io.github.vinicreis.dht.model.service.Address
import io.github.vinicreis.dht.model.service.Node
import io.github.vinicreis.dht.model.service.Port
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.logging.Logger

@OptIn(ExperimentalCoroutinesApi::class)
class DHTServiceServerGrpcImplTests {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, t ->
        fail("Caught $t in $coroutineContext", t)
    }
    private val testDispatcher = StandardTestDispatcher()
    private val hashStrategy = HashStrategyMD5(4L)
    private lateinit var sut: DHTServiceServerGrpcImpl

    @AfterEach
    fun tearDown() {
        sut.shutdown()
    }

    @Nested
    inner class StartTests {
        @Test
        fun `Should send join requests to all known hosts on start until one succeeds`() = runTest(testDispatcher) {
            val stub1: DHTServiceCoroutineStub = mockk {
                coEvery { join(any(), any()) } throws StatusException(Status.CANCELLED)
            }
            val stub2: DHTServiceCoroutineStub = mockk {
                coEvery { join(any(), any()) } returns joinResponse { result = ResultOuterClass.Result.SUCCESS }
            }
            val stub3: DHTServiceCoroutineStub = mockk {
                coEvery { join(any(), any()) } answers { fail("Should not be called!") }
            }
            val finalStub: DHTServiceCoroutineStub = mockk {
                coEvery { newNode(any(), any()) } returns newNodeResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubManyMock(stub1, stub2, then = finalStub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
                hashStrategy = hashStrategy,
                coroutineContext = testDispatcher,
            )

            sut.start()
            sut.joinOk(
                joinOkRequest {
                    next = node(id = 2, port = 10092).asGrpc
                    previous = node(id = 1, port = 10091).asGrpc
                }
            )

            runCurrent()

            coVerify(exactly = 1) { stub1.join(any(), any()) }
            coVerify(exactly = 1) { stub2.join(any(), any()) }
            coVerify(exactly = 0) { stub3.join(any(), any()) }
            assertTrue(sut.responsibleForIds.containsAll(listOf(0)))
            assertFalse(sut.responsibleForIds.contains(1))
            assertFalse(sut.responsibleForIds.contains(2))
            assertFalse(sut.responsibleForIds.contains(3))
        }

        @Test
        fun `Should consider if this is the first node on network`() = runTest(testDispatcher) {
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { join(any(), any()) } throws StatusException(Status.CANCELLED)
            }

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubSingleMock(stub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
                hashStrategy = hashStrategy,
                coroutineContext = testDispatcher,
            )

            sut.start()

            runCurrent()

            coVerify(exactly = 3) { stub.join(any(), any()) }
            assertTrue(sut.responsibleForIds.containsAll(knownNodes.map { it.id }))
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

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubSingleMock(stub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
                hashStrategy = hashStrategy,
                coroutineContext = testDispatcher,
            )

            sut.join(
                joinRequest {
                    node = node(id = 1, port = 10091).asGrpc
                }
            )

            runCurrent()

            coVerify(exactly = 1) { stub.joinOk(any(), any()) }
            coVerify(exactly = 1) { stub.transfer(any(), any()) }
            assertFalse(sut.responsibleForIds.contains(1))
        }

        @Test
        fun `Should send a new node request to the previous node on join ok`() = runTest(testDispatcher) {
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { newNode(any(), any()) } returns newNodeResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubSingleMock(stub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
                hashStrategy = hashStrategy,
                coroutineContext = testDispatcher,
            )

            sut.joinOk(
                joinOkRequest {
                    next = node(id = 2, port = 10092).asGrpc
                    previous = node(id = 1, port = 10091).asGrpc
                }
            )

            runCurrent()

            coVerify(exactly = 1) { stub.newNode(any(), any()) }
            assertTrue(sut.responsibleForIds.containsAll(listOf(0)))
            assertFalse(sut.responsibleForIds.contains(1))
            assertFalse(sut.responsibleForIds.contains(2))
            assertFalse(sut.responsibleForIds.contains(3))
        }

        @Test
        fun `Should set next node when receiving a new node request`() = runTest(testDispatcher) {
            val stub: DHTServiceCoroutineStub = mockk()

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubSingleMock(stub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
                hashStrategy = hashStrategy,
                coroutineContext = testDispatcher,
            )

            sut.newNode(
                newNodeRequest {
                    this.node = Node(1, Address("localhost"), Port(10091)).asGrpc
                }
            )

            runCurrent()

            assertNotNull(sut.next)
            assertEquals(node(1L, "localhost", 10091), sut.next)
        }
    }

    @Nested
    inner class LeaveTests {
        @Test
        fun `Should set next node when receiving a node gone request`() = runTest(testDispatcher) {
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { newNode(any(), any()) } returns newNodeResponse { result = ResultOuterClass.Result.SUCCESS }
                coEvery { leave(any(), any()) } returns leaveResponse { result = ResultOuterClass.Result.SUCCESS }
                coEvery { transfer(any(), any()) } returns transferResponse { result = ResultOuterClass.Result.SUCCESS }
                coEvery { nodeGone(any(), any()) } returns nodeGoneResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubSingleMock(stub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
                hashStrategy = object : HashStrategy {
                    override fun invoke(key: String): Long = 4L
                },
                coroutineContext = testDispatcher + coroutineExceptionHandler,
            )

            sut.joinOk(
                joinOkRequest {
                    next = node(id = 1, port = 10091).asGrpc
                    previous = node(id = 2, port = 10092).asGrpc
                }
            )
            sut.leave()

            runCurrent()

            coVerify(exactly = 1) { stub.newNode(any(), any()) }
            coVerify(exactly = 1) { stub.leave(any(), any()) }
            coVerify(exactly = 1) { stub.transfer(any(), any()) }
            coVerify(exactly = 1) { stub.nodeGone(any(), any()) }
        }

        @Test
        fun `Should set previous node when receiving a leave request`() = runTest(testDispatcher) {
            val stub: DHTServiceCoroutineStub = mockk()

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubSingleMock(stub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
                hashStrategy = object : HashStrategy {
                    override fun invoke(key: String): Long = 4L
                },
                coroutineContext = testDispatcher + coroutineExceptionHandler,
            )

            assertTrue(sut.responsibleForIds.containsAll(knownNodes.map { it.id }))

            sut.leave(
                leaveRequest {
                    node = node().asGrpc
                    previous = node(id = 2, port = 10092).asGrpc
                }
            )

            runCurrent()

            assertNotNull(sut.previous)
            assertEquals(node(id = 2, port = 10092), sut.previous)
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

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubSingleMock(stub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
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

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubSingleMock(stub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
                hashStrategy = object : HashStrategy {
                    override fun invoke(key: String): Long = 4L
                },
                coroutineContext = testDispatcher + coroutineExceptionHandler,
            )

            sut.newNode(newNodeRequest { node = node(id = 1, port =10091).asGrpc })
            sut.set(
                setRequest {
                    node = node().asGrpc
                    key = "key".asByteString
                    data = data { content = "data".asByteString }
                }
            )

            runCurrent()

            coVerify(exactly = 1) { stub.set(any(), any()) }
        }

        @Test
        fun `Should save data queue node is responsible for`() = runTest(testDispatcher) {
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { set(any(), any()) } returns setResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubSingleMock(stub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
                hashStrategy = object : HashStrategy { override fun invoke(key: String): Long = 0L },
                coroutineContext = testDispatcher + coroutineExceptionHandler,
            )

            sut.newNode(newNodeRequest { node = node(id = 1, port =10091).asGrpc })
            sut.set(
                setRequest {
                    node = node().asGrpc
                    key = "key".asByteString
                    data = data { content = "data".asByteString }
                }
            )

            runCurrent()

            coVerify(exactly = 0) { stub.set(any(), any()) }
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

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubSingleMock(stub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
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

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubSingleMock(stub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
                hashStrategy = object : HashStrategy {
                    override fun invoke(key: String): Long = 4L
                },
                coroutineContext = testDispatcher + coroutineExceptionHandler,
            )

            sut.newNode(newNodeRequest { node = node(id = 1, port =10091).asGrpc })
            sut.set(
                setRequest {
                    node = node().asGrpc
                    key = "key".asByteString
                    data = data { content = "data".asByteString }
                }
            )

            runCurrent()

            coVerify(exactly = 1) { stub.set(any(), any()) }
        }

        @Test
        fun `Should save data queue node is responsible for`() = runTest(testDispatcher) {
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { set(any(), any()) } returns setResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubSingleMock(stub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
                hashStrategy = object : HashStrategy { override fun invoke(key: String): Long = 0L },
                coroutineContext = testDispatcher + coroutineExceptionHandler,
            )

            sut.newNode(newNodeRequest { node = node(id = 1, port =10091).asGrpc })
            sut.set(SetRequest.getDefaultInstance())

            runCurrent()

            coVerify(exactly = 0) { stub.set(any(), any()) }
        }
    }

    @Nested
    inner class TransferTests {
        @Test
        fun `Should collect and be responsible for transfered data`() = runTest(testDispatcher) {
            val stub: DHTServiceCoroutineStub = mockk {
                coEvery { set(any(), any()) } returns setResponse { result = ResultOuterClass.Result.SUCCESS }
            }

            sut = DHTServiceServerGrpcImpl(
                info = node(),
                knownNodes = knownNodes,
                serverStubStrategy = ServerStrategyStubSingleMock(stub),
                clientStubStrategy = ClientStrategyStubSingleMock(mockk()),
                logger = Logger.getLogger(DHTServiceServerGrpcImpl::class.java.simpleName),
                hashStrategy = object : HashStrategy { override fun invoke(key: String): Long = 0L },
                coroutineContext = testDispatcher + coroutineExceptionHandler,
            )

            sut.transfer(
                flow {
                    emit(
                        transferRequest {
                            node = node().asGrpc
                            key = "key1".asByteString
                            data = data { content = "data1".asByteString }
                        }
                    )

                    emit(
                        transferRequest {
                            node = node().asGrpc
                            key = "key2".asByteString
                            data = data { content = "data2".asByteString }
                        }
                    )

                    emit(
                        transferRequest {
                            node = node().asGrpc
                            key = "key3".asByteString
                            data = data { content = "data3".asByteString }
                        }
                    )
                }
            )

            runCurrent()

            assertTrue(sut.data.containsKey("key1"))
            assertTrue(sut.data.containsKey("key2"))
            assertTrue(sut.data.containsKey("key3"))
        }
    }

    companion object {
        private fun node(
            id: Long = 0,
            address: String = "localhost",
            port: Int = 10090,
        ): Node = Node(
            id = id,
            address = Address(address),
            port = Port(port),
        )

        private val knownNodes = listOf(
            node(id = 0, port = 10090),
            node(id = 1, port = 10091),
            node(id = 2, port = 10092),
            node(id = 3, port = 10093),
        )
    }
}
