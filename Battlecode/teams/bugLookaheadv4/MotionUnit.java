package bugLookaheadv4;

import java.util.ArrayList;

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
	MapLocation goal;
	int obstacle_number;
	int lookahead;
	boolean onPath;
	
	public MotionUnit(RobotController r) {
		rc = r;
		map = new int[rc.getMapWidth()+2][rc.getMapWidth()+2];
		obstacle_number = 2;
		lookahead = 5;
		onPath = false;
		//buildMap(rc);
		path = new ArrayList<MapLocation>();
	}
	
	public int getMapData(MapLocation l) {
		if(l.x<0 || l.y < 0 || l.x+1>=map.length|| l.y+1>=map[0].length) {
			return -1;
		}
		if(map[l.x+1][l.y+1] == 0) {
			TerrainTile t = rc.senseTerrainTile(l);
			if(t==TerrainTile.VOID || t==TerrainTile.OFF_MAP) {
				map[l.x+1][l.y+1] = -1;
			} else {
				map[l.x+1][l.y+1] = 1;
			}
		}
		return map[l.x+1][l.y+1];
	}
	
	public void setMapData(MapLocation l,int v) {
		map[l.x+1][l.y+1]=v;
	}
	
	public void connectMap(MapLocation l, int val) { //everything is in game coordinates so we need to do the offset of that
		if(getMapData(l) == -1) {
			setMapData(l, val);
			connectMap(l.add(Direction.NORTH),val);
			connectMap(l.add(Direction.SOUTH),val);
			connectMap(l.add(Direction.EAST),val);
			connectMap(l.add(Direction.WEST),val);
		}
	}
	
	public boolean clearedObstacle(MapLocation p, MapLocation end,int obstacle) {		
		while(true) {
			if(getMapData(p) == obstacle) {
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
				System.out.println("Waypoint Reached new path: " + path);
			}
			MapLocation n = currentPos;
			MapLocation oldn = n;
			boolean collision = false;
			for(int i = 0; i < lookahead; i++) {
				oldn = n;
				if(!n.equals(nextPos)) {
					n = n.add(n.directionTo(nextPos));
				}
				if(getMapData(n) != 1) {
					collision = true;
					break;
				}
			}
			if(collision) {
				MapLocation follow1 = oldn; //f1 follows the object clockwise
				MapLocation follow2 = oldn; //f2 follows the object counter-clockiwse
				//System.out.println("Collision at " + n);
				if(getMapData(n) == -1) {
					connectMap(n,obstacle_number);
					obstacle_number++;
				}
				int obs = getMapData(n);
				boolean follow1ok = false;
				boolean follow2ok = false;
				
				Direction f1d = follow1.directionTo(nextPos);
				Direction f2d = follow2.directionTo(nextPos);
				for(int offset = 8; offset > 0; offset--) {
					Direction trialDir = allDirections[(f1d.ordinal()+offset+8)%8];
					MapLocation t = follow1.add(trialDir);
					if(getMapData(t) != obs) {
						follow1 = t;
						f1d = trialDir;
						break;
					}
				}
				for(int offset = 0; offset < 8; offset++) {
					Direction trialDir = allDirections[(f2d.ordinal()+offset+8)%8];
					MapLocation t = follow2.add(trialDir);
					if(getMapData(t) != obs) {
						follow2 = t;
						f2d = trialDir;
						break;
					}
				}
				
				for(int x = 0; x < 100; x++) {
					//System.out.println(follow1 + " " + follow2);
					if(!follow1ok && clearedObstacle(follow1, nextPos, obs)) {
						follow1ok = true;
					} else {
						if(!follow1ok) {
							//System.out.println("F1:" + follow1 + " going " + f1d);
							for(int offset = 2; offset >= -3; offset--) {
								Direction trialDir = allDirections[(f1d.ordinal()+offset+8)%8];
								//System.out.println("trying: " + trialDir);
								MapLocation t = follow1.add(trialDir);
								if(getMapData(t) != obs) {
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
							for(int offset = -2; offset <= 3; offset++) {
								Direction trialDir = allDirections[(f2d.ordinal()+offset+8)%8];
								MapLocation t = follow2.add(trialDir);
								if(getMapData(t) != obs) {
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
	
	public void continuePath() {
		updateWaypoints();
		Direction dir = rc.getLocation().directionTo(path.get(0));
		try{
			if(rc.isActive()) {
				BasicPathing.tryToMove(dir, true, rc, directionalLooks, allDirections);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}