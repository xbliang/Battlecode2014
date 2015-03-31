package multicorner1001NoisePattern;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class BFSPattern extends NoiseTowerPattern {
	NoiseTowerPattern backup;
	RobotController tower;
	MapLocation center;
	MapLocation bestLoc;
	int shootPosition;
	int range;
	Direction flow[][];
	int shortestPathLength[][];
	
	static final Direction[] allDirections = Direction.values();
	
	public BFSPattern(RobotController t, int range, MapLocation center) throws GameActionException {
		tower = t;
		this.center = center;
		this.range = range;
		bestLoc = mostCowsWithinDonutSq(5, range, 4);
		backup = new LinearPattern(t, range);

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
		while(q.size() > 0) {
			MapLocation next = q.poll();
			Direction incoming = f.poll();
			if (!shouldSearch(m, next, range)) {
				continue;
			}
			flow[next.x][next.y] = incoming.opposite();
			shortestPathLength[next.x][next.y] = splAt(m) + 1;
			
			for (Direction d : allDirections) {
				f.add(d);
				q.add(next.add(d));
			}
		}
	}
	
	public boolean shouldSearch(MapLocation m, MapLocation next, int range) {
		return (tower.senseTerrainTile(next) == TerrainTile.NORMAL || tower.senseTerrainTile(next) == TerrainTile.ROAD)
				&& (splAt(next) == 0 || splAt(next) > splAt(m) + 1) && !next.equals(center)
				&& next.distanceSquaredTo(center) <= range;
	}
	
	public int splAt(MapLocation m) {
		return shortestPathLength[m.x][m.y];
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
	
	public void shootNext() throws GameActionException {
		if (tower.isActive()) {
			if (bestLoc != null) {
				tower.attackSquare(bestLoc.add(flowAt(bestLoc).opposite(), 2));				
			} else {
				backup.shootNext();
			}
		} else {
			bestLoc = mostCowsWithinDonutSq(9, range, 500);
		}
	}
	
}
