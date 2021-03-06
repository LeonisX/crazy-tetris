# Crazy Tetris на стороне сервера используя простые веб-технологии

Это технологический трюк, демонстрирующий, что серверная сторона Java пригодна даже для интерактивных игр.
Код игры запускается на движке сервлета, а пользовательский интерфейс построен с использованием [Vaadin](http://vaadin.com/). 
Автоматическое push-соединение в Vaadin использует веб-сокеты, если они применимы для клиента, сервера (и прокси),
в другом случае используются длинные запросы или стриминг. 

Графика динамически рисуется с помощью дополнения [Canvas](http://vaadin.com/directory#addon/canvas).
Естественно, это нагружает траффик, но это можно оптимизировать, например, использовать SVG, или HTML-таблицы
вместо графики. Всё играбельно даже в GSM сетях.

Сделано по мотивам [Vaadin + HTML 5 Canvas Demo](https://github.com/samie/VaadinTetris).
