package instaPASTR;

import java.io.File;
import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldierController {
	private static RobotController rc;
	private static MotionUnit motion;
	private static MapLocation target = new MapLocation(0,0);
	public static int readPos = 99;
	public static final int helpRange = 100;
	private static Direction allDirections[] = Direction.values();
	private static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	private int targetChannel = 0;
	private static int LEADER_CHANNEL = 1000;
	
	
	public SoldierController(RobotController r,int t) throws GameActionException {
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
			if(idist > 0) {
				return 0;
			}
			return 50 + 0.5*(100-r.health); 
		} else if(r.type == RobotType.PASTR) {
			if(idist > 0) {
				return 0;
			} else 
			return 5+(200-r.health)/10;
		} else if(r.type == RobotType.NOISETOWER) {
			if(idist > 0) {
				return 0;
			}
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
		//return avf.distanceSquaredTo(rc.getLocation()) < 10;
		return true;
	}
	
	/*private static Direction optimalFormationMove(RobotInfo[] frinedlies,RobotInfo[] enemies, MapLocation loc) {
		double best = formationQuality(frinedlies,enemies,loc);
		Direction dir = Direction.NONE;
		for(int i = 0; i < 8; i++) {
			Direction d = allDirections[i];
			if(rc.canMove(d)) {
				double q = formationQuality(frinedlies,enemies,loc.add(d));
				if(q < best) {
					best = q;
					dir = d;
				}
			}
	}
		return dir;
	}*/
	
	private static double distToNearest(MapLocation start, RobotInfo[] robots) {
		int dsq = 100000;
		MapLocation loc = null;
		for(RobotInfo r : robots) {
			int d = start.distanceSquaredTo(r.location);
			if(d < dsq) {
				dsq = d;
				loc = r.location;
			}
		}
		return Math.sqrt(dsq);
	}
	
	private static int distToNearestSq(MapLocation start, RobotInfo[] robots) {
		int dsq = 100000;
		MapLocation loc = null;
		for(RobotInfo r : robots) {
			int d = start.distanceSquaredTo(r.location);
			if(d < dsq) {
				dsq = d;
				loc = r.location;
			}
		}
		return dsq;
	}
	
	private static int nearbyRobots(MapLocation start, RobotInfo[] robots, int distSq) {
		int close = 0;
		for(RobotInfo r : robots) {
			int d = start.distanceSquaredTo(r.location);
			if(d <= distSq) {
				close++;
			}
		}
		return close;
	}
	
	private static MapLocation averageRobotLocation(RobotInfo[] robots) {
		MapLocation pos = new MapLocation(0,0);
		for(RobotInfo r : robots) {
			pos = pos.add(r.location.x, r.location.y);
		}
		return new MapLocation(pos.x/robots.length,pos.y/robots.length);
	}
	
	/*private static Direction adjustFormation(RobotInfo[] friendlies,RobotInfo[] enemies, MapLocation loc) throws GameActionException {
		int confidence = -1;
		double targetDist = 5;
		double targetThreshold = 0.5;
		double myDist = distToNearest(loc,enemies);
		double mydiff = Math.abs(distToNearest(loc,enemies)-targetDist);
		Direction bestd = Direction.NONE;
		boolean adj = false;
		boolean allgood = true;
		int inPos = 0;
		double friendlyTurns = 0;
		if(mydiff < targetThreshold) {
			inPos++;
			friendlyTurns += rc.getHealth();
		}
		for(RobotInfo info : friendlies) {	
			if(info.location.isAdjacentTo(loc)) {
				adj = true;
			}
			if(Math.abs(distToNearest(info.location, enemies)-targetDist) > targetThreshold) {
				allgood = false;
			} else {
				friendlyTurns += info.health;
				inPos++;
			}
		}
		double enemyTurns = 0;
		MapLocation averageEnemy = new MapLocation(0,0);
		for(RobotInfo info : enemies) {
			if(info.type == RobotType.SOLDIER) {
				enemyTurns += info.health;
				averageEnemy = averageEnemy.add(info.location.x,info.location.y);
			}
		}
		averageEnemy = new MapLocation(averageEnemy.x/enemies.length,averageEnemy.y/enemies.length);
		enemyTurns /= (inPos)*10;
		friendlyTurns /= (enemies.length*10);
		friendlyTurns += confidence;
		rc.setIndicatorString(0, "" + (inPos + confidence) +" " + enemies.length);
		if(inPos + confidence > enemies.length && enemyTurns > 0) { //gogogo
			callForHelp(averageEnemy);
			return rc.getLocation().directionTo(averageEnemy);
		}
		boolean isGood = mydiff < targetThreshold;
		for(int i = 0; i < 8; i++) {
			Direction d = allDirections[i];
			if(BasicPathing.canMove(d, false, rc)) {
				double newdiff = Math.abs(distToNearest(loc.add(d), enemies)-targetDist);
				if(isGood) {
					if(newdiff < targetThreshold) {
						mydiff = newdiff;
						bestd = d;
					}
				} else {
					if(newdiff < mydiff) {
						mydiff = newdiff;
						bestd = d;
					}
				}
			}
		}
		return bestd;
	}*/
	
	
	private static Direction adjustFormation(RobotInfo[] friendlies, RobotInfo[] enemies, MapLocation loc) throws GameActionException {
		double targetUpper = 6;
		double targetThreshold = 0.5;
		int confidence = -5;
		double myDist = distToNearest(loc, enemies);		
		double myDiff = Math.abs(myDist - targetUpper);
		int inPos = 0;
		if(myDiff < targetThreshold) {
			inPos++;
		}
		for(RobotInfo info : friendlies) {	
			if(Math.abs(distToNearest(info.location, enemies)-targetUpper) < targetThreshold) {
				inPos++;
			}
		}
		double enemyTurns = 0;
		boolean enemySoldiers = false;
		for(RobotInfo info : enemies) {	
			if(info.type == RobotType.SOLDIER) {
				enemySoldiers = true;
			}
		}
		if(inPos + confidence > enemies.length && enemySoldiers) { //gogogo
			MapLocation average = averageRobotLocation(enemies);
			callForHelp(average);
			return rc.getLocation().directionTo(average);
		}
		
		
		
		Direction bestd = Direction.NONE;
		double best = 100;
		for(int i = 0; i < 9; i++) {
			Direction d = allDirections[i];
			if(BasicPathing.canMove(d, false, rc)) {
				double r = rateSquare(friendlies,enemies,loc.add(d),targetUpper,0);
				if(r < best) {
					best = r;
					bestd = d;
				}
			}
		}
		return bestd;
	}
	public static double fightQuality(RobotInfo[] friendlies, RobotInfo[] enemies) {
		double friendlyTurns = 0;
		int inPos = 0;
		int targetDist = 20;
		int myDist = distToNearestSq(rc.getLocation(), enemies);
		if(myDist <= targetDist) {
			inPos++;
			friendlyTurns += rc.getHealth();
		}
		for(RobotInfo info : friendlies) {
			if(distToNearestSq(info.location, enemies) <= targetDist) {
				friendlyTurns += info.health;
				inPos++;
			}
		}
		double enemyTurns = 0;
		for(RobotInfo info : enemies) {
			if(info.type == RobotType.SOLDIER) {
				enemyTurns += info.health;
			}
		}
		return friendlyTurns - enemyTurns;		
	}
	public static double rateSquare(RobotInfo[] friendlies, RobotInfo[] enemies, MapLocation loc, double targetUpper, double targetLower) {
		double fightQuality = fightQuality(friendlies, enemies);
		rc.setIndicatorString(0, "FIGHT QUALITY " + fightQuality);
		if(friendlies.length > 0) {
			MapLocation averageFriendly = averageRobotLocation(friendlies);
			double distFromAverage = Math.sqrt(averageFriendly.distanceSquaredTo(rc.getLocation()));
			
			rc.setIndicatorString(1, ""+averageFriendly + " Dist from average " + distFromAverage);
			if(distFromAverage > friendlies.length + 3) {
				return distFromAverage;
			}
		}
		
		double myDist = distToNearest(loc, enemies);
		double myDiff = Math.abs(myDist - targetUpper);
		double threshold = 0;
		double badness = 0;
		if(myDiff >= threshold) {
			badness += myDiff;
		}
		return badness;	
	}

	private boolean build() throws GameActionException {
		MapLocation buildSpot = Util.intToLoc(rc.readBroadcast(RobotPlayer.FIRST_PASTR_CHANNEL));
		if(!rc.isActive() || rc.getLocation().distanceSquaredTo(buildSpot) > 5 || rc.getLocation().x == 0 || rc.getLocation().y == 0 || rc.getLocation().x == rc.getMapWidth()-1 || rc.getLocation().y == rc.getMapHeight()-1)
			return false;
		int currTurn = Clock.getRoundNum();
		if(currTurn > rc.readBroadcast(RobotPlayer.NOISE_BUILD_CHANNEL)) {
			rc.broadcast(RobotPlayer.NOISE_BUILD_CHANNEL, currTurn + 105);
			rc.construct(RobotType.NOISETOWER);
			return true;
		}
		if(currTurn > rc.readBroadcast(RobotPlayer.PASTR_BUILD_CHANNEL)) {
			rc.broadcast(RobotPlayer.PASTR_BUILD_CHANNEL, currTurn + 55);
			rc.construct(RobotType.PASTR);
			return true;
		}
		return false;
	}
	
	public void runSoldier() throws GameActionException {
		rc.setIndicatorString(0, "");
		rc.setIndicatorString(1, "");
		rc.setIndicatorString(2, "");
		rc.setIndicatorString(3, "");
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class,144);		
		RobotInfo[] robotInfo = new RobotInfo[nearbyRobots.length];
		int numFriendlySoldiers = 0;
		int numEnemySoldiers = 0;
		for(int i = 0; i < nearbyRobots.length; i++) {
			robotInfo[i] = rc.senseRobotInfo(nearbyRobots[i]);
			if(robotInfo[i].type == RobotType.SOLDIER) {
				if(robotInfo[i].team == rc.getTeam()) {
					numFriendlySoldiers++;
				} else {
					numEnemySoldiers++;
				}
			}
			if(robotInfo[i].type == RobotType.HQ && robotInfo[i].team ==rc.getTeam().opponent()) {
				numEnemySoldiers++;
			}
		}
		
		int friendlyPos = 0;
		int enemyPos = 0;
		RobotInfo[] friendlySoldiers = new RobotInfo[numFriendlySoldiers];
		RobotInfo[] enemySoldiers = new RobotInfo[numEnemySoldiers];
		
		double highestRating = -10000;
		MapLocation highestTarget = null;		
		
		int friendly=1;
		double friendlyTurns = rc.getHealth();
		MapLocation averageFriendly = rc.getLocation();
		
		int enemy=0;
		double enemyTurns = 0;
		MapLocation averageEnemy = new MapLocation(0,0);
		int enemiesInRange = 0;
		
		int closestDist = 100000;
		MapLocation closestEnemy = new MapLocation(0,0);
		for(RobotInfo info : robotInfo) {			
			if(info.team == rc.getTeam().opponent()) {
				double rating = rateTarget(info);					
				if(rating > highestRating) {
					highestRating = rating;
					highestTarget = info.location;
				}
			}			
			if(info.type == RobotType.SOLDIER) {
				if(info.team == rc.getTeam().opponent()) {
					enemySoldiers[enemyPos] = info;
					enemyPos++;
					/*++enemy;					
					enemyTurns += info.health;
					averageEnemy = averageEnemy.add(info.location.x,info.location.y);
					int dist =info.location.distanceSquaredTo(rc.getLocation());
					if(dist<=25) {
						enemiesInRange++;
					}
					if(dist < closestDist) {
						closestEnemy = info.location;
						closestDist = dist;
					}*/
				} else {
					friendlySoldiers[friendlyPos] = info;
					friendlyPos++;
					/*++friendly;
					friendlyTurns += info.health;
					averageFriendly = averageFriendly.add(info.location.x,info.location.y);*/
				}
			}
			if(info.type == RobotType.HQ && info.team ==rc.getTeam().opponent()) {
				enemySoldiers[enemyPos] = info;
				enemyPos++;
			}
		}
		averageFriendly = new MapLocation(averageFriendly.x/friendly,averageFriendly.y/friendly);
		if(enemy > 0) {
			averageEnemy = new MapLocation(averageEnemy.x/enemy,averageEnemy.y/enemy);
		}
		//averageEnemy = target;
		enemyTurns *= (0.1)/friendly;
		friendlyTurns *= (0.1)/enemy;
		if(highestRating > 0) {
			rc.setIndicatorString(1, "fighting " + highestTarget);
			callForHelp(highestTarget);	
			if(rc.isActive() && rc.getLocation().distanceSquaredTo(highestTarget) <= 10) {
				
				rc.attackSquare(highestTarget);
							
			}/* else {
				BasicPathing.tryToMove(rc.getLocation().directionTo(highestTarget), false, rc, directionalLooks, allDirections);
			}*/	
		}else{
			if(!helpFriends()) {
				if(numEnemySoldiers > 0) {
					Direction move = adjustFormation(friendlySoldiers,enemySoldiers,rc.getLocation());
					//rc.setIndicatorString(1, "Adjust to "+ move);
					if(rc.isActive() && move != Direction.NONE && BasicPathing.canMove(move, false, rc))
						BasicPathing.tryToMove(move,false,rc,directionalLooks,allDirections);
				/*}else if(enemy != 0 && (enemyTurns > friendlyTurns + 2 || !helpNear(averageFriendly,closestEnemy))) {			
					retreatFrom(averageEnemy);
					rc.setIndicatorString(1, "retreating");
					//System.out.println(helpNear(averageFriendly,averageEnemy));
					callForHelp(averageEnemy);*/
				} else {
					if(build())
						return;				
					//BasicPathing.tryToMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()), true, rc, directionalLooks, allDirections);
					MapLocation dest = Util.intToLoc(rc.readBroadcast(targetChannel));
					if(!dest.equals(target)) {
						target = dest;
						motion.startPath(dest);
					}
					rc.setIndicatorString(1, "Moving to target " + dest);
					if(rc.getLocation().distanceSquaredTo(target) > 24)
						motion.tryNextMove();
					else
						motion.tryNextSneak();
				}
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
			rc.setIndicatorString(2,"ReadCall " + loc);
			//System.out.println("Heard Call " + i +": " + loc);
			int dist = loc.distanceSquaredTo(rc.getLocation());
			if(dist < nearestDist && dist < helpRange) {
				nearestLoc = loc;
				nearestDist = dist;
			}
			
		}
		readPos = readThrough;
		//rc.setIndicatorString(2,"ReadThrough " + readPos);
		if(nearestLoc != null) {			
			BasicPathing.tryToMove(rc.getLocation().directionTo(nearestLoc), true, rc, directionalLooks, allDirections);
			rc.setIndicatorString(1,"Helping " + nearestLoc);
			/*rc.setIndicatorString(2,"Helping " + nearestLoc);
			if(!nearestLoc.equals(target)) {
				target = nearestLoc;
				motion.startPath(nearestLoc);
			}
			motion.tryNextMove();*/
			return true;
		}
		return false;
		
	}
	
}
