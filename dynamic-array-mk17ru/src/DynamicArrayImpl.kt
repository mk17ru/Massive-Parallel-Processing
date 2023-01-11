import kotlinx.atomicfu.*

interface ChangeNode<T> {
    val value: T
}

class Normal<T>(override val value: T) : ChangeNode<T> {}

class Swapping<T>(override val value: T) : ChangeNode<T> {}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME

class DynamicArrayImpl<E> : DynamicArray<E> {

    private val core = atomic(Core<E>(INITIAL_CAPACITY))

    private val siz = atomic(0)

    override fun get(index: Int): E {
        if (!checkIndex(index)) {
            throw IllegalArgumentException();
        }
        do {
            val currentCore = core.value
            if (currentCore.getCapacity() <= index) {
                continue
            }
            val element = currentCore.get(index)
            if (element != null) {
                return element.value
            } else {
                throw IllegalArgumentException();
            }
        } while (true)
    }

    private fun upgradeSize(currentCore: Core<E>) {
        if (currentCore.isStartedUpdate.compareAndSet(false, true)) {
            val newCore = Core<E>(siz.value * 2);
            for (i in 0..(currentCore.getCapacity() - 1)) {
                do {
                    val element = currentCore.get(i)
                    if (element == null) {
                        continue;
                    }
                    if (element is Swapping<*>) {
                        break;
                    }
                    val copy = Swapping(element.value)
                    if (currentCore.compareAndSetVal(element, copy, i)) {
                        newCore.compareAndSetVal(null, element, i);
                        break;
                    }
                } while (true)
            }
            this.core.compareAndSet(currentCore, newCore);
        }
    }

    override fun pushBack(element: E) {
        val insertedNode = Normal(element);
        do {
            val currentCore = core.value
            val curSize = siz.value;
            if (siz.value >= currentCore.getCapacity()) {
                upgradeSize(currentCore);
                continue;
            }
            if (currentCore.compareAndSetVal(null, insertedNode, curSize)) {
                siz.incrementAndGet();
                return;
            }
        } while (true);
    }


    override fun put(index: Int, element: E) {
        if (!checkIndex(index)) {
            throw IllegalArgumentException("Illegal index: $index");
        }
        do {
            val currentCore = core.value
            val value = currentCore.get(index);
            if (value != null) {
                if (!(value is Swapping<*>) && currentCore.compareAndSetVal(value, Normal(element), index)) {
                    return
                }
            } else {
                throw IllegalArgumentException();
            }
        } while (true)
    }


    override val size: Int
        get() = siz.value

    fun checkIndex(index: Int): Boolean {
        return index < siz.value //|| index < capacity
    }
}

private class Core<E>(
    capacity: Int,
) {
    private val array = atomicArrayOfNulls<ChangeNode<E>>(capacity)
    val isStartedUpdate = atomic(false);

    fun getCapacity(): Int {
        return array.size
    }

    operator fun get(index: Int): ChangeNode<E>? {
        return array[index].value;
    }

    fun compareAndSetVal(current: ChangeNode<E>?, next: ChangeNode<E>?, index: Int): Boolean {
        return array[index].compareAndSet(current, next)
    }
}

