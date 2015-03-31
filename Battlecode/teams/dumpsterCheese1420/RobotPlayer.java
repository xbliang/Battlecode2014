package dumpsterCheese1420;

import battlecode.common.*;

public class RobotPlayer {
	// constants (declare broadcast channels here i.e. int BROADCAST_CHANNEL = 0)
	public static final Direction[] allDirections = Direction.values();
	public static final int TARGET_CHANNEL = 0;
	public static final int NOISE_BUILD_CHANNEL = 1;
	public static final int PASTR_BUILD_CHANNEL = 2;
	public static final int PASTR_LOCATION_CHANNEL = 3;
	public final static int FIRST_PASTR_CHANNEL = 4;			// contains first PASTR location
	//public final static int SECOND_PASTR_CHANNEL = 5;			// contains second PASTR location
	public static final int WRITE_POS_CHANNEL = 99;
	public static final int SOLDIER_ATTACK_RANGE = RobotType.SOLDIER.attackRadiusMaxSquared;
	public static final int SOLDIER_SENSOR_RANGE = RobotType.SOLDIER.sensorRadiusSquared;
	public static int pastrTime;
	
	// unit memory
	private static RobotController rc;
	private static SoldierController soldierController;
	
	// soldier memory
//	private static MotionUnit motion;
//	private static MapLocation target = new MapLocation(0,0);
	public static boolean combatStatus = false;
	public static int readPos = 99;
	
	// HQ memory
	private static MapLocation closestCorner;
	//private static MapLocation secondClosestCorner;
	private static int closestCornerIndex;
	private static MapLocation[] corners = new MapLocation[4];
	private static MapLocation middle;
	private static int width;
	private static int height;
	private static double[][] cowGrowth;
	private static double maxGrowth = 0;
	private static MapLocation bestSquare;
	private static int startColumn,startRow,dx,dy;
	
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
		else if(rc.getType()==RobotType.PASTR)
			runPASTR();
	}

	private static void runNoiseTower() throws GameActionException {
		rc.broadcast(NOISE_BUILD_CHANNEL, Clock.getRoundNum()+2);
		noiseTowerPattern.shootNext();
	}

	private static void runPASTR() throws GameActionException {
		rc.broadcast(PASTR_BUILD_CHANNEL, Clock.getRoundNum()+2);
	}

	private static void runSoldier() throws GameActionException {
		soldierController.runSoldier();
	}
	
	private static void runHQ() throws GameActionException{
		if(rc.isActive() && !HQshoot()) // presumably the HQ always wants to be shooting/spawning whenever possible
			spawn(); // but if for some reason that's not true this can be changed
		findPASTRs();
		updatePastrLocations();
	}
	
	private static void updatePastrLocations() throws GameActionException {
		if(startColumn < 0 || startColumn >= width)
			return;
		int x = startColumn;
		int y = startRow;
		startColumn+=dx;
		while(x >= 0 && x < width && y >= 0 && y < height)
		{
			MapLocation loc = new MapLocation(x,y);
			if(rc.senseTerrainTile(loc) != TerrainTile.VOID && cowGrowth[x][y] >= maxGrowth) {
				maxGrowth = cowGrowth[x][y];
				bestSquare = loc;
			}
			x += dx;
			y += dy;
		}
		if(bestSquare.x == 0 || bestSquare.y == 0 || bestSquare.x == width - 1 || bestSquare.y == height - 1)
			bestSquare = bestSquare.add(bestSquare.directionTo(middle));
		rc.broadcast(FIRST_PASTR_CHANNEL,Util.locToInt(bestSquare));
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
		if(enemyPASTRs.length == 1) {
			closestTarget = enemyPASTRs[0];
		}
		if(closestTarget != null)
			rc.broadcast(TARGET_CHANNEL, Util.locToInt(closestTarget));
		else if(rc.senseRobotCount() < 10)
			rc.broadcast(TARGET_CHANNEL, Util.locToInt(rc.senseHQLocation()));
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

	private static void initNoiseTower() throws GameActionException {
		noiseTowerPattern = new SpiralPatternReachable(rc, 15);
	}

	private static void initSoldier() throws GameActionException {
		soldierController = new SoldierController(rc,TARGET_CHANNEL);
	}

	private static void initHQ() throws GameActionException {
		rc.wearHat();
		rc.broadcast(TARGET_CHANNEL, Util.locToInt(rc.senseHQLocation()));
		rc.broadcast(WRITE_POS_CHANNEL, 99);
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		int mapSize = height + width;
		pastrTime = pastrTimeOf(mapSize);
		rc.broadcast(NOISE_BUILD_CHANNEL, pastrTime-100);
		rc.broadcast(PASTR_BUILD_CHANNEL, pastrTime);
		cowGrowth = rc.senseCowGrowth();
		corners[0] = new MapLocation(0,0);
		corners[1] = new MapLocation(width-1, 0);
		corners[2] = new MapLocation(0, height-1);
		corners[3] = new MapLocation(width-1, height-1);
		middle = new MapLocation(width/2, height/2);
		findClosestCorners();
		initPastrLocations();
	}
	
	private static int pastrTimeOf(int mapSize){
		return 1000 - 4*mapSize;
	}
	
	private static void initPastrLocations() throws GameActionException {
		rc.broadcast(FIRST_PASTR_CHANNEL, Util.locToInt(closestCorner.add(closestCorner.directionTo(middle))));
	}

	private static boolean validPastrLoc(MapLocation m) {
		switch(rc.senseTerrainTile(m)) {
		case ROAD:
		case NORMAL:
			return !m.equals(rc.senseHQLocation()) && (cowGrowth[m.x][m.y] > 0);
		default:
			break;
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
		closestCornerIndex = 0;
		bestSquare = closestCorner.add(closestCorner.directionTo(middle));
		int[] closestTwoDists = {distances[0], 10000};
		
		for(int i = 1; i < 4; i++) {
			if (distances[i] < closestTwoDists[0]) {
				closestTwoDists[1] = closestTwoDists[0];
				closestTwoDists[0] = distances[i];
				closestCorner = corners[i];
				closestCornerIndex = i;
			}
		}
		
		switch(closestCornerIndex)
		{
		case 0:
			startColumn = width - 1;
			startRow = 0;
			dx = -1;
			dy = 1;
			break;
		case 1:
			startColumn = 0;
			startRow = 0;
			dx = 1;
			dy = 1;
			break;
		case 2:
			startColumn = width - 1;
			startRow = height - 1;
			dx = -1;
			dy = -1;
			break;
		case 3:
			startColumn = 0;
			startRow = height-1;
			dx = 1;
			dy = -1;
			break;
		}
		
		return;
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
