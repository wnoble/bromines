package org.cow9.minesweeper;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.SwingUtilities;

public abstract class MouseHandler {
	private final boolean[] b = new boolean[3];
	private Handler cur;
	private final MouseObserver observer;
	public MouseHandler(MouseObserver observer) {
		cur = up;
		this.observer = observer;
	}
	abstract public void setListeners(MouseListener l, MouseMotionListener m);
	public void disconnect() { cur = doNothing; }
	public void reconnect() {
		assert cur == doNothing;
		cur = up;
	}
	private boolean any() { return b[0] || b[1] || b[2]; }
	
	private class Handler {
		public void moved(int x, int y) { observer.move(x, y); }
		public void pressed(int n) {}
		public void released(int n) {}
	}
	
	private int buttonNumber(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) return 1;
		else if (SwingUtilities.isMiddleMouseButton(e)) return 2;
		else if (SwingUtilities.isRightMouseButton(e)) return 3;
		return -1;
	}
	
	public void install() {
		setListeners(new MouseListener() {
			@Override public void mouseClicked(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {
				cur.moved(e.getX(), e.getY());
			}
			@Override public void mouseExited(MouseEvent e) {
				observer.exit();
			}
			@Override public void mousePressed(MouseEvent e) {
				b[buttonNumber(e)-1] = true;
				cur.pressed(buttonNumber(e));
			}
			@Override public void mouseReleased(MouseEvent e) {
				b[buttonNumber(e)-1] = false;
				cur.released(buttonNumber(e));
			}
		}, new MouseMotionListener() {
			@Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }
			@Override public void mouseMoved(MouseEvent e) { cur.moved(e.getX(), e.getY()); }
		});
	}
	
	private Handler doNothing = new Handler() {
		@Override public void moved(int x, int y) {}
	};
	
	private Handler up = new Handler() {
		@Override public void pressed(int n) {
			switch (n) {
			case 1: cur = selecting; observer.startSelect(); break;
			case 2: cur = sweeping; observer.startSweep(); break;
			case 3: cur = postSweep; observer.rightClick(); break;
			}
		}
	};
	
	private Handler selecting = new Handler() {
		@Override public void pressed(int n) {
			if (n == 2 || n == 3) {
				cur = sweeping;
				observer.startSweep();
			}
		}
		@Override public void released(int n) {
			if (n == 1) {
				cur = up;
				observer.click();
			}
		}
	};
	
	private Handler sweeping = new Handler() {
		@Override public void released(int n) {
			cur = any() ? postSweep : up;
			observer.sweepClick();
		}
	};
	
	private Handler postSweep = new Handler() {
		@Override public void pressed(int n) {
			cur = sweeping;
			observer.startSweep();
		}
		@Override public void released(int n) {
			if (!any()) cur = up;
		}
	};
}
