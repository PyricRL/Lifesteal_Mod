package com.pyric.lifestealmod;

public class ModConfig{
    // Heart cap 40 sets max hearts to 20
    public static int maxHealthCap = 40;

    // Heart min at 14 sets min hearts to 7
    public static int minHealthCap = 14;

    // Health increase set to 2 increases health by 1 heart each time
    public static double healthIncrease = 2.0;

    // Health decrease set to 2 decreases health by 1 heart each time
    public static double healthDecrease = 2.0;

    // Sets whether hearts have the ability to be crafted
    public static boolean heartCraft = true;

    // Sets whether a player under a certain amount of hearts gains hearts after a period of time
    public static boolean heartRegen = true;

    // Sets max health to regen, 20 is health regen amount is 10 hearts
    public static double healthRegenAmount = 20.0;

    // Sets the amount of time to regen a player heart  (in minutes)
    public static int heartRegenTime = 30;

    // Sets whether a player can withdraw hearts
    public static boolean heartWithdraw = true;
}