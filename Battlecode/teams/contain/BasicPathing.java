package contain;

import java.util.ArrayList;

import battlecode.common.*;

public class BasicPathing{
	
	static ArrayList<MapLocation> snailTrail = new ArrayList<MapLocation>();
	
	public static boolean canMove(Direction dir, boolean selfAvoiding,RobotController rc){
		//include both rc.canMove and the snail Trail requirements
		if(selfAvoiding){
			MapLocation resultingLocation = rc.getLocation().add(dir);
			for(int i=0;i<snailTrail.size();i++){
				MapLocation m = snailTrail.get(i);
				if(!m.equals(rc.getLocation())){
					if(resultingLocation.isAdjacentTo(m)||resultingLocation.equals(m)){
						return false;
					}
				}
			}
		}
		//if you get through the loop, then dir is not adjacent to the icky snail trail
		return rc.canMove(dir);
	}
	
	public static boolean canMoveAvoidHQ(Direction dir, boolean selfAvoiding,RobotController rc){
		//include both rc.canMove and the snail Trail requirements
		if(selfAvoiding){
			MapLocation resultingLocation = rc.getLocation().add(dir);
			int range = 26;
			if(resultingLocation.distanceSquaredTo(rc.senseEnemyHQLocation()) <= range) {
				return false;
			}
			for(int i=0;i<snailTrail.size();i++){
				MapLocation m = snailTrail.get(i);
				if(!m.equals(rc.getLocation())){
					if(resultingLocation.isAdjacentTo(m)||resultingLocation.equals(m)){
						return false;
					}
				}
			}
		}
		//if you get through the loop, then dir is not adjacent to the icky snail trail
		return rc.canMove(dir);
	}
	
	public static void tryToMove(Direction chosenDirection,boolean selfAvoiding,RobotController rc, int[] directionalLooks, Direction[] allDirections) throws GameActionException{
		while(snailTrail.size()<2)
			snailTrail.add(new MapLocation(-1,-1));
		if(rc.isActive()){
			snailTrail.remove(0);
			snailTrail.add(rc.getLocation());
			for(int directionalOffset:directionalLooks){
				int forwardInt = chosenDirection.ordinal();
				Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
				if(canMoveAvoidHQ(trialDir,selfAvoiding,rc)){
					rc.move(trialDir);
					break;
				}
			}
		}
	}
	
}