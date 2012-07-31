package open.dolphin.infomodel;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.Key;
import org.hibernate.search.filter.FilterKey;
import org.hibernate.search.filter.StandardFilterKey;
import org.hibernate.search.filter.impl.CachingWrapperFilter;

/**
 * Hibernate searchにおいてfacilityPkでfilteringするFilterFactory
 *   KarteEntryBean.javaのUserModel creator
 *   UserModel.javaのFacilityModel facility
 * に"@IndexedEmbedded"アノテーションを追加している
 * @author masuda, Masuda Naika
 */

public class FacilityPkFilterFactory {

    private long facilityPk;

    public void setFacilityPk(long facilityPk) {
        this.facilityPk = facilityPk;
    }

    @Key
    public FilterKey getKey() {
        StandardFilterKey key = new StandardFilterKey();
        key.addParameter(facilityPk);
        return key;
    }

    @Factory
    public Filter getFilter() {
        Query query = new TermQuery( new Term("creator.facility.id", String.valueOf(facilityPk)));
        return new CachingWrapperFilter( new QueryWrapperFilter(query) );
    }
}
