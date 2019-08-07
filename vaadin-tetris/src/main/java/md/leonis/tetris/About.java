package md.leonis.tetris;

import org.vaadin.viritin.components.DisclosurePanel;
import org.vaadin.viritin.label.RichText;

class About extends DisclosurePanel {

    About() {
        setCaption("Server side Crazy Tetris game? Read more »");
        setContent(new RichText().withMarkDownResource("/about.md"));
    }
}
