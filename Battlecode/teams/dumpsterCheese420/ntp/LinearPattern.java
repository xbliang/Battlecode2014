package dumpsterCheese420.ntp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import dumpsterCheese420.RobotPlayer;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class LinearPattern extends NoiseTowerPattern {
	RobotController tower;
	MapLocation target;
	double range;
	double maxRange;
	double minRange = 3;
	double velocity = 2;
	double angle = 0;
	double angularVelocity = 3*Math.PI/8;
	MapLocation center;
	int mapHeight;
	int mapWidth;
	Direction allDirections[] = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, 
			Direction.NORTH_WEST, Direction.NORTH_EAST, Direction.SOUTH_EAST, Direction.SOUTH_WEST};
	int directionIndex;
	public double numRevolutions;
	double period = 16;
	
	public LinearPattern(RobotController t, int r) {
		tower = t;
		center = t.getLocation();
		range = r;
		maxRange = r;
		mapHeight = t.getMapHeight();
		mapWidth = t.getMapWidth();
		directionIndex = new Random().nextInt(8);
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

	public static MapLocation intToLoc(int i){
		return new MapLocation(i/100,i%100);
	}

	public void shootNext() throws GameActionException {
		MapLocation pastr = intToLoc(tower.readBroadcast(RobotPlayer.PASTR_LOCATION_CHANNEL));
		if(pastr.distanceSquaredTo(tower.getLocation()) < 300 && !pastr.equals(new MapLocation(0,0))) {
			center = pastr;
		}
		if (tower.isActive()){
//			if (directionIndex < 3) {
//				target = center.add(allDirections[directionIndex], (int) range);			
//			}
//			else {
//				target = center.add(allDirections[directionIndex], (int) (range / 1.414));
//			}
			target = center.add((int)(Math.cos(angle)*range),(int)(Math.sin(angle)*range));
			range -= velocity;
			if (range < minRange) {
				range = maxRange;
				angle += angularVelocity;
				numRevolutions += 1.0 / period;
			}
			if (usefulShot(target) && lineOfSight(center, target.add(target.directionTo(center), 3))) {
//			if (usefulShot(target)) {
				tower.attackSquare(target);
			} else {
				shootNext();
			}
		}
	}
	
}
