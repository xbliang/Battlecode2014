package lawrence1004;

// strategy: build 1 noise tower, build PASTRs, seek and destroy enemy PASTRs
// channel 0 noise tower build status
// channel 1 PASTR location
// channel 2 PASTR build status
// channel 3 stores cry for help
// channel 4 signals robots to roll out
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
	private static MotionUnit motion;
	private static MapLocation target;
	private static NoiseTowerPattern noiseTowerPattern;
	public static boolean outnumbered = false;
	
	public static void run(RobotController rcIn){
		rc = rcIn;
		if(rc.getType() == RobotType.SOLDIER) {
			try {
				motion = new MotionUnit(rc);
				standAside(); // in case there's only one square next to the HQ that isn't a wall
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
//			e.printStackTrace();
		}
	}

	private static void standAside() throws GameActionException {
		if(rc.isActive())
			BasicPathing.tryToMove(rc.getLocation().directionTo(rc.senseHQLocation()), false, rc, directionalLooks, allDirections);
		while(!rc.isActive())
			rc.yield();
	}

	private static void setupNoiseTower() {
		noiseTowerPattern = new NoiseTowerPattern(rc,20);
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
		cryForHelp();
		if(!shootCows() && !shoot()) // short circuiting
			seekAndDestroy();
	}

	private static boolean shootCows() throws GameActionException {
		for(MapLocation loc : MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), rc.getType().attackRadiusMaxSquared)) {
			if(rc.isActive() && worthShooting(loc)) {
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
			if(p.distanceSquaredTo(loc) <= 25) // in enemy PASTR range; worth
				return true;
		return false;
	}

	private static void cryForHelp() throws GameActionException { // cry 
		int broadcast = rc.readBroadcast(3);
		MapLocation currentCry = new MapLocation((broadcast - 1)/1024, (broadcast - 1)%1024);
		if(broadcast != 0) { // clear cries for neutralized enemies
			rc.setIndicatorString(0,currentCry.toString());
			if(rc.canSenseSquare(currentCry)) { // look at the square for which the cry was issued
				GameObject enemy = rc.senseObjectAtLocation(currentCry);
				if(enemy == null || enemy.getTeam() == rc.getTeam()) // if there's no enemy robot there
					rc.broadcast(3,0); // cry is no longer relevant
			}
		}
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		MapLocation closestThreat = null;
		MapLocation HQLoc = rc.senseHQLocation();
		int minDist = 65536;
		int numHostiles = 0;
		if(rc.readBroadcast(3) != 0)
			minDist = currentCry.distanceSquaredTo(HQLoc);
		for(Robot anEnemy : enemyRobots){
			RobotInfo anEnemyInfo = rc.senseRobotInfo(anEnemy);
			if(anEnemyInfo.type == RobotType.SOLDIER)
				numHostiles++;
			if(anEnemyInfo.type != RobotType.HQ && anEnemyInfo.location.distanceSquaredTo(HQLoc) < minDist) {
				closestThreat = anEnemyInfo.location;
				minDist = closestThreat.distanceSquaredTo(HQLoc);
			}
		}
		outnumbered = numHostiles > 0 && rc.getLocation().distanceSquaredTo(HQLoc) > 2 && numHostiles > rc.senseNearbyGameObjects(Robot.class,rc.getType().sensorRadiusSquared,rc.getTeam()).length;
		if(closestThreat != null)
			rc.broadcast(3, closestThreat.x*1024 + closestThreat.y + 1);
	}

	private static boolean seekAndDestroy() throws GameActionException { // now goes after 1) cries for help. 2) enemy PASTRs
		/*if(rc.readBroadcast(3) == 0 && rc.readBroadcast(5) == 0) { // nothing to chase
			motion.done();
			return false;
		}
		else
		{*/
		if(outnumbered)
			target = rc.senseHQLocation();
		else if(rc.readBroadcast(3) != 0)
			target = new MapLocation((rc.readBroadcast(3) - 1)/1024, (rc.readBroadcast(3) - 1)%1024);
		else if(rc.readBroadcast(5) != 0)
			target = new MapLocation((rc.readBroadcast(5) - 1)/1024, (rc.readBroadcast(5) - 1)%1024);
		else if(rc.readBroadcast(4) != 0)
			target = rc.senseEnemyHQLocation();
		else
			target = rc.senseHQLocation();
		if(motion.onPath && motion.goal.equals(target)) {
			BasicPathing.tryToMove(motion.getNextDirection(rc.getLocation()), true, rc, directionalLooks, allDirections);
		} else
		{
			motion.startPath(target);
		}
		return true;
		//}
	}
	public static int locToInt(MapLocation m){
		 return (m.x*100 + m.y);
	}
	public static MapLocation intToLoc(int i){
		 return new MapLocation(i/100,i%100);
	}

	private static boolean buildStructures() throws GameActionException {
		boolean noiseExists = false, pastrExists = false;
		for(Robot robot : rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam())) {
			if(rc.senseRobotInfo(robot).type == RobotType.NOISETOWER)
				noiseExists = true;
			if(rc.senseRobotInfo(robot).type == RobotType.PASTR)
				pastrExists = true;
		}
		if(Clock.getRoundNum() >= rc.readBroadcast(0) && !noiseExists && rc.isActive()) { // build a noise tower
			rc.construct(RobotType.NOISETOWER);
			rc.broadcast(0, Clock.getRoundNum()+102);
			return true;
		}
		if(Clock.getRoundNum() >= rc.readBroadcast(2) && !pastrExists && rc.isActive()) { // build PASTR 
			rc.construct(RobotType.PASTR);
			rc.broadcast(2, Clock.getRoundNum()+52);
			rc.broadcast(1, locToInt(rc.getLocation()));
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
		if(outnumbered)
			return false;
		int broadcast = rc.readBroadcast(3);
		if(broadcast != 0) { // help the cry first
			MapLocation cry = new MapLocation((broadcast - 1)/1024, (broadcast - 1)%1024);
			if(cry.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared && rc.isActive()) {
				rc.attackSquare(cry);
				return true;
			}
		}
		// attack anything that isn't an HQ
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
		// attack anything that isn't an HQ
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
		rollOut();
		cryForHelp();
		findPASTRs();
		if(!shootFromHQ())
			spawn();
	}

	private static void rollOut() throws GameActionException {
		if(rc.senseRobotCount() > GameConstants.MAX_ROBOTS/3)
			rc.broadcast(4,1);
		else
			rc.broadcast(4,0);
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
			rc.broadcast(5, closestTarget.x*1024 + closestTarget.y + 1);
		else
			rc.broadcast(5,0);
	}

	public static void spawn() throws GameActionException {
		if(rc.isActive()&&rc.senseRobotCount()<GameConstants.MAX_ROBOTS){
			for(Direction spawnDir : directions){
				if(rc.canMove(spawnDir)){
					rc.spawn(spawnDir);
					return;
				}
			}
		}
	}
}