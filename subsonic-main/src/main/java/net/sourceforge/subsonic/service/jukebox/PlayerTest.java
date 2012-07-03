package net.sourceforge.subsonic.service.jukebox;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class PlayerTest implements AudioPlayer.Listener {

    private AudioPlayer player;

    public PlayerTest() throws Exception {
        player = new AudioPlayer(new FileInputStream("i:\\tmp\\foo.au"), this);
        createGUI();
    }

    private void createGUI() {
        JFrame frame = new JFrame();

        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        JButton resetButton = new JButton("Reset");
        final JSlider gainSlider = new JSlider(0, 1000);

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                player.play();
            }
        });
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                player.pause();
            }
        });
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                player.close();
            }
        });
        gainSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                float gain = (float) gainSlider.getValue() / 1000.0F;
                player.setGain(gain);
            }
        });

        frame.setLayout(new FlowLayout());
        frame.add(startButton);
        frame.add(stopButton);
        frame.add(resetButton);
        frame.add(gainSlider);

        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        new PlayerTest();
    }

    public void stateChanged(AudioPlayer player, AudioPlayer.State state) {
        System.out.println(state);
    }
}

