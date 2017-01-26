package main;
import battlecode.common.*;
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

    //max respawn numbers
    static int GARDENER_MAX = 3;
    static int SCOUT_MAX = 3;
    static int LUMBERJACK_MAX = 10;

    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        myRand = new Random(rc.getID());
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
                    System.out.print("lol");
                    makeLeader(rc);
                }

                //Do LEADER_ARCHON ACTIONS
                if (rc.getID() == rc.readBroadcast(LEAD_ARCHON_CHANNEL)) {
                    int prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
                    rc.broadcast(GARDENER_CHANNEL, 0);
                    if (prevNumGard < GARDENER_MAX && rc.canHireGardener(dir)) {
                        rc.hireGardener(dir);
                        rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
                        System.out.println(prevNumGard);
                    }
                    System.out.println(prevNumGard);
                }
                //DO NON LEADER ACTIONS
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
               int treeCount = 0;
               int prev = rc.readBroadcast(GARDENER_CHANNEL);
               rc.broadcast(GARDENER_CHANNEL, prev + 1);

               if (rc.getRoundNum() < 300) {
                   int prevNumScout = rc.readBroadcast(SCOUT_CHANNEL);
                   if (prevNumScout < SCOUT_MAX && rc.canBuildRobot(RobotType.SCOUT, dir)) {
                       rc.buildRobot(RobotType.SCOUT, dir);
                       rc.broadcast(SCOUT_CHANNEL, prevNumScout + 1);
                   }
               }

               for (int i = 0; i < 5; i++) {
                   if (rc.canPlantTree(initial.rotateLeftDegrees(i * 60))) {
                       rc.plantTree(initial.rotateLeftDegrees(i * 60));
                   }
                   else {
                       treeCount++;
                   }

                   //Water trees if low health
                   TreeInfo[] trees = rc.senseNearbyTrees();
                   for (int j = 0; j < trees.length; j++) {
                       if (trees[j].getHealth() <  BULLET_TREE_MAX_HEALTH - 10) {
                           if (rc.canWater(trees[j].getID())) {
                               rc.water(trees[j].getID());
                               break;
                           }
                       }
                   }
                   //construct lumberjacks when there are 4 trees for protection
                   if (treeCount == 4) {
                       if (rc.canBuildRobot(RobotType.LUMBERJACK, initial.rotateLeftDegrees(5 * 60))) {
                           rc.buildRobot(RobotType.LUMBERJACK, initial.rotateLeftDegrees(5 * 60));
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
                //sensing nearby robots
                RobotInfo[] bots = rc.senseNearbyRobots();
                for (RobotInfo b: bots) {
                    if (b.getTeam() != rc.getTeam() && b.getType() == RobotType.GARDENER) {
                        //If the Archon is not part of the team
                        //Send a message describing the location of the Archon
                        writeLocation(b.getLocation(), ENEMY_ARCHON_CHANNEL);
                        rc.broadcast(ENEMY_ARCHON_SPOTTED, rc.getRoundNum());
                        Direction towards = rc.getLocation().directionTo(b.getLocation());
                        if (rc.canFireSingleShot()){
                            rc.fireSingleShot(towards);
                        }
                        break;
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


}
