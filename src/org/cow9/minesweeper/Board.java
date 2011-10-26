package org.cow9.minesweeper;

import static org.cow9.minesweeper.GameState.*;

import java.util.Iterator;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Board extends Component {
	private GameState state = UNSTARTED;
    private int width, height, nMines, nFlagged, nUnopened;
    private boolean b1, b3, inCanceledSweep;
    private Cell[][] cells;
    private Cell cur;
    private Random rand = new Random();
    private List<Runnable> gameStateHooks = new ArrayList<Runnable>(),
    	flagHooks = new ArrayList<Runnable>();

    private final int cellSize = 16;
    private Color white = Color.WHITE, gray = new Color(0xc0, 0xc0, 0xc0),
    	darkGray = new Color(0x80, 0x80, 0x80);

    private ImgSet imgSet;

    // Non-reentrant iterator for neighboring cells.
    private class NeighborIterable implements Iterable<Cell> {
    	public int rem;
    	public final Cell[] buf = new Cell[8];
    	private Iterator<Cell> it = new Iterator<Cell>() {
    		public boolean hasNext() {return rem > 0;}
    		public Cell next() {return buf[--rem];}
    		public void remove() {}
    	};
    	public Iterator<Cell> iterator() {return it;}
    }
    
    private final NeighborIterable neighborIterable = new NeighborIterable();
    
    private class Cell {
    	final byte x, y;
    	byte flags = 0;
    	public Cell(byte x, byte y) {
    		this.x = x;
    		this.y = y;
    	}
    	
    	public void init() {flags = 0;}

    	public boolean isMine() {return (flags & 1) != 0;}
    	public void setMine(boolean value) {
    		if (value) flags |= 1;
    		else flags -= (flags & 1);
    	}
   	 
    	public boolean isFlagged() {return (flags & 2) != 0;}
    	public void toggleFlag() {
    		if (state == UNSTARTED) {
    			placeMines();
    			setState(ALIVE);
    		}
    		if (!isOpened()) {
    			setNumFlagged(nFlagged + 1 - (flags & 2));
    			flags ^= 2;
    			repaintSurrounding();
    		}
    	}
   	 
    	public boolean isOpened() {return (flags & 4) != 0;}
    	public void setOpened(boolean value) {
    		if (value) flags |= 4;
    		else flags -= (flags & 4);
    	}
    	
    	public Iterable<Cell> getNeighbors() {
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
    	
    	private int nNeighborMines() {
    		int n = 0;
    		for (Cell c: getNeighbors()) if (c.isMine()) n++;
    		return n;
    	}
    	
    	private int nNeighborFlags() {
    		int n = 0;
    		for (Cell c: getNeighbors()) if (c.isFlagged()) n++;
    		return n;
    	}
    	
        private boolean isAdjacent(Cell c)
        	{return (Math.abs(x-c.x) <= 1 && Math.abs(y-c.y) <= 1);}
    	
    	public void open() {open(false);}
        private void open(boolean sweep) {
    		if (state == UNSTARTED) {
    			placeMines();
    			setState(ALIVE);
    		}
    		
    		if (sweep) for (Cell c: getNeighbors()) openStack.push(c);
    		else openStack.push(this);
    		
    		int minx = width-1, miny = height-1, maxx = 0, maxy = 0;
    		
    		while (!openStack.isEmpty()) {
    			Cell c = openStack.pop();
    			if (c.isOpened()) continue;
    			c.setOpened(true);
    			if (c.isMine()) {
    				setState(DEAD);
    				repaint();
    				return;
    			} else if (--nUnopened == 0) {
    				setState(CLEARED);
    				repaint();
    				return;
    			}
    			minx = Math.min(minx, c.x);
    			miny = Math.min(miny, c.y);
    			maxx = Math.max(maxx, c.x);
    			maxy = Math.max(maxy, c.y);
    			
    			if (c.nNeighborMines() == 0)
    				for (Cell n: c.getNeighbors()) openStack.push(n);
    		}

    		if (maxx == 0) repaintSurrounding();
    		else repaintCells(minx, miny, maxx-minx+1, maxy-miny+1);
        }
        
    	public void repaintSurrounding() {repaintBounding(this);}
    	
    	public void repaintBounding(Cell other) {
    		int x1 = Math.max(0, Math.min(x, other.x)-1);
    		int y1 = Math.max(0, Math.min(y, other.y)-1);
    		int x2 = Math.min(width-1, Math.max(x, other.x)+1);
    		int y2 = Math.min(height-1, Math.max(y, other.y)+1);
    		repaintCells(x1, y1, x2-x1+1, y2-y1+1);
    	}
    	
    	public void sweepClick() {
    		if (state == UNSTARTED) repaintSurrounding();
    		else if (state == ALIVE) {
	    		if (isOpened() &&
	    				(nNeighborMines() == nNeighborFlags()))
	    			open(true);
	    		else repaintSurrounding();
    		}
    	}
    	
    	public void enter() {
    		if (cur != this) {
    			if (state == UNSTARTED && b1) repaintBounding(cur);
    			else if (state == ALIVE) {
    				if (b1) repaintBounding(cur);
    				else if (!isOpened() && !isFlagged()) detector.start();
    				else detector.stop();
    			}
    			cur = this;
    		}
    	}
    	
    	public void paint(Graphics g) {
    		int px = x*cellSize, py = y*cellSize;
    		boolean selected = (b1 && !inCanceledSweep &&
    				(this == cur || (b3 && isAdjacent(cur))));
    		
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
    			if (state == DEAD) {
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
    			int n = nNeighborMines();
    			if (n > 0) imgSet.paintNum(Board.this, g, px, py, n);
    			break;
    		case 5: /* mine && opened */
    			paintDepression(g);
    			imgSet.paintMineFatal(Board.this, g, px, py);
    			break;
    		// 6 and 7 don't exist because you can't open a flagged cell
    		}
    	}

    	public void paintDepression(Graphics g) {
            g.setColor(darkGray);
            g.drawLine(x*cellSize, y*cellSize, x*cellSize, (y+1)*cellSize);
            g.drawLine(x*cellSize, y*cellSize, (x+1)*cellSize, y*cellSize);
            g.setColor(gray);
            g.fillRect(x*cellSize+1, y*cellSize+1, cellSize-1, cellSize-1);
    	}
    }
    
    private Cell nullCell = new Cell((byte)-10, (byte)-10) {
    	public void enter() {
    		detector.stop();
    		if ((state == UNSTARTED || state == ALIVE) && b1)
    			cur.repaintSurrounding();
    		cur = nullCell;
    	}
    	public void sweepClick() {}
    	public void open() {}
    };

    private class Detector implements BoundedRangeModel {
    	private final int sweeperDelay = 200;
    	private int value = 0;
    	private List<ChangeListener> listeners =
    		new ArrayList<ChangeListener>();
    	private final ChangeEvent ev = new ChangeEvent(this);
    	
    	private final Timer timer = new Timer(20, new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			value = (value + 1) % sweeperDelay;
    			if (value == 0) {
    				if (cur.isMine()) cur.toggleFlag();
    				else cur.open();
    				timer.stop();
    			}
    			broadcast();
    		}
    	});
    	
    	private void broadcast() {
    		for (ChangeListener lis: listeners)
    			lis.stateChanged(ev);
    	}
    	
    	public int getValue() {return value;}
    	public int getMinimum() {return 0;}
    	public int getMaximum() {return sweeperDelay;}
    	public int getExtent() {return 0;}
    	public void addChangeListener(ChangeListener lis) {listeners.add(lis);}
    	public void removeChangeListener(ChangeListener lis) {listeners.remove(lis);}
    	public boolean getValueIsAdjusting() {return value == 0;}
    	public void setValue(int value) {}
    	public void setExtent(int extent) {}
    	public void setMinimum(int min) {}
    	public void setMaximum(int max) {}
    	public void setRangeProperties(int v, int e, int min, int max, boolean adj) {}
    	public void setValueIsAdjusting(boolean adj) {}
    	
    	public void start() {
    		if (timer.isRunning()) {
    			value = 0;
    			timer.restart();
    			broadcast();
    		} else {
    			timer.start();
    		}
    	}
		public void stop() {
			if (timer.isRunning()) {
				value = 0;
				timer.stop();
				broadcast();
			}
		}
    }
    
    private final Detector detector = new Detector();
	public BoundedRangeModel getDetectorModel() {return detector;}

	public GameState getState() {return state;}
    public int getNumMines() {return nMines;}
    public int getNumFlagged() {return nFlagged;}
    public ImgSet getImgSet() {return imgSet;}
    
    private Cell getCellFromPoint(int x, int y) {
    	try {
    		return cells[(int)(y/cellSize)][(int)(x/cellSize)];
    	} catch (ArrayIndexOutOfBoundsException e) {
    		return nullCell;
    	}
    }
    
    public void addGameStateHook(Runnable r) {
    	assert state == UNSTARTED;
    	gameStateHooks.add(r);
    }
    public void addFlagHook(Runnable r) {
    	assert state == UNSTARTED;
    	flagHooks.add(r);
    }
    
    public Board(ImgSet imgSet, int height, int width, int nMines) {
    	this.imgSet = imgSet;
    	b1 = b3 = inCanceledSweep = false;
    	Point p = getMousePosition();
    	cur = (p == null) ? nullCell : getCellFromPoint(p.x, p.y);
    	MouseHandler mouse = new MouseHandler();
    	addMouseListener(mouse);
    	addMouseMotionListener(mouse);
    	setupGame(height, width, nMines);
    }

    private void setupGame(int height, int width, int nMines) {
        this.height = height;
        this.width = width;
        this.nMines = nMines;

        nFlagged = 0;
        nUnopened = width*height - nMines;
        cells = new Cell[height][width];
        openStack = new OpenStack();
        for (byte i = 0; i < height; i++)
        	for (byte j = 0; j < width; j++)
        		cells[i][j] = new Cell(j, i);

        setPreferredSize(new Dimension(width*cellSize, height*cellSize));
    }
    
    private void placeMines() {
    	// Count number of squares eligible for mining.
    	int n = width*height-1;
    	for (Cell x: cur.getNeighbors()) {
    		n = n - 1 + (x.x-x.x); // shut up javac I don't need x
    	}

    	// Create a n-sized boolean buffer, set the first nMines slots to
    	// true, and shuffle entire array.
    	boolean[] buf = new boolean[n];
    	for (int i = 0; i < nMines; i++) buf[i] = true;
    	for (int i = n-1; i >= 1; i--) {
    		int j = rand.nextInt(i+1);
    		boolean tmp = buf[i];
    		buf[i] = buf[j];
    		buf[j] = tmp;
    	}
    	
    	// Copy the mine arrangement to the board, skipping the click radius.
    	for (int y = 0, i = 0; y < height; y++) {
    		for (int x = 0; x < width; x++) {
    			Cell c = cells[y][x];
    			if (!c.isAdjacent(cur))
    				c.setMine(buf[i++]);
    		}
    	}
    }

    // To-visit stack implementation based on the no-initialization set.
    private class OpenStack {
    	private int[][] position = new int[height][width];
    	private Cell[] buf = new Cell[width*height];
    	private int i = 0;
		public boolean isEmpty() {return i == 0;}
		public Cell pop() {return buf[--i];}
		private boolean contains(Cell c) {
			int pos = position[c.y][c.x];
			return pos < i && buf[pos] == c;
		}
		public void push(Cell c) {
			if (!c.isOpened() && !c.isFlagged() && !contains(c)) {
				buf[i] = c;
				position[c.y][c.x] = i;
				i++;
			}
		}
    };
    
    private OpenStack openStack;
    
    private void setState(GameState state) {
    	this.state = state;
    	for (Runnable r: gameStateHooks) r.run();
    }
    private void setNumFlagged(int n) {
    	nFlagged = n;
    	for (Runnable r: flagHooks) r.run();
    }
    
    public void restart() {
    	if (state != UNSTARTED) {
    		nUnopened = width*height-nMines;
    		for (int i = 0; i < width; i++)
    			for (int j = 0; j < height; j++)
    				cells[j][i].init();
    		setNumFlagged(0);
			setState(UNSTARTED);
			repaint();
    	}
    }

    public void repaintCells(int x, int y, int cols, int rows) {
       repaint(x*cellSize, y*cellSize, cols*cellSize, rows*cellSize);
    }

    public void paint(Graphics g) {
    	int s = cellSize;
		Rectangle b = g.getClip().getBounds();
		int bx = b.x, by = b.y;
		int bw = b.width, bh = b.height;
		int x1 = bx / s, y1 = by / s;
		int x2 = (bx + bw - 1) / s, y2 = (by + bh - 1) / s;

//    	g.setColor(gray);
//    	g.fillRect(bx, by, bw, bh);

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

    private class MouseHandler implements MouseListener,
    	MouseMotionListener {
		public void mousePressed(MouseEvent e) {
		    int b = e.getButton();
		    if (b == MouseEvent.BUTTON1) b1 = true;
		    else if (b == MouseEvent.BUTTON2) b1 = b3 = true;
		    else if (b == MouseEvent.BUTTON3) b3 = true;
	
		    if (state == ALIVE || state == UNSTARTED) {
		    	detector.stop();
		    	inCanceledSweep = false;
		    	if (b == MouseEvent.BUTTON3 && !b1) cur.toggleFlag();
		    	else cur.repaintSurrounding();
		    }
		}
	
		public void mouseReleased(MouseEvent e) {
		    boolean sweeping = ((state == UNSTARTED || state == ALIVE) &&
		    		b1 && b3 && !inCanceledSweep);
		    boolean oldB1 = b1;
		    int b = e.getButton();
		    b1 = (b1 && b != MouseEvent.BUTTON1 && b != MouseEvent.BUTTON2);
		    b3 = (b3 && b != MouseEvent.BUTTON3 && b != MouseEvent.BUTTON2);
		    
		    if (state == ALIVE || state == UNSTARTED) {
			    if (sweeping) cur.sweepClick();
			    else if (oldB1 && b == MouseEvent.BUTTON1 && !inCanceledSweep)
			    	cur.open();
			    else cur.repaintSurrounding();
			    inCanceledSweep = (sweeping && (b1 || b3));
		    }
		}
	
		public void mouseMoved(MouseEvent e)
		    {getCellFromPoint(e.getX(), e.getY()).enter();}
		public void mouseEntered(MouseEvent e) {mouseMoved(e);}
		public void mouseDragged(MouseEvent e) {mouseMoved(e);}
		public void mouseExited(MouseEvent e) {nullCell.enter();}
		public void mouseClicked(MouseEvent e) {}
    }
}
