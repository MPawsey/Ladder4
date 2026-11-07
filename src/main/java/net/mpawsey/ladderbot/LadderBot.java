package net.mpawsey.ladderbot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashMap;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.JDA.Status;

public class LadderBot {

	public static final int TERMINATE_PERM = 1 << 0;
	public static final int LADDER_PERM = 1 << 4;
	public static Role test;

	private static String identifier = "ladder";
	
	public static JDA api;
	private static LadderMessageHandler messageHandler = new LadderMessageHandler();
	
	public static HashMap<Long, Ladder> ladders = new HashMap<Long, Ladder>();
	
	public static Ladder getLadder(long id)
	{
		Ladder ladder = ladders.get(id);
		if (ladder == null)
		{
			ladder = new Ladder(id);
			ladders.put(id, ladder);
		}
		return ladder;
	}
	
	public static void main(String[] args) throws Exception
	{
		try {
			// This reads the bot's token
			BufferedReader reader = new BufferedReader(
									new InputStreamReader(
									new FileInputStream(
									new File("../Tokens/"+
									         identifier+"Token"))));
			String token = reader.readLine();
			reader.close();

			try {
				// Creates the bot instance
				api = JDABuilder.createDefault(token).build();
				// Adds the event listener
				api.addEventListener(messageHandler);
				// Gets the discord id of the bot

				// Initialises message handler
				messageHandler.initialise(api.getSelfUser().getId());
				
				api.awaitStatus(Status.CONNECTED);
				
				
				System.out.println("Initialising ladders...");
				
				
				
				System.out.println("Ladders initialised.");

			} catch (LoginException le) {
				// This is not the correct token, did you regenerate it perhaps?
				System.out.println("The token provided for "+
									identifier+"BOT is invalid");

			} catch (IllegalArgumentException iae) {
				// There doesn't appear to be a token in the file
				System.out.println("The token provided for "+
									identifier+"BOT is null or empty");
			}
		} catch (FileNotFoundException fnfe) {
			// Unable to find the file. I've either put it in the wrong place or you haven't send me one
			System.out.println("Unable to find "+
								identifier+"BOT's token");

		} catch (SecurityException se) {
			// I haven't given the bot read privileges for it's token file, oops
			System.out.println("Unable to obtain read permissions for "+
								identifier+"BOT's token");

		} catch (NullPointerException npe) {
			// Reading the file returned null
			System.out.println("Path to "+
								identifier+"BOT's token not set");
		}
	}
}
