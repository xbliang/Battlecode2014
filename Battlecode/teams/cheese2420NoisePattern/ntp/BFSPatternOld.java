package cheese2420NoisePattern.ntp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import cheese2420NoisePattern.RobotPlayer;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class BFSPatternOld extends NoiseTowerPattern {
	ArrayList<MapLocation> fireOrder;
	RobotController tower;
	int shootPosition;
	
	public BFSPatternOld(RobotController t, int range) {
		tower = t;
		fireOrder = new ArrayList<MapLocation>();
		shootPosition = 0;
		search(t.getLocation(), range);
	}
	public void search(MapLocation l, int val) {
		System.out.println("Searching");
//		int squaresConsidered = 0;
		Queue<MapLocation> q = new LinkedList<MapLocation>();
		q.add(l);
		while(q.size() > 0) {
//			squaresConsidered++;
			MapLocation next = q.poll();
			if(l.distanceSquaredTo(next) >= val || fireOrder.contains(next) || q.contains(next)) {
				continue;
			}
			TerrainTile tile = tower.senseTerrainTile(next);
			if(tile != TerrainTile.OFF_MAP && tile != TerrainTile.VOID ) {
				fireOrder.add(0,next);
				for (Direction d : RobotPlayer.allDirections){
					q.add(next.add(d));
				}
			}
		}
//		System.out.println("search done " + squaresConsidered);
		
	}
	public void shootNext() {
		try{
			if(tower.isActive()) {
				tower.attackSquare(fireOrder.get(shootPosition));
				shootPosition=(shootPosition+1)%fireOrder.size();
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
}
