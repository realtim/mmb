<?
//
// Скрипт для обмена данными с приложением Sportiduino Manager
// -----------------------------------------------------------

// Указываем в заголовке версию API, включаем сжатие
define("PROTOCOL_VERSION", 1);
header("X-Sportiduino-Protocol", PROTOCOL_VERSION);
ob_start("ob_gzhandler");

// Проверка того, что скрипт запущен напрямую, а не включен куда-то через include
if (realpath(__FILE__) != realpath($_SERVER['DOCUMENT_ROOT'] . $_SERVER['SCRIPT_NAME']))
    die("Некорректный запуск скрипта");

// Получаем заголовки запроса клиента, вызвавшего наш скрипт
$request = getallheaders();

// Проверяем версию API клиента
if (!isset($request["X-Sportiduino-Protocol"])) die("Запрос сделан не из приложения под Android");
if ($request["X-Sportiduino-Protocol"] != PROTOCOL_VERSION) die("Некорректная версия протокола обмена, требуется " . PROTOCOL_VERSION);

// Подключаемся к базе данных
include("settings.php");
try {
    $pdo = new PDO("mysql:host=$ServerName;dbname=$DBName;charset=utf8", $WebUserName, $WebUserPassword);
} catch (Exception $e) {
    die("Не удалось подключиться к базе ММБ: " . $e->getMessage());
}

// Устанавливаем часовой пояс по умолчанию
// TODO: Обсудить потенциальную проблему с летним временем
date_default_timezone_set("Etc/GMT-3");

// Проверяем авторизацию
if (!isset($request["X-Sportiduino-Auth"])) die("В запросе отсутствуют данные о пользователе");
list($email, $password) = explode("|", $request["X-Sportiduino-Auth"]);
if (!isset($email)) die("Отсутствует емейл пользователя");
if (!preg_match("/[a-zA-Z0-9.-]+@[a-zA-Z0-9.-]+/", $email)) die("Некорректный емейл пользователя");
if (!isset($password)) die("Отсутствует пароль пользователя");
if (!preg_match("/[a-z0-9]+/", $password)) die("Некорректный пароль пользователя");
$sql = $pdo->prepare("SELECT user_id, user_admin FROM Users WHERE user_email = ? AND user_hide = 0 AND user_password = ?");
$sql->execute(array($email, $password));
$row = $sql->fetch(PDO::FETCH_ASSOC);
if (!$row) die("Некорректные логин или пароль");
$user_id = intval($row["user_id"]);
$can_access = $row["user_admin"];
$sql = null;

// Выясняем id крайнего ММБ в базе
$sql = $pdo->prepare("SELECT raid_id FROM Raids ORDER BY raid_id DESC LIMIT 1");
$sql->execute();
$row = $sql->fetch(PDO::FETCH_ASSOC);
if (!$row) die("В базе марш-бросков не найдено");
$raid_id = intval($row["raid_id"]);
$sql = null;

// Если пользователь не админ, то может он модератор этого марш-броска?
if (!$can_access) {
    $sql = $pdo->prepare("SELECT raidmoderator_id FROM RaidModerators WHERE raid_id = :raid_id AND user_id = :user_id AND raidmoderator_hide = 0");
    $sql ->bindParam("raid_id", $raid_id, PDO::PARAM_INT);
    $sql ->bindParam("user_id", $user_id, PDO::PARAM_INT);
    $sql->execute();
    $can_access = $sql->rowCount();
    $sql = null;
}

// Но может быть он хотя бы волонтер?
if (!$can_access) {
    $sql = $pdo->prepare("SELECT raiddeveloper_id FROM RaidDevelopers WHERE raid_id = :raid_id AND user_id = :user_id AND raiddeveloper_hide = 0");
    $sql ->bindParam("raid_id", $raid_id, PDO::PARAM_INT);
    $sql ->bindParam("user_id", $user_id, PDO::PARAM_INT);
    $sql->execute();
    $can_access = $sql->rowCount();
    $sql = null;
}

// Обычные пользователи не допускаются, чтобы они не увидели параметры дистанции
if (!$can_access) die("Работать с системой могут только админы/модераторы/волонтеры");

// Все в порядке, обрабатываем запрос пользователя
if (!isset($request["X-Sportiduino-Action"])) die("Отсутствует код операции, запрошенной у сервера");
switch ($request["X-Sportiduino-Action"]) {
    case 1:
        SendDistance($pdo, $raid_id);
        break;
    case 2:
        ReceiveResults($pdo, $user_id);
        break;
    case 3:
        SendResults($pdo, $raid_id, $request);
        break;
    case 4:
        ReceiveDatabase($pdo, $user_id);
        break;
    default:
        die("Неизвестный код операции");
}

function SendDistance(PDO $pdo, $raid_id)
{
    // Получаем название марш-броска и интервал до старта, за который марш-бросок переводится в режим readonly
    $sql = $pdo->prepare("SELECT raid_name, raid_readonlyhoursbeforestart FROM Raids WHERE raid_id = :raid_id");
    $sql ->bindParam("raid_id", $raid_id, PDO::PARAM_INT);
    $sql->execute();
    $row = $sql->fetch(PDO::FETCH_ASSOC);
    $raid_name = trim(strtr($row["raid_name"], "\t\n", "  "));
    $ro_hours = intval($row["raid_readonlyhoursbeforestart"]);
    $sql = null;

    // Пока поддерживаем только марш-броски с одной и только одной дистанцией
    $sql = $pdo->prepare("SELECT distance_id FROM Distances WHERE raid_id = :raid_id AND distance_hide = 0");
    $sql ->bindParam("raid_id", $raid_id, PDO::PARAM_INT);
    $sql->execute();
    $row = $sql->fetch(PDO::FETCH_ASSOC);
    if (!count($row)) die("В марш-броск отсутствуют дистанции");
    if (count($row) > 1) die("Марш-броски из нескольких дистанций пока не поддерживаются");
    $distance_id = intval($row["distance_id"]);
    $sql = null;

    // Получаем все точки дистанции
    $points = array();
    $raid_start = -1;
    $prev_start = -1;
    $max_order = -1;
    $sql = $pdo->prepare("SELECT levelpoint_order, levelpoint_name, pointtype_id, levelpoint_penalty, UNIX_TIMESTAMP(levelpoint_mindatetime) AS mintime, UNIX_TIMESTAMP(levelpoint_maxdatetime) AS maxtime FROM LevelPoints WHERE distance_id = :distance_id AND levelpoint_hide = 0 ORDER BY levelpoint_order ASC");
    $sql ->bindParam("distance_id", $distance_id, PDO::PARAM_INT);
    $sql->execute();
    $result = $sql->fetchAll(PDO::FETCH_ASSOC);
    if (!count($result)) die("На дистанции отсутствуют точки");
    foreach ($result as $row) {
        if (isset($points["p" . $row["levelpoint_order"]])) die("На дистанции две точки с номером " . $row["levelpoint_order"]);
        if ($raid_start == -1) {
            if (!$row["mintime"]) die("Точка старта 1 этапа без времени начала работы");
            $raid_start = $row["mintime"];
        }
        if (!$row["mintime"]) $row["mintime"] = $prev_start; else $prev_start = $row["mintime"];
        if ($row["levelpoint_order"] <= 0) die("Некорректный номер точки " . $row["levelpoint_order"]);
        if ($row["levelpoint_order"] > $max_order) $max_order = $row["levelpoint_order"];
        $row["levelpoint_name"] = trim(strtr($row["levelpoint_name"], "\t\n", "  "));
        $points["p" . $row["levelpoint_order"]] = array("name" => $row["levelpoint_name"], "type" => intval($row["pointtype_id"]), "penalty" => intval($row["levelpoint_penalty"]), "start" => intval($row["mintime"]), "end" => intval($row["maxtime"]));
    }
    $sql = null;
    $db_ready_date = $raid_start - $ro_hours * 60 * 60;

    // Генерируем отсутствующие времена закрытия точек
    $raid_end = -1;
    $prev_end = -1;
    $keys = array_keys($points);
    for ($n = end($keys); $n > 0; $n--) {
        if ($raid_end == -1) {
            if (!$points[$n]["end"]) die("Точка финиша последнего этапа без времени окончания работы");
            $raid_end = $points[$n]["end"];
        }
        if (!$points[$n]["end"]) $points[$n]["end"] = $prev_end; else $prev_end = $points[$n]["end"];
    }

    // Получаем амнистии
    $discounts = array();
    $sql = $pdo->prepare("SELECT levelpointdiscount_value AS value, levelpointdiscount_start AS start, levelpointdiscount_finish AS finish FROM LevelPointDiscounts WHERE distance_id = :distance_id AND levelpointdiscount_hide = 0");
    $sql ->bindParam("distance_id", $distance_id, PDO::PARAM_INT);
    $sql->execute();
    $result = $sql->fetchAll(PDO::FETCH_ASSOC);
    foreach ($result as $row) {
        if (!$row["value"]) die("Нулевая амнистия для интервала " . $row["start"] . " - " . $row["finish"]);
        if (!isset($points["p" . $row["start"]])) die("Не существует точка " . $row["start"] . " из интервала амнистии");
        if (!isset($points["p" . $row["finish"]])) die("Не существует точка " . $row["finish"] . " из интервала амнистии");
        $discounts[] = array("value" => intval($row["value"]), "start" => intval($row["start"]), "end" => intval($row["finish"]));
    }
    $sql = null;

    // Получаем список неудаленных команд
    $teams = array();
    $max_number = -1;
    $sql = $pdo->prepare("SELECT team_num, team_name, team_mapscount FROM Teams WHERE distance_id = :distance_id AND team_hide = 0 ORDER BY team_num ASC");
    $sql ->bindParam("distance_id", $distance_id, PDO::PARAM_INT);
    $sql->execute();
    $result = $sql->fetchAll(PDO::FETCH_ASSOC);
    foreach ($result as $row) {
        if ($row["team_num"] <= 0) die("Некорректный номер команды " . $row["team_num"]);
        if (isset($teams["t" . $row["team_num"]])) die("Дублирующийся номер команды " . $row["team_num"]);
        if ($row["team_num"] > $max_number) $max_number = $row["team_num"];
        $row["team_name"] = trim(strtr($row["team_name"], "\t\n", "  "));
        $teams["t" . $row["team_num"]] = array("name" => $row["team_name"], "maps" => intval($row["team_mapscount"]), "members" => 0);
    }
    $sql = null;
    if (!count($teams)) die("Не заявилось ни одной команды");

    // Получаем список участников из этих команд
    $members = array();
    $sql = $pdo->prepare("SELECT Users.user_id, Teams.team_num, Users.user_name, Users.user_birthyear, Users.user_phone FROM Teams, TeamUsers, Users WHERE distance_id = :distance_id AND team_hide = 0 AND teamuser_hide = 0 AND user_hide = 0 AND TeamUsers.user_id = Users.user_id AND TeamUsers.team_id = Teams.team_id ORDER BY Users.user_id ASC");
    $sql ->bindParam("distance_id", $distance_id, PDO::PARAM_INT);
    $sql->execute();
    $result = $sql->fetchAll(PDO::FETCH_ASSOC);
    foreach ($result as $row) {
        if (isset($members["m" . $row["user_id"]])) die("Дублирующийся участник " . $row["user_id"]);
        $row["team_num"] = intval($row["team_num"]);
        if (!isset($teams["t" . $row["team_num"]])) die("Участник " . $row["user_id"] . " из несуществующей команды #" . $row["team_num"]);
        $teams["t" . $row["team_num"]]["members"]++;
        $name = trim(strtr($row["user_name"] . " " . $row["user_birthyear"], "\t\n", "  "));
        $row["user_phone"] = trim(strtr($row["user_phone"], "\t\n", "  "));
        $members["m" . $row["user_id"]] = array("team" => $row["team_num"], "name" => $name, "phone" => $row["user_phone"]);
    }
    $sql = null;

    // Проверяем, не оказалось ли команд без участников
    foreach ($teams as $n => $team)
        if (!$team["members"]) die("Команда $n без участников");

    // Все данные извлечены успешно, отправляем их
    echo "\n";
    echo "R\t$raid_id\t$db_ready_date\t$raid_end\t$raid_name\n";
    echo "P\t", count($points), "\t$max_order\n";
    foreach ($points as $n => $point)
        echo "\t", substr($n, 1), "\t", $point["type"], "\t", $point["penalty"], "\t", $point["start"], "\t", $point["end"], "\t", $point["name"], "\n";
    echo "D\t", count($discounts), "\n";
    foreach ($discounts as $discount)
        echo "\t", $discount["value"], "\t", $discount["start"], "\t", $discount["end"], "\n";
    echo "T\t", count($teams), "\t$max_number\n";
    foreach ($teams as $n => $team)
        echo "\t", substr($n, 1), "\t", $team["members"], "\t", $team["maps"], "\t", $team["name"], "\n";
    echo "M\t", count($members), "\n";
    foreach ($members as $n => $member)
        echo "\t", substr($n, 1), "\t", $member["team"], "\t", $member["name"], "\t", $member["phone"], "\n";
    echo "E\n";
}

function ReceiveResults(PDO $pdo, $user_id)
{
    // Проверяем корректность первой строки с датой локальной базы и количеством данных с чипов
    if (!isset($_POST["data"])) die("Запрос клиента некорректно сформирован");
    $lines = explode("\n", $_POST["data"]);
    if (count($lines) < 2) die("В запросе недостаточное количество строк");
    list($db_dl_time, $n_events) = explode("\t", $lines[0]);
    if (!isset($db_dl_time) || !isset($n_events)) die("Некорректная первая строка запроса '" . $lines[0] ."'");
    if (intval($n_events) != (count($lines) - 1)) die("Некорректное количество данных из чипов '$n_events'");

    // Получаем текущий максимальный id в таблице сырых данных, чтобы потом понимать, какие строчки в нее добавились
    $sql = $pdo->prepare("SELECT MAX(sportiduinochips_id) FROM SportiduinoChips");
    $sql->execute();
    $row = $sql->fetch(PDO::FETCH_NUM);
    $max_id = intval($row[0]);
    $sql = null;

    // Готовим запрос на вставку присланных данных в таблицу сырых данных
    $sql = $pdo->prepare("INSERT IGNORE INTO SportiduinoChips (user_id, sportiduino_dbdate, sportiduino_stationmac, sportiduino_stationtime, sportiduino_stationdrift, sportiduino_stationnumber, sportiduino_stationmode, sportiduino_inittime, team_num, sportiduino_teammask, levelpoint_order, teamlevelpoint_datetime) VALUES (?, FROM_UNIXTIME(?), ?, FROM_UNIXTIME(?), ?, ?, ?, FROM_UNIXTIME(?), ?, ?, ?, FROM_UNIXTIME(?))");
    // Выполняем транзакцию по добавлению всех присланных данных в таблицу
    try {
        $pdo->beginTransaction();
        // В цикле сканируем все строки с данными из чипов и добавляем их в транзакцию
        for ($i = 1; $i <= $n_events; $i++) {
            // Формируем массив из целых чисел, описывающих событие с чипом
            $event_data = explode("\t", $lines[$i]);
            if (count($event_data) != 10) die("Некорректное количество данных в строке '" . $lines[$i] . "'");
            $values = array($user_id, intval($db_dl_time));
            foreach ($event_data as $value)
                $values[] = intval($value);
            // Добавляем их отдельной строкой в таблице
            $sql->execute($values);
        }
        // Пробуем выполнить транзакцию
        $pdo->commit();
    } catch (Exception $e) {
        // Отменяем транзацию в случае ошибки
        $pdo->rollback();
        die($e);
    }
    $sql = null;

    // Копируем упрощенную версию присланных результатов в таблицу с результатами
    $sql = $pdo->prepare("INSERT INTO SportiduinoResults(team_num, sportiduino_teammask, levelpoint_order, teamlevelpoint_datetime) SELECT team_num, MIN(sportiduino_teammask), levelpoint_order, MAX(teamlevelpoint_datetime) FROM SportiduinoChips WHERE sportiduinochips_id > :max_id GROUP BY team_num, levelpoint_order");
    $sql ->bindParam("max_id", $max_id, PDO::PARAM_INT);
    $sql->execute();
    $sql = null;

    // TODO: Обновить маску и время на точках, где работали судейские станции на основе всех данных, а не только полученных прямо сейчас

    // Сообщаем  о количестве обработанных данных
    // (количество добавленных в таблицу не проверяется и может быть меньше количества обработанных из-за дублей)
    echo "\n$n_events";
}

function SendResults(PDO $pdo, $raid_id, $request)
{
    if (!isset($request["X-Sportiduino-Time"])) $from_time = "1970-01-01 00:00:00"; else $from_time = $request["X-Sportiduino-Time"];
    if (!preg_match("/[0-9 :-]+/", $from_time)) die("Некорректное время начала данных");
    die("Функция еще не реализована");
}

function ReceiveDatabase(PDO $pdo, $user_id)
{
    // Проверяем корректность первой строки с именем отправленного файла
    if (!isset($_POST["data"])) die("Запрос клиента некорректно сформирован");

    // Распаковываем содержимое файла
    $base64 = str_replace("_", "/", str_replace("-", "+", $_POST["data"]));
    $content = base64_decode($base64);
    if ($content === FALSE) die("Ошибка распаковки файла");

    // Сохраняем файл
    $fullname = "/var/www/clients/client5/web3/web/logs/mmb.sqlite.$user_id." . date("c");
    $file = fopen($fullname, "w");
    if ($file === FALSE) die("Ошибка создания файла '" . $fullname . "'");
    $result = fwrite($file, $content);
    if ($result === FALSE) die("Ошибка записи в файл");
    fclose($file);

    // Сообщаем  пустой строкой об успехе
    echo "\n";
}
?>