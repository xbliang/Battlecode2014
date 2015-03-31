package dumpsterCheese420420;

import java.util.Arrays;

//import dumpsterCheese420.ntp.*;
import battlecode.common.*;

public class RobotPlayer {
	// constants (declare broadcast channels here i.e. int BROADCAST_CHANNEL = 0)
	public static final Direction[] allDirections = Direction.values();
	public static final int TARGET_CHANNEL = 0;
	public static final int NOISE_BUILD_CHANNEL = 1;
	public static final int PASTR_BUILD_CHANNEL = 2;
	public static final int PASTR_LOCATION_CHANNEL = 3;
	public static final int FIRST_PASTR_CHANNEL = 4;			// contains first PASTR location
	public static final int FOCUS_CHANNEL = 5;
	public static final int CONFIDENCE_CHANNEL = 6;
	public static final int PASTR_URGENCY_CHANNEL = 7;
	public static final int NOISE_URGENCY_CHANNEL = 8;
	public static final int DESPERATION_CHANNEL = 9;
	//public final static int SECOND_PASTR_CHANNEL = 5;			// contains second PASTR location
	public static final int WRITE_POS_CHANNEL = 99;
	public static final int MATRIX_CHANNEL = 50000;
	public static final int SOLDIER_ATTACK_RANGE = RobotType.SOLDIER.attackRadiusMaxSquared;
	public static final int SOLDIER_SENSOR_RANGE = RobotType.SOLDIER.sensorRadiusSquared;
	
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
	public static int pastrTime;
	public static int rallyPoint;
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
	private static int[][] distToTarget;
	private static boolean startedMatrix = false, foundBestSpot = false;
	private static QueueOfMapLocations expandQueue = new QueueOfMapLocations();
	
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
		rc.broadcast(NOISE_BUILD_CHANNEL, Clock.getRoundNum()+10);
		rc.broadcast(NOISE_URGENCY_CHANNEL, 0);
		Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		for(Robot enemy : nearbyEnemies) {
			callForHelp(rc.senseRobotInfo(enemy).location);
			rc.broadcast(NOISE_URGENCY_CHANNEL, 1);
		}
		noiseTowerPattern.shootNext();
	}

	private static void runPASTR() throws GameActionException {
		rc.broadcast(PASTR_BUILD_CHANNEL, Clock.getRoundNum()+5);
		rc.broadcast(PASTR_URGENCY_CHANNEL, 0);
		Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		for(Robot enemy : nearbyEnemies) {
			callForHelp(rc.senseRobotInfo(enemy).location);
			rc.broadcast(PASTR_URGENCY_CHANNEL, 1);
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
	
	private static void runSoldier() throws GameActionException {
		soldierController.runSoldier();
	}
	
	private static void runHQ() throws GameActionException{
		if(rc.isActive() && !HQshoot()) // presumably the HQ always wants to be shooting/spawning whenever possible
			spawn(); // but if for some reason that's not true this can be changed
		findPASTRs();
		if(!foundBestSpot)
			updatePastrLocations();
		else {
			if(!startedMatrix) {
				expandQueue.add(bestSquare);
				distToTarget[bestSquare.x][bestSquare.y] = 1;
				startedMatrix = true;
			}
			while(Clock.getBytecodesLeft() > 1000 && expandQueue.size() > 0 && expandQueue.tail < 120000)
				updatePathMatrix();
		}
	}
	
	private static void updatePathMatrix() throws GameActionException {
		MapLocation toExpand = expandQueue.poll();
		MapLocation enemyHQ = rc.senseEnemyHQLocation();
		int currLen = distToTarget[toExpand.x][toExpand.y];
		rc.broadcast(MATRIX_CHANNEL+Util.locToInt(toExpand), currLen);
		MapLocation nw = toExpand.add(Direction.NORTH_WEST);
		if(rc.senseTerrainTile(nw) == TerrainTile.NORMAL && nw.distanceSquaredTo(enemyHQ) > 25 && (currLen + 28 < distToTarget[nw.x][nw.y] || distToTarget[nw.x][nw.y] < 1)) {
			distToTarget[nw.x][nw.y] = currLen + 28;
			expandQueue.add(nw);
		} else if(rc.senseTerrainTile(nw) == TerrainTile.ROAD && nw.distanceSquaredTo(enemyHQ) > 25 &&  (currLen + 14 < distToTarget[nw.x][nw.y] || distToTarget[nw.x][nw.y] < 1)) {
			distToTarget[nw.x][nw.y] = currLen + 14;
			expandQueue.add(nw);
		}
		MapLocation sw = toExpand.add(Direction.SOUTH_WEST);
		if(rc.senseTerrainTile(sw) == TerrainTile.NORMAL && sw.distanceSquaredTo(enemyHQ) > 25 &&  (currLen + 28 < distToTarget[sw.x][sw.y] || distToTarget[sw.x][sw.y] < 1)) {
			distToTarget[sw.x][sw.y] = currLen + 28;
			expandQueue.add(sw);
		} else if(rc.senseTerrainTile(sw) == TerrainTile.ROAD && sw.distanceSquaredTo(enemyHQ) > 25 &&  (currLen + 14 < distToTarget[sw.x][sw.y] || distToTarget[sw.x][sw.y] < 1)) {
			distToTarget[sw.x][sw.y] = currLen + 14;
			expandQueue.add(sw);
		}
		MapLocation ne = toExpand.add(Direction.NORTH_EAST);
		if(rc.senseTerrainTile(ne) == TerrainTile.NORMAL && ne.distanceSquaredTo(enemyHQ) > 25 &&  (currLen + 28 < distToTarget[ne.x][ne.y] || distToTarget[ne.x][ne.y] < 1)) {
			distToTarget[ne.x][ne.y] = currLen + 28;
			expandQueue.add(ne);
		} else if(rc.senseTerrainTile(ne) == TerrainTile.ROAD && ne.distanceSquaredTo(enemyHQ) > 25 && (currLen + 14 < distToTarget[ne.x][ne.y] || distToTarget[ne.x][ne.y] < 1)) {
			distToTarget[ne.x][ne.y] = currLen + 14;
			expandQueue.add(ne);
		}
		MapLocation se = toExpand.add(Direction.SOUTH_EAST);
		if(rc.senseTerrainTile(se) == TerrainTile.NORMAL && se.distanceSquaredTo(enemyHQ) > 25 && (currLen + 28 < distToTarget[se.x][se.y] || distToTarget[se.x][se.y] < 1)) {
			distToTarget[se.x][se.y] = currLen + 28;
			expandQueue.add(se);
		} else if(rc.senseTerrainTile(se) == TerrainTile.ROAD && se.distanceSquaredTo(enemyHQ) > 25 && (currLen + 14 < distToTarget[se.x][se.y] || distToTarget[se.x][se.y] < 1)) {
			distToTarget[se.x][se.y] = currLen + 14;
			expandQueue.add(se);
		}
		MapLocation n = toExpand.add(Direction.NORTH);
		if(rc.senseTerrainTile(n) == TerrainTile.NORMAL && n.distanceSquaredTo(enemyHQ) > 25 && (currLen + 20 < distToTarget[n.x][n.y] || distToTarget[n.x][n.y] < 1)) {
			distToTarget[n.x][n.y] = currLen + 20;
			expandQueue.add(n);
		} if(rc.senseTerrainTile(n) == TerrainTile.ROAD && n.distanceSquaredTo(enemyHQ) > 25 && (currLen + 10 < distToTarget[n.x][n.y] || distToTarget[n.x][n.y] < 1)) {
			distToTarget[n.x][n.y] = currLen + 10;
			expandQueue.add(n);
		}
		MapLocation w = toExpand.add(Direction.WEST);
		if(rc.senseTerrainTile(w) == TerrainTile.NORMAL && w.distanceSquaredTo(enemyHQ) > 25 && (currLen + 20 < distToTarget[w.x][w.y] || distToTarget[w.x][w.y] < 1)) {
			distToTarget[w.x][w.y] = currLen + 20;
			expandQueue.add(w);
		} else if(rc.senseTerrainTile(w) == TerrainTile.ROAD && w.distanceSquaredTo(enemyHQ) > 25 && (currLen + 10 < distToTarget[w.x][w.y] || distToTarget[w.x][w.y] < 1)) {
			distToTarget[w.x][w.y] = currLen + 10;
			expandQueue.add(w);
		}
		MapLocation e = toExpand.add(Direction.EAST);
		if(rc.senseTerrainTile(e) == TerrainTile.NORMAL && e.distanceSquaredTo(enemyHQ) > 25 && (currLen + 20 < distToTarget[e.x][e.y] || distToTarget[e.x][e.y] < 1)) {
			distToTarget[e.x][e.y] = currLen + 20;
			expandQueue.add(e);
		} else if(rc.senseTerrainTile(e) == TerrainTile.ROAD && e.distanceSquaredTo(enemyHQ) > 25 && (currLen + 10 < distToTarget[e.x][e.y] || distToTarget[e.x][e.y] < 1)) {
			distToTarget[e.x][e.y] = currLen + 10;
			expandQueue.add(e);
		}
		MapLocation s = toExpand.add(Direction.SOUTH);
		if(rc.senseTerrainTile(s) == TerrainTile.NORMAL && s.distanceSquaredTo(enemyHQ) > 25 && (currLen + 20 < distToTarget[s.x][s.y] || distToTarget[s.x][s.y] < 1)) {
			distToTarget[s.x][s.y] = currLen + 20;
			expandQueue.add(s);
		} else if(rc.senseTerrainTile(s) == TerrainTile.ROAD && s.distanceSquaredTo(enemyHQ) > 25 && (currLen + 10 < distToTarget[s.x][s.y] || distToTarget[s.x][s.y] < 1)) {
			distToTarget[s.x][s.y] = currLen + 10;
			expandQueue.add(s);
		}
		////rc.setIndicatorString(0,expandQueue.size()+"");
	}

	private static void updatePastrLocations() throws GameActionException {
		if(startColumn < 0 || startColumn >= width) {
			foundBestSpot = true;
			return;
		}
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
		//if(bestSquare.x == 0 || bestSquare.y == 0 || bestSquare.x == width - 1 || bestSquare.y == height - 1)
			//bestSquare = bestSquare.add(bestSquare.directionTo(middle));
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
		
		if(Clock.getRoundNum() > pastrTime - 100 && (Clock.getRoundNum() > rc.readBroadcast(NOISE_BUILD_CHANNEL) || Clock.getRoundNum() > rc.readBroadcast(PASTR_BUILD_CHANNEL))) {
			rc.broadcast(TARGET_CHANNEL, Util.locToInt(bestSquare));
			rc.broadcast(FOCUS_CHANNEL, 1);
		} else if(closestTarget != null) {
			rc.broadcast(TARGET_CHANNEL, Util.locToInt(closestTarget));
			rc.broadcast(FOCUS_CHANNEL, 1);
		} else if(Clock.getRoundNum() > pastrTime - 100) {
			rc.broadcast(TARGET_CHANNEL, Util.locToInt(bestSquare));
			rc.broadcast(FOCUS_CHANNEL, 0);
		} else { //if(rc.senseRobotCount() >= 10)
			rc.broadcast(TARGET_CHANNEL, Util.locToInt(rc.senseEnemyHQLocation()));
			rc.broadcast(FOCUS_CHANNEL, 0);
		}
		//else
		//	rc.broadcast(TARGET_CHANNEL, rallyPoint);
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
		soldierController = new SoldierController(rc,TARGET_CHANNEL,FOCUS_CHANNEL,CONFIDENCE_CHANNEL);
	}

	private static void initHQ() throws GameActionException {
		rc.wearHat();
		rc.broadcast(TARGET_CHANNEL, Util.locToInt(rc.senseHQLocation()));
		rc.broadcast(CONFIDENCE_CHANNEL, -2);
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
		MapLocation a = rc.senseHQLocation();
		MapLocation b = rc.senseEnemyHQLocation();
		rallyPoint = Util.locToInt(new MapLocation((a.x*2 + b.x)/3, (a.y*2 + b.y)/3));
		distToTarget = new int[width][height];
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
		bestSquare = closestCorner;//.add(closestCorner.directionTo(middle));
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
