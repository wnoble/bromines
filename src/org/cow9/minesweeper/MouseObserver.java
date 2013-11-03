package org.cow9.minesweeper;

public interface MouseObserver {
	void exit();
	void startSelect();
	void startSweep();
	void click();
	void sweepClick();
	void rightClick();
	void move(int x, int y);
}
