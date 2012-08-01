
package open.dolphin.infomodel;

import java.util.Date;

/**
 * 転送用のAppointmentModel
 * @author masuda, Masuda Naika
 */
public class AppointmentTransferModel extends AbstractKarteEntryTransferModel {
    
    private String patientId;
    private int state;
    private String name;
    private String memo;
    private Date date;

    @Override
    public AppointmentModel getKarteEntryBean() {
        AppointmentModel model = new AppointmentModel();
        restore(model);
        model.setPatientId(patientId);
        model.setState(state);
        model.setName(name);
        model.setMemo(memo);
        model.setDate(date);
        return model;
    }

    @Override
    public void setKarteEntryBean(KarteEntryBean karteEntryBean) {
        store(karteEntryBean);
        AppointmentModel model = (AppointmentModel) karteEntryBean;
        patientId = model.getPatientId();
        state = model.getState();
        name = model.getName();
        memo = model.getMemo();
        date = model.getDate();
    }
    
}
