package com.nexoventas.desktop;

import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

/** Visual identity shared by the desktop shell and its native window icon. */
public final class BrandAssets {
    private BrandAssets() { }

    public static Image windowIcon() {
        Canvas canvas = new Canvas(64, 64);
        draw(canvas.getGraphicsContext2D(), 64);
        WritableImage image = new WritableImage(64, 64);
        canvas.snapshot(new SnapshotParameters(), image);
        return image;
    }

    public static Node logoMark(double size) {
        Canvas canvas = new Canvas(size, size);
        draw(canvas.getGraphicsContext2D(), size);
        StackPane container = new StackPane(canvas);
        container.getStyleClass().add("logo-mark");
        return container;
    }

    private static void draw(GraphicsContext graphics, double size) {
        double scale = size / 64d;
        graphics.setFill(Color.web("#102a4c"));
        graphics.fillRoundRect(2 * scale, 2 * scale, 60 * scale, 60 * scale, 16 * scale, 16 * scale);
        graphics.setStroke(Color.web("#42c7ff"));
        graphics.setLineWidth(3 * scale);
        graphics.strokeRoundRect(3.5 * scale, 3.5 * scale, 57 * scale, 57 * scale, 14 * scale, 14 * scale);
        graphics.setFill(Color.web("#42c7ff"));
        graphics.fillRoundRect(16 * scale, 18 * scale, 32 * scale, 9 * scale, 4 * scale, 4 * scale);
        graphics.fillRoundRect(16 * scale, 34 * scale, 20 * scale, 9 * scale, 4 * scale, 4 * scale);
        graphics.setFill(Color.web("#ffffff"));
        graphics.fillArc(39 * scale, 34 * scale, 12 * scale, 12 * scale, 45, 270, ArcType.ROUND);
    }
}
