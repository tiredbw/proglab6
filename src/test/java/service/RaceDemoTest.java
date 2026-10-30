package service;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import structure.SynchronizedTicketHeap;
import structure.TicketHeap;

import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceDemoTest {

    private static final int THREADS = 8;
    private static final int INSERTS = 20_000;

    @RepeatedTest(5)
    @Timeout(30)
    void protectedHeapKeepsAllElements() throws InterruptedException {
        RaceDemo.Result result = RaceDemo.insertConcurrently(
                new SynchronizedTicketHeap<>(Comparator.naturalOrder()), THREADS, INSERTS);

        assertTrue(result.isCorrect());
    }

    @Test
    @Timeout(30)
    void unprotectedRunFinishesWithoutLosingControl() throws InterruptedException {
        RaceDemo.Result result = RaceDemo.insertConcurrently(
                new TicketHeap<>(Comparator.naturalOrder()), THREADS, INSERTS);

        // Сам факт порчи зависит от планировщика, поэтому здесь проверяется только граница
        assertTrue(result.actualSize() <= result.expected());
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
    void reportDescribesExpectedCount() throws InterruptedException {
        RaceDemo.Report report = RaceDemo.run();

        assertEquals(RaceDemo.THREADS * RaceDemo.INSERTS_PER_THREAD, report.safe().expected());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
