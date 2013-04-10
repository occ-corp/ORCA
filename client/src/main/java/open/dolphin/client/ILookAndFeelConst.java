package open.dolphin.client;

/**
 * LAF定数群
 * @author masuda, Masuda Naika
 */
public interface ILookAndFeelConst {
    
    // LAF
    public static final String WIN_LAF_CLS = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
    public static final String NIMBUS_LAF_CLS = "javax.swing.plaf.nimbus.NimbusLookAndFeel";
    
    public static final String QUAQUA_LAF_CLS = "ch.randelshofer.quaqua.QuaquaLookAndFeel";
    
    public static final String JGOODIES = "com.jgoodies.looks.";
    public static final String JGOODIES_WINDOWS_LAF_CLS = JGOODIES + "windows.WindowsLookAndFeel";
    public static final String JGOODIES_PLASTIC_LAF_CLS = JGOODIES + ".plastic.PlasticLookAndFeel";
    public static final String JGOODIES_PLASTIC3D_LAF_CLS = JGOODIES + "plastic.Plastic3DLookAndFeel";
    public static final String JGOODIES_PLASTICXP_LAF_CLS = JGOODIES + "plastic.PlasticXPLookAndFeel";
    
    public static final String JTATTOO = "com.jtattoo.plaf.";
    public static final String JTATTOO_ACRYL_LAF_CLS = JTATTOO + "acryl.AcrylLookAndFeel";
    public static final String JTATTOO_AERO_LAF_CLS = JTATTOO + "aero.AeroLookAndFeel";
    public static final String JTATTOO_ALUMINIUM_LAF_CLS = JTATTOO + "aluminium.AluminiumLookAndFeel";
    public static final String JTATTOO_BERNSTEIN_LAF_CLS = JTATTOO + "bernstein.BernsteinLookAndFeel";
    public static final String JTATTOO_FAST_LAF_CLS = JTATTOO + "fast.FastLookAndFeel";
    public static final String JTATTOO_HIFI_LAF_CLS = JTATTOO + "hifi.HiFiLookAndFeel";
    public static final String JTATTOO_MCWIN_LAF_CLS = JTATTOO + "mcwin.McWinLookAndFeel";
    public static final String JTATTOO_MINT_LAF_CLS = JTATTOO + "mint.MintLookAndFeel";
    public static final String JTATTOO_NOIRE_LAF_CLS = JTATTOO + "noire.NoireLookAndFeel";
    public static final String JTATTOO_SMART_LAF_CLS = JTATTOO + "smart.SmartLookAndFeel";
    public static final String JTATTOO_LUNA_LAF_CLS = JTATTOO + "luna.LunaLookAndFeel";
    public static final String JTATTOO_TEXTURE_LAF_CLS = JTATTOO + "texture.TextureLookAndFeel";
    
    public static final String SEAGLASS_LAF_CLS = "com.seaglasslookandfeel.SeaGlassLookAndFeel";
    public static final String WEB_LAF_CLS = "com.alee.laf.WebLookAndFeel";
    
    public static final String SUBSTANCE = "org.pushingpixels.substance.api.skin.";
    public static final String SUBSTANCE_BUSINESS_LAF_CLS = SUBSTANCE + "SubstanceBusinessLookAndFeel";
    public static final String SUBSTANCE_BUSINESS_BLUE_STEEL_LAF_CLS = SUBSTANCE + "SubstanceBusinessBlueSteelLookAndFeel";
    public static final String SUBSTANCE_BUSINESS_BLACK_STEEL_LAF_CLS = SUBSTANCE + "SubstanceBusinessBlackSteelLookAndFeel";
    public static final String SUBSTANCE_CREME_LAF_CLS = SUBSTANCE + "SubstanceCremeLookAndFeel";
    public static final String SUBSTANCE_CREME_COFFEE_LAF_CLS = SUBSTANCE + "SubstanceCremeCoffeeLookAndFeel";
    public static final String SUBSTANCE_SAHARA_LAF_CLS = SUBSTANCE + "SubstanceSaharaLookAndFeel";
    public static final String SUBSTANCE_MODERATE_LAF_CLS = SUBSTANCE + "SubstanceModerateLookAndFeel";
    public static final String SUBSTANCE_NEBULA_LAF_CLS = SUBSTANCE + "SubstanceNebulaLookAndFeel";
    public static final String SUBSTANCE_NEBULA_BRICK_WALL_LAF_CLS = SUBSTANCE + "SubstanceNebulaBrickWallLookAndFeel";
    public static final String SUBSTANCE_AUTUMN_LAF_CLS = SUBSTANCE + "SubstanceAutumnLookAndFeel";
    public static final String SUBSTANCE_MIST_SILVER_LAF_CLS = SUBSTANCE + "SubstanceMistSilverLookAndFeel";
    public static final String SUBSTANCE_MIST_AQUA_LAF_CLS = SUBSTANCE + "SubstanceMistAquaLookAndFeel";
    public static final String SUBSTANCE_DUST_LAF_CLS = SUBSTANCE + "SubstanceDustLookAndFeel";
    public static final String SUBSTANCE_DUST_COFFEE_LAF_CLS = SUBSTANCE + "SubstanceDustCoffeeLookAndFeel";
    public static final String SUBSTANCE_GEMINI_LAF_CLS = SUBSTANCE + "SubstanceGeminiLookAndFeel";
    public static final String SUBSTANCE_MARINER_LAF_CLS = SUBSTANCE + "SubstanceMarinerLookAndFeel";
    public static final String SUBSTANCE_TWILIGHT_LAF_CLS = SUBSTANCE + "SubstanceTwilightLookAndFeel";
    public static final String SUBSTANCE_MAGELLAN_LAF_CLS = SUBSTANCE + "SubstanceMagellanLookAndFeel";
    public static final String SUBSTANCE_GRAPHITE_LAF_CLS = SUBSTANCE + "SubstanceGraphiteLookAndFeel";
    public static final String SUBSTANCE_GRAPHITE_GLASS_LAF_CLS = SUBSTANCE + "SubstanceGraphiteGlassLookAndFeel";
    public static final String SUBSTANCE_GRAPHITE_AQUA_LAF_CLS = SUBSTANCE + "SubstanceGraphiteAquaLookAndFeel";
    public static final String SUBSTANCE_RAVEN_LAF_CLS = SUBSTANCE + "SubstanceRavenLookAndFeel";
    public static final String SUBSTANCE_CALLENGER_DEEP_LAF_CLS = SUBSTANCE + "SubstanceChallengerDeepLookAndFeel";
    public static final String SUBSTANCE_EMERALD_DUSK_LAF_CLS = SUBSTANCE + "SubstanceEmeraldDuskLookAndFeel";

    public static final String[][] EXT_LAF_INFO = {
        {null, "Quaqua", QUAQUA_LAF_CLS},
        
        {"JGoodies", "Windows", JGOODIES_WINDOWS_LAF_CLS},
        {"JGoodies", "Plastic", JGOODIES_PLASTIC_LAF_CLS},
        {"JGoodies", "Plastic3D", JGOODIES_PLASTIC3D_LAF_CLS},
        {"JGoodies", "PlasticXP", JGOODIES_PLASTICXP_LAF_CLS},
        
        {"JTattoo", "Acryl", JTATTOO_ACRYL_LAF_CLS},
        {"JTattoo", "Aero", JTATTOO_AERO_LAF_CLS},
        {"JTattoo", "Aluminium", JTATTOO_ALUMINIUM_LAF_CLS},
        {"JTattoo", "Bernstein", JTATTOO_BERNSTEIN_LAF_CLS},
        {"JTattoo", "Fast", JTATTOO_FAST_LAF_CLS},
        {"JTattoo", "HiFi", JTATTOO_HIFI_LAF_CLS},
        {"JTattoo", "McWin", JTATTOO_MCWIN_LAF_CLS},
        {"JTattoo", "Mint", JTATTOO_MINT_LAF_CLS},
        {"JTattoo", "Noire", JTATTOO_NOIRE_LAF_CLS},
        {"JTattoo", "Smart", JTATTOO_SMART_LAF_CLS},
        {"JTattoo", "Luna", JTATTOO_LUNA_LAF_CLS},
        {"JTattoo", "Texture", JTATTOO_TEXTURE_LAF_CLS},
        
        {null, "Seaglass", SEAGLASS_LAF_CLS},
/*        
        {null, "Web Look And Feel", WEB_LAF_CLS},

        {"Substance", "Business", SUBSTANCE_BUSINESS_LAF_CLS},
        {"Substance", "Business Blue Steel", SUBSTANCE_BUSINESS_BLUE_STEEL_LAF_CLS},
        {"Substance", "Business Black Steel", SUBSTANCE_BUSINESS_BLACK_STEEL_LAF_CLS},
        {"Substance", "Creme", SUBSTANCE_CREME_LAF_CLS},
        {"Substance", "Creme Coffee", SUBSTANCE_CREME_COFFEE_LAF_CLS},
        {"Substance", "Sahara", SUBSTANCE_SAHARA_LAF_CLS},
        {"Substance", "Moderate", SUBSTANCE_MODERATE_LAF_CLS},
        {"Substance", "Nebula", SUBSTANCE_NEBULA_LAF_CLS},
        {"Substance", "Nebula Brick Wall", SUBSTANCE_NEBULA_BRICK_WALL_LAF_CLS},
        {"Substance", "Autumun", SUBSTANCE_AUTUMN_LAF_CLS},
        {"Substance", "Mist Silver", SUBSTANCE_MIST_SILVER_LAF_CLS},
        {"Substance", "Mist Aqua", SUBSTANCE_MIST_AQUA_LAF_CLS},
        {"Substance", "Dust", SUBSTANCE_DUST_LAF_CLS},
        {"Substance", "Dust Coffee", SUBSTANCE_DUST_COFFEE_LAF_CLS},
        {"Substance", "Gemini", SUBSTANCE_GEMINI_LAF_CLS},
        {"Substance", "Mariner", SUBSTANCE_MARINER_LAF_CLS},
        {"Substance", "Twilight", SUBSTANCE_TWILIGHT_LAF_CLS},
        {"Substance", "Magellan", SUBSTANCE_MAGELLAN_LAF_CLS},
        {"Substance", "Graphite", SUBSTANCE_GRAPHITE_LAF_CLS},
        {"Substance", "Graphite Glass", SUBSTANCE_GRAPHITE_GLASS_LAF_CLS},
        {"Substance", "Graphite Aqua", SUBSTANCE_GRAPHITE_AQUA_LAF_CLS},
        {"Substance", "Raven", SUBSTANCE_RAVEN_LAF_CLS},
        {"Substance", "Challenger Deep", SUBSTANCE_CALLENGER_DEEP_LAF_CLS},
        {"Substance", "Emerald Dusk", SUBSTANCE_EMERALD_DUSK_LAF_CLS},
*/
     };
}
