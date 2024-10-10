package lrgs.lrgsmain;

public class PreloadLrgsInterface
	implements LrgsInputInterface
{
	int slot = 0;

	@Override
	public int getType()
	{
		return 0;
	}

	@Override
	public void setSlot(int slot)
	{
		this.slot = slot;
	}

	@Override
	public int getSlot()
	{
		return slot;
	}

	@Override
	public String getInputName()
	{
		return "preload";
	}

	@Override
	public void initLrgsInput() throws LrgsInputException
	{
	}

	@Override
	public void shutdownLrgsInput()
	{
	}

	@Override
	public void enableLrgsInput(boolean enabled)
	{
	}

	@Override
	public boolean hasBER()
	{
		return false;
	}

	@Override
	public String getBER()
	{
		return null;
	}

	@Override
	public boolean hasSequenceNums()
	{
		return false;
	}

	@Override
	public int getStatusCode()
	{
		return 0;
	}

	@Override
	public String getStatus()
	{
		return "";
	}

	@Override
	public int getDataSourceId()
	{
		return 0;
	}

	@Override
	public boolean getsAPRMessages()
	{
		return false;
	}

	@Override
	public String getGroup()
	{
		return null;
	}
}
