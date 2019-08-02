package md.leonis.tetris;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

abstract class Monitor implements ActionListener, KeyListener {
    abstract public void actionPerformed(ActionEvent e);

    abstract public void keyPressed(KeyEvent e);

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }
}
