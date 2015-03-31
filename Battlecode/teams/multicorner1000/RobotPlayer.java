package multicorner1000;

import multicorner1000.BasicPathing;
import multicorner1000.MotionUnit;
import multicorner1000.NoiseTowerPattern;
import battlecode.common.*;

public class RobotPlayer {
	// constants
	private static int BUILD_NOISE_TOWER_TURN = 300;
	private static int BUILD_PASTR_TURN = 350;
	private final static int SECOND_PASTR_DELAY = 5;
	
	// channels
	private final static int FIRST_PASTR_CHANNEL = 1;			// contains index in corners of first PASTR location
	private final static int SECOND_PASTR_CHANNEL = 2;			// contains index in corners of second PASTR location
	private final static int ROLE_CHANNEL = 100;				// contains role of currently building unit
	private final static int NOISETOWER1_HEALTH_CHANNEL = 101;	// if nt1 alive, contains current round number; else, when it died
	private final static int NOISETOWER2_HEALTH_CHANNEL = 102;	// if nt2 alive, contains current round number; else, when it died
	private final static int PASTR1_HEALTH_CHANNEL = 103;		// if PASTR1 alive, contains current round number; else, when it died
	private final static int PASTR2_HEALTH_CHANNEL = 104;		// if PASTR2 alive, contains current round number; else, when it died
	
	// unit memory
	private static RobotController rc;
	private static MapLocation[] corners = new MapLocation[4];
	private static MapLocation middle;
	private static int width;
	private static int height;
	private static final Direction[] directions = {Direction.EAST,Direction.NORTH_EAST,Direction.NORTH,Direction.NORTH_WEST,Direction.WEST,Direction.SOUTH_WEST,Direction.SOUTH,Direction.SOUTH_EAST};
	
	// soldier memory
	private static MotionUnit motion;
	private static MapLocation target;
	private static MapLocation homeCorner;
	private static int homeCornerIndex;
	public enum Role {
		NONE, NOISETOWER1, NOISETOWER2, PASTR1, PASTR2, DEFENDER
	}
	private static Role role = Role.NONE;
	private static Direction allDirections[] = Direction.values();
	private static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};

	// HQ memory
	private static int numSoldiersSpawned = 0;
	private static MapLocation closestCorner;
	private static MapLocation secondClosestCorner;
	private static int closestCornerIndex;
	private static int secondClosestCornerIndex;
	
	// noise tower memory
	private static NoiseTowerPattern noiseTowerPattern;
	private final static int NOISETOWER_RANGE = 17;
	
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
		rc.setIndicatorString(0, role.name());
		if(rc.getType()==RobotType.HQ)
			runHQ();
		else if(rc.getType()==RobotType.SOLDIER)
			runSoldier();
		else if(rc.getType()==RobotType.NOISETOWER)
			runNoiseTower();
		else if(rc.getType()==RobotType.PASTR)
			runPastr();
	}

	private static void runNoiseTower() throws GameActionException {
		if (role.equals(Role.NOISETOWER1)) {
			rc.broadcast(NOISETOWER1_HEALTH_CHANNEL, Clock.getRoundNum());
		} else {
			rc.broadcast(NOISETOWER2_HEALTH_CHANNEL, Clock.getRoundNum());
		}		
		noiseTowerPattern.shootNext();
	}

	private static void runSoldier() throws GameActionException {
		if (!rc.isActive()) {
			return;
		}
		if (!performBuildingRole()) {	
			// I am a defender
			Robot enemies[] = rc.senseNearbyGameObjects(Robot.class, 10, rc.getTeam().opponent());
			if (enemies.length > 0) {
				rc.attackSquare(rc.senseLocationOf(enemies[0]));
				return;
			}
			checkStructureHealth();
		}
		// Move towards target, whatever that may be
		if (!(rc.getLocation().equals(target))) {
			if(motion.onPath && motion.goal.equals(target)) {
				Direction d = motion.getNextDirection(rc.getLocation());
				if (role == Role.DEFENDER && rc.getLocation().distanceSquaredTo(target) < 25) {
					BasicPathing.tryToSneak(d, true, rc, directionalLooks, allDirections);
				} else {
					BasicPathing.tryToMove(d, true, rc, directionalLooks, allDirections);
				}
			} else {
				motion.startPath(target);
			}
		}

	}
	
	private static boolean performBuildingRole() throws GameActionException {
		if ((role == Role.NOISETOWER1)) {
			rc.broadcast(NOISETOWER1_HEALTH_CHANNEL, Clock.getRoundNum());
			if (Clock.getRoundNum() >= BUILD_NOISE_TOWER_TURN || rc.getLocation().equals(target)) {
				// Time to make a noise tower
				rc.construct(RobotType.NOISETOWER);
				rc.broadcast(NOISETOWER1_HEALTH_CHANNEL, -Clock.getRoundNum());
			}
			return true;
		} else if (role == Role.NOISETOWER2) {
			rc.broadcast(NOISETOWER2_HEALTH_CHANNEL, Clock.getRoundNum());
			if (Clock.getRoundNum() >= BUILD_NOISE_TOWER_TURN + SECOND_PASTR_DELAY || rc.getLocation().equals(target)) {
				// Time to make a noise tower
				rc.construct(RobotType.NOISETOWER);
				rc.broadcast(NOISETOWER2_HEALTH_CHANNEL, -Clock.getRoundNum());
			}
			return true;
		} else if (role == Role.PASTR1) {
			rc.broadcast(PASTR1_HEALTH_CHANNEL, Clock.getRoundNum());
			if (Clock.getRoundNum() >= BUILD_PASTR_TURN) {
				// Time to make a PASTR
				rc.construct(RobotType.PASTR);
				rc.broadcast(PASTR1_HEALTH_CHANNEL, -Clock.getRoundNum());
			}
			return true;
		} else if (role == Role.PASTR2) {
			rc.broadcast(PASTR2_HEALTH_CHANNEL, Clock.getRoundNum());
			if (Clock.getRoundNum() >= BUILD_PASTR_TURN + SECOND_PASTR_DELAY){
				// Time to make a PASTR, slightly later for undefended one
				rc.broadcast(PASTR2_HEALTH_CHANNEL, -Clock.getRoundNum());
				rc.construct(RobotType.PASTR);
			}
			return true;
		}
		return false;
	}

	private static void checkStructureHealth() throws GameActionException {
		// Check for health of structures
		int currTurn = Clock.getRoundNum();
		int noisetower1Health = rc.readBroadcast(NOISETOWER1_HEALTH_CHANNEL);
		int pastr1Health = rc.readBroadcast(PASTR1_HEALTH_CHANNEL);
		if ((noisetower1Health > 0 && noisetower1Health < currTurn - 5) 	
		 || (noisetower1Health < 0 && noisetower1Health < -currTurn - 105)) {	// Need new noisetower 1
			role = Role.NOISETOWER1;
			rc.broadcast(NOISETOWER1_HEALTH_CHANNEL, Clock.getRoundNum());
			BUILD_NOISE_TOWER_TURN += Clock.getRoundNum();
			// Reset home
			homeCornerIndex = rc.readBroadcast(FIRST_PASTR_CHANNEL);
			homeCorner = corners[homeCornerIndex].add(corners[homeCornerIndex].directionTo(middle), 1);
			target = homeCorner;
		} else if ((pastr1Health > 0 && pastr1Health < currTurn - 5)
		 || (pastr1Health < 0 && pastr1Health < -currTurn - 55)) { 	// Need new PASTR 1
			role = Role.PASTR1;
			BUILD_PASTR_TURN += Clock.getRoundNum();
			rc.broadcast(PASTR1_HEALTH_CHANNEL, Clock.getRoundNum());
			// Reset home
			homeCornerIndex = rc.readBroadcast(FIRST_PASTR_CHANNEL);
			homeCorner = corners[homeCornerIndex].add(corners[homeCornerIndex].directionTo(middle), 2);
			target = homeCorner;
		}
	}

	private static void runHQ() throws GameActionException{
		if(rc.isActive() && !HQshoot()) // presumably the HQ always wants to be shooting/spawning whenever possible
			spawn(); // but if for some reason that's not true this can be changed
	}
	
	private static void runPastr() throws GameActionException {
		if (role.equals(Role.PASTR1)) {
			rc.broadcast(PASTR1_HEALTH_CHANNEL, Clock.getRoundNum());
		} else {
			rc.broadcast(PASTR2_HEALTH_CHANNEL, Clock.getRoundNum());
		}
		
	}

	private static void initUnit() throws GameActionException {
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		
		corners[0] = new MapLocation(0,0);
		corners[1] = new MapLocation(width-1, 0);
		corners[2] = new MapLocation(0, height-1);
		corners[3] = new MapLocation(width-1, height-1);
		middle = new MapLocation(width/2, height/2);
		
		if(rc.getType()==RobotType.HQ)
			initHQ();
		else if(rc.getType()==RobotType.SOLDIER)
			initSoldier();
		else if(rc.getType()==RobotType.NOISETOWER)
			initNoiseTower();
		else if(rc.getType()==RobotType.PASTR)
			initPastr();
	}

	private static void initNoiseTower() throws GameActionException {
		int i1 = rc.readBroadcast(FIRST_PASTR_CHANNEL);
		int i2 = rc.readBroadcast(SECOND_PASTR_CHANNEL);
		
		if (rc.getLocation().distanceSquaredTo(corners[i1]) < rc.getLocation().distanceSquaredTo(corners[i2])) {
			role = Role.NOISETOWER1;
		} else {
			role = Role.NOISETOWER2;
		}
		noiseTowerPattern = new NoiseTowerPattern(rc, NOISETOWER_RANGE, homeCorner);
	}

	private static void initSoldier() throws GameActionException {
		motion = new MotionUnit(rc);
		role = intToRole(rc.readBroadcast(ROLE_CHANNEL));
		// Set location of home corner, and set initial target to that location 
		switch(role) {
		case NOISETOWER1:
			homeCornerIndex = rc.readBroadcast(FIRST_PASTR_CHANNEL);
			// Can't target actual corners because of nav bug
			homeCorner = corners[homeCornerIndex].add(corners[homeCornerIndex].directionTo(middle), 1);
			target = homeCorner;
			break;
		case NOISETOWER2:
			homeCornerIndex = rc.readBroadcast(SECOND_PASTR_CHANNEL);
			homeCorner = corners[homeCornerIndex].add(corners[homeCornerIndex].directionTo(middle), 1);
			target = homeCorner;
			break;
		case PASTR1:
			homeCornerIndex = rc.readBroadcast(FIRST_PASTR_CHANNEL);
			homeCorner = corners[homeCornerIndex].add(corners[homeCornerIndex].directionTo(middle), 2);
			target = homeCorner;
			break;
		case PASTR2:
			homeCornerIndex = rc.readBroadcast(SECOND_PASTR_CHANNEL);
			homeCorner = corners[homeCornerIndex].add(corners[homeCornerIndex].directionTo(middle), 2);
			target = homeCorner;
			break;
		case DEFENDER:
			homeCornerIndex = rc.readBroadcast(FIRST_PASTR_CHANNEL);
			homeCorner = corners[homeCornerIndex].add(corners[homeCornerIndex].directionTo(middle), 4);
			target = homeCorner;
		}

		rc.setIndicatorString(1, homeCorner.toString());
	}
	
	private static Role intToRole(int n) {
		switch (n) {
		case 1: return Role.NOISETOWER1;
		case 2: return Role.NOISETOWER2;
		case 3: return Role.PASTR1;
		case 4: return Role.PASTR2;
		}
		// if not one of first four, go defend
		return Role.DEFENDER;
	}

	private static void initHQ() throws GameActionException {
		findClosestCorners();
		rc.broadcast(FIRST_PASTR_CHANNEL, closestCornerIndex);
		rc.broadcast(SECOND_PASTR_CHANNEL, secondClosestCornerIndex);
	}
	
	private static void initPastr() throws GameActionException {
		int i1 = rc.readBroadcast(FIRST_PASTR_CHANNEL);
		int i2 = rc.readBroadcast(SECOND_PASTR_CHANNEL);
		
		if (rc.getLocation().distanceSquaredTo(corners[i1]) < rc.getLocation().distanceSquaredTo(corners[i2])) {
			role = Role.PASTR1;
		} else {
			role = Role.PASTR2;
		}
	}

	private static void spawn() throws GameActionException {
		if(rc.senseRobotCount()<GameConstants.MAX_ROBOTS)
			for(Direction spawnDir : directions)
				if(rc.canMove(spawnDir)) {
					numSoldiersSpawned++;
					broadcastInfo();
					rc.spawn(spawnDir);
					return;
				}
	}
	
	private static void broadcastInfo() throws GameActionException {
		rc.broadcast(ROLE_CHANNEL, numSoldiersSpawned);
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

	private static void findClosestCorners() {
		// Sets the array closestCorners so that closestCorners[0] is the index of the closest corner to the HQ
		// and closestCorners[1] is the second-closest.
		MapLocation hq = rc.senseHQLocation();
		
		int[] distances = new int[4];
		distances[0] = hq.distanceSquaredTo(corners[0]);
		distances[1] = hq.distanceSquaredTo(corners[1]);
		distances[2] = hq.distanceSquaredTo(corners[2]);
		distances[3] = hq.distanceSquaredTo(corners[3]);
		
		closestCorner = corners[0];
		int[] closestTwoDists = {distances[0], 10000};
		
		for(int i = 1; i < 4; i++) {
			if (distances[i] < closestTwoDists[0]) {
				closestTwoDists[1] = closestTwoDists[0];
				closestTwoDists[0] = distances[i];
				secondClosestCorner = closestCorner;
				secondClosestCornerIndex = closestCornerIndex;
				closestCorner = corners[i];
				closestCornerIndex = i;
			} else if (distances[i] < closestTwoDists[1]) {
				closestTwoDists[1] = distances[i];
				secondClosestCorner = corners[i];
				secondClosestCornerIndex = i;
			}
		}
		
		return;
	}
	
	private static int locToInt(MapLocation m) {
		return 101*m.x + m.y + 1;
	}
	
	private static MapLocation intToLoc(int i) {
		return new MapLocation((i-1) / 101, (i-1) % 101);
	}
	
}