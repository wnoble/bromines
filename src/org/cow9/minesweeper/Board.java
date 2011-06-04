package org.cow9.minesweeper;

import static org.cow9.minesweeper.GameState.*;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cow9.util.Stack;
import org.cow9.util.ArrayStack;

public class Board extends Component implements MouseListener,
	MouseMotionListener {
	private GameState state = UNSTARTED;
    private int width, height, nMines, nFlagged, nUnopened, mouseX, mouseY;
    private boolean b1, b3, inCanceledSweep, withinBoard;
    private boolean[][] mine, flagged, opened;
    private Pos[][] pos;
    private Random rand = new Random();
    private List<Runnable> gameStateHooks = new ArrayList<Runnable>(),
    	flagHooks = new ArrayList<Runnable>();

    private final int cellSize = 16;
    private Color white = Color.WHITE, gray = new Color(0xc0, 0xc0, 0xc0),
    	darkGray = new Color(0x80, 0x80, 0x80);

    private ImgSet imgSet;

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
    				if (mine[mouseY][mouseX]) toggleFlag(mouseX, mouseY);
    				else open(mouseX, mouseY);
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
    	withinBoard = (getMousePosition() != null);
    	setBackground(gray);
    	addMouseListener(this);
    	addMouseMotionListener(this);
    	setupGame(height, width, nMines);
    }
    
    private void setupGame(int height, int width, int nMines) {
        this.height = height;
        this.width = width;
        this.nMines = nMines;

        nFlagged = 0;
        nUnopened = width*height - nMines;
        mine = new boolean[height][width];
        flagged = new boolean[height][width];
        opened = new boolean[height][width];
        pos = new Pos[height][width];
        for (int i = 0; i < height; i++)
        	for (int j = 0; j < width; j++)
        		pos[i][j] = new Pos(j, i);

        setPreferredSize(new Dimension(width*cellSize, height*cellSize));
    }
    
    private void placeMines(int openX, int openY) {
    	int n = width*height-1;
    	for (Pos x: getNeighbors(openX, openY)) {
    		n -= (1-x.x+x.x); // shut up javac I don't need x
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
    			if (!isAdjacent(openX, openY, x, y))
    				mine[y][x] = buf[i++];
    		}
    	}
    }
    
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
    		for (int i = 0; i < width; i++) {
    			for (int j = 0; j < height; j++) {
    				opened[j][i] = mine[j][i] = flagged[j][i] = false;
    			}
    		}
    		setNumFlagged(0);
			setState(UNSTARTED);
			repaint();
    	}
    }

    public void repaintCells(int x, int y, int cols, int rows) {
       repaint(x*cellSize, y*cellSize, cols*cellSize, rows*cellSize);
    }

    public void repaintSurrounding(int x, int y) {
        int x1 = Math.max(0, x-1);
        int y1 = Math.max(0, y-1);
        int x2 = Math.min(width-1, x+1);
        int y2 = Math.min(height-1, y+1);
        repaintCells(x1, y1, x2-x1+1, y2-y1+1);
    }

    public void paint(Graphics g) {
    	int s = cellSize;
		Rectangle b = g.getClip().getBounds();
		int bx = b.x, by = b.y;
		int bw = b.width, bh = b.height;
		int x1 = bx / s, y1 = by / s;
		int x2 = (bx + bw - 1) / s, y2 = (by + bh - 1) / s;

    	g.setColor(gray);
    	g.fillRect(bx, by, bw, bh);

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

		for (int x = x1; x <= x2; x++) {
			for (int y = y1; y <= y2; y++) {
				if (opened[y][x]) {
					paintDepressed(g, x, y);
					int n = nNeighborMines(x, y);
					if (mine[y][x]) paintMineFatal(g, x, y);
					else if (n > 0) paintNum(g, x, y, n);
				} else if (flagged[y][x]) {
					if (state == DEAD && !mine[y][x]) {
						paintDepressed(g, x, y);
						paintMineWrong(g, x, y);
					} else {
						paintFlag(g, x, y);
					}
				} else if (state == DEAD && mine[y][x]) {
					paintDepressed(g, x, y);
					paintMine(g, x, y);
				} else if (isCellDepressed(x, y)) {
					paintDepressed(g, x, y);
				}
			}
		}
    }

    private void paintFlag(Graphics g, int x, int y)
    	{imgSet.paintFlag(this, g, x*cellSize, y*cellSize);}
    private void paintMine(Graphics g, int x, int y)
        {imgSet.paintMine(this, g, x*cellSize, y*cellSize);}
    private void paintMineFatal(Graphics g, int x, int y)
        {imgSet.paintMineFatal(this, g, x*cellSize, y*cellSize);}
    private void paintMineWrong(Graphics g, int x, int y)
    	{imgSet.paintMineWrong(this, g, x*cellSize, y*cellSize);}
    private void paintNum(Graphics g, int x, int y, int n)
        {imgSet.paintNum(this, g, x*cellSize, y*cellSize, n);}

    public void paintDepressed(Graphics g, int x, int y) {
        g.setColor(darkGray);
        g.drawLine(x*cellSize, y*cellSize, x*cellSize, (y+1)*cellSize);
        g.drawLine(x*cellSize, y*cellSize, (x+1)*cellSize, y*cellSize);
        g.setColor(gray);
        g.fillRect(x*cellSize+1, y*cellSize+1, cellSize-1, cellSize-1);
    }

    private static class Pos {
    	public final int x, y;
    	public Pos(int x, int y) {this.x = x; this.y = y;}
    }
    
    private Iterable<Pos> getNeighbors(int x, int y) {
    	ArrayList<Pos> ls = new ArrayList<Pos>(8);
    	int i = 0;
    	for (;;) {
    		try {
    			switch (i) {
    			case 0: ls.add(pos[y-1][x-1]); i++;
    			case 1: ls.add(pos[y-1][x]); i++;
    			case 2: ls.add(pos[y-1][x+1]); i++;
    			case 3: ls.add(pos[y][x-1]); i++;
    			case 4: ls.add(pos[y][x+1]); i++;
    			case 5: ls.add(pos[y+1][x-1]); i++;
    			case 6: ls.add(pos[y+1][x]); i++;
    			case 7: ls.add(pos[y+1][x+1]);
    			default: return ls;
    			}
    		} catch (IndexOutOfBoundsException e) {
    			i++;
    			continue;
    		}
    	}
    }

    public void open(int x, int y) {open(x, y, false);}

    private void open(int x, int y, boolean isSweep) {
    	if (state == UNSTARTED) {
        	placeMines(x, y);
        	setState(ALIVE);
        }
        
        Stack<Pos> stack = new ArrayStack<Pos>() {
        	public void push(Pos p) {
        		if (!opened[p.y][p.x] && !flagged[p.y][p.x])
        			super.push(p);
        	}
        };
        if (isSweep) for (Pos p: getNeighbors(x, y)) stack.push(pos[p.y][p.x]);
        else stack.push(pos[y][x]);
        
        if (stack.isEmpty()) return;

        int minx = width-1, miny = height-1, maxx = 0, maxy = 0;
        
        while (!stack.isEmpty()) {
        	Pos p = stack.pop();
        	
        	if (opened[p.y][p.x]) continue;
    		opened[p.y][p.x] = true;
    		
    		if (mine[p.y][p.x]) {
    			setState(DEAD);
    			repaint();
    			return;
    		} else if (--nUnopened == 0) {
    			setState(CLEARED);
    	        for (int i = 0; i < width; i++)
    	            for (int j = 0; j < height; j++)
    	            	flagged[j][i] = (flagged[j][i] || mine[j][i]);
    	        repaint();
    	        return;
    		}    		
    		minx = Math.min(minx, p.x);
    		miny = Math.min(miny, p.y);
    		maxx = Math.max(maxx, p.x);
    		maxy = Math.max(maxy, p.y);
    		
    		if (nNeighborMines(p.x, p.y) == 0)
    			for (Pos n: getNeighbors(p.x, p.y)) stack.push(n); 
        }
        
        repaintCells(minx, miny, maxx-minx+1, maxy-miny+1);
    }

    public void sweepClick(int x, int y) {
        if (opened[y][x] && nNeighborMines(x, y) == nNeighborFlags(x, y))
        	open(x, y, true);
        else
        	repaintSurrounding(x, y);
    }

    public void toggleFlag(int x, int y) {
    	if (state == UNSTARTED) {
    		placeMines(x, y);
    		setState(ALIVE);
    	}

        if (!opened[y][x]) {
	        if (flagged[y][x]) {
	        	flagged[y][x] = false;
	        	setNumFlagged(nFlagged-1);
	        } else {
	        	flagged[y][x] = true;
	        	setNumFlagged(nFlagged+1);
	        }
	        repaintCells(x, y, 1, 1);
        }
    }

    public boolean isAdjacent(int x1, int y1, int x2, int y2)
        {return (Math.abs(x1-x2) <= 1 && Math.abs(y1-y2) <= 1);}

    public int nNeighborMines(int x, int y) {
        int n = 0;
        for (Pos p: getNeighbors(x, y)) if (mine[p.y][p.x]) n++;
        return n;
    }

    private int nNeighborFlags(int x, int y) {
        int n = 0;
        for (Pos p: getNeighbors(x, y)) if (flagged[p.y][p.x]) n++;
        return n;
    }

	private void setPos(MouseEvent e) {
	    mouseX = (int)(e.getX() / cellSize);
	    mouseY = (int)(e.getY() / cellSize);
	}
	private boolean inSweepMode()
	    {return b1 && b3 && withinBoard && !inCanceledSweep;}
	public boolean isCellDepressed(int x, int y) {
	    return b1 && !inCanceledSweep && withinBoard &&
		((x == mouseX && y == mouseY) || (b3 && isAdjacent(x, y, mouseX, mouseY)));
	}

	public void mousePressed(MouseEvent e) {
	    int b = e.getButton();
	    if (b == MouseEvent.BUTTON1) b1 = true;
	    else if (b == MouseEvent.BUTTON2) b1 = b3 = true;
	    else if (b == MouseEvent.BUTTON3) b3 = true;
	    
	    if (state == DEAD || state == CLEARED) return;
	    
	    detector.stop();

	    inCanceledSweep = false;
	    if (b1 && b3) repaintSurrounding(mouseX, mouseY);
	    else if (b == MouseEvent.BUTTON1) repaintCells(mouseX, mouseY, 1, 1);
	    else if (b == MouseEvent.BUTTON3) toggleFlag(mouseX, mouseY);
	}

	public void mouseReleased(MouseEvent e) {
	    boolean sweeping = inSweepMode();
	    boolean oldB1 = b1;
	    int b = e.getButton();
	    b1 = (b1 && b != MouseEvent.BUTTON1 && b != MouseEvent.BUTTON3);
	    b3 = (b3 && b != MouseEvent.BUTTON2 && b != MouseEvent.BUTTON3);
	    
	    if (state == DEAD || state == CLEARED) return;

	    inCanceledSweep = (sweeping && (b1 || b3));
	    if (!withinBoard) return;

	    if (sweeping)
	    	sweepClick(mouseX, mouseY);
	    else if (oldB1 && b == MouseEvent.BUTTON1 && !inCanceledSweep)
	    	open(mouseX, mouseY);
	}

	public void mouseDragged(MouseEvent e) {
	    if (!withinBoard) return;

	    int oldx = mouseX, oldy = mouseY;
	    setPos(e);
	    
	    if (state == DEAD || state == CLEARED) return;

	    if (state == ALIVE && !(mouseX == oldx && mouseY == oldy)) {
	    	int minx = Math.min(mouseX, oldx);
	    	int miny = Math.min(mouseY, oldy);
	    	int maxx = Math.max(mouseX, oldx);
	    	int maxy = Math.max(mouseY, oldy);

	    	minx = Math.max(minx-1, 0);
	    	miny = Math.max(miny-1, 0);
	    	maxx = Math.min(maxx+1, width);
	    	maxy = Math.min(maxy+1, height);
	    	
	    	repaintCells(minx, miny, maxx-minx+1, maxy-miny+1);
	    }
	}

	public void mouseMoved(MouseEvent e) {
		int oldx = mouseX, oldy = mouseY;
		setPos(e);
		if (state == ALIVE && (oldx != mouseX || oldy != mouseY)) {
			if (!opened[mouseY][mouseX] && !flagged[mouseY][mouseX])
				detector.start();
			else detector.stop();
		}
	}
	public void mouseEntered(MouseEvent e) {
	    setPos(e);
	    withinBoard = true;
	    if (state == ALIVE) {
	    	if (b1)
	    		repaintSurrounding(mouseX, mouseY);
	    	else if (!opened[mouseY][mouseX] && !flagged[mouseY][mouseX])
	    		detector.start();
	    }
	    if (state == ALIVE && b1) repaintSurrounding(mouseX, mouseY);
	}
	public void mouseExited(MouseEvent e) {
	    withinBoard = false;
	    detector.stop();
	    if (state == ALIVE && b1) repaintSurrounding(mouseX, mouseY);
	}
	public void mouseClicked(MouseEvent e) {}
}
