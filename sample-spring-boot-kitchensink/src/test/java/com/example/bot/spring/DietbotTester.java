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


@RunWith(SpringRunner.class)
//@SpringBootTest(classes = { DietbotTester.class, DatabaseEngine.class })
@SpringBootTest(classes = { DietbotTester.class, SQLDatabaseEngine.class })
public class DietbotTester {
	@Autowired
	private SQLDatabaseEngine databaseEngine;
	
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
}
