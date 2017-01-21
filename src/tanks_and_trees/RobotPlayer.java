package tanks_and_trees;
import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;
	/**
	 * run() is the method that is called when a robot is instantiated in the Battlecode world.
	 * If this method returns, the robot dies!
	 **/
	@SuppressWarnings("unused")

	// Keep broadcast channels
	static int GARDENER_CHANNEL = 5;
	static int LUMBERJACK_CHANNEL = 6;
	static int last_broadcastx = 0; 
	static int last_broadcasty = 0; 
	

	// Keep important numbers here
	static int GARDENER_MAX = 4;
	static int LUMBERJACK_MAX = 10;
	public static void run(RobotController rc) throws GameActionException {

		// This is the RobotController object. You use it to perform actions from this robot,
		// and to get information on its current status.
		RobotPlayer.rc = rc;


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

		case TANK:
			runTank();
			break;    
		}
	}

	static void runArchon() throws GameActionException {
		System.out.println("I'm an archon!");

		// The code you want your robot to perform every round should be in this loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				Donate();
				// Generate a random direction
				Direction dir = randomDirection();
				
				// Randomly attempt to build a gardener in this direction
				int prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
				rc.broadcast(GARDENER_CHANNEL, 0);
				if (prevNumGard < GARDENER_MAX && rc.canHireGardener(dir)) {
					rc.hireGardener(dir);
					rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
				}
				// Move randomly
				tryMove(randomDirection());

				// Broadcast archon's location for other robots on the team to know
				MapLocation myLocation = rc.getLocation();
				rc.broadcast(0,(int)myLocation.x);
				rc.broadcast(1,(int)myLocation.y);

				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Archon Exception");
				e.printStackTrace();
			}
		}
	}

	static void runGardener() throws GameActionException {
	/*	System.out.println("I'm a gardener!");

		// The code you want your robot to perform every round should be in this loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				
				// Listen for home archon's location
				int xPos = rc.readBroadcast(0);
				int yPos = rc.readBroadcast(1);
				MapLocation archonLoc = new MapLocation(xPos,yPos);

				// Generate a random direction
				Direction dir = randomDirection();

				// Randomly attempt to build a soldier or lumberjack in this direction


				if (rc.canBuildRobot(RobotType.TANK, dir) && Math.random()<.6 && rc.getRoundNum() > 2000) {
					rc.buildRobot(RobotType.TANK, dir);
					System.out.println("PLANTING THAT TANK NIGGA");
				}

				else if(rc.canBuildRobot(RobotType.SCOUT, dir) && rc.getRoundNum() < 150) {

					rc.buildRobot(RobotType.SCOUT, dir);
					System.out.println("PLANTING THAT SCOUT NIGGA");
				}




			*/
		
	        System.out.println("I'm a gardener!");

	        // The code you want your robot to perform every round should be in this loop
	        while (true) {

	            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
	            try {
	            	Direction dir = randomDirection();
	        
	            	if( rc.getRoundNum() < 50 && rc.canBuildRobot(RobotType.SCOUT, dir)) {
	            		
						rc.buildRobot(RobotType.SCOUT, dir);
						System.out.println("PLANTING THAT SCOUT NIGGA");
					}

	           
	            	int treecount = 0;
	            	int botcount = 0;
	            	RobotInfo[] robot = rc.senseNearbyRobots();
	            	
	            if(rc.getRoundNum() >50){	
	            	
	            	for (int k = 0; k < robot.length; k++ ){
	            		System.out.println(robot[k].getType());
	            		//if (robot[k].getType() == Gardener){
	            			
	            		//	tryMove(randomDirection());
	            		//}
	            	}	
	            	
	            	
	            	
	            		Direction direction_start = Direction.getEast();
	            		for (int i = 0; i < 5 ; i++){

	            			if (rc.canPlantTree(direction_start.rotateRightDegrees(60*i))) {
	            				rc.plantTree(direction_start.rotateRightDegrees(60*i));
	            			}
	            			else{
	            				treecount++;
	            			}
	            			if (treecount == 5) {
	            				TreeInfo[] tree_list = rc.senseNearbyTrees();
	            				for (int j = 0; j < tree_list.length ; j++) {
	            					if(tree_list[j].getHealth() < (GameConstants.BULLET_TREE_MAX_HEALTH - 10)){

	            						rc.water(tree_list[j].getID());
	            						break;
	            					}
	            					else {
	            						botcount++;
	            					}
	            					if (botcount == 5) {
	            						if (rc.canBuildRobot(RobotType.SOLDIER, direction_start.rotateRightDegrees(60*5))) {
	            		                    rc.buildRobot(RobotType.SOLDIER, direction_start.rotateRightDegrees(60*5));
	            		                
	            					}
	            					
	            					
	            					}
	            			}
	            		}
	            	}
	            	
	            }	
	            	
	            	
	            } catch (Exception e) {
	                System.out.println("Gardener Exception");
	                e.printStackTrace();
	            }
	        }
	    }
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
	
	

	static void runSoldier() throws GameActionException {
		System.out.println("I'm an soldier!");
		Team enemy = rc.getTeam().opponent();

		// The code you want your robot to perform every round should be in this loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				dodge();
				MapLocation myLocation = rc.getLocation();
			    SeekandDestroy(15);
				
				
				// See if there are any nearby enemy robots
				RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

				// If there are some...
				if (robots.length > 0) {
					Broadcastenemy(robots);
					if (rc.canFirePentadShot()) {
						// ...Then fire a bullet in the direction of the enemy.
						rc.firePentadShot(rc.getLocation().directionTo(robots[0].location));
					}
				}

				Clock.yield();

			} catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}
	}


	static void runScout() throws GameActionException {
		System.out.println("I'm an scout!");
		Team enemy = rc.getTeam().opponent();

		// The code you want your robot to perform every round should be in this loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				MapLocation myLocation = rc.getLocation();

				// See if there are any nearby enemy robots
				RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

				// If there are some...
				if (robots.length > 0) {
					//Broadcast their location!!!
					Broadcastenemy(robots);
					// And we have enough bullets, and haven't attacked yet this turn...
					if (rc.canFireSingleShot()) {
						// ...Then fire a bullet in the direction of the enemy.
						rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
					}
				}


				TreeInfo[] tree_list =  rc.senseNearbyTrees(-1,Team.NEUTRAL);



				for (int i = 0; i < tree_list.length ; i++) {
					if(tree_list[i].getContainedBullets() > 0){
						if(!rc.hasMoved()){
							if(rc.canMove(tree_list[i].getLocation())){

								rc.move(tree_list[i].getLocation());
								rc.shake(tree_list[i].getID());
								if(!rc.canShake()){
									System.out.println("I shook it");
									break;
								}
								break;
							}
							else{
								Direction direction_moved = rc.getLocation().directionTo(tree_list[i].getLocation());
								if(!rc.hasMoved()){
									tryMove(direction_moved);}

							}


							break;
						}
					}
				}			



				if(rc.getRoundNum() < 70){
					MapLocation archonLoc = rc.getInitialArchonLocations(enemy)[0];
					Direction enemydirection = new Direction(rc.getLocation(), archonLoc);
					tryMove(enemydirection);
					if(rc.hasMoved()){
						System.out.println("scouting enemy archon");}
				}

				
				
				int enemyxPos = 0;
				int enemyyPos = 0;


				enemyxPos = rc.readBroadcast(2);
				enemyyPos = rc.readBroadcast(3);
				MapLocation enemyLoc = new MapLocation(enemyxPos,enemyyPos);

				Direction enemydirection = new Direction(rc.getLocation(), enemyLoc);


				if(enemyxPos != 0  && rc.getRobotCount() > 5 && !rc.hasMoved()){
					System.out.println("Moving Towards Enemy: ("+ enemyxPos + ", " + enemyyPos+")" );
					tryMove(enemydirection);
					last_broadcastx = enemyxPos;

				}
				
				
				
				
				if(!rc.hasMoved()){

					tryMove(randomDirection());
				}
				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();

			}
			catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}
	}

	static void runTank() throws GameActionException {
		System.out.println("I'm a Tank");
		Team enemy = rc.getTeam().opponent();

		// The code you want your robot to perform every round should be in this loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				dodge();
				MapLocation myLocation = rc.getLocation();

				// See if there are any nearby enemy robots
				RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

				// If there are some...
				if (robots.length > 0) {
					// And we have enough bullets, and haven't attacked yet this turn...
					if (rc.canFirePentadShot()) {
						// ...Then fire a bullet in the direction of the enemy.
						rc.firePentadShot(rc.getLocation().directionTo(robots[0].location));
					}
				}

				// Move randomly

				int enemyxPos = 0;
				int enemyyPos = 0;


				enemyxPos = rc.readBroadcast(2);
				enemyyPos = rc.readBroadcast(3);
				MapLocation enemyLoc = new MapLocation(enemyxPos,enemyyPos);

				Direction enemydirection = new Direction(rc.getLocation(), enemyLoc);


				if(enemyxPos != 0 && last_broadcastx!= enemyxPos && rc.getRobotCount() > 15 && !rc.hasMoved()){
					System.out.println("Moving Towards Enemy: ("+ enemyxPos + ", " + enemyyPos+")" );
					tryMove(enemydirection);
					last_broadcastx = enemyxPos;

				}
				else{

					if(!rc.hasMoved()){
						System.out.println("Moving in Random Direction");
						tryMove(randomDirection());}

				}
				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Tank Exception");
				e.printStackTrace();
			}
		}
	}
	static void runLumberjack() throws GameActionException {
		System.out.println("I'm a lumberjack!");
		Team enemy = rc.getTeam().opponent();

		// The code you want your robot to perform every round should be in this loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {

				// See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
				RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

				if(robots.length > 0 && !rc.hasAttacked()) {
					// Use strike() to hit all nearby robots!
					rc.strike();
				} else {
					// No close robots, so search for robots within sight radius
					robots = rc.senseNearbyRobots(-1,enemy);

					// If there is a robot, move towards it
					if(robots.length > 0) {
						MapLocation myLocation = rc.getLocation();
						MapLocation enemyLocation = robots[0].getLocation();
						Direction toEnemy = myLocation.directionTo(enemyLocation);

						tryMove(toEnemy);

						if(tryMove(toEnemy) == false ){
							tryMove(randomDirection());
							System.out.println("RANDOM INSTEAD");
						}
					} else {
						// Move Randomly
						tryMove(randomDirection());
					}
				}

				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Lumberjack Exception");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns a random Direction
	 * @return a random Direction
	 */
	static Direction randomDirection() {
		return new Direction((float)Math.random() * 2 * (float)Math.PI);
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
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}

		// Now try a bunch of similar angles
		boolean moved = false;
		int currentCheck = 1;

		while(currentCheck<=checksPerSide) {
			// Try the offset of the left side
			if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
				rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
				return true;
			}
			// Try the offset on the right side
			if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
				rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
				return true;
			}
			// No move performed, try slightly further
			currentCheck++;
		}

		// A move never happened, so return false.
		return false;
	}

	/**
	 * A slightly more complicated example function, this returns true if the given bullet is on a collision
	 * course with the current robot. Doesn't take into account objects between the bullet and this robot.
	 *
	 * @param bullet The bullet in question
	 * @return True if the line of the bullet's path intersects with this robot's current position.
	 */
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
		if (Math.abs(theta) > Math.PI/2) {
			return false;
		}

		// distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
		// This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
		// This corresponds to the smallest radius circle centered at our location that would intersect with the
		// line that is the path of the bullet.
		float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

		return (perpendicularDist <= rc.getType().bodyRadius);
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
			if (willCollideWithMe(bi) && !rc.hasMoved()) {
				trySidestep(bi);
			}
		}

	}

	static void Broadcastenemy(RobotInfo[] robots) throws GameActionException {
		System.out.println("Broadcasting enemy location: ");
		rc.broadcast(2,(int)robots[0].getLocation().x);
		rc.broadcast(3,(int)robots[0].getLocation().y);

	}

	static void Donate() throws GameActionException {
		if(rc.getRobotCount() > 75 && rc.getRoundNum() > 1200)
			
			if(rc.getTeamBullets() > 100){
			
			rc.donate(rc.getTeamBullets() - 100);
			System.out.println("DONATING BULLETS");
			
			
			
			}

	}
	
	
	
	static void SeekandDestroy(int reinforcements)  throws GameActionException {

		int enemyxPos = 0;
		int enemyyPos = 0;


		enemyxPos = rc.readBroadcast(2);
		enemyyPos = rc.readBroadcast(3);
		MapLocation enemyLoc = new MapLocation(enemyxPos,enemyyPos);

		Direction enemydirection = new Direction(rc.getLocation(), enemyLoc);

		if(enemyxPos != 0  && (last_broadcastx != enemyxPos || last_broadcasty != enemyyPos) && rc.getRobotCount() > reinforcements && !rc.hasMoved()){
			System.out.println("Moving Towards Enemy: ("+ enemyxPos + ", " + enemyyPos+")" );
			tryMove(enemydirection);
			last_broadcastx = enemyxPos;
			last_broadcasty = enemyyPos;


		}
		else{

			if(!rc.hasMoved()){
				System.out.println("Moving in Random Direction");
				tryMove(randomDirection());}

		}
	
	}
}


