package net.mpawsey.ladderbot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.mpawsey.bothelper.AbstractMessageHandler;
import net.mpawsey.bothelper.CmdParam;
import net.mpawsey.bothelper.Command;
import net.mpawsey.bothelper.CommandDescription;
import net.mpawsey.bothelper.CommandRequires;
import net.mpawsey.bothelper.MessageHelper;
import net.mpawsey.bothelper.OnMessageReceived;
import net.mpawsey.bothelper.CmdParam.Type;

public class LadderMessageHandler extends AbstractMessageHandler 
{
	public EmbedBuilder initEmbed(Ladder ladder)
	{
		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(new Color(105, 105, 255, 255));
		
		eb.setFooter("Made by R0bit");
		eb.setThumbnail(ladder.getThumbnail());
		
		
		return eb;
	}
	
	@Command("terminate")
	@CommandRequires(LadderBot.TERMINATE_PERM)
	@CommandDescription(description="Terminates the bot", usage="terminate [force|save]")
	public void terminateCmd(@CmdParam(Type.CHANNEL) MessageChannel channel, @CmdParam(Type.ARGS_RAW) String args)
	{
		if (args.equals("save"))
		{
			for (Ladder ladder : LadderBot.ladders.values())
			{
				if (!ladder.savePending())
				{
					MessageHelper.sendError("Unable to save the pending results, please resolve them or type `terminate force` to forcibly terminate ladder.", channel);
					return;
				}
			}
			
			MessageHelper.sendMessage("Saved.", channel);
				
		}
		else if (!args.equals("force") && LadderBot.ladders.values().stream().anyMatch(x -> x.hasPending()))
		{
			MessageHelper.sendError("The ladder still has results pending, please resolve them, type `terminate force` to forcibly terminate ladder, or `save` to save the results for when the bot starts (although more likely to break).", channel);
			return;
		}
		
		System.out.println("Shutting down.");
		//LadderBot.ladders.values().stream().forEach(x -> x.save());
		MessageHelper.sendMessage("Beep Boop :(", channel);
		LadderBot.api.shutdownNow();
		System.exit(0);
	}
	
	@Command("reset")
	@CommandRequires(LadderBot.LADDER_PERM)
	@CommandDescription(usage="reset", description="Resets all ladder values. This can only be undone by going back into the filesystem. Be careful.")
	public void resetCmd(@CmdParam(Type.CHANNEL) MessageChannel channel, @CmdParam(Type.GUILD) Guild guild)
	{
		Ladder ladder = LadderBot.getLadder(guild.getIdLong());
		EmbedBuilder eb = initEmbed(ladder);
		eb.setTitle("Random Match.");
		eb.addField("Team 1", "", false);
		eb.addField("Team 2", "", false);
		MessageHelper.sendTemporaryMessage(eb.build(), channel, 3600);
		
		
	}
	
	@Command("settings")
	@CommandRequires(LadderBot.LADDER_PERM)
	@CommandDescription(description="Shows/edits the current settings of the ladder", usage="settings [Key] [Value]")
	public void settingsCmd(@CmdParam(Type.CHANNEL) MessageChannel channel, @CmdParam(Type.ARGS) String[] args, @CmdParam(Type.GUILD) Guild guild)
	{
		Ladder ladder = LadderBot.getLadder(guild.getIdLong());
		if (args.length == 0)
		{
			final int whitespace = 20;
			
			//TODO: Print settings
			StringBuilder sb = new StringBuilder();
			sb.append("```hs\n");
			
			// Channels
			sb.append("results_channel");
			for (int i = "results_channel".length(); i <= whitespace; ++i)
				sb.append(' ');
			if (ladder.getResultsChannel() != null)
				sb.append(ladder.getResultsChannel().getName());
			else
				sb.append("-");
			sb.append('\n');

			sb.append("lb_channel");
			for (int i = "lb_channel".length(); i <= whitespace; ++i)
				sb.append(' ');
			if (ladder.getLeaderboardChannel() != null)
				sb.append(ladder.getLeaderboardChannel().getName());
			else
				sb.append("-");
			sb.append('\n');

			sb.append("history_channel");
			for (int i = "history_channel".length(); i <= whitespace; ++i)
				sb.append(' ');
			if (ladder.getHistoryChannel() != null)
				sb.append(ladder.getHistoryChannel().getName());
			else
				sb.append("-");
			sb.append('\n');

			sb.append("admin_channel");
			for (int i = "admin_channel".length(); i <= whitespace; ++i)
				sb.append(' ');
			if (ladder.getAdminChannel() != null)
				sb.append(ladder.getAdminChannel().getName());
			else
				sb.append("-");
			sb.append('\n');
			
			// History Length
			sb.append("history_length");
			for (int i = "history_length".length(); i <= whitespace; ++i)
				sb.append(' ');
			sb.append(ladder.getHistoryLength());
			sb.append('\n');
			
			// Thumbnail
			sb.append("thumbnail");
			for (int i = "thumbnail".length(); i <= whitespace; ++i)
				sb.append(' ');
			if (ladder.getThumbnail() != null)
				sb.append(ladder.getThumbnail());
			else
				sb.append('-');
			sb.append('\n');
			
			// Hue Colour Picker
			sb.append("hue");
			for (int i = "hue".length(); i <= whitespace; ++i)
				sb.append(' ');
			sb.append(Math.round(ladder.getHue() * 3600) / 10.f);
			sb.append('\n');

			sb.append("saturation");
			for (int i = "saturation".length(); i <= whitespace; ++i)
				sb.append(' ');
			sb.append(Math.round(ladder.getSaturation() * 1000) / 10.f);
			sb.append('\n');

			sb.append("brightness");
			for (int i = "brightness".length(); i <= whitespace; ++i)
				sb.append(' ');
			sb.append(Math.round(ladder.getBrightness() * 1000) / 10.f);
			sb.append('\n');
			
			
			sb.append("```");
			channel.sendMessage(sb.toString()).queue();
			
		}
		else if (args.length == 1)
		{
			MessageHelper.sendError("Too few arguments supplied.", channel);
		}
		else
		{
			String setting = args[0];
			String value = args[1];
			MessageChannel c;
			
			switch(setting)
			{
			case "results_channel":
				if (value.matches("<#[0-9]*>"))
					value = value.substring(3, value.length() - 1);
				c = guild.getTextChannelById(value);
				if (c == null)
				{
					MessageHelper.sendError("Could not find the channel with id " + value + ".", channel);
					return;
				}
				ladder.setResultsChannel(c);
				break;
			case "lb_channel":
				if (value.matches("<#[0-9]*>"))
					value = value.substring(3, value.length() - 1);
				c = guild.getTextChannelById(value);
				if (c == null)
				{
					MessageHelper.sendError("Could not find the channel with id " + value + ".", channel);
					return;
				}
				ladder.setLeaderboardChannel(c);
				break;
			case "history_channel":
				if (value.matches("<#[0-9]*>"))
					value = value.substring(3, value.length() - 1);
				c = guild.getTextChannelById(value);
				if (c == null)
				{
					MessageHelper.sendError("Could not find the channel with id " + value + ".", channel);
					return;
				}
				ladder.setHistoryChannel(c);
				break;
			case "admin_channel":
				if (value.matches("<#[0-9]*>"))
					value = value.substring(3, value.length() - 1);
				c = guild.getTextChannelById(value);
				if (c == null)
				{
					MessageHelper.sendError("Could not find the channel with id " + value + ".", channel);
					return;
				}
				ladder.setAdminChannel(c);
				break;
			case "history_length":
				if (value.matches("[0-9]*"))
					ladder.setHistoryLength(Integer.parseInt(value));
				else
				{
					MessageHelper.sendError("Invalid length. Accepts any positive integer or 0 for lifetime", channel);
					return;
				}
				break;
			case "thumbnail":
				ladder.setThumbnail(value);
				break;
			case "hue":
				if (value.matches("[0-9]*.[0-9]*"))
				{
					int val = Integer.parseInt(value);
					if (val < 0 || val > 360)
						val = Math.abs(val % 360);
					ladder.setHue(val);
				}
				else
				{
					MessageHelper.sendError("Invalid number given.", channel);
					return;
				}
				break;
			case "saturation":
				if (value.matches("[0-9]*.[0-9]*"))
				{
					int val = Integer.parseInt(value);
					if (val < 0 || val > 100)
					{
						MessageHelper.sendError("The value must be between 0 and 100.", channel);
						return;
					}
					ladder.setSaturation(val);
				}
				else
				{
					MessageHelper.sendError("Invalid number given.", channel);
					return;
				}
				break;
			case "brightness":
				if (value.matches("[0-9]*.[0-9]*"))
				{
					int val = Integer.parseInt(value);
					if (val < 0 || val > 100)
					{
						MessageHelper.sendError("The value must be between 0 and 100.", channel);
						return;
					}
					ladder.setBrightness(val);
				}
				else
				{
					MessageHelper.sendError("Invalid number given.", channel);
					return;
				}
				break;
			}
			
			MessageHelper.sendSuccess("Updated " + setting + ".", channel);
		}
	}
	
	@Command("match")
	@CommandDescription(description="Creates a match from the players in the current vc. Mmr is default", usage="match [random|mmr|captains]")
	public void matchCmd(@CmdParam(Type.CHANNEL) MessageChannel channel, @CmdParam(Type.ARGS_RAW) String args, @CmdParam(Type.GUILD) Guild guild, @CmdParam(Type.AUTHOR_MEMBER) Member member, @CmdParam(Type.MSG) Message msg)
	{
		MessageHelper.deleteMessageAfter(msg, messageLife);
		if (member.getVoiceState().inVoiceChannel())
		{
			MessageHelper.sendError("You must be in a vc to use this command.", channel, messageLife);
			return;
		}
		
		// Get relevant vars
		Ladder ladder = LadderBot.getLadder(guild.getIdLong());
		List<Member> vc = member.getVoiceState().getChannel().getMembers();
		List<Long> vcIds = vc.parallelStream().map(x -> x.getIdLong()).collect(Collectors.toList());
		SortedSet<Player> players = ladder.getPlayers().stream().filter(x -> vcIds.contains(x.getId())).collect(Collectors.toCollection(TreeSet::new));
		Player[] playersArray = players.toArray(new Player[6]);
		List<Player> team1 = new ArrayList<Player>();
		List<Player> team2 = new ArrayList<Player>();
		
		
		
		if (args.length() > 0)
		{
			if (args.equals("random"))
			{
				// random
				
				// Create list of possible players
				List<Player> availablePlayers = new ArrayList<Player>();
				for (int i = 0; i < vc.size(); ++i)
					availablePlayers.add(playersArray[i]);
				
				// Select team1
				Random random = new Random();
				for (int i = 0; i < vc.size() / 2; ++i)
				{
					int index = random.nextInt(availablePlayers.size());
					team1.add(availablePlayers.get(index));
					availablePlayers.remove(index);
				}
				
				// Available Players = team2 as team1 was removed
				team2 = availablePlayers;
				
				// Create embed
				EmbedBuilder eb = initEmbed(ladder);
				eb.setTitle("Random Match.");
				eb.addField("Team 1", "", false);
				eb.addField("Team 2", "", false);
				MessageHelper.sendTemporaryMessage(eb.build(), channel, 3600);
				
				return;
			}
			else if (args.equals("captains"))
			{
				// captains
				return;
			}
			else if (!args.equals("mmr"))
			{
				MessageHelper.sendError("Unknown match type `" + args + "`.", channel, messageLife);
				return;
			}
		}
		// mmr
		
	}
	

	@Command("result")
	@CommandDescription(description = "Adds a pending result", usage = "result <Score 1> <Team 1 @'s...> <Score 2> <Team 2 @'s...>")
	public void onResultCmd(@CmdParam(Type.MSG) Message msg, @CmdParam(Type.ARGS) String[] args, @CmdParam(Type.ARGS_RAW) String content, @CmdParam(Type.CHANNEL) MessageChannel channel, @CmdParam(Type.GUILD) Guild guild)
	{
		Ladder ladder = LadderBot.getLadder(guild.getIdLong());
		
		if (ladder.getResultsChannel() != null && ladder.getResultsChannel().getIdLong() == channel.getIdLong())
		{
			MessageHelper.deleteMessageAfter(msg, messageLife);
			
			// Check if result is valid format
			if (!content.matches("[0-9]+ (<@!?[0-9]+> )+[0-9]+( <@!?[0-9]+>)+")) 
			{
				MessageHelper.sendError("The result does not match the specified format.", channel, messageLife);
				return;
			}
			
			
			// Extract data from result
			int score1, score2 = 0;
			List<Long> team1 = new ArrayList<Long>(), team2 = new ArrayList<Long>();
			
			score1 = Integer.parseInt(args[0]);
			
			boolean hasSwappedTeam = false;
			
			for (int i = 1; i < args.length; ++i)
			{
				if (args[i].matches("[0-9]+"))
				{
					if (!hasSwappedTeam || team1.size() == 0)
						hasSwappedTeam = true;
					else
					{
						MessageHelper.sendError("Expected a user, was given the number `" + args[i] + "`.`", channel, messageLife);
						return;
					}
					
					score2 = Integer.parseInt(args[i]);
				}
				else
				{
					long id = MessageHelper.extractId(args[i]);
					if (team1.contains(id) || team2.contains(id))
					{
						MessageHelper.sendError("The same user was entered twice.", channel, messageLife);
						return;
					}
					
					if (hasSwappedTeam)
						team2.add(id);
					else
						team1.add(id);
				}
			}
			
			if (score1 == score2)
			{
				MessageHelper.sendError("A series must have a winner.", channel, messageLife);
				return;
			}
			
			if (score1 < score2)
			{
				List<Long> tmp = team2;
				team2 = team1;
				team1 = tmp;
			}
			
			EmbedBuilder eb = initEmbed(ladder);
			
			eb.setTitle("Ladder Update - Series #" + (ladder.getSeriesCount()+1));
			
			eb.addField("Winners (" + score1 + ")", team1.stream().map(id -> (String)MessageHelper.getNameFromId(id, guild, true)).collect(Collectors.joining("\n")), false);
			eb.addField("Losers (" + score2 + ")", team2.stream().map(id -> (String)MessageHelper.getNameFromId(id, guild, true)).collect(Collectors.joining("\n")), false);
			eb.addField("", "Please upvote this result if it is correct or downvote it if it is incorrect.", false);
			
			Result result = new Result(channel.sendMessage(eb.build()).complete(), team1, team2, score1, score2);
			ladder.addResult(result);
			addMessageListener(new ResultMessageListener(result, ladder));
		}
	}
	
	
	@OnMessageReceived(ignorePing=true)
	public void onMessageRecieved(@CmdParam(Type.MSG) Message msg, @CmdParam(Type.ARGS) String[] args, @CmdParam(Type.ARGS_RAW) String content, @CmdParam(Type.CHANNEL) MessageChannel channel, @CmdParam(Type.GUILD) Guild guild)
	{
		// Auto-removes messages from ladder channel
		Ladder ladder = LadderBot.getLadder(guild.getIdLong());
		if (ladder.getResultsChannel() != null && channel.equals(ladder.getResultsChannel()))
		{
			//  Parse result if detected
			if (content.matches("[0-9]+ (<@!?[0-9]+> )+[0-9]+( <@!?[0-9]+>)+"))
				onResultCmd(msg, args, content, channel, guild);
			else
				MessageHelper.deleteMessageAfter(msg, messageLife);
			
			return;
		}
	}
}
