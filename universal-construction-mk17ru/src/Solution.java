/**
 * @author Frolov Mikhail
 */
public class Solution implements AtomicCounter {

    private final Node rootNode;
    private final ThreadLocal<Node> last;

    public Solution() {
        this.rootNode = new Node(new Consensus<Node>(), 0);
        this.last = ThreadLocal.withInitial(() -> this.rootNode);
    }

    public int updateValue(int value, int change) {
        return value + change;
    }

    public int getAndAdd(int x) {
        Node node;
        int resultValue;
        do {
            resultValue = last.get().getValue();
            final int result = updateValue(resultValue, x);
            node = new Node(new Consensus<Node>(), result);
            last.set(last.get().getConsenus().decide(node));
        } while(last.get() != node);
        return resultValue;
    }

    private static class Node {
        private final Consensus<Node> consenus;
        private final int value;

        private Node(Consensus<Node> consenus, int value) {
            this.consenus = consenus;
            this.value = value;
        }

        public Consensus<Node> getConsenus() {
            return consenus;
        }

        public int getValue() {
            return value;
        }
    }
}
