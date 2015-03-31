package dumpsterCheese420.ntp;

import java.util.ArrayList;
import dumpsterCheese420.RobotPlayer;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class SpiralPatternReachable extends NoiseTowerPattern {
	ArrayList<MapLocation> fireOrder;
	RobotController tower;
	int shootPosition;
	double angle = 0;
	double resolution = 0;
	double range = 0;
	int maxRange = 0;
	double velocity = 8;
	MapLocation center;
	int mapHeight;
	int mapWidth;
	public double numRevolutions = 0;
	int searchRange = 150;

	// search stuff
	static Direction flow[][];
	static int shortestPathLength[][];
//	static Queue<MapLocation> q = new LinkedList<MapLocation>();
//	static Queue<Direction> f = new LinkedList<Direction>();
	static QueueOfDirections f = new QueueOfDirections();
	static QueueOfMapLocations q = new QueueOfMapLocations();
	static MapLocation current;
	static MapLocation previous;
	static Direction incoming;
	public static boolean doneSearching = false;
	Direction allDirections[] = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, 
			Direction.NORTH_WEST, Direction.NORTH_EAST, Direction.SOUTH_EAST, Direction.SOUTH_WEST};


	// Initializer which sets center to tower location
	public SpiralPatternReachable(RobotController t, int r) {
		tower = t;
		fireOrder = new ArrayList<MapLocation>();
		shootPosition = 0;
		center = t.getLocation();
		range = r;
		maxRange = r;
		resolution = 4;
		mapHeight = t.getMapHeight();
		mapWidth = t.getMapWidth();
		
		shortestPathLength = new int[t.getMapWidth()][t.getMapHeight()];
		shortestPathLength[center.x][center.y] = 0;
		flow = new Direction[t.getMapWidth()][t.getMapHeight()];
		flow[center.x][center.y] = Direction.NONE;
		q.add(center);
		f.add(Direction.NONE);

	}
	
	// Initializer which takes center as input
	public SpiralPatternReachable(RobotController t, int r, MapLocation center) {
		tower = t;
		fireOrder = new ArrayList<MapLocation>();
		shootPosition = 0;
		this.center = center;
		range = r;
		maxRange = r;
		resolution = 4;
		mapHeight = t.getMapHeight();
		mapWidth = t.getMapWidth();
		
		shortestPathLength = new int[t.getMapWidth()][t.getMapHeight()];
		shortestPathLength[center.x][center.y] = 0;
		flow = new Direction[t.getMapWidth()][t.getMapHeight()];
		flow[center.x][center.y] = Direction.NONE;
		q.add(center);
		f.add(Direction.NONE);

	}
	
	// Initializer which sets center to tower location
	public SpiralPatternReachable(RobotController t, int r, int vel) {
		tower = t;
		fireOrder = new ArrayList<MapLocation>();
		shootPosition = 0;
		center = t.getLocation();
		range = r;
		maxRange = r;
		resolution = 4;
		mapHeight = t.getMapHeight();
		mapWidth = t.getMapWidth();
		velocity = vel;
		
		shortestPathLength = new int[t.getMapWidth()][t.getMapHeight()];
		shortestPathLength[center.x][center.y] = 0;
		flow = new Direction[t.getMapWidth()][t.getMapHeight()];
		flow[center.x][center.y] = Direction.NONE;
		q.add(center);
		f.add(Direction.NONE);

	}
	
	// Initializer which takes center as input
	public SpiralPatternReachable(RobotController t, int r, MapLocation center, int vel) {
		tower = t;
		fireOrder = new ArrayList<MapLocation>();
		shootPosition = 0;
		this.center = center;
		range = r;
		maxRange = r;
		resolution = 4;
		mapHeight = t.getMapHeight();
		mapWidth = t.getMapWidth();
		velocity = vel;
		
		shortestPathLength = new int[t.getMapWidth()][t.getMapHeight()];
		shortestPathLength[center.x][center.y] = 0;
		flow = new Direction[t.getMapWidth()][t.getMapHeight()];
		flow[center.x][center.y] = Direction.NONE;
		q.add(center);
		f.add(Direction.NONE);

	}

	public void searchStep() {
		if (q.size() == 0) {
			doneSearching = true;
			return;
		}
		int i = Clock.getBytecodesLeft();
		current = q.poll();
		incoming = f.poll();
		previous = current.add(incoming.opposite()); 
		if (!shouldSearch(previous, current, searchRange)) {
			return;
		}
		flow[current.x][current.y] = incoming.opposite();
		shortestPathLength[current.x][current.y] = splAt(previous) + 1;
		
		for (Direction d : allDirections) {
			f.add(d);
			q.add(current.add(d));
		}
		System.out.println(i - Clock.getBytecodesLeft());
	}
	
	public boolean shouldSearch(MapLocation previous, MapLocation current, int range) {
		TerrainTile terrain = tower.senseTerrainTile(current);
		return (splAt(current) == 0 || splAt(current) > splAt(previous) + 1) 
				&& (terrain == TerrainTile.NORMAL || terrain == TerrainTile.ROAD)
				&& current.distanceSquaredTo(center) <= range 
				&& (!current.equals(center) || previous.equals(center));
	}
	
	public int splAt(MapLocation m) {
		if (onMap(m)){
			return shortestPathLength[m.x][m.y];
		}
		return -1;
	}
	
	public boolean onMap(MapLocation m) {
		int width = tower.getMapWidth();
		int height = tower.getMapHeight();
		return (m.x >= 0 && m.y >= 0 && m.x < width && m.y < height);
	}
	
	public boolean usefulShot(MapLocation p) {
		if(p.distanceSquaredTo(tower.getLocation()) > 300) {
			return false;
		}
		TerrainTile tile = tower.senseTerrainTile(p);
		if(doneSearching) {
			for (MapLocation l : MapLocation.getAllMapLocationsWithinRadiusSq(p, 4)){
				if (splAt(l) > 0) {
					return true;					
				}
			}
			return false;
		}
		// If not done searching
		if(tile != TerrainTile.OFF_MAP) {
			return true;
		} else {
			int x = 0;
			int y = 0;
			if(p.x < 0) {
				x = -p.x;
			} else if(p.x >= tower.getMapWidth()) {
				x = p.x-tower.getMapWidth()-1;
			}
			if(p.y < 0) {
				y = -p.y;
			} else if(p.y >= tower.getMapHeight()) {
				y = p.x-tower.getMapHeight()-1;
			}

			return x <= 2 || y <= 2;
		}
	}

	public void updateAim() {
		//v = range*dr
		double dr = velocity/range;
		angle = angle + dr;
		range -= 0.1;
		if(angle >= Math.PI*2) {
			angle = 0;
			if(range <= 8) {
				range = maxRange;
				numRevolutions++;
			}
		}
	}

	public static MapLocation intToLoc(int i){
		return new MapLocation(i/100,i%100);
	}

	public void shootNext() throws GameActionException {
		MapLocation pastr = intToLoc(tower.readBroadcast(RobotPlayer.PASTR_LOCATION_CHANNEL));
		if(pastr.distanceSquaredTo(tower.getLocation()) < 300 && !pastr.equals(new MapLocation(0,0))) {
			center = pastr;
		}
		if(tower.isActive()) {
			MapLocation target = center.add((int)(Math.cos(angle)*range),(int)(Math.sin(angle)*range));
			updateAim();
			while(!usefulShot(target)) {
				target = center.add((int)(Math.cos(angle)*range),(int)(Math.sin(angle)*range));
				updateAim();
			}
			tower.attackSquare(target);
		}
		while (Clock.getBytecodesLeft() > 590 && !doneSearching) {
			searchStep();
		}
	}
	
}
