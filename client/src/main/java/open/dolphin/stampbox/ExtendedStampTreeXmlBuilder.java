
package open.dolphin.stampbox;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import open.dolphin.client.ClientContext;
import open.dolphin.delegater.StampDelegater;
import open.dolphin.infomodel.ModuleInfoBean;
import open.dolphin.infomodel.StampModel;
import open.dolphin.project.Project;
import open.dolphin.util.HexBytesTool;
import open.dolphin.util.XmlUtils;
import org.apache.log4j.Logger;

/**
 * stampBytesも含めたStampTreeXmlBuilder
 *
 * based on DefaultStampTreeXmlBuilder.java
 * @author masuda, Masuda Naika
 */

public class ExtendedStampTreeXmlBuilder {

    /** Control staffs */
    private LinkedList<StampTreeNode> linkedList;
    private BufferedWriter writer;
    private StringWriter stringWriter;
    private StampTreeNode rootNode;
    private Logger logger;

    // Creates new ExtendedStampTreeXmlBuilder
    public ExtendedStampTreeXmlBuilder() {
        logger = ClientContext.getBootLogger();
    }

    /**
     * Returns the product of this builder
     * @return vector that contains StampTree instances
     */
    public String getProduct() {
        String result = stringWriter.toString();
        if (logger != null) {
            logger.debug(result);
        }
        return result;
    }

    /**
     * Return the product of this builder
     * @return StampTree XML data
     */
    public void buildStart() throws IOException {
        if (logger != null) {
            logger.debug("StampTree Build start");
        }
        stringWriter = new StringWriter();
        writer = new BufferedWriter(stringWriter);
        writer.write(makeComment());
        writer.write("<extendedStampTree project=");
        writer.write(addQuote("open.dolphin"));
        writer.write(" version=");
        writer.write(addQuote("1.0"));
        writer.write(">\n");
    }

    private String makeComment() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!-- StampBox Export Data, Creator: ");
        sb.append(Project.getUserModel().getFacilityModel().getFacilityName());
        sb.append(", Created on: ");
        sb.append(new Date().toString());
        sb.append(" -->\n");
        return sb.toString();
    }

    public void buildRoot(StampTreeNode root) throws IOException {
        if (logger != null) {
            logger.debug("Build Root Node: " + root.toString());
        }
        rootNode = root;
        TreeInfo treeInfo = (TreeInfo)rootNode.getUserObject();
        writer.write("<root name=");
        writer.write(addQuote(treeInfo.getName()));
        writer.write(" entity=");
        writer.write(addQuote(treeInfo.getEntity()));
        writer.write(">\n");
        linkedList = new LinkedList<StampTreeNode>();
        linkedList.addFirst(rootNode);
    }

    public void buildNode(StampTreeNode node) throws Exception {

        if ( node.isLeaf() ) {
            buildLeafNode(node);
        } else {
            buildDirectoryNode(node);
        }
    }

    private void buildDirectoryNode(StampTreeNode node) throws IOException {

        // 子ノードを持たないディレクトリノードは書き出さない
        if (node.getChildCount() != 0) {

            if (logger != null) {
                logger.debug("Build Directory Node: " + node.toString());
            }

            StampTreeNode myParent = (StampTreeNode) node.getParent();
            StampTreeNode curNode = getCurrentNode();

            if (myParent != curNode) {
                closeBeforeMyParent(myParent);
            }
            linkedList.addFirst(node);

            writer.write("<node name=");
            // 特殊文字を変換する
            String val = toXmlText(node.toString());
            writer.write(addQuote(val));
            writer.write(">\n");
        }
    }

    private void buildLeafNode(StampTreeNode node) throws IOException, Exception {

        if (logger != null) {
            logger.debug("Build Leaf Node: " + node.toString());
        }

        StampTreeNode myParent = (StampTreeNode) node.getParent();
        StampTreeNode curNode = getCurrentNode();
        
        ModuleInfoBean info = (ModuleInfoBean) node.getUserObject();
        String stampId = info.getStampId();
        
        // ここで対応するstampBytesをデータベースから読み込む。
        String stampHexBytes = getHexStampBytes(stampId);
        // 実体のないスタンプの場合があった。なぜゾンビができたのだろう？？
        if (stampId != null && stampHexBytes == null) {
            //System.out.println("ゾンビ:" + stampId);
            node.removeFromParent();
            return;
        }
        
        if (myParent != curNode) {
            closeBeforeMyParent(myParent);
        }

        // 特殊文字を変換する
        writer.write("<stampInfo name=");
        String val = toXmlText(node.toString());
        writer.write(addQuote(val));

        writer.write(" role=");
        writer.write(addQuote(info.getStampRole()));

        writer.write(" entity=");
        writer.write(addQuote(info.getEntity()));

        writer.write(" editable=");
        val = String.valueOf(info.isEditable());
        writer.write(addQuote(val));

        val = info.getStampMemo();
        if (val != null) {
            writer.write(" memo=");
            val = toXmlText(val);
            writer.write(addQuote(val));
        }

        if (info.isSerialized()) {
            val = info.getStampId();
            writer.write(" stampId=");
            writer.write(addQuote(val));
            // stampBytesを書き出す
            writer.write(" stampBytes=");
            writer.write(addQuote(stampHexBytes));
        }

        writer.write("/>\n");
    }

    // StampIdから対応するStampModelを取得してstampBytesのHex文字列を作成する
    private String getHexStampBytes(String stampId) throws Exception{

        StampDelegater del = StampDelegater.getInstance();
        // スタンプの実体を取得
        StampModel model = del.getStamp(stampId);
        // データベースにない場合はnullを返す
        if (model == null){
            return null;
        }
        // stampBytesを返す
        byte[] stampBytes = model.getStampBytes();
        return HexBytesTool.bytesToHex(stampBytes);
    }

    public void buildRootEnd() throws IOException {

        if (logger != null) {
            logger.debug("Build Root End");
        }
        closeBeforeMyParent(rootNode);
        writer.write("</root>\n");
    }

    public void buildEnd() throws IOException {
        if (logger != null) {
            logger.debug("Build end");
        }
        writer.write("</extendedStampTree>\n");
        writer.flush();
    }

    private StampTreeNode getCurrentNode() {
        return linkedList.getFirst();
    }

    private void closeBeforeMyParent(StampTreeNode parent) throws IOException {

        int index = linkedList.indexOf(parent);

        if (logger != null) {
            logger.debug("Close before my parent: " + index);
        }
        for (int j = 0; j < index; j++) {
            writer.write("</node>\n");
            linkedList.removeFirst();
        }
    }

    private String addQuote(String s) {
        StringBuilder buf = new StringBuilder();
        buf.append("\"");
        buf.append(s);
        buf.append("\"");
        return buf.toString();
    }

    /**
     * 特殊文字を変換する。
     */
    private String toXmlText(String text) {
        return XmlUtils.toXml(text);
    }

}
