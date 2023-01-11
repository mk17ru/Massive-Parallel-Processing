import kotlinx.atomicfu.*

class FAAQueue<T> {


    private val head: AtomicRef<Segment> // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private val tail: AtomicRef<Segment> // Tail pointer, similarly to the Michael-Scott queue



    init {
        val firstNode = Segment()
        head = atomic(firstNode)
        tail = atomic(firstNode)
    }

    /**
     * Adds the specified element [x] to the queue.
     */
    fun enqueue(x: T) {
        while(true) {
            var currentTail = tail.value
            val enqIdx = currentTail.enqIdx.incrementAndGet()
            if (enqIdx >= SEGMENT_SIZE) {
                val newTail : Segment = Segment(x)
                if (currentTail.next.compareAndSet(null, newTail)) {
                    tail.compareAndSet(currentTail, newTail)
                    break
                } else {
                    val notIsEnd = updateHeadOrTail(currentTail, isHead = false)
                    if (notIsEnd) {
                        continue
                    }
                }
            } else {
                if (currentTail.elements[enqIdx].compareAndSet(null, x)) {
                    break;
                } else {
                    continue;
                }
            }
        }
    }

    /**
     * Retrieves the first element from the queue
     * and returns it; returns `null` if the queue
     * is empty.
     */
    fun dequeue(): T? {
        while (true) {
            val currentHead = head.value
            val deqIdx = currentHead.deqIdx.getAndIncrement()
            if (SEGMENT_SIZE <= deqIdx) {
                val isEmpt = updateHeadOrTail(currentHead, isHead = true)
                if (isEmpt) {
                    return null;
                }
                continue
            } else {
                val answer = currentHead.elements[deqIdx].getAndSet(DONE)
                if (answer == null) {
                    continue
                }
                return answer as T?
            }
        }
    }


    private fun updateHeadOrTail(current: Segment, isHead : Boolean) : Boolean {
        val nextEl = current.next.value
        if (nextEl == null) {
            return true;
        }
        if (isHead) {
            head.compareAndSet(current, nextEl)
        } else {
            tail.compareAndSet(current, nextEl)
        }
        return false;
    }

    /**
     * Returns `true` if this queue is empty;
     * `false` otherwise.
     */
    val isEmpty: Boolean get() {
        while (true) {
            val currentHead = head.value
            if (currentHead.isEmpty) {
                if (updateHeadOrTail(currentHead, isHead = true)) {
                    return true;
                }
                continue
            } else {
                return false
            }
        }
    }
}

private class Segment {

    val enqIdx = atomic(0) // index for the next enqueue operation
    val deqIdx = atomic(0) // index for the next dequeue operation

    val next: AtomicRef<Segment?> = atomic(null)
    val elements = atomicArrayOfNulls<Any>(SEGMENT_SIZE)

    constructor() // for the first segment creation

    constructor(x: Any?) { // each next new segment should be constructed with an element
        enqIdx.getAndSet(1)
        elements[0].getAndSet(x)
    }

    val isEmpty: Boolean get() = deqIdx.value >= enqIdx.value || deqIdx.value >= SEGMENT_SIZE

}

private val DONE = Any() // Marker for the "DONE" slot state; to avoid memory leaks
const val SEGMENT_SIZE = 2 // DO NOT CHANGE, IMPORTANT FOR TESTS

