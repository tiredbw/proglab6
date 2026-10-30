package service;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import structure.SynchronizedTicketHeap;
import structure.TicketHeap;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceDemoTest {

    private static final int THREADS = 8;
    private static final int INSERTS = 20_000;

    @RepeatedTest(5)
    @Timeout(30)
    void protectedHeapKeepsAllElements() throws InterruptedException {
        RaceDemo.Result result = RaceDemo.insertConcurrently(
                new SynchronizedTicketHeap<Integer>(Comparator.naturalOrder()), THREADS, INSERTS);

        assertTrue(result.isCorrect());
    }

    @Test
    @Timeout(30)
    void unprotectedRunFinishesWithinBounds() throws InterruptedException {
        RaceDemo.Result result = RaceDemo.insertConcurrently(
                new TicketHeap<Integer>(Comparator.naturalOrder()), THREADS, INSERTS);

        // Сам факт порчи зависит от планировщика, поэтому здесь проверяются только границы
        assertTrue(result.actualSize() > 0 && result.actualSize() <= result.expected());
    }

    // Детерминированная гонка «проверил, затем действую»: первый поток решил поднять элемент
    // и замер внутри сравнения, а второй за это время изменил кучу. Обмен первого портит инвариант
    @Test
    @Timeout(10)
    void staleSwapBreaksUnprotectedHeap() throws InterruptedException {
        CountDownLatch slowInCompare = new CountDownLatch(1);
        CountDownLatch mainFinished = new CountDownLatch(1);
        AtomicBoolean paused = new AtomicBoolean();

        Comparator<Integer> pausingComparator = (a, b) -> {
            if (a == 3 && paused.compareAndSet(false, true)) {
                slowInCompare.countDown();
                await(mainFinished);
            }
            return Integer.compare(a, b);
        };

        TicketHeap<Integer> heap = new TicketHeap<>(pausingComparator);
        heap.insert(5);

        Thread slow = new Thread(() -> heap.insert(3));
        slow.start();
        slowInCompare.await();

        heap.insert(1);
        mainFinished.countDown();
        slow.join();

        assertFalse(heap.isValidHeap());
    }

    @Test
    void reportShowsProtectedHeapAsCorrect() throws InterruptedException {
        assertTrue(RaceDemo.run().safe().isCorrect());
    }

    @Test
    void descriptionContainsActualSize() {
        RaceDemo.Result unsafe = new RaceDemo.Result(100, 42, false, 3);
        RaceDemo.Result safe = new RaceDemo.Result(100, 100, true, 0);

        assertTrue(RaceDemo.describe(new RaceDemo.Report(unsafe, safe)).contains("В куче элементов: 42"));
    }

    @Test
    void everyPublicHeapMethodIsSynchronized() throws NoSuchMethodException {
        for (Method method : TicketHeap.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                Method override = SynchronizedTicketHeap.class
                        .getDeclaredMethod(method.getName(), method.getParameterTypes());
                assertTrue(Modifier.isSynchronized(override.getModifiers()), method.getName());
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
