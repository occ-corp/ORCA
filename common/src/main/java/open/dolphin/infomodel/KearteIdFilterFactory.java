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
 * Hibernate searchにおいてkarteIdでfilteringするFilterFactory
 * なんかよくわからんけどｗ
 *   KarteEntryBean.javaのKarteBean karte
 * に"@IndexedEmbedded"アノテーションを追加している
 *
 * @author masuda, Masuda Naika
 */

public class KearteIdFilterFactory {

    private long karteId;

    public void setKarteId(long karteId) {
        this.karteId = karteId;
    }

    @Key
    public FilterKey getKey() {
        StandardFilterKey key = new StandardFilterKey();
        key.addParameter(karteId);
        return key;
    }

    @Factory
    public Filter getFilter() {
        Query query = new TermQuery( new Term("karte.id", String.valueOf(karteId)));
        return new CachingWrapperFilter( new QueryWrapperFilter(query) );
    }
}
