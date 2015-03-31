package splitpusher0420;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SoldierController {
	private static RobotController rc;
	private static MotionUnit motion;
	private static int role;
	private static MapLocation target;
	
	public SoldierController(RobotController r) throws GameActionException {
		rc = r;
		role = rc.readBroadcast(Constants.ROLE_CHANNEL);
		rc.broadcast(Constants.ROLE_CHANNEL, (role+1)%8);
		switch(role) {
		case Constants.NOISE_BUILDER_1:
		case Constants.PASTR_BUILDER_1:
			target = Util.intToLoc(rc.readBroadcast(Constants.BUILD_LOC1));
			break;
		case Constants.NOISE_BUILDER_2:
		case Constants.PASTR_BUILDER_2:
			target = Util.intToLoc(rc.readBroadcast(Constants.BUILD_LOC2));
			break;
		case Constants.NOISE_BUILDER_3:
		case Constants.PASTR_BUILDER_3:
			target = Util.intToLoc(rc.readBroadcast(Constants.BUILD_LOC3));
			break;
		case Constants.NOISE_BUILDER_4:
		case Constants.PASTR_BUILDER_4:
			target = Util.intToLoc(rc.readBroadcast(Constants.BUILD_LOC4));
			break;
		}
		rc.setIndicatorString(1,target.toString());
		motion = new MotionUnit(rc);
		motion.startPath(target);
	}
	
	private static void retreatFrom(MapLocation averageEnemy) throws GameActionException {
		Direction d = Constants.allDirections[(rc.getLocation().directionTo(averageEnemy).ordinal()+4)%8];
		BasicPathing.tryToMove(d, false, rc, Constants.directionalLooks, Constants.allDirections);
	}

	
	
	public void runSoldier() throws GameActionException {
		if(!kite() && !build()) {
			if(rc.getLocation().distanceSquaredTo(target) > 69)
				motion.tryNextMove();
			else
				motion.tryNextSneak();
		}
	}

	private boolean build() throws GameActionException {
		if(!rc.isActive() || rc.getLocation().distanceSquaredTo(target) > 5 || rc.getLocation().x == 0 || rc.getLocation().y == 0 || rc.getLocation().x == rc.getMapWidth()-1 || rc.getLocation().y == rc.getMapHeight()-1)
			return false;
		int currTurn = Clock.getRoundNum();
		int noiseBuildTurn, pastrBuildTurn;
		switch(role) {
		case Constants.NOISE_BUILDER_1:
			noiseBuildTurn = rc.readBroadcast(Constants.NOISE_BUILD_STATUS_1);
			if(currTurn > noiseBuildTurn) {
				rc.broadcast(Constants.NOISE_BUILD_STATUS_1, currTurn + 102);
				if(rc.readBroadcast(Constants.PASTR_BUILD_STATUS_1) == 9999) // unlock PASTR build after noise tower
					rc.broadcast(Constants.PASTR_BUILD_STATUS_1, currTurn + Constants.PASTR_DELAY);
				rc.construct(RobotType.NOISETOWER);
			}
			break;
		case Constants.NOISE_BUILDER_2:
			noiseBuildTurn = rc.readBroadcast(Constants.NOISE_BUILD_STATUS_2);
			if(currTurn > noiseBuildTurn) {
				rc.broadcast(Constants.NOISE_BUILD_STATUS_2, currTurn + 102);
				if(rc.readBroadcast(Constants.PASTR_BUILD_STATUS_2) == 9999) // unlock PASTR build after noise tower
					rc.broadcast(Constants.PASTR_BUILD_STATUS_2, currTurn + Constants.PASTR_DELAY);
				rc.construct(RobotType.NOISETOWER);
			}
			break;
		case Constants.NOISE_BUILDER_3:
			noiseBuildTurn = rc.readBroadcast(Constants.NOISE_BUILD_STATUS_3);
			if(currTurn > noiseBuildTurn) {
				rc.broadcast(Constants.NOISE_BUILD_STATUS_3, currTurn + 102);
				if(rc.readBroadcast(Constants.PASTR_BUILD_STATUS_3) == 9999) // unlock PASTR build after noise tower
					rc.broadcast(Constants.PASTR_BUILD_STATUS_3, currTurn + Constants.PASTR_DELAY);
				rc.construct(RobotType.NOISETOWER);
			}
			break;
		case Constants.NOISE_BUILDER_4:
			noiseBuildTurn = rc.readBroadcast(Constants.NOISE_BUILD_STATUS_4);
			if(currTurn > noiseBuildTurn) {
				rc.broadcast(Constants.NOISE_BUILD_STATUS_4, currTurn + 102);
				if(rc.readBroadcast(Constants.PASTR_BUILD_STATUS_4) == 9999) // unlock PASTR build after noise tower
					rc.broadcast(Constants.PASTR_BUILD_STATUS_4, currTurn + Constants.PASTR_DELAY);
				rc.construct(RobotType.NOISETOWER);
			}
			break;
		case Constants.PASTR_BUILDER_1:
			pastrBuildTurn = rc.readBroadcast(Constants.PASTR_BUILD_STATUS_1);
			if(currTurn > pastrBuildTurn) {
				rc.broadcast(Constants.PASTR_BUILD_STATUS_1, currTurn + 52);
				rc.construct(RobotType.PASTR);
			}
			break;
		case Constants.PASTR_BUILDER_2:
			pastrBuildTurn = rc.readBroadcast(Constants.PASTR_BUILD_STATUS_2);
			if(currTurn > pastrBuildTurn) {
				rc.broadcast(Constants.PASTR_BUILD_STATUS_2, currTurn + 52);
				rc.construct(RobotType.PASTR);
			}
			break;
		case Constants.PASTR_BUILDER_3:
			pastrBuildTurn = rc.readBroadcast(Constants.PASTR_BUILD_STATUS_3);
			if(currTurn > pastrBuildTurn) {
				rc.broadcast(Constants.PASTR_BUILD_STATUS_3, currTurn + 52);
				rc.construct(RobotType.PASTR);
			}
			break;
		case Constants.PASTR_BUILDER_4:
			pastrBuildTurn = rc.readBroadcast(Constants.PASTR_BUILD_STATUS_4);
			if(currTurn > pastrBuildTurn) {
				rc.broadcast(Constants.PASTR_BUILD_STATUS_4, currTurn + 52);
				rc.construct(RobotType.PASTR);
			}
			break;
		}
		return false;
	}

	private boolean kite() throws GameActionException {
		Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		if (nearbyEnemies.length > 0) {
			Robot[] attackableEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
			if (attackableEnemies.length > 0) {
				RobotInfo robotInfo = rc.senseRobotInfo(attackableEnemies[0]);
				if(rc.isActive())
					rc.attackSquare(robotInfo.location);
			} else {
				MapLocation averageEnemy = new MapLocation(0,0);
				for(Robot enemy : nearbyEnemies) {
					RobotInfo robotInfo = rc.senseRobotInfo(enemy);
					averageEnemy = averageEnemy.add(robotInfo.location.x, robotInfo.location.y);
				}
				averageEnemy = new MapLocation(averageEnemy.x/nearbyEnemies.length, averageEnemy.y/nearbyEnemies.length);
				retreatFrom(averageEnemy);
			}
			return true;
		}
		return false;
	}
}
