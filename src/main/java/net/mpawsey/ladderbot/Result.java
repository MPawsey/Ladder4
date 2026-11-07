package net.mpawsey.ladderbot;

import java.util.List;

import net.dv8tion.jda.api.entities.Message;

public class Result {
	
	private List<Long> winners, losers;
	private int scoreW, scoreL;
	private Message resultMsg;
	private boolean valid = false;
	
	public Result(Message resultMsg, List<Long> winners, List<Long> losers, int scoreW, int scoreL)
	{
		this.resultMsg = resultMsg;
		this.winners = winners;
		this.losers = losers;
		this.scoreW = scoreW;
		this.scoreL = scoreL;
	}
	
	public Message getResultMessage()
	{
		return resultMsg;
	}
	
	public List<Long> getWinners()
	{
		return winners;
	}
	
	public List<Long> getLosers()
	{
		return losers;
	}
	
	public int getWinnersScore()
	{
		return scoreW;
	}
	
	public int getLosersScore()
	{
		return scoreL;
	}
	
	public boolean isValid()
	{
		return valid;
	}
	
	public void setValid(boolean valid)
	{
		this.valid = valid;
	}
	
}
