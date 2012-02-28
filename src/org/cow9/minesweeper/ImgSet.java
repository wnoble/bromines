package org.cow9.minesweeper;

import java.awt.Component;
import java.awt.Graphics;

public interface ImgSet {
    int width();
    int height();
    void paintNum(Component c, Graphics g, int x, int y, int n);
    void paintFlag(Component c, Graphics g, int x, int y);
    void paintMine(Component c, Graphics g, int x, int y);
    void paintMineFatal(Component c, Graphics g, int x, int y);
    void paintMineWrong(Component c, Graphics g, int x, int y);
}
