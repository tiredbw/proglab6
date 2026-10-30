package service;

import structure.SynchronizedTicketHeap;
import structure.TicketHeap;

import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

// Демонстрация гонки: несколько потоков одновременно вставляют числа в кучу.
// Работает с собственными кучами чисел, очередь талонов не затрагивается.
public final class RaceDemo {
    public static final int THREADS = 8;
    public static final int INSERTS_PER_THREAD = 50_000;

    public record Result(int expected, int actualSize, boolean validHeap, int failedInserts) {
        public boolean isCorrect() {
            return actualSize == expected && validHeap && failedInserts == 0;
        }
    }

    public record Report(Result unsafe, Result safe) {
    }

    private RaceDemo() {
    }

    public static Report run() throws InterruptedException {
        Result unsafe = insertConcurrently(new TicketHeap<>(Comparator.naturalOrder()), THREADS, INSERTS_PER_THREAD);
        Result safe = insertConcurrently(
                new SynchronizedTicketHeap<>(Comparator.naturalOrder()), THREADS, INSERTS_PER_THREAD);
        return new Report(unsafe, safe);
    }

    public static String describe(Report report) {
        return "Потоков: " + THREADS + ", вставок на поток: " + INSERTS_PER_THREAD
                + ", всего должно быть элементов: " + report.safe().expected() + "\n\n"
                + "Без защиты\n" + describe(report.unsafe()) + "\n\n"
                + "С synchronized\n" + describe(report.safe()) + "\n\n"
                + "Результат незащищённой кучи меняется от запуска к запуску: он зависит от планировщика потоков.";
    }

    private static String describe(Result result) {
        return "В куче элементов: " + result.actualSize()
                + ", потеряно: " + (result.expected() - result.actualSize())
                + "\nИнвариант кучи: " + (result.validHeap() ? "соблюдён" : "нарушен")
                + "\nВставок, упавших с исключением: " + result.failedInserts();
    }

    public static Result insertConcurrently(TicketHeap<Integer> heap, int threads, int insertsPerThread)
            throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger failedInserts = new AtomicInteger();

        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            int threadNumber = i;
            workers[i] = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int value = insertsPerThread; value > 0; value--) {
                    try {
                        // Убывающие значения заставляют каждую вставку всплывать до корня и менять много ячеек
                        heap.insert(value * threads + threadNumber);
                    } catch (RuntimeException e) {
                        // Незащищённая куча может упасть на полпути: такой исход и показывает демонстрация
                        failedInserts.incrementAndGet();
                    }
                }
            });
            workers[i].start();
        }

        start.countDown();
        for (Thread worker : workers) {
            worker.join();
        }

        return new Result(threads * insertsPerThread, heap.size(), heap.isValidHeap(), failedInserts.get());
    }
}
