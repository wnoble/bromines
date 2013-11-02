package org.cow9.minesweeper;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Bromines {
	private final GameObserver observer = new GameObserver() {
		@Override public void started() { stopWatch.start(); }
		@Override public void restarted() { stopWatch.reset(); }
		@Override public void cleared() { stopWatch.stop(); }
		@Override public void died() { stopWatch.stop(); }
		@Override public void numFlagged(int n) { mineCounter.numFlagged(n); }
	};
    private Board board = new Board(new NativeImgSet(), 16, 30, 99, observer);
    private final StopWatch stopWatch = new StopWatch();
    private final MineCounter mineCounter = new MineCounter();
    private final Color white = Color.WHITE, gray = new Color(0xc0, 0xc0, 0xc0),
        darkGray = new Color(0x80, 0x80, 0x80);

    private class TopLevel extends JFrame {
    	public void setupUI() {
            setTitle("Bromines");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            add(new GameContent(), BorderLayout.CENTER);
            
            JMenuBar menuBar = new JMenuBar();
            JMenu gameMenu = new JMenu("Game");
            menuBar.add(gameMenu);
            gameMenu.add(new JMenuItem("New"));
            gameMenu.addSeparator();
            JMenuItem beg = new JRadioButtonMenuItem("Beginner");
            gameMenu.add(beg);
            JMenuItem inter = new JRadioButtonMenuItem("Intermediate");
            gameMenu.add(inter);
            JMenuItem expert = new JRadioButtonMenuItem("Expert");
            gameMenu.add(expert);
            
            setJMenuBar(menuBar);

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
    }
    
    private class StopWatch extends Counter {
        private final Timer timer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent ev) { incr(); }
        });
        
        public StopWatch() { super(3, 0); }
        public void reset() {
        	if (timer.isRunning()) timer.stop();
        	setValue(0);
        }
        public void start() { timer.start(); }
        public void stop() { timer.stop(); }
    }
    
    private class MineCounter extends Counter {
        public MineCounter() { super(3, board.getNumMines()); }
        public void numFlagged(int n) { setValue(board.getNumMines()-n); }
    }
    
    private class GameContent extends JPanel {
        private Dimension d = board.getPreferredSize();
        private int width, height;
        private Dimension td = stopWatch.getPreferredSize();
        private Dimension mcd = mineCounter.getPreferredSize();
        
        public GameContent() {
            super(null, false);
            width = d.width+20;
            height = d.height+64;
            setPreferredSize(new Dimension(width, height));
            add(board);
            board.setBounds(12, 55, d.width, d.height);
            
            add(stopWatch);
            stopWatch.setBounds(width-54, 16, td.width, td.height);
            add(mineCounter);
            mineCounter.setBounds(17, 16, mcd.width, mcd.height);
        }
        
        public void paintComponent(Graphics g) {
            g.setColor(gray);
            g.fillRect(0, 0, width, height);
            g.setColor(white);
            g.fillRect(0, 0, width, 3);
            g.fillRect(0, 0, 3, height);
            paintBorder(g, 9, 9, d.width+6, 37, 2, false);
            paintBorder(g, 9, 51, d.width+6, d.height+6, 3, false);
            paintBorder(g, width-55, 15, td.width+2, td.height+2, 1, false);
            paintBorder(g, 16, 15, mcd.width+2, mcd.height+2, 1, false);
        }

        private void paintBorder(Graphics g, int x, int y, int width,
                int height, int thickness, boolean raised) {
            g.setColor(raised ? white : darkGray);
            for (int i = 0; i < thickness; i++) {
                g.drawLine(x+i, y+i, x+width-2-i, y+i);
                g.drawLine(x+i, y+i, x+i, y+height-2-i);
            }
            g.setColor(raised ? darkGray : white);
            for (int i = 0; i < thickness; i++) {
                g.drawLine(x+width-1-i, y+1+i, x+width-1-i, y+height-1);
                g.drawLine(x+i+1, y+height-1-i, x+width-1, y+height-1-i);
            }
        }
    };
    
    private void run() {
    	board.reset();
    	TopLevel topLevel = new TopLevel();
    	topLevel.setupUI();
    	topLevel.setVisible(true);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
        	@Override public void run() { (new Bromines()).run(); }
        });
    }
}

