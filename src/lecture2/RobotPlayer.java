package lecture2;
import battlecode.common.*;
import java.util.*;

/**
 * Created by lguti on 1/17/17.
 */
public strictfp class RobotPlayer {
    static RobotController rc;
    static Random myRand;
    //In Java, constants are usually capitalized
    static int GARDNER_CHANNEL = 5;
    static int LUMBERJACK_CHANNEL = 6;

    //Keep max of spawn
    static int GARDNER_MAX = 4;
    static int LUMBERJACK_MAX = 10;

    public static void run(RobotController rc) throws GameActionException {
        //RobotController object.
        RobotPlayer.rc = rc;
        myRand = new Random(rc.getID());
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardner();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
        }
    }

    static void runArchon() throws GameActionException {
        while(true) {
            try {
                //Choose random direction
                Direction dir = randomDirection();
                //Determine the previous number of gardners available. But what if nothing?
                int prevNumGard = rc.readBroadcast(GARDNER_CHANNEL);
                //broadcast's a 0 in the Gardner_Channel
                rc.broadcast(GARDNER_CHANNEL, 0);
                if (prevNumGard < GARDNER_MAX && rc.canHireGardener(dir)) {
                    rc.hireGardener(dir);
                    rc.broadcast(GARDNER_CHANNEL, prevNumGard + 1);
                }
                //End the processing of the robot during the current round
                System.out.println(prevNumGard);
                Clock.yield();
            } catch (Exception e) {
                //What happens when there is an exception
                e.printStackTrace();
            }
        }
    }

    static void runGardner() throws GameActionException {
        while (true) {
            try {
                dodge();
                //Tells you how many gardners are active.
                int prev = rc.readBroadcast(GARDNER_CHANNEL);
                rc.broadcast(GARDNER_CHANNEL, prev + 1);
                System.out.println("TESTING :" + Integer.toString(prev));
                //wander around?
                wander();
                Direction dir = randomDirection();
                if (rc.getRoundNum() < 500) {
                    int prevNumGard = rc.readBroadcast(LUMBERJACK_CHANNEL);
                    if (prevNumGard <= LUMBERJACK_MAX && rc.canBuildRobot(RobotType.LUMBERJACK, dir)) {
                        rc.buildRobot(RobotType.LUMBERJACK, dir);
                        rc.broadcast(LUMBERJACK_CHANNEL, prevNumGard + 1);
                    } else {
                        if (rc.canBuildRobot(RobotType.SOLDIER, Direction.getEast())) {
                            rc.buildRobot(RobotType.SOLDIER, Direction.getEast());
                        }
                    }
                    Clock.yield();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runSoldier() throws GameActionException {
        while (true) {
            try{
                dodge();
                RobotInfo[] bots = rc.senseNearbyRobots();
                for (RobotInfo b : bots) {
                    if (b.getTeam() != rc.getTeam()) {
                        //this is direction that points towards the enemy
                        Direction towards = rc.getLocation().directionTo(b.getLocation());
                        rc.fireSingleShot(towards);
                        break;
                    }
                }
                wander();
                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        while(true) {
            try{
                dodge();
                RobotInfo[] bots = rc.senseNearbyRobots();
                for (RobotInfo b: bots) {
                    if (b.getTeam() != rc.getTeam() && rc.canStrike()){
                        rc.strike();
                        Direction chase = rc.getLocation().directionTo(b.getLocation());
                        tryMove(chase);
                        break;
                    }
                }
                TreeInfo[] trees = rc.senseNearbyTrees();
                for (TreeInfo t: trees){
                    if (rc.canChop(t.getLocation())){
                        rc.chop(t.getLocation());
                        break;
                    }
                }
                if (!rc.hasAttacked()){
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

    //Make methods public
    public static Direction randomDirection() {
        //returns an instance of direction
        //the radians at which you wish this direction represent based off unit circle
        return (new Direction(myRand.nextFloat() * 2 * (float) Math.PI));
    }

    //Determine if the bullet will collide with the robot
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

    static boolean tryMove(Direction dir) throws GameActionException {
        //give to other tryMove method
        return tryMove(dir, 20, 3);
    }

    /*
    Attempts to move in a given direction, while avoiding small obstacles direction in the path
    @param dir the Intended direction of movement
    @param degreeOffset Spacing between checked direction (degrees)
    @param  checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
    @return true if a move was performed
    @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {
        //try intended direction
        if (!rc.hasMoved() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        int currentCheck = 1;

        while (currentCheck <= checksPerSide) {
            // Try the offset on the leftside
            if (!rc.hasMoved() && rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
                return true;
            }
            // Try the offset on the rightside
            if (!rc.hasMoved() && rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
                return true;
            }
            // No move performed, try slightly more
            currentCheck++;

        }
        // Move never happened so return false
        return false;
    }

    //What does trySidestep do?
    static boolean trySidestep(BulletInfo bullet) throws GameActionException {
        Direction towards = bullet.getDir();
        //returns a new MapLocation object representing a location dist unit away from this one in given dir.
        MapLocation leftGoal = rc.getLocation().add(towards.rotateLeftDegrees(90), rc.getType().bodyRadius);
        MapLocation rightGoal = rc.getLocation().add(towards.rotateRightDegrees(90), rc.getType().bodyRadius);

        return (tryMove(towards.rotateRightDegrees(90)) || tryMove(towards.rotateLeftDegrees(90)));
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



}
