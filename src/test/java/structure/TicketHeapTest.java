package structure;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketHeapTest {

    private static final long SEED = 42;

    private TicketHeap<Integer> heapOf(Integer... values) {
        TicketHeap<Integer> heap = new TicketHeap<>(Comparator.naturalOrder());
        for (int value : values) {
            heap.insert(value);
        }
        return heap;
    }

    // Изменяемый элемент: нужен, чтобы проверить changePriority
    private static final class Box {
        private int value;

        private Box(int value) {
            this.value = value;
        }
    }

    private TicketHeap<Box> boxHeapOf(int... values) {
        TicketHeap<Box> heap = new TicketHeap<>(Comparator.comparingInt((Box box) -> box.value));
        for (int value : values) {
            heap.insert(new Box(value));
        }
        return heap;
    }

    @Test
    void minimumStaysInRoot() {
        assertEquals(1, heapOf(5, 3, 8, 1, 4).peek());
    }

    @Test
    void peekDoesNotRemoveElement() {
        TicketHeap<Integer> heap = heapOf(5, 3);

        heap.peek();

        assertEquals(2, heap.size());
    }

    @Test
    void extractMinReturnsElementsInComparatorOrder() {
        TicketHeap<Integer> heap = heapOf(5, 3, 8, 1, 4, 7, 2, 6);
        List<Integer> extracted = new ArrayList<>();

        while (!heap.isEmpty()) {
            extracted.add(heap.extractMin());
        }

        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), extracted);
    }

    @Test
    void extractMinFromEmptyHeapFails() {
        assertThrows(IllegalStateException.class, () -> heapOf().extractMin());
    }

    @Test
    void peekOnEmptyHeapFails() {
        assertThrows(IllegalStateException.class, () -> heapOf().peek());
    }

    @Test
    void capacityDoublesWhenArrayIsFull() {
        TicketHeap<Integer> heap = heapOf();
        int initial = heap.capacity();

        for (int i = 0; i <= initial; i++) {
            heap.insert(i);
        }

        assertEquals(initial * 2, heap.capacity());
    }

    @Test
    void findReturnsIndexOfMatchingElement() {
        TicketHeap<Integer> heap = heapOf(1, 2, 3);

        assertEquals(3, heap.get(heap.find(value -> value == 3)));
    }

    @Test
    void findReturnsMinusOneWhenNothingMatches() {
        assertEquals(-1, heapOf(1, 2, 3).find(value -> value == 9));
    }

    @Test
    void findVisitsEveryNodeWhenNothingMatches() {
        TicketHeap<Integer> heap = heapOf(1, 2, 3);

        heap.find(value -> value == 9);

        assertArrayEquals(new int[]{0, 1, 2}, heap.lastVisited());
    }

    @Test
    void deleteLastElementKeepsOthers() {
        TicketHeap<Integer> heap = heapOf(1, 2, 3);

        heap.delete(2);

        assertEquals(List.of(1, 2), heap.toList());
    }

    @Test
    void deleteMovesLastElementUpWhenItIsSmallerThanParent() {
        // Дерево: 1 -> (5, 2), 5 -> (6, 7), 2 -> (3, 4). Последний элемент 4 меньше родителя 5
        TicketHeap<Integer> heap = heapOf(1, 5, 2, 6, 7, 3, 4);

        heap.delete(3);

        assertTrue(heap.isValidHeap());
    }

    @Test
    void deleteMovesLastElementDownWhenItIsLargerThanChildren() {
        TicketHeap<Integer> heap = heapOf(1, 2, 3, 4, 5, 6, 7);

        heap.delete(1);

        assertTrue(heap.isValidHeap());
    }

    @Test
    void deleteReturnsRemovedElement() {
        TicketHeap<Integer> heap = heapOf(1, 2, 3);

        assertEquals(2, heap.delete(1));
    }

    @Test
    void deleteWithBadIndexFails() {
        assertThrows(IndexOutOfBoundsException.class, () -> heapOf(1).delete(1));
    }

    @Test
    void topKReturnsSmallestInOrder() {
        assertEquals(List.of(1, 2, 3), heapOf(5, 3, 8, 1, 4, 7, 2, 6).topK(3));
    }

    @Test
    void topKDoesNotChangeHeap() {
        TicketHeap<Integer> heap = heapOf(5, 3, 8, 1, 4, 7, 2, 6);
        List<Integer> before = heap.toList();

        heap.topK(5);

        assertEquals(before, heap.toList());
    }

    @Test
    void topKLargerThanSizeReturnsAllElements() {
        assertEquals(List.of(1, 2), heapOf(2, 1).topK(10));
    }

    @Test
    void topKOfZeroIsEmpty() {
        assertEquals(List.of(), heapOf(1, 2).topK(0));
    }

    @Test
    void topKVisitsOnlySelectedNodes() {
        TicketHeap<Integer> heap = heapOf(1, 2, 3, 4, 5, 6, 7);

        heap.topK(2);

        assertEquals(2, heap.lastVisited().length);
    }

    @Test
    void changePriorityMovesElementUp() {
        TicketHeap<Box> heap = boxHeapOf(1, 2, 3, 4, 5, 6, 7);

        heap.changePriority(heap.find(box -> box.value == 7), box -> box.value = 0);

        assertEquals(0, heap.peek().value);
    }

    @Test
    void changePriorityMovesElementDown() {
        TicketHeap<Box> heap = boxHeapOf(1, 2, 3, 4, 5, 6, 7);

        heap.changePriority(0, box -> box.value = 9);

        assertEquals(2, heap.peek().value);
    }

    @Test
    void changePriorityKeepsInvariant() {
        TicketHeap<Box> heap = boxHeapOf(1, 2, 3, 4, 5, 6, 7);

        heap.changePriority(1, box -> box.value = 8);

        assertTrue(heap.isValidHeap());
    }

    @Test
    void changingElementOutsideHeapBreaksInvariant() {
        TicketHeap<Box> heap = boxHeapOf(1, 2, 3);

        heap.get(0).value = 9;

        assertFalse(heap.isValidHeap());
    }

    @Test
    void mergeKeepsInvariant() {
        TicketHeap<Integer> heap = heapOf(9, 7, 5, 3);

        heap.merge(heapOf(8, 6, 4, 2));

        assertTrue(heap.isValidHeap());
    }

    @Test
    void mergeKeepsAllElements() {
        TicketHeap<Integer> heap = heapOf(1, 3);

        heap.merge(heapOf(2, 4, 5));

        assertEquals(5, heap.size());
    }

    @Test
    void mergeWithEmptyHeapChangesNothing() {
        TicketHeap<Integer> heap = heapOf(1, 2, 3);

        heap.merge(heapOf());

        assertEquals(List.of(1, 2, 3), heap.toList());
    }

    @Test
    void mergeDoesNotChangeSecondHeap() {
        TicketHeap<Integer> other = heapOf(4, 5);

        heapOf(1).merge(other);

        assertEquals(List.of(4, 5), other.toList());
    }

    @Test
    void mergeWithItselfIsForbidden() {
        TicketHeap<Integer> heap = heapOf(1, 2);

        assertThrows(IllegalArgumentException.class, () -> heap.merge(heap));
    }

    @Test
    void addAllBuildsValidHeapFromUnsortedElements() {
        TicketHeap<Integer> heap = heapOf();

        heap.addAll(List.of(9, 8, 7, 6, 5, 4, 3, 2, 1));

        assertTrue(heap.isValidHeap());
    }

    @Test
    void clearRemovesAllElements() {
        TicketHeap<Integer> heap = heapOf(1, 2, 3);

        heap.clear();

        assertTrue(heap.isEmpty());
    }

    @Test
    void replaceWithTakesAllElements() {
        TicketHeap<Integer> heap = heapOf(9);

        heap.replaceWith(heapOf(1, 2, 3));

        assertEquals(List.of(1, 2, 3), heap.toList());
    }

    @Test
    void replaceWithLeavesSourceEmptyAndIndependent() {
        TicketHeap<Integer> heap = heapOf();
        TicketHeap<Integer> source = heapOf(1, 2, 3);

        heap.replaceWith(source);
        source.insert(7);

        assertEquals(List.of(1, 2, 3), heap.toList());
    }

    @Test
    void insertRecordsSiftUpPath() {
        TicketHeap<Integer> heap = heapOf(1, 2, 3, 4);

        heap.insert(0);

        assertArrayEquals(new int[]{4, 1, 0}, heap.lastVisited());
    }

    @Test
    void randomOperationsKeepInvariant() {
        Random random = new Random(SEED);
        TicketHeap<Integer> heap = heapOf();
        boolean alwaysValid = true;

        for (int step = 0; step < 20_000; step++) {
            if (heap.isEmpty() || random.nextInt(3) > 0) {
                heap.insert(random.nextInt(1000));
            } else if (random.nextBoolean()) {
                heap.extractMin();
            } else {
                heap.delete(random.nextInt(heap.size()));
            }
            alwaysValid &= heap.isValidHeap();
        }

        assertTrue(alwaysValid);
    }

    @Test
    void heapSortMatchesArraysSort() {
        Random random = new Random(SEED);
        int[] values = random.ints(5_000, 0, 1000).toArray();
        TicketHeap<Integer> heap = heapOf();
        for (int value : values) {
            heap.insert(value);
        }

        int[] extracted = new int[values.length];
        for (int i = 0; i < extracted.length; i++) {
            extracted[i] = heap.extractMin();
        }

        Arrays.sort(values);
        assertArrayEquals(values, extracted);
    }
}
