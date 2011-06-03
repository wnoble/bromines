package org.cow9.minesweeper;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;

import javax.swing.BoundedRangeModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TopPanel extends JPanel {
	private final Board board;
	
	private class Status extends JLabel {{
		board.addGameStateHook(new Runnable() {
			public void run() {
				switch (board.getState()) {
				case UNSTARTED: setText(""); break;
				case DEAD: setText("Died"); break;
				case CLEARED: setText("Cleared"); break;
				}
			}
		});
	}}
	
	private class StopWatch extends JLabel {
		private BigInteger seconds = BigInteger.ZERO;
		private int deciseconds;
		private final Timer timer = new Timer(100, new ActionListener() {
			public void actionPerformed(ActionEvent ev) {tick();}
		});
		
		public StopWatch() {
			super("0.0");
			board.addGameStateHook(new Runnable() {public void run() {update();}});
		}

		private void update() {
			switch (board.getState()) {
			case UNSTARTED:
				if (timer.isRunning()) timer.stop();
				seconds = BigInteger.ZERO;
				deciseconds = 0;
				setText("0.0");
				break;
			case ALIVE:
				timer.start();
				break;
			default:
				timer.stop();
			}
		}
		
		private void tick() {
			if (deciseconds == 9) {
				seconds = seconds.add(BigInteger.ONE);
				deciseconds = 0;
			} else deciseconds++;
			setText(seconds + "." + deciseconds);
		}
	}
	
	private class MineCounter extends JLabel {
		public MineCounter() {
			super(board.getNumMines() + "/" + board.getNumMines(),
					new Icon() {
						public int getIconHeight() {return 16;}
						public int getIconWidth() {return 16;}
						public void paintIcon(Component c, Graphics g, int x,
								int y) {
							board.getImgSet().paintMine(c, g, x, y);
						}
				
			}, SwingConstants.CENTER);
			board.addFlagHook(new Runnable() {
				public void run() {
					int nMines = board.getNumMines();
					setText((nMines - board.getNumFlagged()) + "/" + nMines);
				}
			});
		}
	}
	
	private class SweeperProgress extends JPanel {
		private final JProgressBar bar;
		private boolean showing = false;
		
		public SweeperProgress() {
			BoundedRangeModel model = board.getDetectorModel();
			model.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					int d = bar.getValue()*3 - bar.getMaximum();
					if (showing && d < 0) {
						remove(bar);
						validate();
						repaint();
						showing = false;
					} else if (!showing && d > 0) {
						add(bar);
						showing = true;
					}
				}
			});
			
			bar = new JProgressBar(model);
			bar.setStringPainted(false);
			setPreferredSize(bar.getPreferredSize());
		}
	}
	
	public TopPanel(Board board) {
		this.board = board;
		add(new Status());
		add(new StopWatch());
		add(new MineCounter());
		add(new SweeperProgress());
	}
}
