



Андроид-приложение для ввода взятых КП с карточки




!!! Проекты datacollector_model и datacollector_server устарели и больше не используются.

Основной рабочий проект - datacollector_tablet.

Разработка ведётся в AndroidStudio.
В git файлы проекта idea не положены. Нужно в своей версии AndroidStudio просто импортировать build.gradle из datacollector_tablet.

Сборка проекта - gradle assemble.
Затем можно достать готовый APK из директории app/build/output/apk.

Для того, чтобы в GIT не пролезли файлы от Idea и Eclipse их надо добавить в файл глобальных исключений Git строки:
.idea
*.iml
.settings
