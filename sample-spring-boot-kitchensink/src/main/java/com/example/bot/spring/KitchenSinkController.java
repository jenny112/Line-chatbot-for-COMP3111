/*
 * Copyright 2016 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.example.bot.spring;

import java.util.ArrayList;
import java.io.StringWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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

import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

@Slf4j
@LineMessageHandler
public class KitchenSinkController {
	private static final int MAX_SEARCHED_TOURS = 10;
	private static final int MAX_BOOKING_TOURS = 10;
	private static final String NOT_FOUND = "Sorry, we don't have answer for this";
	private boolean searchingTour = false;
	private Tour[] searchedTours = new Tour[MAX_SEARCHED_TOURS];
	private int noOfSearchedTours = 0;
	
	private boolean bookingTour = false;
	private Tour[] bookingTours = new Tour[MAX_BOOKING_TOURS];
	private int noOfBookingTours = 0;
	
	
	@Autowired
	private LineMessagingClient lineMessagingClient;

	@EventMapping
	public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
		log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		log.info("This is your entry point:");
		log.info("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		TextMessageContent message = event.getMessage();
		handleTextContent(event.getReplyToken(), event, message);
	}

	@EventMapping
	public void handleStickerMessageEvent(MessageEvent<StickerMessageContent> event) {
		handleSticker(event.getReplyToken(), event.getMessage());
	}

	@EventMapping
	public void handleLocationMessageEvent(MessageEvent<LocationMessageContent> event) {
		LocationMessageContent locationMessage = event.getMessage();
		reply(event.getReplyToken(), new LocationMessage(locationMessage.getTitle(), locationMessage.getAddress(),
				locationMessage.getLatitude(), locationMessage.getLongitude()));
	}

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
		reply(((MessageEvent) event).getReplyToken(), new ImageMessage(jpg.getUri(), jpg.getUri()));

	}

	@EventMapping
	public void handleAudioMessageEvent(MessageEvent<AudioMessageContent> event) throws IOException {
		final MessageContentResponse response;
		String replyToken = event.getReplyToken();
		String messageId = event.getMessage().getId();
		try {
			response = lineMessagingClient.getMessageContent(messageId).get();
		} catch (InterruptedException | ExecutionException e) {
			reply(replyToken, new TextMessage("Cannot get image: " + e.getMessage()));
			throw new RuntimeException(e);
		}
		DownloadedContent mp4 = saveContent("mp4", response);
		reply(event.getReplyToken(), new AudioMessage(mp4.getUri(), 100));
	}

	@EventMapping
	public void handleUnfollowEvent(UnfollowEvent event) {
		log.info("unfollowed this bot: {}", event);
	}

	@EventMapping
	public void handleFollowEvent(FollowEvent event) {
		String replyToken = event.getReplyToken();
		this.replyText(replyToken, "Got followed event");
	}

	@EventMapping
	public void handleJoinEvent(JoinEvent event) {
		String replyToken = event.getReplyToken();
		this.replyText(replyToken, "Joined " + event.getSource());
	}

	@EventMapping
	public void handlePostbackEvent(PostbackEvent event) {
		String replyToken = event.getReplyToken();
		this.replyText(replyToken, "Got postback " + event.getPostbackContent().getData());
	}

	@EventMapping
	public void handleBeaconEvent(BeaconEvent event) {
		String replyToken = event.getReplyToken();
		this.replyText(replyToken, "Got beacon message " + event.getBeacon().getHwid());
	}

	@EventMapping
	public void handleOtherEvent(Event event) {
		log.info("Received message(Ignored): {}", event);
	}

	private void reply(@NonNull String replyToken, @NonNull Message message) {
		reply(replyToken, Collections.singletonList(message));
	}

	private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
		try {
			BotApiResponse apiResponse = lineMessagingClient.replyMessage(new ReplyMessage(replyToken, messages)).get();
			log.info("Sent messages: {}", apiResponse);
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private void replyText(@NonNull String replyToken, @NonNull String message) {
		if (replyToken.isEmpty()) {
			throw new IllegalArgumentException("replyToken must not be empty");
		}
		if (message.length() > 1000) {
			message = message.substring(0, 1000 - 2) + "..";
		}
		this.reply(replyToken, new TextMessage(message));
	}


	private void handleSticker(String replyToken, StickerMessageContent content) {
		reply(replyToken, new StickerMessage(content.getPackageId(), content.getStickerId()));
	}

	private void handleTextContent(String replyToken, Event event, TextMessageContent content)
            throws Exception {
		// Get User Input
        String text = content.getText();
        
        //Load Tokenizer model
        InputStream tokenis = this.getClass().getResourceAsStream("/static/en-token.bin");
        TokenizerModel modelToken = new TokenizerModel(tokenis);
        
        //Load POS tagging model
        InputStream taggeris = this.getClass().getResourceAsStream("/static/en-pos-maxent.bin");
        POSModel modelPOS = new POSModel(taggeris);
        POSTaggerME tagger = new POSTaggerME(modelPOS);
        
        //Tokenize the sentence
        Tokenizer tokenizer = new TokenizerME(modelToken);
        String[] tokens = tokenizer.tokenize(text);
        
        //Generate tags[]
        String tags[] = tagger.tag(tokens);
        
        log.info("Got text message from {}: {}", replyToken, text);
        
        //Reply
		String reply = "";
		try {
			//reply = database.search(text);
			
			//Search keywords for greeting
		    reply = searchForKeywords(tokens, tags);
		    
		    //When asking confirm from client to choose tour
		    if (reply.equals("askingConfirmToChooseTour")) {
		    	TextMessage message1 = new TextMessage(bookingTours[noOfBookingTours - 1].toString() + ". We have confirmed tour on 6/11, 15/11 We have tour on 13/11 still accept application. Fee: Weekday 299 / Weekend 399");
		    	//TextMessage message2 = new TextMessage("Do you want to book this one?");
		    	ConfirmTemplate confirmTemplate = new ConfirmTemplate(
		    			"Do you want to book this one?", 
		    			new MessageAction("Yes", ":)"), 
		    			new MessageAction("No", ":(")
		    	);
		    	TemplateMessage message2 = new TemplateMessage("Do you want to book this one?", confirmTemplate);
		    	ArrayList<Message> messages = new ArrayList<Message>();
		    	messages.add(message1);
		    	messages.add(message2);
		    	this.reply(replyToken, messages);
		    }
		    
		    //If client is not greeting, reply don't have answer
		    if (reply.equals(""))
		    	reply = NOT_FOUND;
		} catch (Exception e) {
			//For debug use only
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			this.replyText(replyToken, sw.toString());
		} finally {
			tokenis.close();
			taggeris.close();
		}
		log.info("Returns echo message {}: {}", replyToken, reply);
		this.replyText(replyToken, reply);
	}
	
	private String searchForKeywords(String[] tokens, String[] tags) throws Exception {
		//Check if tokens and tags are not null
		if (tokens == null || tags == null) {
			throw new Exception("Passing null arguments to searchForKeywords()");
		}
		//Define greeting words
		String greetingString = "hi hello yo";
		String questionVerbs = "ask know";
		String questionWords = "what when how where";
		
		//Search for greeting words from client
		boolean greeting = false;
		boolean question = false;
		boolean tour = false;
		String adj = "";
		
		//If client is searching for tours
		if (searchingTour) {
			for (int i = 0; i < tags.length; i++) {
				// Check which tour client wants to book
				if (tags[i].equals("CD")) {
					int tourIdInChatBot = Integer.parseInt(tokens[i]);
					if (tourIdInChatBot > noOfSearchedTours) {
						break;
					}
					Tour t = searchedTours[tourIdInChatBot - 1];
					//Store the tour in bookingTours array
					bookingTours[noOfBookingTours] = t;
					noOfBookingTours++;
					searchingTour = false;
					bookingTour = true;
					return "askingConfirmToChooseTour";
					//return t.toString() + ". We have confirmed tour on 6/11, 15/11 We have tour on 13/11 still accept application. Fee: Weekday 299 / Weekend 399 Do you want to book this one?";
				}
			}
		} // If client is booking tours
		else if (bookingTour) {
			bookingTour = false;
			return "bookingTour";
		}
		
		for (int i = 0; i < tags.length; i++) {
			//for greeting
			if (!greeting) {
				if (tags[i].equals("PRP$") || tags[i].equals("UH")) {
					if (greetingString.contains(tokens[i].toLowerCase())) {
						greeting = true;
					}
				}
			}

			//for questions
			if (!question) {
				if (tags[i].equals("VB")) {
					if (questionVerbs.contains(tokens[i].toLowerCase())) {
						question = true;
						continue;
					}
				} else if (tags[i].equals("WP")) {
					if (questionWords.contains(tokens[i].toLowerCase())) {
						question = true;
						continue;
					}
				}
			}
			if (question) {
				//for display tour
				if (tags[i].equals("NN") || tags[i].equals("NNS")) {
					if (tokens[i].contains("tour")) {
						tour = true;
						//Search backwards for adj describing the tour
						//e.g. "hot spring tour" <-- search for "hot spring"
						for (int j = i - 1; j > -1; j--) {
							if (tags[j].equals("NN") || tags[j].equals("JJ")) {
								adj = tokens[j] + " " + adj;
							} else {
								break;
							}
						}
						break;
					}
				}
			}

		}
		
		//Return Greeting message if client greets first
		if (greeting) {
			return "Hi! How can I help you?";
		} else if (tour) {
			//Search for tour in db
			//To be implemented...
			searchingTour = true;
			return searchForTours(adj);
		}
//		return "Searching for " + adj + "tours...";
		String message = printStringArray(tags);
		message += printStringArray(tokens);
		return message;
	}
	
	//Search and print tours
	private String searchForTours(String tourName) {
		//Remove previous search and booking record
		searchedTours = new Tour[MAX_SEARCHED_TOURS];
		noOfSearchedTours = 0;
		bookingTours = new Tour[MAX_BOOKING_TOURS];
		noOfBookingTours = 0;
		
		//To be implemented with database
		//Hard-coded for now
		Tour t = new Tour(1, "2D002", "Yangshan Hot Spring Tour", "* Unlimited use of hot spring * Famous Yangshan roaster cusine");
		
		// Save searchedTours in array for later use
		searchedTours[noOfSearchedTours] = t;
		noOfSearchedTours++;
		
		String text = "We have 1 tour.\n";
		text = text + "  " + t.idInChatBot + ". " + t.getId() + " " + t.getName();
		text += "\n Please indicate the corresponding tour number to book the tour.";
		return text;
	}
	
	//For debug use only
	private String printStringArray(String[] stringArray) {
		String message = "";
		for (String s: stringArray) {
			message = message + s + " ";
		}
		message += "\n";
		return message;
	}

	static String createUri(String path) {
		return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).build().toUriString();
	}

	private void system(String... args) {
		ProcessBuilder processBuilder = new ProcessBuilder(args);
		try {
			Process start = processBuilder.start();
			int i = start.waitFor();
			log.info("result: {} =>  {}", Arrays.toString(args), i);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InterruptedException e) {
			log.info("Interrupted", e);
			Thread.currentThread().interrupt();
		}
	}

	private static DownloadedContent saveContent(String ext, MessageContentResponse responseBody) {
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

	private static DownloadedContent createTempFile(String ext) {
		String fileName = LocalDateTime.now().toString() + '-' + UUID.randomUUID().toString() + '.' + ext;
		Path tempFile = KitchenSinkApplication.downloadedContentDir.resolve(fileName);
		tempFile.toFile().deleteOnExit();
		return new DownloadedContent(tempFile, createUri("/downloaded/" + tempFile.getFileName()));
	}


	


	public KitchenSinkController() {
		database = new SQLDatabaseEngine();
		itscLOGIN = System.getenv("ITSC_LOGIN");
	}

	private SQLDatabaseEngine database;
	private String itscLOGIN;
	

	//The annontation @Value is from the package lombok.Value
	//Basically what it does is to generate constructor and getter for the class below
	//See https://projectlombok.org/features/Value
	@Value
	public static class DownloadedContent {
		Path path;
		String uri;
	}


	//an inner class that gets the user profile and status message
	class ProfileGetter implements BiConsumer<UserProfileResponse, Throwable> {
		private KitchenSinkController ksc;
		private String replyToken;
		
		public ProfileGetter(KitchenSinkController ksc, String replyToken) {
			this.ksc = ksc;
			this.replyToken = replyToken;
		}
		@Override
    	public void accept(UserProfileResponse profile, Throwable throwable) {
    		if (throwable != null) {
            	ksc.replyText(replyToken, throwable.getMessage());
            	return;
        	}
        	ksc.reply(
                	replyToken,
                	Arrays.asList(new TextMessage(
                		"Display name: " + profile.getDisplayName()),
                              	new TextMessage("Status message: "
                            		  + profile.getStatusMessage()))
        	);
    	}
    }
	
	

}