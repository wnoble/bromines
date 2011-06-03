package org.cow9.minesweeper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Bromines extends JFrame {
    private Board board;
    private TopPanel top;
    private ImgSet imgSet;

    public Bromines(int height, int width, int mineCount) {
        setTitle("Bromines");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        imgSet = new NativeImgSet();

        board = new Board(imgSet, height, width, mineCount);
        add(board, BorderLayout.CENTER);

        top = new TopPanel(board);
        add(top, BorderLayout.NORTH);

        addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {}
			public void keyReleased(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_F2: board.restart(); break;
				case KeyEvent.VK_ESCAPE: System.exit(0); break;
				}
			}
		});

        pack();
        setResizable(false);
    }
    
    public static void main(String[] args) {
    	SwingUtilities.invokeLater(new Runnable() {
    		public void run() {
    			(new Bromines(16, 30, 99)).setVisible(true);
    		}
    	});
    }
}

