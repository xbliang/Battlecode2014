package dumpsterBotv8;

import battlecode.common.*;

public class RobotPlayer {
	// constants (declare broadcast channels here i.e. int BROADCAST_CHANNEL = 0)
	private static final Direction[] directions = {Direction.EAST,Direction.NORTH_EAST,Direction.NORTH,Direction.NORTH_WEST,Direction.WEST,Direction.SOUTH_WEST,Direction.SOUTH,Direction.SOUTH_EAST};
	private static final int TARGET_CHANNEL = 0;
	private static final int FOCUS_CHANNEL = 1;
	private static final int CONFIDENCE_CHANNEL = 2;
	private static final int HELP_CHANNEL = 99;
	private static final int LEADER_CHANNEL = 1000;
	// unit memory
	private static RobotController rc;
	
	// soldier memory
	private static SoldierController soldierController;
	
	// HQ memory
	
	// noise tower memory
	
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
		// TODO Auto-generated method stub
		
	}

	private static void runSoldier() throws GameActionException {
		soldierController.runSoldier();
		
	}

	private static void runHQ() throws GameActionException{
		rc.broadcast(LEADER_CHANNEL, Util.locToInt(new MapLocation(-1,-1)));
		if(rc.isActive() && !HQshoot()) // presumably the HQ always wants to be shooting/spawning whenever possible
			spawn(); // but if for some reason that's not true this can be changed
		setTarget();
	}

	private static void setTarget() throws GameActionException { // finds the closest PASTR/
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
			if(pastr.distanceSquaredTo(averageLoc) < minDist && pastr.distanceSquaredTo(rc.senseEnemyHQLocation()) > 50) {
				closestTarget = pastr;
				minDist = pastr.distanceSquaredTo(averageLoc);
			}
		}
		if(enemyPASTRs.length == 1) {
			closestTarget = enemyPASTRs[0];
		}
		if(closestTarget != null)
			rc.broadcast(TARGET_CHANNEL, Util.locToInt(closestTarget));
		//else if(rc.readBroadcast(PASTR_LOCATION_CHANNEL) != 0)
		//	rc.broadcast(TARGET_CHANNEL, rc.readBroadcast(PASTR_LOCATION_CHANNEL));
		else if(rc.senseRobotCount() >= 10)
			rc.broadcast(TARGET_CHANNEL, Util.locToInt(rc.senseEnemyHQLocation()));
		/*else
			rc.broadcast(TARGET_CHANNEL, Util.locToInt(rc.senseHQLocation()));*/
	}
	
	/*private static void findPASTRs() throws GameActionException { // finds the closest PASTR
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
	}*/
	
	private static void initUnit() throws GameActionException {
		if(rc.getType()==RobotType.HQ)
			initHQ();
		else if(rc.getType()==RobotType.SOLDIER)
			initSoldier();
		else if(rc.getType()==RobotType.NOISETOWER)
			initNoiseTower();
	}

	private static void initNoiseTower() {
		// TODO Auto-generated method stub
		
	}

	private static void initSoldier() throws GameActionException {
		soldierController = new SoldierController(rc,TARGET_CHANNEL,FOCUS_CHANNEL,CONFIDENCE_CHANNEL);
		
	}

	private static void initHQ() throws GameActionException {
		rc.broadcast(HELP_CHANNEL, 99);
		rc.broadcast(TARGET_CHANNEL, Util.locToInt(rc.senseHQLocation()));
		rc.broadcast(LEADER_CHANNEL, Util.locToInt(new MapLocation(-1,-1)));
		rc.broadcast(FOCUS_CHANNEL,0);
		rc.broadcast(CONFIDENCE_CHANNEL,-2);
	}
	
	private static void spawn() throws GameActionException {
		if(rc.senseRobotCount()<GameConstants.MAX_ROBOTS)
			for(Direction spawnDir : directions)
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