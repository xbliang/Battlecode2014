package lawrence;

// strategy: build 1 noise tower, build 1 PASTR, seek and destroy enemy PASTRs
// channel 0 gives info about what has been built
// channel 1 stores noise tower attack location
// channel 3 stores pastr hp
// channel 5 stores enemy PASTR location

import battlecode.common.*;
import java.util.ArrayList;

public class RobotPlayer{
	
	private static final Direction[] directions = {Direction.EAST,Direction.NORTH_EAST,Direction.NORTH,Direction.NORTH_WEST,Direction.WEST,Direction.SOUTH_WEST,Direction.SOUTH,Direction.SOUTH_EAST};
	private static RobotController rc;
	private static ArrayList<MapLocation> path;
	private static int bigBoxSize = 5;
	private static Direction allDirections[] = Direction.values();
	private static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	
	public static void run(RobotController rcIn){
		rc = rcIn;
		if(rc.getType()!=RobotType.HQ)
			BreadthFirst.init(rc, bigBoxSize);
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
	
	private static void runNoiseTower() throws GameActionException {
		int square = rc.readBroadcast(1); // an integer about which square to attack. spirals inwards, the greater "square" is the closer we shoot
		do {
			if(square/8 <= 8) // far away; use larger attacks
			{
				if(square % 2 == 0) { // cardinal direction
					if(rc.canAttackSquare(rc.getLocation().add(directions[square%8], 20 - square/8))) {
						rc.attackSquare(rc.getLocation().add(directions[square%8], 20 - square/8));
					}
				}
				else { // diagonal
					if(rc.canAttackSquare(rc.getLocation().add(directions[square%8], 14 - square/8))) {
						rc.attackSquare(rc.getLocation().add(directions[square%8], 14 - square/8));
					}
				}
			}
			else // near; use smaller attacks
			{
				if(rc.canAttackSquare(rc.getLocation().add(directions[2*(square%4)], 12 - (square-72)/4))) {
					rc.attackSquareLight(rc.getLocation().add(directions[2*(square%4)], 12 - (square-72)/4));
				}
			}
			square++;
			rc.broadcast(1, square % 108);
		} while(rc.isActive() && square > 0);
	}

	private static void runSoldier() throws GameActionException {
		if(!buildStructures() && !shoot() && !seekAndDestroy()) // short circuiting
			wander();
	}
	
	private static boolean seekAndDestroy() throws GameActionException {
		if(rc.readBroadcast(5) == 0) // no enemy PASTRs
			return false;
		else
		{
			if(path == null || path.size()==0) {
				MapLocation target = new MapLocation(rc.readBroadcast(5)/1024, rc.readBroadcast(5)%1024);
				path = BreadthFirst.pathTo(VectorFunctions.mldivide(rc.getLocation(),bigBoxSize), VectorFunctions.mldivide(target,bigBoxSize), 100000);
			}
			if(path != null && path.size() > 1) {
				Direction bdir = BreadthFirst.getNextDirection(path, bigBoxSize);
				BasicPathing.tryToMove(bdir, true, rc, directionalLooks, allDirections);
				return true;
			}
		}
		return false;
	}

	private static boolean buildStructures() throws GameActionException {
		int buildStatus = rc.readBroadcast(0); // 0: nothing built, 1: noise tower built, 2: noise tower + PASTR built
		if(buildStatus == 0 && rc.isActive()) { // build a noise tower
			rc.construct(RobotType.NOISETOWER);
			rc.broadcast(0, 1);
			return true;
		}
		if(buildStatus == 1 && rc.isActive()) { // build PASTR 
			rc.construct(RobotType.PASTR);
			rc.broadcast(0, 2);
			return true;
		}
		return false;
	}

	private static void wander() throws GameActionException {
		Direction allDirections[] = Direction.values();
		Direction chosenDirection = allDirections[(int)(Math.random()*8)];
		if(rc.isActive()&&rc.canMove(chosenDirection))
			rc.sneak(chosenDirection);
	}

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

	private static void runHQ() throws GameActionException {
		spawn();
		findPASTRs();
	}

	private static void findPASTRs() throws GameActionException { // finds the closest PASTR
		MapLocation[] enemyPASTRs = rc.sensePastrLocations(rc.getTeam().opponent());
		MapLocation HQLoc = rc.getLocation();
		int minDist = 65536;
		MapLocation closestPASTR = null;
		for(MapLocation pastr : enemyPASTRs) {
			if(pastr.distanceSquaredTo(HQLoc) < minDist) {
				closestPASTR = pastr;
				minDist = pastr.distanceSquaredTo(HQLoc);
			}
		}
		if(closestPASTR != null)
			rc.broadcast(5, closestPASTR.x*1024 + closestPASTR.y);
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