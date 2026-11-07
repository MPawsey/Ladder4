package net.mpawsey.ladderbot;

public class Player implements Comparable<Player> 
{
	private static final double STARTING_MMR = 1000.d;
	
	private final long ID;
	private double mmr;
	private int printedPos;
	// Double so that values are nullable
	private Double[] mmrHistory;
	
	public Player(long id, int historyLength)
	{
		this.ID = id;
		this.mmr = STARTING_MMR;
		this.printedPos = Integer.MAX_VALUE;
		this.mmrHistory = new Double[historyLength];
		
	}
	
	public Player(long id, int historyLength, double mmr)
	{
		this.ID = id;
		this.mmr = mmr;
		this.printedPos = Integer.MAX_VALUE;
		this.mmrHistory = new Double[historyLength];
	}
	
	public Player(String[] str, int historyLength)
	{
		this.ID = Long.parseLong(str[0]);
		this.mmr = Long.parseLong(str[1]);
		this.printedPos = Integer.parseInt(str[2]);
		
		this.mmrHistory = new Double[historyLength];
		for (int i = 0; i < mmrHistory.length; ++i)
		{
			if (str[3+i].length() > 0)
				mmrHistory[i] = Double.parseDouble(str[3+i]);
		}
		
	}
	
	public void pushMmr(double mmr)
	{
		for (int i = 0; i < mmrHistory.length - 1; ++i)
		{
			mmrHistory[i+1] = mmrHistory[i];
		}
		mmrHistory[0] = this.mmr;
		this.mmr = mmr;
	}
	
	public Double[] getMmrHistory()
	{
		return mmrHistory;
	}
	
	public long getId()
	{
		return ID;
	}
	
	public double getMmr()
	{
		return mmr;
	}
	
	public int getPrintedPos()
	{
		return printedPos;
	}
	
	public void setPrintedPos(int pos)
	{
		printedPos = pos;
	}
	
	@Override
	public int compareTo(Player p)
	{
		int cmp = Double.compare(mmr, p.mmr);
		return cmp != 0 ? cmp : Long.compare(ID, p.ID);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(ID);
		sb.append(',');
		sb.append(mmr);
		//sb.append(',');
		//sb.append(',');
		//sb.append(',');
		return sb.toString();
	}
}
