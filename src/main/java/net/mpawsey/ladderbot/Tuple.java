package net.mpawsey.ladderbot;

public class Tuple<X, Y> {
	public X first;
	public Y second;
	
	public Tuple(X first, Y second)
	{
		this.first = first;
		this.second = second;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Tuple))
			return false;
		
		Tuple<?,?> t = (Tuple<?,?>)o;
		
		if (!(first.getClass().equals(t.first.getClass())) || !(second.getClass().equals(t.second.getClass())))
			return false;
		
		return first.equals(t.first) && second.equals(t.second);
	}
}
