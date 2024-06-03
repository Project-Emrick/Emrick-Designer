package org.emrick.project;

import org.emrick.project.effect.RFTrigger;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

public class FlowViewGUI extends JPanel {
    private ArrayList<FlowViewItem> items;
    private RFSignalListener rfSignalListener;
    public FlowViewGUI(HashMap<Integer, RFTrigger> count2RFTrigger, RFSignalListener rfSignalListener) {
        super();
        this.setLayout(new GridBagLayout());
        this.rfSignalListener = rfSignalListener;
        Iterator<RFTrigger> iterator = count2RFTrigger.values().iterator();
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
        GridBagConstraints gc = new GridBagConstraints();
        Border innerBorder = BorderFactory.createTitledBorder("Flow Viewer");
        Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        this.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));

        Insets spacedInsets = new Insets(0, 0, 0, 20);
        Insets noSpacedInsets = new Insets(0, 0, 0, 0);

        gc.weightx = 1;
        gc.weighty = 0.2;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = noSpacedInsets;
        this.add(new JLabel("Index"), gc);

        gc.weightx = 1;
        gc.weighty = 0.2;

        gc.gridx = 1;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = noSpacedInsets;
        this.add(new JLabel("Count"), gc);

        gc.weightx = 1;
        gc.weighty = 0.2;

        gc.gridx = 2;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = noSpacedInsets;
        this.add(new JLabel("Title"), gc);

        gc.weightx = 1;
        gc.weighty = 0.2;

        gc.gridx = 3;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = noSpacedInsets;
        this.add(new JLabel("Description"), gc);

        gc.weightx = 1;
        gc.weighty = 0.2;

        gc.gridx = 4;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = noSpacedInsets;
        this.add(new JLabel("Cue"), gc);

        gc.weightx = 1;
        gc.weighty = 0.2;

        gc.gridx = 5;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = noSpacedInsets;
        this.add(new JLabel("Execute"), gc);

        for (i = 0; i < items.size(); i++) {
            gc.weightx = 1;
            gc.weighty = 0.2;

            gc.gridx = 0;
            gc.gridy = i+1;
            gc.fill = GridBagConstraints.BOTH;
            gc.anchor = GridBagConstraints.WEST;
            gc.insets = noSpacedInsets;
            this.add(items.get(i).indexLabel, gc);

            gc.weightx = 1;
            gc.weighty = 0.2;

            gc.gridx = 1;
            gc.gridy = i+1;
            gc.fill = GridBagConstraints.BOTH;
            gc.anchor = GridBagConstraints.WEST;
            gc.insets = noSpacedInsets;
            this.add(items.get(i).countLabel, gc);

            gc.weightx = 1;
            gc.weighty = 0.2;

            gc.gridx = 2;
            gc.gridy = i+1;
            gc.fill = GridBagConstraints.BOTH;
            gc.anchor = GridBagConstraints.WEST;
            gc.insets = noSpacedInsets;
            this.add(items.get(i).titleLabel, gc);

            gc.weightx = 1;
            gc.weighty = 0.2;

            gc.gridx = 3;
            gc.gridy = i+1;
            gc.fill = GridBagConstraints.BOTH;
            gc.anchor = GridBagConstraints.WEST;
            gc.insets = noSpacedInsets;
            this.add(items.get(i).descriptionLabel, gc);

            gc.weightx = 1;
            gc.weighty = 0.2;

            gc.gridx = 4;
            gc.gridy = i+1;
            gc.fill = GridBagConstraints.BOTH;
            gc.anchor = GridBagConstraints.WEST;
            gc.insets = noSpacedInsets;
            this.add(items.get(i).cueLabel, gc);

            gc.weightx = 1;
            gc.weighty = 0.2;

            gc.gridx = 5;
            gc.gridy = i+1;
            gc.fill = GridBagConstraints.BOTH;
            gc.anchor = GridBagConstraints.WEST;
            gc.insets = noSpacedInsets;
            this.add(items.get(i).getExecuteButton(), gc);

        }
    }


    // For testing
    public static void main(String[] args) {
        HashMap<Integer, RFTrigger> count2RFTrigger = new HashMap<>();
        count2RFTrigger.put(0, new RFTrigger(0,0));
        count2RFTrigger.put(1, new RFTrigger(32, 0));
        count2RFTrigger.put(2, new RFTrigger(16,0));
        count2RFTrigger.put(3, new RFTrigger(24, 0));
        FlowViewGUI flowViewGUI = new FlowViewGUI(count2RFTrigger, null);
        for (FlowViewItem fvi : flowViewGUI.items) {
            System.out.println(fvi.getCount());
        }
        JFrame jf = new JFrame();
        jf.setSize(500,500);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.add(flowViewGUI);
        jf.setVisible(true);
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
