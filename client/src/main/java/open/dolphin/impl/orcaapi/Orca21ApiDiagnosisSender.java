package open.dolphin.impl.orcaapi;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Date;
import java.util.List;
import open.dolphin.client.Chart;
import open.dolphin.client.IDiagnosisSender;
import open.dolphin.client.KarteSenderResult;
import open.dolphin.infomodel.RegisteredDiagnosisModel;
import open.dolphin.project.Project;

/**
 * Orca21ApiDiagnosisSender
 * 
 * @author masuda, Masuda Naika
 */
public class Orca21ApiDiagnosisSender implements IDiagnosisSender {

    private static final String ORCA_API = "ORCA API";
    private Chart context;
    private List<RegisteredDiagnosisModel> rdList;
    private PropertyChangeSupport boundSupport;
    
    @Override
    public Chart getContext() {
        return context;
    }

    @Override
    public void setContext(Chart context) {
        this.context = context;
    }
    
    @Override
    public void setModel(List<RegisteredDiagnosisModel> rdList) {
        this.rdList = rdList;
    }
    
    @Override
    public void addListener(PropertyChangeListener listener) {
        if (boundSupport == null) {
            boundSupport = new PropertyChangeSupport(this);
        }
        boundSupport.addPropertyChangeListener(KarteSenderResult.PROP_DIAG_SENDER_RESULT, listener);
    }
    
    @Override
    public void removeListeners() {
        if (boundSupport != null) {
            for (PropertyChangeListener listener : boundSupport.getPropertyChangeListeners()) {
                boundSupport.removePropertyChangeListener(listener);
            }
        }
    }

    @Override
    public void fireResult(KarteSenderResult result) {
        if (boundSupport != null) {
            boundSupport.firePropertyChange(KarteSenderResult.PROP_DIAG_SENDER_RESULT, null, result);
        }
    }
    
    @Override
    public void send() {
        
        if (rdList == null 
                || rdList.isEmpty() 
                || context == null) {
            fireResult(new KarteSenderResult(ORCA_API, KarteSenderResult.SKIPPED, null, this));
            return;
        }
        
        // ORCA API使用しない場合はリターン
        if (!Project.getBoolean(Project.USE_ORCA_API)) {
            fireResult(new KarteSenderResult(ORCA_API, KarteSenderResult.SKIPPED, null, this));
            return;
        }
        
        MedicalModModel modModel = new MedicalModModel();
        modModel.setContext(context);
        modModel.setDepartmentCode(context.getPatientVisit().getDeptCode());
        modModel.setPhysicianCode(Project.getString(Project.ORCA_STAFF_CODE));
        modModel.setPerformDate(new Date());
        modModel.setDiagnosisList(rdList);
        
        KarteSenderResult result = OrcaApiDelegater.getInstance().sendMedicalModModel(modModel);
        result.setDiagnosisSender(this);
        fireResult(result);
    }
    
}
