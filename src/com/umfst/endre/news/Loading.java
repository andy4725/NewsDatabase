package com.umfst.endre.news;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class Loading extends JPanel {
    private final JLabel loadingLabel;
    private final JProgressBar progressBar;

    public Loading() {
        super(new FlowLayout(FlowLayout.CENTER));

        loadingLabel = new JLabel("Loading...");
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(150, 20));

        add(loadingLabel);
        add(progressBar);

        loadingLabel.setVisible(false);
        progressBar.setVisible(false);
        setPreferredSize(new Dimension(300, 25));
    }

    public void showLoading() {
        progressBar.setValue(0);
        loadingLabel.setVisible(true);
        progressBar.setVisible(true);
        refreshPanel();
    }

    public void setLoadingProgress(int value) {
        progressBar.setValue(value);
    }

    public void hideLoading() {
        loadingLabel.setVisible(false);
        progressBar.setVisible(false);
        refreshPanel();
    }

    private void refreshPanel() {
        revalidate();
        repaint();
    }
}