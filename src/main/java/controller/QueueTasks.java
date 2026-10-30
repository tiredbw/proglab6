package controller;

import csv.CsvLoader;
import javafx.concurrent.Task;
import model.Ticket;
import service.TicketGenerator;
import structure.TicketHeap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

// Фоновые операции. Они работают только с локальными данными и не трогают основную кучу: готовую кучу
// подменяет FX-поток, когда задача завершилась. Поэтому гонки здесь нет, а GUI не ждёт тяжёлой сборки.
public final class QueueTasks {
    private static final int PROGRESS_STEP = 1000;

    public record LoadedQueue(TicketHeap<Ticket> heap, int loadedCount, List<String> errors) {
    }

    private QueueTasks() {
    }

    public static <V> Task<V> background(String message, Callable<V> work) {
        return new Task<>() {
            @Override
            protected V call() throws Exception {
                updateMessage(message);
                return work.call();
            }
        };
    }

    public static TicketHeap<Ticket> heapOf(List<Ticket> tickets) {
        TicketHeap<Ticket> heap = new TicketHeap<>(Ticket.PRIORITY);
        heap.addAll(tickets);
        return heap;
    }

    // current нужен снимком, сделанным в FX-потоке до старта задачи
    public static Task<TicketHeap<Ticket>> generate(List<Ticket> current, int count) {
        return new Task<>() {
            @Override
            protected TicketHeap<Ticket> call() {
                updateMessage("Генерация " + count + " талонов");
                TicketGenerator generator = new TicketGenerator();
                List<Ticket> tickets = new ArrayList<>(count);
                for (int i = 0; i < count && !isCancelled(); i++) {
                    tickets.add(generator.next());
                    if (i % PROGRESS_STEP == 0) {
                        updateProgress(i, count);
                    }
                }
                TicketHeap<Ticket> queue = heapOf(current);
                queue.addAll(tickets);
                return queue;
            }
        };
    }

    // Файл читается как вторая очередь и сливается с current. Для простой загрузки current пуст
    public static Task<LoadedQueue> loadCsv(Path file, List<Ticket> current) {
        return new Task<>() {
            @Override
            protected LoadedQueue call() throws IOException {
                updateMessage("Чтение файла " + file.getFileName());
                CsvLoader.LoadResult result = new CsvLoader().loadWithReport(file, (done, total) -> {
                    if (isCancelled()) {
                        throw new CancellationException();
                    }
                    updateProgress(done, total);
                });

                TicketHeap<Ticket> queue = heapOf(current);
                queue.merge(heapOf(result.tickets()));
                return new LoadedQueue(queue, result.tickets().size(), result.errors());
            }
        };
    }
}
