<?php

/**
 *
 * +++++++++++ Обработчик действий, связанных с результатом +++++++++++++++++++
 *
 * @var string $action
 */

// Предполагается, что он вызывается после обработчика teamaction,
// поэтому не проверяем сессию и прочее

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) {
    return;
}

// =============== Показываем страницу администрирования ===================
if ($action === "ViewAdminDataPage") {
    // Действие вызывается ссылкой Администрирование

    CMmb::setViews('ViewAdminDataPage', '');
} elseif ($action === 'PrintRaidTeams') {
    // =============== Печать карточек ===================
    // Проверяем, что передали идентификатор ММБ
    $RaidId = (int)mmb_validateInt($_REQUEST, 'RaidId', -1);
    if ($RaidId < 0) {
        CMmb::setShortResult('Марш-бросок не найден', '');
        return;
    }

    print('Дистанция;Номера карточек<br />' . "\n");
    $sql = "select t.team_num, d.distance_name
	  from Teams t
	       inner join Distances d on t.distance_id = d.distance_id
	  where d.distance_hide = 0 and t.team_hide = 0 and COALESCE(t.team_outofrange, 0) = 0 and d.raid_id = $RaidId
	  order by d.distance_name, team_num asc";

    $Result = MySqlQuery($sql);

    $PredDistance = "";
    $CardsArr = "";
    while ($Row = mysqli_fetch_assoc($Result)) {
        if ($Row['distance_name'] <> $PredDistance) {
            if ($PredDistance <> "") // записываем накопленное
            {
                print("$CardsArr<br />\n");
            }
            $PredDistance = $Row['distance_name'];
            $CardsArr = $PredDistance . ';' . $Row['team_num'];
        } else // копим
        {
            $CardsArr = $CardsArr . ',' . $Row['team_num'];
        }
    }
    mysqli_free_result($Result);

    // записываем накопленное
    print($CardsArr . '<br />' . "\n");
    print('====<br />' . "\n");


    print('Дистанция;Номер;GPS;Название;Участники;Карты;Сумма<br />' . "\n");
    $sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name,
	  t.team_mapscount, d.distance_name, d.distance_id
	  from Teams t
		inner join Distances d on t.distance_id = d.distance_id
	  where d.distance_hide = 0 and t.team_hide = 0  and COALESCE(t.team_outofrange, 0) = 0 and d.raid_id = $RaidId
	  order by d.distance_name, team_num asc";

    $Result = MySqlQuery($sql);

    while ($Row = mysqli_fetch_assoc($Result)) {
        $sql = "select tu.teamuser_id, u.user_name, u.user_birthyear, u.user_id
		from TeamUsers tu
			inner join Users u on tu.user_id = u.user_id
		where tu.teamuser_hide = 0   and team_id = {$Row['team_id']}
		order by tu.teamuser_id asc";
        $UserResult = MySqlQuery($sql);

        $First = 1;
        while ($UserRow = mysqli_fetch_assoc($UserResult)) {
            if ($First == 1) {
                print($Row['distance_name'] . ';' . $Row['team_num'] . ';' . ($Row['team_usegps'] == 1 ? '+' : '') . ';' . $Row['team_name'] . ';' . $UserRow['user_name'] . ' ' . $UserRow['user_birthyear'] . ';' . $Row['team_mapscount'] . ';' . CalcualteTeamPayment(
                        $Row['team_id']
                    ) . '<br />' . "\n");
                $First = 0;
            } else {
                print(';;;;' . $UserRow['user_name'] . ' ' . $UserRow['user_birthyear'] . ';<br />' . "\n");
            }
        }

        mysqli_free_result($UserResult);
    }

    mysqli_free_result($Result);

    print("<br/>\n");

    // Можно не прерывать, но тогда нужно написать обработчик в index, чтобы не выводить дальше ничего
    die();
} elseif ($action === 'RaidCardsExport') {
    // =============== Генерация списка карточек в файл  ===================
    if ($RaidId <= 0) {
        CMmb::setShortResult('Марш-бросок не найден', '');
        return;
    }

    // рассылать всем может только администратор
    if (!$Administrator) {
        return;
    }

    CMmb::setViews('ViewAdminDataPage', '');

    // Заголовки, чтобы скачивать можно было и на мобильных устройствах просто браузером (который не умеет делать Save as...)
    header('Content-Type: text/plain;');
    header('Content-Disposition: attachment; filename=raidcards.txt');

    // create a file pointer connected to the output stream
    $output = fopen('php://output', 'w');

    //fwrite($output, iconv('UTF-8', 'Windows-1251', 'Дистанция;Номера карточек')."\n");
    fwrite($output, 'Дистанция;Номера карточек' . "\n");

    $sql = "select t.team_num, d.distance_name
		  from Teams t
	       		inner join Distances d on t.distance_id = d.distance_id
		  where d.distance_hide = 0 and t.team_hide = 0 and COALESCE(t.team_outofrange, 0) = 0 and d.raid_id = $RaidId
		  order by d.distance_name, team_num asc";

    $Result = MySqlQuery($sql);

    $PredDistance = "";
    $CardsArr = "";
    while ($Row = mysqli_fetch_assoc($Result)) {
        if ($Row['distance_name'] <> $PredDistance) {
            if ($PredDistance <> "") // записываем накопленное
            {
                fwrite($output, $CardsArr . "\n");
            }
            $PredDistance = $Row['distance_name'];
            $CardsArr = $PredDistance . ';' . $Row['team_num'];
        } else // копим
        {
            $CardsArr = $CardsArr . ',' . $Row['team_num'];
        }
    }
    mysqli_free_result($Result);

    // записываем накопленное
    fwrite($output, $CardsArr . "\n");
    fwrite($output, '====' . "\n");
    fwrite($output, 'Дистанция;Номер;GPS;Название;Участники;Карты;Сумма' . "\n");

    $sql = "select t.team_num, t.team_id, t.team_usegps, t.team_name,
		  t.team_mapscount, d.distance_name, d.distance_id
	  		from Teams t
			inner join Distances d on t.distance_id = d.distance_id
		  where d.distance_hide = 0 and t.team_hide = 0  and COALESCE(t.team_outofrange, 0) = 0 and d.raid_id = $RaidId
	  	  order by d.distance_name, team_num asc";

    $Result = MySqlQuery($sql);

    while ($Row = mysqli_fetch_assoc($Result)) {
        $sql = "select tu.teamuser_id, u.user_name, u.user_birthyear, u.user_id
			from TeamUsers tu
				inner join Users u on tu.user_id = u.user_id
			where tu.teamuser_hide = 0   and team_id = {$Row['team_id']}
			order by tu.teamuser_id asc";
        $UserResult = MySqlQuery($sql);

        $First = 1;
        while ($UserRow = mysqli_fetch_assoc($UserResult)) {
            if ($First == 1) {
                $strtowrite = $Row['distance_name'] . ';' . $Row['team_num'] . ';' . ($Row['team_usegps'] == 1 ? '+' : '') . ';' . $Row['team_name'] . ';' . $UserRow['user_name'] . ' ' . $UserRow['user_birthyear'] . ';' . $Row['team_mapscount'] . ';' . CalcualteTeamPayment(
                        $Row['team_id']
                    );

                fwrite($output, $strtowrite . "\n");
                $First = 0;
            } else {
                $strtowrite = ';;;;' . $UserRow['user_name'] . ' ' . $UserRow['user_birthyear'];
                fwrite($output, $strtowrite . "\n");
            }
        }

        mysqli_free_result($UserResult);
    }

    mysqli_free_result($Result);

    fclose($output);

    die();
} elseif ($action === 'JSON') {
    // =============== Получение дампа ===================
    if (!$Administrator && !$Moderator) {
        CMmb::setShortResult('Нет прав на экспорт', '');
        return;
    }

    // include("json.php");

    if ($RaidId <= 0) {
        CMmb::setShortResult('Не указан ММБ', '');
        return;
    }

    // Сбор данных для дампа
    $data = [];
    // Raids: raid_id, raid_name, raid_registrationenddate
    $Sql = "select raid_id, raid_name, raid_registrationenddate from Raids where raid_id = $RaidId";
    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["Raids"][] = $Row;
    }
    mysqli_free_result($Result);
    // Distances: distance_id, raid_id, distance_name
    $Sql = "select distance_id, raid_id, distance_name from Distances d where d.distance_hide = 0 and d.raid_id = $RaidId";
    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["Distances"][] = $Row;
    }
    mysqli_free_result($Result);
    // ScanPoints:
    $Sql = "select sp.scanpoint_id, sp.raid_id, sp.scanpoint_name, sp.scanpoint_order  from ScanPoints sp where sp.scanpoint_hide = 0 and sp.raid_id = $RaidId";
    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["ScanPoints"][] = $Row;
    }
    mysqli_free_result($Result);
    // 18/02/2014 добавил scanpoint_id
    // LevelPoints: levelpoint_id, level_id, pointtype_id
    $Sql = "select lp.levelpoint_id, lp.pointtype_id, lp.distance_id, lp.levelpoint_name, lp.levelpoint_order, lp.levelpoint_penalty, lp.levelpoint_mindatetime, lp.levelpoint_maxdatetime, scanpoint_id from LevelPoints lp inner join Distances d on lp.distance_id = d.distance_id where lp.levelpoint_hide = 0 and d.distance_hide = 0 and d.raid_id = $RaidId";
    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["LevelPoints"][] = $Row;
    }
    mysqli_free_result($Result);
    // LevelPointDiscounts:
    $Sql = "select lpd.levelpointdiscount_id, lpd.distance_id, lpd.levelpointdiscount_value, lpd.levelpointdiscount_start, lpd.levelpointdiscount_finish from LevelPointDiscounts lpd inner join Distances d on lpd.distance_id = d.distance_id where lpd.levelpointdiscount_hide = 0 and d.distance_hide = 0 and d.raid_id = $RaidId";
    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["LevelPointDiscounts"][] = $Row;
    }
    mysqli_free_result($Result);
    // Teams: team_id, distance_id, team_name, team_num // *
    $Sql = "select team_id, t.distance_id, team_name, team_num from Teams t inner join Distances d on t.distance_id = d.distance_id where t.team_hide = 0 and COALESCE(t.team_outofrange, 0) = 0 and d.raid_id = $RaidId";
    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["Teams"][] = $Row;
    }
    mysqli_free_result($Result);
    // Users: user_id, user_name, user_birthyear // *
    $Sql = "select user_id, user_name, user_birthyear from Users where user_hide = 0";
    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["Users"][] = $Row;
    }
    mysqli_free_result($Result);
    // TeamUsers: teamuser_id, team_id, user_id, teamuser_hide
    $Sql = "select teamuser_id, tu.team_id, user_id, teamuser_hide from TeamUsers tu inner join Teams t on tu.team_id = t.team_id inner join Distances d on t.distance_id = d.distance_id where t.team_hide = 0  and COALESCE(t.team_outofrange, 0) = 0  and d.raid_id = $RaidId";
    $Result = MySqlQuery($Sql);
    while (($Row = mysqli_fetch_assoc($Result))) {
        $data["TeamUsers"][] = $Row;
    }
    mysqli_free_result($Result);
    // 27/03/2013 Отключил экспорт TeamLevelPoints т.к. эта таблица формируется исключительно при импорте данных.
    // Пока на планшетах не производится сведение всех данных - не нужна
    // TeamLevelDismiss: user_id, levelpoint_id, team_id, teamuser_id, teamleveldismiss_date, device_id

    // Заголовки, чтобы скачивать можно было и на мобильных устройствах просто браузером (который не умеет делать Save as...)
    header("Content-Type: application/octet-stream");
    header("Content-Disposition: attachment; filename=\"update.json\"");
    // Вывод json
    print json_encode($data);

    // Можно не прерывать, но тогда нужно написать обработчик в index, чтобы не выводить дальше ничего
    die();
} elseif ($action === 'LoadRaidDataFile') {
    // =============== Загрузка файла с планшета =======
    $statustext = '';

    // Пока разрешил и модератору
    if (!$Administrator && !$Moderator) {
        return;
    }

    include('import.php');

//	$statustext = $statustext.'</br>'.$n_new.' результатов добавлено, '.$n_updated.' изменено, '.$n_unchanged.' являются дубликатами';
    $view = "ViewAdminDataPage";
} elseif ($action === 'SportiduinoImport') {
    // =============== Импорт данных Sportiduino =======
    if (!$Administrator && !$Moderator) {
        return;
    }
    include('sportiduino_import.php');
    $view = "ViewAdminDataPage";
} elseif ($action == 'RecalcRaidResults') {
    // =============== Пересчет результатов ММБ администратором ===================
    if ($RaidId <= 0) {
        CMmb::setShortResult('Марш-бросок не найден', '');
        return;
    }

    if (!$Administrator && !$Moderator) {
        return;
    }

    RecalcTeamResultFromTeamLevelPoints($RaidId, 0);
    /*
        $sql = 'select team_id
            from Teams t
                inner join Distances d on t.distance_id = d.distance_id
            where d.distance_hide = 0 and t.team_hide = 0 and d.raid_id = '.$RaidId.'
                   order by team_id';

            $Result = MySqlQuery($sql);
        // Цикл по всем командам
        while ($Row = mysqli_fetch_assoc($Result))
        {
            $RecalcTeamId = $Row['team_id'];
            RecalcTeamLevelDuration($RecalcTeamId);
            RecalcTeamLevelPenalty($RecalcTeamId);
            //  10/06/2014 если старый ММБ. то не обновляем результат
            // Обновляем результат команды
            if (!$OldMmb) {
                RecalcTeamResult($RecalcTeamId);
            }
        }
        mysqli_free_result($Result);

    */
    CMmb::setShortResult('Результаты марш-броска пересчитаны', 'ViewAdminDataPage');
} elseif ($action === 'FindRaidErrors') {
    // =============== Поиск ошибок =======
    if ($RaidId <= 0) {
        CMmb::setShortResult('Марш-бросок не найден', '');
        return;
    }
    if (!$Administrator && !$Moderator) {
        return;
    }

    $total_errors = FindErrors($RaidId, 0);

    CMmb::setShortResult("Результаты марш-броска проверены, найдено $total_errors ошибок", 'ViewAdminDataPage');
}
// =============== Показываем страницу модераторов ===================
if ($action === "ViewAdminModeratorsPage") {
    // Действие вызывается ссылкой Модераторы

    CMmb::setViews('ViewAdminModeratorsPage', '');
}


// =============== Показываем страницу волонтёров ===================
if ($action === "ViewRaidDevelopersPage") {
    // Действие вызывается ссылкой Судьи
    CMmb::setViews('ViewRaidDevelopersPage', '');
}

// =============== Показываем страницу объединения команд ===================
if ($action === "ViewAdminUnionPage") {
    // Действие вызывается ссылкой Объединение

    CMmb::setViews('ViewAdminUnionPage', '');
}
// =============== Показываем страницу объединения пользователей ===================
if ($action === "ViewUserUnionPage") {
    // Действие вызывается ссылкой Объединение

    CMmb::setViews('ViewUserUnionPage', '');
}
// =============== Показываем страницу рейтинга пользователей ===================
if ($action === "ViewRankPage") {
    // Действие вызывается ссылкой Объединение

    CMmb::setViews('ViewRankPage', '');
} else {
    // =============== Показываем страницу логов ===================
    if ($action === "viewLogs") {
        // todo добавить проверку прав!!!
        CMmb::setViews('viewLogs', '');
    } elseif ($action === 'RecalcRaidRank') {
        // =============== Пересчет рейтинга для ММБ ===================
        if ($RaidId <= 0) {
            CMmb::setShortResult('Марш-бросок не найден', '');
            return;
        }

        if (!$Administrator && !$Moderator) {
            return;
        }

        $Result = 0;
        $Result = RecalcTeamUsersRank($RaidId);

        CMmb::setShortResult('Рейтинг участников марш-броска пересчитан', 'ViewAdminDataPage');
    } elseif ($action === 'SendMessageForAll') {
        // =============== Рассылка всем участникам ММБ ===================
        if ($RaidId <= 0) {
            CMmb::setShortResult('Марш-бросок не найден', '');
            return;
        }

        // рассылать всем может только администратор
        if (!$Administrator) {
            return;
        }

        CMmb::setViews('ViewAdminDataPage', '');

        $pText = $_POST['MessageText'];
        $pSubject = $_POST['MessageSubject'];
        $pSendType = mmb_validateInt($_POST, 'SendForAllTypeId');

        if (empty($pSubject) || trim($pSubject) === 'Тема рассылки') {
            CMmb::setError('Укажите тему сообщения.', $view, '');
            return;
        }

        if (empty($pText) || trim($pText) === 'Текст сообщения') {
            CMmb::setError('Укажите текст сообщения.', $view, '');
            return;
        }

        if (empty($pSendType) || $pSendType == 0) {
            CMmb::setError('Укажите тип рассылки.', $view, '');
            return;
        }

        $Result = 0;

        //письмо о начале рассылки в группу
        SendMail('mmbsitedeveloper@googlegroups.com', 'Запущена рассылка', 'Админы и разработчики ММБ');

        $Result = SendMailForAll($RaidId, $pSubject, $pText, $pSendType);

        if ($Result == 1) {
            CMmb::setShortResult('Рассылка запущена', 'ViewAdminDataPage');
        } else {
            CMmb::setError('Ошибка при отправке рассылки.', $view, '');
            return;
        }
    } elseif ($action === 'RaidTeamUsersExport') {
        // =============== Генерация списка участников  ===================
        if ($RaidId <= 0) {
            CMmb::setShortResult('Марш-бросок не найден', '');
            return;
        }

        // рассылать всем может только администратор
        if (!$Administrator) {
            return;
        }

        CMmb::setViews('ViewAdminDataPage', '');

        $Sql = "select u.user_name, user_birthyear, 
			COALESCE(u.user_city, '') as user_city,
			COALESCE(u.user_phone, '') as user_phone,
		        t.team_num, t.team_name, t.team_outofrange  
		from Teams t 
			inner join Distances d on t.distance_id = d.distance_id
			inner join TeamUsers tu on tu.team_id = t.team_id
			inner join Users u on tu.user_id = u.user_id
		where t.team_hide = 0 
			and tu.teamuser_hide = 0
			and d.raid_id = $RaidId
		order by user_name
		";

        $Result = MySqlQuery($Sql);

        // Заголовки, чтобы скачивать можно было и на мобильных устройствах просто браузером (который не умеет делать Save as...)
        header('Content-Type: text/plain; charset=windows-1251');
        header('Content-Disposition: attachment; filename=raidteamusers.txt');

        // create a file pointer connected to the output stream
        $output = fopen('php://output', 'w');

        while (($Row = mysqli_fetch_assoc($Result))) {
            $strtowrite = trim($Row['user_name']) . ';' . $Row['user_birthyear'] . ';' . $Row['user_city'] . ';' .
                $Row['user_phone'] . ';' . $Row['team_num'] . ';' . trim($Row['team_name']) . ';' .
                $Row['team_outofrange'];
            fwrite($output, iconv('UTF-8', 'Windows-1251', $strtowrite) . "\n");
            //fputcsv($output, $Row, ';');
        }
        mysqli_free_result($Result);

        fclose($output);
        die();
    } elseif ($action === 'RankInvitations') {
        // =============== Выдача приглашений по рангу  ===================
        CMmb::setViews('ViewAdminDataPage', '');

        if ($RaidId <= 0) {
            CMmb::setError('Марш-бросок не найден.', $view, '');
            return;
        }

        $pInvitationsCount = mmb_validateInt($_POST, 'RankInvitationsCount');
        if ($pInvitationsCount <= 0) {
            CMmb::setError('Не указано число приглашений.', $view, '');
            return;
        }

        $pInvitationsEndDate = trim($_POST['InvitationsEndDate']);
        if (empty($pInvitationsEndDate)) {
            CMmb::setError('Не указана дата окончания действия приглашений.', $view, '');
            return;
        }

        // проверки на дату
        $sql = "select count(*) enddtcheck
    			from Raids r
	    		where  r.raid_id = $RaidId
	    			and r.raid_registrationenddate >=  '$pInvitationsEndDate'
	    			and NOW() < '$pInvitationsEndDate'
	    	";
        $endDtCheck = CSql::singleValue($sql, 'enddtcheck', false);

        if (!$endDtCheck) {
            CMmb::setErrorSm('Дата окончания действия приглашений не прошла проверку.');
            return;
        }

        // пересчитываем рейтинг на всякий случай
        RecalcUsersRank($RaidId);

        // проверяем
        if (!CRights::canDeliveryInvitation($UserId, $RaidId, 1)) {
            CMmb::setError('Невозможно провести выдачу по рейтингу.', $view, '');
            return;
        }

        if ($pInvitationsCount > CSql::availableInvitationsCount($RaidId)) {
            CMmb::setError('Заявок указано больше, чем доступно.', $view, '');
            return;
        }

        $sql = "insert into InvitationDeliveries (raid_id, invitationdelivery_type, invitationdelivery_dt, user_id, invitationdelivery_amount)
					VALUES ($RaidId, 1, NOW(), $UserId, $pInvitationsCount)
		";

        $newInvDeliveryId = MySqlQuery($sql);

        if ($newInvDeliveryId <= 0) {
            CMmb::setErrorSm('Ошибка записи раздачи приглашения.');
            return;
        }

        // на всякий случай ещё раз проверяем
        if (
            !CRights::canDeliveryInvitation($UserId, $RaidId, 1)
            || $pInvitationsCount > CSql::availableInvitationsCount($RaidId)
        ) {
            CMmb::setErrorMessage('Не хватает прав или нет доступных приглашений');
            return;
        }

        // можно добавить ещё проверку на то, что выдача по рангу не проводилась или, что лотерея не проводилась
        $pInvitationsEndDate = trim($pInvitationsEndDate) . ' 23:59:59';

        $sql = "insert into Invitations (user_id, invitation_begindt, invitation_enddt, invitationdelivery_id)
		SELECT 	u.user_id, NOW(), '$pInvitationsEndDate', $newInvDeliveryId
		FROM Users u
		WHERE COALESCE(u.user_noinvitation, 0) = 0
		ORDER BY u.user_r6 DESC
		LIMIT 0, $pInvitationsCount
		";

        MySqlQuery($sql);

        CMmb::setShortResult('Приглашения по рейтингу выданы', '');
        //CMmb::setResult('', "ViewAdminDataPage", "");
        return;
    } elseif ($action === 'LottoInvitations') {
        // =============== Розыгрыш лотереи  ===================
        CMmb::setViews('ViewAdminDataPage', '');

        if ($RaidId <= 0) {
            CMmb::setError('Марш-бросок не найден.', $view, '');
            return;
        }

        $pInvitationsCount = mmb_validateInt($_POST, 'LottoInvitationsCount');
        if ($pInvitationsCount <= 0) {
            CMmb::setError('Не указано число приглашений.', $view, '');
            return;
        }

        // пересчитываем рейтинг на всякий случай
        RecalcUsersRank($RaidId);

        // проверяем
        if (!CRights::canDeliveryInvitation($UserId, $RaidId, 2)) {
            CMmb::setError('Невозможно провести лотерею.', $view, '');
            return;
        }

        if ($pInvitationsCount > CSql::availableInvitationsCount($RaidId)) {
            CMmb::setError('Заявок указано больше, чем доступно.', $view, '');
            return;
        }

        // можно добавить ещё проверку на то, что выдача по рангу не проводилась или, что лотерея не проводилась

        $sql = "insert into InvitationDeliveries (raid_id, invitationdelivery_type, invitationdelivery_dt, user_id, invitationdelivery_amount)
					VALUES ($RaidId, 2, NOW(), $UserId, $pInvitationsCount)
		";

        $newInvDeliveryId = MySqlQuery($sql);

        if ($newInvDeliveryId <= 0) {
            CMmb::setErrorSm('Ошибка записи раздачи приглашения.');
            return;
        }

        // Здесь нужна фиксация всех пользователей, которые участвуют в этом розыгрыше
        // не допускаем к лотерее новых пользователей
        // Если нужно не пускать тех, кто не вышел на старт или дисквалифицирован, то  нужно дополнительно смотреть таблицу Users

        $sql = "update TeamUsers tu
			inner join Teams t
			on tu.team_id = t.team_id
			inner join Distances d
			on t.distance_id = d.distance_id
		set tu.invitationdelivery_id = $newInvDeliveryId
		where t.team_hide = 0
			and t.team_outofrange = 1
			and COALESCE(tu.teamuser_new, 0) = 0
			and tu.teamuser_hide = 0
			and d.raid_id = $RaidId
		";

        MySqlQuery($sql);

        // на всякий случай ещё раз проверяем
        if (
            !CRights::canDeliveryInvitation($UserId, $RaidId, 1)
            || $pInvitationsCount > CSql::availableInvitationsCount($RaidId)
        ) {
            CMmb::setErrorMessage('Не хватает прав или нет доступных приглашений');
            return;
        }

        // А вот дальше собственно вставка приглашений на основании лотереи
        // и их привязка к команде
        $sql = " CREATE TEMPORARY TABLE
				tmp_lottoteams (
				 num INT NOT NULL AUTO_INCREMENT PRIMARY KEY, 
                                 team_id INT, 
                                 user_id  INT
				        ) 
				ENGINE=MEMORY, AUTO_INCREMENT = 0";
        $rs = MySqlQuery($sql);

        // отбираем в таблицу команды вне зачета с автонумерованной первой колонкой
        $sql = " INSERT INTO tmp_lottoteams (team_id, user_id)
			 select t.team_id, MIN(tu.user_id) as user_id 
         			from   Teams t
        				inner join TeamUsers tu
				        on t.team_id = tu.team_id
					inner join Distances d
					on t.distance_id = d.distance_id
           			where tu.invitationdelivery_id = $newInvDeliveryId
				group by t.team_id
			";
        $rs = MySqlQuery($sql);

        // на всякий случай ещё раз проверяем
        if (
            !CRights::canDeliveryInvitation($UserId, $RaidId, 1)
            || $pInvitationsCount > CSql::availableInvitationsCount($RaidId)
        ) {
            CMmb::setErrorMessage('Не хватает прав или нет доступных приглашений');
            return;
        }

        // Если нужны разные вероятности, то можно добавить столько раз строчку команды, сколько "веса" она должна получить
        // находим случайно нужное число команд
        // вставляем приглашения

        $sql = "insert into Invitations (user_id, invitation_begindt, invitation_enddt, invitationdelivery_id)
		 select user_id, NOW(), NOW(), $newInvDeliveryId
		 from tmp_lottoteams
 		 ORDER BY RAND()
		 LIMIT $pInvitationsCount";

        $rs = MySqlQuery($sql);

        // активируем команды, которые связаны с одной стороны с временной таблицы, а через неё с приглашениями
        $sql = "update  Teams t
		  	 inner join  tmp_lottoteams tmp
			 on t.team_id = tmp.team_id
			 inner join Invitations inv
			 on inv.user_id = tmp.user_id
		 set t.team_outofrange = 0, t.invitation_id = inv.invitation_id
		 where inv.invitationdelivery_id = $newInvDeliveryId";

        $rs = MySqlQuery($sql);

//	$sql = " DELETE FROM tmp_lottoteams  ";
//	$rs = MySqlQuery($sql);

        CMmb::setShortResult('Лотерея проведена', '');
        //CMmb::setResult('Лотерея проведена', "ViewAdminDataPage", "");
        return;
    } elseif ($action === 'RankRecalc') {
        // =============== Пересчет рейтинга пользователей администратором ===================
        if ($RaidId <= 0) {
            CMmb::setShortResult('Марш-бросок не найден', '');
            return;
        }

        if (!$Administrator && !$Moderator) {
            return;
        }

        RecalcUsersRank($RaidId);

        CMmb::setShortResult('Рейтинг пересчитан', 'ViewAdminDataPage');
    } elseif ($action === 'DeleteOutOfRangeTeams') {
        // =============== Удаление команд вне зачета   ===================
        CMmb::setViews('ViewAdminDataPage', '');

        if ($RaidId <= 0) {
            CMmb::setError('Марш-бросок не найден.', $view, '');
            return;
        }

        // проверяем
        if (!CRights::canDeleteOutOfRangeTeams($UserId, $RaidId)) {
            CMmb::setError('Невозможно удалить команды вне зачета.', $view, '');
            return;
        }

        // рассылка
        SendMailForAll(
            $RaidId,
            "Удаление команд, не получивших приглашения",
            "К сожалению, ваша команда не получила приглашения и будет удалена.",
            5
        );

        $sql = "UPDATE TeamUsers tu INNER JOIN Teams t on tu.team_id = t.team_id INNER JOIN Distances d on t.distance_id = d.distance_id  
		SET tu.teamuser_hide = 1  
		WHERE d.raid_id = $RaidId and t.team_outofrange = 1
		";

        MySqlQuery($sql);

        $sql = "UPDATE Teams t INNER JOIN Distances d on t.distance_id = d.distance_id 
		SET t.team_hide = 1  
		WHERE d.raid_id = $RaidId and t.team_outofrange = 1";

        MySqlQuery($sql);

        CMmb::setShortResult('Команды вне зачета удалены', '');
        //CMmb::setResult('', "ViewAdminDataPage", "");
        return;
    } // =============== Никаких действий не требуется ==============================
    else {
    }
}

// Сохранение флага ошибки в базе
// функция больше не используется
function LogError($teamlevel_id, $error)
{
    $sql = "update TeamLevels set error_id = $error where teamlevel_id = $teamlevel_id";
    $Result = MySqlQuery($sql);
    return ($error);
}

// Проверка конкретной команды
// функция больше не используется
function ValidateTeam($Team, $Levels)
{
    // Получаем список записей результатов из TeamLevels
    foreach ($Levels['level_id'] as $n => $level_id) {
        $sql = "select * from TeamLevels where level_id = $level_id and team_id = {$Team['team_id']} and teamlevel_hide = 0";
        $Result = MySqlQuery($sql);
        if (mysqli_num_rows($Result) > 1) {
            die('Несколько записей на один этап для команды ' . $Team['team_id']);
        }
        $Row = mysqli_fetch_assoc($Result);
        if ($Row) {
            $TeamLevels[$n] = $Row;
        }
        mysqli_free_result($Result);
    }
    // Проверяем все этапы, о которых есть записи в таблицах
    $team_result = 0;
    $team_progress = 0;
    $finished = 1;
    if (isset($TeamLevels)) {
        foreach ($TeamLevels as $n => $teamlevel) {
            $begtime = strtotime($teamlevel['teamlevel_begtime']);
            $endtime = strtotime($teamlevel['teamlevel_endtime']);
            // проверяем абсолютную корректность времени старта и финиша
            if ($begtime && (($begtime < $Levels['level_begtime'][$n]) || ($begtime > $Levels['level_maxbegtime'][$n]))) {
                return (LogError($teamlevel['teamlevel_id'], 1));
            }
            if (!$begtime && ($Levels['level_starttype'][$n] == 1)) {
                return (LogError($teamlevel['teamlevel_id'], 2));
            }
            if ($begtime && (($Levels['level_starttype'][$n] == 2) || ($Levels['level_starttype'][$n] == 3))) {
                return (LogError($teamlevel['teamlevel_id'], 3));
            }
            if ($endtime && (($endtime < $Levels['level_minendtime'][$n]) || ($endtime > $Levels['level_endtime'][$n]))) {
                return (LogError($teamlevel['teamlevel_id'], 4));
            }
            // вычисляем время старта, если он общий или в момент финиша на пред.этапе
            if ($Levels['level_starttype'][$n] == 2) {
                $begtime = $Levels['level_begtime'][$n];
            }
            if (($Levels['level_starttype'][$n] == 3) && isset($TeamLevels[$n - 1])) {
                $begtime = strtotime($TeamLevels[$n - 1]['teamlevel_endtime']);
            }
            // сравниваем время старта и финиша
            if ($begtime && $endtime && ($begtime >= $endtime)) {
                return (LogError($teamlevel['teamlevel_id'], 5));
            }
            if ($begtime && $endtime && (($endtime - $begtime) < 3 * 3600)) {
                return (LogError($teamlevel['teamlevel_id'], -1));
            }
            /* if ($begtime && $endtime && (($endtime - $begtime) > 23*3600)) return(LogError($teamlevel['teamlevel_id'], -2)); */
            // проверяем корректность прогресса на дистанции
            if ($teamlevel['teamlevel_begtime'] && ($teamlevel['teamlevel_progress'] == 0)) {
                return (LogError($teamlevel['teamlevel_id'], 6));
            }
            if ($endtime && ($teamlevel['teamlevel_progress'] <> 2)) {
                return (LogError($teamlevel['teamlevel_id'], 7));
            }
            if (!$endtime && ($teamlevel['teamlevel_progress'] == 2)) {
                return (LogError($teamlevel['teamlevel_id'], 8));
            }
            // проверяем наличие времени финиша и списка КП у финишировавшей команды
            if (($teamlevel['teamlevel_endtime'] == "") && !(strpos($teamlevel['teamlevel_points'], "1") === false)) {
                return (LogError($teamlevel['teamlevel_id'], 12));
            }
            if (($teamlevel['teamlevel_endtime'] != "") && ($teamlevel['teamlevel_points'] == "")) {
                return (LogError($teamlevel['teamlevel_id'], 13));
            }
            // проверяем длину списка КП и пересчитываем штраф
            $level_pointpenalties = explode(',', $Levels['level_pointpenalties'][$n]);
            $level_discountpoints = explode(',', $Levels['level_discountpoints'][$n]);
            if ($teamlevel['teamlevel_points'] == "") {
                unset($teamlevel_points);
                foreach ($level_pointpenalties as $penalty) {
                    $teamlevel_points[] = "0";
                }
            } else {
                $teamlevel_points = explode(',', $teamlevel['teamlevel_points']);
            }
            if (count($teamlevel_points) <> count($level_pointpenalties)) {
                return (LogError($teamlevel['teamlevel_id'], 9));
            }
            $teamlevel_penalty = 0;
            $teamlevel_selectpenalty = 0;
            foreach ($teamlevel_points as $npoint => $point) {
                if (empty($level_pointpenalties[$npoint])) {
                    $NowLevelPointPenalty = 0;
                } else {
                    $NowLevelPointPenalty = (int)$level_pointpenalties[$npoint];
                }

                if ((($point == "0") && ($NowLevelPointPenalty > 0)) || (($point == "1") && ($NowLevelPointPenalty < 0))) {
                    if (!empty($level_discountpoints[$npoint])) {
                        $teamlevel_selectpenalty += $NowLevelPointPenalty;
                    } else {
                        $teamlevel_penalty += $NowLevelPointPenalty;
                    }
                }
            }
            if ($Levels['level_discount'][$n]) {
                $teamlevel_selectpenalty -= $Levels['level_discount'][$n];
                if ($teamlevel_selectpenalty < 0) {
                    $teamlevel_selectpenalty = 0;
                }
            }
            $teamlevel_penalty += $teamlevel_selectpenalty;
            if ($teamlevel_penalty <> $teamlevel['teamlevel_penalty']) {
                return (LogError($teamlevel['teamlevel_id'], 10));
            }
            // пока считаем, что ошибок на этапе нет
            LogError($teamlevel['teamlevel_id'], 0);
            // добавляем результаты этапа к общему результату
            if ($begtime && $endtime) {
                $team_result += ($endtime - $begtime) / 60;
            }
            $team_result += $teamlevel_penalty;
            $team_progress += (int)$teamlevel['teamlevel_progress'];
            if ($teamlevel['teamlevel_progress'] <> 2) {
                $finished = 0;
            }
        }
    }
    // Считаем, что на отсутствующие в базе записи о прохождении этапов команда не выходила
    foreach ($Levels['level_id'] as $n => $level_id) {
        if (!isset($TeamLevels[$n])) {
            $TeamLevels[$n]['teamlevel_progress'] = 0;
            $finished = 0;
        }
    }
    // Смотрим, чтобы после схода команда опять не появлялась на дистанции
    foreach ($TeamLevels as $n => $teamlevel) {
        if ($n > 1) {
            if ($teamlevel['teamlevel_progress'] > $TeamLevels[$n - 1]['teamlevel_progress']) {
                return (LogError($teamlevel['teamlevel_id'], 11));
            }
            if (($teamlevel['teamlevel_progress'] == 1) && ($TeamLevels[$n - 1]['teamlevel_progress'] == 1)) {
                return (LogError($teamlevel['teamlevel_id'], 11));
            }
        }
    }
    // Сверяем итоговые прогресс и результат команды
    if (!$finished) {
        $team_result = "";
    } else {
        $team_result = sprintf("%d:%02d:00", $team_result / 60, $team_result % 60);
    }
    if ($team_result <> $Team['team_result']) {
        echo "Ошибка подсчета итогового времени у команды {$Team['team_id']}: правильное=$team_result, в базе={$Team['team_result']}<br/>";
    }
    if ($team_progress <> $Team['team_progress']) {
        echo "Ошибка подсчета степени продвижения по дистанции у команды {$Team['team_id']}: правильное=$team_result, в базе={$Team['team_result']}<br/>";
    }

    // Ошибок в результатах команды не обнаружено
    return (0);
}

?>
