package open.dolphin.client;

import ch.randelshofer.quaqua.QuaquaManager;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.util.List;
import java.util.*;
import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import open.dolphin.exception.DolphinException;
import open.dolphin.infomodel.DepartmentModel;
import open.dolphin.infomodel.DiagnosisCategoryModel;
import open.dolphin.infomodel.DiagnosisOutcomeModel;
import open.dolphin.infomodel.LicenseModel;
import open.dolphin.project.Project;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

/**
 * Dolphin Client のコンテキストクラス。
 *
 * @author  Kazushi Minagawa, Digital Globe, Inc.
 */
public final class ClientContextStub {

    //--------------------------------------------------------------------------
    private final String RESOURCE_LOCATION = "/open/dolphin/resources/";
    private final String TEMPLATE_LOCATION = "/open/dolphin/resources/templates/";
    private final String IMAGE_LOCATION = "/open/dolphin/resources/images/";
    private final String SCHEMA_LOCATION = "/open/dolphin/resources/schema/";
    private final String RESOURCE = "open.dolphin.resources.Dolphin_ja";
    private final String PROPERTY_OS = "os.name";
    private final String OS_WIN = "windows";
    private final String OS_MAC = "mac";
    private final String OS_LINUX = "linux";
    private final String RESNAME_VERSION = "version";
    private final String RESNAME_APP_TITLE = "application.title";
    private final String TITLE_CONCAT = "-";
    //--------------------------------------------------------------------------

    private HashMap<String, Color> eventColorTable;
    private LinkedHashMap<String, String> toolProviders;

    private String pathToDolphin;

    private boolean dolphinPro;
    
    private boolean debug;

    //private URLClassLoader pluginClassLoader;

    /**
     * ClientContextStub オブジェクトを生成する。
     */
    public ClientContextStub(boolean pro) {
        this(pro, false);
    }    
    
    public ClientContextStub(boolean pro, boolean debug) {

        dolphinPro = pro;
        this.debug = debug;

        try {
            //----------------------------------------
            // user.home に Dolphin directoryを生成する
            //----------------------------------------
//masuda^
/*
            if (dolphinPro) {
                pathToDolphin = createDirectory(System.getProperty("user.home"), "OpenDolphinPro");
            } else {
                pathToDolphin = createDirectory(System.getProperty("user.home"), "OpenDolphin");
            }
*/
            pathToDolphin = createDirectory(System.getProperty("user.home"), "OpenDolphin-m");
//masuda%
            String pathToSetting = createDirectory(pathToDolphin, "setting");
            createDirectory(pathToDolphin, "log");
            createDirectory(pathToDolphin, "pdf");
            createDirectory(pathToDolphin, "schema");
            //createDirectory(pathToDolphin, "plugins");
            createDirectory(pathToDolphin, "odt_template");

            //------------------------------
            // Log4J のコンフィグレーションを行う
            //------------------------------
            File log4jProp = new File(pathToSetting, "log4j.properties");

            if (log4jProp.exists()) {
                PropertyConfigurator.configure(log4jProp.getPath());

            } else {
                Properties prop = new Properties();
                BufferedInputStream in = new BufferedInputStream(getResourceAsStream("log4j.properties"));
                prop.load(in);
                in.close();
                prop.setProperty("log4j.appender.bootAppender.File", pathToLogFile("boot.log"));
                prop.setProperty("log4j.appender.part11Appender.File", pathToLogFile("part11.log"));
                prop.setProperty("log4j.appender.delegaterAppender.File", pathToLogFile("delegater.log"));
                prop.setProperty("log4j.appender.pvtAppender.File", pathToLogFile("pvt.log"));
                prop.setProperty("log4j.appender.labTestAppender.File", pathToLogFile("labTest.log"));
                prop.setProperty("log4j.appender.claimAppender.File", pathToLogFile("claim.log"));
                prop.setProperty("log4j.appender.mmlAppender.File", pathToLogFile("mml.log"));
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(log4jProp));
                prop.store(out, getVersion());
                out.close();
                PropertyConfigurator.configure(prop);
            }

            //------------------------------
            // 基本情報を出力する
            //------------------------------
            if (getBootLogger().getLevel() == Level.DEBUG) {
                getBootLogger().debug("boot.time = " + DateFormat.getDateTimeInstance().format(new Date()));
                getBootLogger().debug("os.name = " + System.getProperty("os.name"));
                getBootLogger().debug("java.version = " + System.getProperty("java.version"));
                getBootLogger().debug("dolphin.version = " + getVersion());
                getBootLogger().debug("base.directory = " + getBaseDirectory());
                getBootLogger().debug("setting.directory = " + getSettingDirectory());
                getBootLogger().debug("log.directory = " + getLogDirectory());
                getBootLogger().debug("pdf.directory = " + getPDFDirectory());
                getBootLogger().debug("schema.directory = " + getSchemaDirectory());
                //getBootLogger().debug("plugins.directory = " + getPluginsDirectory());
                getBootLogger().debug("log.config.file = " + getString("log.config.file"));
                getBootLogger().debug("veleocity.log.file = " + getString("application.velocity.log.file"));
            }

//            //------------------------------
//            // Plugin Class Loader を生成する
//            //------------------------------
//            List<String> test = new ArrayList<String>();
//            File pluginDir = new File(getLocation("plugins"));
//            listJars(test, pluginDir);
//            List<URL> list = new ArrayList<URL>();
//            StringBuilder sb;
//            for (String path : test) {
//                sb = new StringBuilder();
//                if (isWin()) {
//                    sb.append("jar:file:/");
//                } else {
//                    sb.append("jar:file://");
//                }
//                sb.append(path);
//                sb.append("!/");
//                URL url = new URL(sb.toString());
//                list.add(url);
//            }
//            URL[] urls = list.toArray(new URL[list.size()]);
//            pluginClassLoader = new URLClassLoader(urls);

            //------------------------------
            // Velocityを初期化する
            //------------------------------
            StringBuilder sb = new StringBuilder();
            sb.append(getLogDirectory());
            sb.append(File.separator);
            sb.append(getString("application.velocity.log.file"));
            Velocity.setProperty("runtime.log", sb.toString());
            Velocity.init();
            getBootLogger().debug("Velocity did initialize");

            //------------------------------
            // LookANdFeel、フォント、mac Menubarを変更する
            //------------------------------
//masuda^   Projectを作ってからにする
            //setUI();
//masuda$

        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private String createDirectory(String parent, String child) throws DolphinException {

        File dir = new File(parent, child);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                StringBuilder sb = new StringBuilder();
                sb.append("ディレクトリ");
                sb.append(parent).append(File.separator).append(child);
                sb.append("を作成できません。");
                throw new DolphinException(sb.toString());
            }
        }
        return dir.getPath();
    }

    private String pathToLogFile(String logFile) {
        StringBuilder sb = new StringBuilder();
        sb.append(getLogDirectory()).append("/").append(logFile);
        return sb.toString();
    }
    
    public LinkedHashMap<String, String> getToolProviders() {
        return toolProviders;
    }

//    public URLClassLoader getPluginClassLoader() {
//        return pluginClassLoader;
//    }

    public VelocityContext getVelocityContext() {
        return new VelocityContext();
    }

    public ResourceBundle getBundle(Class cls) {

        String path = getClassResource(cls);

        // No cache
        return ResourceBundle.getBundle(path, Locale.getDefault(), cls.getClassLoader(), new ResourceBundle.Control() {

            @Override
            public long getTimeToLive(String baseName, Locale locale) {
                return ResourceBundle.Control.TTL_DONT_CACHE;
            }
        });
    }

    private static String getClassResource(Class cls) {
        StringBuilder sb = new StringBuilder();
        String clsName = cls.getName();
        int index = clsName.lastIndexOf(".");
        sb.append(clsName.subSequence(0, index));
        sb.append(".resources.");
        sb.append(clsName.substring(index+1));
        return sb.toString();
    }

    //---------------------------------------------------------

    public Logger getBootLogger() {
        Logger logger = Logger.getLogger("boot.logger");
        if (debug) {
            logger.setLevel(Level.DEBUG);
        }
        return logger;
    }

    public Logger getPart11Logger() {
        Logger logger = Logger.getLogger("part11.logger");
        if (debug) {
            logger.setLevel(Level.DEBUG);
        }
        return logger;
    }

    public Logger getClaimLogger() {
        Logger logger = Logger.getLogger("claim.logger");
        if (debug) {
            logger.setLevel(Level.DEBUG);
        }
        return logger;
    }

    public Logger getMmlLogger() {
        Logger logger = Logger.getLogger("mml.logger");
        if (debug) {
            logger.setLevel(Level.DEBUG);
        }
        return logger;
    }

    public Logger getPvtLogger() {
        Logger logger = Logger.getLogger("pvt.logger");
        if (debug) {
            logger.setLevel(Level.DEBUG);
        }
        return logger;
    }

    public Logger getDelegaterLogger() {
        Logger logger =Logger.getLogger("delegater.logger");
        if (debug) {
            logger.setLevel(Level.DEBUG);
        }
        return logger;
    }

    public Logger getLaboTestLogger() {
        Logger logger =Logger.getLogger("labTest.logger");
        if (debug) {
            logger.setLevel(Level.DEBUG);
        }
        return logger;
    }

    //-----------------------------------------------------------

    public boolean isMac() {
        return System.getProperty(PROPERTY_OS).toLowerCase().startsWith(OS_MAC);
    }

    public boolean isWin() {
        return System.getProperty(PROPERTY_OS).toLowerCase().startsWith(OS_WIN);
    }

    public boolean isLinux() {
        return System.getProperty(PROPERTY_OS).toLowerCase().startsWith(OS_LINUX);
    }

    public boolean isOpenDolphin() {
        boolean open = !dolphinPro;
        return open;
    }

    public boolean isDolphinPro() {
        return dolphinPro;
    }

    //-----------------------------------------------------------

    private String getLocation(String dirName) {
        StringBuilder sb = new StringBuilder();
        sb.append(getBaseDirectory()).append(File.separator).append(dirName);
        return sb.toString();
    }

    public String getBaseDirectory() {
        return pathToDolphin;
    }

//    public String getPluginsDirectory() {
//        return getLocation("plugins");
//    }

    public String getSettingDirectory() {
        return getLocation("setting");
    }

    public String getLogDirectory() {
        return getLocation("log");
    }
    
    public String getPDFDirectory() {
        return getLocation("pdf");
    }

    public String getSchemaDirectory() {
        return getLocation("schema");
    }
    
    public String getOdtTemplateDirectory() {
        return getLocation("odt_template");
    }

    //-----------------------------------------------------------

    public String getVersion() {
        return getString(RESNAME_VERSION);
    }

    public String getFrameTitle(String title) {
        try {
            String resTitle = getString(title);
            if (resTitle != null) {
                title = resTitle;
            }
        } catch (Exception e) {
        }
        StringBuilder buf = new StringBuilder();
        buf.append(title);
        buf.append(TITLE_CONCAT);
        buf.append(getString(RESNAME_APP_TITLE));
        buf.append(TITLE_CONCAT);
        buf.append(getString(RESNAME_VERSION));
        return buf.toString();
    }

    public URL getResource(String name) {
        if (!name.startsWith("/")) {
            name = RESOURCE_LOCATION + name;
        }
        return this.getClass().getResource(name);
    }

    public URL getImageResource(String name) {
        if (!name.startsWith("/")) {
            name = IMAGE_LOCATION + name;
        }
        return this.getClass().getResource(name);
    }

    public InputStream getResourceAsStream(String name) {
        if (!name.startsWith("/")) {
            name = RESOURCE_LOCATION + name;
        }
        return this.getClass().getResourceAsStream(name);
    }

    public InputStream getTemplateAsStream(String name) {
        if (!name.startsWith("/")) {
            name = TEMPLATE_LOCATION + name;
        }
        return this.getClass().getResourceAsStream(name);
    }

    public ImageIcon getImageIcon(String name) {
        if (name!=null) {
            return new ImageIcon(getImageResource(name));
        }
        return null;
    }

    public ImageIcon getSchemaIcon(String name) {
        if (!name.startsWith("/")) {
            name = SCHEMA_LOCATION + name;
        }
        return new ImageIcon(this.getClass().getResource(name));
    }

    public LicenseModel[] getLicenseModel() {
        String[] desc = getStringArray("licenseDesc");
        String[] code = getStringArray("license");
        String codeSys = getString("licenseCodeSys");
        LicenseModel[] ret = new LicenseModel[desc.length];
        LicenseModel model;
        for (int i = 0; i < desc.length; i++) {
            model = new LicenseModel();
            model.setLicense(code[i]);
            model.setLicenseDesc(desc[i]);
            model.setLicenseCodeSys(codeSys);
            ret[i] = model;
        }
        return ret;
    }

    public DepartmentModel[] getDepartmentModel() {
        String[] desc = getStringArray("departmentDesc");
        String[] code = getStringArray("department");
        String codeSys = getString("departmentCodeSys");
        DepartmentModel[] ret = new DepartmentModel[desc.length];
        DepartmentModel model;
        for (int i = 0; i < desc.length; i++) {
            model = new DepartmentModel();
            model.setDepartment(code[i]);
            model.setDepartmentDesc(desc[i]);
            model.setDepartmentCodeSys(codeSys);
            ret[i] = model;
        }
        return ret;
    }

    public DiagnosisOutcomeModel[] getDiagnosisOutcomeModel() {
        String[] desc = getStringArray("diagnosis.outcomeDesc");
        String[] code = getStringArray("diagnosis.outcome");
        String codeSys = getString("diagnosis.outcomeCodeSys");
        DiagnosisOutcomeModel[] ret = new DiagnosisOutcomeModel[desc.length];
        DiagnosisOutcomeModel model;
        for (int i = 0; i < desc.length; i++) {
            model = new DiagnosisOutcomeModel();
            model.setOutcome(code[i]);
            model.setOutcomeDesc(desc[i]);
            model.setOutcomeCodeSys(codeSys);
            ret[i] = model;
        }
        return ret;
    }

    public DiagnosisCategoryModel[] getDiagnosisCategoryModel() {
        String[] desc = getStringArray("diagnosis.outcomeDesc");
        String[] code = getStringArray("diagnosis.outcome");
        String[] codeSys = getStringArray("diagnosis.outcomeCodeSys");
        DiagnosisCategoryModel[] ret = new DiagnosisCategoryModel[desc.length];
        DiagnosisCategoryModel model;
        for (int i = 0; i < desc.length; i++) {
            model = new DiagnosisCategoryModel();
            model.setDiagnosisCategory(code[i]);
            model.setDiagnosisCategoryDesc(desc[i]);
            model.setDiagnosisCategoryCodeSys(codeSys[i]);
            ret[i] = model;
        }
        return ret;
    }

    public NameValuePair[] getNameValuePair(String key) {
        NameValuePair[] ret;
        String[] code = getStringArray(key + ".value");
        String[] name = getStringArray(key + ".name");
        int len = code.length;
        ret = new NameValuePair[len];

        for (int i = 0; i < len; i++) {
            ret[i] = new NameValuePair(name[i], code[i]);
        }
        return ret;
    }

    public HashMap<String, Color> getEventColorTable() {
        if (eventColorTable == null) {
            setupEventColorTable();
        }
        return eventColorTable;
    }

    private void setupEventColorTable() {
        // イベントカラーを定義する
        eventColorTable = new HashMap<String, Color>(10, 0.75f);
        eventColorTable.put("TODAY", getColor("color.TODAY_BACK"));
        eventColorTable.put("BIRTHDAY", getColor("color.BIRTHDAY_BACK"));
        eventColorTable.put("PVT", getColor("color.PVT"));
        eventColorTable.put("DOC_HISTORY", getColor("color.PVT"));
//masuda^   来院歴のカレンダーに処方や予約のある日を色つきで表示するため
        eventColorTable.put("RP", getColor("color.RP"));
        eventColorTable.put("EXAM_APPO2", getColor("color.EXAM_APPO2"));
        eventColorTable.put("TEST2", getColor("color.TEST2"));
        eventColorTable.put("IMAGE2", getColor("color.IMAGE2"));
        eventColorTable.put("OTHER2", getColor("color.OTHER2"));
//masuda$
    }

    public String getString(String key) {
//masuda^   add trim()
        //return ResourceBundle.getBundle(RESOURCE).getString(key);
        return ResourceBundle.getBundle(RESOURCE).getString(key).trim();
//masuda$
    }

    public String[] getStringArray(String key) {
        String line = getString(key);
        return line.split(",");
    }

    public int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    public int[] getIntArray(String key) {
        String[] obj = getStringArray(key);
        int[] ret = new int[obj.length];
        for (int i = 0; i < obj.length; i++) {
            ret[i] = Integer.parseInt(obj[i]);
        }
        return ret;
    }

    public long getLong(String key) {
        return Long.parseLong(getString(key));
    }

    public long[] getLongArray(String key) {
        String[] obj = getStringArray(key);
        long[] ret = new long[obj.length];
        for (int i = 0; i < obj.length; i++) {
            ret[i] = Long.parseLong(obj[i]);
        }
        return ret;
    }

    public float getFloat(String key) {
        return Float.parseFloat(getString(key));
    }

    public float[] getFloatArray(String key) {
        String[] obj = getStringArray(key);
        float[] ret = new float[obj.length];
        for (int i = 0; i < obj.length; i++) {
            ret[i] = Float.parseFloat(obj[i]);
        }
        return ret;
    }

    public double getDouble(String key) {
        return Double.parseDouble(getString(key));
    }

    public double[] getDoubleArray(String key) {
        String[] obj = getStringArray(key);
        double[] ret = new double[obj.length];
        for (int i = 0; i < obj.length; i++) {
            ret[i] = Double.parseDouble(obj[i]);
        }
        return ret;
    }

    public boolean getBoolean(String key) {
        return Boolean.valueOf(getString(key)).booleanValue();
    }

    public boolean[] getBooleanArray(String key) {
        String[] obj = getStringArray(key);
        boolean[] ret = new boolean[obj.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = Boolean.valueOf(obj[i]).booleanValue();
        }
        return ret;
    }

    public Point lgetPoint(String name) {
        int[] data = getIntArray(name);
        return new Point(data[0], data[1]);
    }

    public Dimension getDimension(String name) {
        int[] data = getIntArray(name);
        return new Dimension(data[0], data[1]);
    }

    public Insets getInsets(String name) {
        int[] data = getIntArray(name);
        return new Insets(data[0], data[1], data[2], data[3]);
    }

    public Color getColor(String key) {
        int[] data = getIntArray(key);
//masuda^   アルファ値があればそれも設定
        if (data.length == 4) {
            return new Color(data[0], data[1], data[2], data[3]);
        }
        return new Color(data[0], data[1], data[2]);
//masuda$
    }

    public Color[] getColorArray(String key) {
        int[] data = getIntArray(key);
        int cnt = data.length / 3;
        Color[] ret = new Color[cnt];
        for (int i = 0; i < cnt; i++) {
            int bias = i * 3;
            ret[i] = new Color(data[bias], data[bias + 1], data[bias + 2]);
        }
        return ret;
    }

    public Class[] getClassArray(String name) {
        String[] clsStr = getStringArray(name);
        Class[] ret = new Class[clsStr.length];
        try {
            for (int i = 0; i < clsStr.length; i++) {
                ret[i] = Class.forName(clsStr[i]);
            }
            return ret;

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    public int getHigherRowHeight() {
        return 20;
    }

    public int getMoreHigherRowHeight() {
        return isMac() ? 20 : 25;
    }
    
    private void listJars(List list, File dir) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                listJars(list, file);
            } else if (file.isFile()) {
                String path = file.getPath();
                if (path.toLowerCase().endsWith(".jar")) {
                    list.add(path);
                }
            }
        }
    }

    /**
     * LookAndFeel、フォント、Mac メニューバー使用を設定する。
     * Modified by masuda
     */
    public void setUI() {

        final String defaultLaf = UIManager.getSystemLookAndFeelClassName();
        final String nimbusCls = "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";
        final String quaquaCls = "ch.randelshofer.quaqua.QuaquaLookAndFeel";
        //String userLaf = Project.getString("lookAndFeel", nimbusCls);
        String userLaf = Project.getString("lookAndFeel", nimbusCls);

        // Quaqua設定
        if (quaquaCls.equals(userLaf)) {
            // QuaquaのFileChooserは慣れないので使用しない。
            if (!isMac()) {
                Set<String> excludes = new HashSet<String>();
                excludes.add("FileChooser");
                QuaquaManager.setExcludedUIs(excludes);
            }

            System.setProperty("Quaqua.tabLayoutPolicy", "wrap");
            UIManager.put("ComboBox.maximumRowCount", 20);
            UIManager.put("TextComponent.autoSelect", Boolean.FALSE);
            UIManager.put("OptionPane.cancelButtonText", "キャンセル");
            UIManager.put("OptionPane.okButtonText", "OK");
            UIManager.put("OptionPane.yesButtonText", "はい");
            UIManager.put("OptionPane.noButtonText", "いいえ");
            //UIManager.put("Component.visualMargin", new Insets(0,0,0,0));
            //UIManager.put("TabbedPane.contentBorderPainted", false);
        }

        try {
            if (isMac() && nimbusCls.equals(userLaf)) {
                userLaf = defaultLaf;
            }
            UIManager.setLookAndFeel(userLaf);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            getBootLogger().warn(e.getMessage());
        }

        // quaquaでもHOME/ENDは行頭/行末に移動するためにInputMapを上書きする
        if (quaquaCls.equals(userLaf)) {

            UIDefaults defaults = UIManager.getLookAndFeelDefaults();
            final String focusInputMaps[] = {
                "TextPane.focusInputMap",
                "TextArea.focusInputMap",
                "EditorPane.focusInputMap"};
            for (String key : focusInputMaps) {
                InputMap map = (InputMap) defaults.get(key);
                map.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), DefaultEditorKit.beginLineAction);
                map.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), DefaultEditorKit.endLineAction);
                defaults.put(key, map);
            }
        }

        if (isWin() || isLinux()) {
            int size = isLinux() ? 13: 12;
            Font font = new Font("SansSerif", Font.PLAIN, size);
            UIManager.put("Label.font", font);
            UIManager.put("Button.font", font);
            UIManager.put("ToggleButton.font", font);
            UIManager.put("Menu.font", font);
            UIManager.put("MenuItem.font", font);
            UIManager.put("CheckBox.font", font);
            UIManager.put("CheckBoxMenuItem.font", font);
            UIManager.put("RadioButton.font", font);
            UIManager.put("RadioButtonMenuItem.font", font);
            UIManager.put("ToolBar.font", font);
            UIManager.put("ComboBox.font", font);
            UIManager.put("TabbedPane.font", font);
            UIManager.put("TitledBorder.font", font);
            UIManager.put("List.font", font);

            getBootLogger().debug("デフォルトのフォントを変更しました。");
        }

        // defaultLafではカルテのフォントをちょっと大きく
        if (isWin() && defaultLaf.equals(userLaf)) {
            Font font = new Font("SansSerif", Font.PLAIN, 13);
            UIManager.put("TextPane.font", font);
        }
    }
}