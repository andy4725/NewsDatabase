package com.umfst.endre.news;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class Loading extends JPanel {

    // Text shown next to the progress bar while feeds are loading.
    private final JLabel loadingLabel;

    // Displays loading progress from 0 to 100.
    private final JProgressBar progressBar;

    public Loading() {
        // Arrange the label and progress bar in a centered row.
        super(new FlowLayout(FlowLayout.CENTER));

        loadingLabel = new JLabel("Loading...");
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(150, 20));

        // Add both components to this panel.
        add(loadingLabel);
        add(progressBar);

        // Keep them hidden until loading starts.
        loadingLabel.setVisible(false);
        progressBar.setVisible(false);

        // Give the panel a preferred size in its parent layout.
        setPreferredSize(new Dimension(300, 25));
    }

    // Show the loading components and reset progress to zero.
    public void showLoading() {
        progressBar.setValue(0);
        loadingLabel.setVisible(true);
        progressBar.setVisible(true);
        refreshPanel();
    }

    // Update the progress bar. The expected value is between 0 and 100.
    public void setLoadingProgress(int value) {
        progressBar.setValue(value);
    }

    // Hide the loading components when loading is finished.
    public void hideLoading() {
        loadingLabel.setVisible(false);
        progressBar.setVisible(false);
        refreshPanel();
    }

    // Ask Swing to recalculate the layout and redraw this panel.
    private void refreshPanel() {
        revalidate();
        repaint();
    }
}