package com.example.bot.spring;

public class Tour {
	int idInChatBot = -1; 		//Save tour's order of displaying for later use
	private String id;
	private String name;
	private String description;
	
	public Tour(int idInChatBot, String id, String name, String description) {
		this.idInChatBot = idInChatBot;
		this.id = id;
		this.name = name;
		this.description = description;
	}
	
	public String toString() {
		return id + " " + name + " " + description;
	}
	
	
	public String getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public String getDescription() {
		return description;
	}
}