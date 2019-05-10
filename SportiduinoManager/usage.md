# Описание работы со SportiduinoManager 


## Загрузка данных

* Если приложение тестировалось до ММБ - удалить тестовую версию со всеми данными
* Установить последнюю версию приложения
* Выбрать пункт меню "Получить дистанцию с сайта"
* На ММБ-2019-весна включить галочку "Использовать тестовый сайт"
* Ввести свои емейл и пароль с сайта mmb.progressor.ru
* Загрузить данные с сайта


## Подключение к станции

* Выбрать пункт меню "Настройка станции"
* Запустить поиск станций
* Нажать иконку подключение напротив станции с нужным именем
* Проверить номер КП, режим работы, время станции
* Синхронизировать время станции, если оно отличается не более, чем на 30 минут
* При необходимости сменить номер КП и режим работы


## Инициализация чипов для их выдачи перед стартом

* Подключиться к станции
* Перевести станцию в режим "Инициализация на точке Инициализация чипов"
* Выбрать пункт меню "Инициализация чипов"
* У представителя команды спросить ее номер, ввести его кнопками на форме
* Сверить у представителя команды название его команды и состав участников
* Если ошибки в номере команды не обнаружено, нужно снять галочки у участников, не вышедших на старт
* Когда всё проверено и отмечено, нужно положить чип сверху на станцию и нажать кнопку "Записать чип"
* Появится бегунок прогресса (ждать и чип не трогать), затем появится сообщение об успехе и информация о команде исчезнет из формы
* Если чип убрать раньше времени, то появится сообщение о сбое. Чип в этой ситуации не записан, его можно попытаться инициализировать повторно
* Периодически, когда очереди желающих получить чип нет, нужно заходить на экран работы с сайтом и нажимать кнопку "Отправить мои данные на сайт"


## Начало работы на активной точке

* Подключиться к станции
* На том же экране выбрать новую активную точку из списка
* При необходимости сменить режим (Инициализация, Обычный, Финишный), который был предложен приложением по умолчанию
* Нажать кнопку "Сохранить"
* Если до этого у станции был номер другой активной точки, то запустится процесс сохранения данных с этой точки, который нужно довести до конца
* Выбрать пункт меню "Активная точка ..."
* Начнется первоначальный обмен приложения со станцией. Там, где отображается время станции, будет показано "???"
* Когда приложение получит со станции все данные, отобразится список команд + текущее время станции. Если на станции команды еще не отмечались, обмен пройдет очень быстро
* Приложение готово к совместной работе со станцией на активной точке


## Работа на активной точке

* Волонтер с планшетом постоянно сидит рядом со станцией, опрос приложением станции идёт раз в секунду
* Список уже посетивших станцию команд отсортирован так, что последние пришедшие вверху
* Пришедшая команда подносит чип к станции и появляется в приложении. Нужно посмотреть состав команды
* Если команда из одного человека, то ничего делать не надо
* Если команда из нескольких человек, то надо попросить команду подтвердить, что все собрались. Доступ других команд к станции на время общения с командой надо заблокировать
* Целесообразно на одной станции принимать только команды из "одиночек" с синими чипами, а на второй - всех остальных
* Если в команде есть сошедшие участники, которые в приложении показаны как присутствующие, то нужно снять им галочки и нажать кнопку "Зарегистрировать сход"
* После регистрации схода попросить команду повторно поднести чип к станции. Если команда уже убежала - не страшно, в базе сход зарегистрирован
* Периодически, когда очереди из команд нет, нужно заходить на экран работы с сайтом и нажимать кнопку "Отправить мои данные на сайт"
* Если команд нет и не предвидится, можно гасить экран планшета, но не отключаться от станции


## Завершение работы на активной точке и сохранение данных

* Выбрать пункт меню "Настройка станции"
* Выбрать точку "Инициализация чипов" и режим "Инициализация чипов"
* Нажать кнопку "Сохранить"
* Начнётся сохранение данных со станции в планшет перед сменой ее номера
* После сохранения данных станция сотрет все данные в своей памяти, сменит номер точки и пискнет. Данные по старой точке останутся только на планшете волонтёра.
* В случае сбоев при сохранении данных, станция точку не сменит, можно будет повторить попытку. Данные будут сохраняться повторно.
* После завершения процесса надо зайти на экран работы с сайтом и нажимать кнопку "Отправить мои данные на сайт"


## Особенности работы на активных точках разных типов

* **Старт первого и второго этапа**
   * Тип точки выбирать обязательно "Обычный"
   * На старте 1 этапа можно не проверять состав команд, если его проверяют при выдаче чипов
   * На старте 2 этапа состав обязательно проверять и отмечать сошедших (те, кто после ночевки на ПФ передумали идти дальше)
   * В чипы и в базу пишется только первое время прикладывания чипа к станции, повторные прикладывания ничего не меняют

* **Смена карт, обязательные КП и КП с контролем разделения команд**
   * Тип точки оставлять тот, который предлагается по умолчанию
   * Обязательно проверять состав команды, отставших дожидаться или помечать сошедшими (по желанию команды)
   * Если команда дожидалась отставшего участника, то повторно чип можно не прикладывать - на результат команды это не влияет

* **Финиш первого и второго этапа**
   * Тип точки выбирать обязательно "Финишный"
   * Если команда дожидалась отставшего участника, то обязательно попросить команду повторно приложить чип после появления участника
   * Если в станциях не будет исправлен баг с перезаписью флеша, то на финише второго этапа повторно надо прикладывать чип ко второй станции
   * Обязательно проверять состав команды и помечать сошедших
   * После финиша 1 этапа чип остается у команды, после финиша 2 этапа чип забирается у команды и складывается в пакет, который в Москве передается разработчикам


## Нестандартные ситуации

* **Команде был выдан чужой чип, что было обнаружено до старта**
* **Команда на точке заявила о утере своего чипа**
* **Участники принесли чип, найденный в лесу**