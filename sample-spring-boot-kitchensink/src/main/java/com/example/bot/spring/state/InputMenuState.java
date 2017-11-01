package com.example.bot.spring;

import com.example.bot.spring.DietbotController.DownloadedContent;
import com.asprise.ocr.Ocr;
import java.io.File;
import java.nio.file.Path;

public class InputMenuState extends State {
    // Constant values
	private final String NO_CHARACTER_MESSAGE = "There is no chracter in the image!";
    /**
     * Default constructor for InputMenuState
     */
	public InputMenuState() {
		
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

    /**
     * Reply a message for input text
     * Overload the function inherited from abstract base class
     * @param jpg A DownloadedContent data type
     * @return A String data type
     */
	public String reply(DownloadedContent jpg) {
		String ocr_string = decodeImage(jpg);
		return ocr_string;
	}

    /**
     * Perform OCR on the image
     * @param jpg A DownloadedContent data type
     * @return A String data type
     */
	public String decodeImage(DownloadedContent jpg) {
		Ocr.setUp();    // One time setup
		Ocr ocr = new Ocr();    // Create a new OCR engine
		ocr.startEngine("eng", Ocr.SPEED_FAST);    // English
		String ocr_string = ocr.recognize(new File[] {new File(jpg.getPathString())},
										Ocr.RECOGNIZE_TYPE_ALL,
										Ocr.OUTPUT_FORMAT_PLAINTEXT);
		ocr.stopEngine();    // Stop the OCR engine

		switch(ocr_string) {
			case "": return NO_CHARACTER_MESSAGE;
			default: return "The characters in the image are: \n \n" + ocr_string;
		}
	}
}