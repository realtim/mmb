<?php

/**
 * +++++++++++ Обработчик действий, связанных с командой +++++++++++++++++++++
 *
 * @var string|null $action
 * @var int|null $TeamId
 * @var int|null $RaidId
 * @var int|null $UserId
 * @var int<0,1>|null $Administrator
 * @var int<0,1>|null $Moderator
 */

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) {
    return;
}

// Для всех обработок, кроме ViewRaidTeams,
// требуем, чтобы пользователь вошёл в систему


function setViewError($message): void
{
    CMmb::setError($message, 'ViewTeamData', 'ReturnAfterError');
}

function setUnionError($message): void
{
    global $viewmode;       // пока так
    $viewmode = "";

    CMmb::setError($message, 'ViewAdminUnionPage', 'ReturnAfterError');
}

// ============ Обработка возможности регистрации команды =====================
if ($action === "RegisterNewTeam") {
    CMmb::setViews('ViewTeamData', 'Add');
    if ($RaidId <= 0) {
        CMmb::setMessage('Не указан ММБ (выберите из списка).');
        return;
    }

    // Проверка возможности создать команду
    //	if (!CanCreateTeam($Administrator, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange))
    if (!CRights::canCreateTeam($UserId, $RaidId)) {
        CMmb::setMessage('Регистрация на марш-бросок закрыта');
        return;
    }

    // Проверка, что пользователь не включен в команду на выбранном ММБ
    $sql = "select t.team_num
		from TeamUsers tu
			inner join Teams t on tu.team_id = t.team_id
			inner join Distances d on t.distance_id = d.distance_id
		where d.raid_id = $RaidId and tu.teamuser_hide = 0 and tu.user_id = $UserId";
    $TeamNum = CSql::singleValue($sql, 'team_num', false);
    if ($TeamNum > 0) {
        CMmb::setMessage("Уже есть команда c Вашим участием (N $TeamNum)");
        return;
    }
} // ============ Изменение данных команды или запись новой команды =============
elseif ($action === 'TeamChangeData' || $action === "AddTeam") {
    //$view = "ViewTeamData";
    CMmb::setViews($_POST['view'], $action === "AddTeam" ? 'Add' : '');      // ой, как нехорошо с $_POST!

    // пока валим всё в одну кучу - проверяем ниже
    $pDistanceId = (int)$_POST['DistanceId'];
    $pTeamNum = (int)$_POST['TeamNum'];
    $pTeamName = $_POST['TeamName'];
    $pTeamUseGPS = mmb_isOn($_POST, 'TeamUseGPS');

    // Ниже  доп. проверка на права и определение этого флага при записи данных
//	$pTeamOutOfRange = mmb_isOn($_POST, 'TeamOutOfRange');
    $pTeamMapsCount = (int)$_POST['TeamMapsCount'];
    $pTeamGreenPeace = mmb_isOn($_POST, 'TeamGreenPeace');
    $pNewTeamUserEmail = mmb_validate($_POST, 'NewTeamUserEmail', '');
    $pTeamConfirmation = mmb_isOn($_POST, 'Confirmation');


    if (($action !== "AddTeam") && ($TeamId <= 0)) {
        setViewError("Не найден идентификатор команды.");
        return;
    }
    if (empty($pDistanceId)) {
        setViewError("Не указана дистанция.");
        return;
    }
    if ($RaidId <= 0) {
        setViewError("Не указан ММБ.");
        return;
    }
    if (trim($pTeamName) === '' || trim($pTeamName) === 'Название команды') {
        setViewError("Не указано название.");
        return;
    }
    // 20/05/2014 Добавил проверку на угловые скобки
    if (strpos($pTeamName, '>') !== false || strpos($pTeamName, '<') !== false) {
        setViewError("Название не должно содержать угловых скобок.");
        return;
    }

    if (($pTeamMapsCount <= 0) || ($pTeamMapsCount > 15)) {
        setViewError("Не указано число карт или недопустимое число карт.");
        return;
    }

    if (($action === "AddTeam") && ($pTeamConfirmation === 0)) {
        setViewError("Подтвердите, что прочитали и согласны с правилами участия в ММБ.");
        return;
    }

    // Проверяем, нет ли уже команды с таким названием
    $sql = "select count(*) as resultcount
			from Teams t
				inner join Distances d
				on t.distance_id = d.distance_id
			where d.raid_id = $RaidId and trim(team_name) = '$pTeamName' and team_hide = 0 and team_id <> $TeamId";
    if (CSql::singleValue($sql, 'resultcount') > 0) {
        setViewError("Уже есть команда с таким названием.");
        return;
    }

    // Проверяем номер команды: если новая - 0 и такого номера не должно быть
    $sql = "select count(*) as resultcount
			from Teams t
				inner join Distances d
				on t.distance_id = d.distance_id
			where d.raid_id = $RaidId and team_num = $pTeamNum and team_hide = 0 and team_id <> $TeamId";

    if (CSql::singleValue($sql, 'resultcount') > 0) {
        setViewError("Уже есть команда с таким номером.");
        return;
    }

    // Проверяем email нового участника команды
    if (!empty($pNewTeamUserEmail) && trim($pNewTeamUserEmail) !== 'Email нового участника') {
        $sql = "select user_id, user_prohibitadd from Users where ltrim(COALESCE(user_password, '')) <> '' and user_hide = 0 and trim(user_email) = trim('$pNewTeamUserEmail')";
        $Row = CSql::singleRow($sql);
        $NewUserId = $Row['user_id'];
        $UserProhibitAdd = $Row['user_prohibitadd'];
        if (empty($NewUserId)) {
            setViewError('Пользователь с таким email не найден.');
            return;
        }
        // Проверка на запрет включения в команду
        if ($UserProhibitAdd && $NewUserId <> $UserId && !$Moderator) {
            $NewUserId = 0;
            setViewError('Пользователь запретил добавлять себя в команду другим пользователям.');
            return;
        }
        // Проверка на наличие пользователя в другой команде
        $sql = "select count(*) as result
			from TeamUsers tu
				inner join Teams t on tu.team_id = t.team_id
				inner join Distances d on t.distance_id = d.distance_id
			where teamuser_hide = 0 and d.raid_id = $RaidId and user_id = $NewUserId";
        if (CSql::singleValue($sql, 'result') > 0) {
            $NewUserId = 0;
            setViewError('Пользователь с таким email уже включен в другую команду');
            return;
        }

        // Проверка на наличие пользователя в судьях
        $sql = "select count(*) as result
			from RaidDevelopers rd
			where rd.raiddeveloper_hide = 0 and rd.raid_id = $RaidId and rd.user_id = $NewUserId";
        if (CSql::singleValue($sql, 'result') > 0) {
            $NewUserId = 0;
            setViewError('Пользователь с таким email включен в судьи и не может быть участником');
            return;
        }

        // Проверка на возможность вставки ещё одного участника (ограничение в 10 участников)
        if (!CRights::canAddTeamUser($UserId, $RaidId, $TeamId)) {
            $NewUserId = 0;
            setViewError('Добавление участника запрещено');
            return;
        }
    } else {
        // Проверяем, что для новой команды передали email участника
        if ($action === "AddTeam") {
            $NewUserId = 0;
            setViewError('Для новой команды должен быть указан email участника.');
            return;
        }
        $NewUserId = 0;
    } // Конец проверки на корректную передачу email

    // 21.05.2016 Проверка на права правки и добавления команды
    if ($action === 'AddTeam' && !CRights::canCreateTeam($UserId, $RaidId)) {
        setViewError('Регистрация команды запрещена');
        return;
    }

    if ($action === 'TeamChangeData' && !CRights::canEditTeam($UserId, $RaidId, $TeamId)) {
        setViewError('Правка команды запрещена');
        return;
    }


    // 09/06/2016 Добавил определение нового пользователя.
    // Определяем ключ предыдущего марш-броска, в который данный пользователь заявлялся, но не участвовал
    $NotStartPreviousRaidId = 0;
    $TeamUserNew = 0;
    if ($NewUserId > 0) {
        // смотрим минимальный, максимальный ммб, в котором пользователь участвовал и максимальный, в котором он регистрировался, но не вышел на старт
        // эти поля обновляются при пересечёте рейтинга
        // Если пользователь имеет минимальный - значит не новичок
        // Если ммб не участия больше чем максимальный - его и заносим, как последний неучастия (не перерытый участием)
        $sqlUser = "select COALESCE(u.user_minraidid, 0) as minraidid,  
				COALESCE(u.user_maxraidid, 0) as maxraidid,  
				COALESCE(u.user_maxnotstartraidid, 0) as maxnotstartraidid,
				COALESCE(u.user_amateur, 1) as amateur  
			from Users u
			where u.user_id = $NewUserId";

        $RowUser = CSql::singleRow($sqlUser);

        $NotStartPreviousRaidId = ($RowUser['maxnotstartraidid'] > $RowUser['maxraidid']) ? $RowUser['maxnotstartraidid'] : 0;
        $TeamUserNew = ($RowUser['amateur'] > 0) ? 1 : 0;
    }


//	$OutOfRaidLimit =  IsOutOfRaidLimit($RaidId);
//	$WaitTeamId = FindFirstTeamInWaitList($RaidId);

    // Добавляем/изменяем команду в базе
    $TeamActionTextForEmail = "";
    // Новая команда
    if ($action === "AddTeam") {
        /*
                // Дополнительная проверка на флаг.  Если регистрация закончена, то
                if (CSql::raidStage($RaidId) >= 2 OR $OutOfRaidLimit > 0 OR $WaitTeamId > 0)  {
                    $TeamOutOfRange = 1;
                } else {
                    $TeamOutOfRange = 0;
                }
        */

        // 01.06.2016 все команды вне зачета
        $TeamOutOfRange = 1;

        $sql = "insert into Teams (team_num, team_name, team_usegps, team_mapscount, distance_id,
			team_registerdt, team_greenpeace, team_outofrange, team_waitdt)
			values ((select COALESCE(MAX(t.team_num), 0) + 1
				from Teams t
					inner join Distances d on t.distance_id = d.distance_id
				where d.raid_id = $RaidId), 
				'$pTeamName', $pTeamUseGPS, $pTeamMapsCount, $pDistanceId, NOW(),
				$pTeamGreenPeace, $TeamOutOfRange, NULL)
			
			";
        /*
                // Если превышен лимит команл или есть команда в списке ожидания, то не регистрируем в зачет
                if ($OutOfRaidLimit > 0 OR $WaitTeamId > 0) {
                    $sql .= ", NOW())";
                } else {
                    $sql .= ", NULL)";
                }
        */
        // При insert должен вернуться послений id - это реализовано в MySqlQuery
        $TeamId = MySqlQuery($sql);
        if ($TeamId <= 0) {
            setViewError('Ошибка записи новой команды.');
            return;
        }
        $sql = "insert into TeamUsers (team_id, user_id, teamuser_notstartraidid, teamuser_new) values ($TeamId, $NewUserId, $NotStartPreviousRaidId, $TeamUserNew)";
        MySqlQuery($sql);

        // Поменялся TeamId, заново определяем права доступа
        GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange);

        // 27/01/2017 пересчет данных об участниках в команде
        RecalcTeamUsersStatistic(0, $TeamId);

        $TeamActionTextForEmail = "создана команда";
        $SendEmailToAllTeamUsers = 1;
        // Теперь нужно открыть на просмотр
        $viewmode = "";
    } else // Изменения в уже существующей команде
    {
        $TeamActionTextForEmail = "изменение данных команды";
        $SendEmailToAllTeamUsers = 0;

        $sql = "update Teams set team_name = trim('$pTeamName'),
					distance_id = $pDistanceId,
					team_usegps = $pTeamUseGPS,
					team_greenpeace = $pTeamGreenPeace,
					team_mapscount = $pTeamMapsCount
			where team_id = $TeamId";

        $rs = MySqlQuery($sql);

// 14/06/2016 Убрал обработку поля "вне зачета"	 - перевод в зачет только через приглашения
        /*
                        // Проверка, на правку поля "Вне зачета"
                        if (CanEditOutOfRange($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange))
                {
                    $sql = "update Teams set team_outofrange = $pTeamOutOfRange
                        where team_id = $TeamId";

                    $rs = MySqlQuery($sql);

                        // Перезапрашиваем права и тип команды
                    GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange);

                }
                // Конец проверки на право правки "Вне зачета"
        */
        // Если добавляли участника
        if ($NewUserId > 0) {
            $sql = "insert into TeamUsers (team_id, user_id, teamuser_notstartraidid, teamuser_new) values ($TeamId, $NewUserId, $NotStartPreviousRaidId, $TeamUserNew)";
            MySqlQuery($sql);
            $TeamActionTextForEmail = "добавлен участник " . CSql::userName($NewUserId);
        }

        // 27/01/2017 пересчет данных об участниках в команде
        RecalcTeamUsersStatistic(0, $TeamId);
    }
    // Конец разных вариантов действий при создании и редактировании команды

    // Обновляем результат команды (реально нужно только при изменения этапа невыхода команды)
//	if ($UserId > 0 and $TeamId > 0) RecalcTeamResult($TeamId);

// Если пересчитывать, то так
//RecalcTeamResultFromTeamLevelPoints(0, $TeamId)

    // Отправляем письмо всем участникам команды об изменениях
    // Кроме того, кто вносил изменения, если $SendEmailToAllTeamUsers <> 1
    if ($UserId > 0 and $TeamId > 0) {
        $ChangeDataUserName = CSql::userName($UserId);
        $userCond = $SendEmailToAllTeamUsers <> 1 ? "u.user_id <> $UserId" : "true";
        $sql = "select u.user_email, u.user_name, t.team_num, d.distance_name, r.raid_name
			from Users u
				inner join TeamUsers tu on tu.user_id = u.user_id
				inner join Teams t on tu.team_id = t.team_id
				inner join Distances d on t.distance_id = d.distance_id
				inner join Raids r on d.raid_id = r.raid_id
			where tu.teamuser_hide = 0 and tu.team_id = $TeamId
				and $userCond
			order by tu.teamuser_id asc";

        $Result = MySqlQuery($sql);
        while ($Row = mysqli_fetch_assoc($Result)) {
            // Формируем сообщение
            $Msg = "Уважаемый участник {$Row['user_name']}!\n\n";
            $Msg .= "Действие: $TeamActionTextForEmail.\n";
            $Msg .= "Команда N {$Row['team_num']}, Дистанция: {$Row['distance_name']}, ММБ: " . trim($Row['raid_name']) . ".\n";
            $Msg .= "Автор изменений: $ChangeDataUserName.\n";
            $Msg .= "Вы можете увидеть результат на сайте и при необходимости внести свои изменения.\n\n";
            $Msg .= "P.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
            // Отправляем письмо
            SendMail($Row['user_email'], $Msg, $Row['user_name']);
        }
        mysqli_free_result($Result);
    }
    // Конец отправки писем

    // Если передали альтернативную страницу, на которую переходить (пока только одна возможность - на список команд)
    if (empty($view)) {
        $view = "ViewTeamData";
    }
} // ============ Информация о команде по номеру ================================
elseif ($action === 'FindTeam') {
    $TeamNum = mmb_validateInt($_REQUEST, 'TeamNum', '');
    if ($TeamNum === false) {
        CMmb::setShortResult('Не указан номер команды', '');
        return;
    }
    $sql = "select team_id from Teams t
			inner join Distances d on t.distance_id = d.distance_id
		where d.raid_id = $RaidId and t.team_hide = 0 and t.team_num = " . (int)$TeamNum;

    $TeamId = CSql::singleValue($sql, 'team_id', false);
    // Поменялся TeamId, заново определяем права доступа
    GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange);
    if ($TeamId <= 0) {
        CMmb::setResult('Команда с номером ' . (int)$TeamNum . ' не найдена', '');
        return;
    }
    $view = "ViewTeamData";
} elseif ($action === 'TeamInfo' || $action === 'TlpInfo' || $action === 'AddTlp' || $action === 'ChangeTlp' || $action === 'HideTlp') {
// ============ Информация о команде по Id ====================================
// Пропускаем также события для редактирования результата
    if ($TeamId <= 0) {
        CMmb::setErrorMessage('Id команды не указан');
        return;
    }
    CMmb::setViews('ViewTeamData', '');
} elseif ($action === 'HideTeamUser') {
    // ============ Удаление участника команды ====================================
    $HideTeamUserId = mmb_validateInt($_POST, 'HideTeamUserId');
    if ($HideTeamUserId <= 0) {
        CMmb::setErrorMessage('Участник не найден');
        return;
    }
    if ($TeamId <= 0) {
        CMmb::setErrorMessage('Команда не найдена');
        return;
    }
    if ($RaidId <= 0) {
        CMmb::setErrorMessage('Марш-бросок не найден');
        return;
    }
    if ($SessionId <= 0) {
        CMmb::setErrorMessage('Сессия не найдена');
        return;
    }

    // Проверка возможности редактировать команду
    if (!CanEditTeam($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange)) {
        CMmb::setErrorMessage('Изменения в команде запрещены');
        return;
    }

    // Проверка на повторное удаление
    $sql = "select teamuser_id from TeamUsers where teamuser_hide = 0 and teamuser_id = $HideTeamUserId";
    if (CSql::singleValue($sql, 'teamuser_id') <> $HideTeamUserId) {
        CMmb::setErrorMessage('Удаляемый пользователь не найден или уже удален');
        return;
    }

    // Смотрим, был ли это последний участник или нет
    $sql = "select count(*) as result from TeamUsers where teamuser_hide = 0 and team_id = $TeamId";
    $StartTeamUserCount = CSql::singleValue($sql, 'result');


    // Отправить письмо всем участникам команды об удалении (до физического удаления!)
    // Кроме того, кто удалял
    if ($UserId > 0 and $TeamId > 0) {
        $ChangeDataUserName = CSql::userName($UserId);
        $sql = "select user_name from Users u inner join TeamUsers tu on tu.user_id = u.user_id where tu.teamuser_id = $HideTeamUserId";
        $DelUserName = CSql::singleValue($sql, 'user_name');

        $sql = "select u.user_email, u.user_name, t.team_num, d.distance_name, r.raid_name
			from Users u
				inner join TeamUsers tu on tu.user_id = u.user_id
				inner join Teams t on tu.team_id = t.team_id
				inner join Distances d on t.distance_id = d.distance_id
				inner join Raids r on d.raid_id = r.raid_id
			where tu.teamuser_id = $HideTeamUserId or (tu.teamuser_hide = 0 and tu.team_id = $TeamId and u.user_id <> $UserId)
			order by tu.teamuser_id asc";
        $Result = MySqlQuery($sql);

        if ($StartTeamUserCount == 1) {
            $Row = mysqli_fetch_assoc($Result);
            $Msg = "Уважаемый участник " . $Row['user_name'] . "!\n\nВаша команда (N " . $Row['team_num'] . ", Дистанция: " . trim($Row['distance_name']) . ", ММБ: " . trim(
                    $Row['raid_name']
                ) . ") была удалена.\nАвтор изменений: " . $ChangeDataUserName . ".\n\nP.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
            // Отправляем письмо
            SendMail($Row['user_email'], $Msg, $Row['user_name']);
        } else {
            while ($Row = mysqli_fetch_assoc($Result)) {
                // Формируем сообщение
                if (trim($DelUserName) <> trim($Row['user_name'])) {
                    $Msg = "Уважаемый участник " . $Row['user_name'] . "!\n\nИз Вашей команды (N " . $Row['team_num'] . ", Дистанция: " . trim($Row['distance_name']) . ", ММБ: " . trim(
                            $Row['raid_name']
                        ) . ") был удален участник: " . $DelUserName . ".\nАвтор изменений: " . $ChangeDataUserName . ".\nВы можете увидеть результат на сайте и при необходимости внести свои изменения.\n\nP.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
                } else {
                    $Msg = "Уважаемый участник " . $Row['user_name'] . "!\n\nВы были удалены из команды (N " . $Row['team_num'] . ", Дистанция: " . trim($Row['distance_name']) . ", ММБ: " . trim(
                            $Row['raid_name']
                        ) . ")\nАвтор изменений: " . $ChangeDataUserName . ".\n\nP.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
                }
                // Отправляем письмо
                SendMail($Row['user_email'], $Msg, $Row['user_name']);
            }
        }
        mysqli_free_result($Result);
    }
    // Конец отправки писем об удалении


    // 04.2016 вернул обратно
    // 07.2015 Заменил на физическое удаление
    //$sql = "delete from TeamUsers where teamuser_id = $HideTeamUserId";
    $sql = "update TeamUsers set teamuser_hide = 1, teamuser_changedt = NOW() where teamuser_id = " . $HideTeamUserId;
    $rs = MySqlQuery($sql);

    // 27/01/2017 пересчет данных об участниках в команде
    RecalcTeamUsersStatistic(0, $TeamId);

    // Повторная проверка после удаления
    $sql = "select count(*) as result from TeamUsers where teamuser_hide = 0 and team_id = $TeamId";
    $TeamUserCount = CSql::singleValue($sql, 'result');

    if ($StartTeamUserCount == 1 and $TeamUserCount == 0)    // Это был последний участник
    {
        $sql = "update Teams set team_hide = 1 where team_id = $TeamId";
        $rs = MySqlQuery($sql);
        /*

        // 01.06.2016 отмена электронной очереди
                // Ищем первую команду в листе ожидания
                $WaitTeamId = FindFirstTeamInWaitList($RaidId);
                $RaidOutOffLimit = IsOutOfRaidLimit($RaidId);
                if ($RaidOutOffLimit == 0 AND $WaitTeamId > 0 AND $RaidStage == 1) {
                    $sql = "update Teams set team_outofrange = 0, team_waitdt = NULL where team_id = $WaitTeamId";
                    $rs = MySqlQuery($sql);
                }
        */
        $view = "";
    } else {
        $view = "ViewTeamData";
    }

    $view = mmb_validate($_POST, 'view', 'ViewTeamData');
}

// ============ Смена этапа схода участника команды ===========================
// 26/06/2015 Удалил код обработки схода на этапе
// 25/11/2013 Оставил для совместимости старый вариант с этапами
// ============ Смена точки неявки участника команды ===========================
elseif ($action === 'TeamUserNotInPoint') {
    $HideTeamUserId = mmb_validateInt($_POST, 'HideTeamUserId');
    if ($HideTeamUserId <= 0) {
        CMmb::setErrorMessage('Участник не найден');
        return;
    }
    // Здесь может быть 0 точка - значит, что участник везде явился
    $LevelPointId = mmb_validateInt($_POST, 'UserNotInLevelPointId');
    if ($LevelPointId < 0) {
        CMmb::setErrorMessage('Точка не найдена');
        return;
    }
    if ($TeamId <= 0) {
        CMmb::setErrorMessage('Команда не найдена');
        return;
    }
    if ($RaidId <= 0) {
        CMmb::setErrorMessage('Не найден ММБ.');
        return;
    }
    if ($SessionId <= 0) {
        CMmb::setErrorMessage('Не найдена сессия.');
        return;
    }

    // Проверка возможности редактировать результаты
    if (!CanEditResults($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange)) {
        CMmb::setErrorMessage('Изменение результатов команды запрещено');
        return;
    }

    // Смотрим, есть ли точка сход в TeamLevelDismiss

    $sql = "select teamleveldismiss_id from TeamLevelDismiss where teamuser_id = $HideTeamUserId";
    $DismissId = CSql::singleValue($sql, 'teamleveldismiss_id');

    if ($LevelPointId) {
        if ($DismissId) {
            // Точка уже есть и пользователь сошёл - обновляем точку
            $sql = "update TeamLevelDismiss set levelpoint_id = $LevelPointId where teamleveldismiss_id = $DismissId";
            $rs = MySqlQuery($sql);
        } else {
            // Точки нет, а пользователь сошёл - создаём точку
            $sql = "insert into TeamLevelDismiss (teamuser_id, levelpoint_id, device_id) 
			        values ($HideTeamUserId, $LevelPointId, 1) ";
            $rs = MySqlQuery($sql);
        }
    } else {
        if ($DismissId) {
            // Точка есть, а пользователь не сошёл - удаляем точку
            $sql = "delete from TeamLevelDismiss where teamleveldismiss_id = $DismissId";
            $rs = MySqlQuery($sql);
        } else {
            // ТОчка нет и пользователь не сошёл - ничего не делаем
            $view = mmb_validate($_POST, 'view', 'ViewTeamData');
            return;
        }
    }
    // Конец разбора возможных ситуаций со сходами и наличием точки в TeamLevelDismiss

    /*
        $sql = "update TeamUsers set levelpoint_id = ".($LevelPointId > 0 ? $LevelPointId : 'null' )." where teamuser_id = ".$HideTeamUserId;
        $rs = MySqlQuery($sql);
        $view = "ViewTeamData";
    */
    // Письмо об изменениях	всем, кроме автора изменений
    // !!! Сход относится к результатам на дистанции и об их изменений письма слать не надо
    $ChangeDataUserName = CSql::userName($UserId);
    $sql = "select u.user_email, u.user_name, t.team_num, d.distance_name, r.raid_name
		from Users u
			inner join TeamUsers tu on tu.user_id = u.user_id
			inner join Teams t on tu.team_id = t.team_id
			inner join Distances d on t.distance_id = d.distance_id
			inner join Raids r on d.raid_id = r.raid_id
		where tu.teamuser_hide = 0 and tu.team_id = $TeamId and u.user_id <> $UserId
		order by tu.teamuser_id asc";
    $Result = MySqlQuery($sql);
    while ($Row = mysqli_fetch_assoc($Result)) {
        // Формируем сообщение
        $Msg = "Уважаемый участник {$Row['user_name']}!\n\n";
        $Msg .= "Действие: изменение данных команды.\n";
        $Msg .= "Команда N {$Row['team_num']}, Дистанция: {$Row['distance_name']}, ММБ: " . trim($Row['raid_name']) . ".\n";
        $Msg .= "Автор изменений: $ChangeDataUserName.\n";
        $Msg .= "Вы можете увидеть результат на сайте и при необходимости внести свои изменения.\n\n";
        $Msg .= "P.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
        // Отправляем письмо
        SendMail($Row['user_email'], $Msg, $Row['user_name']);
    }
    mysqli_free_result($Result);

    $view = $_POST['view'];
    if (empty($view)) {
        $view = "ViewTeamData";
    }
} // ============ Обратимое удаление команды ====================================
elseif ($action == 'HideTeam') {
    if ($TeamId <= 0) {
        CMmb::setErrorMessage('Команда не найдена');
        return;
    }
    if ($RaidId <= 0) {
        CMmb::setErrorMessage('Марш-бросок не найден');
        return;
    }
    if ($SessionId <= 0) {
        CMmb::setErrorMessage('Сессия не найдена');
        return;
    }

    // Проверка возможности удалить команду
    if (!CanEditTeam($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange)) {
        CMmb::setErrorMessage('Удаление команды запрещено');
        return;
    }

    // Уведомление всем. в т.ч тому, кто удалял
    if (($UserId > 0) && ($TeamId > 0)) {
        $ChangeDataUserName = CSql::userName($UserId);
    }

    $sql = "select u.user_email, u.user_name, t.team_num, d.distance_name, r.raid_name
		from Users u
			inner join TeamUsers tu on tu.user_id = u.user_id
			inner join Teams t on tu.team_id = t.team_id
			inner join Distances d on t.distance_id = d.distance_id
			inner join Raids r on d.raid_id = r.raid_id
		where tu.teamuser_hide = 0 and tu.team_id = $TeamId
		order by tu.teamuser_id asc";
    $Result = MySqlQuery($sql);
    while ($Row = mysqli_fetch_assoc($Result)) {
        // Формируем сообщение
        $Msg = "Уважаемый участник " . $Row['user_name'] . "!\n\nВаша команда (N " . $Row['team_num'] . ", Дистанция: " . trim($Row['distance_name']) . ", ММБ: " . trim(
                $Row['raid_name']
            ) . ") была удалена.\nАвтор изменений: " . $ChangeDataUserName . ".\n\n\nP.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
        // Отправляем письмо
        SendMail($Row['user_email'], $Msg, $Row['user_name']);
    }
    mysqli_free_result($Result);

    // 04.2016 вернул обновление
    // 21/03/2016 заменил на удаление
    //$sql = "delete from TeamUsers where team_id = $TeamId";
    $sql = "update TeamUsers set teamuser_hide = 1, teamuser_changedt = NOW() where team_id = $TeamId";
    $rs = MySqlQuery($sql);
    $sql = "update Teams set team_hide = 1 where team_id = $TeamId";
    $rs = MySqlQuery($sql);

    /*
    // 01/06/2016 Отмена электронной очереди
        // Ищем первую команду в листе ожидания
        $WaitTeamId = FindFirstTeamInWaitList($RaidId);
        $RaidOutOffLimit = IsOutOfRaidLimit($RaidId);
        if ($RaidOutOffLimit == 0 AND $WaitTeamId > 0 AND $RaidStage == 1) {
            $sql = "update Teams set team_outofrange = 0, team_waitdt = NULL where team_id = $WaitTeamId";
            $rs = MySqlQuery($sql);
        }
    */

    $view = "ViewRaidTeams";
} elseif ($action === 'InviteTeam') {
    // ============ Перевод команды в зачет ====================================
    if ($TeamId <= 0) {
        CMmb::setErrorMessage('Команда не найдена');
        return;
    }
    if ($RaidId <= 0) {
        CMmb::setErrorMessage('Марш-бросок не найден');
        return;
    }
    if ($SessionId <= 0) {
        CMmb::setErrorMessage('Сессия не найдена');
        return;
    }

    // Проверка возможности пригласить команду
    $inviteId = CRights::canInviteTeam($UserId, $TeamId);

    if (!$inviteId) {
        CMmb::setErrorMessage('Приглашение команды невозможно');
        return;
    }

    $sql = "update Teams set team_outofrange = 0, invitation_id = $inviteId, invitation_usedt = NOW()  where team_id = $TeamId";
    $rs = MySqlQuery($sql);

    $view = "ViewRaidTeams";
} elseif ($action === "CancelChangeTeamData") {
    // ============ Отмена изменений в команде ====================================
    $view = "ViewTeamData";
} elseif ($action === "ViewRaidTeams") {
    // ============ Действие вызывается ссылкой Отмена ============================
    $view = "ViewRaidTeams";
} elseif ($action === 'JsonExport') {
    // =============== Получение JSON экспорта для желающих анализировать протокол ===================
    // Сначала поместил в административный, но там нет доступа, кроме администратора
    $RaidId = mmb_validateInt($_REQUEST, 'RaidId', -1);
    if ($RaidId <= 0) {
        CMmb::setMessage('Не выбран марш-бросок');
        return;
    }

    // Сбор данных для дампа
    $data = [];

    // Raids: raid_id, raid_name, raid_registrationenddate
    $Sql = "select raid_id, raid_name 
	        from Raids 
			where raid_id = $RaidId";

    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["Raids"][] = $Row;
    }
    mysqli_free_result($Result);

    // Distances: distance_id, raid_id, distance_name
    $Sql = "select distance_id, raid_id, distance_name 
			from Distances
			where distance_hide = 0 and raid_id = $RaidId";

    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["Distances"][] = $Row;
    }
    mysqli_free_result($Result);


    // Teams: team_id, distance_id, team_name, team_num // *
    $Sql = "select team_id, t.distance_id, team_name, team_num, team_usegps, team_greenpeace,
					team_result, team_registerdt, team_outofrange,
					team_maxlevelpointorderdone,
					team_minlevelpointorderwitherror, invitation_id, team_dismiss
			from Teams t
			     inner join Distances d on t.distance_id = d.distance_id
			where t.team_hide = 0 and d.distance_hide = 0  and d.raid_id = $RaidId";

    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["Teams"][] = $Row;
    }
    mysqli_free_result($Result);

    // Initations: team_id, distance_id, team_name, team_num // *
    $Sql = "select inv.invitation_id, inv.invitation_begindt, inv.invitation_enddt,
			inv.user_id, invd.invitationdelivery_type
		from Teams t
			     inner join Distances d on t.distance_id = d.distance_id
			     inner join Invitations inv
			     on t.invitation_id = inv.invitation_id
			     inner join InvitationDeliveries  invd
			     on inv.invitationdelivery_id = invd.invitationdelivery_id
		where t.team_hide = 0 and d.distance_hide = 0  and d.raid_id = $RaidId";

    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["Invitations"][] = $Row;
    }
    mysqli_free_result($Result);


    // Users: user_id, user_name, user_birthyear // *
    // Добавил ограничение - только по текущему ММБ
    $Sql = "select u.user_id, CASE WHEN COALESCE(u.user_noshow, 0) = 1 THEN '$Anonimus' ELSE u.user_name END as user_name,
	               u.user_birthyear, u.user_city, u.user_minraidid, u.user_maxraidid,
		       u.user_maxnotstartraidid, u.user_r6, u.user_noinvitation, u.user_sex 
	        from Users u
			     inner join TeamUsers tu on u.user_id = tu.user_id
		    	 inner join Teams t on tu.team_id = t.team_id
		      	 inner join Distances d on t.distance_id = d.distance_id
			where u.user_hide = 0 and t.team_hide = 0 and tu.teamuser_hide = 0 and d.distance_hide = 0 and d.raid_id = $RaidId";


    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["Users"][] = $Row;
    }
    mysqli_free_result($Result);

    // TeamUsers: teamuser_id, team_id, user_id, teamuser_hide
    $Sql = "select teamuser_id, tu.team_id, tu.user_id, tu.teamuser_rank, tu.teamuser_new, tu.teamuser_notstartraidid 
	        from TeamUsers tu 
		     	inner join Teams t on tu.team_id = t.team_id
		     	inner join Distances d on t.distance_id = d.distance_id
			where t.team_hide = 0 and tu.teamuser_hide = 0 and d.distance_hide = 0 and d.raid_id = $RaidId";

    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["TeamUsers"][] = $Row;
    }
    mysqli_free_result($Result);


    // Определяем, можно ли показывать пользователю информацию об этапах дистанции
    $LevelDataVisible = CanViewResults($Administrator, $Moderator, $RaidStage);
    $CanViewResults = CanViewResults($Administrator, $Moderator, $RaidStage);

    if ($LevelDataVisible) {
        // LevelPoints: levelpoint_id, distance_id, levelpoint_name, levelpoint_order, pointtype_id, level_pointnames, level_pointpenalties, level_begtime, level_maxbegtime, level_minendtime, level_endtime
        $Sql = "select lp.levelpoint_id, lp.distance_id, lp.levelpoint_name, lp.levelpoint_order, lp.pointtype_id, 
					lp.levelpoint_penalty, lp.levelpoint_mindatetime, lp.levelpoint_maxdatetime,
					lp.scanpoint_id
				from LevelPoints lp 
					inner join Distances d on lp.distance_id = d.distance_id 
				where  d.distance_hide = 0 and d.raid_id = $RaidId";

        $Result = MySqlQuery($Sql);
        while (($Row = mysqli_fetch_assoc($Result))) {
            $data["Levels"][] = $Row;
        }
        mysqli_free_result($Result);


        // TeamLevelDismiss:
        $Sql = "select teamleveldismiss_id, tld.levelpoint_id, 
					teamleveldismiss_date, teamuser_id
			from TeamLevelDismiss tld
			     inner join LevelPoints lp on tld.levelpoint_id = lp.levelpoint_id
			     inner join Distances d on lp.distance_id = d.distance_id
			where  d.distance_hide = 0 and d.raid_id = $RaidId";

        $Result = MySqlQuery($Sql);

        while (($Row = mysqli_fetch_assoc($Result))) {
            $data["TeamLevelDismiss"][] = $Row;
        }
        mysqli_free_result($Result);
    }
    // Конец проверки, что можно экспортировать данные по этапам

    $sql = "select  raid_fileprefix
		    from Raids
	        where raid_id = $RaidId";
    $Prefix = trim(CSql::singleValue($sql, 'raid_fileprefix'));
    $JsonFileName = 'TeamLevelPoints.json';
    $fullJSONfileName = $MyStoreFileLink . $Prefix . $JsonFileName;
    $zipfileName = $MyStoreFileLink . $Prefix . 'mmbdata.zip';

    $output = fopen($fullJSONfileName, 'w');
    fwrite($output, '{"TeamLevelPoints":[');

    if ($CanViewResults) {
        // TeamLevelPoints:
        $Sql = "select teamlevelpoint_id, tlp.team_id, tlp.levelpoint_id, 
					teamlevelpoint_datetime, teamlevelpoint_comment,
					teamlevelpoint_penalty,
					error_id, teamlevelpoint_duration,
					time_to_sec(coalesce(teamlevelpoint_duration, 0))/60.0 as teamlevelpoint_durationdecimal,
					teamlevelpoint_result
				from TeamLevelPoints tlp
		    		 inner join Teams t on tlp.team_id = t.team_id
			    	 inner join Distances d on t.distance_id = d.distance_id
				where t.team_hide = 0 and d.distance_hide = 0 and d.raid_id = $RaidId";

        $Result = MySqlQuery($Sql);

        $firstRow = true;
        while (($Row = mysqli_fetch_assoc($Result))) {
            if ($firstRow) {
                $firstRow = false;
            } else {
                fwrite($output, ",");
            }
            fwrite($output, json_encode($Row));
        }
    }

    fwrite($output, ']}');
    mysqli_free_result($Result);
    fclose($output);

    $JsonMainDataFileName = 'maindata.json';
    $fullJSONmaindatafileName = $MyStoreFileLink . $Prefix . $JsonMainDataFileName;

    $output2 = fopen($fullJSONmaindatafileName, 'w');
    fwrite($output2, json_encode($data) . "\n");
    fclose($output2);
    unset($data);

    $zip = new ZipArchive(); //Создаём объект для работы с ZIP-архивами
    $zip->open($zipfileName, ZIPARCHIVE::CREATE); //Открываем (создаём) архив archive.zip
    $zip->addFile($fullJSONfileName, $JsonFileName);
    $zip->addFile($fullJSONmaindatafileName, $JsonMainDataFileName);
    $zip->close();

    if (file_exists($zipfileName)) {
        header("Content-Type: application/zip");
        header("Content-Disposition: attachment; filename=mmbdata.zip");
        ob_clean();
        flush();
        readfile($zipfileName);
    }

    // Можно не прерывать, но тогда нужно написать обработчик в index, чтобы не выводить дальше ничего
    die();
} elseif ($action === "AddTeamInUnion") {
    // Действие вызывается нажатием кнопки "Объединить"

    if ($TeamId <= 0) {
        CMmb::setErrorMessage('Команда не найдена');
        return;
    }
    if ($SessionId <= 0) {
        CMmb::setErrorMessage('Сессия не найдена');
        return;
    }

    // Права на редактирование
    if (!$Administrator and !$Moderator) {
        CMmb::setErrorMessage('Нет прав на объединение');
        return;
    }

    // Проверяем, что команды нет в объединении
    $sql = " select teamunionlog_id
	         from TeamUnionLogs 
			 where teamunionlog_hide = 0 
                   and union_status = 2
			       and team_id = $TeamId";

    if (CSql::rowCount($sql) > 0) {
        setUnionError('Команды уже объединена');
        return;
    }

    // Проверяем, что команды не скрыта и в зачете
    $sql = " select team_id 
	         from Teams 
			 where team_hide = 0 
                               and COALESCE(team_outofrange, 0) = 0
			       and team_id = $TeamId";

    if (CSql::rowCount($sql) <= 0) {
        setUnionError('Команда скрыта или вне зачета');
        return;
    }

    $Sql = "select teamunionlog_id,  teamunionlog_hide
                        from TeamUnionLogs
		        where team_id = $TeamId
			      and (union_status = 1 or union_status = 3)
		        LIMIT 0,1 ";

    $Row = CSql::singleRow($Sql);
    $TeamUnionLogId = $Row['teamunionlog_id'];
    $TeamUnionLogHide = $Row['teamunionlog_hide'];

    $TeamAdd = 0;

    if (empty($TeamUnionLogId)) {
        $Sql = "insert into TeamUnionLogs (user_id, teamunionlog_dt,
			         teamunionlog_hide, team_id, team_parentid, union_status)
				  values ($UserId, now(), 0, $TeamId, null, 1)";
        MySqlQuery($Sql);

        $TeamAdd = 1;
    } else {
        if ($TeamUnionLogHide == 0) {
            $TeamAdd = 0;
        } else {
            // Есть и команда скрыта -  обновляем
            $Sql = "update TeamUnionLogs set teamunionlog_hide = 0, teamunionlog_dt = now(), union_status = 1  where teamunionlog_id = $TeamUnionLogId";
            MySqlQuery($Sql);

            $TeamAdd = 1;
        }
        // Конец проверки существующей записи

    }
    // Конец разбора ситуации с добавлением команды в объединение


    if ($TeamAdd) {
        $ChangeDataUserName = CSql::userName($UserId);

        /*
                     $Sql = "select user_name, user_email from  Users where user_id = ".$pUserId;
                 $Row = CSql::fullUser($Sql);
                 $pUserName = $Row['user_name'];
                 $pUserEmail = $Row['user_email'];

                         $Msg = "Уважаемый пользователь ".$pUserName."!\r\n\r\n";
                 $Msg =  $Msg."Вы получили статус модератора марш-броска ".$RaidName."\r\n";
                 $Msg =  $Msg."Автор изменений: ".$ChangeDataUserName.".\r\n\r\n";

        */
        // Отправляем письмо
        //  SendMail(trim($pUserEmail), $Msg, $pUserName);

        CMmb::setResult('Команда добавлена в объединение', "ViewAdminUnionPage");
    } else {
        CMmb::setResult('Команда уже включена в объединение!', "ViewTeamData");
    }
} elseif ($action === "HideTeamInUnion") {
    // Действие вызывается нажатием кнопки "Удалить" на странице со списком команд в объединении

    // Права
    if (!$Administrator and !$Moderator) {
        CMmb::setErrorMessage('Нет прав на объединение');
        return;
    }

    $TeamUnionLogId = mmb_validateInt($_POST, 'TeamUnionLogId');

    // Если вызвали с таким действием, должны быть определены оба пользователя
    if ($TeamUnionLogId <= 0 || (!$Administrator and !$Moderator)) {
        return;
    }
    $Sql = "update TeamUnionLogs set teamunionlog_hide = 1, union_status = 0  where teamunionlog_id = $TeamUnionLogId";
    MySqlQuery($Sql);

    $ChangeDataUserName = CSql::userName($UserId);

    /*
                 $Sql = "select user_name, user_email from  Users where user_id = ".$pUserId;
             $Row = CSql::fullUser($Sql);
             $pUserName = $Row['user_name'];
             $pUserEmail = $Row['user_email'];

                     $Msg = "Уважаемый пользователь ".$pUserName."!\r\n\r\n";
             $Msg =  $Msg."Вы получили статус модератора марш-броска ".$RaidName."\r\n";
             $Msg =  $Msg."Автор изменений: ".$ChangeDataUserName.".\r\n\r\n";

    */
    // Отправляем письмо
    //  SendMail(trim($pUserEmail), $Msg, $pUserName);

    // Остаемся на той же странице
    CMmb::setResult('Команда удалена из объединения', 'ViewAdminUnionPage');
} elseif ($action === "ClearUnionTeams") {
    // Действие вызывается нажатием кнопки "Очистить объединение" на странице со списокм команд в объединении


    // Права
    if (!$Administrator && !$Moderator) {
        CMmb::setErrorMessage('Нет прав на объединение');
        return;
    }

    $sql = "update TeamUnionLogs
            set union_status = 0, teamunionlog_hide = 1
		    where teamunionlog_hide = 0 and union_status = 1";

    $Result = MySqlQuery($sql);
    CMmb::setResult('Объединение очищено', 'ViewAdminUnionPage');
} elseif ($action === "ClearUnionTransaction") {
    // Действие вызывается нажатием кнопки "Сбросить транзакцию" на странице со списокм команд в объединении


    // Права
    if (!$Administrator and !$Moderator) {
        CMmb::setErrorMessage('Нет прав на объединение');
        return;
    }


    $sql = "update TeamUnionLogs
                SET union_status = 0, teamunionlog_hide = 1
		where union_status = 3
	      ";

    $Result = MySqlQuery($sql);
    CMmb::setResult('Транзакция по объединению сброшена', 'ViewAdminUnionPage');
} elseif ($action === "UnionTeams") {
    // Действие вызывается нажатием кнопки "Объединить"\

    // Права
    if (!$Administrator && !$Moderator) {
        CMmb::setErrorMessage('Нет прав на объединение');
        return;
    }

    $pTeamName = $_POST['TeamName'];

    if (trim($pTeamName) === '' || trim($pTeamName) === 'Название объединённой команды') {
        setUnionError('Не указано название.');
        return;
    }

    $sql = "select  MAX(TIME_TO_SEC(COALESCE(t.team_result, 0))) - MIN(TIME_TO_SEC(COALESCE(t.team_result, 0))) as deltaresult,
                    MAX(COALESCE(t.team_maxlevelpointorderdone, 0)) - MIN(COALESCE(t.team_maxlevelpointorderdone, 0)) as deltaprogress,
                    MAX(COALESCE(t.team_minlevelpointorderwitherror, 0)) - MIN(COALESCE(t.team_minlevelpointorderwitherror, 0)) as deltaerror,
		    MAX(t.distance_id) as maxdistanceid, 
		    MIN(t.distance_id) as mindistanceid,
		    SUM(t.team_mapscount) as mapscount, 
		    count(t.team_id) as teamcount 
		        from  TeamUnionLogs tul
			      inner join Teams t
			      on t.team_id = tul.team_id
			where tul.teamunionlog_hide = 0 
                              and tul.union_status = 1";

    $Row = CSql::singleRow($sql);

    // Проверяем, что результат отличается не больше чем на 15 минут
    if ($Row['deltaresult'] > 15 * 60) {
        setUnionError('Результат команд отличается больше чем на 15 минут');
        return;
    }

    // Проверяем, что прогресс и ошибки одинаковые
    if ($Row['deltaprogress'] > 0 || $Row['deltaerror'] > 0) {
        setUnionError('Различаются финишные точки или точки с ошибкой');
        return;
    }


    if ($Row['maxdistanceid'] <> $Row['mindistanceid']) {
        setUnionError('Разные дистанции у объединяемых команд');
        return;
    }

    if ($Row['teamcount'] < 2) {
        setUnionError('Меньше двух команд в объединении или не закончилось предыдущее');
        return;
    }

    $teamCount = $Row['teamcount'];
    $pDistanceId = $Row['maxdistanceid'];
    $pTeamMapsCount = $Row['mapscount'];
    // Проверяем одинаковое число взятых КП
    $sql = "select  tlp.levelpoint_id
		        from  TeamUnionLogs tul
			      inner join Teams t
			      on t.team_id = tul.team_id
	              inner join TeamLevelPoints tlp
			      on t.team_id = tlp.team_id 
			where tul.teamunionlog_hide = 0 
                  and tul.union_status = 1
           group by tlp.levelpoint_id
			having count(*) <> $teamCount ";

    if (CSql::rowCount($sql) > 0) {
        setUnionError('Различается список взятых КП');
        return;
    }

    if ($RaidId <= 0) {
        setUnionError('Не указан ММБ.');
        return;
    }

    if ($pDistanceId <= 0) {
        setUnionError('Не указана дистанция.');
        return;
    }

    $sql = "select  MAX(t.team_usegps) as team_usegps,
	                MIN(t.team_greenpeace) as team_greenpeace
	        from  TeamUnionLogs tul
		      inner join Teams t
		      on t.team_id = tul.team_id
		where tul.teamunionlog_hide = 0 
                      and tul.union_status = 1";

    $Row = CSql::singleRow($sql);

    $pTeamUseGPS = $Row['team_usegps'];
    $pTeamGreenPeace = $Row['team_greenpeace'];

    // Проверяем, что нет начатой транзакции
    $sql = "select tul.team_id
	        from  TeamUnionLogs tul
	        where tul.union_status = 3
                ";

    if (CSql::rowCount($sql) > 0) {
        setUnionError('Есть незаконченное активное объединение.');
        return;
    }

    // Приступаем, собственно к объединению:
    // переводим текущие команды в статус 3
    // Создаём новую команду и ставим ей признак скрытая
    // GPS по принципу ИЛИ, greenpeace по принципу И, число карт - суммируем
    // Добавляем всех участников
    // группируем результаты по этапам и для старта берём MIN время, для финиша - максимальное
    // Проставляем новую команду в поле parent_id
    // Открываем новую команду
    // Скрываем старые команды
    // Ставим статус в журнале, что команды объединены

    // Удаляем хвосты предыдущего объединения, если они есть (статус 3)
    //	$sql = " update TeamUnionLogs set union_status = 0, teamunionlog_hide = 1
    //		 where union_status = 3 ";
    //	MySqlQuery($sql);

    // переводим текущие команды в статус 3
    $sql = " update TeamUnionLogs set union_status = 3
			 where teamunionlog_hide = 0 
                               and union_status = 1 ";
    MySqlQuery($sql);

    $sql = "insert into Teams (team_num, team_name, team_usegps, team_mapscount, distance_id,
			team_registerdt, team_greenpeace, team_hide, team_outofrange)
			values (

			(select COALESCE(MAX(t.team_num), 0) + 1
				from Teams t
					inner join Distances d on t.distance_id = d.distance_id
				where d.raid_id = $RaidId)
		
			-- Все остальное
			, '$pTeamName', $pTeamUseGPS, $pTeamMapsCount, $pDistanceId, NOW(), $pTeamGreenPeace, 1, 0)";

    // При insert должен вернуться последний id - это реализовано в MySqlQuery
    $TeamId = MySqlQuery($sql);
    // Поменялся TeamId, заново определяем права доступа

    //  По-моему здесь необязательно запрашивать привилегии
    //	GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange);


    if ($TeamId <= 0) {
        CMmb::setResult('Ошибка записи новой команды.', "ViewAdminUnionPage");
        return;
    }
    $sql = "insert into TeamUsers (team_id, user_id, teamuser_hide) 
                        select $TeamId , tu.user_id, 1
		        from  TeamUnionLogs tul
			      inner join Teams t
			      on t.team_id = tul.team_id
			      inner join TeamUsers tu
			      on tu.team_id = t.team_id
			where tul.union_status = 3
	                      and tu.teamuser_hide = 0 ";

// Правильнее написать ХП, которая делает это
// т.к. нельзя в одной строке передать несколько запросов		
    MySqlQuery($sql);

    $sql = " insert into TeamLevelPoints (levelpoint_id, team_id, 
						 teamlevelpoint_datetime, teamlevelpoint_duration,
						 teamlevelpoint_penalty, teamlevelpoint_comment,
						 teamlevelpoint_result
						)
		         select  tlp.levelpoint_id, $TeamId,
		                CASE WHEN lp.pointtype_id = 1
					THEN  MIN(tlp.teamlevelpoint_datetime)  
					ELSE MAX(tlp.teamlevelpoint_datetime) 
				END,
		                MIN(tlp.teamlevelpoint_duration),
		                MAX(tlp.teamlevelpoint_penalty),
		                MAX(tlp.teamlevelpoint_comment),
		                MAX(tlp.teamlevelpoint_result)
 		         from  TeamUnionLogs tul
			       inner join Teams t
			       on t.team_id = tul.team_id
                   	       inner join TeamLevelPoints tlp
			       on t.team_id = tlp.team_id 
			       inner join LevelPoints lp
			       on tlp.levelpoint_id = lp.levelpoint_id
			 where tul.union_status = 3
              group by tlp.levelpoint_id ";

    MySqlQuery($sql);

    // вставка неявки для новой команды
    // user_id  в таблице TeamLevelDismiss - это автор записи, а не ключ пользователя-участника
    $sql = "insert into TeamLevelDismiss (device_id, user_id, teamuser_id, levelpoint_id) 
                        select 1, 0, tu2.teamuser_id, tld.levelpoint_id
		        from  TeamUnionLogs tul
			      inner join Teams t
			      on t.team_id = tul.team_id
			      inner join TeamUsers tu
			      on tu.team_id = t.team_id
			      inner join TeamUsers tu2
			      on tu2.team_id = $TeamId
			         and tu2.user_id = tu.user_id
			      inner join TeamLevelDismiss tld
			      on tld.teamuser_id = tu.teamuser_id
			where tul.union_status = 3
	                      and tu.teamuser_hide = 0 ";

    MySqlQuery($sql);

    $sql = " update TeamUnionLogs set team_parentid = $TeamId
			 where union_status = 3 ";

    MySqlQuery($sql);

    $sql = " update Teams t
			  inner join
			  (
			    select  tul.team_id
		            from  TeamUnionLogs tul
  			    where tul.teamunionlog_hide = 0 
	                          and tul.union_status = 3
			          and tul.team_parentid = $TeamId
			    group by  tul.team_id
			   )  a
			  on  t.team_id = a.team_id
		  set t.team_hide = 1, 
                      t.team_parentid = $TeamId";

    MySqlQuery($sql);


    $sql = " update TeamUsers tu
			  inner join
			  (
			    select  t.team_id
		            from  Teams t
  			    where t.team_parentid = $TeamId
			   )  a
			  on  tu.team_id = a.team_id
		  set tu.teamuser_hide = 1 ";

    MySqlQuery($sql);


    $sql = " update Teams set team_hide = 0
			 where team_id = $TeamId";

    MySqlQuery($sql);

    $sql = " update TeamUsers set teamuser_hide = 0
			 where team_id = $TeamId";

    MySqlQuery($sql);

    $sql = "update TeamUnionLogs set union_status = 2
			 where union_status = 3";

    MySqlQuery($sql);

    RecalcTeamResultFromTeamLevelPoints(0, $TeamId);

    // 27/01/2017 пересчет данных об участниках в команде
    RecalcTeamUsersStatistic(0, $TeamId);

    CMmb::setResult('Команды объединены', "ViewRaidTeams");
} elseif ($action === "CancelUnionTeams") {
    // Действие вызывается нажатием кнопки "Объединить"


    // Права
    if (!$Administrator and !$Moderator) {
        CMmb::setErrorMessage('Нет прав на отмену объединения');
        return;
    }

    $pParentTeamId = mmb_validateInt($_POST, 'TeamId');


    if ($RaidId <= 0) {
        setUnionError('Не указан ММБ.');
        return;
    }

    if ($pParentTeamId <= 0) {
        setUnionError('Не указана команда.');
        return;
    }

    // Проверяем, что команда есть в объединении
    $sql = " select teamunionlog_id
	         from TeamUnionLogs 
			 where teamunionlog_hide = 0 
                               and union_status = 2
			       and team_parentid = $pParentTeamId";

    if (CSql::rowCount($sql) <= 0) {
        setUnionError('Команды нет в объединении');
        return;
    }

    // Приступаем, собственно к отмене:
    // Удаляем новую объединённую команду
    $sql = " update Teams t
 		         set t.team_hide = 1 
                         where t.team_id = $pParentTeamId";
    MySqlQuery($sql);

    // её участников
    $sql = " update TeamUsers tu
 		         set tu.teamuser_hide = 1 
                         where tu.team_id = $pParentTeamId";


    MySqlQuery($sql);

    // Открываем старые команды

    $sql = " update Teams t
 		         set t.team_hide = 0 
                         where t.team_parentid = $pParentTeamId";
    MySqlQuery($sql);

    // Открываем участников старых команд
    $sql = " update TeamUsers tu
			  inner join
			  (
			    select  t.team_id
		            from  Teams t
  			    where t.team_parentid = $pParentTeamId
			   )  a
			  on  tu.team_id = a.team_id
			  inner join
			  (
			    select  tu2.user_id
		            from  TeamUsers tu2
  			    where tu2.team_id = $pParentTeamId
			   )  b
			  on  tu.user_id = b.user_id
			 set tu.teamuser_hide = 0
		  ";

    MySqlQuery($sql);

    // Ставим изменения в лог
    $sql = " update TeamUnionLogs set union_status = 0, team_parentid = null, teamunionlog_hide = 1
			 where teamunionlog_hide = 0 and union_status = 2 and team_parentid = $pParentTeamId";

    MySqlQuery($sql);

    $sql = "select team_id 
	        from  Teams
		    where team_hide = 0	and team_parentid = $pParentTeamId";

    $Result = MySqlQuery($sql);

    while ($Row = mysqli_fetch_assoc($Result)) {
        RecalcTeamResultFromTeamLevelPoints(0, $Row['team_id']);
    }

    mysqli_free_result($Result);

    // Сбрасываем команду, в которую объединяли
    $sql = "update Teams t set t.team_parentid = null where t.team_parentid = $pParentTeamId";

    MySqlQuery($sql);

    CMmb::setResult('Объединение отменено', 'ViewRaidTeams');
} // ============ Никаких действий не требуется =================================
?>
