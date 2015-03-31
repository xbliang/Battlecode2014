package dumpsterCheese420420;

import battlecode.common.MapLocation;

public class QueueOfMapLocations {
	private MapLocation[] list;
	private int head = 0;
	public int tail = 0;
	
	public QueueOfMapLocations() {
		list = new MapLocation[123456];
	}
	
	public MapLocation poll() {
		MapLocation result = list[head];
		head++;
		return result;
	}
	
	public void add(MapLocation obj) {
		list[tail] = obj;
		tail++;
	}
	
	public int size() {
		return tail - head;
	}
	
	public MapLocation get(int index) {
		return list[head + index];
	}
}