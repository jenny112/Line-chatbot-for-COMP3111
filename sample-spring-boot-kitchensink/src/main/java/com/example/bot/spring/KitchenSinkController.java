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

import java.io.StringWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
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

import java.lang.NumberFormatException;
import java.text.DecimalFormat;

@Slf4j
@LineMessageHandler
public class KitchenSinkController {

/*	private static final int MAX_SEARCHED_TOURS = 15;
	private static final int MAX_BOOKING_TOURS = 15;
	private static final String NOT_FOUND = "Sorry, we don't have answer for this";
	private static final String CONFIRMED_BOOKING_PAYMENT = "Thank you. Please pay the tour fee by ATM to 123-345-432-211 of ABC Bank or by cash in our store. When you complete the ATM payment, please send the bank in slip to us. Our staff will validate it.";
	private static final String NO_TOUR_RETURNED_FROM_DB = "Sorry, there is no matching tours.";
*/	

	private static final String CONFIRMED_BOOKING_PAYMENT = "Please pay the tour fee by ATM to 123-345-432-211 of ABC Bank or by cash in our store. When you complete the ATM payment, please send the bank in slip to us. Our staff will validate it.";

	//FAQ answers
	private static final String HOW_TO_APPLY = "Customers shall approach the company by phone or visit our store (in Clearwater bay) with the choosen tour code and departure date. If it is not full, customers will be advised by the staff to pay the tour fee. Tour fee is non refundable. Customer can pay their fee by ATM to 123-345-432-211 of ABC Bank or by cash in our store. Customer shall send their pay-in slip to us by email or LINE.";
	private static final String GATHERING_POINT = "We gather at the gathering spot \"Exit A, Futian port, Shenzhen\" at 8:00AM on the departure date. We dismiss at the same spot after the tour. \n(See the below picture.)";
	private static final String CANCELLED_TOUR = "In case a tour has not enough people or bad weather condition and the tour is forced to cancel, customers will be informed 3 days in advanced. Either change to another tour or refund is avaliable for customers to select. However, due to other reasons such as customers' personal problem no refund can be made.";
	private static final String ADDITIONAL_CHARGE = "Each customer need to pay an additional service charge at the rate $60/day/person, on top of the tour fee. It is collected by the tour guide at the end of the tour.";
	private static final String TRANSPORTATION = "A tour bus";
	private static final String TOUR_GUIDE_CONTACT = "Each tour guide has a LINE account and he will add the customers as his friends before the departure date. You can contact him/her accordingly.";
	private static final String INSURANCE = "Insurance is covered. Each customers are protected by the Excellent Blue-Diamond Scheme by AAA insurance company.";
	private static final String HOTEL_BED = "All rooms are twin beds. Every customer needs to share a room with another customer. If a customer prefer to own a room by himself/herself, additional charge of 30% will be applied.";
	private static final String VISA = "Please refer the Visa issue to the immigration department of China. The tour are assembled and dismissed in mainland and no cross-border is needed. However, you will need a travelling document when you check in the hotel.";
	private static final String SWIMMING_SUIT = "You need swimming suit in a water theme park or a hot spring resort. Otherwise you may not use the facility.";
	private static final String VEGETARIAN = "Sorry, we don't serve vegetarian.";
	private static final String CHILDREN_TOUR_FEE = "Age below 3 (including 3) is free. Age between 4 to 11 (including 4 and 11) has a discount of 20% off. Otherwise full fee applies. The same service charge is applied to all age customers.";
	private static final String LATE = "You shall contact the tour guide if you know you will be late and see if the tour guide can wait a little bit longer. No refund or make up shall be made if a customer is late for the tour.";
	
	private static final String NOT_FOUND = "Sorry I don't understand what you are saying.";
	
	private static ArrayList<Customer> customerList = new ArrayList<Customer>();
	private Customer customer;
	private int status = -100;
	private InProgressBooking booking;
	private ArrayList<Tour> tourArrTemp;
	
	//arraylist of confirmed booking

/*	private boolean searchingTour = false;
	private Tour[] searchedTours = new Tour[MAX_SEARCHED_TOURS];
	private int noOfSearchedTours = 0;
	
	private boolean bookingTour = false;
	private Tour[] bookingTours = new Tour[MAX_BOOKING_TOURS];
	private int noOfBookingTours = 0;
	
	private boolean confirmingTour = false;
	private InProgressBooking inProgressBooking = null;

	private static String userID;
	
	boolean date = false;
	
	private ConfirmedBooking confirmedBooking = null;
	
	private Customer customer = new Customer();
	
	private boolean firstTimeEnterBookingTour = false;
*/	
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

	private void handleTextContent(String replyToken, Event event, TextMessageContent content) throws Exception {
		//Get user input
		String text = content.getText();

		//Get and set the Line ID of the user
		String lineID = event.getSource().getUserId();
		
		if (customerList.size() != 0) {
			for(int i = 0; i < customerList.size(); i++) {
				if (lineID.equals(customerList.get(i).getLineID())) {
					customer = customerList.get(i);
					status = customer.getStatus();
					booking = customer.getBooking();
					tourArrTemp = customer.getTourArr();
					break;
				}
				else status = -100;
			}
		}

		if (status == -100) {
			customer = new Customer();
			status = customer.getStatus();
			booking = customer.getBooking();
			tourArrTemp = customer.getTourArr();
			customer.setLineID(lineID);
			customerList.add(customer);
		}
		
		//Get customer's data if the user exists in the database
		if (customer.getCustomerID() == null) {
			database.checkExistingCustomer(lineID, customer);
		}

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

		//convert the input message to lower case
		for(int i = 0; i < tokens.length; i++) {
			tokens[i] = tokens[i].toLowerCase();
			log.info("show tag: {}", tags[i]);/////////////////////////////////////////////////////////////////////////////////////////////////debug used
		}

		//Identify keyword
		if (status < 1) {
			keywordClassification(text.toLowerCase(), tokens, tags);
		}

		switch (status) {
			case 0: this.replyText(replyToken, "Welcome! How can I help you?"); customer.setStatus(-2); break;	//Greeting
			case 1: this.reply(replyToken, searchForFAQ(text.toLowerCase(), tokens, tags)); break; //Search FAQ
			case 2: this.reply(replyToken, searchTour(text, tokens, tags)); break; //Search tour
			case 3: this.reply(replyToken, bookingProcess(text.toLowerCase())); break; //Book tour
			case 4: this.reply(replyToken, searchPreviousRecord()); break; //Search previous record
			case 5: this.reply(replyToken, createCustomer(text)); break; //Create customer
			case -1: this.replyText(replyToken, "Thank you for using our service. Bye!"); customer.setStatus(-2); break; //Exit
			case -2: this.replyText(replyToken, NOT_FOUND); break; //Error message
			default: break;
		}
	}
	/*************************************************************************************************************************************************************/
	/****************************************************************keywordClassification************************************************************************/
	/*************************************************************************************************************************************************************/
	private void keywordClassification(String text, String[] tokens, String[] tags){

		String[] greeting = {"hi", "hello", "hey", "yo", "is anyone there"};
		String[] exit = {"bye", "byebye", "goodbye", "thanks", "see you",  "thank you"};
		String[] questionWord = {"what", "when", "where", "which", "how", "do", "does", "did", "can", "could", "will", "would", "is", "are", "was", "were"};
		String[] faqSpecialCase = {"visa", "pass", "passport"};
		String[] questionVerb = {"ask", "know", "join", "book", "search", "find", "choose"}; //go?
		String[] noRespond = {"yes", "ok"};
		String[] tempVB = {"help"};

		booking.clearAllData();
		customer.setBooking(booking);
		
		//remove elements of the temporary TourArr
		for(int i = 0; i < tourArrTemp.size(); i++) {
			tourArrTemp.remove(i);	
		}
		customer.setTourArr(tourArrTemp);

		//check whether it is a greeting
		for(int i = 0; i < greeting.length - 1; i++) {
			if (greeting[i].equals(tokens[0]) || text.contains(greeting[4])) {
				status = 0;
				customer.setStatus(status); 
				return;
			}
		}

		//check whether the user wants to end the conversation
		for(int i = 0; i < exit.length; i++) {
			if (tokens.length > 1) {
				for(int j = 0; j < tokens.length - 1; j++) {
					if ((tokens[j]+" "+tokens[j+1]).contains(exit[i])) {
						status = -1;
						customer.setStatus(status);
						return;
					}
				}
			}
			else if (exit[i].equals(tokens[0]) || tokens[0].equals("no")) {
				status = -1;
				customer.setStatus(status);
				return;
			}
		}

		//no response needed
		for(int i = 0; i < noRespond.length; i++) {
			if(noRespond[i].equals(tokens[0])) {
				status = -3;
				customer.setStatus(status);
				return;
			}
		}

		//search booking record
		for(int i = 0; i < tokens.length; i++) {
			if ((tokens[i].equals("booking") || tokens[i].equals("previous")) && (tokens[i+1].contains("record") || (tokens[i+1].contains("histor")))) {
				status = 4;
				customer.setStatus(status);
				return;
			}
		}

		//check whether the user wants to search tour / book tour
		for(int i = 0; i < tokens.length; i++) {
			for(int j = 0; j < questionVerb.length; j++) {
				if (text.contains("tour") && tokens[i].equals(questionVerb[j])) {
					status = 2;
					customer.setStatus(status);
					return;
				}
			}
		}

		//check whether the user wants to search FAQ	
		for(int i = 0; i < questionWord.length; i++) {
			if (tokens[0].equals(questionWord[i])) {
				status = 1;
				customer.setStatus(status);
				return;
			}
		}
		for(int i = 0; i < faqSpecialCase.length; i++) {
			if (tokens[0].equals(faqSpecialCase[i])) {
				status = 1;
				customer.setStatus(status);
				return;
			}
		}
		


	}

	/*************************************************************************************************************************************************************/
	/****************************************************************Search FAQ***********************************************************************************/
	/*************************************************************************************************************************************************************/
	private ArrayList<Message> searchForFAQ(String text, String[] tokens, String[] tags) throws Exception {
		log.info("Entered FAQ");
		ArrayList<Message> messages = new ArrayList<Message>();
		String[] faq9 = {"visa", "pass", "passport"};
		// FAQ 9: VISA
		for (int i = 0; i < tokens.length; i++) {
			for (String s: faq9) {
				if (tokens[i].equals(s)) {
					messages.add(new TextMessage(VISA));
					return messages;
				}
			}
		}

		// Check for question words and store them
		String[] questionWord = {"what", "where", "which", "how", "do", "does", "can", "could", "will", "would", "is", "are"};
		boolean[] containQW = {false, false, false, false, false, false, false, false, false, false, false, false};
		boolean question = false;
		for (int i = 0; i < questionWord.length; i++) {
			if (text.contains(questionWord[i])) {
				if (!question)
					question = true;
				containQW[i] = true;
			}
		}
		
		
		// FAQ 1: How to apply?
		String faq1 = "apply";
		if (containQW[0] || containQW[3]) {
			if (text.contains(faq1)) {
				messages.add(new TextMessage(HOW_TO_APPLY));
				return messages;
			}
		}
		
		// FAQ 2: Gathering spot
		String[] faq2 = {"gather", "meet", "assemble", "dismiss", "gathering", "meeting", "dismissing"};
		if (containQW[1] || containQW[10] || containQW[11]) {
			for (int i = 0; i < tokens.length; i++) {
				for (String s: faq2) {
					if (tokens[i].equals(s)) {
						messages.add(new TextMessage(GATHERING_POINT));
						//URL url = this.getClass().getResource("/static/gather.jpg");
						String url = "https://raw.githubusercontent.com/khwang0/2017F-COMP3111/master/Project/topic%201/gather.jpg";
						messages.add(new ImageMessage(url, url));
						return messages;
					}
				}
			}
		}
		
		// FAQ 3: Cancelled
		String faq3 = "cancel";
		if (text.contains("tour")) {
			if (containQW[0] || containQW[3]) {
				if (text.contains(faq3)) {
					messages.add(new TextMessage(CANCELLED_TOUR));
					return messages;
				}
			}
		}
		
		// FAQ 4: Additional charge
		String[] faq41 = {"additional", "extra"};
		boolean faq4First = false;
		String[] faq42 = {"charge", "fee", "cost", "charges", "fees", "costs"};
		boolean faq4Second = false;
		String faq4 = "surcharge";
		if (containQW[0] || containQW[3] || containQW[10] || containQW[11]) {
			for (int i = 0; i < tokens.length; i++) {
				if (!faq4First) {
					for (String s1: faq41) {
						if (tokens[i].equals(s1)) {
							faq4First = true;
							continue;
						}
					}
				}
				if (!faq4Second){
					for (String s2: faq42) {
						if (tokens[i].equals(s2)) {
							faq4Second = true;
							continue;
						}
					}
				}
				if (faq4First && faq4Second) {
					break;
				}
			}
			if (faq4First && faq4Second) {
				messages.add(new TextMessage(ADDITIONAL_CHARGE));
				return messages;
			} else {
				if (text.contains(faq4)) {
					messages.add(new TextMessage(ADDITIONAL_CHARGE));
					return messages;
				}
			}
		}
		
		// FAQ 5: Transportation in Guangdong
		String[] faq5 = {"transportation", "transport", "go", "goes"};
		if (containQW[0] || containQW[3]) {
			if (text.contains("guangdong")) {
				for (int i = 0; i < tokens.length; i++) {
					for (String s: faq5) {
						if (tokens[i].equals(s)) {
							messages.add(new TextMessage(TRANSPORTATION));
							return messages;
						}
					}
				}
			}
		}
		
		// FAQ 6: Contact tour guide
		String[] faq6 = {"contact", "find", "talk", "reach", "speak"};
		if (text.contains("tour guide")) {
			if (containQW[0] || containQW[3] || containQW[6] || containQW[7]) {
				for (int i = 0; i < tokens.length; i++) {
					for (String s: faq6) {
						if (tokens[i].contains(s)) {
							messages.add(new TextMessage(TOUR_GUIDE_CONTACT));
							return messages;
						}
					}
				}
			}
		}
		
		// FAQ 7: Insurance
		String faq7 = "insurance";
		if (containQW[0] || containQW[3] || containQW[4] || containQW[5] || containQW[8] || containQW[9] || containQW[10] || containQW[11]) {
			for (String s: tokens) {
				if (s.equals(faq7)) {
					messages.add(new TextMessage(INSURANCE));
					return messages;
				}
			}
		}
		
		// FAQ 8: Bed arrangement in hotel
		String[] faq81 = {"bed", "room"};
		boolean faq8First = false;
		String[] faq82 = {"arrangement", "arrange", "type", "single", "double", "twin", "number", "hotel"};
		for (String s: faq81) {
			if (text.contains(s)) 
				faq8First = true;
		}
		if (faq8First) {
			for (int i = 0; i < tokens.length; i++) {
				for (String s: faq82) {
					if (tokens[i].equals(s)) {
						messages.add(new TextMessage(HOTEL_BED));
						return messages;
					}
				}
			}
		}
		
		// FAQ 10: Swimming suit
		String[] faq10 = {"swimming suit", "bathing suit", "beachwear", "bikini", "swimsuit", "swimwear"};
		if (containQW[0] || containQW[3] || containQW[4]|| containQW[5] || containQW[6] || containQW[7] || containQW[8] || containQW[9]) {
			for (String s: faq10) {
				if (text.contains(s)) {
					messages.add(new TextMessage(SWIMMING_SUIT));
					return messages;
				}
			}
		}
		
		// FAQ 11: Vegeterian
		String faq11 = "vegetarian";
		if (containQW[4] || containQW[5] || containQW[8] || containQW[9]) {
			if (text.contains(faq11)) {
				messages.add(new TextMessage(VEGETARIAN));
				return messages;
			}
		}
		
		// FAQ 12: Children fee
		String[] faq121 = {"fee", "charge", "price", "cost"};
		boolean faq12First = false;
		String[] faq122 = {"kid", "child", "toodler", "kids", "toodlers", "children"};
		boolean faq12Second = false;
		String faq12 = "how much";
		if (containQW[0] || containQW[3] || containQW[4] || containQW[5] || containQW[8] || containQW[9] || containQW[10] || containQW[11]) {
			if (text.contains(faq12)) {
				for (int i = 0; i < tokens.length; i++) {
					for (String s: faq122) {
						if (tokens[i].equals(s)) {
							messages.add(new TextMessage(CHILDREN_TOUR_FEE));
							return messages;
						}
					}
				}
			} else {
				for (int i = 0; i < tokens.length; i++) {
					
					if (!faq12First) {
						for (String s: faq121) {
							if (tokens[i].equals(s)) {
								faq12First = true;
								break;
							}
						}
					}
					if (!faq12Second) {
						for (String s: faq122) {
							if (tokens[i].equals(s)) {
								faq12Second = true;
								break;
							}
						}
					}
					if (faq12First && faq12Second) {
						break;
					}
				}
			}
			if (faq12First && faq12Second) {
				messages.add(new TextMessage(CHILDREN_TOUR_FEE));
				return messages;
			}
		}
		
		// FAQ 13: Late in departure date
		String[] faq13 = {"leave", "depart", "departure", "leaving"};
		if (containQW[0] || containQW[4] || containQW[5] || containQW[6] || containQW[7] || containQW[8] || containQW[9]) {
			if (text.contains("late")) {
				for (int i = 0; i < tokens.length; i++) {
					for (String s: faq13) {
						if (tokens[i].equals(s)) {
							messages.add(new TextMessage(LATE));
							return messages;
						}
					}
				}
			}
		}
		status = -2;
		messages.add(new TextMessage(NOT_FOUND));
		return messages;
	}

	/*************************************************************************************************************************************************************/
	/****************************************************************SearchTour***********************************************************************************/
	/*************************************************************************************************************************************************************/
	private ArrayList<Message> searchTour(String text, String[] tokens, String[] tags) throws Exception {
		ArrayList<Message> messages = new ArrayList<Message>();

		String adj = "tour";
		String verb = null;
		String time = null;
		String place = null;
		String hotel = null;
		int tourNo = -1;
		String[] number = {"one", "two", "three", "four", "five"};
		String[] month = {"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"};

		if (booking.getTour() == null && tokens[0].equals("quit")) {
			customer.setStatus(-2);
			TextMessage message = new TextMessage("How can I help you?");
			messages.add(message);
			return messages;
		}

		//search for adjectives decribing the tour
		for(int i = 0; i < tokens.length; i++) {
			if (tags[i].equals("VB")) {
				verb = tokens[i];
			}

			if (tokens[i].contains("tour")) {
				for(int j = i - 1; j >= 0; j--) {
					if ((tags[j].equals("NN") || tags[j].equals("NNP") || tags[j].equals("JJ") || tags[j].equals("VBG")) && !tokens[j].equals(verb)) { ///////not equal questionVB
						adj = tokens[j] + " " + adj;
					}
					else break;
				}
			}
		}

		for(int i = 0; i < tags.length; i++) {
			if (tags[i].equals("LS") || tags[i].equals("CD")){
				try {
					tourNo = Integer.parseInt(tokens[i]);
				} catch (NumberFormatException e) {
					for(int j = 0; j < number.length; j++) {
						if (tokens[i].equals(number[j])) {
							tourNo = j + 1;
							break;
						}
					}
				}
			}
		}

		if (adj.length() > 4 && tourNo == -1 && booking.getTour() == null) {
			tourArrTemp = database.searchTour(adj.substring(0,adj.length()-5));
			customer.setTourArr(tourArrTemp);
			if (tourArrTemp.size() == 0) {
				TextMessage message = new TextMessage("Sorry we currently do not provide such tour. Please search for other tours.");
				messages.add(message); //do you need recommendation????????????????????????????????????????????
				return messages;
			}

			TextMessage totalNo = new TextMessage("Yes. We have " + tourArrTemp.size() + " similar tours.\nPlease enter the number to view the tour details.");
			messages.add(totalNo);
			if(tourArrTemp.size() >= 5){
				TextMessage message = new TextMessage("Sorry Free Version Line chatbot cannot reply more than 5 messages. Please be more specific.");
				messages.add(message);
				return messages;
			}

			for(int i = 0; i < tourArrTemp.size(); i++) {
				TextMessage message = new TextMessage(String.valueOf(i+1) + ". " + tourArrTemp.get(i).getTourID() + " " + tourArrTemp.get(i).getName());
				messages.add(message);
			}
			return messages;
		}
		else if (text.contains("tour") && adj.equals("tour") && booking.getTour() == null) {
			TextMessage message = new TextMessage("What kind of tours would you like to " + verb +"?"); //go to recommendation function??????????
			messages.add(message);
			return messages;
		}

		if(tourNo > 0 && tourNo <= tourArrTemp.size()) {
			Tour tour = tourArrTemp.get(tourNo-1);
			TextMessage message1 = new TextMessage(tour.getTourID() + " " + tour.getName() + " * " + tour.getDescription());
			messages.add(message1);

			DateFormat df = new SimpleDateFormat("d/MM");
			DecimalFormat priceFormat = new DecimalFormat("#0.#");

			//get all confirmed date	
			String confirmedDateString = "";
			ArrayList<Date> confirmedDate = tour.getConfirmedDate();
			for(int i = 0; i < confirmedDate.size(); i++) {
				if (i == confirmedDate.size() - 1) 
					confirmedDateString = confirmedDateString + df.format(confirmedDate.get(i));
				else
					confirmedDateString = df.format(confirmedDate.get(i)) + ", " + confirmedDateString;
			}
			if (confirmedDate.size() != 0) {
				TextMessage message2 = new TextMessage("We have confirmed tour on " + confirmedDateString);
				messages.add(message2);
			}
			//get all available date
			String availableDateString = "";
			ArrayList<Date> availableDate = tour.getAvailableDate();
			for(int i = 0; i < availableDate.size(); i++) {
				if (i == availableDate.size() - 1)
					availableDateString = availableDateString + df.format(availableDate.get(i));
				else
					availableDateString = df.format(availableDate.get(i)) + ", " + availableDateString;
			}
			if (availableDate.size() == 0) {
				booking.clearAllData();
				customer.setBooking(booking);
				TextMessage message6 = new TextMessage("All tours of " + tour.getTourID() + " are full. Maybe you can search for another tour.");
				messages.add(message6);
			}
			else {
				TextMessage message3 = new TextMessage("We have tour on " + availableDateString + " still accept application");
				TextMessage message4;
				if (tour.getWeekdayPrice() != tour.getWeekendPrice()) {
					message4 = new TextMessage("Fee: Weekday $" + priceFormat.format(tour.getWeekdayPrice()) + " / Weekend $" + priceFormat.format(tour.getWeekendPrice()));
				}
				else {
					message4 = new TextMessage("Fee: $" + priceFormat.format(tour.getWeekdayPrice()));
				}				
				TextMessage message5 = new TextMessage("Do you want to book this one? (Yes/No)");
				messages.add(message3);
				messages.add(message4);
				messages.add(message5);
				booking.setTour(tour);
				customer.setBooking(booking);
			}

			return messages;
		}

		if(booking.getTour() != null) {
			if (tokens[0].equals("yes")) {
				TextMessage message = new TextMessage("Thank you for choosing " + booking.getTour().getTourID() + " " + booking.getTour().getName() + "!");
				messages.add(message);

				if (customer.getCustomerID() == null) {
					customer.setStatus(5);
					TextMessage message1 = new TextMessage("You have not booked any tours before. Please enter your personal information.");
					TextMessage message2 = new TextMessage("What is your name?");
					messages.add(message1);
					messages.add(message2);
					return messages;
				}
				else{
					customer.setStatus(3);
					TextMessage message1 = new TextMessage("Which date you are going?");
					messages.add(message1);
					return messages;
				}
			}
			else if (tokens[0].equals("no")) {
				booking.clearAllData();
				customer.setBooking(booking);
				TextMessage message = new TextMessage("You can choose other tours from the above list or search for another tour.\nType 'quit' if you don't want to search tours.");
				messages.add(message);
				return messages;
			}
		}

		TextMessage message = new TextMessage("Sorry I don't understand. Please re-enter.");
		messages.add(message);
		return messages;
	}

	/*************************************************************************************************************************************************************/
	/****************************************************************Booking Process******************************************************************************/
	/*************************************************************************************************************************************************************/
	private ArrayList<Message> bookingProcess(String text) throws Exception {
		Tour tour = booking.getTour();
		String lineID = booking.getLineID();
		Date chosenDate = booking.getDate();
		int adult = booking.getNoOfAdults();
		int children = booking.getNoOfChildren();
		int toddler = booking.getNoOfToddler();
		double price = booking.getTourFee();
		double totalFee = booking.getTotalFee();
		String request = booking.getSpecialRequest();
		ArrayList<Message> messages = new ArrayList<Message>();
		DateFormat df = new SimpleDateFormat("d/MM");
		DecimalFormat priceFormat = new DecimalFormat("#0.#");

		if (text.equals("no")) {
			customer.setStatus(-2);
			booking.clearAllData();
			customer.setBooking(booking);
			TextMessage message1 = new TextMessage("You have abandoned this booking process.");
			TextMessage message2 = new TextMessage("What other help do you need?");
			messages.add(message1);
			messages.add(message2);
			return messages;
		}
		else if (totalFee != -1 && text.equals("confirmed")) {
			java.sql.Date sqlDate = new java.sql.Date(chosenDate.getTime());
			boolean success = database.bookTour(lineID, tour.getTourID(), sqlDate, adult, children, toddler, 0, totalFee, request);
			TextMessage message1;
			TextMessage message2;
			if (success) {
				message1 = new TextMessage("The booking process is done. Thank you for booking " + tour.getName() + "!");
				message2 = new TextMessage(CONFIRMED_BOOKING_PAYMENT);
			}
			else {
				message1 = new TextMessage("Some errors occur during the booking process.");
				message2 = new TextMessage("Please contact our customer service for help.");
			}
			
			TextMessage message3 = new TextMessage("What other help do you need?");
			customer.setStatus(-2);
			booking.clearAllData();
			customer.setBooking(booking);
			messages.add(message1);
			messages.add(message2);
			messages.add(message3);
			return messages;
		}

		if (lineID == null) {
			booking.setLineID(customer.getLineID());
			customer.setBooking(booking);			
		}
		
		if (price == -1 && chosenDate != null) {
			booking.setTourFee();
			customer.setBooking(booking);
		}

		if(chosenDate == null) { 		
			for(int i = 0; i < tour.getAvailableDate().size(); i++) {
				if (text.equals(df.format(tour.getAvailableDate().get(i)))) {
					booking.setDate(tour.getAvailableDate().get(i));
					customer.setBooking(booking);
					TextMessage message = new TextMessage("How many adults?");
					messages.add(message);
					return messages;
				}
			}

			TextMessage message = new TextMessage("Invalid date. Please choose an available date.");
			messages.add(message);
			return messages;
		}
		else if (adult == -1 && (Integer.parseInt(text) >= 0 && Integer.parseInt(text) <= tour.getCapacity())) {
			booking.setNoOfAdults(Integer.parseInt(text));
			customer.setBooking(booking);
			TextMessage message = new TextMessage("How many children (Age 4-11)?");
			messages.add(message);
			return messages;
		}
		else if (adult == -1 && !(Integer.parseInt(text) >= 0 && Integer.parseInt(text) <= tour.getCapacity())) {
			TextMessage message1 = new TextMessage("Sorry! There is only " + String.valueOf(tour.getCapacity()) + " vacancy.");
			TextMessage message2 = new TextMessage("Do you want to continue this booking? (If yes, please re-enter the number of adults. Otherwise, type 'no'.)");
			messages.add(message1);
			messages.add(message2);
			return messages;
		}
		else if (children == -1 && (Integer.parseInt(text) >= 0 && Integer.parseInt(text) <= tour.getCapacity() - adult)) {
			booking.setNoOfChildren(Integer.parseInt(text));
			customer.setBooking(booking);
			TextMessage message = new TextMessage("How many toddler (Age 0-3)?");
			messages.add(message);
			return messages;
		}
		else if (children == -1 && !(Integer.parseInt(text) >= 0 && Integer.parseInt(text) <= tour.getCapacity() - adult)) {
			TextMessage message1 = new TextMessage("Sorry! There is only " + String.valueOf(tour.getCapacity()-adult) + " vacancy.");
			TextMessage message2 = new TextMessage("Do you want to continue this booking? (If yes, please re-enter the number of children. Otherwise, type 'no'.)");
			messages.add(message1);
			messages.add(message2);
			return messages;
		}
		else if (toddler == -1 && (Integer.parseInt(text) >= 0 && Integer.parseInt(text) <= tour.getCapacity() - adult - children)) {
			booking.setNoOfToddler(Integer.parseInt(text));
			customer.setBooking(booking);
			TextMessage message = new TextMessage("Do you have any special request? (If yes, please enter your request. Otherwise, type 'NULL')");
			messages.add(message);
			return messages;
		}
		else if (toddler == -1 && !(Integer.parseInt(text) >= 0 && Integer.parseInt(text) <= tour.getCapacity() - adult - children)) {
			TextMessage message1 = new TextMessage("Sorry! There is only " + String.valueOf(tour.getCapacity()-adult-children) + " vacancy.");
			TextMessage message2 = new TextMessage("Do you want to continue this booking? (If yes, please re-enter the number of toddler. Otherwise, type 'no'.)");
			messages.add(message1);
			messages.add(message2);
			return messages;
		}
		else if (request == null && (text.length() > 0 && text.length() <= 50)) {
			booking.setSpecialRequest(text);
			int totalNoOfPeople = adult + children + toddler;
			double serviceCharge = 60.0 * tour.getDays() * totalNoOfPeople;
			double adultTotalFee = price * adult;
			double childrenTotalFee = price * 0.8 * children;
			booking.setTotalFee(adultTotalFee + childrenTotalFee + serviceCharge);
			customer.setBooking(booking);
			String line1 = tour.getTourID() + " " + tour.getName() + " " + df.format(chosenDate);
			String line2 = "\nNo. of adults: " + String.valueOf(adult) + " * $" + priceFormat.format(price) + " = $" + priceFormat.format(adultTotalFee);
			String line3 = "\nNo. of children: " + String.valueOf(children) + " * $" + priceFormat.format(price*0.8) + " = $" + priceFormat.format(childrenTotalFee);
			String line4 = "\nNo. of toddler: " + String.valueOf(toddler) + " (free of charge)";
			String line5 = "\nService Charge: $60 * " + String.valueOf(totalNoOfPeople) + " people * " + String.valueOf(tour.getDays()) + " days = $" + priceFormat.format(serviceCharge);
			String line6 = "\nTotal fee: $" + priceFormat.format(adultTotalFee + childrenTotalFee + serviceCharge);
			String line7 = "\n\nSpecial request: " + booking.getSpecialRequest();
			TextMessage message1 = new TextMessage(line1 + line2 + line3 + line4 + line5 + line6 + line7);
			TextMessage message2 = new TextMessage("Do you wish to confirm this booking? (If yes, please enter 'confirmed'. Otherwise, enter 'no'.)");
			messages.add(message1);
			messages.add(message2);
			return messages;
		}

		TextMessage message = new TextMessage("Sorry I don't understand. Please re-enter.");
		messages.add(message);
		return messages;
	}

	/*************************************************************************************************************************************************************/
	/****************************************************************Create Customer******************************************************************************/
	/*************************************************************************************************************************************************************/
	private ArrayList<Message> createCustomer(String text) throws Exception {
		ArrayList<Message> messages = new ArrayList<Message>();
		boolean success = false;		

		if (customer.getName() == null && (text.length() > 0 && text.length() <= 30)) {
			customer.setName(text);
			TextMessage message = new TextMessage("What is your phone number?");
			messages.add(message);
			return messages;
		}
		else if (customer.getName() == null && !(text.length() > 0 && text.length() <= 30)) {
			TextMessage message = new TextMessage("Name is too long. Please re-enter.");
			messages.add(message);
			return messages;
		}
		else if(customer.getPhoneNo() == null && (text.length() > 0 && text.length() <= 8)) {
			customer.setPhoneNo(text);
			TextMessage message = new TextMessage("What is your age?");
			messages.add(message);
			return messages;
		}
		else if (customer.getPhoneNo() == null && !(text.length() > 0 && text.length() <= 8)) {
			TextMessage message = new TextMessage("Invalid phone number. Please re-enter.");
			messages.add(message);
			return messages;
		}
		else if (customer.getAge() == -1 && (Integer.parseInt(text) > 0 && Integer.parseInt(text) <= 100)) {
			customer.setAge(Integer.parseInt(text));
			success = database.createCustomer(customer.getLineID(), customer.getName(), customer.getPhoneNo(), customer.getAge());
			success = database.checkExistingCustomer(customer.getLineID(), customer);
		}
		else if (customer.getAge() == -1 && !(Integer.parseInt(text) > 0 && Integer.parseInt(text) <= 100)) {
			TextMessage message = new TextMessage("Invalid age. Please re-enter.");
			messages.add(message);
			return messages;
		}

		if(success) {
			customer.setStatus(3);
			TextMessage message1 = new TextMessage("You have successfully create your account.");
			TextMessage message2 = new TextMessage("Which date you are going?");
			messages.add(message1);
			messages.add(message2);
			return messages;
		}
		else {
			customer.setStatus(-2);
			booking.clearAllData();
			TextMessage message1 = new TextMessage("You fail to create the account. Please contact our customer service for help.");
			TextMessage message2 = new TextMessage("What other help do you need?");///////////////////////////////////////////////////////
			messages.add(message1);
			messages.add(message2);
			return messages;
		}
	}

	/*************************************************************************************************************************************************************/
	/***************************************************************Previous Record*******************************************************************************/
	/*************************************************************************************************************************************************************/
	private ArrayList<Message> searchPreviousRecord() throws Exception {
		ArrayList<Message> messages = new ArrayList<Message>();
		ArrayList<String[]> recordArr = database.searchBookingRecord(customer.getLineID());

		if (recordArr.size() == 0) {
			customer.setStatus(-2);
			TextMessage message = new TextMessage("You do not have any booking record.");
			messages.add(message);
			return messages;
		}

		TextMessage message1 = new TextMessage("You have " + recordArr.size() + " booking record(s)."); ///////////////////////////////////////more than four???
		messages.add(message1);
		if (recordArr.size() <= 4) {
			for(int i = 0; i < recordArr.size(); i++) {
				String[] temp = recordArr.get(i);
				String line1 = String.valueOf(i+1) + ". Tour: (" + temp[0] + ") " + temp[1] + " - " + temp[2];
				String line2 = "\n Number of adults: " + temp[3];
				String line3 = "\n Number of children: " + temp[4];
				String line4 = "\n Number of toddler: " + temp[5];
				String line5 = "\n Total fee: " + temp[6];
				String line6 = "\n Amount paid: " + temp[7];
				String line7 = "\n Special request: " + temp[8];
				TextMessage message2 = new TextMessage(line1 + line2 + line3 + line4 + line5 + line6 + line7);
				messages.add(message2);
			}
		}

		customer.setStatus(-2);
		return messages;
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
