package cheese1000;

// template for basic robot player
// sample code at the end that'll probably be used in every bot

import battlecode.common.*;

public class RobotPlayer {
	// constants (declare broadcast channels here i.e. int BROADCAST_CHANNEL = 0)
	private static final Direction[] allDirections = Direction.values();
	private static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	public static final int TARGET_CHANNEL = 0;
	public static final int NOISE_BUILD_CHANNEL = 1;
	public static final int PASTR_BUILD_CHANNEL = 2;
	public static final int WRITE_POS_CHANNEL = 99;
	public static final int SOLDIER_ATTACK_RANGE = RobotType.SOLDIER.attackRadiusMaxSquared;
	public static final int SOLDIER_SENSOR_RANGE = RobotType.SOLDIER.sensorRadiusSquared;
	
	// unit memory
	private static RobotController rc;
	private static SoldierController soldierController;
	
	// soldier memory
	private static MotionUnit motion;
	private static MapLocation target = new MapLocation(0,0);
	public static boolean combatStatus = false;
	public static int readPos = 99;
	
	// HQ memory
	
	// noise tower memory
	private static NoiseTowerPattern noiseTowerPattern;
	
	public static void run(RobotController rcIn){
		rc = rcIn; // store the RobotController so every function can refer to it
		try
		{
			initUnit(); // run precomputations
			rc.yield();
			while(true) {
				runUnit(); // code to run each round
				rc.yield();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static void runUnit() throws GameActionException {
		if(rc.getType()==RobotType.HQ)
			runHQ();
		else if(rc.getType()==RobotType.SOLDIER)
			runSoldier();
		else if(rc.getType()==RobotType.NOISETOWER)
			runNoiseTower();
	}

	private static void runNoiseTower() {
		noiseTowerPattern.shootNext();
	}

	private static void runSoldier() throws GameActionException {

		soldierController.runSoldier();
		/*if(!rc.isActive())
			return;
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
			if(info.type == RobotType.SOLDIER) {
				if(info.team == rc.getTeam().opponent()) {
					++enemy;
					double rating = rateTarget(info);					
					if(rating > highestRating) {
						highestRating = rating;
						highestTarget = info.location;
					}
					
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
		if(enemy == 0){
			combatStatus = false;
		} else if(enemyDamage > friendlyDamage || !helpNear(averageFriendly,averageEnemy)) {
			//retreat();
			Direction d = allDirections[(rc.getLocation().directionTo(averageEnemy).ordinal()+4)%8];
			BasicPathing.tryToMove(d, false, rc, directionalLooks, allDirections);			
			combatStatus = false;
			rc.setIndicatorString(2, "retreating");
			callForHelp(averageEnemy);
			return;
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
				if(rc.getLocation().distanceSquaredTo(highestTarget) <= SOLDIER_ATTACK_RANGE) {
					rc.attackSquare(highestTarget);
				} else {
					BasicPathing.tryToMove(rc.getLocation().directionTo(highestTarget), false, rc, directionalLooks, allDirections);
				}
			} else {
				BasicPathing.tryToMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()), false, rc, directionalLooks, allDirections);
			}
			return;
		} else {
			if(shootCows())
				return;
			MapLocation dest = Util.intToLoc(rc.readBroadcast(TARGET_CHANNEL));
			if(!dest.equals(target)) {
				target = dest;
				motion.startPath(dest);
			}
			if(shootTarget())
				return;
			if(!helpFriends()) {
				rc.setIndicatorString(2, "Moving to target");
				//BasicPathing.tryToMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()), true, rc, directionalLooks, allDirections);
				motion.tryNextMove();
			}
		}*/
	}

	/*private static boolean helpFriends() throws GameActionException  {
		int readThrough = rc.readBroadcast(WRITE_POS_CHANNEL);
		int readEnd = readThrough;
		if(readEnd < readPos) {
			readEnd += 100;
		}
		//System.out.println(readThrough);
		//System.out.println(readEnd);
		
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
	
	private static void callForHelp(MapLocation enemy) throws GameActionException {
		int writePos = (rc.readBroadcast(WRITE_POS_CHANNEL) + 1);
		if(writePos >= 200) {
			writePos -= 100;
		}
		//System.out.println("Call for help " + writePos +": " + enemy);
		rc.broadcast(writePos, Util.locToInt(enemy));
		rc.broadcast(WRITE_POS_CHANNEL, writePos);
	}
	
	private static boolean helpNear(MapLocation avgFriendly, MapLocation avgEnemy) {
		//System.out.println(avf+" " + ave);
		return avgFriendly.distanceSquaredTo(avgEnemy) < 30;
	}
	
	
	private static double rateTarget(RobotInfo r) {
		double idist = rc.getLocation().distanceSquaredTo(r.location)-SOLDIER_ATTACK_RANGE;
		if(idist < 0) {
			idist = 0;
		}
		return 100-r.health - (idist); 
	}
	
	private static boolean shootTarget() throws GameActionException {
		if(rc.canAttackSquare(target)) {
			rc.attackSquare(target);
			return true;
		}
		return false;
	}
	
	private static boolean shootCows() throws GameActionException {
		for(MapLocation loc : MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), SOLDIER_ATTACK_RANGE)) {
			if(worthShooting(loc)) {
				rc.attackSquare(loc);
				return true;
			}
		}
		return false;
	}
	
	private static boolean worthShooting(MapLocation loc) throws GameActionException {
		if(rc.senseCowsAtLocation(loc) < 1000) // not enough cows; not worth
			return false;
		for(MapLocation p : rc.sensePastrLocations(rc.getTeam().opponent()))
			if(p.distanceSquaredTo(loc) <= 100) // in enemy PASTR range; worth
				return true;
		return false;
	}*/
	
	private static void runHQ() throws GameActionException{
		if(rc.senseRobotCount() >= 5)
			findPASTRs();
		if(rc.isActive() && !HQshoot()) // presumably the HQ always wants to be shooting/spawning whenever possible
			spawn(); // but if for some reason that's not true this can be changed
	}
	
	private static void findPASTRs() throws GameActionException { // finds the closest PASTR/
		Robot[] army = rc.senseNearbyGameObjects(Robot.class, 20000, rc.getTeam());
		int count = 0;
		MapLocation averageLoc = new MapLocation(0,0);
		for(Robot r : army) {
			RobotInfo info = rc.senseRobotInfo(r);
			if(info.type == RobotType.SOLDIER) {
				++count;
				averageLoc = averageLoc.add(info.location.x,info.location.y);
			}
		}
		if(count > 0)
			averageLoc = new MapLocation(averageLoc.x/count,averageLoc.y/count);
		MapLocation[] enemyPASTRs = rc.sensePastrLocations(rc.getTeam().opponent());
		int minDist = 65536;
		MapLocation closestTarget = null;
		for(MapLocation pastr : enemyPASTRs) {
			if(pastr.distanceSquaredTo(averageLoc) < minDist && pastr.distanceSquaredTo(rc.senseEnemyHQLocation()) > 15) {
				closestTarget = pastr;
				minDist = pastr.distanceSquaredTo(averageLoc);
			}
		}
		if(closestTarget != null)
			rc.broadcast(TARGET_CHANNEL, Util.locToInt(closestTarget));
		else
			rc.broadcast(TARGET_CHANNEL, Util.locToInt(rc.senseEnemyHQLocation()));
	}
	
	private static void initUnit() throws GameActionException {
		if(rc.getType()==RobotType.HQ)
			initHQ();
		else if(rc.getType()==RobotType.SOLDIER)
			initSoldier();
		else if(rc.getType()==RobotType.NOISETOWER)
			initNoiseTower();
	}

	private static void initNoiseTower() {
		noiseTowerPattern = new NoiseTowerPattern(rc,16);
	}

	private static void initSoldier() throws GameActionException {
		soldierController = new SoldierController(rc,TARGET_CHANNEL);
	}

	private static void initHQ() throws GameActionException {
		rc.broadcast(TARGET_CHANNEL, Util.locToInt(rc.senseHQLocation()));
		rc.broadcast(WRITE_POS_CHANNEL, 100);
		rc.broadcast(NOISE_BUILD_CHANNEL, 750);
		rc.broadcast(PASTR_BUILD_CHANNEL, 800);
	}
	
	private static void spawn() throws GameActionException {
		if(rc.senseRobotCount()<GameConstants.MAX_ROBOTS)
			for(Direction spawnDir : allDirections)
				if(rc.canMove(spawnDir)) {
					rc.spawn(spawnDir);
					return;
				}
	}

	private static boolean HQshoot() throws GameActionException{ // returns whether the HQ shot this turn
		// get a list of enemies
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		for(Robot anEnemy : enemyRobots){ // loop through enemies
			RobotInfo anEnemyInfo = rc.senseRobotInfo(anEnemy);
			if(rc.canAttackSquare(anEnemyInfo.location)) { // attack if possible
				rc.attackSquare(anEnemyInfo.location);
				return true;
			}
		}
		// if an enemy wasn't attacked directly, try to get splash dmg
		for(Robot anEnemy : enemyRobots) {
			RobotInfo anEnemyInfo = rc.senseRobotInfo(anEnemy);
			for(MapLocation adjacentSquare : MapLocation.getAllMapLocationsWithinRadiusSq(anEnemyInfo.location, 2)) {
				if(rc.canAttackSquare(adjacentSquare)) {
					rc.attackSquare(adjacentSquare);
					return true;
				}
			}
		}
		return false;
	}


}