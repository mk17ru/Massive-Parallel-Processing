import kotlinx.atomicfu.*

class AtomicArray<E>(size: Int, initialValue: E) {
    val a = atomicArrayOfNulls<Any>(size)

    enum class OUTCOME {
        SUCCESS, FAIL, UNDEFINED
    }

    init {
        for (i in 0 until size) {
            a[i].value = initialValue
        }
    }

    fun get(index: Int) : Any? = getImpl(index, null)

    private fun getImpl(index : Int, notSkippedDesc : Descriptor?) : Any? {
        do {
            val v = a[index].value
            if (v is Descriptor) {
                if (v !== notSkippedDesc) {
                    v.complete()
                } else {
                    return v
                }
            } else {
                return v
            }
        } while(true)
    }

    fun set(index: Int, newVal: E) : Unit {
        do {
            val v = a[index].value
            if (v is Descriptor) {
                v.complete()
            } else {
                if (a[index].compareAndSet(v, newVal)) {
                    return
                }
            }
        } while(true)
    }


    interface Descriptor{
        fun complete() : Boolean;
    }

    inner class CommonDescriptor(
        val indexA: Int, val expectA: E,
        val indexB: Int, val expectB: E, val updateA: E, val updateB: E, val outcome: AtomicRef<OUTCOME> = atomic(OUTCOME.UNDEFINED)
    ): Descriptor {
        override fun complete() : Boolean {
            if (this.outcome.value == OUTCOME.UNDEFINED) {
                val descDCSS = RDCSSDescriptor(indexA = indexB, expectA = expectB, expectB = OUTCOME.UNDEFINED, updateA = this)
                var isChanged = false
                do {
                    val current = getImpl(indexB, this@CommonDescriptor)
                    when {
                        current === this@CommonDescriptor -> {
                            isChanged = true; break;
                        }
                        current != expectB -> {
                            isChanged = false; break;
                        }
                        a[indexB].compareAndSet(expectB, descDCSS) -> {
                            isChanged = descDCSS.complete(); break;
                        }
                        else -> continue
                    }
                } while (true)
                if (isChanged) {
                    this.outcome.compareAndSet(OUTCOME.UNDEFINED, OUTCOME.SUCCESS)
                } else {
                    this.outcome.compareAndSet(OUTCOME.UNDEFINED, OUTCOME.FAIL)
                }
            }
            return if (this.outcome.value == OUTCOME.SUCCESS) {
                consensusSwap(this@CommonDescriptor, indexA, indexB, updateA, updateB)
                true
            } else {
                consensusSwap(this@CommonDescriptor, indexA, indexB, expectA, expectB)
                false
            }
        }
    }

    private fun consensusSwap(desc: Descriptor, indexA : Int, indexB: Int, aValue : E, bValue : E) {
        a[indexA].compareAndSet(desc, aValue)
        a[indexB].compareAndSet(desc, bValue)
    }


    inner class RDCSSDescriptor(
        val indexA : Int, val expectA : E,
        val updateA : CommonDescriptor, val expectB : OUTCOME
    ) : Descriptor {
        override fun complete() : Boolean {
            return when {
                updateA.outcome.value == expectB -> {
                    a[indexA].compareAndSet(this, updateA)
                    true
                }
                else -> {
                    a[indexA].compareAndSet(this, expectA)
                    false
                }
            }
        }
    }

    fun casImpl(index: Int, expected: Any, update: Any) : Boolean {
        while(true) {
            if (expected == getImpl(index, null)) {
                if (a[index].compareAndSet(expected, update)) {
                    return@casImpl true
                }
            } else {
                break;
            }
        }
        return false
    }

    fun cas(index: Int, expected: E, update: E) : Boolean {
       if (casImpl(index, expected as Any, update as Any)) {
           return true;
       } else {
           return false;
       }
    }

    fun cas2(index1: Int, expected1: E, update1: E,
             index2: Int, expected2: E, update2: E): Boolean {
        if (index1 == index2) {
            return equalsIndexes(index1, expected1, update1, expected2, update2)
        }

        return when {
            index1 > index2 -> {
                val desc = CommonDescriptor(indexA = index2, expectA = expected2, indexB = index1, expectB = expected1, updateA = update2, updateB = update1)
                casFunction(desc, index2, expected2)
            }
            else -> {
                val desc = CommonDescriptor(indexA = index1, expectA = expected1, indexB = index2, expectB = expected2, updateA = update1, updateB = update2)
                casFunction(desc, index1, expected1)
            }
        }

    }

    private fun casFunction(desc : CommonDescriptor, index1: Int, expected1: E) : Boolean {
        if (!casImpl(index1, expected1 as Any, desc)) {
            return false;
        }
        desc.complete()
        if (desc.outcome.value == OUTCOME.SUCCESS) {
            return true
        } else {
            return false
        }
    }

    private fun equalsIndexes(index1: Int, expected1: E, update1: E, expected2: E, update2: E) : Boolean {
        if (expected1 != expected2) {
            return false;
        }
        return casImpl(index1, expected2 as Any, update2 as Any)
    }
}