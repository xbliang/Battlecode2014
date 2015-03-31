package multicorner1001;

import java.util.ArrayList;
import java.util.Arrays;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

//on maps -1 is unpassable
// 0 is unpolled
// 1 is passable
// higher integers are contiguous impassible objects
public class MotionUnit {
	int[][] map;
	static Direction allDirections[] = Direction.values();
	static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	RobotController rc;
	ArrayList<MapLocation> path;
	public MapLocation goal;
	int obstacle_number;
	int lookahead;
	public boolean onPath;
	
	public MotionUnit(RobotController r) {
		rc = r;
		map = new int[rc.getMapWidth()][rc.getMapHeight()];
		obstacle_number = 2;
		lookahead = 5;
		onPath = false;
		//buildMap(rc);
		path = new ArrayList<MapLocation>();
	}
	
	public int getMapData(int x, int y) {
		if(x < 0 || y < 0 || x >= map.length || y >= map[0].length) {
			return -2;
		}
		if(map[x][y] == 0) {
			TerrainTile t = rc.senseTerrainTile(new MapLocation(x,y));
			if(t==TerrainTile.VOID || t==TerrainTile.OFF_MAP) {
				map[x][y] = -1;
			} else {
				map[x][y] = 1;
			}
		}
		return map[x][y];
	}
	
	/*public void setMapData(MapLocation l,int v) {
		map[l.x+1][l.y+1]=v;
	}*/
	
	public void connectMap(int x, int y, int val) { //everything is in game coordinates so we need to do the offset of that
		if(getMapData(x,y) == -1) {
			map[x][y] = val;
			connectMap(x, y-1,val);
			connectMap(x, y+1,val);
			connectMap(x+1, y,val);
			connectMap(x-1, y,val);
		}
	}
	
	public boolean clearedObstacle(MapLocation p, MapLocation end,int obstacle) {		
		while(true) {
			if(getMapData(p.x, p.y) == obstacle) {
				return false;
			}
			if(!p.equals(end)) {
				p = p.add(p.directionTo(end));
			} else {
				return true;
			}
		}
	}
	public void startPath(MapLocation dest) {
		path.clear();
		goal = dest;
		path.add(goal);
		onPath = true;
	}
	
	public boolean onPath() {
		return onPath;
	}
	
	public void updateWaypoints() {
		MapLocation currentPos = rc.getLocation();
		if(!currentPos.equals(goal)) {
			MapLocation nextPos = path.get(0);
			if(currentPos.equals(nextPos)) {
				path.remove(0);
				nextPos = path.get(0);
				//System.out.println("Waypoint Reached new path: " + path);
			}
			MapLocation n = currentPos;
			MapLocation oldn = n;
			boolean collision = false;
			for(int i = 0; i < lookahead; i++) {
				oldn = n;
				if(!n.equals(nextPos)) {
					n = n.add(n.directionTo(nextPos));
				}
				if(getMapData(n.x, n.y) != 1) {
					//System.out.println("MAP DATA:" +getMapData(n.x, n.y));
					collision = true;
					break;
				}
			}
			if(collision) {
				MapLocation follow1 = oldn; //f1 follows the object clockwise
				MapLocation follow2 = oldn; //f2 follows the object counter-clockiwse
				//System.out.println("Collision at " + n);
				
				if(getMapData(n.x, n.y) == -1) {
					connectMap(n.x, n.y,obstacle_number);
					obstacle_number++;
				}
				/*for(int y = 0; y < map[0].length; y++) {
					for(int x = 0; x < map.length; x++) {
						System.out.print(map[x][y]);
					}
					System.out.println();
				}*/
				int obs = getMapData(n.x, n.y);
				boolean follow1ok = false;
				boolean follow2ok = false;
				boolean follow1end = false;
				boolean follow2end = false;
				
				//setup directions
				Direction f1d = follow1.directionTo(nextPos);
				Direction f2d = follow2.directionTo(nextPos);
				for(int offset = 8; offset > 0; offset--) {
					Direction trialDir = allDirections[(f1d.ordinal()+offset+8)%8];
					MapLocation t = follow1.add(trialDir);
					int data = getMapData(t.x, t.y);
					if(data == -2) {
						follow1end = true;
						break;
					} else if(data != obs && data != -2) {
						follow1 = t;
						f1d = trialDir;
						break;
					}
				}
				for(int offset = 0; offset < 8; offset++) {
					Direction trialDir = allDirections[(f2d.ordinal()+offset+8)%8];
					MapLocation t = follow2.add(trialDir);
					int data = getMapData(t.x, t.y);
					if(data == -2) {
						follow2end = true;
						break;
					} else if(data != obs && data != -2) {
						follow2 = t;
						f2d = trialDir;
						break;
					}
				}
				
				//follow around obstacle
				for(int x = 0; x < 1000; x++) {
					//System.out.println(follow1 + " " + follow2);
					if(!follow1ok && clearedObstacle(follow1, nextPos, obs)) {
						follow1ok = true;
					} else {
						if(!follow1ok) {
							//System.out.println("F1:" + follow1 + " going " + f1d);
							for(int offset = 2; offset >= -4; offset--) {
								Direction trialDir = allDirections[(f1d.ordinal()+offset+8)%8];
								//System.out.println("trying: " + trialDir);
								MapLocation t = follow1.add(trialDir);
								int data = getMapData(t.x, t.y);
								if(data == -2) {
									follow1end = true;
									break;
								} else if(data != obs && data != -2) {
									follow1 = t;
									f1d = trialDir;
									break;
								}
							}
							
						}
					}
					//System.out.println("NewF1:" + follow1 + " going " + f1d);
					if(!follow2ok && clearedObstacle(follow2, nextPos, obs)) {
						follow2ok = true;
					} else {
						if(!follow2ok) {
							for(int offset = -2; offset <= 4; offset++) {
								Direction trialDir = allDirections[(f2d.ordinal()+offset+8)%8];
								MapLocation t = follow2.add(trialDir);
								int data = getMapData(t.x, t.y);
								if(data == -2) {
									follow2end = true;
									break;
								} else if(data != obs && data != -2) {
									follow2 = t;
									f2d = trialDir;
									break;
								}
							}
						}
					}
					/*if(follow1ok && follow2ok) {
						double d1 = Math.sqrt(follow1.distanceSquaredTo(nextPos)) + Math.sqrt(currentPos.distanceSquaredTo(follow1));
						double d2 = Math.sqrt(follow2.distanceSquaredTo(nextPos)) + Math.sqrt(currentPos.distanceSquaredTo(follow2));
						if(d1 <d2) {
							path.add(0,follow1);
						} else {
							path.add(0,follow2);
						}										
						System.out.println("NewPath " + path);
						break;
					}*/
					if(follow1ok) {
						path.add(0,follow1);
						//System.out.println("NewPath " + path);
						break;
					}
					if(follow2ok) {
						path.add(0,follow2);
						//System.out.println("NewPath " + path);
						break;
					}
				}									
				
			}
		}
	}
	
	public void tryNextMove() throws GameActionException {
		updateWaypoints();
//		rc.setIndicatorString(0, ""+path.get(0));
		Direction dir = rc.getLocation().directionTo(path.get(0));
		if(rc.isActive()) {
			BasicPathing.tryToMove(dir, false, rc, directionalLooks, allDirections);
		}
		
	}
	
	public void tryNextSneak() throws GameActionException {
		updateWaypoints();
//		rc.setIndicatorString(0, ""+path.get(0));
		Direction dir = rc.getLocation().directionTo(path.get(0));
		if(rc.isActive()) {
			BasicPathing.tryToSneak(dir, false, rc, directionalLooks, allDirections);
		}
		
	}
	
	public Direction getNextDirection() {
		return rc.getLocation().directionTo(path.get(0));
	}
}
