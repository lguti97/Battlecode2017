package main;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

import static battlecode.common.GameConstants.*;

/**
 * Created by lguti on 1/19/17.
 */
public class RobotPlayer {

    static RobotController rc;
    static Random myRand;
    static MapLocation myDest = null;
    static Direction goingDir;
    static Direction myDir = null;
    static boolean stuck = false;
    static boolean swarm = false;

    //broadcast channels
    static int GARDENER_CHANNEL = 5;
    static int SCOUT_CHANNEL = 17;
    static int SOLDIER_CHANNEL = 1;
    static int LUMBERJACK_CHANNEL = 6;
    static int ENEMY_ARCHON_CHANNEL = 50;
    static int ENEMY_ARCHON_SPOTTED = 54;
    static int LEAD_ARCHON_CHANNEL = 97;
    static int TREES_CHANNEL = 69;
    static int FIRST_ARCHON = 0;
    static int SWARM_CHANNEL = 100;
    static int INITIAL_ENEMY_ARCHON_CHANNEL = 2;
    static int ENEMY_ARCHON_KILLED = 3;


    //max respawn numbers
    static int GARDENER_MAX = 1;
    static int SOLDIER_MAX = 2;
    static int SCOUT_MAX = 1;
    static int LUMBERJACK_MAX = 2;

    //other important stuff
    static Direction[] dirList = new Direction[9];

    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        myRand = new Random(rc.getID());
        goingDir = randomDirection();
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
                makeLeader(rc);

                //Do LEADER_ARCHON ACTIONS
                if (rc.getID() == rc.readBroadcast(LEAD_ARCHON_CHANNEL)) {
                    int prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
                    rc.broadcast(GARDENER_CHANNEL, 0);

                    if (prevNumGard < GARDENER_MAX) {
                        tryBuild(RobotType.GARDENER);
                        rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
                    }

                }
                //DO NON LEADER ACTIONS
                if (rc.getInitialArchonLocations(rc.getTeam()).length > 1) {
                    //spawn the gardeners after x amount of rounds
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

               //to keep live analysis of gardeners
               int prev = rc.readBroadcast(GARDENER_CHANNEL);
               rc.broadcast(GARDENER_CHANNEL, prev + 1);

               //for the first 150 rounds
               if (rc.getRoundNum() < 150) {

                   int prevNumLum = rc.readBroadcast(LUMBERJACK_CHANNEL);
                   if (rc.senseNearbyTrees(3f).length > 3) {
                       if (prevNumLum < LUMBERJACK_MAX) {
                           if (tryBuild(RobotType.LUMBERJACK)) {
                               rc.broadcast(LUMBERJACK_CHANNEL, prevNumLum + 1);
                           }
                       }
                   }

                   //look for optimal position to plant trees while trying to spawn scouts
                   lookForOptimal(rc);

                   int prevNumScout = rc.readBroadcast(SCOUT_CHANNEL);
                   if (prevNumScout < SCOUT_MAX) {
                       if (tryBuild(RobotType.SCOUT)) {
                           rc.broadcast(SCOUT_CHANNEL, prevNumScout + 1);
                       }

                   }
                   int prevNumSold = rc.readBroadcast(SOLDIER_CHANNEL);

                   if (prevNumScout == 1 && prevNumSold < SOLDIER_MAX && rc.getTeamBullets() > 100.00) {
                       if (tryBuild(RobotType.SOLDIER)) {
                           rc.broadcast(SOLDIER_CHANNEL, prevNumSold + 1);
                       }
                   }

               }
               //After the gardener has built 1 scout + 2 unit robots == > build trees.
               else {
                   if (rc.senseNearbyTrees(rc.getLocation(), 1.7f, rc.getTeam()).length < 4) {
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
                       if (trees[j].getHealth() <  BULLET_TREE_MAX_HEALTH - 5) {
                           if (rc.canWater(trees[j].getID())) {
                               rc.water(trees[j].getID());
                               break;
                           }
                       }
                   }

                   tryBuild(RobotType.SOLDIER);

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
                dodge();
                Boolean archonDead = rc.readBroadcastBoolean(ENEMY_ARCHON_KILLED);
                Boolean treeNotAhead = true;
                TreeInfo[] trees = rc.senseNearbyTrees(1.5f, rc.getTeam().opponent());
                for (TreeInfo tree: trees) {
                    treeNotAhead = false;
                    break;
                }



                RobotInfo[] bots = rc.senseNearbyRobots(8f, rc.getTeam().opponent());
                for (RobotInfo bot: bots) {
                    Direction toward= rc.getLocation().directionTo(bot.getLocation());
                    if (bot.getType() != RobotType.ARCHON && treeNotAhead) {
                        goTowards(bot.getLocation());
                        if (rc.canFireTriadShot()) {
                            rc.fireTriadShot(toward);
                        } else if (rc.canFireSingleShot()) {
                            rc.fireSingleShot(toward);
                        }
                        break;
                    } else if (bot.getType() == RobotType.ARCHON && treeNotAhead) {
                        goTowards(bot.getLocation());
                        if (rc.canFirePentadShot()) {
                            rc.firePentadShot(toward);
                        } else if (rc.canFireTriadShot()) {
                            rc.fireTriadShot(toward);
                        } else if (rc.canFireSingleShot()) {
                            rc.fireSingleShot(toward);
                        }
                        break;
                    } else {
                        moveToTarget(bot.getLocation());
                    }
                }



                //check if we should go for another archon
                if (rc.getLocation().distanceTo(rc.getInitialArchonLocations(rc.getTeam().opponent())[rc.readBroadcast(INITIAL_ENEMY_ARCHON_CHANNEL)]) < 3f && !archonDead) {
                    //this will execute if no archon is found in the range
                    if (!checkArchon(rc)) {
                        int initialArchon = rc.readBroadcast(INITIAL_ENEMY_ARCHON_CHANNEL);
                        System.out.println(initialArchon);
                        System.out.println(rc.getInitialArchonLocations(rc.getTeam().opponent()).length - 1);
                        if (initialArchon < rc.getInitialArchonLocations(rc.getTeam().opponent()).length - 1) {
                            rc.broadcast(INITIAL_ENEMY_ARCHON_CHANNEL, initialArchon + 1);
                            rc.broadcastBoolean(ENEMY_ARCHON_KILLED, false);
                        } else {
                            rc.broadcastBoolean(ENEMY_ARCHON_KILLED, true);
                        }
                    }
                }



                if (!rc.hasAttacked()) {
                    int initialArchon = rc.readBroadcast(INITIAL_ENEMY_ARCHON_CHANNEL);
                    if (!archonDead) {
                        goTowards(rc.getInitialArchonLocations(rc.getTeam().opponent())[initialArchon]);
                        if (!rc.hasMoved()) {
                            moveToTarget(rc.getInitialArchonLocations(rc.getTeam().opponent())[initialArchon]);
                        }
                    } else {
                        System.out.println("Is move thing working");
                        lookAround(rc);
                    }
                }

                Clock.yield();

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

                TreeInfo[] trees = rc.senseNearbyTrees();
                for (TreeInfo t: trees) {
                    if (t.getContainedBullets() > 0) {
                        if (!rc.hasMoved()) {
                            if (rc.canMove(t.getLocation())) {
                                rc.move(t.getLocation());
                            }
                        }
                    }
                    if (rc.canShake(t.getLocation())){
                        System.out.println("I shook?");
                        rc.shake(t.getID());
                    }
                }



                RobotInfo[] bots = rc.senseNearbyRobots();
                for (RobotInfo b: bots) {
                    if (b.getTeam() != rc.getTeam() && b.getType() == RobotType.ARCHON) {
                        writeLocation(b.getLocation(), ENEMY_ARCHON_CHANNEL);
                        rc.broadcast(ENEMY_ARCHON_SPOTTED, rc.getRoundNum());
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
        //make the initial broadcast a number that's very high
        if (rc.readBroadcast(TREES_CHANNEL) == 0 && rc.readBroadcast(FIRST_ARCHON) == 0) {
            rc.broadcast(TREES_CHANNEL, 10000);
            rc.broadcast(FIRST_ARCHON, rc.readBroadcast(FIRST_ARCHON) + 1);
        }

        if (rc.getRoundNum() == 1) {
            if (rc.senseNearbyTrees().length < rc.readBroadcast(TREES_CHANNEL)) {
                rc.broadcast(TREES_CHANNEL, rc.senseNearbyTrees().length);
            }
        }

        else if (rc.getRoundNum() == 2 && rc.readBroadcast(LEAD_ARCHON_CHANNEL) == 0
                && rc.readBroadcast(TREES_CHANNEL) == rc.senseNearbyTrees().length) {
            rc.broadcast(LEAD_ARCHON_CHANNEL, rc.getID());
        }

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

    /*
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
            System.out.println("no robot can be made :(");
        }
    }*/

    public static void initDirList() {
        for(int i = 0; i < 9; i ++) {
            float radians = (float) (2*Math.PI * ((float)i)/4);
            dirList[i] = new Direction(radians);
        }
    }

    public static Boolean tryBuild(RobotType type) throws GameActionException {
        Boolean soldierBuilt = false;
        for (int i = 0; i < 9; i++) {
            if (rc.canBuildRobot(type, dirList[i])) {
                rc.buildRobot(type, dirList[i]);
                soldierBuilt = true;
                break;
            }
            System.out.println("Can't build soldiers");
        }
        return soldierBuilt;
    }




    public static void lookForOptimal(RobotController rc) throws GameActionException {
        RobotInfo[] bots = rc.senseNearbyRobots(rc.getLocation(), 3.00f, rc.getTeam());
        TreeInfo[] trees = rc.senseNearbyTrees(3.00f);
        if (bots.length == 0 && trees.length == 0) {
            System.out.println("I can plant my own trees!");
        } else {
            lookAround(rc);
        }
    }

    public static void fixLocation(RobotController rc, RobotInfo bot) throws GameActionException {
        MapLocation enemyLocation = bot.getLocation();
        MapLocation friendlyLocation = rc.getLocation();
        //provide how many steps to move in direction of enemy robot
        float distanceAway = friendlyLocation.distanceTo(enemyLocation);
        float distanceToMove = Math.abs(4.00f - distanceAway);
        Direction toward = friendlyLocation.directionTo(enemyLocation);
        moveToTarget(friendlyLocation.add(toward, distanceToMove));
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

    public static void swarm(MapLocation location, int quantity) throws GameActionException{
        int rollcall = 0;
        if (swarm) {
            moveToTarget(rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);
            return;
        }
        RobotInfo[] bots = rc.senseNearbyRobots();
        for (RobotInfo b : bots) {
            if (b.getTeam() == rc.getTeam() && b.getType() == RobotType.SOLDIER) {
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

    public static void updateArchon(RobotController rc) throws GameActionException {
        RobotInfo[] bots = rc.senseNearbyRobots();
        for (RobotInfo bot: bots) {
            if (bot.getTeam() != rc.getTeam() && bot.getType() == RobotType.ARCHON) {
                return;
            }
        }
        int ArchonNum = rc.readBroadcast(INITIAL_ENEMY_ARCHON_CHANNEL);
        myDest = null;
        rc.broadcast(INITIAL_ENEMY_ARCHON_CHANNEL, ArchonNum + 1);

    }

    public static void lookAround(RobotController rc) throws GameActionException {
        if (rc.canMove(goingDir)){
            rc.move(goingDir);
        } else {
            goingDir = randomDirection();
        }

    }

    public static Boolean checkArchon(RobotController rc) throws GameActionException {
        Boolean Archon = false;
        RobotInfo[] possibleArchon = rc.senseNearbyRobots(8f, rc.getTeam().opponent());
        for (RobotInfo bot: possibleArchon) {
            if (bot.getType() == RobotType.ARCHON) {
                //Archon Alive!!!
                Archon = true;
                break;
            }
        }
        return Archon;
    }


}
