package splitpusher0420;

// template for basic robot player
// sample code at the end that'll probably be used in every bot

import battlecode.common.*;

public class RobotPlayer {
	private static RobotController rc;
	private static SoldierController sc;
	private static HQController hc;
	private static NoiseTowerPattern nc;
	private static int corner_id = 0;
	
	public static void run(RobotController rcIn){
		rc = rcIn; // store the RobotController so every function can refer to it
		try
		{
			if(rc.getType()==RobotType.HQ) {
				hc = new HQController(rc);
				while(true) {
					hc.runHQ();
					rc.yield();
				}
			}
			else if(rc.getType()==RobotType.SOLDIER) {
				sc = new SoldierController(rc);
				while(true) {
					sc.runSoldier();
					rc.yield();
				}
			}
			else if(rc.getType()==RobotType.NOISETOWER) {
				MapLocation buildLoc = new MapLocation(0,0);
				for(int i = 0; i < 4; i++) {
					buildLoc = Util.intToLoc(rc.readBroadcast(Constants.BUILD_LOC1+i));
					if(rc.getLocation().distanceSquaredTo(buildLoc) <= 5) {
						corner_id = i;
						break;
					}
				}
				nc = new SpiralPatternReachable(rc, 16, buildLoc, 6);
				while(true) {
					rc.broadcast(Constants.NOISE_BUILD_STATUS_1+corner_id, Clock.getRoundNum()+2);
					nc.shootNext();
					rc.yield();
				}
			}
			else if(rc.getType()==RobotType.PASTR) {
				for(int i = 0; i < 4; i++) {
					MapLocation buildLoc = Util.intToLoc(rc.readBroadcast(Constants.BUILD_LOC1+i));
					if(rc.getLocation().distanceSquaredTo(buildLoc) <= 5) {
						corner_id = i;
						break;
					}
				}
				while(true) {
					rc.broadcast(Constants.PASTR_BUILD_STATUS_1+corner_id, Clock.getRoundNum()+2);
					rc.yield();
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
