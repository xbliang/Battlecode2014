package dumpsterBotv3;

import battlecode.common.MapLocation;

public class Util {
	public static int locToInt(MapLocation m){
		 return (m.x*100 + m.y);
	}
	public static MapLocation intToLoc(int i){
		 return new MapLocation(i/100,i%100);
	}
	public static int twoLocsToInt(MapLocation one, MapLocation two) {
		return (one.x*1000000 + one.y*10000 + two.x*100 + two.y);
	}
	
	public static MapLocation intTolocOne(int i) {
		return new MapLocation(i/1000000,(i/10000)%100);
	}
	public static MapLocation intTolocTwo(int i) {
		return new MapLocation((i/100)%100,i%100);
	}
 }
