package dumpsterBotv6;

import java.io.File;
import java.util.ArrayList;

import sun.text.normalizer.CharTrie.FriendAgent;
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
	
	private static MapLocation averageFriendly;
	private static MapLocation averageEnemy;
	private static RobotInfo [] friendlySoldiers;
	private static int numFriendlySoldiers;
	private static RobotInfo [] enemySoldiers;
	private static int numEnemySoldiers;
	private static RobotInfo [] enemies;
	private static int numEnemies;
	private static boolean retreat = false;
	private static int retreatLooks[] = new int[]{2,3,4,5,6};
	private static int advanceLooks[] = new int[]{-2,-1,0,1,2,3};
	private static boolean healing = false;
	private static int healTo = 20;
	
	
	
	public SoldierController(RobotController r,int t) throws GameActionException {
		rc = r;
		motion = new MotionUnit(rc);
		motion.startPath(Util.intToLoc(rc.readBroadcast(0)));
		targetChannel = t;
		friendlySoldiers = new RobotInfo[500];
		enemySoldiers = new RobotInfo[500];
		enemies = new RobotInfo[500];
	}
	
	
	private static void updateInformation() throws GameActionException {
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class,144);		
		RobotInfo[] robotInfo = new RobotInfo[nearbyRobots.length];
		numFriendlySoldiers = 0;
		numEnemySoldiers = 0;
		numEnemies = 0;
		int averageFriendlyx = rc.getLocation().x;
		int averageFriendlyy = rc.getLocation().y;
		int averageEnemyx = 0;
		int averageEnemyy = 0;
		for(int i = 0; i < nearbyRobots.length; i++) {
			RobotInfo info = rc.senseRobotInfo(nearbyRobots[i]);
			if(info.team == rc.getTeam()) {
				friendlySoldiers[numFriendlySoldiers] = info;
				numFriendlySoldiers++;
				averageFriendlyx += info.location.x;
				averageFriendlyy += info.location.x;
			} else {
				enemies[numEnemies] = info;
				numEnemies++;
				if(info.type == RobotType.SOLDIER) {
					enemySoldiers[numEnemySoldiers] = info;
					numEnemySoldiers++;
					averageEnemyx += info.location.x;
					averageEnemyy += info.location.y;
				}
			}
		}
		if(numFriendlySoldiers == 0) {
			averageFriendly= rc.getLocation();
		} else {
			averageFriendly = new MapLocation(averageFriendlyx/numFriendlySoldiers,averageFriendlyy/numFriendlySoldiers);
		}
		if(numEnemySoldiers == 0) {
			averageEnemy= rc.senseEnemyHQLocation();
		} else {
			averageEnemy = new MapLocation(averageEnemyx/numEnemySoldiers,averageEnemyy/numEnemySoldiers);
		}
		retreat = false;
		
	}
	private static double rateTarget(RobotInfo r) {
		if(rc.getLocation().distanceSquaredTo(r.location)>10)
			return 0;
		if(r.type == RobotType.SOLDIER) {
			return 50 + 0.5*(100-r.health); 
		} else if(r.type == RobotType.PASTR) {
			return 5+(200-r.health)/10;
		} else if(r.type == RobotType.NOISETOWER) {
			return 1;
		} else {
			return 0;
		}
	}
	
	private static double distToNearest(MapLocation start, RobotInfo[] robots, int numRobots) {
		int dsq = 100000;
		for(int i = 0; i < numRobots; i++) {
			RobotInfo r = robots[i];
			int d = start.distanceSquaredTo(r.location);
			if(d < dsq) {
				dsq = d;
			}
		}
		return Math.sqrt(dsq);
	}
	
	private static int distToNearestSq(MapLocation start, RobotInfo[] robots, int numRobots) {
		int dsq = 100000;
		for(int i = 0; i < numRobots; i++) {
			RobotInfo r = robots[i];
			int d = start.distanceSquaredTo(r.location);
			if(d < dsq) {
				dsq = d;
			}
		}
		return dsq;
	}
	
	private static MapLocation nearestTo(MapLocation start, RobotInfo[] robots, int numRobots) {
		int dsq = 100000;
		MapLocation loc = null;
		for(int i = 0; i < numRobots;i++) {
			RobotInfo r = robots[i];
			int d = start.distanceSquaredTo(r.location);
			if(d < dsq) {
				dsq = d;
				loc = r.location;
			}
		}
		return loc;
	}
	
	private static int nearbyRobots(MapLocation start, RobotInfo[] robots,int numRobots, int distSq) {
		int close = 0;
		for(int i = 0; i < numRobots;i++) {
			RobotInfo r = robots[i];
			int d = start.distanceSquaredTo(r.location);
			if(d <= distSq) {
				close++;
			}
		}
		return close;
	}
	
	private static int adjacentRobots(MapLocation start, RobotInfo[] robots, int numRobots) {
		int close = 0;
		for(int i = 0; i < numRobots;i++) {
			RobotInfo r = robots[i];
			if(start.isAdjacentTo(r.location)) {
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
	
	private static Direction rotateDirectionBy(Direction d,int i) {
		return allDirections[(d.ordinal()+i+8)%8];
	}
	
	public static double rateMove(MapLocation oldLoc, MapLocation loc) {
		int oldDist = distToNearestSq(oldLoc, enemySoldiers,numEnemySoldiers);
		int dist = distToNearestSq(loc, enemySoldiers,numEnemySoldiers);
		
		if(healing) {
			return dist;
		}

		if(dist <= 10) {
			//return 0;
			return 100*favorableAdvance(loc);
		} else {
			return oldDist-dist; //how much closer we got
		}
	}
	
	private static Direction adjustFormation() throws GameActionException {
		Direction toNext = motion.getNextDirection();
		Direction bestd = Direction.NONE;
		double best = 0;
		for(int look : advanceLooks) {
			Direction d = rotateDirectionBy(toNext, look);
			if(rc.canMove(d)) {				
				double r = rateMove(rc.getLocation(),rc.getLocation().add(d));
				//System.out.println(""+d + " " + r);
				if(r > best) {
					best = r;
					bestd = d;
				}
			}
		}
		return bestd;		
	}
	
	private static Direction retreat() throws GameActionException {
		rc.setIndicatorString(1, "retreating");	
		Direction bestd = Direction.NONE;
		MapLocation nearest = averageEnemy;
		Direction toNearest = rc.getLocation().directionTo(nearest);
		double best = rateRetreatDirection(Direction.NONE,rc.getLocation(),nearest);
		//System.out.println(""+nearest + "" + toNearest);
		for(int look : retreatLooks) {
			Direction d = rotateDirectionBy(toNearest, look);
			if(BasicPathing.canMove(d, false, rc)) {
				double r = rateRetreatDirection(d, rc.getLocation(), nearest);
				if(r > best) {
					best = r;
					bestd = d;
				}
			}
		}
		return bestd;		
	}
	
	private static double rateRetreatDirection(Direction d, MapLocation square, MapLocation enemy) {
		double quality = -2*enemyDamageTo(square.add(d));
		if(quality + rc.getHealth() < 0) { //if we die taking this move it's bad;
			return -1000;
		}
		quality+=friendlyDamageTo(enemy.add(d));
		return quality;
		/*double quality = -enemyDamageTo(square.add(d));
		if(quality < 0) {
			return -1000;
		}
		quality+=friendlyDamageTo(enemy.add(d));
		return quality;*/
	}
	
	public static double enemyDamageTo(MapLocation loc) {
		int damage = 0;
		for(int i = 0; i < numEnemySoldiers; i++) {
			RobotInfo info = enemySoldiers[i];
			int dist = loc.distanceSquaredTo(info.location);
			if(dist <= 10) {
				damage += 10;
			}
		}
		return damage;
	}
	public static double friendlyDamageTo(MapLocation loc) {
		double damage = 0;
		for(int i = 0; i < numFriendlySoldiers; i++) {
			RobotInfo info = friendlySoldiers[i];
			int dist = loc.distanceSquaredTo(info.location);
			if(dist <= 10) {
				damage += 10;
			} else {
				damage += 50.0/dist;
			}
		}
		return damage;
	}
	
	public static MapLocation[] targets = new MapLocation[50];
	public static int numTargets = 0;
	public static boolean favorableShot() {
		numTargets = 0;
		int favorable = 1;
		double remainingHealth = rc.getHealth();
		for(int i = 0; i < numEnemySoldiers; i++) {
			RobotInfo einfo = enemySoldiers[i];
			if(rc.getLocation().distanceSquaredTo(einfo.location) <= 10) {
				--favorable;
				targets[numTargets] = einfo.location;
				numTargets++;
				remainingHealth -= 10;
				if(einfo.health <= 10) {
					favorable++;
				}
			}
		}
		for(int j = 0; j < numFriendlySoldiers; j++) {
			RobotInfo finfo = friendlySoldiers[j];
			for(int t = 0; t < numTargets; t++) {
				if(finfo.location.distanceSquaredTo(targets[t]) <= 10 && finfo.health > 10) {
					++favorable;
					break;
				}
			}
		}
		rc.setIndicatorString(0, ""+favorable +" " + remainingHealth);
		if(remainingHealth <= 0) {
			return false;
		}
		return favorable > 0;
	}
	
	public static int favorableAdvance(MapLocation loc) {
		numTargets = 0;
		int confidence = -2;
		int favorable = 1 + confidence;
		double remainingHealth = rc.getHealth();
		for(int i = 0; i < numEnemySoldiers; i++) {
			RobotInfo einfo = enemySoldiers[i];
			int dist = loc.distanceSquaredTo(einfo.location);
			if(dist <= 17) {
				favorable--;
				remainingHealth -= 10;
				if(dist <= 10) {
					targets[numTargets] = einfo.location;
					numTargets++;	
				}
			}
		}
		for(int j = 0; j < numFriendlySoldiers; j++) {
			RobotInfo finfo = friendlySoldiers[j];
			for(int t = 0; t < numTargets; t++) {
				if(finfo.location.distanceSquaredTo(targets[t]) <= 17) {
					favorable++;
					break;
				}
			}
		}
		if(remainingHealth <= 0 || favorable < 1) {
			return -1000;
		}
		return favorable;
	}
	
	/*public int sdQuality(MapLocation) {
		
	}*/
 	
	public void runSoldier() throws GameActionException {
		rc.setIndicatorString(0, "");
		rc.setIndicatorString(1, "");
		rc.setIndicatorString(2, "");
		rc.setIndicatorString(3, "");
		if(rc.getHealth() <= 10) { //manage healing state
			healing = true;
		} else if(rc.getHealth() > healTo) {
			healing = false;
		}
		if(healing) {
			rc.setIndicatorString(0, "HEALING");
		}
		if(rc.isActive()) {
			
			updateInformation();
			double highestRating = -10000;
			MapLocation highestTarget = null;		
			for(int i = 0; i < numEnemies; i++) {
				RobotInfo info = enemies[i];
				if(info.team == rc.getTeam().opponent()) {
					double rating = rateTarget(info);					
					if(rating > highestRating) {
						highestRating = rating;
						highestTarget = info.location;
					}
				}
			}
			retreat = !favorableShot();
			if(retreat) {
				Direction move = retreat();
				//System.out.println("try retreat");
				if(move != Direction.NONE && BasicPathing.canMove(move, false, rc)) {
					rc.move(move);
				} else {
					retreat = false;
				}
			}
			if(!retreat) {
				if(highestRating > 0) {
					rc.setIndicatorString(1, "fighting " + highestTarget);
					callForHelp(highestTarget);	
					if(rc.getLocation().distanceSquaredTo(highestTarget) <= 10) {
						rc.attackSquare(highestTarget);				
					}
				} else {
					if(numEnemySoldiers > 0) {
						Direction move = adjustFormation();
						if(move != Direction.NONE && BasicPathing.canMove(move, false, rc))
							rc.move(move);
					} else {				
						if(!helpFriends()) {
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
		} else {
			helpFriends(); //just to flush the buffer that we can't use
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
			//rc.setIndicatorString(1,"Helping " + nearestLoc);
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
