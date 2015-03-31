package dumpsterCheese420420;

import battlecode.common.Direction;

public class QueueOfDirections {
	private Direction[] list;
	private int head = 0;
	private int tail = 0;
	
	public QueueOfDirections() {
		list = new Direction[123456];
	}
	
	public Direction poll() {
		Direction result = list[head];
		head++;
		return result;
	}
	
	public void add(Direction obj) {
		list[tail] = obj;
		tail++;
	}
	
	public int size() {
		return tail - head;
	}
}
