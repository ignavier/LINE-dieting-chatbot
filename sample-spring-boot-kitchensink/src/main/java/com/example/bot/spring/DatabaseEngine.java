package com.example.bot.spring;

import java.util.Arrays;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

// sample-spring-boot-kitchensink/resources/static/database.txt.
//
// This file contains a few lines with the format <input>:<output>.
// The program below wish to perform an exact word match the input text
// against the <input> against each line. The bot should replys 
// "XXX says <output>"
// For instance, if the client sends "abc", the bot should reply 
// "kevinw says def" 
// If you registered your ITSC login as kevinw.
////

@Slf4j
public class DatabaseEngine {
	String search(String text) throws Exception {
		String result = null;
		BufferedReader br = null;
		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(
                    this.getClass().getResourceAsStream(FILENAME));
			br = new BufferedReader(isr);
			String sCurrentLine;
			
			while (result == null && (sCurrentLine = br.readLine()) != null) {
				String[] parts = sCurrentLine.split(":");
				if (text.toLowerCase().contains(parts[0].toLowerCase())) {
					result = parts[1];
				}
			}
		} catch (IOException e) {
			log.info("IOException while reading file: {}", e.toString());
		} finally {
			try {
				if (br != null)
					br.close();
				if (isr != null)
					isr.close();
			} catch (IOException ex) {
				log.info("IOException while closing file: {}", ex.toString());
			}
		}
		if (result != null)
			return result;
		throw new Exception("NOT FOUND");
    }

	private final String FILENAME = "/static/database.txt";

}