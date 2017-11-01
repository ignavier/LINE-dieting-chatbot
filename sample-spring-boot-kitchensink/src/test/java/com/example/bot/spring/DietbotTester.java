package com.example.bot.spring;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.io.ByteStreams;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.MessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.LineBotMessages;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.example.bot.spring.DatabaseEngine;

import com.rivescript.Config;
import com.rivescript.RiveScript;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
//@SpringBootTest(classes = { DietbotTester.class, DatabaseEngine.class })
@SpringBootTest(classes = { DietbotTester.class, SQLDatabaseEngine.class })
public class DietbotTester {
	@Autowired
	private SQLDatabaseEngine databaseEngine;

	private RiveScript bot;
	
	@Test
	public void writeUserInfoNonExisting() throws Exception {
		boolean thrown = false;
		String[] allergies = {"peanuts", "shrimp"};
		try {
			this.databaseEngine.writeUserInfo("testUser", 20, "male", 1.75, 60, allergies);
		} catch (Exception e) {
			thrown = true;
		} finally {
			this.databaseEngine.deleteUserInfo("testUser");
		}
		assertThat(!thrown);
	}
	
	@Test
	public void writeUserInfoExisting() throws Exception {
		boolean thrown = false;
		String[] allergies = {"water"};
		try {
			this.databaseEngine.writeUserInfo("testUserExisting", 21, "female", 1.64, 55, allergies);
		} catch (Exception e) {
			thrown = true;
		}
		assertThat(!thrown);
	}

	@Test
	public void testNewRivescript() throws Exception {
		bot = new RiveScript();
		File resourcesDirectory = new File("src/test/resources/rivescript");
		// assertThat(resourcesDirectory.getAbsolutePath()).isEqualTo("abc");
		bot.loadDirectory(resourcesDirectory.getAbsolutePath());

		// Sort the replies after loading them!
		bot.sortReplies();

		// Get a reply.
		String reply = bot.reply("user2", "can you tell me my id");
		assertThat(reply).isEqualTo("user2");
	}

	// to test how to use RiveScript with different users and get user variables
	// that have been set before or not.
	@Test
	public void testRivescriptToGetVariableFromDifferentUsers() throws Exception {
		bot = new RiveScript();
		File resourcesDirectory = new File("src/test/resources/rivescript");
		bot.loadDirectory(resourcesDirectory.getAbsolutePath());

		bot.sortReplies();

		String reply1 = bot.reply("user1", "can you tell me my id");
		assertThat(reply1).isEqualTo("user1");

		String reply2 = bot.reply("user2", "can you tell me my id");
		assertThat(reply2).isEqualTo("user2");

		// try to set user variable "name" for two different users
		String setName1 = bot.reply("user1", "my name is Gordon.");
		assertThat(setName1).contains("Gordon");
		String setName2 = bot.reply("user2", "my name is Tom.");
		assertThat(setName2).contains("Tom");

		// try to get them back and check equality
		String user1Name = bot.getUservar("user1", "name");
		assertThat(user1Name).isEqualTo("Gordon");
		String user2Name = bot.getUservar("user2", "name");
		assertThat(user2Name).isEqualTo("Tom");

		// try to get unset user variable
		String unsetVar = bot.getUservar("user1", "age");
		assertThat(unsetVar).isEqualTo(null);


		Map<String, String> varSetToValue = new HashMap<String, String>();
		varSetToValue.put("age", "100");
		varSetToValue.put("weight", "200");
		bot.setUservars("user1", varSetToValue);
		String age = bot.getUservar("user1", "age");
		assertThat(age).isEqualTo("100");

		String weight = bot.getUservar("user1", "weight");
		assertThat(weight).isEqualTo("200");
	}
}