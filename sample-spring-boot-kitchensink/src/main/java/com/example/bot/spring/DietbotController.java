package com.example.bot.spring;

import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import com.linecorp.bot.model.profile.UserProfileResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.google.common.io.ByteStreams;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.BeaconEvent;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.JoinEvent;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.UnfollowEvent;
import com.linecorp.bot.model.event.message.AudioMessageContent;
import com.linecorp.bot.model.event.message.ImageMessageContent;
import com.linecorp.bot.model.event.message.LocationMessageContent;
import com.linecorp.bot.model.event.message.StickerMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.message.AudioMessage;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.ImagemapMessage;
import com.linecorp.bot.model.message.LocationMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.imagemap.ImagemapArea;
import com.linecorp.bot.model.message.imagemap.ImagemapBaseSize;
import com.linecorp.bot.model.message.imagemap.MessageImagemapAction;
import com.linecorp.bot.model.message.imagemap.URIImagemapAction;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.message.template.ConfirmTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.event.source.UserSource;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LineMessageHandler
public class DietbotController {
	
	@Autowired
	private LineMessagingClient lineMessagingClient;
	
	private StateManager stateManager;
    // To do: delete 'controller: '
	private final String defaultString = "I don't understand"; 
    private final String appreciateUsingCoupon = "Thanks, this is your coupon!";
	private RecommendFriendState recommendFriendState = new RecommendFriendState();
	
	/**
     * Default constructor of DiebotController
     */
	protected DietbotController() {
		stateManager = new StateManager("sample-spring-boot-kitchensink/src/main/resources/rivescript");
	}

	/**
	 * Handle text message event from user
	 * @param event TextMessageContent data type
     */
	@EventMapping
	public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
		TextMessageContent message = event.getMessage();
		handleTextContent(event.getReplyToken(), event, message);
	}

	/**
	 * Handle image message event from user
	 * @param event ImageMessageContent data type
     */
	@EventMapping
	public void handleImageMessageEvent(MessageEvent<ImageMessageContent> event) throws IOException {
		final MessageContentResponse response;
		String replyToken = event.getReplyToken();
		String messageId = event.getMessage().getId();
		
		try {
			response = lineMessagingClient.getMessageContent(messageId).get();
		} catch (InterruptedException | ExecutionException e) {
			reply(replyToken, new TextMessage("Cannot get image: " + e.getMessage()));
			throw new RuntimeException(e);
		}
		
		DownloadedContent jpg = saveContent("jpg", response);
		handleImageContent(event.getReplyToken(), event, jpg);

	}
    
    /**
	 * This function is triggered when a new user friends/unblock the chatbot
	 * @param event FollowEvent data type
     */
    @EventMapping
    public static void handleFollowEvent(FollowEvent event) {
        // String replyToken = event.getReplyToken();
        String userId = event.getSource().getUserId();
		SQLDatabaseEngine sql = new SQLDatabaseEngine();

		if(!sql.searchUser(userId, "userinfo")
			&& !sql.searchUser(userId, "campaign_user")){
			sql.addCampaignUser(userId);
		}
    }

     /**
	 * This function is used to generate a PostBackEvent
	 * @param event FollowEvent data type
     */
    @EventMapping
    public void handlePostbackEvent(PostbackEvent event) {
        String replyToken = event.getReplyToken();
        String userId = event.getSource().getUserId();
        String data = event.getPostbackContent().getData();
        String date = event.getPostbackContent().getParams().toString();
        List<Message> replyList = null;
        date = date.replace("{date=", "").replace("}", "");

        if (date.length() > 0) {
        	String[] temp = date.split("-");
        	LocalDate inputDate = LocalDate.of(Integer.parseInt(temp[0]), Integer.parseInt(temp[1]), Integer.parseInt(temp[2]));
        	LocalDate today = LocalDate.now().plusDays(1);
        	long daysBetween = ChronoUnit.DAYS.between(inputDate, today);
        	data = data + " " + Long.toString(daysBetween);
        }

        try {

			replyList = stateManager.chat(userId, data, true);
	        this.reply(replyToken, replyList);

    	} catch (Exception e) {
    		this.replyText(replyToken, defaultString);
    		return;
    	}
        // this.replyText(replyToken, "Got postback data " + event.getPostbackContent().getData()
        // 	+ ", param " + event.getPostbackContent().getParams().toString());
    }

	/**
	 * Push text message to user
	 * @param replyToken String data type representing the reply token
	 * @param message String representing message to be replied to user
     */	
	private void replyText(@NonNull String replyToken, @NonNull String message) {
		if (replyToken.isEmpty()) {
			throw new IllegalArgumentException("replyToken must not be empty");
		}
		if (message.length() > 1000) {
			message = message.substring(0, 1000 - 2) + "..";
		}
		this.reply(replyToken, new TextMessage(message));
	}

	/**
	 * Push image to user
	 * @param replyToken String data type representing the reply token
	 * @param message String representing url of image to be replied to user
     */	
	private void replyImage(@NonNull String replyToken, @NonNull String url) {
		if (replyToken.isEmpty()) {
			throw new IllegalArgumentException("replyToken must not be empty");
		}
		this.reply(replyToken, new ImageMessage(url, url));
	}
	
	/**
	 * Push message to user
	 * @param replyToken String data type representing the reply token
	 * @param message String representing message to be replied to user
     */	
	private void reply(@NonNull String replyToken, @NonNull Message message) {
		reply(replyToken, Collections.singletonList(message));
	}

	/**
	 * Push message to user
	 * @param replyToken String data type representing the reply token
	 * @param message List of Message objects to be replied to user
     */	
	private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
		try {
			BotApiResponse apiResponse = lineMessagingClient.replyMessage(new ReplyMessage(replyToken, messages)).get();
			log.info("Sent messages: {}", apiResponse);
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Push message to user
	 * @param to String data type
	 * @param message List of Message objects to be replied to user
     */	
	private void pushText(@NonNull String to, @NonNull String message) {
		if (to.isEmpty()) {
			throw new IllegalArgumentException("user id must not be empty");
		}
		if (message.length() > 1000) {
			message = message.substring(0, 1000 - 2) + "..";
		}
		this.push(to, new TextMessage(message));
	}

	/**
     * Push message to user
	 * @param to String data type
	 * @param url String representing url of image to be replied to user
     */	
	private void pushImage(@NonNull String to, @NonNull String url) {
		if (to.isEmpty()) {
			throw new IllegalArgumentException("user id must not be empty");
		}
		this.push(to, new ImageMessage(url, url));
	}

	/**
	 * Push message to user
	 * @param to String data type
	 * @param message Message data type
     */	
	private void push(@NonNull String to, @NonNull Message message) {
		push(to, Collections.singletonList(message));
	}

	/**
	 * Push message to user
	 * @param to String data type
	 * @param message List of Message objects to be replied to user
     */
	private void push(@NonNull String to, @NonNull List<Message> messages) {
		try {
			BotApiResponse apiResponse = lineMessagingClient.pushMessage(new PushMessage(to, messages)).get();
			log.info("Sent messages: {}", apiResponse);
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * This function handles text content from user, and uses stateManager object to generate appropriate replies to user
	 * @param replyToken String representing reply token
	 * @param event Event data type
	 * @param content TextMessageContent object from user
     */
	private void handleTextContent(String replyToken, Event event, TextMessageContent content) {
        String text = content.getText();
        log.info("Got text message from {}: {}", replyToken, text);
        
        Vector<String> reply = null;
        List<Message> replyList = null;
        String userId = event.getSource().getUserId();
        SQLDatabaseEngine sql = new SQLDatabaseEngine();
            System.out.println("controller 1");

        try {
			UserProfileResponse profile = lineMessagingClient.getProfile(userId).get();

			// text: "code 123456"
			// Exception couponIsValid
			if (recommendFriendState.matchTrigger(text).equals("CODE") && sql.searchUser(userId, "userinfo")){
				String code = text.split(":")[1];
				reply = recommendFriendState.actionForCodeCommand(userId, code);
				if(reply.size() == 2) {
					String url = sql.getCouponUrl();
					String requestUser = reply.get(0);
            		// String temp = reply.get(1);	
            		// Reply to claimUser
                    this.pushText(userId, appreciateUsingCoupon);
            		this.pushImage(userId, url);
            		// Push image to requestUser
                    this.pushText(requestUser, appreciateUsingCoupon);
            		this.pushImage(requestUser, url);
					return;
				}
            System.out.println("controller 2");

				// create a List of Message object for this condition
				replyList = new ArrayList<Message>(0);
		    	for (String replyMessage:reply) {
		         	log.info("Returns echo message {}: {}", replyToken, replyMessage);
		         	replyList.add(new TextMessage(replyMessage));
		        }
    	
            System.out.println("controller 3");
			}
			else {
            System.out.println("controller 4");
				// a general List of message
				replyList = stateManager.chat(userId, text, true);

            System.out.println("controller 5");
			}
    	} catch (Exception e) {
    		this.replyText(replyToken, defaultString);
    		return;
    	}
    	
        this.reply(replyToken, replyList);
     
    }

    /**
	 * This function handles image content from user, and uses stateManager object to generate appropriate replies to user
	 * @param replyToken String representing reply token
	 * @param event Event data type
	 * @param jpg DownloadedContent object representing image from user
     */
	private void handleImageContent(String replyToken, Event event, DownloadedContent jpg) {
		Vector<String> reply = null;
		List<Message> replyList = new ArrayList<Message>(0);
	    	try {
	    		reply = stateManager.chat(event.getSource().getUserId(), jpg, true);
	    	} catch (Exception e) {
	    		this.replyText(replyToken,defaultString);
	    		return;
	    	}
	        
	    	for (String replyMessage:reply) {
	         log.info("Returns echo message {}: {}", replyToken, replyMessage);
	         replyList.add(new TextMessage(replyMessage));
	    	}
	    this.reply(replyToken,replyList);
    }
	
	/**
	 * Helper function 
	 * @param ext String representing extension 
	 * @param responseBody MessageContentResponse data type
     */
	public static DownloadedContent saveContent(String ext, MessageContentResponse responseBody) {
		log.info("Got content-type: {}", responseBody);

		DownloadedContent tempFile = createTempFile(ext);
		try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
			ByteStreams.copy(responseBody.getStream(), outputStream);
			log.info("Saved {}: {}", ext, tempFile);
			return tempFile;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	/**
	 * Helper function 
	 * @param ext String representing extension
     */
	public static DownloadedContent createTempFile(String ext) {
		String fileName = LocalDateTime.now().toString() + '-' + UUID.randomUUID().toString() + '.' + ext;
		Path tempFile = DietbotApplication.downloadedContentDir.resolve(fileName);
		tempFile.toFile().deleteOnExit();
		return new DownloadedContent(tempFile, createUri("/downloaded/" + tempFile.getFileName()));
	}
	
	public static String createUri(String path) {
		return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).build().toUriString();
	}

	//The annontation @Value is from the package lombok.Value
	//Basically what it does is to generate constructor and getter for the class below
	//See https://projectlombok.org/features/Value
	@Value
	public static class DownloadedContent {
		Path path;
		String uri;

		public String getPathString() {
			return path.toString();
		}
		public String getUrl() {
			return uri;
		}
	}
}