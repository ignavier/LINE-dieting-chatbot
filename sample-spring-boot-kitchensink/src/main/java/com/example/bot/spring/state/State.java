/**
* StateManager.java - A abstract class for implementing different states
* To do: Add implementation for singleton
*/

package com.example.bot.spring;

//import java.io.IOException;

abstract class State {
    /**
     * Reply a message for input text
     * @param text A String data type
     * @return A String data type
     */
	public abstract String reply(String text);
}