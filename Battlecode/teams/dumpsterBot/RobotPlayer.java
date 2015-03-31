package dumpsterBot;

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
	
	private static RobotController rc;
	private static ArrayList<MapLocation> path;
	private static Direction allDirections[] = Direction.values();
	private static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	private static MotionUnit motion;
	private static MapLocation target = new MapLocation(0,0);
	private static NoiseTowerPattern noiseTowerPattern;
	public static boolean outnumbered = false;
	public static boolean combatStatus = false;
	public static int readPos = 99;
	public static final int helpRange = 100;
	private static SoldierController soldierController;
	
	public static void run(RobotController rcIn){
		rc = rcIn;

		try {
			if(rc.getType()==RobotType.HQ)
				setupHQ();
			else if(rc.getType()==RobotType.SOLDIER)
				setupSoldier();
			
			while(true){
				if(rc.getType()==RobotType.HQ)
					runHQ();
				else if(rc.getType()==RobotType.SOLDIER)
					runSoldier();
				rc.yield();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void standAside() throws GameActionException {
		if(rc.isActive())
			BasicPathing.tryToMove(rc.getLocation().directionTo(rc.senseHQLocation()), false, rc, directionalLooks, allDirections);
		while(!rc.isActive())
			rc.yield();
	}
	
	private static void setupSoldier() throws GameActionException {
		soldierController = new SoldierController(rc,0);
	}
	private static void runSoldier() throws GameActionException {
		soldierController.runSoldier();
		
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

	private static boolean seekAndDestroy() throws GameActionException { // now goes after 1) cries for help. 2) enemy PASTRs
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
			BasicPathing.tryToMove(motion.getNextDirection(), true, rc, directionalLooks, allDirections);
		} else {
			motion.startPath(target);
		}
		return true;
		//}
	}
	
	

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

	private static void setupHQ() throws GameActionException {
		rc.broadcast(0, Util.locToInt(rc.senseHQLocation()));
		rc.broadcast(99, 100);
	}
	
	private static void runHQ() throws GameActionException {
		rollOut();
		spawn();
		if(rc.senseRobotCount() >= 10) {
			//MapLocation dest = new MapLocation((rc.senseEnemyHQLocation().x + rc.senseHQLocation().x)/2,(rc.senseEnemyHQLocation().y+rc.senseHQLocation().y)/2);
			MapLocation dest = rc.senseEnemyHQLocation();
			rc.broadcast(0, Util.locToInt(dest));
		}
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
			for(Direction spawnDir : allDirections){
				if(rc.canMove(spawnDir)){
					rc.spawn(spawnDir);
					return;
				}
			}
		}
	}
}