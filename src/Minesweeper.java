/**
 * Description: Main Game version 3
 * Date: June 17th, 2016
 * Author: Simon Zeng
 * The core of this game is based off of SliderV2 (my slider game), but many parts were redone and added to make this into a full fledged minesweeper game
 * This is the third version
 * v3.0 - QOL features (e.g. always start on completely empty spot, larger display with better color, better death flag handling, polished code, etc.)
 * Now with comments
 * Essentially identical to MainV3, with minor cleanups on generateGame and in the win popup
 */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;

import javax.imageio.ImageIO;
import javax.swing.*;

@SuppressWarnings("serial") //eclipse was bothering me about adding this, so i did
public class Minesweeper extends JFrame implements MouseListener{
	private int time = 0, gameSetting, minesLeft, cellsOpened; //time holds the timer for game time, gameSetting holds the setting (size + mines) that the game is being played at, minesLeft is amount of flags user has placed, and cellsOpened is amount of cells that the player has revealed
	private JButton[] buttons; //This array holds the buttons (each button is a cell)
	private JPanel gameboardPanel; //This panel holds the actual game
	private int[] boardState, currentBoardState, mines, checkers; //boardState holds the values of each cell, currentBoardState is what is currently being displayed to the user, and mines holds the indices of where the mines will be placed
	private int[] gameParameters = new int[3]; //If user decides to make custom minesweeper game, this array holds length, height and # mines in that order
	private Color[] colours = {Color.lightGray, Color.gray, Color.blue, Color.green, Color.red, Color.blue, Color.red, Color.cyan, Color.black, Color.darkGray}; //Each number traditionally has a color associated with it - this array makes displaying each number with their color in a loop trivial
	private boolean dead = false, solved = false, generated = false; //booleans that show whether the user has died, has solved the puzzle, or has clicked on at least one cell respectively
	private static boolean debugMode = false; //whether or not debug mode is on or not. this was made static to implement launching the jar file with console args, but that didn't work due to time constraints. console outputs and certain hotkeys will only work with debug mode on. Also updates the title with a DEBUG text as well as displaying the index of the cell that the user is currently mousing over
	private ImageIcon[] icons = new ImageIcon[3]; //holds the image for the mine, the flag, and the crossed out flag
	private String debugString = ""; //if debug mode is on then this becomes "DEBUG"
	private String[] difficulties = {"Easy", "Medium", "Hard", "Custom"}; //array of strings that displays the difficulty that the user is playing the game at
	
	public static void main(String[] args) {
		Minesweeper minesweeper = new Minesweeper();
		
		//Add our keyboard shortcuts here
		minesweeper.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

		    @Override
		    public void keyPressed(KeyEvent e) {
		    	if (debugMode)
		    		System.out.print("[DEBUG] Key pressed: "); //basic debug display
		    	if(e.getKeyCode() == KeyEvent.VK_S) {
					if (debugMode) {
						System.out.print("s");
						minesweeper.showAll(); //If debug mode is on, pressing s reveals all cells
					}
				}
		    	if(e.getKeyCode() == KeyEvent.VK_R) {
					if (debugMode) {
						System.out.print("r");
						minesweeper.resetState(); //If debug mode is on, pressing r resets the exact same game to unsolved state (no generating new puzzle)
					}
				}
				if (e.getKeyCode() == KeyEvent.VK_N) {
					if (debugMode) 
						System.out.print("n");
					minesweeper.newGame(); //Pressing n creates new game
				}
				if (e.getKeyCode() == KeyEvent.VK_C) {
					if (debugMode)
						System.out.print("c");
					minesweeper.sizeSet(); //Pressing c allows changing parameters
				}
				if (e.getKeyCode() == KeyEvent.VK_D) {
					System.out.print("Debug mode toggled o");
					debugMode = !debugMode; //And pressing d toggles debug mode
					if (debugMode) 
						System.out.print("n");
					else
						System.out.print("ff");
					minesweeper.titleUpdate(0);
				}
		    	System.out.println();
		    }

		    @Override
		    public void keyReleased(KeyEvent e) {
		    }});
	}
	
	public Minesweeper()
	{
		super("Minesweeper");
		try { //This is where we initialize the images
			icons[0] = new ImageIcon(ImageIO.read(getClass().getResource("mine-sweeper.png")));
			icons[1] = new ImageIcon(ImageIO.read(getClass().getResource("flag.png")));
			icons[2] = new ImageIcon(ImageIO.read(getClass().getResource("flagx.png")));
		} catch (Exception e) {
			System.out.println("Pictures not found!");
			System.exit(1);
		}
		
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//Display instructions to user
		JOptionPane.showMessageDialog(null, "This is a run of the mill minesweeper game. \nStandard controls apply: left click to clear a cell, and right click to flag it as a mine. \nYou may also left click a cleared cell with a number in it to clear all cells around it, if that cell has the right amount of flags around it. \nPress 'n' to generate new game, or press 'c' to change game parameters", "Rules", 1);
		
		//Set game parameters and creates game
		sizeSet();
		
		//Some crazy stuff I have to add to implement a timer
		Runnable timerUpdate = new Runnable() {public void run() {titleUpdate(1);}};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(timerUpdate, 1, 1, TimeUnit.SECONDS);
		
	}
	
	public void sizeSet() {
		try {
			remove(gameboardPanel); //If sizeSet is called by pressing 'c', this ensures that the previous game is destroyed
		} catch (Exception e) {
			
		}
		
		gameboardPanel = new JPanel();
		setVisible(false);
		
		//prompts for custom settings (for looping)
		String[] prompts = {"Please enter # of columns minesweeper game", "Please enter # of rows of minesweeper game", "Please enter amount of mines"};
		
		//gets user input for game parameters settings (basic error checking code)
		while (true) {
			String input = JOptionPane.showInputDialog("Enter '1' for easy, '2' for medium, '3' for hard, and '4' for custom minesweeper:");
			try {
				gameSetting = Integer.parseInt(input);
				if (gameSetting >= 1 && gameSetting <= 4) {
					break;
				}
			} catch (Exception e) {
				if (input.isEmpty()) {
					System.exit(0);
				}
			}
		}
		
		//This sets the appropriate game settings to match the difficulty inputed by the user
		switch (gameSetting) {
		case 1:
			gameParameters[0] = 9;
			gameParameters[1] = 9;
			gameParameters[2] = 12;
			break;
		case 2:
			gameParameters[0] = 16;
			gameParameters[1] = 16;
			gameParameters[2] = 50;
			break;
		case 3:
			gameParameters[0] = 32;
			gameParameters[1] = 16;
			gameParameters[2] = 100;
			break;
		case 4:
			//case 4 is custom minesweeper, so we have a loop here to allow the user to input parameters (with error checking, ofc)
			for (int i = 0; i < gameParameters.length; i++) {
				while (true) {
					String input = JOptionPane.showInputDialog(prompts[i]);
					try {
						gameParameters[i] = Integer.parseInt(input);
						if ((gameParameters[i] > 0 && gameParameters[i] <= 40 && (i == 0 || i == 1)) || (i == 2 && gameParameters[i] > 0 && gameParameters[i] < gameParameters[0]*gameParameters[1])) {
							break;
						}
					} catch (Exception e) {
						if (input.isEmpty()) {
							System.exit(0);
						}
					}
				}
			}
		}
		
		if (debugMode)
			System.out.println(gameParameters[0] + " x " + gameParameters[1]);
		
		//basic init
		setSize(gameParameters[0]*50, gameParameters[1]*50);
		buttons = new JButton[gameParameters[0]*gameParameters[1]];
		boardState = new int[gameParameters[0]*gameParameters[1]];
		
		//the cells on display are all unopened, so they are filled with -1. Opened empty cells are represented with 0, each number is represented by themselves, mines are 9, flags are 10, and x flags are 11. This is also true for boardState
		currentBoardState = new int[gameParameters[0]*gameParameters[1]];
		Arrays.fill(currentBoardState, -1);
		
		//initalizes the mines array to the size of the amount of mines specified earlier
		mines = new int[gameParameters[2]];
		
		//we can't use curly braces to initialize an array after declaration, so this is a workaround instead. What exactly is checkers?
		int[] temp = {-gameParameters[0]-1, -gameParameters[0], -gameParameters[0]+1, -1, 1, gameParameters[0]-1, gameParameters[0], gameParameters[0]+1};
		checkers = temp; //In minesweeper, there are many times where you check all cells around a cell you are currently on. This array is simple all modifiers to the original cell to access the adjacent cells, and it makes it really easy to loop things
		
		//resetting all the relevant variables
		time = 0;
		minesLeft = gameParameters[2];
		dead = false;
		solved = false;
		cellsOpened = 0;
		generated = false; 
		
		//basic init
		gameboardPanel.setLayout(new GridLayout(gameParameters[1],gameParameters[0],1,1));
		gameboardPanel.setBackground(Color.black);
		add(gameboardPanel);
		setFocusable(true);
		
		//sets the jpanel up with buttons
		initPanel();
		
		setVisible(true);     //Turn on JFrame
		
	}
	
	public boolean wrapCheck(int check, int iter) { //because of the fact that the minesweeper board is backed by a 1d array, checking a cell +- 1 can lead to checking the cell on the opposite side of the board due to looparound. this bit of code prevents that
		return check%gameParameters[0] == 0 && (iter == 0 || iter == 3 || iter == 5) || (check%gameParameters[0] == gameParameters[0] - 1 && (iter == 2 || iter == 4 || iter == 7));
		//Everytime there is a possible wrap around bug, it is because we are looping through cells and the cell checkers. iter represents the current index of checkers, and check represents the cell being checked. should this return true, than all instances where this is called will have something akin to iter++
	}
	
	public void generateGame(int exclude) { //this method is called on the users first click of the game - it ensures that they don't die on their first click and have a reasonable amount of space to work with from the beginning rather than being stuck with a 1x1 area
		if (debugMode)
			System.out.println(mines.length);
		
		//We fill our mines array with random numbers - each number is an index on boardState
		Random rng = new Random();
		for (int i = 0; i < gameParameters[2]; i++) {
			mines[i] = rng.nextInt(gameParameters[0]*gameParameters[1]);
		}
		
		//Sort out the array for neatness
		Arrays.sort(mines);
		if (debugMode)
			System.out.println(Arrays.toString(mines));
		
		//our randomly generated set of mines may be unsafe for multiple reasons - either there are duplicates, or the spot that the user clicks will not be empty
		boolean unsafe = false;
		
		//this loop checks for unsafeness and redistributes mines until it is safe
		for (int i = 1; i < gameParameters[2]; i++) {
			unsafe = false;
			for (int j = 0; j < checkers.length; j++) {
				try {
					if (wrapCheck(mines[i], j)) {
						j++; //I know it's not good form to touch for loop iterators in the loop, but it's a necessary evil here to avoid wrap around
					}
					unsafe = unsafe || mines[i] + checkers[j] == exclude || mines[i] == mines[i-1] || mines[i] == exclude;
					if (unsafe)
						break;
				} catch (Exception e) {
					
				}
			}
			if (unsafe) {
				mines[i] = rng.nextInt(gameParameters[0]*gameParameters[1]); //if a mine makes the board unsafe, then it is rerolled
				Arrays.sort(mines); //and the array is sorted again
				if (debugMode)
					System.out.println(Arrays.toString(mines));
				i = 1; //and we do the whole shebang from the beginning. and yes, i am setting i = 1. deal with it
			}
		}
		
		//here we will the board with the mines in their appropriate spots and update surroundging cells with the appropriate number
		for (int i = 0; i < gameParameters[2]; i++) {
			boardState[mines[i]] = 9; //sets the mine spots to mines on the actual board - If an error traces to here than thats just because Random nextInt is unclear about its definition of exclusive bound
			for (int j = 0; j < checkers.length; j++) {
				try {
					if (wrapCheck(mines[i], j)) {
						j++; //wrapcheck again
					}
					if (boardState[mines[i]+checkers[j]] != 9) //updates cells adjacent to mines to reflect their existence, presuming that those cells aren't mines themselves
						boardState[mines[i]+checkers[j]]++; 
				} catch (Exception e) {
					if (debugMode)
						System.out.println("Caught: tried to access cell out of bounds");
				}
			}
		}
		
		generated = true; //the game is now generated, so interaction is allowed
	}
	
	//Same thing from SliderV2 more or less - this runs every second to update the timer, but also contains useful information such as hotkeys, as well as the current state of the game (e.g. whether the game is dead/solved, or whether or not the user is in debug mode)
	public void titleUpdate(int timeUpdate) {
		if (debugMode) {
			debugString = "DEBUG - ";
		} else {
			debugString = "";
		}
		if (dead) {
			setTitle(debugString + "DEAD - Press 'n' to create new game, or 'c' to change parameters - Mines Left: " + minesLeft + " - Time: " + time + " seconds");
		} else if (solved) {
			setTitle(debugString + "SOLVED - Press 'n' to create new game, or 'c' to change parameters - Mines Left: " + minesLeft + " - Time: " + time + " seconds");
		} else {
			time += timeUpdate;
			setTitle(debugString + "Minesweeper - Mines Left: " + minesLeft + " - Time: " + time + " seconds" + " - Press 'n' to create new game, or 'c' to change parameters");
		}
	}
	
	//initalizies the panel by placing all the buttons onto them and putting the appropriate settings
	public void initPanel() {
		gameboardPanel.removeAll(); //this is for when initPanel is called from pressing 'c'
		
		//these comments are inherited from SliderPractice lol
		for (int i = 0; i < buttons.length; i++)  //From i is 0 to 15
		{	
			buttons[i] = new JButton();  //Constructor sets text on new buttons
			buttons[i].setFocusable(false);
			buttons[i].setFont(new Font("Courier prime", Font.PLAIN, 18));
			buttons[i].setBackground(Color.lightGray);
			buttons[i].addMouseListener(this);   //Set up ActionListener on each button
			gameboardPanel.add(buttons[i]);    //Add buttons to the grid layout of 
											   //gameboardPanel
		}
		
		//This seems pointless but is required for this entire method to work
		buttons[0].setVisible(false);
		buttons[0].setVisible(true);
		
		//start with the title initialized right off the bat
		titleUpdate(0);
	}
	
	public void updatePanel() {
		//check if win
		if (cellsOpened >= gameParameters[0]*gameParameters[1]-gameParameters[2]) { //the win condition is if every non mine cell has been uncovered. that means it is possible to do a no flags run, since the only requirement is to uncover every cell that isnt a mine
			solved = true; //set game to solved and disables interaction
			minesLeft = 0;
			
			//auto flags all remaining mines
			for (int i = 0; i < gameParameters[2]; i++) {
				currentBoardState[mines[i]] = 10;
			} 
		}
		
		//similar to initpanel, though this code is written with the mindset of updating rather than initiazling
		for (int i = 0; i < buttons.length; i++)  //From i is 0 to 15
		{	
			buttons[i].setIcon(null);
			buttons[i].setText("" + currentBoardState[i]);  //Constructor sets text on new buttons
			buttons[i].setBackground(Color.gray);
			if (currentBoardState[i] < 0 || currentBoardState[i] > 9)
				buttons[i].setBackground(Color.lightGray);
			if (currentBoardState[i] >= 9) {
				buttons[i].setIcon(icons[currentBoardState[i]-9]);
				buttons[i].setText(null);
			}
			try {
				buttons[i].setForeground(colours[currentBoardState[i]+1]);   //Text colour
			} catch (Exception e) {
				if (debugMode)
					System.out.println("Caught: no foreground colour found");
			}
		}
		
		//This seems pointless but is required for this entire method to work
		buttons[0].setVisible(false);
		buttons[0].setVisible(true);
		
		
		
		if (solved) //displays win popup if solved
			JOptionPane.showMessageDialog(null, "You Won! \nDifficulty: " + difficulties[gameSetting - 1] + "\nTime: " + time + "s \nPress 'n' to play again, or press 'c' to change game settings", "Great!", 1);
		
		//updates title
		titleUpdate(0);
	}
	
	public void showAll() { //debug method - reveals all cells
		for (int i = 0; i < boardState.length; i++) {
			currentBoardState[i] = boardState[i];
		}
		updatePanel();
	}
	
	public void resetState() { //debug method - resets all cells to unopened state without generating new puzzle
		Arrays.fill(currentBoardState, -1);
		dead = false;
		solved = false;
		cellsOpened = 0;
		time = 0;
		updatePanel();
	}
	
	public void newGame() { //generates new puzzle with same parameters
		Arrays.fill(currentBoardState, -1);
		Arrays.fill(boardState, 0);
		dead = false;
		solved = false;
		cellsOpened = 0;
		time = 0;
		generated = false;
		initPanel(); //normally updatePanel would be used for speed, but it just so happens that the slowness of initPanel produces a nice effect here
	}
	
	public void open(int index) { //now we're getting to the fun parts
		if (debugMode)
			System.out.println("Cell: " + index + " Row: " + index/gameParameters[0] + " Column: " + index%gameParameters[0] + " Contents: " + boardState[index]);
		if (currentBoardState[index] < 0) { //if the cell clicked on is unopened, then we open it
			currentBoardState[index] = boardState[index];
			cellsOpened++;
		}
		if (boardState[index] == 0) { //if what we open is an empty cell, 
			for (int i = 0; i < checkers.length; i++) { //then we open all cells around it too
				if (wrapCheck(index, i)) {
					i++;
				}
				try {
					if (currentBoardState[index+checkers[i]] < 0)
						open(index+checkers[i]);
				} catch (Exception e) {
					if (debugMode)
						System.out.println("Caught: tried to open cell outside of borders");
				}
			}
		}
		if (boardState[index] == 9) { //if the cell we just opened is a mine
			dead = true;
			if (debugMode)
				System.out.println(Arrays.toString(mines));
			for (int i = 0; i < gameParameters[0]*gameParameters[1]; i++) { //reveal all other mines and which flags were wrong
				if (Arrays.binarySearch(mines, i) >= 0) {
					if (currentBoardState[i] != 10) {
						currentBoardState[i] = 9;
					}
				} else if (currentBoardState[i] == 10) {
					currentBoardState[i] = 11;
				}
			}
			updatePanel();
			JOptionPane.showMessageDialog(null, "You lost! \nPress 'n' to play again, or press 'c' to change game parameters", "Too bad!", 1); //display losing screen
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		int click = Arrays.asList(buttons).indexOf(e.getSource()); //store index of click here
		if (!dead && !solved && generated) { //if we're not dead or done, and the game is generated
			if (SwingUtilities.isRightMouseButton(e)) { //if it's a right click
				if (debugMode)
					System.out.println("Right Click");
				if (currentBoardState[click] < 0) { //then flag the cell if its not flagged and is unopened
					currentBoardState[click] = 10;
					updatePanel();
					minesLeft--;
				} else if (currentBoardState[click] == 10) { //else deflag it if it is
					currentBoardState[click] = -1;
					updatePanel();
					minesLeft++;
				}
			} else { //if left click
				if (currentBoardState[click] < 0) { //if empty cell, then open it
					open(click);
					updatePanel();
				} else if (currentBoardState[click] != 10 && currentBoardState[click] > 0) { //if its a # from 1-8 (9 will kill you so it doesn't matter to check, and 11 will only happen on death)
					int flags = 0;
					for (int i = 0; i < checkers.length; i++) { //we check for the amount of flags around the cell
						if (wrapCheck(click, i)) {
							i++;
						}
						try {
							if (currentBoardState[click + checkers[i]] == 10) {
								flags++;
							}
						} catch (Exception f) {
							if (debugMode)
								System.out.println("Caught: tried to check cell outside of game borders");
						}
						
					}
					if (flags == currentBoardState[click]) { //if the amount of flags matches the expected amount, then we open all unopened and unflagged cells around it
						for (int i = 0; i < checkers.length; i++) {
							if (wrapCheck(click, i)) {
								i++;
							}
							try {
								if (currentBoardState[click + checkers[i]] != 10 && currentBoardState[click + checkers[i]] < 0) {
									if (debugMode)
										System.out.println("open");
									open(click + checkers[i]);
								}
							} catch (Exception f) {
								if (debugMode)
									System.out.println("Caught: tried to access cell outside of game borders");
							}
						}
						updatePanel();
					}
				}
			}
		}
		if (!generated && !SwingUtilities.isRightMouseButton(e)) { //if the game is ungenerated and the user clicks any button, then we generate the game such that the button that the user clicked is always an empty cell
			generateGame(click);
			open(click);
			updatePanel();
		}
		titleUpdate(0);
	}
	@Override
	public void mouseClicked(MouseEvent e) {
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		if (debugMode)
			setTitle(debugString + Arrays.asList(buttons).indexOf(e.getSource())); //debug feature
	}
	@Override
	public void mouseExited(MouseEvent e) {
	}
	@Override
	public void mousePressed(MouseEvent e) {
	}
}
