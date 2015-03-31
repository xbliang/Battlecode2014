package dumpsterCheese420.ntp;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class LinearSpiralReachableAlternatingPattern extends NoiseTowerPattern {
//	NoiseTowerPattern current;
	LinearPattern linear;
	SpiralPatternReachable spiral;
//	RobotController tower;
//	int numRevolutions = 0;

	public LinearSpiralReachableAlternatingPattern(RobotController t, int range) throws GameActionException {
//		tower = t;
		linear = new LinearPattern(t, range);
		spiral = new SpiralPatternReachable(t, range);
//		current = linear;
	}

	public void shootNext() throws GameActionException {
		if ((int) linear.numRevolutions == (int) spiral.numRevolutions) {
			linear.shootNext();
		} else {
			spiral.shootNext();
		}
	}
}
