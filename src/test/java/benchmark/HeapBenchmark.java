package benchmark;

import model.RegularTicket;
import model.Ticket;
import service.TicketGenerator;
import structure.TicketHeap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.function.Supplier;

// Замеры TicketHeap и PriorityQueue. Лежит в src/test: по заданию аналога из java.util в основном коде быть не должно.
// Каждая операция прогревается, затем замеряется несколько раз, в таблицу идёт медиана.
public final class HeapBenchmark {
    private static final int[] SIZES = {10_000, 100_000};
    private static final int WARMUP_RUNS = 15;
    private static final int MEASURED_RUNS = 15;
    private static final int LOOKUPS = 200;
    private static final int TOP_K = 100;
    private static final int TOP_K_REPEATS = 100;
    private static final long SEED = 2026;
    private static final double NANOS_IN_MILLI = 1_000_000.0;

    // Результат складывается сюда, чтобы компилятор не выбросил вычисления как ненужные
    private static long sink;

    private record Row(String name, double[] ownMillis, double[] javaMillis) {
    }

    private HeapBenchmark() {
    }

    public static void main(String[] args) {
        List<Row> rows = new ArrayList<>();
        rows.add(measure("insert, n вставок", HeapBenchmark::ownInsert, HeapBenchmark::javaInsert));
        rows.add(measure("extractMin, n извлечений", HeapBenchmark::ownExtract, HeapBenchmark::javaExtract));
        rows.add(measure("find, " + LOOKUPS + " поисков", HeapBenchmark::ownFind, HeapBenchmark::javaFind));
        rows.add(measure("delete, " + LOOKUPS + " удалений", HeapBenchmark::ownDelete, HeapBenchmark::javaDelete));
        rows.add(measure("topK, k=" + TOP_K + ", " + TOP_K_REPEATS + " раз",
                HeapBenchmark::ownTopK, HeapBenchmark::javaTopK));
        rows.add(measure("changePriority, " + LOOKUPS + " изменений",
                HeapBenchmark::ownChange, HeapBenchmark::javaChange));
        rows.add(measure("merge, две очереди по n", HeapBenchmark::ownMerge, HeapBenchmark::javaMerge));

        System.out.println("Прогрев: " + WARMUP_RUNS + " прогонов, замер: " + MEASURED_RUNS
                + " прогонов, значение: медиана, мс. Java " + Runtime.version() + ".");
        System.out.println();
        System.out.println("| Операция | TicketHeap, 10^4 | PriorityQueue, 10^4 | TicketHeap, 10^5 | PriorityQueue, 10^5 |");
        System.out.println("|---|---|---|---|---|");
        for (Row row : rows) {
            System.out.printf(Locale.ROOT, "| %s | %.3f | %.3f | %.3f | %.3f |%n", row.name(),
                    row.ownMillis()[0], row.javaMillis()[0], row.ownMillis()[1], row.javaMillis()[1]);
        }
        System.out.println();
        System.out.println("Контрольная сумма: " + sink);
    }

    private static Row measure(String name, IntFunction<Runnable> own, IntFunction<Runnable> javaQueue) {
        double[] ownMillis = new double[SIZES.length];
        double[] javaMillis = new double[SIZES.length];
        for (int i = 0; i < SIZES.length; i++) {
            int size = SIZES[i];
            ownMillis[i] = medianMillis(() -> own.apply(size));
            javaMillis[i] = medianMillis(() -> javaQueue.apply(size));
        }
        return new Row(name, ownMillis, javaMillis);
    }

    // Подготовка данных идёт вне замера: Supplier собирает операцию, а время берётся только у её запуска
    private static double medianMillis(Supplier<Runnable> prepare) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            prepare.get().run();
        }
        double[] times = new double[MEASURED_RUNS];
        for (int i = 0; i < MEASURED_RUNS; i++) {
            Runnable operation = prepare.get();
            long start = System.nanoTime();
            operation.run();
            times[i] = (System.nanoTime() - start) / NANOS_IN_MILLI;
        }
        Arrays.sort(times);
        return times[MEASURED_RUNS / 2];
    }

    private static List<Ticket> tickets(int count) {
        return new TicketGenerator(SEED).generate(count);
    }

    private static TicketHeap<Ticket> ownHeap(List<Ticket> tickets) {
        TicketHeap<Ticket> heap = new TicketHeap<>(Ticket.PRIORITY);
        heap.addAll(tickets);
        return heap;
    }

    private static PriorityQueue<Ticket> javaQueue(List<Ticket> tickets) {
        PriorityQueue<Ticket> queue = new PriorityQueue<>(Ticket.PRIORITY);
        queue.addAll(tickets);
        return queue;
    }

    private static List<Ticket> sample(List<Ticket> tickets) {
        List<Ticket> shuffled = new ArrayList<>(tickets);
        Collections.shuffle(shuffled, new Random(SEED));
        return shuffled.subList(0, LOOKUPS);
    }

    private static List<RegularTicket> sampleEditable(List<Ticket> tickets) {
        List<Ticket> shuffled = new ArrayList<>(tickets);
        Collections.shuffle(shuffled, new Random(SEED));
        List<RegularTicket> result = new ArrayList<>();
        for (Ticket ticket : shuffled) {
            if (ticket instanceof RegularTicket regular && result.size() < LOOKUPS) {
                result.add(regular);
            }
        }
        return result;
    }

    private static Runnable ownInsert(int size) {
        List<Ticket> tickets = tickets(size);
        return () -> {
            TicketHeap<Ticket> heap = new TicketHeap<>(Ticket.PRIORITY);
            for (Ticket ticket : tickets) {
                heap.insert(ticket);
            }
            sink += heap.size();
        };
    }

    private static Runnable javaInsert(int size) {
        List<Ticket> tickets = tickets(size);
        return () -> {
            PriorityQueue<Ticket> queue = new PriorityQueue<>(Ticket.PRIORITY);
            for (Ticket ticket : tickets) {
                queue.offer(ticket);
            }
            sink += queue.size();
        };
    }

    private static Runnable ownExtract(int size) {
        TicketHeap<Ticket> heap = ownHeap(tickets(size));
        return () -> {
            while (!heap.isEmpty()) {
                sink += heap.extractMin().getUrgency();
            }
        };
    }

    private static Runnable javaExtract(int size) {
        PriorityQueue<Ticket> queue = javaQueue(tickets(size));
        return () -> {
            while (!queue.isEmpty()) {
                sink += queue.poll().getUrgency();
            }
        };
    }

    private static Runnable ownFind(int size) {
        List<Ticket> tickets = tickets(size);
        TicketHeap<Ticket> heap = ownHeap(tickets);
        List<Ticket> targets = sample(tickets);
        return () -> {
            for (Ticket target : targets) {
                String card = target.getCardNumber();
                sink += heap.find(ticket -> ticket.getCardNumber().equals(card));
            }
        };
    }

    private static Runnable javaFind(int size) {
        List<Ticket> tickets = tickets(size);
        PriorityQueue<Ticket> queue = javaQueue(tickets);
        List<Ticket> targets = sample(tickets);
        return () -> {
            for (Ticket target : targets) {
                String card = target.getCardNumber();
                for (Ticket ticket : queue) {
                    if (ticket.getCardNumber().equals(card)) {
                        sink += ticket.getUrgency();
                        break;
                    }
                }
            }
        };
    }

    private static Runnable ownDelete(int size) {
        List<Ticket> tickets = tickets(size);
        TicketHeap<Ticket> heap = ownHeap(tickets);
        List<Ticket> targets = sample(tickets);
        return () -> {
            for (Ticket target : targets) {
                heap.delete(heap.find(ticket -> ticket == target));
            }
            sink += heap.size();
        };
    }

    private static Runnable javaDelete(int size) {
        List<Ticket> tickets = tickets(size);
        PriorityQueue<Ticket> queue = javaQueue(tickets);
        List<Ticket> targets = sample(tickets);
        return () -> {
            for (Ticket target : targets) {
                queue.remove(target);
            }
            sink += queue.size();
        };
    }

    private static Runnable ownTopK(int size) {
        TicketHeap<Ticket> heap = ownHeap(tickets(size));
        return () -> {
            for (int i = 0; i < TOP_K_REPEATS; i++) {
                sink += heap.topK(TOP_K).size();
            }
        };
    }

    // У PriorityQueue нет своего topK: снимаем копию и извлекаем k раз, исходная очередь не меняется
    private static Runnable javaTopK(int size) {
        PriorityQueue<Ticket> queue = javaQueue(tickets(size));
        return () -> {
            for (int i = 0; i < TOP_K_REPEATS; i++) {
                PriorityQueue<Ticket> copy = new PriorityQueue<>(queue);
                for (int k = 0; k < TOP_K; k++) {
                    sink += copy.poll().getUrgency();
                }
            }
        };
    }

    private static Runnable ownChange(int size) {
        List<Ticket> tickets = tickets(size);
        TicketHeap<Ticket> heap = ownHeap(tickets);
        List<RegularTicket> targets = sampleEditable(tickets);
        return () -> {
            for (RegularTicket target : targets) {
                int urgency = (target.getUrgency() + 1) % (Ticket.MAX_URGENCY + 1);
                heap.changePriority(heap.find(ticket -> ticket == target), ticket -> target.setUrgency(urgency));
            }
            sink += heap.size();
        };
    }

    // У PriorityQueue нет изменения приоритета: удаляем элемент, меняем его и вставляем заново
    private static Runnable javaChange(int size) {
        List<Ticket> tickets = tickets(size);
        PriorityQueue<Ticket> queue = javaQueue(tickets);
        List<RegularTicket> targets = sampleEditable(tickets);
        return () -> {
            for (RegularTicket target : targets) {
                int urgency = (target.getUrgency() + 1) % (Ticket.MAX_URGENCY + 1);
                queue.remove(target);
                target.setUrgency(urgency);
                queue.add(target);
            }
            sink += queue.size();
        };
    }

    private static Runnable ownMerge(int size) {
        List<Ticket> tickets = tickets(size);
        TicketHeap<Ticket> first = ownHeap(tickets);
        TicketHeap<Ticket> second = ownHeap(tickets);
        return () -> {
            first.merge(second);
            sink += first.size();
        };
    }

    private static Runnable javaMerge(int size) {
        List<Ticket> tickets = tickets(size);
        PriorityQueue<Ticket> first = javaQueue(tickets);
        PriorityQueue<Ticket> second = javaQueue(tickets);
        return () -> {
            first.addAll(second);
            sink += first.size();
        };
    }
}
