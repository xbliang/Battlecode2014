package lawrence1003NoisePattern;

// strategy: build 1 noise tower, build PASTRs, seek and destroy enemy PASTRs
// channel 0 noise tower build status
// channel 2 PASTR build status
// channel 1 stores noise tower attack location
// channel 3 stores pastr hp
// channel 5 stores enemy PASTR location

import battlecode.common.*;
import lawrence1003NoisePatternv2.MotionUnit;

import java.util.ArrayList;

public class RobotPlayer{
	
	private static final Direction[] directions = {Direction.EAST,Direction.NORTH_EAST,Direction.NORTH,Direction.NORTH_WEST,Direction.WEST,Direction.SOUTH_WEST,Direction.SOUTH,Direction.SOUTH_EAST};
	private static RobotController rc;
	private static ArrayList<MapLocation> path;
	private static int bigBoxSize = 5;
	private static Direction allDirections[] = Direction.values();
	private static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	private static MotionUnit motion;
	private static MapLocation target;
	private static NoiseTowerPattern noiseTowerPattern;
	
	public static void run(RobotController rcIn){
		rc = rcIn;
		if(rc.getType() == RobotType.SOLDIER) {
			try {
				motion = new MotionUnit(rc);
				buildStructures();
			} catch (GameActionException e1) {
				e1.printStackTrace();
			}
		} else if(rc.getType() == RobotType.NOISETOWER) {
			setupNoiseTower();
		}
		try {
			while(true){
				if(rc.getType()==RobotType.HQ)
					runHQ();
				else if(rc.getType()==RobotType.SOLDIER)
					runSoldier();
				else if(rc.getType()==RobotType.NOISETOWER)
					runNoiseTower();
				rc.yield();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void setupNoiseTower() {
		noiseTowerPattern = new NoiseTowerPattern(rc,100);
	}
	
	private static void runNoiseTower() throws GameActionException {
		noiseTowerPattern.shootNext();
		/*int square = rc.readBroadcast(1); // an integer about which square to attack. spirals inwards, the greater "square" is the closer we shoot
		do {
			if(square/8 <= 8) // far away; use larger attacks
			{
				if(square % 2 == 0) { // cardinal direction
					MapLocation target = rc.getLocation().add(directions[square%8], 20 - square/8);
					if(target.x >= 0 && target.y >= 0 && target.x < rc.getMapWidth() && target.y < rc.getMapHeight() && rc.canAttackSquare(target)) {
						rc.attackSquare(target);
					}
				}
				else { // diagonal
					MapLocation target = rc.getLocation().add(directions[square%8], 14 - square/8);
					if(target.x >= 0 && target.y >= 0 && target.x < rc.getMapWidth() && target.y < rc.getMapHeight() && rc.canAttackSquare(target)) {
						rc.attackSquare(target);
					}
				}
			}
			else // near; use smaller attacks
			{
				MapLocation target = rc.getLocation().add(directions[2*(square%4)], 11 - (square-72)/4);
				if(target.x >= 0 && target.y >= 0 && target.x < rc.getMapWidth() && target.y < rc.getMapHeight() && rc.canAttackSquare(target)) {
					rc.attackSquareLight(target);
				}
			}
			square++;
			rc.broadcast(1, square % 104);
		} while(rc.isActive() && square > 0);*/
	}

	private static void runSoldier() throws GameActionException {
		//if(!shoot() && !seekAndDestroy()) // short circuiting
			//wander();
		if(!shoot())
			seekAndDestroy();
	}
	
	private static boolean seekAndDestroy() throws GameActionException {
		if(rc.readBroadcast(5) == 0) { // no enemy PASTRs
			motion.done();
			return false;
		}
		else
		{
			target = new MapLocation(rc.readBroadcast(5)/1024, rc.readBroadcast(5)%1024);
			if(motion.onPath && motion.goal.equals(target)) {
				BasicPathing.tryToMove(motion.getNextDirection(rc.getLocation()), true, rc, directionalLooks, allDirections);
			} else
			{
				motion.startPath(target);
			}
		}
		return false;
	}

	private static boolean buildStructures() throws GameActionException {
		boolean noiseExists = false, pastrExists = false;
		for(Robot robot : rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam())) {
			if(rc.senseRobotInfo(robot).type == RobotType.NOISETOWER)
				noiseExists = true;
			if(rc.senseRobotInfo(robot).type == RobotType.PASTR)
				pastrExists = true;
		}
		if(Clock.getRoundNum() > rc.readBroadcast(0) && !noiseExists && rc.isActive()) { // build a noise tower
			rc.construct(RobotType.NOISETOWER);
			rc.broadcast(0, Clock.getRoundNum()+101);
			return true;
		}
		if(Clock.getRoundNum() > rc.readBroadcast(2) && !pastrExists && rc.isActive()) { // build PASTR 
			rc.construct(RobotType.PASTR);
			rc.broadcast(2, Clock.getRoundNum()+51);
			return true;
		}
		return false;
	}

	/*private static void wander() throws GameActionException {
		Direction allDirections[] = Direction.values();
		Direction chosenDirection = allDirections[(int)(Math.random()*8)];
		if(rc.isActive()&&rc.canMove(chosenDirection))
			rc.sneak(chosenDirection);
	}*/

	private static boolean shoot() throws GameActionException {
		// attack PASTRs first
		for(MapLocation enemyPASTR : rc.sensePastrLocations(rc.getTeam().opponent())){ // attack the first PASTR in range
			if(enemyPASTR.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared && rc.isActive()) {
				rc.attackSquare(enemyPASTR);
				return true;
			}
		}
		// attack anything else that isn't an HQ
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		for(Robot anEnemy : enemyRobots){ // attack the first enemy in range
			RobotInfo anEnemyInfo = rc.senseRobotInfo(anEnemy);
			if(anEnemyInfo.type != RobotType.HQ && anEnemyInfo.location.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared && rc.isActive()) {
				rc.attackSquare(anEnemyInfo.location);
				return true;
			}
		}
		return false;
	}

	private static boolean shootFromHQ() throws GameActionException { // rc.isActive() commented out for sprint round glitch
		// attack PASTRs first
		for(MapLocation enemyPASTR : rc.sensePastrLocations(rc.getTeam().opponent())){ // attack the first PASTR in range
			if(enemyPASTR.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared/* && rc.isActive()*/) {
				rc.attackSquare(enemyPASTR);
				return true;
			}
		}
		for(MapLocation enemyPASTR : rc.sensePastrLocations(rc.getTeam().opponent())) // splash
			for(MapLocation adjacentSquares : MapLocation.getAllMapLocationsWithinRadiusSq(enemyPASTR, 2)) {
				if(adjacentSquares.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared/* && rc.isActive()*/) {
					rc.attackSquare(adjacentSquares);
					return true;
				}
			}
		// attack anything else that isn't an HQ
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		for(Robot anEnemy : enemyRobots){ // attack the first enemy in range
			RobotInfo anEnemyInfo = rc.senseRobotInfo(anEnemy);
			if(anEnemyInfo.type != RobotType.HQ && anEnemyInfo.location.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared /*&& rc.isActive()*/) {
				rc.attackSquare(anEnemyInfo.location);
				return true;
			}
		}
		for(Robot anEnemy : enemyRobots) { // splash
			RobotInfo anEnemyInfo = rc.senseRobotInfo(anEnemy);
			for(MapLocation adjacentSquares : MapLocation.getAllMapLocationsWithinRadiusSq(anEnemyInfo.location, 2)) {
				if(anEnemyInfo.type != RobotType.HQ && adjacentSquares.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared /*&& rc.isActive()*/) {
					rc.attackSquare(adjacentSquares);
					return true;
				}
			}
		}
		return false;
	}

	private static void runHQ() throws GameActionException {
		findPASTRs();
		if(!shootFromHQ())
			spawn();;
	}

	private static void findPASTRs() throws GameActionException { // finds the closest PASTR
		MapLocation[] enemyPASTRs = rc.sensePastrLocations(rc.getTeam().opponent());
		MapLocation HQLoc = rc.getLocation();
		int minDist = 65536;
		MapLocation closestTarget = null;
		for(MapLocation pastr : enemyPASTRs) {
			if(pastr.distanceSquaredTo(HQLoc) < minDist) {
				closestTarget = pastr;
				minDist = pastr.distanceSquaredTo(HQLoc);
			}
		}
		if(closestTarget != null)
			rc.broadcast(5, closestTarget.x*1024 + closestTarget.y);
		else
			rc.broadcast(5,0);
	}

	public static void spawn() throws GameActionException {
		if(rc.isActive()&&rc.senseRobotCount()<GameConstants.MAX_ROBOTS){
			for(Direction spawnDir : directions){
				if(rc.canMove(spawnDir)){
					rc.spawn(spawnDir);
					break;
				}
			}
		}
	}
	
}