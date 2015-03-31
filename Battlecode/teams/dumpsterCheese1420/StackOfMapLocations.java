package dumpsterCheese1420;

import battlecode.common.MapLocation;

public class StackOfMapLocations {
	private MapLocation[] list;
	private int top = 0;
	
	public StackOfMapLocations() {
		list = new MapLocation[123456];
	}
	
	public MapLocation pop() {
		MapLocation result = list[top-1];
		top--;
		return result;
	}
	
	public MapLocation peek() {
		return list[top-1];
	}
	
	public void push(MapLocation obj) {
		list[top] = obj;
		top++;
	}
	
	public int size() {
		return top;
	}
}
