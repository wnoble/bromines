package org.cow9.minesweeper;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

public class NativeImgSet implements ImgSet {
    public void paintMine(Component c, Graphics g, int x, int y) {
        g.setColor(Color.BLACK);
        g.fillRect(x+4, y+6, 9, 5);
        g.fillRect(x+6, y+4, 5, 9);
        g.drawLine(x+4, y+4, x+12, y+12);
        g.drawLine(x+4, y+12, x+12, y+4);
        g.drawLine(x+2, y+8, x+14, y+8);
        g.drawLine(x+8, y+2, x+8, y+14);
        g.setColor(Color.WHITE);
        g.fillRect(x+6, y+6, 2, 2);
    }
    
    public void paintFlag(Component c, Graphics g, int x, int y) {
        g.setColor(Color.RED);
        g.drawPolyline(new int[]{x+4, x+7, x+8, x+8, x+5},
                new int[]{y+5, y+3, y+3, y+7, y+6}, 5);
        g.fillRect(x+5, y+4, 4, 3);
        g.setColor(Color.BLACK);
        g.drawLine(x+8, y+8, x+8, y+9);
        g.drawLine(x+6, y+10, x+9, y+10);
        g.fillRect(x+4, y+11, 8, 2);
    }
    
    public void paintMineFatal(Component c, Graphics g, int x, int y) {
        g.setColor(Color.RED);
        g.fillRect(x+1, y+1, 15, 15);
        paintMine(c, g, x, y);
    }
    public void paintMineWrong(Component c, Graphics g, int x, int y) {
        paintMine(c, g, x, y);
        g.setColor(Color.RED);
        g.drawLine(x+2, y+3, x+13, y+14);
        g.drawLine(x+3, y+3, x+14, y+14);
        g.drawLine(x+2, y+14, x+13, y+3);
        g.drawLine(x+3, y+14, x+14, y+3);
    }
    
    private final class Num {
        public final Color color;
        public final byte[] xs, ys;
        public final int strokes;
        public Num(Color color, byte[] xs, byte[] ys, int strokes) {
            this.color = color;
            this.xs = xs;
            this.ys = ys;
            this.strokes = strokes;
        }
    }
    
    private final Num[] nums = {
            // 1
            new Num(Color.BLUE,
                    new byte[]{8, 5, 6, 9, 9, 11, 11, 5, 5, 7, 7, 8, 8},
                    new byte[]{3, 6, 6, 3, 11, 11, 12, 12, 11, 11, 5, 5, 11},
                    13),
            // 2
            new Num(new Color(0x0, 0x80, 0x0),
                    new byte[]{3, 12, 12, 3, 3, 12, 11, 4, 4, 10, 10, 12, 12,
                        3, 3, 5, 4, 11},
                    new byte[]{12, 12, 11, 11, 10, 6, 7, 11, 9, 6, 5, 5, 4, 4,
                        5, 5, 3, 3},
                    18),
            // 3
            new Num(Color.RED,
                    new byte[]{3, 11, 11, 3, 3, 10, 10, 6, 6, 10, 10, 3, 12,
                        12, 11, 11, 12, 12, 10, 10},
                    new byte[]{12, 12, 3, 3, 4, 4, 7, 7, 8, 8, 11, 11, 11, 9,
                        9, 4, 4, 6, 6, 12},
                    20),
            // 4
            new Num(new Color(0x0, 0x0, 0x80),
                    new byte[]{11, 11, 9, 9, 10, 10, 12, 12, 3, 5, 6, 4, 7, 5,
                        8},
                    new byte[]{3, 12, 12, 3, 3, 11, 7, 8, 8, 3, 3, 8, 3, 7, 7},
                    15),
            // 5
            new Num(new Color(0x80, 0x0, 0x0),
                    new byte[]{12, 3, 3, 10, 10, 3, 3, 11, 12, 12, 11, 5, 5,
                        12, 4, 4, 11, 11},
                    new byte[]{3, 3, 8, 8, 11, 11, 12, 12, 11, 8, 7, 7, 4, 4,
                        4, 8, 8, 11},
                    18),
            //6
            new Num(new Color(0x0, 0x80, 0x80),
                    new byte[]{11, 4, 3, 3, 4, 11, 12, 12, 11, 5, 5, 11, 4,
                        4, 11, 11, 5, 5, 10, 10},
                    new byte[]{3, 3, 4, 11, 12, 12, 11, 8, 7, 7, 4, 4, 4, 11,
                        11, 8, 8, 11, 11, 9},
                    20),
            // 7
            new Num(Color.BLACK,
                    new byte[]{9, 12, 11, 8, 7, 10, 12, 3, 3, 12},
                    new byte[]{12, 5, 5, 12, 12, 5, 4, 4, 3, 3},
                    10),
            // 8
            new Num(new Color(0x80, 0x80, 0x80),
                    new byte[]{3, 3, 12, 12, 11, 12, 12, 3, 3, 4, 4, 11, 11,
                        4, 4, 10, 10, 5, 5, 5, 9, 9, 6},
                    new byte[]{6, 4, 4, 6, 7, 9, 11, 11, 9, 8, 3, 3, 12, 12,
                        4, 4, 11, 11, 5, 7, 7, 8, 8},
                    23)
    };
    
    private final int[] xbuf = new int[23], ybuf = new int[23];
    public void paintNum(Component c, Graphics g, int x, int y, int n) {
        Num num = nums[n-1];
        int strokes = num.strokes;
        for (int i = 0; i < strokes; i++) {
            xbuf[i] = x + num.xs[i];
            ybuf[i] = y + num.ys[i];
        }
        g.setColor(num.color);
        g.drawPolyline(xbuf, ybuf, strokes);
    }

    public int width() {return 16;}
    public int height() {return 16;}
}
