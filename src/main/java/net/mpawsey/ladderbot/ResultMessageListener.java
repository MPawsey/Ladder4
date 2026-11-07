package net.mpawsey.ladderbot;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.mpawsey.bothelper.AbstractMessageListener;

public class ResultMessageListener extends AbstractMessageListener
{
	private final int LADDER_PERM_BIT = 4;
	private final String THUMBS_UP = "U+1f44d";
	private final String THUMBS_DOWN = "U+1f44e";
	
	private Result result;
	private Ladder ladder;
	
	private int up = 0, down = 0;

	public ResultMessageListener(Result result, Ladder ladder) {
		super(result.getResultMessage());
		
		this.result = result;
	}
		
	@Override
	public void onMessageDeleted(MessageDeleteEvent event) 
	{
		ladder.removeResult(result);
		stopListening();
	}

	@Override
	public void onReactionAdded(MessageReactionAddEvent event) 
	{
		if (event.getReactionEmote().isEmoji())
		{
			String emoji = event.getReactionEmote().getEmoji();
			
			if (emoji.equals(THUMBS_UP))
			{
				if (result.getWinners().contains(event.getUserIdLong()) || result.getLosers().contains(event.getUserIdLong()))
				{
					++up;
					// TODO
				}
			}
			else if (emoji.equals(THUMBS_DOWN))
			{
				if (result.getWinners().contains(event.getUserIdLong()) || result.getLosers().contains(event.getUserIdLong()))
				{
					++down;
					// TODO
				}
			}
		}
		
	}

	@Override
	public void onReactionRemoved(MessageReactionRemoveEvent event) 
	{
		
	}
}
