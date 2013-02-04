package open.dolphin.dao;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import open.dolphin.client.ClientContext;
import open.dolphin.util.StringTool;

/**
 * 簡易的に管理テーブルのコピー句を読んでみる。手抜きｗ
 * @author masuda, Masuda Naika
 */
public class IncReader {
    
    private static final int COLUMN_COUNT = 5;
    private static final String CAMMA = ",";
    private static final String COMMENT_MARK = "*";
    private static final String TABLE = "SYS-%s-TBL";
    private static final String[] IGNORES = new String[]{"GF"};
    private static final String ENCODING = "UTF-8";
    
    private String kanricd;
    private String orcaVer;
    
    public IncReader(String kanricd, String orcaVer) {
        this.kanricd = kanricd;
        this.orcaVer = orcaVer.toLowerCase();
        if ("orca47".equals(orcaVer)) {
            this.orcaVer = "orca46";
        }
    }
    
    public Map<String, String> getMap(String data) throws UnsupportedEncodingException, IOException {
        
        // 文字列を扱いやすくするために固定長のデータを作る
        char[] charArray = getCharArray(data);
        
        StringBuilder sb = new StringBuilder();
        sb.append("inc/").append(orcaVer).append("/");
        sb.append("CPSK").append(kanricd).append(".csv");
        String incResource = sb.toString();
        
        InputStream is=  ClientContext.getResourceAsStream(incResource);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, ENCODING));
        
        String line;
        String kanriTblLevel = null;
        Map<String, Integer> nameNumMap = new HashMap<String, Integer>();
        Map<String, String> ret = new HashMap<String, String>();
        String tableName = String.format(TABLE, kanricd);
        int pos = 0;

        while ((line = br.readLine()) != null) {
            
            // コメントならcontinue;
            if (line.startsWith(COMMENT_MARK)) {
                continue;
            }
            
            // データを取得
            String[] datum = line.split(CAMMA);
            if (datum.length != COLUMN_COUNT) {
                continue;
            }
            String level = datum[1];
            String name = datum[2];
            String type = datum[3];
            String length = datum[4];
            
            // SYS-XXXX-TBL行を探す
            if (kanriTblLevel == null) {
                if (name.equals(tableName)) {
                   kanriTblLevel = level; 
                }
                continue;
            }
            
            // GFなどならcontinue
            boolean skip = false;
            for (String ignore : IGNORES) {
                if (ignore.equals(type)) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                continue;
            }
            
            // SYS-XXXX-TBLと同じレベルが出現したらkanriTblの終了
            if (kanriTblLevel != null && kanriTblLevel.equals(level)) {
                break;
            }
            
            // 出現回数を記録する
            Integer num = nameNumMap.get(name);
            if (num == null) {
                num = 0;
            }
            nameNumMap.put(name, ++num);

            // 複数回出現するものは番号をつける
            String keyName = (num == 1)
                    ? name
                    : name + "-" + String.valueOf(num);
            int len = Integer.valueOf(length);
            String value = getString(charArray, pos, len);
            if (!value.isEmpty()) {
                ret.put(keyName, value);
            }
            pos += len;
        }
        
        br.close();
        is.close();
        
        return ret;
    }
    
    // 文字列を扱いやすくするために固定長のデータを作る
    private char[] getCharArray(String str) {
        
        int len = str.length();
        char[] chars = new char[len * 2];
        int pos = 0;
        
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            chars[pos++] = c;
            // 全角文字ならばダミーを挿入して長さを合わせる
            boolean zenkaku = StringTool.isZenkaku(c);
            if (zenkaku) {
                chars[pos++] = 0;
            }
        }
        return chars;
    }

    private String getString(char[] chars, int start, int length) {
        
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < start + length; ++i) {
            char c = chars[i];
            if (c != 0) {
                sb.append(c);
            }
        }
        String ret = sb.toString().trim();
        return ret;
    }
}
