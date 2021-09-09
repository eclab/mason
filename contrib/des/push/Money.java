/** 
	A CountableResource with a cute toString().  Money is assumed to be countable and thus have a
	minimum atomic value: for example the value "1" for US Dollars would be 1 penny, whereas
	a dollar would be 100.
*/

public class Money extends Resource
	{
	public Money(String currencySymbol, int initialAmount)
		{
		super(currencySymbol, initialAmount);
		}
	
	public Money(Money other)
		{
		super(other);
		}

	public Money(Money other, double amount)
		{
		super(other, amount);
		}
		
	/**
		Prints the resource out in a pleasing manner. 
	*/
	public String toString()
		{
		long amt = (long)amount;
		return name + (amt / 10L) + "." + (amt % 10L);
		}
		
	public Money duplicate()
		{
		return new Money(this);
		}
	}