package lecture6;
import battlecode.common.*;
import java.util.*;

/**
 * Created by lguti on 1/19/2017.
 */
public strictfp class RobotPlayer {
    static RobotController rc;
    static Random myRand;
    static MapLocation myDest = null;
    static Direction myDir = null;
    static boolean stuck = false;
    static float stuckDist = 0;
    static boolean swarm = false;

    @SuppressWarnings("unused")
    // Keep broadcast channels
    static int GARDENER_CHANNEL = 5;
    static int LUMBERJACK_CHANNEL = 6;
    static int ENEMY_ARCHON_CHANNEL = 50;
    static int ENEMY_ARCHON_SPOTTED = 54;
    static int OUR_ARCHON_CHANNEL = 55;
    static int SWARM_CHANNEL = 100;

    // Keep important numbers here
    static int GARDENER_MAX = 4;
    static int LUMBERJACK_MAX = 10;

    public static void run(RobotController rc) throws GameActionException {
        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        myRand = new Random(rc.getID());
        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
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
                Direction toEnemy = rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);
                writeLocation(rc.getLocation().add(toEnemy, rc.getType().bodyRadius), SWARM_CHANNEL);
                Direction dir = randomDirection();
                int prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
                rc.broadcast(GARDENER_CHANNEL, 0);
                if (prevNumGard < GARDENER_MAX && rc.canHireGardener(dir)) {
                    rc.hireGardener(dir);
                    rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
                }
                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runGardener() throws GameActionException {
        while (true) {
            try {
                dodge();
                if (! rc.hasMoved()) {
                    //gets the location indicated by the swarm channel
                    MapLocation loc = readLocation(SWARM_CHANNEL);
                    swarm(loc, 8);
                    //moveToTarget(rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);
                }
                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runSoldier() throws GameActionException {
        while (true) {
            try {
                dodge();
                wander();
                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runScout() throws GameActionException {
        while (true) {
            try {
                dodge();
                wander();
                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        while (true) {
            try {
                dodge();
                RobotInfo[] bots = rc.senseNearbyRobots();
                for (RobotInfo b : bots) {
                    if (b.getTeam() != rc.getTeam() && rc.canStrike()) {
                        rc.strike();
                        Direction chase = rc.getLocation().directionTo(b.getLocation());
                        tryMove(chase);
                        break;
                    }
                }
                TreeInfo[] trees = rc.senseNearbyTrees();
                for (TreeInfo t : trees) {
                    if (rc.canChop(t.getLocation())) {
                        rc.chop(t.getLocation());
                        break;
                    }
                }
                if (! rc.hasAttacked()) {
                    wander();
                }
                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void wander() throws GameActionException {
        Direction dir = randomDirection();
        tryMove(dir);
    }


    public static Direction randomDirection() {
        return(new Direction(myRand.nextFloat()*2*(float)Math.PI));
    }

    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI / 2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }


    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
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

    static boolean trySidestep(BulletInfo bullet) throws GameActionException{

        Direction towards = bullet.getDir();
        MapLocation leftGoal = rc.getLocation().add(towards.rotateLeftDegrees(90), rc.getType().bodyRadius);
        MapLocation rightGoal = rc.getLocation().add(towards.rotateRightDegrees(90), rc.getType().bodyRadius);

        return(tryMove(towards.rotateRightDegrees(90)) || tryMove(towards.rotateLeftDegrees(90)));
    }

    static void dodge() throws GameActionException {
        BulletInfo[] bullets = rc.senseNearbyBullets();
        for (BulletInfo bi : bullets) {
            if (willCollideWithMe(bi)) {
                trySidestep(bi);
            }
        }

    }

    /**
     *
     * @param map A MapLocation to convert to integer representation
     * @return An array arr such that:
     *          arr[0] - integer part of x
     *          arr[1] - decimal part of x * 10^6 and rounded
     *          arr[2] - integer part of y
     *          arr[3] - decimal part of y * 10^6 and rounded
     */
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

    /**
     *
     * @param arr An array arr such that:
     *          arr[0] - integer part of x
     *          arr[1] - decimal part of x * 10^6 and rounded
     *          arr[2] - integer part of y
     *          arr[3] - decimal part of y * 10^6 and rounded
     * @return A MapLocation instantiated from the coordinates given by array
     */
    static MapLocation convertLocationInts(int[] arr) {
        float xcoord = (float)(arr[0] + arr[1]/Math.pow(10,6));
        float ycoord = (float)(arr[2] + arr[3]/Math.pow(10,6));
        return(new MapLocation(xcoord,ycoord));
    }

    static MapLocation readLocation(int firstChannel) throws GameActionException{
        int[] array = new int[4];
        array[0] = rc.readBroadcast(firstChannel);
        array[1] = rc.readBroadcast(firstChannel+1);
        array[2] = rc.readBroadcast(firstChannel+2);
        array[3] = rc.readBroadcast(firstChannel+3);
        return convertLocationInts(array);
    }

    static void writeLocation(MapLocation map, int firstChannel) throws GameActionException{
        int[] arr = convertMapLocation(map);
        rc.broadcast(firstChannel, arr[0]);
        rc.broadcast(firstChannel + 1, arr[1]);
        rc.broadcast(firstChannel+2, arr[2]);
        rc.broadcast(firstChannel+3, arr[3]);
    }

    // this is the slugs "tail" imagine leaving a trail of sticky goo on the map that you don't want to step in that slowly dissapates over time
    static ArrayList<MapLocation> oldLocations = new ArrayList<MapLocation>();



    private static boolean slugMoveToTarget(MapLocation target, float strideRadius) throws GameActionException{

        // when trying to move, let's look forward, then incrementing left and right.
        float[] toTry = {0, (float)Math.PI/4, (float)-Math.PI/4, (float)Math.PI/2, (float)-Math.PI/2, 3*(float)Math.PI/4, -3*(float)Math.PI/4, -(float)Math.PI};

        MapLocation ourLoc = rc.getLocation();
        Direction toMove = ourLoc.directionTo(target);

        // let's try to find a place to move!
        for (int i = 0; i < toTry.length; i++) {
            Direction dirToTry = toMove.rotateRightDegrees(toTry[i]);
            if (rc.canMove(dirToTry, strideRadius)) {
                // if that location is free, let's see if we've already moved there before (aka, it's in our tail)
                MapLocation newLocation = ourLoc.add(dirToTry, strideRadius);
                boolean haveWeMovedThereBefore = false;
                for (int j = 0; j < oldLocations.size(); j++) {
                    if (newLocation.distanceTo(oldLocations.get(j)) < strideRadius * strideRadius) {
                        haveWeMovedThereBefore = true;
                        break;
                    }
                }
                if (!haveWeMovedThereBefore) {
                    oldLocations.add(newLocation);
                    if (oldLocations.size() > 10) {
                        // remove the head and chop the list down to size 10 (or whatever you want to use)
                    }
                    if (! rc.hasMoved() && rc.canMove(dirToTry, strideRadius)) {
                        rc.move(dirToTry, strideRadius);
                    }
                    return(true);
                }

            }
        }
        //looks like we can't move anywhere
        return(false);

    }

    private static boolean moveToTarget(MapLocation location) throws GameActionException{
        // try to take a big step
        if (slugMoveToTarget(location, rc.getType().strideRadius)) {
            return(true);
        }
        // try to take a smaller step
        if (slugMoveToTarget(location, rc.getType().strideRadius/2)) {
            return(true);
        }
        // try to take a baby step
        if (slugMoveToTarget(location, rc.getType().strideRadius/4)) {
            return(true);
        }
        else {
            wander();
            return(false);
        }
        // insert move randomly code here

    }

    public static void swarm(MapLocation location, int quantity) throws GameActionException{
        int rollcall = 0;
        if (swarm) {
            moveToTarget(rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);
            return;
        }
        RobotInfo[] bots = rc.senseNearbyRobots();
        for (RobotInfo b : bots) {
            if (b.getTeam() == rc.getTeam() && b.getType() == RobotType.GARDENER) {
                rollcall += 1;
            }
        }
        if (rollcall >= quantity) {
            swarm = true;
            moveToTarget(rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);
        }
        else {
            moveToTarget(location);
        }
    }

}