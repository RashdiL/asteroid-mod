package javagame.mvc.controller;

import javagame.mvc.model.*;
import javagame.mvc.view.GamePanel;


import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

// ===============================================
// == This Game class is the CONTROLLER
// ===============================================

public class Game implements Runnable, KeyListener {

	// ===============================================
	// FIELDS
	// ===============================================

	public static final Dimension DIM = new Dimension(1100, 900); //the dimension of the game.
	private GamePanel gmpPanel;
	//this is used throughout many classes.
	public static Random R = new Random();

	public final static int ANI_DELAY = 40; // milliseconds between screen
											// updates (animation)

	public final static int FRAMES_PER_SECOND = 1000 / ANI_DELAY;

	private Thread animationThread;

	private boolean muted = false;
	enum spawnType {
		VERTICAL,
		HORIZONTAL,
		COMBO_EASY,
		COMBO_MEDIUM,
		COMBO_HARD;
		public static spawnType getRandomSpawn() {
			Random random = new Random();
			return values()[random.nextInt(values().length)];
		}
	}

	private final int PAUSE = 80, // p key
			QUIT = 81, // q key
			LEFT = 37, // rotate left; left arrow
			RIGHT = 39, // rotate right; right arrow
			UP = 38, // thrust; up arrow
			DOWN = 40, // move down; down arrow
			START = 83, // s key

			MUTE = 77; // m-key mute

	// for possible future use
	// HYPER = 68, 					// D key
	// SHIELD = 65, 				// A key
	// SPECIAL = 70; 					// fire special weapon;  F key

	private Clip clpThrust;
	private Clip clpMusicBackground;

	//spawn every 30 seconds
	private static final int SPAWN_NEW_SHIP_FLOATER = FRAMES_PER_SECOND * 10;



	// ===============================================
	// ==CONSTRUCTOR
	// ===============================================

	public Game() {

		gmpPanel = new GamePanel(DIM);
		gmpPanel.addKeyListener(this); //Game object implements KeyListener
		clpThrust = Sound.clipForLoopFactory("whitenoise.wav");
		clpMusicBackground = Sound.clipForLoopFactory("background-music.wav");
		//fire up the animation thread
		animationThread = new Thread(this); // pass the animation thread a runnable object, the Game object
		animationThread.start();
		clpMusicBackground.loop(Clip.LOOP_CONTINUOUSLY);

		//Lower the volume of the sound clip.
		FloatControl gainControl = (FloatControl) clpMusicBackground.getControl(FloatControl.Type.MASTER_GAIN);
		double gain = 0.5;
		float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
		gainControl.setValue(dB);

	}
	public Clip getClpMusicBackground(){
		return clpMusicBackground;
	}

	// ===============================================
	// ==METHODS
	// ===============================================

	public static void main(String args[]) {
		//typical Swing application start; we pass EventQueue a Runnable object.
		EventQueue.invokeLater(Game::new);
	}

	// Game implements runnable, and must have run method
	@Override
	public void run() {

		// lower animation thread's priority, thereby yielding to the "main" aka 'Event Dispatch'
		// thread which listens to keystrokes
		animationThread.setPriority(Thread.MIN_PRIORITY);

		// and get the current time
		long lStartTime = System.currentTimeMillis();

		// this thread animates the scene
		while (Thread.currentThread() == animationThread) {
			gmpPanel.update(gmpPanel.getGraphics()); // see GamePanel class
			checkCollisions();
			checkNewLevel();
			spawnNewShipFloater();

			// surround the sleep() in a try/catch block
			// this simply controls delay time between
			// the frames of the animation
			try {
				// The total amount of time is guaranteed to be at least ANI_DELAY long.  If processing (update) 
				// between frames takes longer than ANI_DELAY, then the difference between lStartTime - 
				// System.currentTimeMillis() will be negative, then zero will be the sleep time
				lStartTime += ANI_DELAY;

				Thread.sleep(Math.max(0,
						lStartTime - System.currentTimeMillis()));
			} catch (InterruptedException e) {
				// do nothing (bury the exception), and just continue, e.g. skip this frame -- no big deal
			}
		} // end while
	} // end run

	private void checkCollisions() {

		Point pntFriendCenter, pntFoeCenter;
		int radFriend, radFoe;

		//Here I want to remove asteroids once they reach the bottom of the screen.
		for (Movable movFoe : CommandCenter.getInstance().getMovFoes()) {
			pntFoeCenter = movFoe.getCenter();
			if (pntFoeCenter.y == Game.DIM.height || pntFoeCenter.y == 0 || pntFoeCenter.x == Game.DIM.width || pntFoeCenter.x == 0)  {
				CommandCenter.getInstance().getOpsQueue().enqueue(movFoe, GameOp.Action.REMOVE);
			}
		}

		//This has order-of-growth of O(n^2), there is no way around this.
		for (Movable movFriend : CommandCenter.getInstance().getMovFriends()) {
			for (Movable movFoe : CommandCenter.getInstance().getMovFoes()) {

				pntFriendCenter = movFriend.getCenter();
				pntFoeCenter = movFoe.getCenter();
				radFriend = movFriend.getRadius();
				radFoe = movFoe.getRadius();

				//detect collision
				if (pntFriendCenter.distance(pntFoeCenter) < (radFriend + radFoe)) {
					//remove the friend (so long as he is not protected)
					if (!movFriend.isProtected()){
						CommandCenter.getInstance().getOpsQueue().enqueue(movFriend, GameOp.Action.REMOVE);
					}
					//remove the foe
					CommandCenter.getInstance().getOpsQueue().enqueue(movFoe, GameOp.Action.REMOVE);
					Sound.playSound("kapow.wav");
				 }

			}//end inner for
		}//end outer for

		//check for collisions between falcon and floaters. Order of growth of O(n) where n is number of floaters
		Point pntFalCenter = CommandCenter.getInstance().getFalcon().getCenter();
		int radFalcon = CommandCenter.getInstance().getFalcon().getRadius();

		Point pntFloaterCenter;
		int radFloater;
		for (Movable movFloater : CommandCenter.getInstance().getMovFloaters()) {
			pntFloaterCenter = movFloater.getCenter();
			radFloater = movFloater.getRadius();
	
			//detect collision
			if (pntFalCenter.distance(pntFloaterCenter) < (radFalcon + radFloater)) {

				CommandCenter.getInstance().getOpsQueue().enqueue(movFloater, GameOp.Action.REMOVE);
				Sound.playSound("pacman_eatghost.wav");
				long oldScore = CommandCenter.getInstance().getScore();
				CommandCenter.getInstance().setScore(oldScore + 50);
			}//end if
		}//end for

		processGameOpsQueue();

	}//end meth

	private void processGameOpsQueue() {

		//deferred mutation: these operations are done AFTER we have completed our collision detection to avoid
		// mutating the movable linkedlists while iterating them above
		while(!CommandCenter.getInstance().getOpsQueue().isEmpty()){
			GameOp gameOp =  CommandCenter.getInstance().getOpsQueue().dequeue();
			Movable mov = gameOp.getMovable();
			GameOp.Action action = gameOp.getAction();

			switch (mov.getTeam()){
				case FOE:
					if (action == GameOp.Action.ADD){
						CommandCenter.getInstance().getMovFoes().add(mov);
					} else { //GameOp.Operation.REMOVE
						CommandCenter.getInstance().getMovFoes().remove(mov);

						//I don't want to spawn smaller asteroids once the asteroid collides with the bottom of the screen.
						/*
						if (mov instanceof Asteroid)
							spawnSmallerAsteroids((Asteroid) mov);
						 */
					}

					break;
				case FRIEND:
					if (action == GameOp.Action.ADD){
						CommandCenter.getInstance().getMovFriends().add(mov);
					} else { //GameOp.Operation.REMOVE
						if (mov instanceof Falcon) {
							CommandCenter.getInstance().initFalconAndDecrementFalconNum();
						} else {
							CommandCenter.getInstance().getMovFriends().remove(mov);
						}
					}
					break;

				case FLOATER:
					if (action == GameOp.Action.ADD){
						CommandCenter.getInstance().getMovFloaters().add(mov);
					} else { //GameOp.Operation.REMOVE
						CommandCenter.getInstance().getMovFloaters().remove(mov);
					}
					break;

				case DEBRIS:
					if (action == GameOp.Action.ADD){
						CommandCenter.getInstance().getMovDebris().add(mov);
					} else { //GameOp.Operation.REMOVE
						CommandCenter.getInstance().getMovDebris().remove(mov);
					}
					break;


			}

		}
	}


	private void spawnNewShipFloater() {

		//appears more often as your level increases.
		if (((System.currentTimeMillis() / ANI_DELAY) % (SPAWN_NEW_SHIP_FLOATER - CommandCenter.getInstance().getLevel() * 7L) == 0) && !CommandCenter.getInstance().isGameOver()) {
			Falcon fal = CommandCenter.getInstance().getFalcon();

			EnemyShip newEnemy = new EnemyShip();
			newEnemy.setCenter(new Point(fal.getCenter().x, 50));
			newEnemy.setDeltaX(0);
			newEnemy.setDeltaY(0);
			CommandCenter.getInstance().getOpsQueue().enqueue(newEnemy, GameOp.Action.ADD);

			CommandCenter.getInstance().getOpsQueue().enqueue(new Bullet(newEnemy, 8), GameOp.Action.ADD);
			Sound.playSound("laser.wav");
		}
	}

	private void spawnNewCoin() {
		CommandCenter.getInstance().getOpsQueue().enqueue(new Coin(), GameOp.Action.ADD);
	}


	private void spawnAsteroid(spawnType movementDirection, int speed) {
		//Asteroids will always spawn from left to right or top to bottom.

		int deltaYMultiplier = 0;
		int deltaXMultiplier = 0;

		int isAsteroidMovingVertically = 0;
		int isAsteroidMovingHorizontally = 0;

		int numberOfAsteroids = 11;
		int wallGap = -2;

		int startingX = 50;
		int startingY  = 50;

		if (movementDirection == spawnType.VERTICAL || movementDirection == spawnType.HORIZONTAL) {
			if (movementDirection == spawnType.VERTICAL) {
				deltaYMultiplier = 1;
				isAsteroidMovingVertically = 1;
				wallGap = Game.R.nextInt(numberOfAsteroids - 2);
				int spawnLocation = Game.R.nextInt(2);
				if (spawnLocation == 1) {
					deltaYMultiplier = -1;
					startingY = 850;
				}
			} else if (movementDirection == spawnType.HORIZONTAL) {
				deltaXMultiplier = 1;
				isAsteroidMovingHorizontally = 1;
				numberOfAsteroids = 9;
				wallGap = Game.R.nextInt(numberOfAsteroids - 2);
				int spawnLocation = Game.R.nextInt(2);
				if (spawnLocation == 1) {
					deltaXMultiplier = -1;
					startingX = 1050;
				}
			}
			for (int i = 0; i < numberOfAsteroids; i++) {
				if (i == wallGap + 1 || i == wallGap) {
					continue;
				}
				Asteroid newAsteroid = new Asteroid(1);
				newAsteroid.setCenter(new Point(100 * i * isAsteroidMovingVertically + startingX, 100 * i * isAsteroidMovingHorizontally + startingY));
				newAsteroid.setDeltaY(speed * deltaYMultiplier);
				newAsteroid.setDeltaX(speed * deltaXMultiplier);
				CommandCenter.getInstance().getOpsQueue().enqueue(newAsteroid, GameOp.Action.ADD);
			}
		} else if (movementDirection == spawnType.COMBO_EASY) {
			int randomDirectionOne = 0;
			int randomDirectionTwo = 0;
			while (randomDirectionOne == randomDirectionTwo) {
				randomDirectionOne = Game.R.nextInt(4);
				randomDirectionTwo = Game.R.nextInt(4);
			}

			if (randomDirectionOne == 0 || randomDirectionTwo == 0) {
				//spawn top (even)
				spawnAsteroidsSparsly("top", speed, 0);
			}
			if (randomDirectionOne == 1 || randomDirectionTwo == 1) {
				//spawn bot (odd)
				spawnAsteroidsSparsly("bottom", speed, 1);
			}
			if (randomDirectionOne == 2 || randomDirectionTwo == 2) {
				//spawn left (even)
				spawnAsteroidsSparsly("left", speed, 0);
			}
			if (randomDirectionOne == 3 || randomDirectionTwo == 3) {
				//spawn right (odd)
				spawnAsteroidsSparsly("right", speed, 1);
			}
		} else if (movementDirection == spawnType.COMBO_MEDIUM) {
				spawnAsteroidsSparsly("top", speed, 0);
				spawnAsteroidsSparsly("left", speed - 3, 0);
				spawnAsteroidsSparsly("right", speed - 3, 1);
		} else {
			spawnAsteroidsSparsly("top", speed, 0);
			spawnAsteroidsSparsly("left", speed - 3, 0);
			spawnAsteroidsSparsly("right", speed - 3, 1);
			spawnAsteroidsSparsly("bottom", speed - 3, 1);
		}
	}

	private void spawnAsteroidsSparsly(String location, int speed, int isOdd) {
		int numberOfAsteroids = 11;

		int deltaYMultiplier = 0;
		int deltaXMultiplier = 0;

		int isAsteroidMovingVertically = 0;
		int isAsteroidMovingHorizontally = 0;

		int startingX = 50;
		int startingY  = 50;

		if (location == "left" || location == "right") {
			isAsteroidMovingHorizontally = 1;
			numberOfAsteroids = 9;
			deltaXMultiplier = 1;
			if (location == "right") {
				startingX = 1050;
				deltaXMultiplier = -1;
			}
		} else {
			isAsteroidMovingVertically = 1;
			deltaYMultiplier = 1;
			if (location == "bottom") {
				startingY = 850;
				deltaYMultiplier = -1;
			}
		}
		for (int i = 0; i < numberOfAsteroids; i++) {
			//if odd spawn left
			//if even spawn right
			if (i % 2 == isOdd) {
				Asteroid newAsteroid = new Asteroid(1);
				newAsteroid.setCenter(new Point(100 * i * isAsteroidMovingVertically + startingX, 100 * i * isAsteroidMovingHorizontally + startingY));
				newAsteroid.setDeltaY(speed * deltaYMultiplier);
				newAsteroid.setDeltaX(speed * deltaXMultiplier);
				CommandCenter.getInstance().getOpsQueue().enqueue(newAsteroid, GameOp.Action.ADD);
			}
		}
	}

	//this method spawns new Large (0) Asteroids
	private void spawnBigAsteroids(int nNum) {
		//Width is 1100 so when asteroids are moving downwards I'll spawn 10 from 50 to 1050
		//Height is 900 so when asteroids are moving left to right or vice versa, I'll spawn 8 from 50 to 850.

		//Easy levels
		int speed = 7;
		/*
		if (speed > 18) {
			speed = 18;
		}
		*/

		if (nNum < 3) {
			int randomDirection = Game.R.nextInt(2);
			if (randomDirection == 0) {
				spawnAsteroid(spawnType.VERTICAL, speed);
			} else {
				spawnAsteroid(spawnType.HORIZONTAL, speed);
			}
		} else if (nNum < 6) {
			spawnAsteroid(spawnType.COMBO_EASY, speed);
		} else if (nNum < 10){
			spawnAsteroid(spawnType.COMBO_MEDIUM, speed);
		} else {
			spawnAsteroid(spawnType.getRandomSpawn(), speed);
		}


	}



	
	private boolean isLevelClear(){
		//if there are no more Asteroids on the screen
		boolean asteroidFree = true;
		for (Movable movFoe : CommandCenter.getInstance().getMovFoes()) {
			if (movFoe instanceof Asteroid){
				asteroidFree = false;
				break;
			}
		}
		return asteroidFree;
	}
	
	private void checkNewLevel(){
		
		if (isLevelClear()) {
			//more asteroids at each level to increase difficulty
			int newLevel = CommandCenter.getInstance().getLevel() + 1;
			CommandCenter.getInstance().setLevel(newLevel);
			spawnBigAsteroids(newLevel);
			//setFade e.g. protect the falcon so that player has time to avoid newly spawned asteroids.
			//I don't want players to be protected after completing a level due to the nature of the game.
			//CommandCenter.getInstance().getFalcon().setFade(Falcon.FADE_INITIAL_VALUE);

			//increase the score after completing each level
			if (CommandCenter.getInstance().getLevel() != 1) {
				long currentScore = CommandCenter.getInstance().getScore();
				CommandCenter.getInstance().setScore(currentScore + 100);
			}


			//remove the coin from the level.
			for (Movable movFloater : CommandCenter.getInstance().getMovFloaters()) {
					CommandCenter.getInstance().getOpsQueue().enqueue(movFloater, GameOp.Action.REMOVE);
			}

			spawnNewCoin();
		}
	}
	
	
	

	// Varargs for stopping looping-music-clips
	private static void stopLoopingSounds(Clip... clpClips) {
		for (Clip clp : clpClips) {
			clp.stop();
		}
	}

	// ===============================================
	// KEYLISTENER METHODS
	// ===============================================


	@Override
	public void keyPressed(KeyEvent e) {
		Falcon fal = CommandCenter.getInstance().getFalcon();
		int nKey = e.getKeyCode();

		if (nKey == START && CommandCenter.getInstance().isGameOver())
			CommandCenter.getInstance().initGame();

		if (fal != null) {

			switch (nKey) {
			case PAUSE:
				CommandCenter.getInstance().setPaused(!CommandCenter.getInstance().isPaused());
				if (CommandCenter.getInstance().isPaused())
					stopLoopingSounds(clpMusicBackground, clpThrust);

				break;
			case QUIT:
				System.exit(0);
				break;
			case UP:
				fal.setOrientation(270);
				fal.thrustOn();
				if (!CommandCenter.getInstance().isPaused() && !CommandCenter.getInstance().isGameOver())
					clpThrust.loop(Clip.LOOP_CONTINUOUSLY);
				break;
			case LEFT:
				fal.setOrientation(180);
				fal.thrustOn();
				if (!CommandCenter.getInstance().isPaused() && !CommandCenter.getInstance().isGameOver())
					clpThrust.loop(Clip.LOOP_CONTINUOUSLY);
				break;
			case DOWN:
				fal.setOrientation(90);
				fal.thrustOn();
				if (!CommandCenter.getInstance().isPaused() && !CommandCenter.getInstance().isGameOver())
					clpThrust.loop(Clip.LOOP_CONTINUOUSLY);
				break;
			case RIGHT:
				fal.setOrientation(0);
				fal.thrustOn();
				if (!CommandCenter.getInstance().isPaused() && !CommandCenter.getInstance().isGameOver())
					clpThrust.loop(Clip.LOOP_CONTINUOUSLY);
				break;

			default:
				break;
			}
		}
	}


	@Override
	public void keyReleased(KeyEvent e) {
		Falcon fal = CommandCenter.getInstance().getFalcon();
		int nKey = e.getKeyCode();
		//show the key-code in the console

		if (fal != null) {
			if (nKey == LEFT || nKey == RIGHT) {
				fal.thrustOff();
				clpThrust.stop();
				fal.setDeltaX(0);
			}
			else if (nKey == UP || nKey == DOWN) {
				fal.thrustOff();
				clpThrust.stop();
				fal.setDeltaY(0);
			}
			else if (nKey == MUTE) {
				if (!muted) {
					stopLoopingSounds(clpMusicBackground);
				} else {
					clpMusicBackground.loop(Clip.LOOP_CONTINUOUSLY);
				}
				muted = !muted;
			}
		}
	}

	@Override
	// does nothing, but we need it b/c of KeyListener contract
	public void keyTyped(KeyEvent e) {
	}


}


