package org.cow9.util;

import java.util.ArrayDeque;
import java.util.Iterator;

public class ArrayStack<E> implements Stack<E> {
	private ArrayDeque<E> q = new ArrayDeque<E>();
	public boolean isEmpty() {return q.isEmpty();}
	public E peek() {return q.peekFirst();}
	public E pop() {return q.pop();}
	public void push(E item) {q.addFirst(item);}
	public Iterator<E> iterator() {return q.iterator();}
}
