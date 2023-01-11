//import kotlinx.coroutines.runBlocking
import kotlin.coroutines.*
import java.util.concurrent.atomic.AtomicReference


/**
 * See the paragraph The Synchronous Dual Queueenqueue method from this document:
 * https://www.cs.rochester.edu/u/scott/papers/2009_Scherer_CACM_SSQ.pdf
 */
class SynchronousQueueMS<E>() : SynchronousQueue<E> {

    var tail: AtomicReference<Node>
    var head: AtomicReference<Node>

    init {
        val fakeVertex = Node(AtomicReference<E?>(null), WORK.SENDER)
        head = AtomicReference(fakeVertex)
        tail = AtomicReference(fakeVertex)
    }

    private suspend fun commonWorker(element: E?, worker: WORK, makeOffer: (Node, Continuation<Unit>) -> Unit,
                                                                successFunc : (Node, Node) -> Pair<Boolean, E?>,
                                                                ) : E? {
        val offer = Node(AtomicReference<E?>(element), worker)
        while(true) {
            val t = tail.get()
            var h = head.get()
            if (h == t || t.work == worker) {
                val cur_next = t.next.get()
                if (t == tail.get()) {
                    if (cur_next !== null) {
                        tail.compareAndSet(t, cur_next)
                    } else {
                        if (suspendCoroutine<Any?> {
                                    continuation ->
                                makeOffer(offer, continuation)
                                if (t.next.compareAndSet(null, offer)) {
                                    tail.compareAndSet(t, offer)
                                } else {
                                    continuation.resume(RETRY)
                                }
                            } == RETRY) {
                            continue
                        }
                        h = head.get()
                        if (offer == h.next.get()) {
                            head.compareAndSet(h, offer)
                        }
                        return offer.data.get()
                    }
                }
            } else {
                val cur_n = h.next.get()
                if (h != head.get() || cur_n === null || t != tail.get()) {
                    continue
                }
                val (isEnd, res) = successFunc(cur_n, h)
                if (isEnd) {
                    return res
                } else {
                    continue
                }
            }
        }
    }


    fun successSend(n : Node, h : Node, element: E) : Pair<Boolean, E?> {
        val success = n.data.compareAndSet(null, element)
        head.compareAndSet(h, n)
        if (success) {
            n.receiverCont?.resume(Unit)
            return Pair(true, null)
        }
        return Pair(false, null)
    }

    fun successReceive(n : Node, h : Node) : Pair<Boolean, E?> {
        val (s, element) = n.sendCont ?: throw IllegalArgumentException("null received value")
        val success = n.data.compareAndSet(element, null)
        head.compareAndSet(h, n)
        if (success) {
            s.resume(Unit)
            return Pair(true, element)
        }
        return Pair(false, null)
    }

    override suspend fun send(element: E) {
        val ignored = commonWorker(element, WORK.SENDER,
        { of: Node, continuation: Continuation<Unit> -> of.sendCont = Pair(continuation, element)
        }, {n : Node, h : Node -> successSend(n, h, element)})
    }

    override suspend fun receive(): E {
        return commonWorker(null, WORK.RECEIVER,
            { of: Node, continuation: Continuation<Unit> -> of.receiverCont = continuation
            }, {n : Node, h : Node -> successReceive(n, h)})!!
    }

    class Retry

    val RETRY = Retry()

    enum class WORK {
        SENDER,
        RECEIVER
    }


    inner class Node(val data : AtomicReference<E?>, var work : WORK) {
        var receiverCont : Continuation<Unit>? = null
        var next : AtomicReference<Node?> = AtomicReference<Node?>(null)
        var sendCont : Pair<Continuation<Unit>, E>? = null
    }
}

//suspend fun main() {
//    val q = SynchronousQueueMS<Int>()
//    GlobalScope.launch {
//        print(1)
//        println(q.receive())
//        print(1)
//        q.send(1)
//        println(q.receive())
//        println(q.receive())
//        println(q.receive())
//        println(q.receive())
//        println(q.receive())
//    }.join()
//
//}