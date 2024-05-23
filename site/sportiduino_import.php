<?php

/**
 * Импорт данных с проверкой из SportiduinoRecords в основные таблицы
 */

// Проверяем, что скрипт запущен из интерфейса
if (isset($MyPHPScript) && ($action === "SportiduinoImport")) {
    if (!$Administrator && !$Moderator) {
        return;
    }
} else {
    die("Некорректный запуск скрипта");
}

// Подключаемся к базе данных
include("settings.php");
try {
    $pdo = new PDO("mysql:host=$ServerName;dbname=$DBName;charset=utf8", $WebUserName, $WebUserPassword);
} catch (Exception $e) {
    die("Не удалось подключиться к базе ММБ: " . $e->getMessage());
}

// Устанавливаем часовой пояс по умолчанию
date_default_timezone_set("Etc/GMT-3");

// Выясняем id крайнего ММБ в базе
$sql = $pdo->prepare("SELECT raid_id, raid_closedate FROM Raids ORDER BY raid_id DESC LIMIT 1");
$sql->execute();
$row = $sql->fetch(PDO::FETCH_ASSOC);
if (!$row) {
    die("В базе марш-бросков не найдено");
}
if (!is_null($row["raid_closedate"])) {
    die("Итоги крайнего марш-броска уже подведены");
}
$raid_id = (int)$row["raid_id"];
$sql = null;

// Пока поддерживаем только марш-броски с одной и только одной дистанцией
$sql = $pdo->prepare("SELECT distance_id FROM Distances WHERE raid_id = :raid_id AND distance_hide = 0");
$sql->bindParam("raid_id", $raid_id, PDO::PARAM_INT);
$sql->execute();
$row = $sql->fetch(PDO::FETCH_ASSOC);
if (!count($row)) {
    die("В марш-броске отсутствуют дистанции");
}
if (count($row) > 1) {
    die("Марш-броски из нескольких дистанций пока не поддерживаются");
}
$distance_id = (int)$row["distance_id"];
$sql = null;

// Получаем все точки дистанции
$points = [];
$raid_start = -1;
$prev_start = -1;
$start_id = 0;
$sql = $pdo->prepare(
    "SELECT levelpoint_order, levelpoint_id, levelpoint_name, pointtype_id, UNIX_TIMESTAMP(levelpoint_mindatetime) AS mintime, UNIX_TIMESTAMP(levelpoint_maxdatetime) AS maxtime FROM LevelPoints WHERE distance_id = :distance_id AND levelpoint_hide = 0 ORDER BY levelpoint_order ASC"
);
$sql->bindParam("distance_id", $distance_id, PDO::PARAM_INT);
$sql->execute();
$result = $sql->fetchAll(PDO::FETCH_ASSOC);
if (!count($result)) {
    die("На дистанции отсутствуют точки");
}
foreach ($result as $row) {
    if (isset($points[$row["levelpoint_order"]])) {
        die("На дистанции две точки с номером " . $row["levelpoint_order"]);
    }
    if ($raid_start == -1) {
        if (!$row["mintime"]) {
            die("Точка старта 1 этапа без времени начала работы");
        }
        $raid_start = $row["mintime"];
        $start_id = $row["levelpoint_order"];
    }
    if (!$row["mintime"]) {
        $row["mintime"] = $prev_start;
    } else {
        $prev_start = $row["mintime"];
    }
    if ($row["levelpoint_order"] <= 0) {
        die("Некорректный номер точки " . $row["levelpoint_order"]);
    }
    $points[$row["levelpoint_order"]] = [
        "id" => $row["levelpoint_id"],
        "name" => $row["levelpoint_name"],
        "type" => (int)$row["pointtype_id"],
        "start" => (int)$row["mintime"],
        "end" => (int)$row["maxtime"],
        "active" => 0,
    ];
}
$sql = null;

// Генерируем отсутствующие времена закрытия точек
$raid_end = -1;
$prev_end = -1;
$keys = array_keys($points);
for ($n = end($keys); $n > 0; $n--) {
    if ($raid_end == -1) {
        if (!$points[$n]["end"]) {
            die("Точка финиша последнего этапа без времени окончания работы");
        }
        $raid_end = $points[$n]["end"];
    }
    if (!$points[$n]["end"]) {
        $points[$n]["end"] = $prev_end;
    } else {
        $prev_end = $points[$n]["end"];
    }
}

// Добавляем точку выдачи чипов
$points[0] = ["name" => "Выдача чипов", "start" => $raid_start, "end" => $raid_end];
// Считаем корректными отметки и инициализации в интервале, начиная за 10 минут до открытия старта и заканчивая 10 минутами после закрытия финиша
$raid_start = date("Y-m-d H:i:s", $raid_start - 600);
$raid_end = date("Y-m-d H:i:s", $raid_end + 600);

// Получаем список неудаленных команд
$teams = [];
$sql = $pdo->prepare("SELECT team_num, team_id, team_name FROM Teams WHERE distance_id = :distance_id AND team_hide = 0 ORDER BY team_num ASC");
$sql->bindParam("distance_id", $distance_id, PDO::PARAM_INT);
$sql->execute();
$result = $sql->fetchAll(PDO::FETCH_ASSOC);

foreach ($result as $row) {
    if ($row["team_num"] <= 0) {
        die("Некорректный номер команды " . $row["team_num"]);
    }
    if (isset($teams[$row["team_num"]])) {
        die("Дублирующийся номер команды " . $row["team_num"]);
    }
    $row["team_name"] = trim(strtr($row["team_name"], "\t\n", "  "));
    $teams[$row["team_num"]] = [
        "id" => $row["team_id"],
        "name" => $row["team_name"],
        "members" => [],
        "init" => [],
        "dismiss" => [],
        "points" => [],
        "registered" => [],
        "stations" => [],
        "sequence" => [],
        "ignore" => 0,
    ];
}
$sql = null;
if (!count($teams)) {
    die("Не заявилось ни одной команды");
}

// Получаем список участников из этих команд
$members = [];
$sql = $pdo->prepare(
    "SELECT Users.user_id, Teams.team_num, teamuser_id, Users.user_name FROM Teams, TeamUsers, Users WHERE distance_id = :distance_id AND team_hide = 0 AND teamuser_hide = 0 AND user_hide = 0 AND TeamUsers.user_id = Users.user_id AND TeamUsers.team_id = Teams.team_id ORDER BY Users.user_id ASC"
);
$sql->bindParam("distance_id", $distance_id, PDO::PARAM_INT);
$sql->execute();
$result = $sql->fetchAll(PDO::FETCH_ASSOC);
foreach ($result as $row) {
    if (isset($members[$row["user_id"]])) {
        die("Дублирующийся участник " . $row["user_id"]);
    }
    $row["team_num"] = (int)$row["team_num"];
    if (!isset($teams[$row["team_num"]])) {
        die("Участник " . $row["user_id"] . " из несуществующей команды #" . $row["team_num"]);
    }
    $teams[$row["team_num"]]["members"][] = $row["user_id"];
    $members[$row["user_id"]] = ["team" => $row["team_num"], "teamuser_id" => $row["teamuser_id"], "name" => $row["user_name"]];
}
$sql = null;

// Проверяем, не оказалось ли команд без участников или со слишком большим числом участников
foreach ($teams as $n => $team) {
    if (!count($team["members"])) {
        die("Команда $n без участников");
    }
    if (count($team["members"]) > 16) {
        die("В команде $n слишком много участников");
    }
}
$MAX_MASK = [0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535];

// Проверяем, что все номера станций соответствуют точкам нашей дистанции
$sql = $pdo->prepare("SELECT levelpoint_order FROM SportiduinoRecords GROUP BY levelpoint_order WHERE teamlevelpoint_datetime >= :raid_start AND teamlevelpoint_datetime <= :raid_end");
$sql->bindParam("raid_start", $raid_start, PDO::PARAM_STR);
$sql->bindParam("raid_end", $raid_end, PDO::PARAM_STR);
$sql->execute();
$result = $sql->fetchAll(PDO::FETCH_ASSOC);
foreach ($result as $row) {
    if (!isset($points[$row["levelpoint_order"]])) {
        die("Несуществующий номер точки " . $row["levelpoint_order"]);
    }
}
$sql = null;

// Отмечаем точки с судейскими станциями
$sql = $pdo->prepare(
    "SELECT sportiduino_stationnumber FROM SportiduinoRecords WHERE sportiduino_stationnumber > 0 AND teamlevelpoint_datetime >= :raid_start AND teamlevelpoint_datetime <= :raid_end GROUP BY sportiduino_stationnumber"
);
$sql->bindParam("raid_start", $raid_start, PDO::PARAM_STR);
$sql->bindParam("raid_end", $raid_end, PDO::PARAM_STR);
$sql->execute();
$result = $sql->fetchAll(PDO::FETCH_ASSOC);
foreach ($result as $row) {
    $points[$row["sportiduino_stationnumber"]]["active"] = 1;
}
$sql = null;


// Статические данные загружены, проверяем базовую корректность данных со станций
// -----------------------------------------------------------------------------

$errors = [];

// Собираем времена инициализации чипов, которые были официально выданы командам
$sql = $pdo->prepare(
    "SELECT team_num, sportiduino_inittime FROM SportiduinoRecords WHERE sportiduino_inittime = teamlevelpoint_datetime AND sportiduino_stationmode = 0 AND teamlevelpoint_datetime >= :raid_start AND teamlevelpoint_datetime <= :raid_end GROUP BY team_num, sportiduino_inittime ORDER BY team_num ASC, sportiduino_inittime ASC"
);
$sql->bindParam("raid_start", $raid_start, PDO::PARAM_STR);
$sql->bindParam("raid_end", $raid_end, PDO::PARAM_STR);
$sql->execute();
$result = $sql->fetchAll(PDO::FETCH_ASSOC);
foreach ($result as $row) {
    if (!isset($teams[$row["team_num"]])) {
        $errors[] = "Несуществующий номер команды " . $row["team_num"];
        continue;
    }
    $teams[$row["team_num"]]["init"][] = $row["sportiduino_inittime"];
}
$sql = null;

// Проверяем, что после выдачи новых чипов на дистанции не появлялись старые
$chips_lost = [];
foreach ($teams as $n => $team) {
    if (count($team["init"]) > 1) {
        $chips_lost[] = $n;
        $sql = $pdo->prepare(
            "SELECT sportiduino_inittime, MAX(teamlevelpoint_datetime) AS max_result FROM SportiduinoRecords WHERE team_num = :team_num AND teamlevelpoint_datetime >= :raid_start AND teamlevelpoint_datetime <= :raid_end GROUP BY team_num, sportiduino_inittime ORDER BY sportiduino_inittime ASC"
        );
        $sql->bindParam("team_num", $n, PDO::PARAM_INT);
        $sql->bindParam("raid_start", $raid_start, PDO::PARAM_STR);
        $sql->bindParam("raid_end", $raid_end, PDO::PARAM_STR);
        $sql->execute();
        $result = $sql->fetchAll(PDO::FETCH_ASSOC);
        $check = [];
        foreach ($result as $row) {
            $check[] = ["init" => $row["sportiduino_inittime"], "max_result" => $row["max_result"]];
        }
        $sql = null;
        for ($i = 1; $i < count($check); $i++) {
            if ($check[$i]["init"] < $check[$i - 1]["max_result"]) {
                $errors[] = "Команда $n использовала свой первый чип в " . $check[$i - 1]["max_result"] . " уже после получения нового чипа в " . $check[$i]["init"];
                $teams[$n]["ignore"] = 1;
            }
        }
    }
}

// Проверяем, что в результатах нет незаявленных команд и участников
$sql = $pdo->prepare(
    "SELECT team_num, MAX(sportiduino_teammask) AS max_mask, MIN(sportiduino_teammask) AS min_mask FROM SportiduinoRecords WHERE teamlevelpoint_datetime >= :raid_start AND teamlevelpoint_datetime <= :raid_end GROUP BY team_num"
);
$sql->bindParam("raid_start", $raid_start, PDO::PARAM_STR);
$sql->bindParam("raid_end", $raid_end, PDO::PARAM_STR);
$sql->execute();
$result = $sql->fetchAll(PDO::FETCH_ASSOC);
foreach ($result as $row) {
    if (!isset($teams[$row["team_num"]])) {
        $errors[] = "Несуществующий номер команды " . $row["team_num"];
        continue;
    }
    if ($MAX_MASK[count($teams[$row["team_num"]]["members"])] < $row["max_mask"]) {
        $errors[] = "Лишние участники в команде " . $row["team_num"];
        $teams[$row["team_num"]]["ignore"] = 1;
    }
    $teams[$row["team_num"]]["max_mask"] = $row["max_mask"];
    $teams[$row["team_num"]]["min_mask"] = $row["min_mask"];
}
$sql = null;

// Проверяем, что командам с результатами нами был выдан хотя бы один чип
foreach ($teams as $n => $team) {
    if (isset($team["max_mask"]) && !count($team["init"])) {
        $errors[] = "Команда $n имеет отметки на дистанции, но не получала от нас чипа";
        $teams[$n]["ignore"] = 1;
    }
}

// Регистрируем неявки участников на выдачу чипов
$nteams_members_absent = 0;
$sql = $pdo->prepare("SELECT team_num, sportiduino_teammask FROM SportiduinoRecords WHERE levelpoint_order = 0 AND teamlevelpoint_datetime >= :raid_start AND teamlevelpoint_datetime <= :raid_end");
$sql->bindParam("raid_start", $raid_start, PDO::PARAM_STR);
$sql->bindParam("raid_end", $raid_end, PDO::PARAM_STR);
$sql->execute();
$result = $sql->fetchAll(PDO::FETCH_ASSOC);
foreach ($result as $row) {
    if (!isset($teams[$row["team_num"]])) {
        continue;
    }
    $n = $row["team_num"];
    $diff = compare_masks($MAX_MASK[count($teams[$n]["members"])], $row["sportiduino_teammask"]);
    if (count($diff["added"]) || !count($diff["removed"])) {
        continue;
    }
    $nteams_members_absent++;
    // В результатах неявка на выдачу чипов будет сохранена как неявка на старт 1 этапа
    foreach ($diff["removed"] as $member) {
        $teams[$n]["dismiss"][$member] = $start_id;
    }
}
$sql = null;

// Регистрируем и проверяем сходы участников на дистанции
$nteams_members_dismissed = 0;
foreach ($teams as $n => $team) {
    if (isset($team["max_mask"]) && ($team["max_mask"] != $team["min_mask"])) {
        $nteams_members_dismissed++;
        // Формируем исходную и обновленную маску для каждой точки с судейской станцией
        $masks = [];
        $index = 0;
        $last_point = -1;
        $sql = $pdo->prepare(
            "SELECT levelpoint_order, sportiduino_teammask FROM SportiduinoRecords WHERE team_num = :team_num AND sportiduino_stationnumber = levelpoint_order AND teamlevelpoint_datetime >= :raid_start AND teamlevelpoint_datetime <= :raid_end ORDER BY teamlevelpoint_datetime ASC, sportiduino_stationtime ASC"
        );
        $sql->bindParam("team_num", $n, PDO::PARAM_INT);
        $sql->bindParam("raid_start", $raid_start, PDO::PARAM_STR);
        $sql->bindParam("raid_end", $raid_end, PDO::PARAM_STR);
        $sql->execute();
        $result = $sql->fetchAll(PDO::FETCH_ASSOC);
        foreach ($result as $row) {
            if ($last_point != $row["levelpoint_order"]) {
                $last_point = $row["levelpoint_order"];
                $masks[$index] = ["point" => $last_point, "mask" => $row["sportiduino_teammask"]];
                $index++;
            } else {
                if ($masks[$index - 1]["mask"] != $row["sportiduino_teammask"]) {
                    $masks[$index - 1]["new_mask"] = $row["sportiduino_teammask"];
                }
            }
        }
        $sql = null;
        // Удаляем новые маски, если они совпадают с исходными
        foreach ($masks as $index => $mask) {
            if (isset($mask["new_mask"]) && $mask["mask"] == $mask["new_mask"]) {
                unset($masks[$index]["new_mask"]);
            }
        }
        // Обрабатываем ситуации, когда команда убегала без обновления маски в чипе
        for ($i = 1; $i < count($masks); $i++) {
            if (isset($masks[$i]["new_mask"]) && (
                    (isset($masks[$i - 1]["new_mask"]) && ($masks[$i]["new_mask"] == $masks[$i - 1]["new_mask"])) ||
                    (!isset($masks[$i - 1]["new_mask"]) && ($masks[$i]["new_mask"] == $masks[$i - 1]["mask"]))
                )) {
                $masks[$i]["mask"] = $masks[$i]["new_mask"];
                unset($masks[$i]["new_mask"]);
            }
        }
        // Проходимся по всем точкам, где изменялась маска, запоминаем сходы и отслеживаем странные изменения маски
        $current_mask = $masks[0]["mask"];
        $old_mask = 0;
        for ($i = 1; $i < count($masks); $i++) {
            unset($diff);
            if (isset($masks[$i]["new_mask"])) {
                // Произошли изменения в составе - на точке есть две отметки со старым и новым составом
                if ($masks[$i]["mask"] == $current_mask) {
                    // Старый состав поменялся на новый
                    $diff = compare_masks($masks[$i]["mask"], $masks[$i]["new_mask"]);
                    $current_mask = $masks[$i]["new_mask"];
                    $old_mask = $masks[$i]["mask"];
                } else {
                    // Двойное изменение состава, так быть не должно
                    if ($masks[$i]["mask"] == $old_mask) {
                        $errors[] = "Команда $n на точке '" . $points[$masks[$i]["point"]]["name"] . "' пришла без записанного в чип схода участников";
                    } else {
                        $errors[] = "Команда $n на точке '" . $points[$masks[$i]["point"]]["name"] . "' имела в чипе состав, который туда не записывали";
                        $teams[$n]["ignore"] = 1;
                    }
                }
            } else {
                // Изменения состава оператором не производились
                if (($masks[$i]["mask"] != $current_mask) && ($masks[$i]["mask"] != $old_mask)) {
                    // В чипе ни последняя маска, ни предпоследняя, а левая
                    $errors[] = "Команда $n на точке '" . $points[$masks[$i]["point"]]["name"] . "' появилась в измененном составе, а сходов до этого зарегистрировано не было";
                    $teams[$n]["ignore"] = 1;
                }
            }
            if (!isset($diff)) {
                continue;
            }
            // Обрабатываем зарегистрированное изменение состава
            if (count($diff["added"])) {
                $errors[] = "В команде $n на точке '" . $points[$masks[$i]["point"]]["name"] . "' появились новые участники";
                $teams[$n]["ignore"] = 1;
                continue;
            }
            foreach ($diff["removed"] as $member) {
                if (isset($team["dismiss"][$member])) {
                    $errors[] = "В команде $n на точке '" . $points[$masks[$i]["point"]]["name"] . "' участник не мог сойти, так как уже сошел на точке '" . $points[$team["dismiss"][$member]]["name"] . "'";
                } else {
                    $teams[$n]["dismiss"][$member] = $masks[$i]["point"];
                    if ($masks[$i]["point"] == $start_id) {
                        $errors[] = "В команде $n сход участников между инициализацией и стартом 1 этапа";
                    }
                }
            }
        }
    }
}


// Общая корректность проверена, загружаем массив результатов на точках
// --------------------------------------------------------------------

$sql = $pdo->prepare(
    "SELECT team_num, levelpoint_order, teamlevelpoint_datetime, sportiduino_inittime, sportiduino_stationnumber, sportiduino_stationmac FROM SportiduinoRecords WHERE levelpoint_order > 0 AND teamlevelpoint_datetime >= :raid_start AND teamlevelpoint_datetime <= :raid_end ORDER BY teamlevelpoint_datetime ASC"
);
$sql->bindParam("raid_start", $raid_start, PDO::PARAM_STR);
$sql->bindParam("raid_end", $raid_end, PDO::PARAM_STR);
$sql->execute();
$stations = [];
while ($row = $sql->fetch(PDO::FETCH_ASSOC)) {
    // Проверяем, что команда была заявлена
    if (!isset($teams[$row["team_num"]])) {
        continue;
    }
    $n = $row["team_num"];
    // Проверяем, что чип с данным временем инициализации был выдан нами
    if (!in_array($row["sportiduino_inittime"], $teams[$n]["init"])) {
        $errors[] = "В команде $n на точке '" . $points[$row["levelpoint_order"]]["name"] . "' в чипе, считанном станцией с адресом " . printMAC(
                $row["sportiduino_stationmac"]
            ) . " и номером " . $row["sportiduino_stationnumber"] . ", чип был выдан не нами в " . $row["sportiduino_inittime"];
    }
    // Анализируем время на точке
    $time = $row["teamlevelpoint_datetime"];
    $point = $row["levelpoint_order"];
    if (isset($teams[$n]["points"][$point])) {
        // У нас уже есть данные о времени команды на этой точке
        if ($teams[$n]["points"][$point] != $time) {
            if ($points[$point]["type"] == 1) {
                $errors[] = "У команды $n на точке '" . $points[$point]["name"] . "', имеющей тип 'Старт', несколько разных отметок времени";
            }
            if (!$points[$point]["active"]) {
                $errors[] = "У команды $n на точке '" . $points[$point]["name"] . "' без судейской станции несколько разных отметок времени: " . $teams[$n]["points"][$point] . " и " . $time;
            }
            // При повторной отметке правильное время - более позднее
            if (($points[$point]["type"] != 1) && $points[$point]["active"]) {
                $teams[$n]["points"][$point] = $time;
            }
        }
    } else {
        // Первое появление данных о команде на этой точке
        $teams[$n]["points"][$point] = $time;
    }
    // Добавляем для станции на активной точке позже по дистанции факт считывания посещения нашей точки
    $mac = $row["sportiduino_stationmac"];
    $station_n = $row["sportiduino_stationnumber"];
    if (!isset($teams[$n]["registered"][$station_n])) {
        $teams[$n]["registered"][$station_n] = [];
    }
    if (!isset($teams[$n]["registered"][$station_n][$mac])) {
        $teams[$n]["registered"][$station_n][$mac] = [];
    }
    $teams[$n]["registered"][$station_n][$mac][$point] = 1;
    // Запоминаем MAC-адрес станции, на которой отметилась команда на активной точке
    if (!isset($team["stations"][$station_n])) {
        $teams[$n]["stations"][$station_n] = [];
    }
    $teams[$n]["stations"][$station_n][$mac] = 1;
    // Обновляем массив станций, работавших на дистанции
    if (!isset($stations[$mac])) {
        $stations[$mac] = [];
    }
    $stations[$mac][$station_n] = ["teams" => 0, "partial" => 0, "all" => 0, "fatal" => 0];
    // Добавляем точку в последовательность взятия КП командой
    $nvisited = count($teams[$n]["sequence"]);
    if (!$nvisited || ($teams[$n]["sequence"][$nvisited - 1] != $point)) {
        $teams[$n]["sequence"][$nvisited] = $point;
    }
}
$sql = null;

// Проверяем целостность данных
$teams_correct = 0;
$teams_with_errors = 0;
$ignored_teams = "";
foreach ($teams as $n => $team) {
    if (count($team["init"]) && (!count($team["sequence"]) || !count($team["points"]))) {
        $errors[] = "Команда $n получила чип, но не вышла на дистанцию";
        $teams[$n]["ignore"] = 1;
    }
    if (!count($team["sequence"]) && count($team["points"])) {
        die("Внутренняя ошибка: массив 'sequence' пустой для команды $n");
    }
    if (count($team["sequence"]) && !count($team["points"])) {
        die("Внутренняя ошибка: массив 'points' пустой для команды $n");
    }
    if (!count($team["sequence"])) {
        continue;
    }
    if ($teams[$n]["ignore"]) {
        $teams_with_errors++;
        $ignored_teams .= " N" . $n;
        continue;
    }
    $teams_correct++;
}

// Определяем на основе порядка взятия КП для каждой команды, с каких станций должна были попасть данные по каждому КП
$read_errors = 0;
$read_ok = 0;
$chips_errors = 0;
$chips_ok = 0;
foreach ($teams as $n => $team) {
    if (!count($team["sequence"])) {
        continue;
    }
    $chip_with_read_errors = false;
    // Составляем список станций, на которых должно засветиться отметка каждого взятого КП
    $reference = [];
    foreach ($team["sequence"] as $i => $point) {
        if ($points[$point]["active"]) {
            // При посещении активной точки в станцию должны были считаться все предыдущие отметки
            $reference[$point] = [];
            for ($j = 0; $j <= $i; $j++) {
                $reference[$point][$team["sequence"][$j]] = 1;
            }
        }
    }
    // Сверяем то, что должно быть, и то, что реально было считано станциями
    foreach ($reference as $station_n => $mandatory) {
        if (!isset($team["registered"][$station_n])) {
            // Отметка в чип записалась, но во все станции на активной точке вообще никаких данных не попало
            $errors[] = "Информация о посещении командой $n активной точки '" . $points[$station_n]["name"] . "' вообще не попала в станции на этой точке";
            $read_errors += count($mandatory);
            // Регистрируем ошибку чтения для всех станций, работавших на данной точке
            foreach ($stations as $mac => $locations) {
                foreach ($locations as $number => $values) {
                    if ($number == $station_n) {
                        $stations[$mac][$station_n]["teams"]++;
                        $stations[$mac][$station_n]["fatal"]++;
                    }
                }
            }
            $chip_with_read_errors = true;
            continue;
        }
        // Проверяем полноту копирования данных для всех станций, на которых отметилась команда на данной активной точке
        foreach ($team["registered"][$station_n] as $mac => $actual) {
            $partial = false;
            $all = true;
            foreach ($mandatory as $point => $value) {
                if (!isset($actual[$point])) {
                    $read_errors++;
                    $partial = true;
                    $chip_with_read_errors = true;
                } else {
                    $read_ok++;
                    if ($point != $station_n) {
                        $all = false;
                    }
                }
            }
            $stations[$mac][$station_n]["teams"]++;
            if ($partial) {
                if ($all) {
                    $stations[$mac][$station_n]["all"]++;
                } else {
                    $stations[$mac][$station_n]["partial"]++;
                }
            }
        }
    }
    if ($chip_with_read_errors) {
        $chips_errors++;
    } else {
        $chips_ok++;
    }
}

// Переводим время работы точек из unixtime
foreach ($points as $n => $point) {
    $points[$n]["start"] = date("Y-m-d H:i:s", $point["start"]);
    $points[$n]["end"] = date("Y-m-d H:i:s", $point["end"]);
}
// Корректируем время старта для нетерпеливых/опоздавших
foreach ($teams as $n => $team) {
    if ($team["ignore"]) {
        continue;
    }
    foreach ($team["points"] as $point => $time) {
        if ($time < $points[$point]["start"]) {
            if ($points[$point]["type"] == 1) {
                $teams[$n]["points"][$point] = $points[$point]["start"];
            } else {
                $errors[] = "отметка команды $n на точке $point сделана в $time до начала ее работы в " . $points[$point]["start"];
            }
        }
        if ($time > $points[$point]["end"]) {
            if ($points[$point]["type"] == 1) {
                $teams[$n]["points"][$point] = $points[$point]["end"];
            } else {
                $errors[] = "отметка команды $n на точке $point сделана в $time после окончания ее работы в " . $points[$point]["end"];
            }
        }
    }
}

// TODO - добавить в статистику станции, работавшие на выдаче чипов


// Вывод статистики и ошибок
// -------------------------

// Весь вывод идет в буфер
ob_start();

// Сообщаем об обнаруженных ошибках
foreach ($errors as $n => $error) {
    echo "$error\n";
}
echo "\n";

// Выводим общее количество команд с корректными данными на дистанции
echo "Корректные отметки на дистанции имеются у $teams_correct команд, у $teams_with_errors команд данные с ошибками\n";
echo "Команды с ошибками (их данные не загружены):" . $ignored_teams . "\n";

// Выводим список команд с несколькими чипами
if (count($chips_lost)) {
    echo "Команды, которым повторно выдавались чипы:";
    foreach ($chips_lost as $team_num) {
        echo " N$team_num";
    }
    echo "\n";
}

// Выводим количество команд с неявками и сходами
echo "Команд, в которых были неявки участников на старт: $nteams_members_absent\n";
echo "Команд, в которых были сходы участников на дистанции: $nteams_members_dismissed\n";

// Выводим статистику по станциям
$n_fatal = 0;
$n_all = 0;
$n_partial = 0;
echo "\n";
foreach ($stations as $mac => $numbers) {
    echo "Станция " . printMAC($mac) . "\n";
    foreach ($numbers as $point => $data) {
        $n_fatal += $data["fatal"];
        $n_all += $data["all"];
        $n_partial += $data["partial"];
        echo "    На точке '" . $points[$point]["name"] . "': команд " . $data["teams"] . ", фатальных ошибок " . $data["fatal"] . ", ошибок считывания " . $data["all"] . ", неполных считываний " . $data["partial"] . "\n";
    }
}
echo "Всего не считано отметок из-за ошибок: $read_errors из " . ($read_ok + $read_errors) . "\n";
echo "Всего $chips_errors чипов из " . ($chips_ok + $chips_errors) . " имели ошибки чтения\n";
echo "Статистика ошибок чтения: $n_fatal раз чип целиком не прочитан, $n_all раз из чипа не считаны отметки, $n_partial из чипа считана только часть отметок\n";

// Сохраняем весь вывод в переменную
$statustext = ob_get_clean();


// Удаление старых "публичных" результатов
// ---------------------------------------

// Временно отключаем индексы и проверки
$sql = $pdo->prepare("ALTER TABLE TeamLevelPoints DISABLE KEYS");
$sql->execute();
$sql = null;
$sql = $pdo->prepare("ALTER TABLE TeamLevelDismiss DISABLE KEYS");
$sql->execute();
$sql = null;
$sql = $pdo->prepare("SET FOREIGN_KEY_CHECKS = 0");
$sql->execute();
$sql = null;
$sql = $pdo->prepare("SET UNIQUE_CHECKS = 0");
$sql->execute();
$sql = null;
$sql = $pdo->prepare("SET AUTOCOMMIT = 0");
$sql->execute();
$sql = null;

// Удаляем отметки команд на точках
$sql = $pdo->prepare("DELETE TeamLevelPoints FROM TeamLevelPoints INNER JOIN LevelPoints WHERE TeamLevelPoints.levelpoint_id = LevelPoints.levelpoint_id AND distance_id = :distance_id");
$sql->bindParam("distance_id", $distance_id, PDO::PARAM_INT);
$sql->execute();
$sql = null;

// Удаляем сходы участников
$sql = $pdo->prepare("DELETE TeamLevelDismiss FROM TeamLevelDismiss INNER JOIN LevelPoints WHERE TeamLevelDismiss.levelpoint_id = LevelPoints.levelpoint_id AND distance_id = :distance_id");
$sql->bindParam("distance_id", $distance_id, PDO::PARAM_INT);
$sql->execute();
$sql = null;


// Запись "публичных" результатов
// ------------------------------

// Сохраняем отметки команд на точках
$sql = $pdo->prepare("INSERT INTO TeamLevelPoints (device_id, levelpoint_id, team_id, teamlevelpoint_datetime) VALUES (1, :levelpoint_id, :team_id, :teamlevelpoint_datetime)");
foreach ($teams as $n => $team) {
    if ($team["ignore"]) {
        continue;
    }
    foreach ($team["points"] as $point => $time) {
        $sql->bindParam("levelpoint_id", $points[$point]["id"], PDO::PARAM_INT);
        $sql->bindParam("team_id", $team["id"], PDO::PARAM_INT);
        $sql->bindParam("teamlevelpoint_datetime", $time, PDO::PARAM_STR);
        $sql->execute();
    }
}
$sql = null;

// Сохраняем сходы участников перед точками
$sql = $pdo->prepare("INSERT INTO TeamLevelDismiss (device_id, levelpoint_id, teamuser_id) VALUES (1, :levelpoint_id, :teamuser_id)");
foreach ($teams as $n => $team) {
    if ($team["ignore"]) {
        continue;
    }
    foreach ($team["dismiss"] as $member => $point) {
        $sql->bindParam("levelpoint_id", $points[$point]["id"], PDO::PARAM_INT);
        $sql->bindParam("teamuser_id", $members[$team["members"][$member]]["teamuser_id"], PDO::PARAM_INT);
        $sql->execute();
    }
}
$sql = null;

// Включаем индексы и проверки назад
$sql = $pdo->prepare("SET UNIQUE_CHECKS = 1");
$sql->execute();
$sql = null;
$sql = $pdo->prepare("SET FOREIGN_KEY_CHECKS = 1");
$sql->execute();
$sql = null;
$sql = $pdo->prepare("COMMIT");
$sql->execute();
$sql = null;
$sql = $pdo->prepare("ALTER TABLE TeamLevelPoints ENABLE KEYS");
$sql->execute();
$sql = null;
$sql = $pdo->prepare("ALTER TABLE TeamLevelDismiss ENABLE KEYS");
$sql->execute();
$sql = null;

// Готовим репорт о проведенном импорте
$statustext .= "\nОтметки команд и сходы участников сохранены в базу, не забудьте сделать пересчет результатов\n";
$statustext = "<pre>$statustext</pre>\n";

//=====================================================================================================================

// Сравниваем старую и новую маску и вычисляем списки сошедших и появившихся участников
function compare_masks($old_mask, $new_mask)
{
    $diff = ["removed" => [], "added" => []];
    $member = 1;
    for ($i = 0; $i < 16; $i++) {
        $old = $old_mask & $member;
        $new = $new_mask & $member;
        $member *= 2;
        if ($old == $new) {
            continue;
        }
        if ($new) {
            $diff["added"][] = $i;
        } else {
            $diff["removed"][] = $i;
        }
    }
    return $diff;
}

// Вывод MAC-адреса станции
function printMAC($long)
{
    $hex = substr("0000000000000000" . dechex($long), -16);
    $mac = substr($hex, 0, 2);
    for ($i = 1; $i < 8; $i++) {
        $mac .= ":" . substr($hex, $i * 2, 2);
    }
    return $mac;
}

?>