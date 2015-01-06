package decodes.polling;

public enum Parity
{
	Unknown('U'),
	None('N'), 
	Odd('O'),
	Even('E'),
	Mark('M'),
	Space('S');

	private char code;
	private Parity(char code)
	{
		this.code = code;
	}
	
	public char getCode() { return code; }
	
	public static Parity fromCode(char code)
	{
		for(Parity p : values())
			if (p.code == code)
				return p;
		return Unknown;
	}
	
	public static Parity fromString(String s)
	{
		for(Parity p : values())
			if (p.toString().equalsIgnoreCase(s))
				return p;
		return Unknown;
	}
}
