package _08final.mvc.model;



import _08final.mvc.controller.Game;
import _08final.mvc.controller.Sound;
import lombok.Data;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

//the lombok @Data gives us automatic getters and setters on all members
@Data
public class CommandCenter {

	private  int numFalcons;
	private  int level;
	private  long score = -100;
	private  boolean paused;
	private  String highScore = "Nobody:0";

	//the falcon is located in the movFriends list, but since we use this reference a lot, we keep track of it in a
	//separate reference. Use final to ensure that the falcon ref always points to the single falcon object on heap
	//Lombok will not provide setter methods on final members
	private final Falcon falcon  = new Falcon();

	//lists containing our movables
	private final List<Movable> movDebris = new LinkedList<>();
	private final List<Movable> movFriends = new LinkedList<>();
	private final List<Movable> movFoes = new LinkedList<>();
	private final List<Movable> movFloaters = new LinkedList<>();

	private final GameOpsQueue opsQueue = new GameOpsQueue();

	//singleton
	private static CommandCenter instance = null;

	// Constructor made private
	private CommandCenter() {}

    //this class maintains game state - make this a singleton.
	public static CommandCenter getInstance(){
		if (instance == null){
			instance = new CommandCenter();
		}
		return instance;
	}


	public void initGame(){
		clearAll();
		setLevel(0);
		setPaused(false);
		//set to one greater than number of falcons lives in your game as initFalconAndDecrementNum() also decrements
		setNumFalcons(2);
		initFalconAndDecrementFalconNum();
		opsQueue.enqueue(falcon, GameOp.Action.ADD);
		if (highScore.equals("Nobody:0")) {
			highScore = this.readHighScore();
		}
	}

	public String getHighScore() {
		return this.highScore;
	}
	public void initFalconAndDecrementFalconNum(){
		setNumFalcons(getNumFalcons() - 1);
		if (isGameOver()) return;
		Sound.playSound("shipspawn.wav");
		falcon.setFade(Falcon.FADE_INITIAL_VALUE);
		//put falcon in the middle of the game-space
		falcon.setCenter(new Point(Game.DIM.width / 2, Game.DIM.height / 2));
		falcon.setOrientation(270);
		falcon.setDeltaX(0);
		falcon.setDeltaY(0);
	}

	private void clearAll(){
		movDebris.clear();
		movFriends.clear();
		movFoes.clear();
		movFloaters.clear();
	}

	public boolean isGameOver() {		//if the number of falcons is zero, then game over
		return getNumFalcons() <= 0;
	}

	public String readHighScore() {
		FileReader readFile = null;
		BufferedReader reader = null;
		try {
			readFile = new FileReader("highscore.dat");
			reader = new BufferedReader(readFile);
			return reader.readLine();
		}

		catch (Exception e) {
			return "Nobody:0";
		}
		finally {
			try {
				if (reader != null) {
					reader.close();
				}

			} catch (Exception e) {

			}

		}


	}

	public void checkScore() {
		if ((int) this.score > Integer.parseInt(this.highScore.split(":")[1])) {
			String name = JOptionPane.showInputDialog("You set a new high score! What's your name?");
			this.highScore = name + ":" + this.score;

			File scoreFile = new File("highscore.dat");
			if (!scoreFile.exists()) {
				try {
					scoreFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			FileWriter writeFile = null;
			BufferedWriter writer = null;
			try {
				writeFile = new FileWriter(scoreFile);
				writer = new BufferedWriter(writeFile);
				writer.write(this.highScore);
			} catch (Exception e) {

			} finally {
				{
					try {
						if (writer != null) {
							writer.close();
						}
					} catch (Exception e) {
					}
				}
			}
		}
	}

}
