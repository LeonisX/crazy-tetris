Crazy Tetris for Vaadin
===========

* [Russian Version](README_RU.md)

This example shows that `Vaadin` is not only suitable for creating modern enterprise websites,
but also to create games that do not require high FPS, for example, `Tetris`.
In this example, the frame rate is 10, and it can be raised to 30, noting only a slight slowdown in gameplay.

* Internalization is performed in manual mode, fortunately, there are few controls.
* The engine freely supports multiple audio channels.
* The rounded edges of the squares overloaded the processor, so they were simplified.
* In order not to complicate the task, the records are stored in memory.

As an engine, a remake of a little-known game of the same name for DOS was used.

![Screenshot](../doc/vaadin.png "Screenshot")

Control
-------
* Four `arrow keys`, `space bar`, `P` for pause, `L` for language, `S` for sound. `F12`: next level.
* If you are too lazy to use the mouse, then the navigation in the windows is as follows: select with the "`Tab`" key; "`space`" to confirm.

Launch
------
Jetty:

`mvn jetty:run`

http://localhost:8080/

The project was also successfully tested in the `WildFly 17` container.

Copyright
---------
I respect the rights of `Tetris Holding` to the` Tetris` trademark (® & © 1985 ~ 2019). The word `Tetris` in the name is used for historical reasons.
This was the name of the game for DOS, a remake made on its basis.

The game sounds the melody "`Korobeiniki`" from the album "`A Tribute to the Music of Tetris: Traditional`", authored by `Brado Popcorn`.
It was honestly bought. [Here is his website](http://bradopopcorn.bandcamp.com/album/a-tribute-to-the-music-of-tetris-traditional).
There you can buy your own copy of the melody if you decide to leave the game.

Emoticons are made according to the model found on the site [freepik.com](https://www.freepik.com/free-vector/funny-smileys-collection-flat-design_837327.htm).

The version for Vaadin is made based on the game [Vaadin Tetris](https://github.com/samie/VaadinTetris) by Sami Ekblad.

Have a nice game :)
