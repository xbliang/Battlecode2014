package splitpusher0420;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class Constants {
	public static Direction allDirections[] = Direction.values();
	public static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	
	public static final int PASTR_DELAY = 100;
	
	public static final int NOISE_BUILDER_1 = 0;
	public static final int PASTR_BUILDER_1 = 1;
	public static final int NOISE_BUILDER_2 = 2;
	public static final int PASTR_BUILDER_2 = 3;
	public static final int NOISE_BUILDER_3 = 4;
	public static final int PASTR_BUILDER_3 = 5;
	public static final int NOISE_BUILDER_4 = 6;
	public static final int PASTR_BUILDER_4 = 7;
	
	public static final int ROLE_CHANNEL = 0;
	public static final int BUILD_LOC1 = 1;
	public static final int BUILD_LOC2 = 2;
	public static final int BUILD_LOC3 = 3;
	public static final int BUILD_LOC4 = 4;
	public static final int NOISE_BUILD_STATUS_1 = 5;
	public static final int NOISE_BUILD_STATUS_2 = 6;
	public static final int NOISE_BUILD_STATUS_3 = 7;
	public static final int NOISE_BUILD_STATUS_4 = 8;
	public static final int PASTR_BUILD_STATUS_1 = 9;
	public static final int PASTR_BUILD_STATUS_2 = 10;
	public static final int PASTR_BUILD_STATUS_3 = 11;
	public static final int PASTR_BUILD_STATUS_4 = 12;
}
