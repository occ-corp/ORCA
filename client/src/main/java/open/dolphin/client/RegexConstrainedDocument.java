package open.dolphin.client;

import java.awt.Toolkit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * RegexConstrainedDocument
 * 
 * @author modified by masuda, Masuda Naika
 */
public final class RegexConstrainedDocument extends PlainDocument {

    private boolean beep;
    private Pattern pattern;

    public RegexConstrainedDocument(AbstractDocument.Content c, String p) {
        super(c);
        setPatternByString(p);
    }

    public RegexConstrainedDocument(String p) {
        super();
        setPatternByString(p);
    }

    private void setPatternByString(String p) {
        pattern = Pattern.compile(p);
    }

    @Override
    public void insertString(int offs, String s, AttributeSet a) throws BadLocationException {

        String proposedInsert = getText(0, getLength()) + s;
        Matcher matcher = pattern.matcher(proposedInsert);
        if (!matcher.matches()) {
            beep();
            return;
        }
        super.insertString(offs, s, a);
    }

    private void beep() {
        if (beep) {
            Toolkit.getDefaultToolkit().beep();
        }
    }
}