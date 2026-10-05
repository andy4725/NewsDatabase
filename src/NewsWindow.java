import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class NewsWindow extends JFrame {
    public NewsWindow() {
        super("News Database");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 450);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());
        JLabel message = new JLabel("Select a news article", JLabel.CENTER);
        panel.add(message, BorderLayout.WEST);

        add(panel);
    }
}