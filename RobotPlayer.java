package main;
import battlecode.common.*;

import java.awt.*;
import java.util.Random;

import static battlecode.common.GameConstants.*;

/**
 * Created by lguti on 1/19/17.
 */
public class RobotPlayer {

    static RobotController rc;
    static Random myRand;
    static MapLocation myDest = null;
    static Direction myDir = null;
    static boolean stuck = false;

    //broadcast channels
    static int GARDENER_CHANNEL = 5;
    static int SCOUT_CHANNEL = 17;
    static int LUMBERJACK_CHANNEL = 6;
    static int ENEMY_ARCHON_CHANNEL = 50;
    static int ENEMY_ARCHON_SPOTTED = 54;
    static int OUR_ARCHON_CHANNEL = 55;
    static int LEAD_ARCHON_CHANNEL = 97;
    static int BUILD_LUMBER = 88;

    //max respawn numbers
    static int GARDENER_MAX = 2;
    static int SCOUT_MAX = 2;
    static int LUMBERJACK_MAX = 10;

    //other important stuff
    static Direction[] dirList = new Direction[4];
    static int TREE_COUNT = 0;

    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        myRand = new Random(rc.getID());
        initDirList();
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
            case SCOUT:
                runScout();
                break;
        }
    }


    static void runArchon() throws GameActionException {
        while (true) {
            try {
                dodge();
                Direction dir = randomDirection();
                //MAKE LEADER ARCHON
                if (rc.getRoundNum() == 1 && rc.readBroadcast(LEAD_ARCHON_CHANNEL) == 0) {
                    makeLeader(rc);
                }
                //if more than one archon is there build a gardener for each and try not
                //cap it to 1 gardener for each archon and then build gardeners for each one.
                //Do LEADER_ARCHON ACTIONS
                if (rc.getID() == rc.readBroadcast(LEAD_ARCHON_CHANNEL)) {
                    int prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
                    rc.broadcast(GARDENER_CHANNEL, 0);

                    //if there are less than 2 scouts keep building scouts for now
                    if (prevNumGard == 1 && rc.readBroadcast(SCOUT_CHANNEL) < 2) {
                        //one gardener already built so build one scout
                        tryBuild(RobotType.SCOUT);
                    }

                    else if (prevNumGard < GARDENER_MAX && rc.canHireGardener(dir)) {
                        rc.hireGardener(dir);
                        rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
                    }

                }
                //DO NON LEADER ACTIONS
                //Skip this component now and focus on just one archon
                else {
                    if (rc.canHireGardener(dir)) {
                        rc.hireGardener(dir);
                    }
                }

                //FOR GENERAL STUFF
                Clock.yield();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /*
    - With a max of three Gardeners, build trees in a circular fashion (to farm bullets)
     - Leave an opening to construct other bots in the game

     */
    static void runGardener() throws GameActionException {
        while (true) {
            try {
               Direction initial = Direction.getEast();
               Direction dir = randomDirection();
               int prev = rc.readBroadcast(GARDENER_CHANNEL);

               //update broadcast saying gardener is alive
               rc.broadcast(GARDENER_CHANNEL, prev + 1);

               //FOR THE FIRST 100 ROUNDS
               if (rc.getRoundNum() < 100) {
                   int prevNumScout = rc.readBroadcast(SCOUT_CHANNEL);
                   int treecount = rc.readBroadcast(BUILD_LUMBER);
                   System.out.println(treecount);
                   if (prevNumScout < SCOUT_MAX && rc.canBuildRobot(RobotType.SCOUT, dir)) {
                       rc.buildRobot(RobotType.SCOUT, dir);
                       rc.broadcast(SCOUT_CHANNEL, prevNumScout + 1);
                   
                   }
                   else if (treecount>2){
                	   if (rc.canBuildRobot(RobotType.LUMBERJACK, dir)){
                		   rc.buildRobot(RobotType.LUMBERJACK, dir);
                	   }
                   }
                   else{
                	   if (rc.canBuildRobot(RobotType.SOLDIER, dir)){
                		   rc.buildRobot(RobotType.SOLDIER, dir);
                	   }
                   }
                   
               }
               //After the gardener has built two scouts
               else {
                   //keep track of the amount of trees ma
                   if (rc.senseNearbyTrees().length < 2) {
                       //place code for planting trees
                       for (int i = 0; i <= 4; i++) {
                           if (rc.canPlantTree(initial.rotateRightDegrees(i * 60))) {
                               rc.plantTree(initial.rotateRightDegrees(i * 60));
                               break;
                           }
                       }
                   }
                   //WATER THE TREE
                   TreeInfo[] trees = rc.senseNearbyTrees();
                   for (int j = 0; j < trees.length; j++) {
                       if (trees[j].getHealth() <  BULLET_TREE_MAX_HEALTH - 10) {
                           if (rc.canWater(trees[j].getID())) {
                               rc.water(trees[j].getID());
                               break;
                           }
                       }
                   }

                   

               }
               
               Clock.yield();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runSoldier () throws GameActionException {
        while(true) {
            try {
                //perhaps I should run the soldier?
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack () throws GameActionException {
        while (true) {
            try {

                dodge();
                TreeInfo[] trees = rc.senseNearbyTrees();
                for (TreeInfo t: trees) {
                    if (rc.canChop(t.getID()) && t.getTeam() != rc.getTeam()) {
                        rc.chop(t.getID());
                        break;
                    }
                }

                RobotInfo[] bots = rc.senseNearbyRobots();
                for (RobotInfo b: bots) {
                    if (b.getTeam() != rc.getTeam() && b.getType() == RobotType.ARCHON) {
                        //If the Archon is not part of the team
                        //Send a message describing the location of the Archon
                        writeLocation(b.getLocation(), ENEMY_ARCHON_CHANNEL);
                        rc.broadcast(ENEMY_ARCHON_SPOTTED, rc.getRoundNum());
                        Direction towards = rc.getLocation().directionTo(b.getLocation());
                        if (rc.canStrike()) {
                            rc.strike();
                            break;
                        }
                    }
                    else if (b.getTeam() != rc.getTeam()) {
                        Direction towards = rc.getLocation().directionTo(b.getLocation());
                        tryMove(towards);
                        if (rc.canStrike()) {
                            rc.strike();
                            break;
                        }
                    }
                }

                //if this shit happened less than 10 rounds ago go towards the Archon
                if (rc.getRoundNum() - rc.readBroadcast(ENEMY_ARCHON_SPOTTED) < 10) {
                    goTowards(readLocation(ENEMY_ARCHON_CHANNEL));
                }
                if (rc.getRoundNum() < 300) {
                    if (myDest == null){
                        MapLocation[] locs = rc.getInitialArchonLocations(rc.getTeam().opponent());
                        myDest = locs[0];
                    }
                }
                if (myDest != null) {
                    goTowards(myDest);
                }

                wander();
                Clock.yield();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runScout () throws GameActionException {
        while (true) {
            try {
                dodge();
                int treeCount = 0;
                //sensing nearby robots
                RobotInfo[] bots = rc.senseNearbyRobots();
                for (RobotInfo b: bots) {
                    if (b.getTeam() != rc.getTeam() && b.getType() == RobotType.ARCHON) {
                        //If the Archon is not part of the team
                        //Send a message describing the location of the Archon
                        writeLocation(b.getLocation(), ENEMY_ARCHON_CHANNEL);
                        rc.broadcast(ENEMY_ARCHON_SPOTTED, rc.getRoundNum());
                        Direction towards = rc.getLocation().directionTo(b.getLocation());
                        break;
                    }
                    else if(b.getType() == RobotType.ARCHON){ //if friendly then count trees
                    	if (rc.getRoundNum() < 100) {
                        	TreeInfo[] trees = rc.senseNearbyTrees();
                        	for (TreeInfo t : trees) {
                        		if(t.getTeam() != rc.getTeam().opponent()){
                        			treeCount += 1;
                        		}
                        	}
                        	//do something with treecount
                        }
                    }
                   
                    
                    
                    
                    
                    else if (rc.canFireSingleShot() && b.getTeam() != rc.getTeam()) {
    						rc.fireSingleShot(rc.getLocation().directionTo(b.location));
    					}
                    
                }
                
                
                
                TreeInfo[] trees = rc.senseNearbyTrees();
                for (TreeInfo t : trees) {
                	if(t.getContainedBullets() > 0){
                		if(!rc.hasMoved()){
							if(rc.canMove(t.getLocation())){

								rc.move(t.getLocation());
                        if(rc.canShake(t.getLocation())){
                           
								rc.shake(t.getID());}
								if(!rc.canShake()){
									System.out.println("I shook it");
									break;
								}
								break;
							}
							else{
								Direction direction_moved = rc.getLocation().directionTo(t.getLocation());
								if(!rc.hasMoved()){
									tryMove(direction_moved);}

							}
				TREE_COUNT = treeCount;
		        rc.broadcast(88,TREE_COUNT);
		        if (TREE_COUNT != 0){
		        	System.out.println(TREE_COUNT);
		        }

							break;
						}
                	}
                }
                wander();
                Clock.yield();


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




    public static Direction randomDirection() {
        return(new Direction(myRand.nextFloat()*2*(float)Math.PI));
    }

    static void makeLeader(RobotController rc) throws GameActionException {
        rc.broadcast(LEAD_ARCHON_CHANNEL, rc.getID());
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (!rc.hasMoved() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        //boolean moved = rc.hasMoved();
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(!rc.hasMoved() && rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(! rc.hasMoved() && rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    public static void dodge() throws GameActionException {
        //senseNearbyBullets returns all bullets within bullet sense radius
        BulletInfo[] bullets = rc.senseNearbyBullets();
        for (BulletInfo bi: bullets) {
            if (willCollideWithMe(bi)){
                //this returns a boolean?
                trySidestep(bi);
            }
        }

    }

    static boolean willCollideWithMe(BulletInfo bullet) {
        //returns the robots location
        MapLocation myLocation = rc.getLocation();

        //Get relevant bullet information. Direction in which this bullet is moving
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        //Calculate bullet relation to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        //If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI / 2){
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, we want to know the length of the opposite leg
        // distance of a line that goes from my myLocation and intersects perpendicularly with propagationDirection
        // This corresponds to the smallest radius circle centered at our location would intersect the
        // line that is the path of the bullet
        float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta));
        return (perpendicularDist <= rc.getType().bodyRadius);
    }

    static boolean trySidestep(BulletInfo bullet) throws GameActionException {
        Direction towards = bullet.getDir();
        //returns a new MapLocation object representing a location dist unit away from this one in given dir.
        MapLocation leftGoal = rc.getLocation().add(towards.rotateLeftDegrees(90), rc.getType().bodyRadius);
        MapLocation rightGoal = rc.getLocation().add(towards.rotateRightDegrees(90), rc.getType().bodyRadius);

        return (tryMove(towards.rotateRightDegrees(90)) || tryMove(towards.rotateLeftDegrees(90)));
    }

    public static void wander() throws GameActionException {
        Direction dir = randomDirection();
        tryMove(dir);
    }

    static void goTowards(MapLocation map) throws GameActionException {
        tryMove(rc.getLocation().directionTo(map));
    }

    static int[] convertMapLocation(MapLocation map) {
        float xcoord = map.x;
        float ycoord = map.y;
        int[] returnarray = new int[4];
        returnarray[0] = Math.round(xcoord - (xcoord % 1));
        returnarray[1] = Math.toIntExact(Math.round((xcoord % 1)*Math.pow(10,6)));
        returnarray[2] = Math.round(ycoord - (ycoord % 1));
        returnarray[3] = Math.toIntExact(Math.round((ycoord % 1)*Math.pow(10,6)));
        return(returnarray);
    }

    //What does readLocation do though?
    static MapLocation readLocation(int firstChannel) throws GameActionException{
        int[] array = new int[4];
        array[0] = rc.readBroadcast(firstChannel);
        array[1] = rc.readBroadcast(firstChannel+1);
        array[2] = rc.readBroadcast(firstChannel+2);
        array[3] = rc.readBroadcast(firstChannel+3);
        return convertLocationInts(array);
    }

    //convert the location integers into a MapLocation
    static MapLocation convertLocationInts(int[] arr) {
        float xcoord = (float)(arr[0] + arr[1]/Math.pow(10,6));
        float ycoord = (float)(arr[2] + arr[3]/Math.pow(10,6));
        return(new MapLocation(xcoord,ycoord));
    }

    static void writeLocation(MapLocation map, int firstChannel) throws GameActionException {
        //int array represents coordinate of maplocation with rounding/precision
        int[] arr = convertMapLocation(map);
        rc.broadcast(firstChannel, arr[0]);
        rc.broadcast(firstChannel +1, arr[1]);
        rc.broadcast(firstChannel +2, arr[2]);
        rc.broadcast(firstChannel +3, arr[3]);
    }

    public static void initDirList(){
        for(int i=0;i<4;i++){
            float radians = (float)(-Math.PI + 2*Math.PI*((float)i)/4);
            dirList[i]=new Direction(radians);
            System.out.println("made new direction "+dirList[i]);
        }
    }

    public static void tryBuild(RobotType type) throws GameActionException {
        for (int i = 0; i < 4; i++) {
            if(rc.canBuildRobot(type,dirList[i])){
                rc.buildRobot(type,dirList[i]);
                break;
            }
        }
    }



}
