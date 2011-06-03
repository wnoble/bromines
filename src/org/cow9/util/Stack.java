package org.cow9.util;

public interface Stack<E> extends Iterable<E> {
	boolean isEmpty();
	E peek();
	E pop();
	void push(E item);
}
