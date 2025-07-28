package org.example;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

// TODO: bug: type character, very quickly after that keystroke, press a non-char like Ctrl -> filter doesn't update
//  commenting all of textfield.addKeyListener(new KeyAdapter() { EXCEPT for the filtering fixes it. Why? Is
//  everything in it EXCEPT the filtering needed? --> needed for Ctrl+A to work

// TODO: ability to set an image
class FilterComboBox extends JComboBox {
    private List<String> array;
    private Supplier<String> filterTextSupplier;

    public FilterComboBox(List<String> array) {
        super(array.toArray());
        this.array = array;
        this.setEditable(true);
        setEditor(new CustomComboBoxEditor());
        final JTextField textfield = (JTextField) this.getEditor().getEditorComponent();
        JTextComponent textComponent = (JTextComponent) textfield;
        textfield.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent ke) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        int keyCode = ke.getKeyCode();
                        if (keyCode == KeyEvent.VK_SHIFT ||
                                keyCode == KeyEvent.VK_CONTROL ||
                                keyCode == KeyEvent.VK_ESCAPE ||
                                keyCode == KeyEvent.VK_ALT ||
                                (keyCode >= KeyEvent.VK_LEFT && keyCode <= KeyEvent.VK_DOWN)) {
                            // This is a non-input or control key
                            // Ignore this keystroke
                            return;
                        }
                        if (ke.isControlDown() && keyCode != KeyEvent.VK_Z) {
                            return;
                        }
                        comboFilter(textfield.getText());
                    }
                });
            }
        });

        filterTextSupplier = textfield::getText;

        textfield.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateStatus(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateStatus(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Not typically used for text content changes
            }

            private void updateStatus(DocumentEvent e) {
                try {
                    String text = e.getDocument().getText(0, e.getDocument().getLength());
                    System.out.println("Text changed to: " + text);
                } catch (javax.swing.text.BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });

        textComponent.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // Show the dropdown when the JComboBox gains focus
                ((JComboBox)((JComponent) e.getSource()).getParent()).showPopup();
                comboFilter(textfield.getText());
            }
        });

        AbstractAction selectAllAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.selectAll();
            }
        };

        // change notification
        addItemListener(new ItemChangeListener());
        addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                // Not sure why we need this to work; appears to be broken as a result of the CustomComboBoxEditor we're using
                textfield.setText(e.getItem().toString());
            }
        });

        addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                //System.out.println("Popup menu will become visible.");
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                //System.out.println("Selected item when hidden: ");
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                //System.out.println("Popup menu was canceled.");
            }
        });

        setRenderer(new CustomComboRenderer(filterTextSupplier));
    }

    public void comboFilter(String enteredText) {
        if (!this.isPopupVisible()) {
            this.showPopup();
        }

        List<String> filterArray= new ArrayList<String>();
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).toLowerCase().contains(enteredText.toLowerCase())) {
                filterArray.add(array.get(i));
            }
        }

        DefaultComboBoxModel model = (DefaultComboBoxModel) this.getModel();
        if (filterArray.size() > 0) {
            model.removeAllElements();
            // https://stackoverflow.com/questions/13856471/how-i-stop-triggering-swing-jcombo-box-item-listener-while-adding-item-to-combo
            for (int i = 0; i < filterArray.size(); i++) {
                insertItemAt(filterArray.get(i), i);
            }
//            for (String s: filterArray)
//                model.addElement(s);

            JTextField textfield = (JTextField) this.getEditor().getEditorComponent();
            textfield.setText(enteredText);
        } else {
            String currentText = (String) getEditor().getItem();
            model.removeAllElements();
            getEditor().setItem(currentText); // restore the text that removeAllElements clears
        }

        setMaximumRowCount(model.getSize());
    }

    /* Testing Codes */
    public static List<String> populateArray() {
        List<String> test = new ArrayList<String>();
        test.add("Mountain Flight");
        test.add("Mount Climbing");
        test.add("Trekking");
        test.add("Rafting");
        test.add("Jungle Safari");
        test.add("Bungie Jumping");
        test.add("Para Gliding");
        return test;
    }

    public static void makeUI() {
        JFrame frame = new JFrame("Adventure in Nepal - Combo Filter Test");
        FilterComboBox acb = new FilterComboBox(populateArray());
        frame.setSize(new Dimension(600, 300));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().add(acb);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        makeUI();
    }

    /**
     * We only want item change events fired from manual clicks
     */
    class ItemChangeListener implements ItemListener{
        @Override
        public void itemStateChanged(ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                Object item = event.getItem();
                System.out.println("Selected Item: " + item.toString());
            }
        }
    }

    public class HintComboBoxRenderer extends DefaultListCellRenderer {
        private String hintText;

        public HintComboBoxRenderer(String hintText) {
            this.hintText = hintText;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (index == -1 && value == null) { // Condition for the selected item in the combobox field
                setText(hintText);
                setForeground(Color.GRAY); // Optional: make hint text appear faded
            } else if (value == null) { // Condition for items in the dropdown list that might be null
                setText(hintText);
                setForeground(Color.GRAY);
            } else {
                setText(value.toString()); // Display the actual item
                setForeground(list.getForeground());
            }
            return this;
        }
    }

//    public class PlaceholderComboBoxRenderer extends DefaultListCellRenderer {
//        private String placeholderText;
//
//        public PlaceholderComboBoxRenderer(String placeholderText) {
//            this.placeholderText = placeholderText;
//        }
//
//        @Override
//        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
//            // Call the superclass method to get the default component
//            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
//
//            // If no item is selected (value is null) and it's the display area (index == -1)
//            if (value == null && index == -1) {
//                setText(placeholderText);
//                // Optionally, set a different color for the placeholder
//                setForeground(Color.GRAY);
//            } else {
//                // Restore default foreground color for actual items
//                setForeground(list.getForeground());
//            }
//            return c;
//        }
//    }

    static class CustomComboBoxEditor extends BasicComboBoxEditor {
        private CustomTextField customTextField;

        public CustomComboBoxEditor() {
            customTextField = new CustomTextField();
        }

        @Override
        public Component getEditorComponent() {
            return customTextField;
        }
    }

    static class CustomTextField extends JTextField {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Call the superclass method first
            if (getText().isBlank()) {
                Graphics2D graphics2d = (Graphics2D) g;
                graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2d.setColor(getDisabledTextColor());
                graphics2d.drawString("Search...", getInsets().left, g.getFontMetrics().getMaxAscent() + getInsets().top);
            }
        }
    }

    static class HtmlHighlighter {
        private static final String HighlightTemplate = "<span style='color:yellow;'>$1</span>";

        public static String highlightText(String text, String textToHighlight) {
            if (textToHighlight.isBlank()) {
                return text;
            }

            try {
                text = text.replaceAll("(?i)(" + Pattern.quote(textToHighlight) + ")", HighlightTemplate);
            } catch (Exception e) {
                return text;
            }
            return "<html>" + text + "</html>";
        }
    }

    static class CustomComboRenderer extends DefaultListCellRenderer {
        private final Supplier<String> highlightTextSupplier;

        public CustomComboRenderer(Supplier<String> highlightTextSupplier) {
            this.highlightTextSupplier = highlightTextSupplier;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value == null)
                return this;

            setText(HtmlHighlighter.highlightText(value.toString(), highlightTextSupplier.get()));
            return this;
        }
    }
}