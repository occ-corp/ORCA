package open.dolphin.session;

import java.util.Collection;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import open.dolphin.infomodel.HealthInsuranceModel;
import open.dolphin.infomodel.PatientModel;

/**
 * AbstractServiceBean
 * @author masuda, Masuda Naika
 */
@Stateless
public class AbstractServiceBean {
    
    private static final String QUERY_INSURANCE_BY_PATIENT_PK 
            = "from HealthInsuranceModel h where h.patient.id=:pk";
    
    protected static final String PK = "pk";
    protected static final String UID = "uid";
    protected static final String FID = "fid";
    protected static final String PID = "pid";
    protected static final String ID = "id";
    protected static final String ENTITY = "entity";
    protected static final String KARTE_ID = "karteId";
    protected static final String FROM_DATE = "fromDate";
    protected static final String TO_DATE = "toDate";
    protected static final String DATE = "date";
    protected static final String PATIENT_PK = "patientPk";
    
    @PersistenceContext
    private EntityManager em;
    

    protected void setHealthInsurances(Collection<PatientModel> list) {
        if (list != null && !list.isEmpty()) {
            for (PatientModel pm : list) {
                setHealthInsurances(pm);
            }
        }
    }
    
    protected void setHealthInsurances(PatientModel pm) {
        if (pm != null) {
            List<HealthInsuranceModel> ins = getHealthInsurances(pm.getId());
            pm.setHealthInsurances(ins);
        }
    }

    protected List<HealthInsuranceModel> getHealthInsurances(long pk) {
        
        List<HealthInsuranceModel> ins =
                em.createQuery(QUERY_INSURANCE_BY_PATIENT_PK)
                .setParameter(PK, pk)
                .getResultList();
        return ins;
    }
}
