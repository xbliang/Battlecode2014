package cheese2420NoisePattern.ntp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import cheese2420NoisePattern.RobotPlayer;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class SpiralPatternLOS extends NoiseTowerPattern {
	ArrayList<MapLocation> fireOrder;
	RobotController tower;
	int shootPosition;
	double angle = 0;
	double resolution = 0;
	double range = 0;
	int maxRange = 0;
	double velocity = 6;
	MapLocation center;
	int mapHeight;
	int mapWidth;
	public double numRevolutions = 0;
	
	// Initializer which sets center to tower location
	public SpiralPatternLOS(RobotController t, int r) {
		tower = t;
		fireOrder = new ArrayList<MapLocation>();
		shootPosition = 0;
		center = t.getLocation();
		range = r;
		maxRange = r;
		resolution = 4;
		mapHeight = t.getMapHeight();
		mapWidth = t.getMapWidth();
	}
	
	// Initializer which takes center as input
	public SpiralPatternLOS(RobotController t, int r, MapLocation center) {
		tower = t;
		fireOrder = new ArrayList<MapLocation>();
		shootPosition = 0;
		this.center = center;
		range = r;
		maxRange = r;
		resolution = 4;
		mapHeight = t.getMapHeight();
		mapWidth = t.getMapWidth();
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

	public void genPattern(MapLocation l, int val, double piover) {
		double angle = Math.PI/piover;
		double twoPI = 2*Math.PI;
		ArrayList<MapLocation> test = new ArrayList<MapLocation>();
		System.out.println("Searching");
		for(int range = val; range >= 3; range-=2) {
			for(double i = 0; i <= twoPI; i+= angle) {
				test.add(l.add((int)(Math.cos(i)*range),(int)(Math.sin(i)*range)));
			}
		}
		for(MapLocation t : test) {
			if(lineOfSight(l,t))
				fireOrder.add(t);
		}
		System.out.println(fireOrder);
		
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
			if (lineOfSight(center, target.add(target.directionTo(center), 3))) {
				tower.attackSquare(target);
			} else {
				shootNext();
			}
	}
	}
	
}
