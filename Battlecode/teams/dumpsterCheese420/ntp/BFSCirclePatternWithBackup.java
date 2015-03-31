package dumpsterCheese420.ntp;

import dumpsterCheese420.RobotPlayer;
import dumpsterCheese420.Util;
import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class BFSCirclePatternWithBackup extends NoiseTowerPattern {
	NoiseTowerPattern backup;
	NoiseTowerPattern noiseTowerPattern;
	BFSCirclePatternStep bfs;
	RobotController tower;

	public BFSCirclePatternWithBackup(RobotController t, int range, NoiseTowerPattern backup) throws GameActionException {
		tower = t;
		this.backup = backup;
		noiseTowerPattern = backup;
	}

	public void shootNext() throws GameActionException {
		noiseTowerPattern.shootNext();
		if (bfs == null) {
			int pastr = tower.readBroadcast(RobotPlayer.PASTR_LOCATION_CHANNEL);
			if (pastr != 0)
				bfs = new BFSCirclePatternStep(tower, 150, Util.intToLoc(pastr));
		} 
		else {
			while (Clock.getBytecodesLeft() > 270 && !BFSCirclePatternStep.doneSearching) {
//				System.out.println(i - Clock.getBytecodesLeft());
//				i = Clock.getBytecodesLeft();
				bfs.searchStep();
			}
			if (BFSCirclePatternStep.doneSearching) {
				noiseTowerPattern = bfs;
			}
		}

	}
}
