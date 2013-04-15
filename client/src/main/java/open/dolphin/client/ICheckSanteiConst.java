package open.dolphin.client;

/**
 * CheckSanteiで使う定数群
 * 
 * @author masuda, Masuda Naika
 */
public interface ICheckSanteiConst {
    
    public static final String[][] SANTEI_MORE_CHECK_DATA = {
        {"尿検査：", "160000310", "尿検査算定していますか？"},
        {"血糖：", "160019410", "グルコース算定していますか？"},
        {"ＵＣＧ：", "160072510", "ＵＣＧ算定していますか？"},
        {"ＵＳ：", "160072210", "ＵＳ算定していますか？"},
        {"ＥＣＧ：", "160068410", "ＥＣＧ算定していますか？"},
        {"ＸＰ：", "170027910", "ＸＰ算定していますか？"},};
    
    public static final String[] TM_KANRI_SRYCD = {
        "113001310",    // 悪性腫瘍特異物質治療管理料（その他・１項目）
        "113002110",    // 悪性腫瘍特異物質治療管理料（その他・２項目以上）
        "113001210"     // 悪性腫瘍特異物質治療管理料（尿中ＢＴＡ）
    };

    public final static String srycdFrmtStr = "000000000";
    
    // ClaimItemのsrycd
    public static final int srycd_Saishin =                  112007410; //再診
    public static final int srycd_Saishin_Dummy =             99120001; //再診DUMMY
    public static final int srycd_Gairai_Riha1 =             113013910; //外来リハビリテーション診療料１
    public static final int srycd_Gairai_Riha2 =             113014010; //外来リハビリテーション診療料２
    public static final int srycd_Saishin_Denwa =            112007950; //電話等再診
    public static final int srycd_Saishin_Doujitsu =         112008350; //同日再診
    public static final int srycd_Saishin_Doujitsu_Denwa =   112008850; //同日電話等再診
    public static final int srycd_Saishin_Jikangai  =        112001110; //時間外（再診）
    public static final int srycd_Saishin_Kyujitsu =         112001210; //休日（再診）
    public static final int srycd_Saishin_Shinya =           112001310; //深夜（再診）
    public static final int srycd_Shoshin =                  111000110; //初診
    public static final int srycd_Shoshin_Jikangai_Kasan =   111000570; //初診（時間外）加算
    public static final int srycd_Shoshin_Kyujitsu_Kasan =   111000670; //初診（休日）加算
    public static final int srycd_Shoshin_Shinya_Kasan =     111000770; //初診（深夜）加算
    public static final int srycd_Gairaikanri_Kasan =        112011010; //外来管理加算
    public static final int srycd_Tokutei_Ryouyou  =         113001810; //特定疾患療養管理料（診療所）
    public static final int srycd_Tokutei_Shohou =           120002270; //特定疾患処方管理加算（処方料）
    public static final int srycd_Tokutei_Shohou_Shohousen = 120002570; //特定疾患処方管理加算（処方せん料）
    public static final int srycd_Chouki_Shohou =            120003170; //長期投薬加算（処方料）
    public static final int srycd_Chouki_Shohousen =         120003270; //長期投薬加算（処方せん料）
    public static final int srycd_Yakuzaijouhou =            120002370; //薬剤情報提供料
    public static final int srycd_Techoukisai =              113701310; //手帳記載加算（薬剤情報提供料）
    public static final int srycd_JikangaiTaikouKasan1 =     112016070; // 時間外対応加算１
    public static final int srycd_JikangaiTaikouKasan2 =     112015670; // 地域医療貢献加算 -> 時間外対応加算２
    public static final int srycd_JikangaiTaikouKasan3 =     112016170; // 時間外対応加算３
    public static final int srycd_GenericName_Kasan =        120003570; // 一般名処方加算（処方せん料）
    
    // 往診
    public static final int srycd_Oushin =                   114000110; //往診
    public static final int srycd_Oushin_Kinkyu_Kasan1 =     114000370; //緊急往診加算（在支診等以外）
    public static final int srycd_Oushin_Yakan_Kasan1 =      114000470; //夜間往診加算（在支診等以外）
    public static final int srycd_Oushin_Shinya_Kasan1 =     114000570; //深夜往診加算（在支診等以外）
    public static final int srycd_Oushin_Kinkyu_Kasan2 =     114011570; //緊急往診加算（在支診等）
    public static final int srycd_Oushin_Yakan_Kasan2 =      114011670; //夜間往診加算（在支診等）
    public static final int srycd_Oushin_Shinya_Kasan2 =     114011770; //深夜往診加算（在支診等）
    public static final int srycd_Oushin_Kinkyu_Kasan3 =     114017470; //緊急往診加算（機能強化した在支診等）（病床あり）
    public static final int srycd_Oushin_Yakan_Kasan3 =      114017570; //夜間往診加算（機能強化した在支診等）（病床あり）
    public static final int srycd_Oushin_Shinya_Kasan3 =     114017670; //深夜往診加算（機能強化した在支診等）（病床あり）
    public static final int srycd_Oushin_Kinkyu_Kasan4 =     114017770; //緊急往診加算（機能強化した在支診等）（病床なし）
    public static final int srycd_Oushin_Yakan_Kasan4 =      114017870; //夜間往診加算（機能強化した在支診等）（病床なし）
    public static final int srycd_Oushin_Shinya_Kasan4 =     114017970; //深夜往診加算（機能強化した在支診等）（病床なし）
    public static final int srycd_Oushin_ShinryoJikan_Kasan = 114000970; //往診（診療時間）加算
    public static final int srycd_Tokubetu_Oushin =          114001610; //特別往診
    public static final int srycd_TOushin_Kinkyu_Kasan1 =    114001870; //緊急特別往診加算（在支診等以外）
    public static final int srycd_TOushin_Yakan_Kasan1 =     114001970; //夜間特別往診加算（在支診等以外）
    public static final int srycd_TOushin_Shinya_Kasan1 =    114002070; //深夜特別往診加算（在支診等以外）
    public static final int srycd_TOushin_Kinkyu_Kasan2 =    114011870; //緊急特別往診加算（在支診等）
    public static final int srycd_TOushin_Yakan_Kasan2 =     114011970; //夜間特別往診加算（在支診等）
    public static final int srycd_TOushin_Shinya_Kasan2 =    114012070; //深夜特別往診加算（在支診等）
    public static final int srycd_TOushin_Kinkyu_Kasan3 =    114022370; //緊急特別往診加算（機能強化した在支診等）（病床あり）
    public static final int srycd_TOushin_Yakan_Kasan3 =     114022470; //夜間特別往診加算（機能強化した在支診等）（病床あり）
    public static final int srycd_TOushin_Shinya_Kasan3 =    114022570; //深夜特別往診加算（機能強化した在支診等）（病床あり）
    public static final int srycd_TOushin_Kinkyu_Kasan4 =    114022670; //緊急特別往診加算（機能強化した在支診等）（病床なし）
    public static final int srycd_TOushin_Yakan_Kasan4 =     114022770; //夜間特別往診加算（機能強化した在支診等）（病床なし）
    public static final int srycd_TOushin_Shinya_Kasan4 =    114022870; //深夜特別往診加算（機能強化した在支診等）（病床なし）
    public static final int srycd_TOushin_ShinryoJikan_Kasan = 114002470; //特別往診（診療時間）加算
    
    // 在医総管・特医総管
    // 1: 在支診等以外
    // 2: 在支診等
    // 3: 機能強化した在支診等、病床あり
    // 4: 機能強化した在支診等、病床なし
    public static final int srycd_ZaiiSoukanEx1 =    114007510; // 在医総管（在支診等以外）（処方せんあり）
    public static final int srycd_ZaiiSoukanIn1 =    114012410; // 在医総管（在支診等以外）（処方せんなし）
    public static final int srycd_ZaiiSoukanEx2 =    114012210; // 在医総管（在支診等）（処方せんあり）
    public static final int srycd_ZaiiSoukanIn2 =    114012310; // 在医総管（在支診等）（処方せんなし）
    public static final int srycd_ZaiiSoukanEx3 =    114018710; // 在医総管（機能強化した在支診等）（病床あり）（処方せんあり）
    public static final int srycd_ZaiiSoukanIn3 =    114018810; // 在医総管（機能強化した在支診等）（病床あり）（処方せんなし）
    public static final int srycd_ZaiiSoukanEx4 =    114018910; // 在医総管（機能強化した在支診等）（病床なし）（処方せんあり）
    public static final int srycd_ZaiiSoukanIn4 =    114019010; // 在医総管（機能強化した在支診等）（病床なし）（処方せんなし）
    public static final int srycd_TokuiSoukanEx1 =   114013210; // 特医総管（在支診等以外）（処方せんあり）
    public static final int srycd_TokuiSoukanIn1 =   114013310; // 特医総管（在支診等以外）（処方せんなし）
    public static final int srycd_TokuiSoukanEx2 =   114013010; // 特医総管（在支診等）（処方せんあり）
    public static final int srycd_TokuiSoukanIn2 =   114013110; // 特医総管（在支診等）（処方せんなし）
    public static final int srycd_TokuiSoukanEx3 =   114019110; // 特医総管（機能強化した在支診等）（病床あり）（処方せんあり）
    public static final int srycd_TokuiSoukanIn3 =   114019210; // 特医総管（機能強化した在支診等）（病床あり）（処方せんなし）
    public static final int srycd_TokuiSoukanEx4 =   114019310; // 特医総管（機能強化した在支診等）（病床なし）（処方せんあり）
    public static final int srycd_TokuiSoukanIn4 =   114019410; // 特医総管（機能強化した在支診等）（病床なし）（処方せんなし）

    // 在宅患者訪問診療料
    public static final int srycd_HoumonShinsatsu_hidouitsu         = 114001110; // 在宅患者訪問診療料（同一建物居住者以外）
    public static final int srycd_HoumonShinsatsu_douitsu_hitokutei = 114012910; // 在宅患者訪問診療料（同一建物居住者）（特定施設等以外入居者）
    public static final int srycd_HoumonShinsatsu_douitsu_tokutei   = 114018010; // 在宅患者訪問診療料（同一建物居住者）（特定施設等入居者）
    
    public static final int srycd_ZaitakuSoukiKasan      = 114016070; // 在宅移行早期加算
    public static final int srycd_JuushoushaKasan        = 114012570; // 重症者加算
    public static final int srycd_HoumounShinsatsuJikan  = 114001470; // 在宅患者訪問診療料（診療時間）加算
    public static final int srycd_Oufuku_Jikan_2gou      = 114002970; // 往診往復時間加算（２号地域）
    
    // 施設基準
    public enum Shienshin {
        NON_SHIENSHIN, SHIENSHIN,
        KYOKA_TANDOKU_WITH_BED, KYOKA_RENKEI_WITH_BED,
        KYOKA_TANDOKU_WO_BED, KYOKA_RENKEI_WO_BED}
    
    // 時間外対応加算
    public enum JikangaiTaiou {
        J_TAIOU_NON, J_TAIOU1, J_TAIOU2, J_TAIOU3
    }
    
    public static final int sk1006_gairaiRiha          = 3052;  // 3052:外来リハビリテーション診察料
    public static final int sk1006_chiikiKoken         = 754;   // 0754:地域医療貢献加算
    public static final int sk1006_jikangaiTaiou1      = 3001;  // 3001:時間外対応加算１
    public static final int sk1006_jikangaiTaiou2      = 3155;  // 3155:時間外対応加算２
    public static final int sk1006_jikangaiTaiou3      = 3002;  // 3002:時間外対応加算３
    public static final int sk1006_zaitakuShien1       = 3055;  // 3055:在宅療養支援診療所（１）   <-機能強化、単独
    public static final int sk1006_zaitakuShien2       = 3056;  // 3056:在宅療養支援診療所（２）   <-機能強化、連携
    public static final int sk1006_zaitakuShien3       = 3168;  // 3168:在宅療養支援診療所（３）   <-機能普通の支援診？
    public static final int sk1006_zaitakuShien3old    = 613;   // 0613:在宅療養支援診療所 -> 3168
    public static final int sk1006_zaitakuShienHsp1    = 3057;  // 3057:在宅療養支援病院（１）     <-機能強化、単独
    public static final int sk1006_zaitakuShienHsp2    = 3058;  // 3058:在宅療養支援病院（２）     <-機能強化、連携
    public static final int sk1006_zaitakuShienHsp3    = 3169;  // 3058:在宅療養支援病院（３）     <-機能普通の支援病？
    public static final int sk1006_zaitakuShienHsp3old = 695 ;  // 0695:在宅療養支援病院 -> 3169
    
    public static final int sk1006_zaiiSoukan          = 721;    // 0721:在宅時医学総合管理料及び特定施設入居時等医学総合管理料
    
    public static final int[] srycd_ZaitakuKanri = {
        srycd_ZaiiSoukanEx1,
        srycd_ZaiiSoukanIn1,
        srycd_ZaiiSoukanEx2,
        srycd_ZaiiSoukanIn2,
        srycd_ZaiiSoukanEx3,
        srycd_ZaiiSoukanIn3,
        srycd_ZaiiSoukanEx4,
        srycd_ZaiiSoukanIn4,
        srycd_TokuiSoukanEx1,
        srycd_TokuiSoukanIn1,
        srycd_TokuiSoukanEx2,
        srycd_TokuiSoukanIn2,
        srycd_TokuiSoukanEx3,
        srycd_TokuiSoukanIn3,
        srycd_TokuiSoukanEx4,
        srycd_TokuiSoukanIn4,
    };

    // 特定疾患などの最大数／月
    public static final int defMaxTokuRyouyou = 2;
    public static final int defMaxTokuShohou = 2;
    public static final int defMaxChouki = 1;
    public static final int defMaxNoTokuRyouCount = 1;
    public static final int defChoukiDay = 28;

    // http://www.mhlw.go.jp/bunya/iryouhoken/iryouhoken12/tensuhyo.html
    public static final int[] srycd_ExclusiveTokuteiRyouyou = {
        113000310,  //ウイルス疾患指導料１
        113000810,  //小児特定疾患カウンセリング料（１回目）
        113000910,  //皮膚科特定疾患指導管理料（１）
        113001510,  //心臓ペースメーカー指導管理料（遠隔モニタリング）
        113001610,  //心臓ペースメーカー指導管理料（イ以外）
        113002210,  //小児科療養指導料
        113002310,  //皮膚科特定疾患指導管理料（２）
        113002850,  //てんかん指導料
        113002910,  //難病外来指導管理料
        113003210,  //ウイルス疾患指導料２
        113006510,  //慢性疼痛疾患管理料
        113006610,  //小児悪性腫瘍患者指導管理料
        113009910,  //小児特定疾患カウンセリング料（２回目）
        113010110,  //耳鼻咽喉科特定疾患指導管理料
        113012210,  //認知症専門診断管理料
        114003510,  //在宅自己腹膜灌流指導管理料
        114003610,  //在宅自己連続携行式腹膜灌流頻回指導管理料
        114003710,  //在宅酸素療法指導管理料（その他）
        114004110,  //在宅酸素療法指導管理料（チアノーゼ型先天性心疾患）
        114004210,  //在宅中心静脈栄養法指導管理料
        114004310,  //在宅成分栄養経管栄養法指導管理料
        114004410,  //在宅自己導尿指導管理料
        114005410,  //在宅人工呼吸指導管理料
        114005610,  //在宅悪性腫瘍患者指導管理料
        114005810,  //在宅寝たきり患者処置指導管理料
        114007010,  //在宅自己疼痛管理指導管理料
        114007310,  //退院前在宅療養指導管理料
        114009210,  //在宅自己注射指導管理料
        114009310,  //在宅血液透析指導管理料
        114009410,  //在宅血液透析頻回指導管理料
        114009710,  //在宅持続陽圧呼吸療法指導管理料
        114010410,  //在宅肺高血圧症患者指導管理料
        114011110,  //在宅気管切開患者指導管理料
        114017110,  //在宅小児低血糖症患者指導管理料
        114017210,  //在宅難治性皮膚疾患処置指導管理料
        180007250,  //家族通院・在宅精神療法（３０分以上）
        180012210,  //通院・在宅精神療法（３０分以上）
        180012410,  //心身医学療法（入院）
        180020010,  //心身医学療法（入院外）（再診時）
        180020410,  //通院・在宅精神療法（初診時精神保健指定医等）
        180020610,  //心身医学療法（入院外）（初診時）
        180031010,  //通院・在宅精神療法（３０分未満）
        180031210,  //家族通院・在宅精神療法（３０分未満）
    };
}
