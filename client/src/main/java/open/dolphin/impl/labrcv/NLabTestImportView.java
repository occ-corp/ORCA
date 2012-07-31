
package open.dolphin.impl.labrcv;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.*;

/**
 * NLabTestImportView改
 * 
 * @author masuda
 */
public class NLabTestImportView extends JPanel {
    
    private JButton addBtn;
    private JButton clearBtn;
    private JLabel countLbl;
    private JButton fileBtn;
    private JTable table;
    private static final Font lblFont = new Font("Lucida Grande", 0, 12);

    public NLabTestImportView(){
        
        fileBtn = new JButton("検査結果ファイル選択");
        fileBtn.setToolTipText("ラボから送られてきた検査結果ファイルを選択します。");
        JLabel arrow = new JLabel("->");
        addBtn = new JButton("登録");
        addBtn.setToolTipText("テーブルに表示されている検査結果をデータベースへ登録します。");
        clearBtn = new JButton("クリア");
        clearBtn.setToolTipText("登録後はクリアしてください。");
        countLbl = new JLabel("0件");
        countLbl.setFont(lblFont);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));
        north.add(fileBtn);
        north.add(arrow);
        north.add(addBtn);
        north.add(clearBtn);
        north.add(Box.createHorizontalGlue());
        north.add(countLbl);

        table = new JTable();
        table.setToolTipText("検査結果をこのテーブルに Drag & Drop することもできます。");
        JScrollPane scroll = new JScrollPane(table);

        this.setLayout(new BorderLayout());
        this.add(north, BorderLayout.NORTH);
        this.add(scroll, BorderLayout.CENTER);
    }

    /**
     * @return the countLbl
     */
    public JLabel getCountLbl() {
        return countLbl;
    }

    /**
     * @return the table
     */
    public JTable getTable() {
        return table;
    }

    /**
     * @return the addBtn
     */
    public JButton getAddBtn() {
        return addBtn;
    }

    /**
     * @return the fileBtn
     */
    public JButton getFileBtn() {
        return fileBtn;
    }

    /**
     * @return the clearBtn
     */
    public JButton getClearBtn() {
        return clearBtn;
    }
}
