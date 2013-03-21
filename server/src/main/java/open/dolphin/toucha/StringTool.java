package open.dolphin.toucha;

import java.text.SimpleDateFormat;

/**
 * Utilities to handel String.
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
public final class StringTool {
    
    private static final char[] komoji = { 'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ', 'っ', 'ゃ',
    'ゅ', 'ょ', 'ゎ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ッ', 'ャ', 'ュ', 'ョ', 'ヮ' };

    private static final char FIRST_HIRAGANA = 'ぁ';
    private static final char LAST_HIRAGANA = 'ん';
    private static final char FIRST_KATAKANA = 'ァ';
    private static final char LAST_KATAKANA = 'ヶ';
    
    private static final Character[] ZENKAKU_UPPER = {new Character('Ａ'), new Character('Ｚ')};
    
    private static final Character[] ZENKAKU_LOWER = {new Character('ａ'), new Character('ｚ')};
    
    private static final Character[] HANKAKU_UPPER = {new Character('A'), new Character('Z')};
    
    private static final Character[] HANKAKU_LOWER = {new Character('a'), new Character('z')};

    
    /** Creates new StringTool */
    public StringTool() {
    }
    
    public static boolean startsWithKatakana(String s) {
        return isKatakana(s.charAt(0));
    }
    
    public static boolean startsWithHiragana(String s) {
        return isHiragana(s.charAt(0));
    }
    
    public static boolean isKatakana(char c) {
        return (c >= FIRST_KATAKANA && c <= LAST_KATAKANA);
    }
    
    public static boolean isHiragana(char c) {
        return (c >= FIRST_HIRAGANA && c <= LAST_HIRAGANA);
    }
    
    private static char toKatakana(char c) {
        return  (char) ((int)FIRST_KATAKANA + (int) c - (int)FIRST_HIRAGANA);
    }
    
    public static String hiraganaToKatakana(String s) {
        
        int len = s.length();
        char[] src = new char[len];
        s.getChars(0, s.length(), src, 0);
        
        char[] dst = new char[len];
        for (int i = 0; i < len; i++) {
            if (isHiragana(src[i])) {
                dst[i] = toKatakana(src[i]);
            } else {
                dst[i] = src[i];
            }
        }
        return new String(dst);
    }
    
    public static boolean isAllDigit(String str) {
        
        boolean ret = true;
        int len = str.length();
        
        for (int i = 0; i < len; i++) {
            
            char c = str.charAt(i);
            
            if (!Character.isDigit(c)) {
                ret = false;
                break;
            }
        }
        return ret;
    }
    
    public static boolean isAllKana(String str) {
        
        boolean ret = true;
        int len = str.length();
        
        for (int i = 0; i < len; i++) {
            
            char c = str.charAt(i);
            
            if (isKatakana(c) || isHiragana(c)) {
                continue;
            } else {
                ret = false;
                break;
            }
        }
        return ret;
    }
    
    /**
     * Convert to Zenkaku
     */
    public static String toZenkaku(String s) {

        if (s != null) {
            for (int i = 0; i < komoji.length; i++) {
                // s = s.replace(komoji[i], ohomoji[i]);
                s = s.replace(komoji[i], '.');
            }
        }

        return s;
    }
    
    public static String toKatakana(String text, boolean b) { 
        if (b) {
            text = toZenkaku(text);
        }
        return hiraganaToKatakana(text);
    }
    
    public static boolean isZenkakuUpper(char c) {
        Character test = new Character(c);
        return (test.compareTo(ZENKAKU_UPPER[0]) >= 0 && test.compareTo(ZENKAKU_UPPER[1]) <= 0);
    }
    
    public static boolean isZenkakuLower(char c) {
        Character test = new Character(c);
        return (test.compareTo(ZENKAKU_LOWER[0]) >= 0 && test.compareTo(ZENKAKU_LOWER[1]) <= 0);
    }
    
    public static boolean isHankakuUpper(char c) {
        Character test = new Character(c);
        return (test.compareTo(HANKAKU_UPPER[0]) >= 0 && test.compareTo(HANKAKU_UPPER[1]) <= 0);
    }
    
    public static boolean isHanakuLower(char c) {
        Character test = new Character(c);
        return (test.compareTo(HANKAKU_LOWER[0]) >= 0 && test.compareTo(HANKAKU_LOWER[1]) <= 0);
    }
    
    public static String toZenkakuUpperLower(String s) {
        int len = s.length();
        char[] src = new char[len];
        s.getChars(0, s.length(), src, 0);
        
        StringBuilder sb = new StringBuilder();
        for (char c : src) {
            if (isHankakuUpper(c) || isHanakuLower(c)) {
                sb.append( (char)((int)c + 65248) );
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
//masuda^   http://www.alqmst.co.jp/tech/040601.html
    public static boolean isZenkaku(char c) {
        if (c <= '\u007e' || // 英数字
            c == '\u00a5' || // \記号
            c == '\u203e' || // ~記号
            (c >= '\uff61' && c <= '\uff9f')) // 半角カナ
        {
            return false;
        } else {
            return true;
        }
    }
    
    public static int getByteLength(String str) {
        int length = 0;
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (isZenkaku(c)) {
                length += 2;
            } else {
                length++;
            }
        }
        return length;
    }
    
    public static int getByteLength(char c) {
        return isZenkaku(c) ? 2 : 1;
    }
    
    // from PatientSearch
    public static boolean isDate(String text) {
        boolean maybe = false;
        if (text != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.parse(text);
                maybe = true;

            } catch (Exception e) {
            }
        }

        return maybe;
    }
    
    public static boolean isNameAddress(String text) {
        boolean maybe = false;
        if (text != null) {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (Character.getType(c) == Character.OTHER_LETTER) {
                    maybe = true;
                    break;
                }
            }
        }
        return maybe;
    }
//masuda$
}