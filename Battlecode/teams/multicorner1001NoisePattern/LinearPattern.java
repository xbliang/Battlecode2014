package multicorner1001NoisePattern;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class LinearPattern extends NoiseTowerPattern {
	RobotController tower;
	double range;
	double maxRange;
	double minRange = 2;
	double velocity = 2;
	MapLocation center;
	int mapHeight;
	int mapWidth;
	Direction allDirections[] = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, 
			Direction.NORTH_WEST, Direction.NORTH_EAST, Direction.SOUTH_EAST, Direction.SOUTH_WEST};
	int directionIndex;
	
	public LinearPattern(RobotController t, int r) {
		tower = t;
		center = t.getLocation();
		range = r;
		maxRange = r;
		mapHeight = t.getMapHeight();
		mapWidth = t.getMapWidth();
		directionIndex = 0;
	}
	
	public boolean lineOfSight(MapLocation p, MapLocation end) {		
		while(true) {
			TerrainTile tile = tower.senseTerrainTile(p);
			if(tile == TerrainTile.OFF_MAP || tile == TerrainTile.VOID) {
				return false;
			}
			if(!p.equals(end)) {
				p = p.add(p.directionTo(end));
			} else {
				return true;
			}
		}
	}	

	public boolean usefulShot(MapLocation p) {
		if(p.distanceSquaredTo(tower.getLocation()) > 300) {
			return false;
		}
		TerrainTile tile = tower.senseTerrainTile(p);
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

	public void shootNext() throws GameActionException {
		if (tower.isActive()){
			MapLocation target;
			if (directionIndex < 3) {
				target = center.add(allDirections[directionIndex], (int) range);			
			}
			else {
				target = center.add(allDirections[directionIndex], (int) (range / 1.414));
			}
			range -= velocity;
			if (range < minRange) {
				range = maxRange;
				directionIndex++;
				directionIndex %= allDirections.length;
			}
			if (usefulShot(target)) {
				tower.attackSquare(target);
			} else {
				shootNext();
			}
		}
	}
	
}
