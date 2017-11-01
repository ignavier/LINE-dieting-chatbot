package com.example.bot.spring;

//import java.io.IOException;

public class ProvideInfoState extends State {
    /**
     * Default constructor for ProvideInfoState
     */
	public ProvideInfoState() {
		
	}

    /**
     * Reply a message for input text
     * Inherited from abstract base class
     * @param text A String data type
     * @return A String data type
     */
	public String reply(String text) {
		return "Your text has been well received!"; 
	}
}