/*      PANDA -- a simple transaction monitor
                                                                                
Copyright (C) 1998-1999 Ogochan.
			  2000-2003 Ogochan & JMA (Japan Medical Association).
                                                                                
This module is part of PANDA.
                                                                                
		PANDA is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY.  No author or distributor accepts responsibility
to anyone for the consequences of using it or for whether it serves
any particular purpose or works at all, unless he says so in writing.
Refer to the GNU General Public License for full details.
                                                                                
		Everyone is granted permission to copy, modify and redistribute
PANDA, but only under the conditions described in the GNU General
Public License.  A copy of this license is supposed to have been given
to you along with PANDA so you can know your rights and
responsibilities.  It should be in a file named COPYING.  Among other
things, the copyright notice and this notice must be preserved on all
copies.
*/

package org.montsuqi.monsia.builders;

import java.awt.Container;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.montsuqi.monsia.ChildInfo;
import org.montsuqi.monsia.Interface;
import org.montsuqi.monsia.Property;
import org.montsuqi.monsia.WidgetInfo;

class CListBuilder extends ContainerBuilder {
	void buildChildren(Interface xml, Container parent, WidgetInfo info) {
		int cCount = info.getChildrenCount();

		String[] columnNames = new String[cCount];
		for (int i = 0; i < cCount; i++) {
			columnNames[i] = "?"; //$NON-NLS-1$
			ChildInfo cInfo = info.getChild(i);
			WidgetInfo wInfo = cInfo.getWidgetInfo();
			if ( ! "Label".equals(wInfo.getClassName())) { //$NON-NLS-1$
				continue;
			}
			int pCount = wInfo.getPropertiesCount();
			for (int j = 0; j < pCount; j++) {
				Property p = wInfo.getProperty(j);
				if ("label".equals(p.getName())) { //$NON-NLS-1$
					columnNames[i] = p.getValue();
					break;
				}
			}
		}
		JTable table = (JTable)parent;
		DefaultTableModel tableModel = new DefaultTableModel(0, cCount) {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		tableModel.setColumnIdentifiers(columnNames);
		table.setModel(tableModel);

		TableColumnModel columnModel = table.getColumnModel();
		for (int i = 0; i < cCount; i++) {
			ChildInfo cInfo = info.getChild(i);
			WidgetInfo wInfo = cInfo.getWidgetInfo();
			TableColumn column = columnModel.getColumn(i);
			JLabel dummy = new JLabel((String)column.getHeaderValue());
			xml.setLongName(wInfo.getLongName(), dummy);
		}
	}
}
