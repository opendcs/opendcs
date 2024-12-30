package decodes.cwms.resevapcalc;

public enum WindShearMethod
{
    DONELAN("Donelan"),
    FISCHER("Fischer");

    private final String name;

    WindShearMethod(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }

    public static WindShearMethod fromString(String name)
    {
        for (WindShearMethod windShearMethod : WindShearMethod.values())
        {
            if (windShearMethod.name.equalsIgnoreCase(name))
            {
                return windShearMethod;
            }
        }
        throw new IllegalArgumentException("No wind shear method with name " + name + " found");
    }
}
