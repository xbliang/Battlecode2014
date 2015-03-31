package dumpsterCheese420.ntp;

import java.util.LinkedList;
import java.util.Queue;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

// First, compute BFS tree from center, ignoring squares further than (range) squared Euclidean distance away.
// Then, take points every (angularVelocity) radians on a circle of radius (sqrt(range)).
// From each point, fire to drive cows along the BFS path to the center.
public class BFSCirclePattern extends NoiseTowerPattern {
	RobotController tower;
	MapLocation center;
	MapLocation bestLoc;
	int range;
	double angle;
	double angularVelocity = Math.PI/4;
	Direction flow[][];
	int shortestPathLength[][];
	
	static final Direction[] allDirections = Direction.values();
	
	public BFSCirclePattern(RobotController t, int range, MapLocation center) throws GameActionException {
		tower = t;
		this.center = center;
		this.range = range;
		bestLoc = mostCowsWithinDonutSq(5, range, 4);

		flow = new Direction[t.getMapWidth()][t.getMapHeight()];
		flow[center.x][center.y] = Direction.NONE;
		shortestPathLength = new int[t.getMapWidth()][t.getMapHeight()];
		shortestPathLength[center.x][center.y] = 0;
		
		search(t.getLocation(), range);
	}

	public void search(MapLocation m, int range) {
		Queue<MapLocation> q = new LinkedList<MapLocation>();
		Queue<Direction> f = new LinkedList<Direction>();
		q.add(m);
		f.add(Direction.NONE);
		MapLocation current;
		MapLocation previous;
		Direction incoming;
		while(q.size() > 0) {
			current = q.poll();
			incoming = f.poll();
			previous = current.add(incoming.opposite()); 
			if (!shouldSearch(previous, current, range)) {
				continue;
			}
			flow[current.x][current.y] = incoming.opposite();
			shortestPathLength[current.x][current.y] = splAt(m) + 1;
			System.out.println(current);
			
			for (Direction d : allDirections) {
				f.add(d);
				q.add(current.add(d));
			}
		}
	}
	
	public boolean shouldSearch(MapLocation previous, MapLocation current, int range) {
		TerrainTile terrain = tower.senseTerrainTile(current);
		return (splAt(current) == 0 || splAt(current) > splAt(previous) + 1) 
				&& (terrain == TerrainTile.NORMAL || terrain == TerrainTile.ROAD)
				&& current.distanceSquaredTo(center) <= range && !current.equals(center);
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
	
	public Direction flowAt(MapLocation m) {
		return flow[m.x][m.y];
	}
	
	public MapLocation mostCowsWithinDonutSq(int minRadius, int maxRadius, int minCows) throws GameActionException {
		double maxCows = -1;
		MapLocation bestLoc = center;
		for (MapLocation m : MapLocation.getAllMapLocationsWithinRadiusSq(center, maxRadius)) {
			if (tower.canSenseSquare(m) && m.distanceSquaredTo(center) > minRadius) {
				double cows = tower.senseCowsAtLocation(m);
				if (cows > maxCows) {
					maxCows = cows;
					bestLoc = m;
				}
			}
		}
		if (maxCows > minCows) {
			return bestLoc;
		} else {
			return null;
		}
	}
	
	public MapLocation nextStartingLocation() {
		angle += angularVelocity;
		int radius = (int) Math.sqrt((double) range);
		for (int r = radius; r > 2; r--) {
			MapLocation m = center.add((int)(Math.cos(angle)*r),(int)(Math.sin(angle)*r));
			if(splAt(m) > 0) {	// If can get to center from m
				return m;
			}
		}
		// If we get here, no locations in the current direction are any good
		return nextStartingLocation();
	}
	
	public void shootNext() throws GameActionException {
		if(tower.isActive()){
			if (bestLoc.distanceSquaredTo(center) < 9) {
				bestLoc = nextStartingLocation();
			} else {
				bestLoc = bestLoc.add(flowAt(bestLoc));
			}
			tower.attackSquare(bestLoc.add(flowAt(bestLoc).opposite(), 2));
		}
	}
	
}
