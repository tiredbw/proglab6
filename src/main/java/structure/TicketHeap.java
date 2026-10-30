package structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

// Двоичная min-куча на массиве. Для узла i родитель (i-1)/2, дети 2i+1 и 2i+2.
// Инвариант: приоритет родителя не больше приоритетов детей.
// Класс не потокобезопасен: для работы из нескольких потоков есть SynchronizedTicketHeap.
public class TicketHeap<T> {
    private static final int INITIAL_CAPACITY = 16;
    private static final int GROWTH_FACTOR = 2;

    private final Comparator<? super T> comparator;
    private T[] items;
    private int size;

    // Индексы узлов, посещённых последней операцией, в порядке обхода
    private int[] visited = new int[INITIAL_CAPACITY];
    private int visitedCount;

    public TicketHeap(Comparator<? super T> comparator) {
        this.comparator = comparator;
        this.items = newArray();
    }

    @SuppressWarnings("unchecked") // Массив T[] создать нельзя, но наружу он не выходит
    private T[] newArray() {
        return (T[]) new Object[INITIAL_CAPACITY];
    }

    public void insert(T item) {
        startVisit();
        ensureCapacity(size + 1);
        items[size] = item;
        size++;
        siftUp(size - 1);
    }

    public T peek() {
        requireNotEmpty();
        startVisit();
        visit(0);
        return items[0];
    }

    public T extractMin() {
        requireNotEmpty();
        return delete(0);
    }

    // Возвращает индекс первого подходящего элемента или -1. Порядка по ключу поиска в куче нет, поэтому обход линейный
    public int find(Predicate<? super T> condition) {
        startVisit();
        for (int i = 0; i < size; i++) {
            visit(i);
            if (condition.test(items[i])) {
                return i;
            }
        }
        return -1;
    }

    public T delete(int index) {
        checkIndex(index);
        startVisit();
        visit(index);
        T removed = items[index];
        int last = size - 1;
        items[index] = items[last];
        items[last] = null;
        size--;
        if (index < size) {
            restore(index);
        }
        return removed;
    }

    // K самых приоритетных элементов по порядку, сама куча не меняется.
    // Минимум всегда лежит среди кандидатов: это корень и дети уже выбранных узлов
    public List<T> topK(int k) {
        startVisit();
        List<T> result = new ArrayList<>();
        if (size == 0 || k <= 0) {
            return result;
        }

        TicketHeap<Integer> candidates = new TicketHeap<>((a, b) -> comparator.compare(items[a], items[b]));
        candidates.insert(0);
        while (result.size() < k && !candidates.isEmpty()) {
            int index = candidates.extractMin();
            visit(index);
            result.add(items[index]);
            if (left(index) < size) {
                candidates.insert(left(index));
            }
            if (right(index) < size) {
                candidates.insert(right(index));
            }
        }
        return result;
    }

    // Единственный безопасный способ изменить поля, влияющие на порядок: после change инвариант восстанавливается
    public void changePriority(int index, Consumer<? super T> change) {
        checkIndex(index);
        startVisit();
        visit(index);
        change.accept(items[index]);
        restore(index);
    }

    // Склейка массивов и heapify за O(n + m). Вторая очередь не меняется
    public void merge(TicketHeap<T> other) {
        if (other == this) {
            throw new IllegalArgumentException("Нельзя слить очередь саму с собой");
        }
        addAll(other.toList());
    }

    // Добавляет сразу много элементов за O(n + m), что быстрее m отдельных вставок
    public void addAll(List<? extends T> newItems) {
        startVisit();
        ensureCapacity(size + newItems.size());
        for (T item : newItems) {
            items[size] = item;
            size++;
        }
        heapify();
    }

    public void clear() {
        startVisit();
        Arrays.fill(items, 0, size, null);
        size = 0;
    }

    // Забирает содержимое другой кучи за O(1): собрать её можно в фоне, а здесь только подменить массив.
    // Вторая куча остаётся пустой, иначе обе делили бы один массив
    public void replaceWith(TicketHeap<T> other) {
        items = other.items;
        size = other.size;
        visited = other.visited;
        visitedCount = other.visitedCount;

        other.items = other.newArray();
        other.size = 0;
        other.visited = new int[INITIAL_CAPACITY];
        other.visitedCount = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return items.length;
    }

    public T get(int index) {
        checkIndex(index);
        return items[index];
    }

    public List<T> toList() {
        return Arrays.asList(Arrays.copyOf(items, size));
    }

    public int[] lastVisited() {
        return Arrays.copyOf(visited, visitedCount);
    }

    // Проверка рассчитана и на кучу, испорченную гонкой, поэтому null и неверный size не приводят к падению
    public boolean isValidHeap() {
        if (size > items.length) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (items[i] == null) {
                return false;
            }
            if (i > 0 && comparator.compare(items[parent(i)], items[i]) > 0) {
                return false;
            }
        }
        return true;
    }

    private void heapify() {
        for (int i = size / 2 - 1; i >= 0; i--) {
            siftDown(i);
        }
    }

    // Элемент мог стать и меньше родителя, и больше детей. Двигаем только в одну сторону
    private void restore(int index) {
        if (siftUp(index) == index) {
            siftDown(index);
        }
    }

    private int siftUp(int index) {
        visit(index);
        while (index > 0 && comparator.compare(items[index], items[parent(index)]) < 0) {
            swap(index, parent(index));
            index = parent(index);
            visit(index);
        }
        return index;
    }

    private void siftDown(int index) {
        visit(index);
        while (left(index) < size) {
            int smallest = left(index);
            if (right(index) < size && comparator.compare(items[right(index)], items[smallest]) < 0) {
                smallest = right(index);
            }
            if (comparator.compare(items[smallest], items[index]) >= 0) {
                return;
            }
            swap(index, smallest);
            index = smallest;
            visit(index);
        }
    }

    private void swap(int first, int second) {
        T temp = items[first];
        items[first] = items[second];
        items[second] = temp;
    }

    private void ensureCapacity(int required) {
        int capacity = items.length;
        while (capacity < required) {
            capacity *= GROWTH_FACTOR;
        }
        if (capacity != items.length) {
            items = Arrays.copyOf(items, capacity);
        }
    }

    private void startVisit() {
        visitedCount = 0;
    }

    private void visit(int index) {
        if (visitedCount == visited.length) {
            visited = Arrays.copyOf(visited, visited.length * GROWTH_FACTOR);
        }
        visited[visitedCount] = index;
        visitedCount++;
    }

    private void requireNotEmpty() {
        if (size == 0) {
            throw new IllegalStateException("Очередь пуста");
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Индекс " + index + " вне кучи размера " + size);
        }
    }

    private static int parent(int index) {
        return (index - 1) / 2;
    }

    private static int left(int index) {
        return 2 * index + 1;
    }

    private static int right(int index) {
        return 2 * index + 2;
    }
}
