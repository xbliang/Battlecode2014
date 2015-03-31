package dumpsterBotv8;

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
	private static int readPos = 99;
	private static final int helpRange = 100;
	private static Direction allDirections[] = Direction.values();
	private static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	private static int targetChannel = 0;
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
	private static int advanceLooksKill[] = new int[]{0,1,2,3,4,5,6,7};
	private static int advanceLooksFocus[] = new int[]{-1,0,1};
	private static boolean focusOn;
	private static boolean healing = false;
	private static int healTo = 20;
	private static int focusChannel;
	private static int confidenceChannel;
	private static int confidence = -2;
	
	private static boolean GOINGIN;
	private static MapLocation BOOMIT;
	
	//private static int confidenceChannel;
	
	
	
	public SoldierController(RobotController r,int t,int fc, int cc) throws GameActionException {
		rc = r;
		motion = new MotionUnit(rc);
		motion.startPath(Util.intToLoc(rc.readBroadcast(0)));
		targetChannel = t;
		focusChannel = fc;
		confidenceChannel = cc;
		//confidenceChannel = cc;
		friendlySoldiers = new RobotInfo[500];
		enemySoldiers = new RobotInfo[500];
		enemies = new RobotInfo[500];
		focusOn = false;
		GOINGIN = false;
	}
	
	
	public static void checkFocus() throws GameActionException {
		int focus = rc.readBroadcast(focusChannel);
		if(focus == 1) {
			focusOn = true;
		} else {
			focusOn = false;
		}
	}
	
	public static void checkTarget() throws GameActionException {
		MapLocation dest = Util.intToLoc(rc.readBroadcast(targetChannel));
		if(!dest.equals(target)) {
			target = dest;
			motion.startPath(dest);
			motion.updateWaypoints();
		}
	}
	
	public static void checkConfidence() throws GameActionException {
		int c = rc.readBroadcast(confidenceChannel);
		if(c <= 0 && c >= -5) {
			confidence = c;
		}
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
	
	private static double adjacentSDDamage(MapLocation start, RobotInfo[] robots, int numRobots,double damage) {
		double close = 0;
		for(int i = 0; i < numRobots;i++) {
			RobotInfo r = robots[i];
			if(start.isAdjacentTo(r.location)) {
				if(damage > r.health) {
					close+=r.health;
				} else {
					close+=damage;
				}
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
			return 100*favorableShot(loc,17,confidence);
			//return 100*favorableShotLocation(loc, 17, -5);
		} else {
			return oldDist-dist; //how much closer we got
		}
	}
	
	private static Direction adjustFormation() throws GameActionException {
		Direction toNext = motion.getNextDirection();
		Direction bestd = Direction.NONE;
		double best = 0;
		int[] advanceLooks = null;
		if(focusOn && !healing) {
			if(toNext == null) {
				return Direction.NONE;
			}
			best = -100;
			advanceLooks = advanceLooksFocus;
			for(int look : advanceLooks) {
				Direction d = rotateDirectionBy(toNext, look);
				if(rc.canMove(d)) {				
					double r = rateMove(rc.getLocation(),rc.getLocation().add(d));
					if(r > best) {
						best = r;
						bestd = d;
					}
				}
			}
			return bestd;	
		} else {
			for(int i = 0; i < 8; i++) {
				Direction d = allDirections[i];
				if(rc.canMove(d)) {				
					double r = rateMove(rc.getLocation(),rc.getLocation().add(d));
					if(r > best) {
						best = r;
						bestd = d;
					}
				}
			}
			return bestd;	
		}
		
			
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
	
	public static int favorableShot(MapLocation loc, int reinforceDist,int confidence) {
		numTargets = 0;
		int favorable = 1 + confidence;
		double remainingHealth = rc.getHealth();
		for(int i = 0; i < numEnemySoldiers; i++) {
			RobotInfo einfo = enemySoldiers[i];
			int dist = loc.distanceSquaredTo(einfo.location);
			if(dist <= reinforceDist) {
				favorable--;
				remainingHealth -= 10;
				if(einfo.health <= 10) {
					favorable++;
				}
				if(dist <= 10) {
					targets[numTargets] = einfo.location;
					numTargets++;
					
				}
			}
		}
		for(int j = 0; j < numFriendlySoldiers; j++) {
			RobotInfo finfo = friendlySoldiers[j];
			for(int t = 0; t < numTargets; t++) {
				if(finfo.location.distanceSquaredTo(targets[t]) <= reinforceDist && finfo.health > 10) {
					favorable++;
					break;
				}
			}
		}
		if(remainingHealth <= 0) {
			return -1000;
		}
		return favorable;
	}
	
	public boolean canMakeDetonation(MapLocation p,MapLocation loc) {
		double remainingHealth = rc.getHealth();
		while(true) {
			if(motion.getMapData(p.x, p.y) != 1) {
				return false;
			}
			if(!p.equals(loc)) {
				p = p.add(p.directionTo(loc));
				remainingHealth -= enemyDamageTo(p);
				if(remainingHealth <= 0) {
					return false;
				}
			} else {
				return true;
			}
			
		}
	}
	/*public int sdQuality(MapLocation loc) {
		int enemyHits = adjacentRobots(loc, enemySoldiers, numEnemySoldiers);
		int enemyHits = adjacentRobots(loc, enemySoldiers, numEnemySoldiers);
		int friendlyHits = adjacentRobots(loc, friendlySoldiers, numFriendlySoldiers);
		return enemyHits-friendlyHits;
		/*boolean canReach = canMakeDetonation(rc.getLocation(), loc);
		if(canReach) {
			return enemyHits - friendlyHits;
		} else {
			return 0;
		}
	}*/
	private static double sdQuality(MapLocation loc,double health) {
		double damage = 41+health/2;
		double enemyDamage = adjacentSDDamage(loc,enemySoldiers,numEnemySoldiers,damage);
		double friendlyDamage = adjacentSDDamage(loc,friendlySoldiers,numFriendlySoldiers,damage);
		return enemyDamage - friendlyDamage;
	}
 	
	public void runSoldier() throws GameActionException {
		rc.setIndicatorString(0, "");
		rc.setIndicatorString(1, "");
		rc.setIndicatorString(2, "");
		rc.setIndicatorString(3, "");
		checkTarget(); //update target information
		if(GOINGIN) {
			if(sdQuality(rc.getLocation(),rc.getHealth()) >= 100) {
				rc.selfDestruct();
			} else {
				GOINGIN = false;
			}
		}
		
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
			
			MapLocation potential = rc.getLocation();
			for(int i = 0; i < 1; i++) {
				Direction d = rc.getLocation().directionTo(averageEnemy);
				potential = rc.getLocation().add(d);
				double remaining = rc.getHealth()-enemyDamageTo(potential);
				double quality = sdQuality(potential,remaining);
				if(quality >= 100 && remaining > 0 && BasicPathing.canMove(d, false, rc)) {
					rc.setIndicatorString(0, "SD");
					System.out.println("BOOMIT");
					GOINGIN = true;
					rc.move(d);
				}
			}
			
			if(!GOINGIN) {
				double highestRating = -10000;
				MapLocation highestTarget = null;		
				for(int i = 0; i < numEnemies; i++) {
					RobotInfo info = enemies[i];
					double rating = rateTarget(info);
					if(info.team == rc.getTeam().opponent()) {
						if(rating > highestRating) {
							highestRating = rating;
							highestTarget = info.location;
						}
					}
					if(info.type == RobotType.PASTR) {
						for(MapLocation loc : MapLocation.getAllMapLocationsWithinRadiusSq(info.location, 5)) {
							if(rc.canAttackSquare(loc)) {
								rating = (rc.senseCowsAtLocation(loc)-500)/10.0;					
								if(rating > highestRating) {
									highestRating = rating;
									highestTarget = loc;
								}
							}
						}
					}
				}
				retreat = favorableShot(rc.getLocation(),10,0)<1;
				if(retreat) {
					Direction move = retreat();
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
							if(move != Direction.NONE && BasicPathing.canMove(move, false, rc)) {
								rc.move(move);
							} else {
								checkTarget();
							}
						} else {				
							if(!helpFriends()) {
												
								motion.tryNextMove();
							}
						}
					}
				}
			} 
		} else {
			checkFocus(); //do this passive check in downtime
			helpFriends(); //just to flush the buffer that we can't use
			//checkTarget(); //update target information
			checkConfidence();
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
