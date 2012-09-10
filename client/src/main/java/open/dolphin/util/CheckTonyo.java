
package open.dolphin.util;

import java.util.HashSet;

/**
 * 頓用コードのチェック
 *
 * @author masuda, Masuda Naika
 */

public final class CheckTonyo {

    private static final HashSet<Integer> codeSet;

    static {
        codeSet = new HashSet<Integer>();
        codeSet.add(Integer.valueOf("001000101"));	//医師の指示通りに
        codeSet.add(Integer.valueOf("001000114"));	//不眠時に
        codeSet.add(Integer.valueOf("001000115"));	//不安時に
        codeSet.add(Integer.valueOf("001000116"));	//検査用薬医師の指示通りに
        codeSet.add(Integer.valueOf("001000117"));	//検査用薬　　月　　日朝　時　分に
        codeSet.add(Integer.valueOf("001000118"));	//検査用薬　　月　　日夜　時　分に
        codeSet.add(Integer.valueOf("001000119"));	//検査用薬　　月　　日朝来院前に
        codeSet.add(Integer.valueOf("001000120"));	//検査用薬　　月　　日の検査用
        codeSet.add(Integer.valueOf("001000121"));	//イライラ時に
        codeSet.add(Integer.valueOf("001000122"));	//痛む時に
        codeSet.add(Integer.valueOf("001000123"));	//頭痛時に
        codeSet.add(Integer.valueOf("001000124"));	//腹痛時に
        codeSet.add(Integer.valueOf("001000125"));	//発作時にお使い下さい
        codeSet.add(Integer.valueOf("001000128"));	//発作時に
        codeSet.add(Integer.valueOf("001000129"));	//けいれん時に
        codeSet.add(Integer.valueOf("001000130"));	//便秘時に
        codeSet.add(Integer.valueOf("001000131"));	//下痢時に
        codeSet.add(Integer.valueOf("001000132"));	//嘔気時に
        codeSet.add(Integer.valueOf("001000133"));	//発熱時に
        codeSet.add(Integer.valueOf("001000134"));	//３８．５℃以上の発熱時に
        codeSet.add(Integer.valueOf("001000137"));	//発熱時又は痛む時に
        codeSet.add(Integer.valueOf("001000145"));	//１回１個発熱時又は痛む時に
        codeSet.add(Integer.valueOf("001000147"));	//１回１／３個発熱時又は痛む時に
        codeSet.add(Integer.valueOf("001000148"));	//１回２／３個発熱時又は痛む時に
        codeSet.add(Integer.valueOf("001000149"));	//１回　　個発熱時又は痛む時に
        codeSet.add(Integer.valueOf("001000150"));	//１日１回お使い下さい。
        codeSet.add(Integer.valueOf("001000151"));	//頭痛時，３８．５℃以上の発熱時
        codeSet.add(Integer.valueOf("001000983"));	//胸痛時に
    }

    public static boolean isTonyo(String code) {
        return codeSet.contains(Integer.valueOf(code));
    }
}
