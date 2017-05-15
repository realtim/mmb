<?php

// Проверка, как именно используем скрипт: из интерфейса или отдельно
if (isset($MyPHPScript) and $action == 'LoadRaidDataFile')
{
	if (!$Administrator && !$Moderator) return;
}
else 
{
	// Общие настройки
	include("settings.php");
	// Библиотека функций
	include("functions.php");

	print('<html>');
	print('<head>');
	print('<title>Импорт данных с Android</title>');
	print('<link rel="Stylesheet" type="text/css" href="styles/mmb.css" />');
	print('<meta http-equiv="Content-Type" content="text/html; charset=utf-8">');
	print('</head>');
	print('<body>');

	print('<form enctype="multipart/form-data" action="import.php" method="POST">');
	print('<input type="hidden" name="MAX_FILE_SIZE" value="2000000" />');
	print('Файл с данными: <input name="android" type="file" /> &nbsp;');
	print('<input type="submit" value="Загрузить" />');
	print('</form>');
}
// Конец проверки, как именно используем скрипт: из интерфейса или отдельно


// Устанавливаем часовой пояс по умолчанию
date_default_timezone_set("Europe/Moscow");
// Подключаемся к базе
$ConnectionId = mysql_connect($ServerName, $WebUserName, $WebUserPassword);
if ($ConnectionId <= 0) die(mysql_error());
// Устанавливаем кодировку для взаимодействия
mysql_query('set names \'utf8\'', $ConnectionId);
// Выбираем БД ММБ
if (mysql_select_db($DBName, $ConnectionId) == "") die(mysql_error());


// Обработка загруженного файла
if (isset($_FILES['android']))
{
	if ($_FILES["android"]["error"] > 0) die("Загрузка файла на сервер не удалась, код ошибки php: ".$_FILES["android"]["error"]);
	echo "<br>Загружено ".$_FILES["android"]["size"] . " байт<br>\n";
	$lines = file($_FILES['android']['tmp_name'], FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);

	// ====== В первый цикл просто проверяем данные, ничего не записывая в базу
	$type = "";
	$password = "";
	$valid_operators = array();
	$valid_devices = array();
	$saved_team_time = array();
	$saved_teamuser_id = array();
	$saved_levelpoint_order = array();
	foreach ($lines as $line_num => $line)
	{
		// Проверка существования пользователя, от имени которого загружаем
		if ($line_num == 0)
		{
			if (is_numeric($line)) $author_id = intval($line); else $author_id = -1;
			if ($author_id <= 0) die("Некорректный автор файла данных $line");
			$sql = "SELECT user_password FROM Users WHERE user_id = $author_id AND user_hide = 0";
			$Result = mysql_query($sql);
			if (!$Result) die("Автор файла данных $line отсутствует в базе");
			$Row = mysql_fetch_assoc($Result);
			if (isset($Row['user_password'])) $password = $Row['user_password'];
			mysql_free_result($Result);
			continue;
		}
		// Проверка пароля пользователя, от имени которого загружаем
		if ($line_num == 1)
		{
			if (!$password) die("В базе у автора файла данных пустой пароль");
			if ($line <> $password) die("Пароль автора файла данных '$line' неправильный");
			continue;
		}
		// Смена типа данных
		if ($line == "---TeamLevelPoints")
		{
			$type = "TeamLevelPoints";
			continue;
		}
		else if ($line == "---TeamLevelDismiss")
		{
			$type = "TeamLevelDismiss";
			continue;
		}
		else if ($line == "end")
		{
			$type = "end";
			continue;
		}

		// Если оказались здесь - обрабатываем строчку с данными
		if (!$type) die("Нет предварительного указания на тип данных (TeamLevelPoints/TeamLevelDismiss) в строке #".$line_num." - ".$line);
		if ($type == "end") die("Данные после строки 'end' в строке #".$line_num." - ".$line);

		// Единая проверка валидности оператора, который ввел данные,
		// его планшета и контрольной точки, где происходил ввод
		if (($type == "TeamLevelDismiss") || ($type == "TeamLevelPoints"))
		{
			// Получаем переменные
			$values = explode(';', $line);
			if (count($values) <> 6) die("Некорректное число параметров в строке #".$line_num." - ".$line);
			foreach ($values as &$value) $value = mysql_real_escape_string(trim($value, '"'));
			if (is_numeric($values[0])) $operator_id = intval($values[0]); else $operator_id = -1;
			if ($type == "TeamLevelPoints") $device_id = $values[4]; else $device_id = $values[5];
			if (is_numeric($device_id)) $device_id = intval($device_id); else $device_id = -1;
			if (is_numeric($values[1])) $point_id = intval($values[1]); else $point_id = -1;
			if (is_numeric($values[2])) $team_id = intval($values[2]); else $team_id = -1;
			if ($type == "TeamLevelPoints") $edit_time = $values[3]; else $edit_time = $values[4];
			if ($type == "TeamLevelPoints") $team_time = $values[5]; else $team_time = "";
			if ($type == "TeamLevelPoints") $user_id = -1; else $user_id = $values[3];
			if (is_numeric($user_id)) $user_id = intval($user_id); else $user_id = -1;

			// Проверяем наличие в базе оператора данных
			if (!in_array($operator_id, $valid_operators))
			{
				$sql = "SELECT user_id FROM Users WHERE user_id = $operator_id AND user_hide = 0";
				$Result = mysql_query($sql);
				if (!$Result || mysql_num_rows($Result) <> 1)
					die("Несуществующий или удаленный автор данных $operator_id в строке #".$line_num." - ".$line);
				mysql_free_result($Result);
				$valid_operators[] = $operator_id;
			}
			// Проверяем наличие в базе устройства для ввода данных
			if (!in_array($device_id, $valid_devices))
			{
				$sql = "SELECT device_id FROM Devices WHERE device_id = $device_id";
				$Result = mysql_query($sql);
				if (!$Result || mysql_num_rows($Result) <> 1)
					die("Несуществующее устройство ввода данных $device_id в строке #".$line_num." - ".$line);
				mysql_free_result($Result);
				$valid_devices[] = $device_id;
			}
			// Проверяем наличие активной точки в базе
			$sql = "SELECT pointtype_id, levelpoint_order, distance_id, levelpoint_mindatetime, levelpoint_maxdatetime, scanpoint_id
				FROM LevelPoints WHERE levelpoint_id = $point_id AND levelpoint_hide = 0";
			$Result = mysql_query($sql);
			if (!$Result || mysql_num_rows($Result) <> 1)
				die("Несуществующая или удаленная точка $point_id в строке #".$line_num." - ".$line);
			$Row = mysql_fetch_assoc($Result);
			$pointtype_id = $Row['pointtype_id'];
			$levelpoint_order = $Row['levelpoint_order'];
			$distance_id = $Row['distance_id'];
			$begtime = $Row['levelpoint_mindatetime'];
			$endtime = $Row['levelpoint_maxdatetime'];
			$scanpoint_id = $Row['scanpoint_id'];
			mysql_free_result($Result);
			// Проверяем, что данные зарегистрированы на точке с известным скрипту типом
			if (($pointtype_id < 1) || ($pointtype_id > 5))
				die("Неподдерживаемый тип точки $pointtype_id в строке #".$line_num." - ".$line);

			// Проверяем наличие неудаленной команды в базе
			$sql = "SELECT distance_id, team_outofrange FROM Teams WHERE team_id = $team_id AND team_hide = 0";
			$Result = mysql_query($sql);
			if (!$Result || mysql_num_rows($Result) <> 1)
				die("Несуществующая или удаленная команда $team_id в строке #".$line_num." - ".$line);
			$Row = mysql_fetch_assoc($Result);
			// Проверяем, что команда могла оказаться на этой точке
			if ($distance_id != $Row['distance_id'])
				die("Команда с чужой дистанции ".$Row['distance_id']." на точке дистанции $distance_id в строке #".$line_num." - ".$line);
			// Проверяем, что команда не выступает вне зачета
			if ($Row['team_outofrange'] >= 1)
				die("Команда $team_id вне зачета в строке #".$line_num." - ".$line);
			mysql_free_result($Result);

			// проверяем на корректность время редактирования данных
			$timestamp = strtotime($edit_time);
			if ($timestamp === false)
				die("Некорректное время редактирования данных '$edit_time' в строке #".$line_num." - ".$line);
			if ($edit_time <> date("Y-m-d H:i:s", $timestamp))
				die("Нестандартный формат времени редактирования данных '$edit_time' в строке #".$line_num." - ".$line);
		}
		else die("Неизвестный тип данных '".$type."' в строке #".$line_num." - ".$line);

		// Данные о сходе участников на контрольной точке
		if ($type == "TeamLevelDismiss")
		{
			// Проверяем, является ли сошедший участник действующим членом команды
			$sql = "SELECT teamuser_id FROM TeamUsers WHERE team_id = $team_id AND user_id = $user_id AND teamuser_hide = 0";
			$Result = mysql_query($sql);
			if (!$Result || mysql_num_rows($Result) <> 1)
				die("Сошедший участник $user_id отсутствует или удален в его команде $team_id в строке #".$line_num." - ".$line);
			// Сохраняем teamuser_id и levelpoint_order для второго прохода скрипта
			$Row = mysql_fetch_assoc($Result);
			$saved_teamuser_id[$line_num] = $Row['teamuser_id'];
			$saved_levelpoint_order[$line_num] = $levelpoint_order;
			mysql_free_result($Result);
			// Проверяем, что сход зарегистрирован на точке с судьями, а не на обычной точке с компостером
			if ($pointtype_id == 5)
				die("Сход участника $user_id на точке $point_id с обычным компостером (pointtype_id=5) в строке #".$line_num." - ".$line);
		}

		// Данные о командах на контрольной точке
		if ($type == "TeamLevelPoints")
		{
			// проверяем на корректность формат времени команды
			if ($team_time <> "NULL")
			{
				$timestamp = strtotime($team_time);
				if ($timestamp === false)
					die("Некорректное время команды '$team_time' в строке #".$line_num." - ".$line);
				if ($team_time <> date("Y-m-d H:i:s", $timestamp))
					die("Нестандартный формат времени команды '$team_time' в строке #".$line_num." - ".$line);
				// Проверяем, что время обработки результата на планшете >= времени прихода команды
				if ($edit_time < $team_time)
					die("Время обработки результата на планшете $edit_time меньше времени посещения точки $team_time в строке #".$line_num." - ".$line);
			}
			// Проверяем само время прихода команды на точку
			switch ($pointtype_id)
			{
				// Корректируем при необходимости время старта
				case 1:
					// При одновременном старте игнорируем время со сканера, оно может быть и до, и после старта
					if ($begtime == $endtime) $team_time = $begtime;
					// Если карточка отсканирована после закрытия старта, то ставим команде время закрытия
					if ($team_time > $endtime) $team_time = $endtime;
				// Время прихода на активную точку должно быть в интервале ее работы
				case 2:
				case 3:
				case 4:
					if ($team_time <> "NULL")
					{
						if (!$scanpoint_id)
							die("Для активной точки без сканера (scanpoint_id=0) не должно быть отметки времени в строке #".$line_num." - ".$line);
						if ($team_time < $begtime)
							die("Команда $team_id отметилась на точке $point_id до начала ее работы в строке #".$line_num." - ".$line);
						if ($team_time > $endtime)
							die("Команда $team_id отметилась на точке $point_id после окончания ее работы в строке #".$line_num." - ".$line);
					}
					else
					{
						if ($scanpoint_id  && ($pointtype_id <> 3))
							die("У команды $team_id отсутствует время на активной точке $point_id (pointtype_id=$pointtype_id) в строке #".$line_num." - ".$line);
					}
					break;
				// На обычной точке без сканера время прихода команды может быть только NULL
				case 5:
					if ($team_time <> "NULL")
						die("Для КП с компостером (pointtype_id=5) не должно быть отметки времени в строке #".$line_num." - ".$line);
					break;
			}
			// Сохраняем проверенное и откорректированное время прихода команды для второго прохода скрипта
			$saved_team_time[$line_num] = $team_time;
		}
	}
	// Проверяем, что в конце файла был end
	if ($line <> "end") die("В конце файла отсутствует 'end'");

	// ====== Если добрались сюда - все данные корректные, можно сохранять в TeamLevelPoints/TeamLevelDismiss
	echo "Проверка данных завершилась успешно<br>\n";
	flush();

	// Повторно сканируем файл и берем из него данные с минимумом проверок
	$n_new = $n_updated = $n_unchanged = 0;
	$d_new = $d_updated = $d_unchanged = 0;
//	$d_dismiss = 0;
	foreach ($lines as $line_num => $line)
	{
		// Логин и пароль уже не проверяем
		if (($line_num == 0) || ($line_num == 1)) continue;
		// Смена типа данных
		if ($line == "---TeamLevelPoints")
		{
			$type = "TeamLevelPoints";
			continue;
		}
		else if ($line == "---TeamLevelDismiss")
		{
			$type = "TeamLevelDismiss";
			continue;
		}
		else if ($line == "end") continue;

		// Получаем переменные
		$values = explode(';', $line);
		foreach ($values as &$value) $value = mysql_real_escape_string(trim($value, '"'));
		$operator_id = intval($values[0]);
		if ($type == "TeamLevelPoints") $device_id = intval($values[4]); else $device_id = intval($values[5]);
		$point_id = intval($values[1]);
		$team_id = intval($values[2]);
		if ($type == "TeamLevelPoints") $edit_time = $values[3]; else $edit_time = $values[4];
		if ($type == "TeamLevelPoints") $team_time = $values[5]; else $team_time = "";
		if ($type == "TeamLevelPoints") $user_id = -1; else $user_id = intval($values[3]);

		// Данные о сходе участников на контрольной точке
		if ($type == "TeamLevelDismiss")
		{
			// Используем teamuser_id и levelpoint_order из первого прохода скрипта
			$teamuser_id = $saved_teamuser_id[$line_num];
			$levelpoint_order = $saved_levelpoint_order[$line_num];
			// Выясняем, есть ли уже запись о сходе этого участника
			$sql = "SELECT levelpoint_order FROM TeamLevelDismiss, LevelPoints".
				" WHERE LevelPoints.levelpoint_id = TeamLevelDismiss.levelpoint_id AND teamuser_id = $teamuser_id".
				" ORDER BY levelpoint_order ASC LIMIT 1";
			$Result = mysql_query($sql);
			unset($Old);
			$Old = mysql_fetch_assoc($Result);
			if (isset($Old['levelpoint_order']))
			{
				// обновляем запись в базе только если точка схода из файла раньше, чем в базе
				if ($Old['levelpoint_order'] <= $levelpoint_order) $Record = "unchanged";
				else $Record = "updated";
			}
			else $Record = "new";
			mysql_free_result($Result);
			if ($Record == "updated")
			{
				// Участник сошел раньше, чем было известно до этого
				// Удаляем все старые записи о сходе участника (теоретически это всего одна запись)
				$d_updated++;
				$sql = "DELETE FROM TeamLevelDismiss WHERE teamuser_id = $teamuser_id";
				mysql_query($sql);
				if (mysql_error()) die($sql.": ".mysql_error());
			}
			if (($Record == "new") || ($Record == "updated"))
			{
				// Вставляем новую/обновленную запись о сходе участника
				if ($Record == "new") $d_new++;
				$sql = "INSERT INTO TeamLevelDismiss (teamleveldismiss_date, user_id, device_id, levelpoint_id, teamuser_id)
					VALUES ('$edit_time', $operator_id, $device_id, $point_id, $teamuser_id)";
				mysql_query($sql);
				if (mysql_error()) die($sql.": ".mysql_error());
			}
			else
			{
				// дубль, игнорируем
				$d_unchanged++;
				continue;
			}
			// =============== Данные импортировали, теперь обновляем другие таблицы на основе импортированной записи
			// В будущем этот блок можно будет удалить
			//
/*			// Выясняем порядковый номер контрольной точки, на которой зарегистрирован сход
			$sql = "SELECT levelpoint_order FROM LevelPoints WHERE levelpoint_id = $point_id";
			$Result = mysql_query($sql);
			$Row = mysql_fetch_assoc($Result);
			$new_levelpoint_order = $Row['levelpoint_order'];
			mysql_free_result($Result);
			// Ищем в базе уже зарегистрированные сходы члена команды и берем из них точку с наименьшим порядковым номером
                        $sql = "SELECT MIN(levelpoint_order) AS old FROM TeamLevelDismiss, LevelPoints
				WHERE teamuser_id = $teamuser_id AND TeamLevelDismiss.levelpoint_id = LevelPoints.levelpoint_id";
			$Result = mysql_query($sql);
			$Row = mysql_fetch_assoc($Result);
			$old_levelpoint_order = $Row['old'];
			mysql_free_result($Result);
			// Если в импортированных данных участник сошел раньше, чем в базе, или в базе сходов не было - обновляем базу
			if (($old_levelpoint_order == "") || ($new_levelpoint_order < $old_levelpoint_order))
			{
				$sql = "UPDATE TeamUsers SET levelpoint_id = $point_id WHERE teamuser_id = $teamuser_id";
				mysql_query($sql);
				if (mysql_error()) die($sql.": ".mysql_error());
				$d_dismiss++;
			}*/
		}

		// Данные о командах на контрольной точке
		if ($type == "TeamLevelPoints")
		{
			// Используем откорректированное время команды из первого прохода скрипта
			if (isset($saved_team_time[$line_num])) $team_time = $saved_team_time[$line_num];
			// Выясняем, есть ли уже такая запись
			$sql = "SELECT teamlevelpoint_date, teamlevelpoint_datetime FROM TeamLevelPoints WHERE levelpoint_id = $point_id AND team_id = $team_id";
			$Result = mysql_query($sql);
			unset($Old);
			$Old = mysql_fetch_assoc($Result);
			if (isset($Old['teamlevelpoint_date']))
			{
				// обновляем запись в базе только если дата ввода данных в файле больше, чем в базе
				if ($Old["teamlevelpoint_datetime"] && ($Old["teamlevelpoint_datetime"] <> "0000-00-00 00:00:00") && ($Old['teamlevelpoint_date'] >= $edit_time)) $Record = "unchanged";
				else $Record = "updated";
				// не заменяем реальное время команды на пустое даже если пустое время было введено позже
				if (($Record == "updated") && $Old["teamlevelpoint_datetime"] && ($team_time == "NULL")) $Record = "unchanged";
			}
			else $Record = "new";
			mysql_free_result($Result);
			// Если записи раньше не было - вставляем ее в таблицу
			if ($Record == "new")
			{
				// новая запись о результате
				$n_new++;
				$sql = "INSERT INTO TeamLevelPoints (teamlevelpoint_date, user_id, device_id, levelpoint_id, team_id, teamlevelpoint_datetime)
					VALUES ('$edit_time', $operator_id, $device_id, $point_id, $team_id, '$team_time')";
				mysql_query($sql);
				if (mysql_error()) die($sql.": ".mysql_error());
			}
			elseif ($Record == "updated")
			{
				// измененная запись о результате
				$n_updated++;
				$sql = "UPDATE TeamLevelPoints SET teamlevelpoint_date = '$edit_time', user_id = $operator_id,
					device_id = $device_id, teamlevelpoint_datetime = '$team_time'
					WHERE levelpoint_id = $point_id AND team_id = $team_id";
				mysql_query($sql);
				if (mysql_error()) die($sql.": ".mysql_error());
			}
			else
			{
				// дубль, игнорируем
				$n_unchanged++;
				continue;
			}
		}
	}
	echo "Команды: $n_new результатов добавлено, $n_updated изменено, $n_unchanged являются дубликатами<br>\n";
	echo "Данные о сходах: $d_new добавлено, $d_updated изменено, $d_unchanged являются дубликатами<br>\n";
//	echo "У $d_dismiss участников изменена информация о первой точке схода<br>\n";
	// Определяем id марш-броска по последней обработанной команде
/*	$sql = "SELECT raid_id FROM Teams, Distances WHERE team_id = $team_id AND Teams.distance_id = Distances.distance_id";
	$Result = mysql_query($sql);
	if (mysql_error()) die($sql.": ".mysql_error());
	$Row = mysql_fetch_assoc($Result);
	$raid_id = $Row['raid_id'];
	mysql_free_result($Result);
	// Пересчитываем общие результаты команд для данного марш-броска
	RecalcTeamResultFromTeamLevelPoints($raid_id, "");
	echo "Результаты марш-броска пересчитаны<br>\n&nbsp;";*/
	echo "&nbsp;<br><strong>Не забудьте нажать на кнопку &quot;Пересчитать результаты&quot; после того, как загрузите все файлы с планшетов</strong><br>\n&nbsp;";
}


if (!isset($MyPHPScript) or $action <> 'LoadRaidDataFile')
{
	print('</body>');
	print('</html>');
}
?>
