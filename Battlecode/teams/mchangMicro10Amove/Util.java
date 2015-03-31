package mchangMicro10Amove;

import battlecode.common.MapLocation;

public class Util {
	public static int locToInt(MapLocation m){
		 return (m.x*100 + m.y);
	}
	public static MapLocation intToLoc(int i){
		 return new MapLocation(i/100,i%100);
	}
}
