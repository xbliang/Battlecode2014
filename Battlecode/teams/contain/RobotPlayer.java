package contain;

import battlecode.common.*;

import java.util.*;
import contain.*;
import bugLookaheadv4.BasicPathing;



public class RobotPlayer {
	static RobotController rc;
	static Random rand;
	public static ArrayList<TreeSet<Integer>> connectivity;
	public static ArrayList<Integer> regionCosts;
	public static ArrayList<ArrayList<MapLocation>> paths;
	public static int[][] map;
	public static boolean mapbuilt = false;
	public static ArrayList<ArrayList<MapLocation>> joints;
	static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	static Direction allDirections[] = Direction.values();
	static MotionUnit motion;
	private static NoiseTowerPattern noiseTowerPattern;
	
	public static MapLocation getClosest(MapLocation point, Collection<MapLocation> locs) {
		int dist = -1;
		MapLocation ret = null;
		for(MapLocation l : locs) {
			int d = point.distanceSquaredTo(l);
			if(d < dist || dist == -1) {
				dist = d;
				ret = l;
			}
		}
		return ret;
	}
	public static MapLocation getClosest(RobotController rc, Robot[] robots) throws GameActionException {
		MapLocation ret = null;
		int dist = -1;	
		MapLocation point = rc.getLocation();
		for(Robot r : robots) {
			RobotInfo info = rc.senseRobotInfo(r);
			MapLocation l = info.location;
			int d = point.distanceSquaredTo(l);
			if(d < dist || dist == -1) {
				dist = d;
				ret = l;
			}
		}
		return ret;
	}
	
	private static boolean buildStructures(RobotController rc) throws GameActionException {
		boolean towerExists = false;
		for(Robot robot : rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam())) {
			if(rc.senseRobotInfo(robot).type == RobotType.NOISETOWER)
				towerExists = true;
		}
		if(Clock.getRoundNum() > rc.readBroadcast(200) && !towerExists && rc.isActive()) { // build tower 
			rc.construct(RobotType.NOISETOWER);
			rc.broadcast(200, Clock.getRoundNum()+101);
			return true;
		}
		boolean pastrExists = false;
		for(Robot robot : rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam())) {
			if(rc.senseRobotInfo(robot).type == RobotType.PASTR)
				pastrExists = true;
		}
		if(Clock.getRoundNum() > rc.readBroadcast(2) && !pastrExists && rc.isActive()) { // build PASTR 
			rc.construct(RobotType.PASTR);
			rc.broadcast(2, Clock.getRoundNum()+51);
			return true;
		}
		return false;
	}
	
	public static void run(RobotController rcIn) {
		rc = rcIn;
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
		int phase = 0;
		//0 = go to enemy base
		//1 = when there, swarm around base;
		if(rc.getType() == RobotType.NOISETOWER) {
			setupNoiseTower();
		}
		while(true) {
			if (rc.getType() == RobotType.HQ) {
				try {					
					//Check if a robot is spawnable and spawn one if it is
					for(Direction d : directions) {//////
						if (rc.isActive()) {
							if (rc.senseObjectAtLocation(rc.getLocation().add(d)) == null) {
								rc.spawn(d);
								break;
							}
						}
					}					
				} catch (Exception e) {
					System.out.println("HQ Exception");
					e.printStackTrace();
				}
			}
			
			if (rc.getType() == RobotType.SOLDIER) {				
				if(motion == null) {
					motion = new MotionUnit(rc);
				}
				boolean attacking = false;
				try {
					buildStructures(rc);
					Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
					ArrayList<MapLocation> targets = new ArrayList<MapLocation>();
					for(Robot r : enemyRobots) {
						RobotInfo info = rc.senseRobotInfo(r);
						if(info.type != RobotType.HQ) {
							targets.add(info.location);
						}
					}
					if(targets.size()>0){						
						MapLocation loc = getClosest(rc.getLocation(), targets);
						if(loc.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared){
							attacking = true;
							if(rc.isActive()){
								rc.attackSquare(loc);
							}
						}
					} 
				
					if(!attacking) {
						if(phase == 0) {
							if(motion.onPath()) {
								motion.continuePath();					
							} else {
								motion.startPath(rc.senseEnemyHQLocation());
							}
						} else if(phase == 1) {
							for(Direction d : directions) {
								
							}
						}
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			if(rc.getType()==RobotType.NOISETOWER) {
				try {
					runNoiseTower();
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			rc.yield();
		}
	}
	
	private static void setupNoiseTower() {
		noiseTowerPattern = new NoiseTowerPattern(rc,20);
	}
	
	private static void runNoiseTower() throws GameActionException {
		noiseTowerPattern.shootNext();
		/*int square = rc.readBroadcast(1); // an integer about which square to attack. spirals inwards, the greater "square" is the closer we shoot
		do {
			if(square/8 <= 8) // far away; use larger attacks
			{
				if(square % 2 == 0) { // cardinal direction
					MapLocation target = rc.getLocation().add(directions[square%8], 20 - square/8);
					if(target.x >= 0 && target.y >= 0 && target.x < rc.getMapWidth() && target.y < rc.getMapHeight() && rc.canAttackSquare(target)) {
						rc.attackSquare(target);
					}
				}
				else { // diagonal
					MapLocation target = rc.getLocation().add(directions[square%8], 14 - square/8);
					if(target.x >= 0 && target.y >= 0 && target.x < rc.getMapWidth() && target.y < rc.getMapHeight() && rc.canAttackSquare(target)) {
						rc.attackSquare(target);
					}
				}
			}
			else // near; use smaller attacks
			{
				MapLocation target = rc.getLocation().add(directions[2*(square%4)], 11 - (square-72)/4);
				if(target.x >= 0 && target.y >= 0 && target.x < rc.getMapWidth() && target.y < rc.getMapHeight() && rc.canAttackSquare(target)) {
					rc.attackSquareLight(target);
				}
			}
			square++;
			rc.broadcast(1, square % 104);
		} while(rc.isActive() && square > 0);*/
	}


}
