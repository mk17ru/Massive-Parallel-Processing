import java.lang.Thread.sleep
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/**
 * Bank implementation.
 *
 * :TODO: This implementation has to be made thread-safe.
 *
 * @author : Frolov Mikhail
 */
class BankImpl(n: Int) : Bank {
    private val accounts: Array<Account> = Array(n) { Account() }

    override val numberOfAccounts: Int
        get() = accounts.size

    /**
     * Complete
     */
    override fun getAmount(index: Int): Long {
        accounts[index].lock.lock()
        val amount = accounts[index].amount
        accounts[index].lock.unlock()
        return amount
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override val totalAmount: Long
        get() {
            for (acc in accounts) {
                acc.lock.lock()
            }
//            accounts.forEach { it.lock.lock() }
            val resultSum = accounts.sumOf { account ->
                account.amount
            }
            for (acc in accounts) {
                acc.lock.unlock()
            }
            return resultSum
        }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun deposit(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.withLock {
            check(!(amount > Bank.MAX_AMOUNT || account.amount + amount > Bank.MAX_AMOUNT)) {
                "Overflow"
            }
            account.amount += amount
            val result = account.amount
            return result
        }
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun withdraw(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.withLock {
            check(account.amount - amount >= 0) {
                "Underflow"
            }
            account.amount -= amount
            val result = account.amount
            return result
        }
    }

    /**
     * :TODO: This method has to be made thread-safe.
     */
    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }
        val from = accounts[fromIndex]
        val to = accounts[toIndex]
        if (fromIndex > toIndex) {
            to.lock.lock()
            from.lock.lock()
        } else {
            from.lock.lock()
            to.lock.lock()
        }
        check(amount <= from.amount) {
            to.lock.unlock()
            from.lock.unlock()
            "Underflow"
        }
        check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) {
            to.lock.unlock()
            from.lock.unlock()
            "Overflow"
        }
        from.amount -= amount
        from.lock.unlock()
        to.amount += amount
        to.lock.unlock()
    }

    /**
     * Private account data structure.
     */
    class Account {
        var lock = ReentrantLock()

        var amount: Long = 0
    }
}