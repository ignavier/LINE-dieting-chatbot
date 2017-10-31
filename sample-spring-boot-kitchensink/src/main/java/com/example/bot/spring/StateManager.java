/**
* StateManager.java - A class for managing different states and transitions
*/

package com.example.bot.spring;

import com.example.bot.spring.DietbotController.DownloadedContent;

public class StateManager {
	// Constant values
	private final int STANDBY_STATE = 0;
	private final int INPUT_MENU_STATE = 3;
	private final int RECOMMEND_STATE = 4;
	// Must first go through InputMenuState before going to RecommendationState,
	// so 4 is not included
	private final int[] FROM_STANBY_STATE = {1, 2, 3, 5};

	// Value to keep track current state
    private int currentState = 0;
    private State[] states = {
    		new StanbyState(),
    		new CollectUserInfoState(),
    		new ProvideInfoState(),
    		new InputMenuState(),
    		new RecommendationState(),
    		new PostEatingState()
    	};

    /**
     * Default constructor for StateManager
     */
	public StateManager() {
	}

    /**
     * Get output message after inputting text
     * @param text A String data type
     * @return A String data type
     */
	public String chat(String text) throws Exception {
		String replyText = null;
		try{
			// Get the next state after current message
			currentState = nextState(text);    // Check trigger
			replyText = states[currentState].reply(text);
		} catch (Exception e) {    // Modify to custom exception TextNotRecognized later
			// Text is not recognized, does not modify current state
			replyText = "Your text is not recognized by us!";
		}
		if(replyText != null) {
			return replyText;
		}
		throw new Exception("NOT FOUND");
	}

    /**
     * Get output message after inputting image
     * @param jpg A DownloadedContent data type
     * @return A String data type
     */
	public String chat(DownloadedContent jpg) throws Exception {
		String replyText = null;
		try{
			// Pass the image into InputMenuState to check if the image is recognized as menu
			replyText = ((InputMenuState) states[INPUT_MENU_STATE]).reply(jpg);
			// If above line does not return exception, then the image is recognized as menu
			currentState = INPUT_MENU_STATE;
		} catch (Exception e) {    // Modify to custom exception ImageNotRecognized later
			// Image is not recognized as menu, does not modify current state
			replyText = "Your image is not recognized by us!";
		}
		if(replyText != null) {
			return replyText;
		}
		throw new Exception("NOT FOUND");
	}

    /**
     * Get the next state after inputting text
     * @param text A String data type
     * @return A int data type
     */
	public int nextState(String text) {
		// Abit of hardcoding
		// To do: add all transitions in 2D arrays in constructor
		if(currentState != STANDBY_STATE) {
			// Current state is not stanby state
			if(states[STANDBY_STATE].checkTrigger(text)) {
				// Transition from any state to stanby state
				return STANDBY_STATE;
			}
			else if(currentState == INPUT_MENU_STATE && states[RECOMMEND_STATE].checkTrigger(text)) {
				// Transition from input menu to recommendation
				return RECOMMEND_STATE;
			}
		}
		else {    // Current state is stanby state
			for(int state: FROM_STANBY_STATE) {
				if(states[state].checkTrigger(text)) {
					// Transition from stanby state to other state
					return state;
				}
			}
		}
		return currentState;
	}
}