Совместная сборка всех проектов.

Проект datacollector_model используется другими проектами.

Проекты datacollector_model и datacollector_server созданы в Eclipse.
Проект datacollector_tablet создан в AndroidStudio.

Автоматическое форматирование

	Файл formatter.xml подключается в Eclipse.
	Window -> Preferences -> Java -> Code Style -> Formatter -> Import...
	Позволяет однотипно форматировать код во всех файлах.

	Автоматическое форматирование при сохранении настраивается в разделе
	Window -> Preferences -> Java -> Editor -> Save Actions

	Автоматического форматирования в AndroidStudio нет.
	Форматирование настроено в самом проекте. Ручное форматирование - Ctrl+Alt+L.

Сборка проектов

	Проект datacollector_model собирается через экспорт в jar прямо в Eclipse.
	Для собрки создан файл описания в папке jardesc.
	Jar-файл собирается и помещается потом как библиотека в другие проекты.
