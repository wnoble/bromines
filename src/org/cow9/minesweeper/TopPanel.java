package org.cow9.minesweeper;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
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

	private class StopWatch extends Counter {
		private final Timer timer = new Timer(1000, new ActionListener() {
			public void actionPerformed(ActionEvent ev) {incr();}
		});
		
		public StopWatch() {
			super(3, 0);
			board.addGameStateHook(new Runnable() {public void run() {update();}});
		}

		private void update() {
			switch (board.getState()) {
			case UNSTARTED:
				if (timer.isRunning()) timer.stop();
				setValue(0);
				break;
			case ALIVE:
				timer.start();
				break;
			default:
				timer.stop();
			}
		}
	}
	
	private class MineCounter extends Counter {
		public MineCounter() {
			super(3, board.getNumMines());
			board.addFlagHook(new Runnable() {
				public void run() {
					setValue(board.getNumMines()-board.getNumFlagged());
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
		add(new JButton("Restart") {{
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					TopPanel.this.board.restart();
				}
			});
		}});
		add(new Status());
		add(new StopWatch());
		add(new MineCounter());
		add(new SweeperProgress());
	}
}
