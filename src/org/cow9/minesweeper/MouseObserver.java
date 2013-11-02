package org.cow9.minesweeper;

public interface MouseObserver {
	void exit();
	void startSelect();
	void startSweep();
	void click();
	void sweepClick();
	void rightClick();
	void move(int x, int y);
	void selectDrag(int x, int y);
	void sweepDrag(int x, int y);
}
