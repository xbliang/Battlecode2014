package splitpusher0420;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class HQController {
	
	private static RobotController rc;
	private static int w,h;
	private static MapLocation center;
	private static double[][] spawnRates;
	private static boolean[][] alreadySearched;
	public static MapLocation corners[] = new MapLocation[4];
	
	public HQController(RobotController r) throws GameActionException {
		rc = r;
		w = rc.getMapWidth();
		h = rc.getMapHeight();
		corners[0] = new MapLocation(0,0);
		corners[1] = new MapLocation(w-1,0);
		corners[2] = new MapLocation(w-1,h-1);
		corners[3] = new MapLocation(0,h-1);
		center = new MapLocation(w/2,h/2);
		spawnRates = rc.senseCowGrowth();
		rc.broadcast(Constants.PASTR_BUILD_STATUS_1,9999);
		rc.broadcast(Constants.PASTR_BUILD_STATUS_2,9999);
		rc.broadcast(Constants.PASTR_BUILD_STATUS_3,9999);
		rc.broadcast(Constants.PASTR_BUILD_STATUS_4,9999);
		alreadySearched = new boolean[w][h];
		broadcastPastrLocations();
	}
	
	private static void broadcastPastrLocations() throws GameActionException {
		int diml = w > h ? w/2 : h/2;
		int dims = w < h ? w/2 : h/2;
		for(int i = 0; i < 4; i++) {
			MapLocation start = corners[i];
			MapLocation next = corners[(i+1)%4];
			MapLocation loc1 = start, loc2 = start;
			outerloop:
			for(int k = 0; k < dims; k++) {
				loc2 = loc1;
				for(int j = 0; j < diml; j++) {
					loc2 = loc2.add(loc2.directionTo(center));
					if(alreadySearched[loc2.x][loc2.y])
						break;
					if(spawnRates[loc2.x][loc2.y] > 0 && loc2.distanceSquaredTo(rc.senseEnemyHQLocation()) > 25)
						break outerloop;
					alreadySearched[loc2.x][loc2.y] = true;
				}
				loc1 = loc1.add(loc1.directionTo(next));
			}
			rc.broadcast(Constants.BUILD_LOC1 + i, Util.locToInt(loc2));
		}
	}

	public void runHQ() throws GameActionException{
		if(rc.isActive() && !HQshoot()) // presumably the HQ always wants to be shooting/spawning whenever possible
			spawn(); // but if for some reason that's not true this can be changed
	}
	
	private static void spawn() throws GameActionException {
		if(rc.senseRobotCount()<GameConstants.MAX_ROBOTS)
			for(Direction spawnDir : Constants.allDirections)
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
