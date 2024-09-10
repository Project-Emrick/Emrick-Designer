package org.emrick.project;

import org.emrick.project.effect.RFTrigger;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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

    public FlowViewGUI(HashMap<Integer, RFTrigger> count2RFTrigger, RFSignalListener rfSignalListener) {
        this.rfSignalListener = rfSignalListener;
        Iterator<RFTrigger> iterator = count2RFTrigger.values().iterator();
        currentTrigger = 0;
        this.scrollablePanel = new JPanel();
        scrollablePanel.setBackground(new Color(0, 0, 0, 0));
        this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS)); //new layout

        this.addKeyListener(new KeyListener() { //When spacebar is hit, move to next trigger
            @Override
            public void keyTyped(KeyEvent e) {
                System.out.println(++currentTrigger);
                rfSignalListener.onRFSignal(currentTrigger);
                System.out.println("Current Trigger: " + currentTrigger);
            }

            @Override
            public void keyPressed(KeyEvent e){}
            @Override
            public void keyReleased(KeyEvent e) {}
        });
        items = new ArrayList<>();
        int i = 0;
        while(iterator.hasNext()) {
            RFTrigger curr = iterator.next();
            items.add(new FlowViewItem(i, curr.getCount(), "", "", ""));
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

    public void initializeFlowViewPanel(){
        scrollablePanel.setLayout(new BoxLayout(scrollablePanel, BoxLayout.Y_AXIS));
        Border innerBorder = BorderFactory.createTitledBorder("Flow View");
        Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        scrollablePanel.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
        for (int i = 0; i< items.size(); i++){
            JPanel FV_panel = new JPanel();
            FV_panel.setLayout(new BoxLayout(FV_panel, BoxLayout.X_AXIS));
            FV_panel.setPreferredSize(new Dimension(10000, 60));
            FV_panel.setMaximumSize(new Dimension(10000, 60));
            FV_panel.setOpaque(false);

            FlowViewItem fv = items.get(i);
            JLabel flowviewLabel = new JLabel("Trigger: "+ fv.getCount());
            FV_panel.add(flowviewLabel);
            FV_panel.add(fv.getTitleLabel());
            FV_panel.add(fv.getExecuteButton());
            FV_panel.add(fv.getCountLabel());
            FV_panel.add(fv.getIndexLabel());
            scrollablePanel.add(FV_panel);
            System.out.println("BIGHUGECAPSYOUCANTMISSME AL LALALALALALALA LA LAL LALA ");
        }
        scrollPane = new JScrollPane(scrollablePanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setBackground(new Color(0, 0, 0, 0));
        scrollPane.setVisible(true);
        this.add(scrollPane);
        this.setVisible(true);
    }

    private class FlowViewItem {
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

        public FlowViewItem(int index, int count, String title, String description, String cue) {
            this.index = index;
            this.count = count;
            this.title = wrap(title);
            this.description = wrap(description);
            this.cue = wrap(cue);
            this.executeButton = new JButton();
            ImageIcon i = new ImageIcon(ScrubBarGUI.PATH_PLAY_ICON);
            executeButton.setIcon(new ImageIcon(i.getImage().getScaledInstance(16,16, Image.SCALE_SMOOTH)));
            executeButton.addActionListener(e -> {
                currentTrigger = index;
                System.out.println(index);
                rfSignalListener.onRFSignal(index);
            });
        }

        private String wrap(String s) {
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
            indexLabel = new JLabel(Integer.toString(index+1));
            countLabel = new JLabel(Integer.toString(count));
            titleLabel = new JLabel(title);
            descriptionLabel = new JLabel(description);
            cueLabel = new JLabel(cue);
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
            executeButton.setIcon(new ImageIcon(i.getImage().getScaledInstance(16,16, Image.SCALE_SMOOTH)));
            executeButton.addActionListener(e -> {
                rfSignalListener.onRFSignal(index);
            });
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
