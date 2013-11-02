package org.cow9.minesweeper;

public interface GameObserver {
	void started();
	void restarted();
	void cleared();
	void died();
	void numFlagged(int n);
}
