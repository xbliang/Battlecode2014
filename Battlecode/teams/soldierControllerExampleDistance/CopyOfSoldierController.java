package soldierControllerExampleDistance;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class CopyOfSoldierController {
	private static RobotController rc;
	private static MotionUnit motion;
	private static MapLocation target = new MapLocation(0,0);
	public static int readPos = 99;
	public static final int helpRange = 100;
	private static Direction allDirections[] = Direction.values();
	private static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	
	public CopyOfSoldierController(RobotController r) throws GameActionException {
		rc = r;
		motion = new MotionUnit(rc);
		motion.startPath(Util.intToLoc(rc.readBroadcast(0)));
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
			return 100-r.health - (idist); 
		} else if(r.type == RobotType.PASTR) {
			return 50-r.health - 200*(idist); 
		} else {
			return -10000;
		}
	}

	private static boolean helpNear(MapLocation avf, MapLocation ave) {
		//System.out.println(avf+" " + ave);
		return avf.distanceSquaredTo(ave) < 35;
	}
	public void runSoldier() throws GameActionException {
		boolean combatStatus = false;
		boolean retreating = false;
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class,35);
		
		
		double highestRating = -10000;
		MapLocation highestTarget = null;		
		
		int friendly=1;
		double friendlyDamage = rc.getHealth();
		MapLocation averageFriendly = rc.getLocation();
		
		int enemy=0;
		double enemyDamage = 0;
		MapLocation averageEnemy = new MapLocation(0,0);
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
					enemyDamage += info.health;
					averageEnemy = averageEnemy.add(info.location.x,info.location.y);
				} else {
					++friendly;
					friendlyDamage += info.health;
					averageFriendly = averageFriendly.add(info.location.x,info.location.y);
				}
			}
		}
		averageFriendly = new MapLocation(averageFriendly.x/friendly,averageFriendly.y/friendly);
		if(enemy > 0) {
			averageEnemy = new MapLocation(averageEnemy.x/enemy,averageEnemy.y/enemy);
		}
		enemyDamage *= (1.0)/friendly;
		friendlyDamage *= (1.0)/enemy;
		if(highestTarget == null){
			combatStatus = false;
		} else if(enemyDamage > friendlyDamage || !helpNear(averageFriendly,averageEnemy)) {
			retreatFrom(averageEnemy);
			retreating = true;
			combatStatus = false;
			rc.setIndicatorString(2, "retreating");
			System.out.println(helpNear(averageFriendly,averageEnemy));
			callForHelp(averageEnemy);
		} else if(enemyDamage <= friendlyDamage) {
			rc.setIndicatorString(2, "Battling");
			combatStatus = true;
			callForHelp(averageEnemy);
		}
		
		//Robot[] enemyRobotsSight = rc.senseNearbyGameObjects(Robot.class,35,rc.getTeam().opponent());
		rc.setIndicatorString(0, " friendly"+friendly);
		rc.setIndicatorString(1, "enemy"+enemy);		
		if(combatStatus){
			//attack lowest health thing in attack range
			if(highestTarget != null) {
				if(rc.isActive() && rc.getLocation().distanceSquaredTo(highestTarget) <= 10) {
					rc.attackSquare(highestTarget);
				} else {
					BasicPathing.tryToMove(rc.getLocation().directionTo(highestTarget), false, rc, directionalLooks, allDirections);
				}
			} else {
				BasicPathing.tryToMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()), false, rc, directionalLooks, allDirections);
			}
		} else if(!retreating) {
			if(!helpFriends()) {
				rc.setIndicatorString(2, "Moving to target");
				//BasicPathing.tryToMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()), true, rc, directionalLooks, allDirections);
				MapLocation dest = Util.intToLoc(rc.readBroadcast(0));
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
			BasicPathing.tryToMove(rc.getLocation().directionTo(nearestLoc), false, rc, directionalLooks, allDirections);
			return true;
		}
		return false;
		
	}
	
}
