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

package org.montsuqi.client;

import java.awt.Component;
import java.awt.Window;
import java.util.HashMap;
import java.util.Map;
import org.montsuqi.monsia.Interface;

class Node {

	private Interface xml;
	private String name;
	private Map changedWidgets;

	Node(Interface xml, String name) {
		this.xml = xml;
		this.name = name;
		this.changedWidgets = new HashMap();
	}

	String getName() {
		return name;
	}

	Interface getInterface() {
		return xml;
	}

	Window getWindow() {
		return (Window)xml.getWidget(name);
	}

	void clearChangedWidgets() {
		changedWidgets.clear();
	}

	Map getChangedWidgets() {
		return changedWidgets;
	}

	void addChangedWidget(String widgetName, Component widget) {
		if ( ! changedWidgets.containsKey(widgetName)) {
			changedWidgets.put(widgetName, widget);
		}
	}
}
