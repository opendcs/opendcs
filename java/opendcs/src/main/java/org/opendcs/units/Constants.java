package org.opendcs.units;

final public class Constants {

    public static final double PI  = 3.141592654;

    /*  K is the von Karman constant. */
    public static final double CONST_K = 0.4;

    /*  G is the acceleration of gravity. */
    public static final double CONST_G = 9.81;

    /* Stephan-Boltzman const.(W/m^2*K^4) */
    public static final double SIGMA = 5.669e-8;

    public static final double ACRES_TO_M2 = 4046.8564224;

    public static final double FT_TO_M = 0.3048;

   /* TK is 0 degrees C in kelvins.*/
    public static final double tK = 273.15;

    /* MW is the molecular weight of water in kg/mole.*/
    public static final double mw = 18.0160E-3;

    /* RGAS is the gas constant in J/mole-K.*/
    public static final double rgas = 8.31441;

    /* P0 is standard pressure in mb.*/
    public static final double p0 = 1013.25;

    // Private constructor to prevent instantiation
    private Constants()
        {
        throw new AssertionError("Constants class cannot be instantiated.");
        }


}