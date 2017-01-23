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
    static int LUMBERJACK_CHANNEL = 6;
    static int ENEMY_ARCHON_CHANNEL = 50;
    static int ENEMY_ARCHON_SPOTTED = 54;
    static int OUR_ARCHON_CHANNEL = 55;

    //max respawn numbers
    static int GARDENER_MAX = 3;
    static int LUMBERJACK_MAX = 10;

    //leader bots
    static int LEAD_ARCHON = 0;

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
                Direction dir = randomDirection();
                if (rc.getRoundNum() == 1 && LEAD_ARCHON == 0) {
                    makeLeader(rc);
                }

                //Do LEADER_ARCHON ACTIONS
                if (rc.getID() == LEAD_ARCHON) {
                    int prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
                    rc.broadcast(GARDENER_CHANNEL, 0);
                    if (prevNumGard < GARDENER_MAX && rc.canHireGardener(dir)) {
                        rc.hireGardener(dir);
                        rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
                    }
                }

                //For general Archons + lead Archon
                tryMove(dir);
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
               int treeCount = 0;
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
                       if (trees[j].getHealth() <  BULLET_TREE_MAX_HEALTH - 5) {
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

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runSoldier () throws GameActionException {
        while(true) {
            try {

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack () throws GameActionException {
        while (true) {
            try {

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runScout () throws GameActionException {
        while (true) {
            try {

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




    public static Direction randomDirection() {
        return(new Direction(myRand.nextFloat()*2*(float)Math.PI));
    }

    public static void makeLeader(RobotController rc) {
        LEAD_ARCHON = rc.getID();
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


}
