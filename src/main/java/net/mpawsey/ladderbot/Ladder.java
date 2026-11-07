package net.mpawsey.ladderbot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.mpawsey.bothelper.FileManager;
import net.mpawsey.bothelper.MessageHelper;

public class Ladder 
{
	private final Guild GUILD;
	private TreeSet<Player> players;
	private MessageChannel resultsChannel = null, leaderboardChannel = null, historyChannel = null, adminChannel = null;
	private int seriesCount;
	private int historyLength;
	private String thumbnail;
	private float hue, saturation, brightness; // HUE colour picker
	
	// MMR system
	// Maps
	// Max players per Team
	
	private Queue<Result> resultsQueue = new LinkedList<Result>();
	
	public Ladder(long guildId)
	{
		this.GUILD = LadderBot.api.getGuildById(guildId);
		this.players = new TreeSet<Player>();
		this.historyLength = 13;
		this.seriesCount = 0;
		this.hue = 0.813888889f;
		this.saturation = 0.9f;
		this.brightness = 0.97f;
		
		// Load from file if possible
		if (FileManager.doesDirectoryExist(GUILD.getId()))
		{
			// Load settings
			String[] settings = FileManager.readFile(getSettingsPath()).get(0).split(",");
			//TODO: Settings
			this.seriesCount = Integer.parseInt(settings[0]);
			
			if (settings[1].length() > 0)
				resultsChannel = GUILD.getTextChannelById(settings[1]);
			if (settings[2].length() > 0)
				leaderboardChannel = GUILD.getTextChannelById(settings[2]);
			if (settings[3].length() > 0)
				historyChannel = GUILD.getTextChannelById(settings[3]);
			if (settings[4].length() > 0)
				adminChannel = GUILD.getTextChannelById(settings[4]);
			
			historyLength = Integer.parseInt(settings[5]);
			if (settings[6].length() > 0)
				thumbnail = settings[6];
			hue = Float.parseFloat(settings[7]);
			saturation = Float.parseFloat(settings[8]);
			brightness = Float.parseFloat(settings[9]);
			
			// Get file and add each player
			List<String> playersFile = FileManager.readFile(GUILD.getId() + getLadderPath());
			for (String player : playersFile)
				this.players.add(new Player(player.split(","), this.historyLength));
		}
		
	}
	
	public void addResult(Result result)
	{
		resultsQueue.add(result);
	}
	
	// Removes result from queue
	public void removeResult(Result result)
	{
		boolean front = false, update = false;
		int count = 0;
		
		for (Result res : resultsQueue)
		{
			if (res == result)
			{
				front = count == 0;
				update = true;
			}
			
			if (update)
			{
				MessageEmbed embed = res.getResultMessage().getEmbeds().get(0);
				embed.getTitle().replaceAll("#[0-9]+", "#" + (seriesCount + count + 1));
			}
			
			++count;
		}
		
		resultsQueue.remove(result);
		
		if (front)
			pollResults();
	}
	
	public void pollResults()
	{
		
	}
	
	public void print(boolean onlyHistory)
	{
		// Get Members and name sizes
		int longestName = 0, longestDiscord = 0;
		List<Tuple<Member, Player>> memberPlayers = new ArrayList<Tuple<Member, Player>>();
		for (Player player : players)
		{
			try
			{
				if (GUILD.retrieveMemberById(player.getId()).complete() == null)
					continue;
				
				Member member = GUILD.getMemberById(player.getId());
				memberPlayers.add(new Tuple<Member, Player>(member, player));
				// TODO: Format
				String name = MessageHelper.getNameFromId(player.getId(), GUILD, true);
				
				if (name.length() > longestName)
					longestName = name.length();
				if (member.getUser().getAsTag().length() > longestDiscord)
					longestDiscord = member.getUser().getAsTag().length();
			}
			catch (Exception e)
			{
				System.err.println("Could not find user " + player.getId() + " in the guild.");
			}
		}
		
		StringBuilder sb = new StringBuilder();

		// Build top row
		sb.append("```py\n");
		sb.append(new SimpleDateFormat("dd/MM").format(new Date()));
		sb.append(" │ Pos. │ Player");
		for (int i = 0; i < longestName - 6; ++i)
			sb.append(' ');
		sb.append(" │  MMR  │ Discord\n");
		sb.append("──────┼──────┼─");
		for (int i = 0; i < longestName; ++i)
			sb.append('─');
		sb.append("─┼───────┼─");
		for (int i = 0; i < longestDiscord; ++i)
			sb.append('─');
		
		sb.append('\n');
		
		// Iterate over players
		int pos = 0;
		for (Tuple<Member, Player> mp : memberPlayers)
		{
			++pos;
			
			// Position change
			if (pos == mp.second.getPrintedPos())
				sb.append(" →   ");
			else if (pos < mp.second.getPrintedPos())
			{
				sb.append(" ↑ ");
				int change = mp.second.getPrintedPos() - pos;
				if (change < 10)
					sb.append(' ');
				sb.append(change);			
			}
			else 
			{
				sb.append(" ↓ ");
				int change = pos - mp.second.getPrintedPos();
				if (change < 10)
					sb.append(' ');
				sb.append(change);	
			}
			mp.second.setPrintedPos(pos);
			sb.append(" │ ");
			
			// Position
			if (pos < 10)
				sb.append(' ');
			sb.append(pos);
			
			if (pos >= 11 && pos <= 13)
				sb.append("th");
			else
			{
				final String[] suffixes = { "th", "st", "nd", "3rd" };
				int tmp = pos % 10;
				if (tmp > 3)
					tmp = 0;
				sb.append(suffixes[tmp]);
			}
			
			sb.append(" │ ");
			
			// Name
			String name = MessageHelper.getNameFromId(mp.second.getId(), GUILD, true);
			sb.append(name);
			for (int i = 0; i < longestName - name.length(); ++i)
				sb.append(' ');
			sb.append(" | ");
			
			// MMR
			int digits = 1;
			for (int i = (int)mp.second.getMmr(); i > 9; i/=10) digits++;
			
			for (int i = 0; i < 5 - digits; ++i)
				sb.append(' ');
			sb.append((int)mp.second.getMmr());
			
			// Tag
			sb.append(" │ ");
			sb.append(mp.first.getUser().getAsTag());
			sb.append('\n');
		}
		
		sb.append("```");
		
		// Print leaderboard
		if (leaderboardChannel != null && !onlyHistory)
		{
			for (Message msg : leaderboardChannel.getIterableHistory())
			{
				if (msg.getAuthor().getId().equals(LadderBot.api.getSelfUser().getId()))
					msg.delete().queue();
			}
			
			// Try to send the message with the graphic
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				ImageIO.write(generateGraph(), "png", os);

				leaderboardChannel.sendMessage(sb.toString()).addFile(os.toByteArray(), "ladder.png").queue();
			} catch (IOException e) {
				e.printStackTrace();
				// Send without graphic instead
				MessageHelper.sendMessage(sb.toString(), leaderboardChannel);
				return;
			}
			
		}
		
		// Print history
		if (historyChannel != null)
			MessageHelper.sendMessage(sb.toString(), historyChannel);

		
	}
	
	// Generates a graph given the current state of the ladder
	public BufferedImage generateGraph()
	{
		BufferedImage bi = new BufferedImage(1600, 900, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		
		// Setup background
		g2d.setColor(new Color(47, 49, 52));
		g2d.fillRect(0, 0, 1600, 900);
		g2d.setColor(Color.BLACK);
		g2d.setStroke(new BasicStroke(5));
		g2d.drawRoundRect(0, 0, 1600, 900, 1, 1);
		
		
		BufferedImage img;
		try {
			HttpURLConnection con = (HttpURLConnection)(new URL(thumbnail).openConnection());
			con.setRequestProperty("User-Agent", "Mozilla");
			img = ImageIO.read(con.getInputStream());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return bi;
		} catch (IOException e) {
			e.printStackTrace();
			return bi;
		}
		
		// Draw icon
		g2d.drawImage(img, 25, 10, 5, 5, null);
		Font font = new Font("Ariel", Font.BOLD | Font.ITALIC, 32);
		g2d.setFont(font);
		g2d.setColor(Color.WHITE);
	    FontRenderContext frc = new FontRenderContext(null, false, false);
		Rectangle2D bounds = font.getStringBounds("LADDER", frc);
		g2d.drawString("LADDER", img.getWidth() / 2 - (float)bounds.getCenterX() + 25, (float)(-15 + img.getHeight() + bounds.getHeight()));
		
		// Draw Axes
		
		Stroke thick = new BasicStroke(2);
		Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {9}, 0);
		
		g2d.setStroke(thick);
		g2d.drawLine(200, 850, 1525, 850);
		g2d.drawLine(1525, 50, 1525, 850);
		
		
		// Draw player names
		double highestMmr = 1100;
		double lowestMmr = 900;
		double bestMmr = 0;
		Player bestPlayer = null;
		Font font2 = new Font("Ariel", Font.BOLD | Font.ITALIC, 16);
		g2d.setFont(font2);

		List<Player> graphPlayers = new ArrayList<Player>();
		
		for (Player player : players)
		{
			Member member = GUILD.retrieveMemberById(player.getId()).complete();
			if (member == null)
				continue;
			
			// Only show players with meaningful changes
			if (player.getMmrHistory()[0] != null && player.getMmr() == player.getMmrHistory()[0])
			{
				boolean valid = false;
				for (int i = 1; i < player.getMmrHistory().length; ++i)
				{
					if (!player.getMmrHistory()[i-1].equals(player.getMmrHistory()[i]))
					{
						valid = true;
						break;
					}
				}
				
				if (!valid)
					continue;
			}
			graphPlayers.add(player);

			
			if (player.getMmr() > bestMmr)
			{
				bestMmr = player.getMmr();
				bestPlayer = player;
			}
			
			if (player.getMmr() > highestMmr)
				highestMmr = player.getMmr();
			else if (player.getMmr() < lowestMmr)
				lowestMmr = player.getMmr();
			
			for (Double d : player.getMmrHistory())
			{
				if (d == null)
					continue;
				
				if (d >  highestMmr)
					highestMmr = d;
				if (d < lowestMmr)
					lowestMmr = d;
			}
		}
		
		// Round mmr scale to first 200 up/down
		int yMax = (int)highestMmr + 200 - ((int)highestMmr % 200);
		int yMin = (int)lowestMmr - ((int)lowestMmr % 200);
		
		
		// Draw guidelines
		for (int i = 0; i <= (yMax - yMin) / 200; ++i)
		{
			int y = 850 - (800 * 200 / (yMax - yMin)) * i;
			
			g2d.setStroke(thick);
			g2d.setColor(Color.WHITE);
			g2d.drawLine(1525, y, 1540, y);
			g2d.drawString("" + (i * 200 + yMin), 1545, y + 5);
			
			if (i != 0)
			{
				g2d.setStroke(dashed);
				g2d.setColor(Color.GRAY);
				g2d.drawLine(200, y, 1524, y);
			}
		}

		Rectangle2D bounds2 = font2.getStringBounds("#000", frc);
		
		int segments = Math.min(historyLength, seriesCount);
		
		for (int i = 0; i < segments + 1; ++i)
		{
			int x = 1525 - (1325 * i / segments);
			
			g2d.setStroke(thick);
			g2d.setColor(Color.WHITE);
			g2d.drawLine(x, 850, x, 865);
			g2d.drawString("#" + (seriesCount - i), x - (int)bounds2.getCenterX(), 885);
		}

		int index = 0;
		// First
		if (bestPlayer != null)
		{
			Color c = generateRandomColour(index, graphPlayers.size());
			int y = (int)(img.getHeight() + bounds.getHeight()) + 30 + (20 * index++);
	
			g2d.setColor(Color.WHITE);
			g2d.drawString(MessageHelper.getNameFromId(bestPlayer.getId(), GUILD, true), 45, y);
			
			g2d.setColor(c);
			g2d.drawLine(25, y-5, 40, y-5);
			
			g2d.setStroke(thick);
			GeneralPath line = new GeneralPath();
			
			line.moveTo(1523, 850 - ((bestPlayer.getMmr() - yMin) * 800 / (yMax - yMin)));
			
			for (int i = 0; i < segments; ++i)
			{
				if (bestPlayer.getMmrHistory()[i] != null)
					line.lineTo(1525 - (1325 * (i+1) / segments - 1), 850 - ((bestPlayer.getMmrHistory()[i] - yMin) * 800 / (yMax - yMin)));
			}
			
			g2d.draw(line);
		}
		
		// Draw player lines
		for (Player player : graphPlayers)
		{
			if (player == bestPlayer)
				continue;
			
			Color c = generateRandomColour(index, graphPlayers.size());
			
			int y = (int)(img.getHeight() + bounds.getHeight()) + 30 + (20 * index++);

			g2d.setColor(Color.WHITE);
			g2d.drawString(MessageHelper.getNameFromId(player.getId(), GUILD, true), 45, y);
			
			g2d.setColor(c);
			g2d.drawLine(25, y-5, 40, y-5);
			
			g2d.setStroke(thick);
			GeneralPath line = new GeneralPath();
			
			line.moveTo(1523, 850 - ((player.getMmr() - yMin) * 800 / (yMax - yMin)));
			
			for (int i = 0; i < segments; ++i)
			{
				// 850 - (800 * 200 / (yMax - yMin)) * i;
				if (player.getMmrHistory()[i] != null)
					line.lineTo(1525 - (1325 * (i+1) / segments - 1), 850 - ((player.getMmrHistory()[i] - yMin) * 800 / (yMax - yMin)));
			}
			
			g2d.draw(line);
			
		}
		
		return bi;
	}

	private Color generateRandomColour(int value, int max)
	{
		// the + is so value 0 = GE colour
		return Color.getHSBColor(value / (float)max + hue, saturation, brightness);
	}
	
	public void savePlayers()
	{
		List<String> playersText = players.parallelStream().map(x -> x.toString()).collect(Collectors.toList());
		FileManager.writeFile(getLadderPath(), playersText, false);
	}
	
	public void saveSettings()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(seriesCount);
		sb.append(',');
		if (resultsChannel != null)
			sb.append(resultsChannel.getId());
		sb.append(',');
		if (leaderboardChannel != null)
			sb.append(leaderboardChannel.getId());
		sb.append(',');
		if (historyChannel != null)
			sb.append(historyChannel.getId());
		sb.append(',');
		if (adminChannel != null)
			sb.append(adminChannel.getId());
		sb.append(',');
		sb.append(historyLength);
		sb.append(',');
		if (thumbnail != null)
			sb.append(thumbnail);
		sb.append(',');
		sb.append(hue);
		sb.append(',');
		sb.append(saturation);
		sb.append(',');
		sb.append(brightness);
		
		FileManager.writeFile(getSettingsPath(), sb.toString(), false);
		
	}
	
	public boolean savePending()
	{
		return true;
	}

	public boolean hasPending()
	{
		return false;
	}
	
	public SortedSet<Player> getPlayers()
	{
		return players;
	}
	
	public String getThumbnail()
	{
		return thumbnail;
	}
	
	public int getSeriesCount()
	{
		return seriesCount;
	}
	
	private String getSettingsPath()
	{
		return GUILD.getId() + "/settings";
	}
	
	private String getLadderPath()
	{
		return GUILD.getId() + "/ladder";
	}
	
	private String getHistoryPath()
	{
		return GUILD.getId() + "/history";
	}
	
	public Guild getGuild()
	{
		return GUILD;
	}
	
	public MessageChannel getResultsChannel()
	{
		return resultsChannel;
	}
	
	public MessageChannel getLeaderboardChannel()
	{
		return leaderboardChannel;
	}
	
	public MessageChannel getHistoryChannel()
	{
		return historyChannel;
	}
	
	public MessageChannel getAdminChannel()
	{
		return adminChannel;
	}
	
	public int getHistoryLength()
	{
		return historyLength;
	}
	
	public float getHue()
	{
		return hue;
	}
	
	public float getSaturation()
	{
		return saturation;
	}
	
	public float getBrightness()
	{
		return brightness;
	}
	
	public void setResultsChannel(MessageChannel channel)
	{
		this.resultsChannel = channel;
	}
	
	public void setLeaderboardChannel(MessageChannel channel)
	{
		this.leaderboardChannel = channel;
	}
	
	public void setHistoryChannel(MessageChannel channel)
	{
		this.historyChannel = channel;
	}
	
	public void setAdminChannel(MessageChannel channel)
	{
		this.adminChannel = channel;
	}
	
	public void setHistoryLength(int historyLength)
	{
		this.historyLength = historyLength;
	}
	
	public void setThumbnail(String thumbnail)
	{
		this.thumbnail = thumbnail;
	}
	
	public void setHue(float hue)
	{
		this.hue = hue;
	}
	
	public void setSaturation(float saturation)
	{
		this.saturation = saturation;
	}
	
	public void setBrightness(float brightness)
	{
		this.brightness = brightness;
	}
	
}
