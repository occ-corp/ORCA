package open.dolphin.ra.api;

/**
 * IPvtClaimListener
 * jarを分けておかないとJBAS014521でハマるｗｗｗ
 * @author masuda, Masuda Naika
 */
public interface IPvtClaimListener {
    
    public void onPvt(String pvtXml);
    
}
