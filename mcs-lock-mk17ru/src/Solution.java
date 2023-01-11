import java.util.concurrent.atomic.AtomicReference;

public class Solution implements Lock<Solution.Node> {
    
    final AtomicReference<Node> tail = new AtomicReference<Node>();
    
    private final Environment env;

    public Solution(Environment env) {
        this.env = env;
    }

    public Node lock() {
        Node my = new Node();
        Node pred = this.tail.getAndSet(my);
        if (pred != null) {
            pred.next.getAndSet(my);
            while (my.locked.get()) {
                this.env.park();
            }
        }
        return my;
    }

    public void unlock(Node my) {
        if (my.next.get() == null) {
            if (this.tail.compareAndSet(my, null)) {
                return;
            }
            while (my.next.get() == null) {}
        }

        Node next = my.next.get();
        next.locked.getAndSet(false);
        this.env.unpark(next.thread);
    }

    static class Node {
        final AtomicReference<Boolean> locked = new AtomicReference<Boolean>(true);
        final Thread thread = Thread.currentThread();
        final AtomicReference<Node> next = new AtomicReference<Node>(null);
    }
}
