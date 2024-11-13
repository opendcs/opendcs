package usace.rowcps.computation.resevap;

public enum WindShearMethod
{
    DONELAN("Donelan"),
    FISCHER("Fischer");

    private final String _name;

    WindShearMethod(String name)
    {
        _name = name;
    }

    @Override
    public String toString()
    {
        return _name;
    }

    public static WindShearMethod fromString(String name)
    {
        for (WindShearMethod windShearMethod : WindShearMethod.values())
        {
            if (windShearMethod._name.equalsIgnoreCase(name))
            {
                return windShearMethod;
            }
        }
        throw new IllegalArgumentException("No wind shear method with name " + name + " found");
    }
}
