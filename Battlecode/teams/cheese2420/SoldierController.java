package cheese2420;

import battlecode.common.*;

public class SoldierController {
	private static RobotController rc;
	private static MotionUnit motion;
	private static MapLocation target = new MapLocation(0,0);
	public static int readPos = 99;
	public static final int helpRange = 100;
	private static Direction allDirections[] = Direction.values();
	private static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	private int targetChannel = 0;
	private boolean noiseBuilder = false;
	private boolean pastrBuilder = false;
	private int startRound;
	private MapLocation buildSpot;
	private final static MapLocation ORIGIN = new MapLocation(0,0);
	
	public SoldierController(RobotController r,int t) throws GameActionException {
		rc = r;
		buildSpot = Util.intToLoc(rc.readBroadcast(RobotPlayer.FIRST_PASTR_CHANNEL));
		motion = new MotionUnit(rc);
		motion.startPath(Util.intToLoc(rc.readBroadcast(RobotPlayer.TARGET_CHANNEL)));
		targetChannel = t;
		startRound = Clock.getRoundNum();
		if(startRound > rc.readBroadcast(RobotPlayer.NOISE_BUILD_CHANNEL)) {
			rc.broadcast(RobotPlayer.NOISE_BUILD_CHANNEL,startRound + 302);
			noiseBuilder = true;
			Robot[] alliedUnits = rc.senseNearbyGameObjects(Robot.class, 20000, rc.getTeam());
			for(Robot unit : alliedUnits) {
				if(rc.senseRobotInfo(unit).type == RobotType.NOISETOWER)
					noiseBuilder = false;
			}
		} else if(startRound > rc.readBroadcast(RobotPlayer.PASTR_BUILD_CHANNEL) && rc.sensePastrLocations(rc.getTeam()).length == 0) {
			rc.broadcast(RobotPlayer.PASTR_BUILD_CHANNEL,startRound + 252);
			pastrBuilder = true;
		}
	}
	
	private static void retreatFrom(MapLocation averageEnemy) throws GameActionException {
		Direction d = allDirections[(rc.getLocation().directionTo(averageEnemy).ordinal()+4)%8];
		BasicPathing.tryToMove(d, false, rc, directionalLooks, allDirections);	
	}
	
	private static double rateTarget(RobotInfo r) {
		double idist = rc.getLocation().distanceSquaredTo(r.location)-RobotPlayer.SOLDIER_ATTACK_RANGE;
		if(idist < 0) {
			idist = 0;
		}
		if(r.type == RobotType.SOLDIER) {
			return 100-r.health + 2*(25 - (idist)); 
		} else if(r.type == RobotType.PASTR) {
			if(idist > 0) {
				return 0;
			} else 
			return 51+(200-r.health)/10;
		} else if(r.type == RobotType.NOISETOWER) {
			return 1;
		} else {
			return 0;
		}
	}
	
	public void runSoldier() throws GameActionException {
		// boolean combatStatus = false;
		// boolean retreating = false;
		MapLocation myLoc = rc.getLocation();
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class,35);
		
		double highestRating = -10000;
		MapLocation highestTarget = null;		
		
		int friendly=1;
		double friendlyTurns = rc.getHealth();
		//MapLocation averageFriendly = myLoc;
		
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
				if(info.type == RobotType.PASTR) {
					for(MapLocation loc : MapLocation.getAllMapLocationsWithinRadiusSq(info.location, 5)) {
						if(rc.canSenseSquare(loc)) {
							rating = (rc.senseCowsAtLocation(loc)-500)/10.0;					
							if(rating > highestRating) {
								highestRating = rating;
								highestTarget = loc;
							}
						}
					}
				}
			}
			
			if(info.type == RobotType.SOLDIER) {
				if(info.team == rc.getTeam().opponent()) {
					++enemy;					
					enemyTurns += info.health;
					averageEnemy = averageEnemy.add(info.location.x,info.location.y);
					if(info.location.distanceSquaredTo(myLoc)<=25) {
						enemiesInRange++;
					}
				} else {
					++friendly;
					friendlyTurns += info.health;
					//averageFriendly = averageFriendly.add(info.location.x,info.location.y);
				}
			}
		}
		//averageFriendly = new MapLocation(averageFriendly.x/friendly,averageFriendly.y/friendly);
		if(enemy > 0) {
			averageEnemy = new MapLocation(averageEnemy.x/enemy,averageEnemy.y/enemy);
			friendlyTurns *= (0.1)/enemy;
		}
		enemyTurns *= (0.1)/friendly;
		if(enemy != 0 && (enemyTurns > friendlyTurns + 1)) {			
			retreatFrom(averageEnemy);
			rc.setIndicatorString(2, "retreating");
			//System.out.println(helpNear(averageFriendly,averageEnemy));
			callForHelp(averageEnemy);
		} else if(highestRating > 0) {
			rc.setIndicatorString(2, "fighting");
			if(rc.isActive() && myLoc.distanceSquaredTo(highestTarget) <= 10) {
				rc.attackSquare(highestTarget);
			} else {
				BasicPathing.tryToMove(myLoc.directionTo(highestTarget), false, rc, directionalLooks, allDirections);
			}
			callForHelp(averageEnemy);
		} else {
			if(noiseBuilder || pastrBuilder) {
				int currentRound = Clock.getRoundNum();
				if(rc.isActive() && (myLoc.distanceSquaredTo(buildSpot) < 3 || currentRound > startRound + 200 || currentRound > 1600)) {
					if(noiseBuilder)
						rc.construct(RobotType.NOISETOWER);
					else if(pastrBuilder) {
						rc.construct(RobotType.PASTR);
						rc.broadcast(RobotPlayer.PASTR_LOCATION_CHANNEL, Util.locToInt(myLoc));
					}
					return;
				}
				if(!buildSpot.equals(target)) {
					target = buildSpot;
					motion.startPath(buildSpot);
				}
				motion.tryNextMove();
				return;
			}
			if(!helpFriends()) {
				rc.setIndicatorString(2, "Moving to target");
				//BasicPathing.tryToMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()), true, rc, directionalLooks, allDirections);
				MapLocation dest = Util.intToLoc(rc.readBroadcast(targetChannel));
				if(!dest.equals(target)) {
					target = dest;
					motion.startPath(dest);
				}
				if(myLoc.distanceSquaredTo(target) < 69)
					motion.tryNextSneak();
				else
					motion.tryNextMove();
			}
		}		
	}
	

	private static void callForHelp(MapLocation enemy) throws GameActionException {
		int writePos = (rc.readBroadcast(RobotPlayer.WRITE_POS_CHANNEL) + 1);
		if(writePos >= 200) {
			writePos -= 100;
		}
		rc.broadcast(writePos, Util.locToInt(enemy));
		rc.broadcast(RobotPlayer.WRITE_POS_CHANNEL, writePos);
	}
	
	private static boolean helpFriends() throws GameActionException  {
		int readThrough = rc.readBroadcast(RobotPlayer.WRITE_POS_CHANNEL);
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
		if(nearestLoc != null && !nearestLoc.equals(ORIGIN)) {
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
