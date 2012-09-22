package open.dolphin.session;

/**
 * IServiceBean
 * @author masuda, Masuda Naika
 */
public interface IServiceBean {
    
    public static final String QUERY_INSURANCE_BY_PATIENT_PK 
            = "from HealthInsuranceModel h where h.patient.id=:pk";
    
    public static final String PK = "pk";
    public static final String UID = "uid";
    public static final String FID = "fid";
    public static final String PID = "pid";
    public static final String ID = "id";
    public static final String ENTITY = "entity";
    public static final String KARTE_ID = "karteId";
    public static final String FROM_DATE = "fromDate";
    public static final String TO_DATE = "toDate";
    public static final String DATE = "date";
    public static final String PATIENT_PK = "patientPk";

}
