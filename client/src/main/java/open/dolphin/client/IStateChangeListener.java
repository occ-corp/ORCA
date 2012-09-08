package open.dolphin.client;

import open.dolphin.infomodel.StateMsgModel;

/**
 * IStateChangeListener
 * @author masuda, Masuda Naika
 */
public interface IStateChangeListener {

    // 変更処理
    public void onMessage(StateMsgModel msg);
}
