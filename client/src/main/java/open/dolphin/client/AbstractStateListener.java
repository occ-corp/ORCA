package open.dolphin.client;

import java.util.List;
import open.dolphin.infomodel.StateMsgModel;

/**
 * AbstractStateListener
 * @author masuda, Masuda Naika
 */
public abstract class AbstractStateListener {
    
    protected String clientUUID;
    
    public AbstractStateListener() {
        clientUUID = Dolphin.getInstance().getClientUUID();
    }
    
    // サーバーからの状態通知を処理する
    public final void stateChanged(List<StateMsgModel> msgList) {
        
        if (msgList == null || msgList.isEmpty()) {
            return;
        }
        for (StateMsgModel msg : msgList) {
            // 自クライアントは更新済みなのでそれ以外のもののみ処理する
            if (!clientUUID.equals(msg.getIssuerUUID())) {
                processStateChange(msg);
            }
        }
        // 更新後の処理
        postStateChange();
    }
    
    // 変更処理
    protected abstract void processStateChange(StateMsgModel msg);
    
    // 更新後の処理
    protected abstract void postStateChange(); 
}
