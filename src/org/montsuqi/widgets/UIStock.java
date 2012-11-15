/*      PANDA -- a simple transaction monitor

Copyright (C) 1998-1999 Ogochan.
              2000-2003 Ogochan & JMA (Japan Medical Association).
              2002-2006 OZAWA Sakuro.

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

package org.montsuqi.widgets;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

/** <p>This class provides mapping from a string name to a set of icon/text/tooltip/accelerator.</p>
 */
public class UIStock {

	private static Map stocks;
	static {
		stocks = new HashMap();
	}

	private static final String BUNDLE_NAME = "org.montsuqi.widgets.uistocks"; //$NON-NLS-1$
	private static final ResourceBundle RESOURCE_BUNDLE =
		ResourceBundle.getBundle(BUNDLE_NAME);

	private String text;
	private String tooltip;
	private Icon icon;
	private KeyStroke accelerator;

	private UIStock(String text, String tooltip, String iconFile, KeyStroke accelerator) {
		assert text != null;
		assert tooltip != null;
		this.text = text;
		this.tooltip = tooltip;
		this.accelerator = accelerator;

		if (iconFile != null) {
			String iconPath = "/org/montsuqi/widgets/images/" + iconFile; //$NON-NLS-1$
			URL url = getClass().getResource(iconPath);
			if (url != null) {
				icon = new ImageIcon(url);
			}
		}

	}

	/** <p>Gets the text resource of this ui scock.</p>
	 * 
	 * @return a text.
	 */
	public String getText() {
		return text;
	}

	/** <p>Gets the tooltip resource of this ui scock.</p>
	 * 
	 * @return a tooltip.
	 */
	public String getToolTip() {
		return tooltip;
	}

	/** <p>Gets the icon resource of this ui scock.</p>
	 * 
	 * @return an icon.
	 */
	public Icon getIcon() {
		return icon;
	}

	/** <p>Gets the accelerator resource of this ui scock.</p>
	 * 
	 * @return an accelerator's key stroke.
	 */
	public KeyStroke getAccelerator() {
		return accelerator;
	}

	/** <p>Returns a ui stock named <var>key</var>.</p>
	 * 
	 * @param key the key to look up the ui stock for.
	 */
	public static UIStock get(String key) {
		if ( ! stocks.containsKey(key)) {
			stocks.put(key, createStock(key));
		}
		return (UIStock)stocks.get(key);
	}

	private static UIStock createStock(String key) {
		String label;
		key = "UIStock." + key; //$NON-NLS-1$
		try {
			label = RESOURCE_BUNDLE.getString(key + ".label"); //$NON-NLS-1$
		} catch (MissingResourceException e) {
			throw new IllegalArgumentException("stock not found"); //$NON-NLS-1$
		}
		String tooltip;
		try {
			tooltip = RESOURCE_BUNDLE.getString(key + ".tooltip"); //$NON-NLS-1$
		} catch (MissingResourceException e) {
			tooltip = ""; //$NON-NLS-1$
		}
		String icon;
		try {
			icon = RESOURCE_BUNDLE.getString(key + ".icon"); //$NON-NLS-1$
		} catch (MissingResourceException e) {
			icon = null;
		}
		KeyStroke accelerator;
		try {
			accelerator = KeyStroke.getKeyStroke(RESOURCE_BUNDLE.getString(key + ".accelerator")); //$NON-NLS-1$
		} catch (MissingResourceException e) {
			accelerator = null;
		}
		return new UIStock(label, tooltip, icon, accelerator);
	}
}