package md.leonis.tetris.sound;

/*
 * http://habrahabr.ru/post/191422/
 * 26 августа 2013 в 20:36
 * Пару слов о поддержке форматов звуковых файлов: забудьте про mp3 и вспомните wav. Также поддерживаются au и aif.
 * (C) raid
 *
 * Эта библиотека проигрывает стандартные звуки Java. Актуально для Java SE 7/8
 * 06.03.2015 модификация и доработка
 * (C) Leonis
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.AudioFormat;

class Sound {

    private boolean released;
    private Clip clip = null;
    private FloatControl volumeC = null;
    private boolean playing = false;

    Sound(File file) {
        released = false;
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = stream.getFormat();
            DataLine.Info info = new DataLine.Info(
                    Clip.class,
                    stream.getFormat(),
                    ((int) stream.getFrameLength() * format.getFrameSize()));
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);
            clip.addLineListener(new Listener());
            volumeC = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            released = true;

        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException exc) {
            exc.printStackTrace();
            released = false;
        }
    }

    Sound(InputStream inputStream) {
        released = false;
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(inputStream);
            AudioFormat format = stream.getFormat();
            DataLine.Info info = new DataLine.Info(
                    Clip.class,
                    stream.getFormat(),
                    ((int) stream.getFrameLength() * format.getFrameSize()));
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(stream);
            clip.addLineListener(new Listener());
            volumeC = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            released = true;

        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException exc) {
            exc.printStackTrace();
            released = false;
        }
    }

    //true если звук успешно загружен, false если произошла ошибка
    boolean isReleased() {
        return released;
    }

    //проигрывается ли звук в данный момент
    boolean isPlaying() {
        return playing;
    }

    //Запуск
    /*
      breakOld определяет поведение, если звук уже играется
      Если breakOld==true, о звук будет прерван и запущен заново
      Иначе ничего не произойдёт
    */
    private void play(boolean breakOld) {
        if (released) {
            if (breakOld) {
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
                playing = true;
            } else if (!isPlaying()) {
                clip.setFramePosition(0);
                clip.start();
                playing = true;
            }
        }
    }

    //То же самое, что и play(true)
    void play() {
        play(true);
    }

    //Останавливает воспроизведение
    void stop() {
        if (playing) {
            clip.stop();
        }
    }

    //Установка громкости
    /*
      x долже быть в пределах от 0 до 1 (от самого тихого к самому громкому)
    */
    void setVolume(float x) {
        if (x < 0) {
            x = 0;
        }
        if (x > 1) {
            x = 1;
        }
        float min = volumeC.getMinimum();
        float max = volumeC.getMaximum();
        volumeC.setValue((max - min) * x + min);
    }

    //Возвращает текущую громкость (число от 0 до 1)
    float getVolume() {
        float v = volumeC.getValue();
        float min = volumeC.getMinimum();
        float max = volumeC.getMaximum();
        return (v - min) / (max - min);
    }

    /**
     * Позиционирование звука: по центру (0), слева (-1.0) или справа (1.0)
     * Если данная функция не поддерживается - просто ничего не произойдёт
     */
    void setBalance(float value) {
        if (clip.isControlSupported(FloatControl.Type.PAN)) //необходим стерео звук, ИНАЧЕ НЕ РАБОТАЕТ!!!
            try {
                FloatControl panControl = (FloatControl) clip.getControl(FloatControl.Type.PAN);
                panControl.setValue(value);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
    }

    //Дожидается окончания проигрывания звука
    void join() {
        if (!released) return;
        synchronized (clip) {
            try {
                while (playing) {
                    clip.wait();
                }
            } catch (InterruptedException exc) {
                //TODO
            }
        }
    }

    //Статический метод, для удобства
    static Sound playSound(String s) {
        File f = new File(s);
        Sound snd = new Sound(f);
        snd.play();
        return snd;
    }

    private class Listener implements LineListener {
        public void update(LineEvent ev) {
            if (ev.getType() == LineEvent.Type.STOP) {
                playing = false;
                synchronized (clip) {
                    clip.notify();
                }
            }
        }
    }
}
