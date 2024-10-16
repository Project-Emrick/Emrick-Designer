package org.emrick.project;

import org.emrick.project.effect.RFTrigger;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

public class FlowViewGUI extends JPanel {
    private ArrayList<FlowViewItem> items;
    private RFSignalListener rfSignalListener;
    private JScrollPane scrollPane;
    private JPanel scrollablePanel;
    private ArrayList<JPanel> flowViewPanels;
    int currentTrigger;
    private JPanel headerPanel;
    private JLabel indexLabel = new JLabel("Index");
    private JLabel countLabel = new JLabel("Count");
    private JLabel titleLabel = new JLabel("Title");
    private JLabel descriptionLabel = new JLabel("Description");
    private JLabel cueLabel = new JLabel("Cue");
    private JLabel executeLabel = new JLabel("Execute");

    public FlowViewGUI(HashMap<Integer, RFTrigger> count2RFTrigger, RFSignalListener rfSignalListener) {
        this.rfSignalListener = rfSignalListener;
        Iterator<RFTrigger> iterator = count2RFTrigger.values().iterator();
        currentTrigger = 0;
        this.scrollablePanel = new JPanel();
        scrollablePanel.setBackground(new Color(0, 0, 0, 0));
        this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS)); //new layout
        this.setFocusable(true);
        this.addPropertyChangeListener(e -> {
            if (this.isShowing()) {
                this.requestFocusInWindow();
            }
        });

        this.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ') { //When spacebar is hit, move to next trigger
                    rfSignalListener.onRFSignal(currentTrigger % items.size());
                    System.out.println("Current Trigger: " + currentTrigger % items.size());
                    currentTrigger++;

                    setCurrentTriggerVisible();
                }
            }

            @Override
            public void keyPressed(KeyEvent e){}
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) { //When down is pressed, move highlight to next trigger but don't execute
                    currentTrigger++;

                    setCurrentTriggerVisible();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) { //When down is pressed, move highlight to previous trigger but don't execute
                    currentTrigger--;
                    if (currentTrigger < 0) {
                        currentTrigger += items.size();
                    }

                    setCurrentTriggerVisible();
                }
            }
        });
        items = new ArrayList<>();
        int i = 0;
        while(iterator.hasNext()) {
            RFTrigger curr = iterator.next();
            items.add(new FlowViewItem(i, curr.getCount(), curr.getTitle(), curr.getDescription(), curr.getCue()));
            i++;
        }
        items.sort(Comparator.comparingInt(FlowViewItem::getCount));
        i = 0;
        for (FlowViewItem fvi : items) {
            fvi.setIndex(i);
            fvi.generateLabels();
            i++;
        }
        initializeFlowViewPanel();
    }

    public void setCurrentTriggerVisible() {
        Rectangle r = items.get(currentTrigger % items.size()).getVisibleRect();
        if (!r.getSize().equals(items.get(currentTrigger % items.size()).getSize())) {
            scrollPane.getVerticalScrollBar().setValue(items.get(currentTrigger % items.size()).getLocation().y - 50);
        }
        reinitializeFlowViewPanel();
    }

    public void initializeFlowViewPanel(){
        scrollablePanel.setLayout(new BoxLayout(scrollablePanel, BoxLayout.Y_AXIS));
        Border innerBorder = BorderFactory.createTitledBorder("Flow View");
        Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        scrollablePanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        headerPanel.setPreferredSize(new Dimension(800, 50));
        headerPanel.setMaximumSize(new Dimension(800, 50));
        headerPanel.setMinimumSize(new Dimension(800, 50));
        indexLabel.setMinimumSize(new Dimension(133, 30));
        indexLabel.setPreferredSize(new Dimension(133, 30));
        indexLabel.setMaximumSize(new Dimension(133, 30));
        countLabel.setMinimumSize(new Dimension(133, 30));
        countLabel.setPreferredSize(new Dimension(133, 30));
        countLabel.setMaximumSize(new Dimension(133, 30));
        titleLabel.setMinimumSize(new Dimension(133, 30));
        titleLabel.setPreferredSize(new Dimension(133, 30));
        titleLabel.setMaximumSize(new Dimension(133, 30));
        descriptionLabel.setMinimumSize(new Dimension(133, 30));
        descriptionLabel.setPreferredSize(new Dimension(133, 30));
        descriptionLabel.setMaximumSize(new Dimension(133, 30));
        cueLabel.setMinimumSize(new Dimension(133, 30));
        cueLabel.setPreferredSize(new Dimension(133, 30));
        cueLabel.setMaximumSize(new Dimension(133, 30));
        executeLabel.setMinimumSize(new Dimension(133, 30));
        executeLabel.setPreferredSize(new Dimension(133, 30));
        executeLabel.setMaximumSize(new Dimension(133, 30));
        headerPanel.add(indexLabel);
        headerPanel.add(countLabel);
        headerPanel.add(titleLabel);
        headerPanel.add(descriptionLabel);
        headerPanel.add(cueLabel);
        headerPanel.add(executeLabel);
        scrollablePanel.add(headerPanel);
        for (int i = 0; i< items.size(); i++){
            scrollablePanel.add(Box.createVerticalStrut(5));
            scrollablePanel.add(items.get(i));
        }
        scrollPane = new JScrollPane(scrollablePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            reinitializeFlowViewPanel();
        });
        scrollPane.setBackground(new Color(0, 0, 0, 0));
        scrollPane.setVisible(true);
        this.add(scrollPane);
        this.setVisible(true);
    }

    public void reinitializeFlowViewPanel() {
        scrollablePanel.removeAll();
        scrollablePanel.add(headerPanel);
        for (FlowViewItem fvi : items) {
            fvi.generateLabels();
            scrollablePanel.add(fvi);
        }
        scrollPane.revalidate();
        scrollPane.repaint();
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    private class FlowViewItem extends JPanel{
        private int index;
        private int count;
        private String title;
        private String description;
        private String cue;
        private JLabel indexLabel;
        private JLabel countLabel;
        private JLabel titleLabel;
        private JLabel descriptionLabel;
        private JLabel cueLabel;
        private JButton executeButton;
        private ActionListener executeListener;

        public FlowViewItem(int index, int count, String title, String description, String cue) {
            this.index = index;
            this.count = count;
            this.title = title;
            this.description = description;
            this.cue = cue;
            this.executeListener = initializeExecuteListener();
            this.executeButton = new JButton();
            ImageIcon i = new ImageIcon(ScrubBarGUI.PATH_PLAY_ICON);
            executeButton.setIcon(new ImageIcon(i.getImage().getScaledInstance(16,16, Image.SCALE_SMOOTH)));
            executeButton.addActionListener(executeListener);
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            this.setPreferredSize(new Dimension(800, 50));
            this.setMaximumSize(new Dimension(800, 50));
            this.setMinimumSize(new Dimension(800, 50));
            this.setBackground(new Color(0xE8E8E8));
            this.setOpaque(true);
        }

        public static String wrap(String s) {
            if (s == null) {
                s = "";
            }
            for (int i = 0; i < s.length() / 30; i += 30) {
                while (i < s.length() && s.charAt(i) != ' ') {
                    i++;
                }
                if (i < 40) {
                    s = s.substring(0, i) + "<br>" + s.substring(i+1);
                } else {
                    i = 40;
                    s = s.substring(0,i+1) + "-<br>" + s.substring(i);
                }
            }
            return "<html>" + s + "</html>";
        }

        public void generateLabels() {
            this.removeAll();
            indexLabel = new JLabel(wrap(Integer.toString(index+1)));
            indexLabel.setMinimumSize(new Dimension(133, 45));
            indexLabel.setPreferredSize(new Dimension(133, 45));
            indexLabel.setMaximumSize(new Dimension(133, 45));
            countLabel = new JLabel(wrap(Integer.toString(count)));
            countLabel.setMinimumSize(new Dimension(133, 45));
            countLabel.setPreferredSize(new Dimension(133, 45));
            countLabel.setMaximumSize(new Dimension(133, 45));
            titleLabel = new JLabel(wrap(title));
            titleLabel.setMinimumSize(new Dimension(133, 45));
            titleLabel.setPreferredSize(new Dimension(133, 45));
            titleLabel.setMaximumSize(new Dimension(133, 45));
            descriptionLabel = new JLabel(wrap(description));
            descriptionLabel.setMinimumSize(new Dimension(133, 45));
            descriptionLabel.setPreferredSize(new Dimension(133, 45));
            descriptionLabel.setMaximumSize(new Dimension(133, 45));
            cueLabel = new JLabel(wrap(cue));
            cueLabel.setMinimumSize(new Dimension(133, 45));
            cueLabel.setPreferredSize(new Dimension(133, 45));
            cueLabel.setMaximumSize(new Dimension(133, 45));
            this.add(indexLabel);
            this.add(countLabel);
            this.add(titleLabel);
            this.add(descriptionLabel);
            this.add(cueLabel);
            executeButton.setPreferredSize(new Dimension(40, 40));
            executeButton.setMaximumSize(new Dimension(80, 40));
            executeButton.setMinimumSize(new Dimension(40, 40));
            executeButton.requestFocusInWindow();
            this.add(executeButton);
            if ((currentTrigger) % items.size() == index) {
                this.setBackground(new Color(0xDBE6FF));
            } else {
                this.setBackground(new Color(0xE8E8E8));
            }
        }

        public ActionListener initializeExecuteListener() {
            return e -> {
                currentTrigger = index+1;
                System.out.println(index);
                rfSignalListener.onRFSignal(index);
                setCurrentTriggerVisible();
            };
        }

        public JButton getExecuteButton() {
            return executeButton;
        }

        public void setExecuteButton(JButton executeButton) {
            this.executeButton = executeButton;
        }

        public JLabel getIndexLabel() {
            return indexLabel;
        }

        public void setIndexLabel(JLabel indexLabel) {
            this.indexLabel = indexLabel;
        }

        public JLabel getCountLabel() {
            return countLabel;
        }

        public void setCountLabel(JLabel countLabel) {
            this.countLabel = countLabel;
        }

        public JLabel getTitleLabel() {
            return titleLabel;
        }

        public void setTitleLabel(JLabel titleLabel) {
            this.titleLabel = titleLabel;
        }

        public JLabel getDescriptionLabel() {
            return descriptionLabel;
        }

        public void setDescriptionLabel(JLabel descriptionLabel) {
            this.descriptionLabel = descriptionLabel;
        }

        public JLabel getCueLabel() {
            return cueLabel;
        }

        public void setCueLabel(JLabel cueLabel) {
            this.cueLabel = cueLabel;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
            executeButton = new JButton();
            ImageIcon i = new ImageIcon(ScrubBarGUI.PATH_PLAY_ICON);
            executeListener = initializeExecuteListener();
            executeButton.setIcon(new ImageIcon(i.getImage().getScaledInstance(16,16, Image.SCALE_SMOOTH)));
            executeButton.addActionListener(executeListener);
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCue() {
            return cue;
        }

        public void setCue(String cue) {
            this.cue = cue;
        }

    }
}
