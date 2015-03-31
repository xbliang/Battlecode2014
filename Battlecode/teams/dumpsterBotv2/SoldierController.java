package dumpsterBotv2;

import java.io.File;
import java.util.ArrayList;

import sun.text.normalizer.CharTrie.FriendAgent;
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
	
	private static Direction adjustFormation(RobotInfo[] robotInfo, MapLocation target) {
		double [] dists = new double[500];
		double distThreshold = 1;
		int num = 1;
		MapLocation loc = rc. getLocation();
		dists[0] = Math.sqrt(loc.distanceSquaredTo(target));
		double avgDist = 0;
		boolean adjFriendly = false;
		for(RobotInfo info : robotInfo) {			
			if(info.type == RobotType.SOLDIER) {
				if(info.team == rc.getTeam()) {
					double d = Math.sqrt(info.location.distanceSquaredTo(target));
					if(loc.isAdjacentTo(info.location)) { //consider if friendly good dist as well
						adjFriendly = true;
					}
					dists[num] = d;
					num++;
				}
			}
		}
		
		for(int i = 0; i < num; i++) {
			//System.out.println(dists[i]);
			avgDist += dists[i];
		}
		avgDist /= num;
		//System.out.println("average"+avgDist);
		double distDiff = Math.abs(avgDist-dists[0]);
		boolean goodSpot = distDiff < distThreshold;
		ArrayList<Direction> goodMoves = new ArrayList<Direction>();
		double minimalDist = 1000;
		Direction minimalDir = Direction.NONE;
		for(int i = 0; i < 8; i++) {
			Direction d = allDirections[i];
			if(rc.canMove(d)) {
				double newddif = Math.abs(Math.sqrt(loc.add(d).distanceSquaredTo(target))-avgDist);				
				if(newddif < distThreshold) {
					goodMoves.add(d);
				}
				if(newddif < minimalDist) {
					minimalDist = newddif;
					minimalDir = d;
				}
			}
		}
		rc.setIndicatorString(2, ""+num+"AvgDIst:"+avgDist + " mydist:" + dists[0]+" "+goodMoves);
		if(goodSpot && adjFriendly) { //I'm good but somebody else wants the spot
			if(goodMoves.size() > 0) //if I still have a good move
				return goodMoves.get(0);
			else
				return Direction.NONE;
		} else if(goodSpot && !adjFriendly) { //if I'm good and nobody wants the spot
			return Direction.NONE;
		} else if(goodMoves.size() > 0) { //if i'm not good and I can be
			return goodMoves.get(0);
		} else { //get better
			return minimalDir;
		}
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
	
	private static Direction adjustFormation(RobotInfo[] frinedlies,RobotInfo[] enemies, MapLocation loc) {
		double myDist = distToNearest(loc,enemies);
		double avgDist = myDist;
		for(RobotInfo info : frinedlies) {			
			avgDist += distToNearest(info.location, enemies);
		}
		avgDist /= (frinedlies.length+1);
		double bestdiff = Math.abs(myDist - avgDist);
		Direction bestd = Direction.NONE;
		for(int i = 0; i < 8; i++) {
			Direction d = allDirections[i];
			if(rc.canMove(d)) {
				double newdif = Math.abs(distToNearest(loc.add(d), enemies)-avgDist);
				if(newdif < bestdiff) {
					bestdiff = newdif;
					bestd = d;
				}
			}
		}
		return bestd;
	}
	
	public void runSoldier() throws GameActionException {
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class,100);		
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
		}
		averageFriendly = new MapLocation(averageFriendly.x/friendly,averageFriendly.y/friendly);
		if(enemy > 0) {
			averageEnemy = new MapLocation(averageEnemy.x/enemy,averageEnemy.y/enemy);
		}
		//averageEnemy = target;
		enemyTurns *= (0.1)/friendly;
		friendlyTurns *= (0.1)/enemy;
		if(highestRating > 0) {
			rc.setIndicatorString(1, "fighting");
			if(rc.isActive() && rc.getLocation().distanceSquaredTo(highestTarget) <= 10) {
				rc.attackSquare(highestTarget);
				callForHelp(highestTarget);				
			} else {
				BasicPathing.tryToMove(rc.getLocation().directionTo(highestTarget), false, rc, directionalLooks, allDirections);
			}		
		}else{
			if(!helpFriends()) {
				if(numEnemySoldiers > 0) {
					Direction move = adjustFormation(friendlySoldiers,enemySoldiers,rc.getLocation());
					rc.setIndicatorString(1, "Adjust to "+ move);
					if(rc.isActive() && move != Direction.NONE)
						rc.move(move);
				}else if(enemy != 0 && (enemyTurns > friendlyTurns + 2 || !helpNear(averageFriendly,closestEnemy))) {			
					retreatFrom(averageEnemy);
					rc.setIndicatorString(1, "retreating");
					//System.out.println(helpNear(averageFriendly,averageEnemy));
					callForHelp(averageEnemy);
				} else {				
					//BasicPathing.tryToMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()), true, rc, directionalLooks, allDirections);
					MapLocation dest = Util.intToLoc(rc.readBroadcast(targetChannel));
					if(!dest.equals(target)) {
						target = dest;
						motion.startPath(dest);
					}
					rc.setIndicatorString(1, "Moving to target " + dest);						
					motion.tryNextMove();
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
