package open.dolphin.dao;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import open.dolphin.client.ClientContext;

/**
 * 簡易的に管理テーブルのコピー句を読んでみる。手抜きｗ
 * @author masuda, Masuda Naika
 */
public class IncReader {
    
    private static final int COLUMN_COUNT = 5;
    private static final String CAMMA = ",";
    private static final String COMMENT_MARK = "*";
    private static final String TABLE = "TBL";
    private static final String[] IGNORES = new String[]{"GF"};
    private static final String ENCODING = "UTF-8";
    
    private String kanricd;
    private String orcaVer;
    
    public IncReader(String kanricd, String orcaVer) {
        this.kanricd = kanricd;
        this.orcaVer = orcaVer.toLowerCase();
    }
    
    public Map<String, int[]> getMap() throws UnsupportedEncodingException, IOException {
        
        StringBuilder sb = new StringBuilder();
        sb.append("SYS-").append(kanricd).append("-");
        String prefix = sb.toString();
        
        sb = new StringBuilder();
        sb.append("inc/").append(orcaVer).append("/");
        sb.append("CPSK").append(kanricd).append(".csv");
        String incResource = sb.toString();
        
        InputStream is=  ClientContext.getResourceAsStream(incResource);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, ENCODING));
        
        String line;
        String kanriTblLevel = null;
        Map<String, Integer> nameNumMap = new HashMap<String, Integer>();
        Map<String, int[]> ret = new HashMap<String, int[]>();
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
            String name = datum[2].replace(prefix, "");
            String type = datum[3];
            String length = datum[4];
            
            // SYS-XXXX-TBL行を探す
            if (kanriTblLevel == null) {
                if (name.equals(TABLE)) {
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

            // 位置とデータ長を記録する。複数回出現するものは番号をつける
            String keyName = (num == 1)
                    ? name
                    : name + "-" + String.valueOf(num);
            int len = Integer.valueOf(length);
            ret.put(keyName, new int[]{pos, len});
            pos += len;
        }
        return ret;
    }
    
}
