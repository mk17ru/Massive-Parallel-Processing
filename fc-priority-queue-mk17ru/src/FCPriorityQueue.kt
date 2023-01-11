import java.util.*
import kotlinx.atomicfu.atomicArrayOfNulls
import kotlin.collections.ArrayList
import kotlinx.atomicfu.atomic

class FCPriorityQueue<E : Comparable<E>> {

    private val fc_lock = atomic(false)

    private val ARRAY_LENGTH = 4

    private val array = atomicArrayOfNulls<Node<E>>(ARRAY_LENGTH)

    private val q = PriorityQueue<E>()

    private val results = atomicArrayOfNulls<E?>(ARRAY_LENGTH)

    class Node<E>(var busy : Boolean, var argument : E?, val operation : OPERATION<E>)

    private fun inc(i: Int): Int {
        return (i + 1) % ARRAY_LENGTH
    }

    fun fillOperation(element : E?, operation: OPERATION<E>) :E? {
        var i = 0
        val newNode = Node<E>(busy = true, operation = operation, argument = element)
        while(true) {
            if (array[i].compareAndSet(null, newNode)) {
                break;
            }
            i = inc(i)
        }
        while(array[i].value!!.busy == true) {
            if (fc_lock.compareAndSet(false, true)) {
                evalOther()
                fc_lock.compareAndSet(true, false)
                break
            }
        }
        val answer = results[i].value
        results[i].getAndSet(null)
        array[i].compareAndSet(array[i].value, null)
        return answer
    }

    private fun evalOther() {
        for (i in 0 until ARRAY_LENGTH){
            val current = array[i].value
            if (current != null) {
                if (current.busy) {
                    results[i].getAndSet(current.operation.doIt(current.argument))
                    current.busy = false
                }
            }
        }
    }


    interface OPERATION<E> {
        fun doIt(element : E?) : E?
    }

    inner class ADD : OPERATION<E> {
        override fun doIt(element: E?): E? {
            q.add(element);
            return null
        }
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        fillOperation(element, ADD())
    }

    inner class PEEK : OPERATION<E> {
        override fun doIt(element: E?): E? {
            return q.peek()
        }
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        return fillOperation(null, PEEK())
    }

    inner class POLL : OPERATION<E> {
        override fun doIt(element: E?): E? {
            return q.poll()
        }
    }

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        return fillOperation(null, POLL())
    }


}