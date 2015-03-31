package soldierControllerExampleDistance;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Copy_2_of_SoldierController {
	private static RobotController rc;
	private static MotionUnit motion;
	private static MapLocation target = new MapLocation(0,0);
	public static int readPos = 99;
	public static final int helpRange = 100;
	private static Direction allDirections[] = Direction.values();
	private static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	private int targetChannel = 0;
	
	public Copy_2_of_SoldierController(RobotController r,int t) throws GameActionException {
		rc = r;
		motion = new MotionUnit(rc);
		motion.startPath(Util.intToLoc(rc.readBroadcast(0)));
		targetChannel = t;
		
	}
	
	private static void retreatFrom(MapLocation averageEnemy) throws GameActionException {
		Direction d = allDirections[(rc.getLocation().directionTo(averageEnemy).ordinal()+4)%8];
		BasicPathing.tryToMove(d, false, rc, directionalLooks, allDirections);	
	}
	
	private static double rateTarget(RobotInfo r) {
		double idist = rc.getLocation().distanceSquaredTo(r.location)-10;
		if(idist < 0) {
			idist = 0;
		}
		if(r.type == RobotType.SOLDIER) {
			return 0.5*(100-r.health) + 2*(25 - (idist)); 
		} else if(r.type == RobotType.PASTR) {
			if(idist > 0) {
				return 0;
			} else 
			return 5+(200-r.health)/10;
		} else if(r.type == RobotType.NOISETOWER) {
			return 1;
		} else {
			return 0;
		}
	}

	/*private static boolean helpNear(MapLocation avf, MapLocation ave) {
		//return true;
		//System.out.println(avf+" " + ave);
		return avf.distanceSquaredTo(ave) < 35;
	}*/
	private static boolean helpNear(MapLocation avf, MapLocation ave) {
		//System.out.println(avf+" " + ave);
		return avf.distanceSquaredTo(rc.getLocation()) < 10;
	}
	public void runSoldier() throws GameActionException {
		boolean combatStatus = false;
		boolean retreating = false;
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class,35);
		
		
		double highestRating = -10000;
		MapLocation highestTarget = null;		
		
		int friendly=1;
		double friendlyTurns = rc.getHealth();
		MapLocation averageFriendly = rc.getLocation();
		
		int enemy=0;
		double enemyTurns = 0;
		MapLocation averageEnemy = new MapLocation(0,0);
		int enemiesInRange = 0;
		for(Robot r : nearbyRobots) {
			RobotInfo info = rc.senseRobotInfo(r);
			
			if(info.team == rc.getTeam().opponent()) {
				double rating = rateTarget(info);					
				if(rating > highestRating) {
					highestRating = rating;
					highestTarget = info.location;
				}
			}
			
			if(info.type == RobotType.SOLDIER) {
				if(info.team == rc.getTeam().opponent()) {
					++enemy;					
					enemyTurns += info.health;
					averageEnemy = averageEnemy.add(info.location.x,info.location.y);
					if(info.location.distanceSquaredTo(rc.getLocation())<=25) {
						enemiesInRange++;
					}
				} else {
					++friendly;
					friendlyTurns += info.health;
					averageFriendly = averageFriendly.add(info.location.x,info.location.y);
				}
			}
		}
		averageFriendly = new MapLocation(averageFriendly.x/friendly,averageFriendly.y/friendly);
		if(enemy > 0) {
			averageEnemy = new MapLocation(averageEnemy.x/enemy,averageEnemy.y/enemy);
		}
		enemyTurns *= (0.1)/friendly;
		friendlyTurns *= (0.1)/enemy;
		if(enemy != 0 && (enemyTurns > friendlyTurns + 1 || !helpNear(averageFriendly,averageEnemy))) {			
			retreatFrom(averageEnemy);
			rc.setIndicatorString(2, "retreating");
			//System.out.println(helpNear(averageFriendly,averageEnemy));
			callForHelp(averageEnemy);
		} else if(highestRating > 0) {
			rc.setIndicatorString(2, "fighting");
			if(rc.isActive() && rc.getLocation().distanceSquaredTo(highestTarget) <= 10) {
				rc.attackSquare(highestTarget);
			} else {
				BasicPathing.tryToMove(rc.getLocation().directionTo(highestTarget), false, rc, directionalLooks, allDirections);
			}
			callForHelp(averageEnemy);
		} else {
			if(!helpFriends()) {
				rc.setIndicatorString(2, "Moving to target");
				//BasicPathing.tryToMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()), true, rc, directionalLooks, allDirections);
				MapLocation dest = Util.intToLoc(rc.readBroadcast(targetChannel));
				if(!dest.equals(target)) {
					target = dest;
					motion.startPath(dest);
				}
				motion.tryNextMove();
			}
		}		
	}
	
	private static void callForHelp(MapLocation enemy) throws GameActionException {
		int writePos = (rc.readBroadcast(99) + 1);
		if(writePos >= 200) {
			writePos -= 100;
		}
		rc.broadcast(writePos, Util.locToInt(enemy));
		rc.broadcast(99, writePos);
	}
	
	private static boolean helpFriends() throws GameActionException  {
		int readThrough = rc.readBroadcast(99);
		int readEnd = readThrough;
		if(readEnd < readPos) {
			readEnd += 100;
		}		
		int nearestDist = 100000;
		MapLocation nearestLoc = null;
		for(int i = readPos + 1; i <= readEnd; i++) {
			MapLocation loc = Util.intToLoc(rc.readBroadcast((i%100)+100));
			//System.out.println("Heard Call " + i +": " + loc);
			int dist = loc.distanceSquaredTo(rc.getLocation());
			if(dist < nearestDist && dist < helpRange) {
				nearestLoc = loc;
				nearestDist = dist;
			}
			
		}
		readPos = readThrough;
		if(nearestLoc != null) {
			rc.setIndicatorString(2,"Helping " + nearestLoc);
			if(!nearestLoc.equals(target)) {
				target = nearestLoc;
				motion.startPath(nearestLoc);
			}
			motion.tryNextMove();
			return true;
		}
		return false;
		
	}
	
}
