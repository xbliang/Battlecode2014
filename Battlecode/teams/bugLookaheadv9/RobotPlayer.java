package bugLookaheadv9;

import battlecode.common.*;

import java.util.*;

import bugLookaheadv5.BasicPathing;



public class RobotPlayer {
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
	
	public static void run(RobotController rc) {
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
		while(true) {
			if (rc.getType() == RobotType.HQ) {
				try {					
					//Check if a robot is spawnable and spawn one if it is
					for(Direction d : directions) {//////
						if (rc.isActive() && rc.senseRobotCount() < 1) {
							if (rc.senseObjectAtLocation(rc.getLocation().add(d)) == null) {
								rc.spawn(Direction.SOUTH);
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
				/*if(Clock.getRoundNum() == 200) {
					motion.startPath(new MapLocation(rc.getMapWidth()-2,rc.getMapHeight()-2));
					System.out.println();
				}
				if(Clock.getRoundNum() == 300) {
					motion.startPath(rc.senseEnemyHQLocation());
					System.out.println();
				}
				if(Clock.getRoundNum() == 400) {
					motion.startPath(new MapLocation(rc.getMapWidth()-2,rc.getMapHeight()-2));
					System.out.println();
				}*/
				/*if(Clock.getRoundNum() > 250) {
					motion.printMap();
				}*/
				
				if(motion.onPath()) {
					try {
						motion.tryNextMove();
					} catch (GameActionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}					
				} else {
					motion.startPath(new MapLocation(75,63));
				}
			}
			
			rc.yield();
		}
	}
}
