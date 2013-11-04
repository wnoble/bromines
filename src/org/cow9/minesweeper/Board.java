package org.cow9.minesweeper;

import java.util.Iterator;
import java.util.Random;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.*;

import javax.swing.*;

public class Board extends Component {
	private enum GameState { UNSTARTED, ALIVE, DEAD, CLEARED }
	private enum MouseState { UP, SELECTING, SWEEPING }
    private GameState state = GameState.UNSTARTED;
    private MouseState mouseState = MouseState.UP;
    private final int cellSize = 16;
    private final Color white = Color.WHITE, gray = new Color(0xc0, 0xc0, 0xc0),
        darkGray = new Color(0x80, 0x80, 0x80);
    private final int width, height, numMines;
    private int numFlagged, numUnopened;
    private boolean placedMines;
    private Cell cur;
    
    private final Cell[][] cells;
    private Random rand = new Random();
    private final GameObserver gameObserver;
    private final ImgSet imgSet;
    private final OpenStack openStack;
    private final Detector detector = new Detector() {
    	public void fire() { cur.reveal(); }
    };
    private final NeighborIterable neighborIterable = new NeighborIterable();
    private final MouseHandler mouse;
    private final MouseObserver mouseObserver = new MouseObserver() {
		@Override public void exit() { nullCell.enter(); }
		@Override public void startSelect() { mouseState = MouseState.SELECTING; cur.repaint(); }
		@Override public void startSweep() { mouseState = MouseState.SWEEPING; cur.repaintSurrounding(); }
		@Override public void click() { mouseState = MouseState.UP; cur.click(); }
		@Override public void sweepClick() { mouseState = MouseState.UP; cur.sweepClick(); }
		@Override public void rightClick() { mouseState = MouseState.UP; cur.rightClick(); }
		@Override public void move(int x, int y) { getCellFromPoint(x, y).enter(); }
    };
    private Cell nullCell = new Cell((byte)-1, (byte)-1) {
    	@Override public boolean isOpened() { return true; }
        @Override public void sweepClick() {}
        @Override public void click() {}
        @Override public void rightClick() {}
        @Override public void paint(Graphics g) {}
        @Override public void repaintSurrounding() {}
        @Override public void repaintBounding(Cell c) { c.repaintSurrounding(); }
    };

    public Board(ImgSet imgSet, int height, int width, int numMines, GameObserver gameObserver) {
    	this.imgSet = imgSet;
    	this.height = height;
    	this.width = width;
    	this.numMines = numMines;
		cells = new Cell[height][width];
		openStack = new OpenStack();
        mouse = new MouseHandler(mouseObserver) {
        	@Override public void setListeners(MouseListener l, MouseMotionListener m) {
        		addMouseListener(l);
        		addMouseMotionListener(m);
        	}
        };
        mouse.install();
        this.gameObserver = gameObserver; 
        for (byte i = 0; i < height; i++)
        	for (byte j = 0; j < width; j++)
        		cells[i][j] = new Cell(j, i);
        setPreferredSize(new Dimension(width*cellSize, height*cellSize));
    }
    
    // Non-reentrant iterator for neighboring cells.
    private class NeighborIterable implements Iterable<Cell> {
        public int rem;
        public final Cell[] buf = new Cell[8];
        private Iterator<Cell> it = new Iterator<Cell>() {
            public boolean hasNext() { return rem > 0; }
            public Cell next() { return buf[--rem]; }
            public void remove() {}
        };
        @Override public Iterator<Cell> iterator() { return it; }
    }
    
    private class Cell {
        public final byte x, y;
        public Cell(byte x, byte y) {
            this.x = x;
            this.y = y;
        }
        
        private byte flags;
        public boolean isMine() { return (flags & 1) != 0; }
        public void setMine() { flags |= 1; }
        public boolean isFlagged() {return (flags & 2) != 0;}
        private void toggleFlagged() { flags ^= 2; }
        public boolean isOpened() { return (flags & 4) != 0; }
        public void setOpened() { flags |= 4; }
        
        public void reset() { flags = 0; }
        
        private Iterable<Cell> getNeighbors() {
            int i = 0, j = 0;
            for (;;) {
                try {
                    switch (i) {
                    case 0: neighborIterable.buf[j] = cells[y+1][x+1]; i++; j++;
                    case 1: neighborIterable.buf[j] = cells[y+1][x]; i++; j++;
                    case 2: neighborIterable.buf[j] = cells[y+1][x-1]; i++; j++;
                    case 3: neighborIterable.buf[j] = cells[y][x+1]; i++; j++;
                    case 4: neighborIterable.buf[j] = cells[y][x-1]; i++; j++;
                    case 5: neighborIterable.buf[j] = cells[y-1][x+1]; i++; j++;
                    case 6: neighborIterable.buf[j] = cells[y-1][x]; i++; j++; 
                    case 7: neighborIterable.buf[j] = cells[y-1][x-1]; i++; j++;
                    default:
                        neighborIterable.rem = j;
                        return neighborIterable;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    i++;
                    continue;
                }
            }
        }
        
        private int numNeighborMines() {
            int n = 0;
            for (Cell c: getNeighbors()) if (c.isMine()) n++;
            return n;
        }
        
        private int numNeighborFlags() {
            int n = 0;
            for (Cell c: getNeighbors()) if (c.isFlagged()) n++;
            return n;
        }
        
        private boolean isAdjacent(Cell c)
            {return (Math.abs(x-c.x) <= 1 && Math.abs(y-c.y) <= 1);}
        
        public void reveal() {
            if (isMine()) rightClick(); else click();
        }
        public void click() { if (!isFlagged() && !isOpened()) open(); }
        public void sweepClick() {
        	if (isOpened() && numNeighborMines() == numNeighborFlags())
        		sweepOpen();
        	else repaintSurrounding();
        }
        public void rightClick() {
            if (state == GameState.UNSTARTED) start();
            if (!isOpened()) {
                setNumFlagged(numFlagged + 1 - (flags & 2));
                toggleFlagged();
                repaint();
            }
        }
        
        private void open() { openOrSweepOpen(false); }
        private void sweepOpen() { openOrSweepOpen(true); }
        public void maybeQueueOpen() {
        	if (!isOpened() && !isFlagged() && !openStack.contains(this)) {
        		openStack.push(this);
        	}
        }
        private void openOrSweepOpen(boolean sweep) {
        	if (state == GameState.UNSTARTED) {
        		if (!placedMines) placeMines(x, y);
        		start();
        	}
            
            openStack.clear();
            if (sweep) for (Cell c: getNeighbors()) c.maybeQueueOpen();
            else maybeQueueOpen();
            
            int minx = width-1, miny = height-1, maxx = 0, maxy = 0;
            
            while (!openStack.isEmpty()) {
                Cell c = openStack.pop();
                if (c.isOpened()) continue;
                c.setOpened();
                if (c.isMine()) { died(); return; }
                else if (--numUnopened == 0) { cleared(); return; }
                minx = Math.min(minx, c.x);
                miny = Math.min(miny, c.y);
                maxx = Math.max(maxx, c.x);
                maxy = Math.max(maxy, c.y);
                
                if (c.numNeighborMines() == 0)
                	for (Cell n: c.getNeighbors())
                		n.maybeQueueOpen();
            }

            if (maxx == 0) repaintSurrounding();
            else repaintCells(minx, miny, maxx-minx+1, maxy-miny+1);
        }
        
        private void repaint() { repaintCells(x, y, 1, 1); }
        
        public void repaintSurrounding() { repaintBounding(this); }
        
        public void repaintBounding(Cell other) {
            int x1 = Math.max(0, Math.min(x, other.x)-1);
            int y1 = Math.max(0, Math.min(y, other.y)-1);
            int x2 = Math.min(width-1, Math.max(x, other.x)+1);
            int y2 = Math.min(height-1, Math.max(y, other.y)+1);
            repaintCells(x1, y1, x2-x1+1, y2-y1+1);
        }
        
        public void enter() {
        	if (this != cur) {
        		Cell old = cur;
        		if (!isFlagged() && !isOpened()) detector.start();
        		else detector.stop();
        		cur = this;
        		repaintBounding(old);
        	}
        }
        
        public void paint(Graphics g) {
            int px = x*cellSize, py = y*cellSize;
            boolean selected =
            		(mouseState == MouseState.SELECTING && this == cur) ||
            		(mouseState == MouseState.SWEEPING && isAdjacent(cur));
            
            switch (flags) {
            case 0: /* !mine, !flagged, !open */
                if (selected) paintDepression(g);
                break;                
            case 1: /* mine, !flagged, !open */
                switch (state) {
                case UNSTARTED:
                case ALIVE:
                    if (selected) paintDepression(g);
                    break;
                case CLEARED:
                    imgSet.paintFlag(Board.this, g, px, py);
                    break;
                case DEAD:
                    paintDepression(g);
                    imgSet.paintMine(Board.this, g, px, py);
                    break;
                }
                break;
            case 2: /* Incorrectly flagged */
                if (state == GameState.DEAD) {
                    paintDepression(g);
                    imgSet.paintMineWrong(Board.this, g, px, py);
                } else {
                    imgSet.paintFlag(Board.this, g, px, py);
                }
                break;
            case 3: /* Correctly flagged */
                imgSet.paintFlag(Board.this, g, px, py);
                break;
            case 4: /* opened && !mine */
                paintDepression(g);
                int n = numNeighborMines();
                if (n > 0) imgSet.paintNum(Board.this, g, px, py, n);
                break;
            case 5: /* mine && opened */
                paintDepression(g);
                imgSet.paintMineFatal(Board.this, g, px, py);
                break;
            // 6 and 7 don't exist because you can't open a flagged cell
            }
        }

        private void paintDepression(Graphics g) {
            g.setColor(darkGray);
            g.drawLine(x*cellSize, y*cellSize, x*cellSize, (y+1)*cellSize);
            g.drawLine(x*cellSize, y*cellSize, (x+1)*cellSize, y*cellSize);
            g.setColor(gray);
            g.fillRect(x*cellSize+1, y*cellSize+1, cellSize-1, cellSize-1);
        }
    }
    
    public BoundedRangeModel getDetectorModel() { return detector; }

    public GameState getState() { return state; }
    public int getNumMines() { return numMines; }
    public int getNumFlagged() { return numFlagged; }
    public ImgSet getImgSet() { return imgSet; }
    
    private Cell getCellFromPoint(int x, int y) {
        try {
            return cells[(int)(y/cellSize)][(int)(x/cellSize)];
        } catch (ArrayIndexOutOfBoundsException e) {
            return nullCell;
        }
    }
    
    public void reset() {
        Point p = getMousePosition();
        cur = (p == null) ? nullCell : getCellFromPoint(p.x, p.y);
		numUnopened = width*height - numMines;
		for (byte i = 0; i < height; i++)
			for (byte j = 0; j < width; j++)
				cells[i][j].reset();
		placedMines = false;
		setNumFlagged(0);
		state = GameState.UNSTARTED;
    }
    
    private void placeMines(int x, int y) {
    	assert !placedMines;
        int numSkip = 9;
        if (x == 0 || x == width-1) numSkip -= 3;
        if (y == 0 || y == height-1) numSkip -= 3;
        if (numSkip == 3) numSkip++; // double-count
        
        boolean[] buf = new boolean[width*height - numSkip];
        
        // Set the first nMines slots to true and shuffle entire array.
        for (int i = 0; i < numMines; i++) buf[i] = true;
        for (int i = buf.length-1; i >= 1; i--) {
            int j = rand.nextInt(i+1);
            boolean tmp = buf[i];
            buf[i] = buf[j];
            buf[j] = tmp;
        }
        
        // Copy the mine arrangement to the board, skipping the click radius.
        for (int j = 0, i = 0; j < height; j++) {
        	boolean neary = j == y-1 || j == y || j == y+1;
        	for (int k = 0; k < width; k++) {
        		boolean nearx = k == x-1 || k == x || k == x+1;
        		if (!(neary && nearx) && buf[i++])
        			cells[j][k].setMine();
        	}
        }
        
        placedMines = true;
    }

    // To-visit stack implementation based on the no-initialization set.
    private class OpenStack {
        private int[][] position = new int[height][width];
        private Cell[] buf = new Cell[width*height];
        private int i;
        public void clear() { i = 0; }
        public boolean isEmpty() { return i == 0; }
        public Cell pop() { return buf[--i]; }
        public boolean contains(Cell c) {
            int pos = position[c.y][c.x];
            return pos < i && buf[pos] == c;
        }
        public void push(Cell c) {
        	buf[i] = c;
        	position[c.y][c.x] = i;
        	i++;
        }
    }
    
    private void endGame(boolean won) {
    	state = won ? GameState.CLEARED : GameState.DEAD;
    	if (won) gameObserver.cleared(); else gameObserver.died();
    	mouse.disconnect();
    	repaint();
    }
    private void cleared() { endGame(true); }
    private void died() { endGame(false); }
    private void start() { state = GameState.ALIVE; gameObserver.started(); }
    
    private void setNumFlagged(int n) {
        numFlagged = n;
        gameObserver.numFlagged(n);
    }
    
    public void restart() {
    	boolean wasAlive = state == GameState.ALIVE;
    	if (state != GameState.UNSTARTED) {
    		reset();
    		gameObserver.restarted();
    		if (!wasAlive) mouse.reconnect();
    		repaint();
    	}
    }

    public void repaintCells(int x, int y, int cols, int rows) {
       repaint(x*cellSize, y*cellSize, cols*cellSize, rows*cellSize);
    }

    @Override public void paint(Graphics g) {
        int s = cellSize;
        Rectangle b = g.getClip().getBounds();
        int bx = b.x, by = b.y;
        int bw = b.width, bh = b.height;
        int x1 = bx / s, y1 = by / s;
        int x2 = (bx + bw - 1) / s, y2 = (by + bh - 1) / s;

        g.setColor(white);
        for (int i = x1; i <= x2; i++)
            g.drawLine(i*s+1, 0, i*s+1, height*s-1);
        g.setColor(darkGray);
        for (int i = x1; i <= x2; i++)
            g.drawLine((i+1)*s-2, 0, (i+1)*s-2, height*s-1);

        g.setColor(white);
        for (int i = y1; i <= y2; i++)
            g.drawLine(0, i*s+1, width*s-1, i*s+1);
        g.setColor(darkGray);
        for (int i = y1; i <= y2; i++)
            g.drawLine(0, (i+1)*s-2, width*s-1, (i+1)*s-2);

        g.setColor(white);
        for (int i = x1; i <= x2; i++)
                g.drawLine(i*s, 0, i*s, height*s-1);
        g.setColor(darkGray);
        for (int i = x1; i <= x2; i++)
            g.drawLine((i+1)*s-1, 0, (i+1)*s-1, height*s-1);

        g.setColor(white);
        for (int i = y1; i <= y2; i++)
            g.drawLine(0, i*s, width*s-1, i*s);
        g.setColor(darkGray);
        for (int i = y1; i <= y2; i++)
            g.drawLine(0, (i+1)*s-1, width*s-1, (i+1)*s-1);

        /* Draw the diagonals */
        g.setColor(gray);
        for (int i = 0, n = width+height-1; i < n; i++) {
            boolean b1 = i < height, b2 = i < width;
            g.drawLine(
                    b1 ? 0 : (i-height+1)*s,
                    b1 ? (i+1)*s-1 : height*s-1,
                    b2 ? (i+1)*s-1 : width*s-1,
                    b2 ? 0 : (i-width+1)*s);
        }

      	for (int y = y1; y <= y2; y++)
       		for (int x = x1; x <= x2; x++)
       			cells[y][x].paint(g);
    }
}
