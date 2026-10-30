package controller;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import model.Ticket;
import structure.TicketHeap;

// Панель кучи: сверху дерево, построенное по индексам массива, снизу сам массив.
// Canvas не умеет менять размер сам, поэтому лежит внутри Pane и следует за его размерами.
public class HeapCanvas extends Pane {
    private static final int MAX_LEVELS = 6;
    private static final int MAX_NODES = (1 << MAX_LEVELS) - 1;

    private static final double CAPTION_HEIGHT = 26;
    private static final double ARRAY_HEIGHT = 66;
    private static final double MARGIN = 10;
    private static final double MAX_RADIUS = 17;
    private static final double RADIUS_TO_SLOT = 0.42;
    private static final double MIN_SLOT_FOR_CARD = 84;
    private static final double CARD_OFFSET = 7;
    private static final double CELL_WIDTH = 38;
    private static final double CELL_HEIGHT = 28;
    private static final double CELL_INDEX_OFFSET = 8;

    private static final Font CAPTION_FONT = Font.font(13);
    private static final Font NODE_FONT = Font.font(14);
    private static final Font SMALL_FONT = Font.font(10);

    private static final Color BACKGROUND = Color.web("#FAFAFA");
    private static final Color TEXT = Color.web("#1F2933");
    private static final Color MUTED_TEXT = Color.web("#52606D");
    private static final Color EDGE = Color.web("#9AA5B1");
    private static final Color NODE = Color.web("#D9E2EC");
    private static final Color NODE_BORDER = Color.web("#486581");
    private static final Color VISITED = Color.web("#FFD166");
    private static final Color LAST_VISITED = Color.web("#F4845F");

    private final TicketHeap<Ticket> heap;
    private final Canvas canvas = new Canvas();

    public HeapCanvas(TicketHeap<Ticket> heap) {
        this.heap = heap;
        getChildren().add(canvas);
        setMinSize(0, 0);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        widthProperty().addListener(observable -> redraw());
        heightProperty().addListener(observable -> redraw());
    }

    public void redraw() {
        double width = getWidth();
        double height = getHeight();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(BACKGROUND);
        g.fillRect(0, 0, width, height);
        g.setTextBaseline(VPos.CENTER);

        int size = heap.size();
        if (size == 0) {
            drawCaption(g, "Очередь пуста", width);
            return;
        }

        int shownNodes = Math.min(size, MAX_NODES);
        int shownCells = Math.min(size, (int) ((width - 2 * MARGIN) / CELL_WIDTH));
        int[] visited = heap.lastVisited();
        boolean[] highlighted = highlightedNodes(visited, Math.max(shownNodes, shownCells));
        int last = visited.length == 0 ? -1 : visited[visited.length - 1];

        drawCaption(g, "Показано " + shownNodes + " из " + size + " узлов и " + shownCells
                + " ячеек массива, ёмкость " + heap.capacity()
                + ". В узле срочность, жёлтым посещённые, оранжевым последний посещённый", width);
        drawTree(g, shownNodes, highlighted, last, width, height - ARRAY_HEIGHT);
        drawArray(g, shownCells, highlighted, last, height - ARRAY_HEIGHT);
    }

    private boolean[] highlightedNodes(int[] visited, int limit) {
        boolean[] highlighted = new boolean[limit];
        for (int index : visited) {
            if (index < limit) {
                highlighted[index] = true;
            }
        }
        return highlighted;
    }

    private void drawCaption(GraphicsContext g, String text, double width) {
        g.setFont(CAPTION_FONT);
        g.setFill(MUTED_TEXT);
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText(text, MARGIN, CAPTION_HEIGHT / 2, width - 2 * MARGIN);
    }

    private void drawTree(GraphicsContext g, int nodes, boolean[] highlighted, int last, double width, double bottom) {
        int levels = levelOf(nodes - 1) + 1;
        double levelHeight = (bottom - CAPTION_HEIGHT) / levels;
        double radius = Math.min(Math.min(MAX_RADIUS, levelHeight * RADIUS_TO_SLOT),
                width / (1 << (levels - 1)) * RADIUS_TO_SLOT);

        g.setStroke(EDGE);
        for (int i = 1; i < nodes; i++) {
            int parent = (i - 1) / 2;
            g.strokeLine(nodeX(parent, width), nodeY(parent, levelHeight), nodeX(i, width), nodeY(i, levelHeight));
        }

        g.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < nodes; i++) {
            double x = nodeX(i, width);
            double y = nodeY(i, levelHeight);
            g.setFill(colorOf(i, highlighted, last));
            g.fillOval(x - radius, y - radius, 2 * radius, 2 * radius);
            g.setStroke(NODE_BORDER);
            g.strokeOval(x - radius, y - radius, 2 * radius, 2 * radius);

            Ticket ticket = heap.get(i);
            g.setFill(TEXT);
            g.setFont(NODE_FONT);
            g.fillText(String.valueOf(ticket.getUrgency()), x, y);
            if (width / (1 << levelOf(i)) >= MIN_SLOT_FOR_CARD) {
                g.setFont(SMALL_FONT);
                g.setFill(MUTED_TEXT);
                g.fillText(ticket.getCardNumber(), x, y + radius + CARD_OFFSET);
            }
        }
    }

    private void drawArray(GraphicsContext g, int cells, boolean[] highlighted, int last, double top) {
        double cellTop = top + CELL_INDEX_OFFSET * 2;
        g.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < cells; i++) {
            double x = MARGIN + i * CELL_WIDTH;
            g.setFill(colorOf(i, highlighted, last));
            g.fillRect(x, cellTop, CELL_WIDTH, CELL_HEIGHT);
            g.setStroke(NODE_BORDER);
            g.strokeRect(x, cellTop, CELL_WIDTH, CELL_HEIGHT);

            g.setFill(MUTED_TEXT);
            g.setFont(SMALL_FONT);
            g.fillText(String.valueOf(i), x + CELL_WIDTH / 2, top + CELL_INDEX_OFFSET);
            g.setFill(TEXT);
            g.setFont(NODE_FONT);
            g.fillText(String.valueOf(heap.get(i).getUrgency()), x + CELL_WIDTH / 2, cellTop + CELL_HEIGHT / 2);
        }
    }

    private Color colorOf(int index, boolean[] highlighted, int last) {
        if (index == last) {
            return LAST_VISITED;
        }
        return highlighted[index] ? VISITED : NODE;
    }

    private static int levelOf(int index) {
        return Integer.SIZE - 1 - Integer.numberOfLeadingZeros(index + 1);
    }

    private static double nodeX(int index, double width) {
        int level = levelOf(index);
        int positionInLevel = index + 1 - (1 << level);
        return (positionInLevel + 0.5) * width / (1 << level);
    }

    private static double nodeY(int index, double levelHeight) {
        return CAPTION_HEIGHT + (levelOf(index) + 0.5) * levelHeight;
    }
}
