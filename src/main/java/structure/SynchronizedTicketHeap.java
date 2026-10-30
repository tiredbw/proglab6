package structure;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

// Защищённая версия кучи: каждая операция идёт под монитором объекта, поэтому потоки выполняют их строго по очереди.
// Составное действие вроде find + delete не атомарно: между вызовами другой поток может изменить кучу.
public class SynchronizedTicketHeap<T> extends TicketHeap<T> {

    public SynchronizedTicketHeap(Comparator<? super T> comparator) {
        super(comparator);
    }

    @Override
    public synchronized void insert(T item) {
        super.insert(item);
    }

    @Override
    public synchronized T peek() {
        return super.peek();
    }

    @Override
    public synchronized T extractMin() {
        return super.extractMin();
    }

    @Override
    public synchronized int find(Predicate<? super T> condition) {
        return super.find(condition);
    }

    @Override
    public synchronized T delete(int index) {
        return super.delete(index);
    }

    @Override
    public synchronized List<T> topK(int k) {
        return super.topK(k);
    }

    @Override
    public synchronized void changePriority(int index, Consumer<? super T> change) {
        super.changePriority(index, change);
    }

    @Override
    public synchronized void merge(TicketHeap<T> other) {
        super.merge(other);
    }

    @Override
    public synchronized void addAll(List<? extends T> newItems) {
        super.addAll(newItems);
    }

    @Override
    public synchronized void clear() {
        super.clear();
    }

    @Override
    public synchronized void replaceWith(TicketHeap<T> other) {
        super.replaceWith(other);
    }

    @Override
    public synchronized int size() {
        return super.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public synchronized int capacity() {
        return super.capacity();
    }

    @Override
    public synchronized T get(int index) {
        return super.get(index);
    }

    @Override
    public synchronized List<T> toList() {
        return super.toList();
    }

    @Override
    public synchronized int[] lastVisited() {
        return super.lastVisited();
    }

    @Override
    public synchronized boolean isValidHeap() {
        return super.isValidHeap();
    }
}
