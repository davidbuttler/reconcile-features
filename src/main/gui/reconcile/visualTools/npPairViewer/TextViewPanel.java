/**
 * TextViewPanel.java Aug 3, 2007
 * 
 * @author ves
 */

package reconcile.visualTools.npPairViewer;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * this panel shows the raw text associated with the annotations
 */

public class TextViewPanel
    extends JPanel {

/**
   * 
   */
private static final long serialVersionUID = 1L;
JTextPane textPane;
SimpleAttributeSet highlighter;
SimpleAttributeSet bkgrdHighlighter;
SimpleAttributeSet blankHighlighter;
StyledDocument textContainer;

public TextViewPanel() {
  super();
  this.setLayout(new BorderLayout());
  textPane = new JTextPane();

  highlighter = new SimpleAttributeSet();
  StyleConstants.setBold(highlighter, true);
  StyleConstants.setUnderline(highlighter, true);
  StyleConstants.setForeground(highlighter, Color.red);

  bkgrdHighlighter = new SimpleAttributeSet();
  blankHighlighter = new SimpleAttributeSet();

  textContainer = new DefaultStyledDocument();
  textPane.setEditable(false);

  add(new JScrollPane(textPane));
}

public void clearPanel()
{
  try {
    System.out.println("Length of document: " + textContainer.getLength());
    System.out.println("End offset: " + textContainer.getEndPosition().getOffset());
    if (textContainer.getLength() > 0) {
      textContainer.remove(0, textContainer.getLength());
    }
  }
  catch (BadLocationException e) {
    System.out.println("BadLocationException 1");
  }
}

public void setInitialText(String s)
{
  try {
    textContainer.insertString(0, s, null);
    textPane.setDocument(textContainer);
  }
  catch (BadLocationException e) {
    System.out.println("BadLocationException 2");
  }

  // System.out.println("Initial Text is : " + s);
}

public void clearHighlights()
{
  textContainer.setCharacterAttributes(0, textContainer.getEndPosition().getOffset(), blankHighlighter, true);

  // System.out.println("Clearing highlights, Endoffset: " + textContainer.getEndPosition().getOffset());
}

// this highlights the appropriate areas in the text according to the byte offsets provided
// by start and end
public void highlightSpan(int start, int end, Color highlightTextColor)
{
  /*System.out.println("Highlighting text span: " + start + " , " + end);
  System.out.println("Start position: " + textContainer.getStartPosition().getOffset());
  System.out.println("End position: " + textContainer.getEndPosition().getOffset());*/
  StyleConstants.setForeground(highlighter, highlightTextColor);
  textContainer.setCharacterAttributes(start, end, highlighter, true);
  textPane.setCaretPosition(start);
}

// this changes the background the appropriate areas in the text according to the byte offsets provided
// by start and end.
public void highlightBackground(int start, int end, Color highlightTextColor)
{
  StyleConstants.setBackground(bkgrdHighlighter, highlightTextColor);
  textContainer.setCharacterAttributes(start, end, bkgrdHighlighter, true);
  textPane.setCaretPosition(start);
}

public JTextPane getTextPane()
{
  return textPane;
}

}
