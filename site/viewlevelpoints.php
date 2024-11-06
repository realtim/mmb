<?php

/**
 * +++++++++++ Добавление/правка/показ точек ++++++++++++++++++++++++++++
 */

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) {
    return;
}

if (!isset($viewmode)) {
    $viewmode = "";
}
if (!isset($viewsubmode)) {
    $viewsubmode = "";
}


// Определяем права по редактированию 
if ($Administrator || $Moderator) {
    $AllowEdit = 1;
    $DisabledText = '';
    $OnSubmitFunction = 'return ValidateLevelPointForm();';
} else {
    $AllowEdit = 0;
    $DisabledText = ' disabled';
    $OnSubmitFunction = 'return false;';
}
// Определяем права по просмотру (после закрытия финиша)
if ($Administrator || $Moderator || $RaidStage >= 5) {
    $AllowViewResults = 1;
} else {
    $AllowViewResults = 0;
}

// ================ Форма для добавления новой  точки ===================================
if ($viewmode === 'Add') {
    // Если вернулись после ошибки переменные не нужно инициализировать
    if ($viewsubmode === "ReturnAfterError") {
        ReverseClearArrays();

        $PointTypeId = $_POST['PointTypeId'];
        $DistanceId = mmb_validateInt($_POST, 'DistanceId');
        $PointName = $_POST['PointName'];
        $PointPenalty = $_POST['PointPenalty'];
        $LevelPointMinYear = $_POST['MinYear'];
        $LevelPointMinDate = $_POST['MinDate'];
        $LevelPointMinTime = $_POST['MinTime'];
        $LevelPointMaxYear = $_POST['MaxYear'];
        $LevelPointMaxDate = $_POST['MaxDate'];
        $LevelPointMaxTime = $_POST['MaxTime'];
        //$ScanPointId = $_POST['ScanPointId'];
        //$LevelId = $_POST['LevelId'];

    } else {
        // Инициализация переменных для новой точки
        // дистанция уже должна быть инициализирована в raidactions!
        if (empty($DistanceId)) {
            return;
        }

        // 18/03/2014 По умолчанию ставим  тип КП
        $PointTypeId = 5;
        $PointName = 'Название КП';
        $PointPenalty = 0;

        $LevelPointMinYear = '';
        $LevelPointMinDate = '';
        $LevelPointMinTime = '';
        $LevelPointMaxYear = '';
        $LevelPointMaxDate = '';
        $LevelPointMaxTime = '';

        // $ScanPointId = 0;
        //  $LevelId = 0;

    }

    // Определяем следующее действие
    $NextActionName = 'AddLevelPoint';
    // Действие на текстовом поле по клику
    $OnClickText = ' onClick="javascript:this.value = \'\';"';
    // Надпись на кнопке
    $SaveButtonText = 'Добавить точку';

    $pLevelPointId = 0;
} else // ================ Редактируем/смотрим существующую точку =================
{
    $pLevelPointId = mmb_validateInt($_POST, 'LevelPointId');

    if ($pLevelPointId <= 0) {
        return;
    }

    // 19.06.2015 Добавил level_id чтобы проставить ввести точки по старым ММБ,
    // потом можно и нужно убрать

    $sql = "select 
               lp.levelpoint_id, 
               pt.pointtype_id, 
               pt.pointtype_name,  
	           lp.levelpoint_name, 
	           lp.levelpoint_penalty, 
		       lp.distance_id, 
		       lp.levelpoint_order,
		       lp.levelpoint_mindatetime, 
		       lp.levelpoint_maxdatetime,
		       DATE_FORMAT(lp.levelpoint_mindatetime, '%Y') as levelpoint_sminyear,
		       DATE_FORMAT(lp.levelpoint_mindatetime, '%d%m') as levelpoint_smindate,
		       DATE_FORMAT(lp.levelpoint_mindatetime, '%H%i') as levelpoint_smintime,
		       DATE_FORMAT(lp.levelpoint_maxdatetime, '%Y') as levelpoint_smaxyear,
		       DATE_FORMAT(lp.levelpoint_maxdatetime, '%d%m') as levelpoint_smaxdate,
		       DATE_FORMAT(lp.levelpoint_maxdatetime, '%H%i') as levelpoint_smaxtime,
		       lp.scanpoint_id
		from LevelPoints lp
		     inner join PointTypes pt
		     on lp.pointtype_id = pt.pointtype_id
		where lp.levelpoint_id = $pLevelPointId";
    $Row = CSql::singleRow($sql);

    // Если вернулись после ошибки переменные не нужно инициализировать
    if ($viewsubmode === "ReturnAfterError") {
        ReverseClearArrays();
        $PointTypeId = mmb_validateInt($_POST, 'PointTypeId');
        $DistanceId = mmb_validateInt($_POST, 'DistanceId');
        $PointName = $_POST['PointName'];
        $PointPenalty = mmb_validateInt($_POST, 'PointPenalty');
        $LevelPointMinYear = $_POST['MinYear'];
        $LevelPointMinDate = $_POST['MinDate'];
        $LevelPointMinTime = $_POST['MinTime'];
        $LevelPointMaxYear = $_POST['MaxYear'];
        $LevelPointMaxDate = $_POST['MaxDate'];
        $LevelPointMaxTime = $_POST['MaxTime'];
        //$ScanPointId = mmb_validateInt($_POST, 'ScanPointId');
        //	$LevelId = $_POST['LevelId'];
    } else {
        $PointTypeId = $Row['pointtype_id'];
        $DistanceId = $Row['distance_id'];
        $PointName = $Row['levelpoint_name'];
        $PointPenalty = $Row['levelpoint_penalty'];
        $LevelPointMinYear = $Row['levelpoint_sminyear'];
        $LevelPointMinDate = $Row['levelpoint_smindate'];
        $LevelPointMinTime = $Row['levelpoint_smintime'];
        $LevelPointMaxYear = $Row['levelpoint_smaxyear'];
        $LevelPointMaxDate = $Row['levelpoint_smaxdate'];
        $LevelPointMaxTime = $Row['levelpoint_smaxtime'];
        //	$ScanPointId = $Row['scanpoint_id'];
        //    $LevelId = $Row['level_id'];
    }

    $NextActionName = 'LevelPointChange';
    $OnClickText = '';
    $SaveButtonText = 'Сохранить изменения точки';
}
// ================ Конец инициализации переменных для добавляемой/редактируемой точки =================


// В форме правки выводится только день и время, год считаем по дате регистрации ММБ и не выводим

if (empty($LevelPointMinYear) || (int)$LevelPointMinYear == 0 or
    empty($LevelPointMaxYear) || (int)$LevelPointMaxYear == 0) {
    $RaidYear = 0;
    $sql = "select YEAR(NOW()) as raidyear
			from Raids r
		             inner join Distances d
			     on d.raid_id = r.raid_id
			where d.distance_id = $DistanceId";

    $RaidYear = CSql::singleValue($sql, 'raidyear');

    if (empty($LevelPointMinYear) || (int)$LevelPointMinYear === 0) {
        $LevelPointMinYear = $RaidYear;
    }
    if (empty($LevelPointMaxYear) || (int)$LevelPointMaxYear === 0) {
        $LevelPointMaxYear = $RaidYear;
    }
}

// Выводим javascrpit
?>

<script language="JavaScript" type="text/javascript">
    // Функция проверки правильности заполнения формы
    function ValidateLevelPointForm() {
        document.LevelPointForm.action.value = "<?php echo $NextActionName; ?>";
        return true;
    }

    // Конец проверки правильности заполнения формы

    // Скрыть точку
    function HideLevelPoint() {
        document.LevelPointForm.action.value = 'HideLevelPoint';
        document.LevelPointForm.submit();
    }

    // Поднять точку (уменьшить порядковый) номер
    function LevelPointUp() {
        document.LevelPointForm.action.value = 'LevelPointOrderDown';
        document.LevelPointForm.submit();
    }

    // Опустить точку (увеличить порядковый номер)
    function LevelPointDown() {
        document.LevelPointForm.action.value = 'LevelPointOrderUp';
        document.LevelPointForm.submit();
    }

    // Править точку
    function EditLevelPoint(levelpointid) {
        document.LevelPointForm.LevelPointId.value = levelpointid;
        document.LevelPointForm.action.value = 'LevelPointInfo';
        document.LevelPointForm.submit();
    }

    // Функция пересоздания этапов
    function RecalculateLevels() {
        document.RecalculateLevelsForm.action.value = "RecalculateLevels";
        document.RecalculateLevelsForm.submit();
    }

    // Функция отмены изменения
    function Cancel() {
        document.LevelPointForm.action.value = "CancelChangeLevelPoint";
        document.LevelPointForm.submit();
    }

</script>


<?php

$TabIndex = 0;

// отдельная маленькая форма-список с дистанциями

print('<form name="DistancesForm" action="' . $MyPHPScript . '" method="post" >' . "\n");
print('<input type="hidden" name="action" value="ViewLevelPointsPage">' . "\n");
print('<input type="hidden" name="view" value="ViewLevelPoints">' . "\n");
print('<input type="hidden" name="RaidId" value="' . $RaidId . '">' . "\n");
print('<table style="font-size: 80%;" border="0" cellpadding="2" cellspacing="0">' . "\n\n");
print('<tr><td class="input">' . "\n");
print('Дистанция: </span>' . "\n");
// Показываем выпадающий список файлов
print('<select name="DistanceId" class="leftmargin" tabindex="' . (++$TabIndex) . '"  onchange="javascript: submit();">' . "\n");
$sql = "select distance_id, distance_name from Distances where distance_hide = 0  and raid_id = $RaidId order by distance_id ";
$Result = MySqlQuery($sql);
while ($Row = mysqli_fetch_assoc($Result)) {
    $distanceselected = ($Row['distance_id'] == $DistanceId ? 'selected' : '');
    print('<option value="' . $Row['distance_id'] . '" ' . $distanceselected . ' >' . $Row['distance_name'] . "</option>\n");
}

mysqli_free_result($Result);
print('</select>' . "\n");
print('</td></tr>' . "\n\n");
print('</table>' . "\n");
print('</form>' . "\r\n");
print('</br>' . "\r\n");


if ($AllowEdit == 1) {
    // Выводим начало формы с точкой
    print('<form name="LevelPointForm" action="' . $MyPHPScript . '" method="post"  onSubmit="' . $OnSubmitFunction . '">' . "\n");
    print('<input type="hidden" name="action" value="">' . "\n");
    print('<input type="hidden" name="view" value="ViewLevelPoints">' . "\n");
    print('<input type="hidden" name="RaidId" value="' . $RaidId . '">' . "\n");
    print('<input type="hidden" name="DistanceId" value="' . $DistanceId . '">' . "\n");
    //print('<input type="hidden" name="UserId" value="0">'."\n\n");
    //print('<input type="hidden" name="LevelPointId" value="'.$LevelPointId.'">'."\n");
    print('<input type="hidden" name="LevelPointId" value="' . $pLevelPointId . '">' . "\n\n");
    print("<table class=\"control\">\r\n");

    $DisabledText = '';

    // 19.06.2015 Добавил level_id чтобы проставить ввести точки по старым ММБ,
    // потом можно и нужно убрать

    /*
        print('<tr><td class="input">'."\n");


        print('Этап (только для старых ММБ!)'."\n");
        print('<select name="LevelId" class="leftmargin" tabindex="'.(++$TabIndex).'"'.$DisabledText.'>'."\n");
        $sql = "select level_id, level_name from Levels where level_hide = 0  and distance_id = ".$DistanceId." order by level_order ";
        $Result = MySqlQuery($sql);

        print('<option value="0">Не указан</option>'."\n");

        while ($Row = mysqli_fetch_assoc($Result))
        {
            $levelselected = ($Row['level_id'] == $LevelId ? 'selected' : '');
            print('<option value="'.$Row['level_id'].'" '.$levelselected.' >'.$Row['level_name']."</option>\n");
        }
        mysqli_free_result($Result);
        print('</select>'."\n");

        print('</td></tr>'."\n\n");
    */
    print('<tr><td class="input">' . "\n");


//	print('<span>Скан-Точка</span>'."\n");
//	print('<select name="ScanPointId" class="leftmargin" tabindex="'.(++$TabIndex).'"'.$DisabledText.'>'."\n");
//	$sql = "select scanpoint_id, scanpoint_name from ScanPoints where scanpoint_hide = 0  and raid_id = ".$RaidId." order by scanpoint_order ";
//	$Result = MySqlQuery($sql);

//	print('<option value="0">Не указана</option>'."\n");

//	while ($Row = mysqli_fetch_assoc($Result))
//	{
//		$scanpointselected = ($Row['scanpoint_id'] == $ScanPointId ? 'selected' : '');
//		print('<option value="'.$Row['scanpoint_id'].'" '.$scanpointselected.' >'.$Row['scanpoint_name']."</option>\n");
//	}
//	mysqli_free_result($Result);
//	print('</select>'."\n");


    print('Тип Точки' . "\n");
    // Показываем выпадающий список файлов
    print('<select name="PointTypeId" class="leftmargin" tabindex="' . (++$TabIndex) . '"' . $DisabledText . '>' . "\n");
    $sql = "select pointtype_id, pointtype_name from PointTypes ";
    $Result = MySqlQuery($sql);
    while ($Row = mysqli_fetch_assoc($Result)) {
        $pointtypeselected = ($Row['pointtype_id'] == $PointTypeId ? 'selected' : '');
        print('<option value="' . $Row['pointtype_id'] . '" ' . $pointtypeselected . ' >' . $Row['pointtype_name'] . "</option>\n");
    }
    mysqli_free_result($Result);
    print('</select>' . "\n");

    print("</td></tr>\r\n");
    print("<tr><td class=\"input\">\n");


    print('<input type="text" name="PointName" size="20" value="' . $PointName . '" tabindex = "' . (++$TabIndex) . '"   ' . $DisabledText . ' '
        . ($viewmode !== 'Add' ? '' : CMmbUI::placeholder($PointName))
        . ' title = "Название КП">' . "\r\n");

    print(' Штраф: <input type="text" name="PointPenalty" size="5" value="' . $PointPenalty . '" tabindex = "' . (++$TabIndex) . '"   ' . $DisabledText . ' '
        . ($viewmode !== 'Add' ? '' : CMmbUI::placeholder($PointPenalty))
        . ' title = "Штраф за невзятие КП в минутах">' . "\r\n");


    print("</td></tr>\r\n");

    print("<tr><td>\r\n");

    print("Дата (ддмм) и время (ччмм) ограничений для прохождения точки: <br/>  снизу\r\n");

    // Можно отключить правку написав  $MinDateReadOnly = 'readonly'
    $MinDateReadOnly = '';

    print('<input type="hidden" maxlength="4" name="MinYear" size="3" value="' . $LevelPointMinYear . '" >' . "\n");
    print('<input type="Text" maxlength="4" name="MinDate" size="3" value="' . $LevelPointMinDate . '" tabindex="' . (++$TabIndex) . '"' . $MinDateReadOnly . ' title="ддмм - день месяц без разделителя" onclick="this.select();" onkeydown="if (event.keyCode == 13 && this.value.length == 4) {document.LevelPointForm.MinTime.focus();}">' . "\n");
    print('<input type="Text" maxlength="4" name="MinTime" size="3" value="' . $LevelPointMinTime . '" tabindex="' . (++$TabIndex) . '"' . ' onclick="this.select();" title="ччмм - часы минуты без разделителя">' . "\n");

    print(' и сверху' . "\n\n");


    $MaxDateReadOnly = '';

    print('<input type="hidden" maxlength="4" name="MaxYear" size="3" value="' . $LevelPointMaxYear . '" >' . "\n");
    print('<input type="Text" maxlength="4" name="MaxDate" size="3" value="' . $LevelPointMaxDate . '" tabindex="' . (++$TabIndex) . '"' . $MaxDateReadOnly . ' title="ддмм - день месяц без разделителя" onclick="this.select();" onkeydown="if (event.keyCode == 13 && this.value.length == 4) {document.LevelPointForm.MaxTime.focus();}">' . "\n");
    print('<input type="Text" maxlength="4" name="MaxTime" size="3" value="' . $LevelPointMaxTime . '" tabindex="' . (++$TabIndex) . '"' . ' onclick="this.select();" title="ччмм - часы минуты без разделителя">' . "\n");


    print('</td></tr>' . "\n\n");

    // ================ Submit для формы ==========================================
    print('</tr><td class="input" style="padding-top: 20px;">' . "\n");
    print('<input type="button" onClick="javascript: if (ValidateLevelPointForm()) submit();" name="RegisterButton" value="' . $SaveButtonText . '" tabindex="' . (++$TabIndex) . '">' . "\n");
//	print('<input type="button" onClick="javascript: Cancel();" name="CancelButton" value="Отмена" tabindex="'.(++$TabIndex).'">'."\n");


    // ============ Кнопка удаления точки
    if (($viewmode !== "Add") && ($AllowEdit == 1)) {
        print('&nbsp; <input type="button" style="margin-left: 20px;" onClick="javascript: {LevelPointUp();}" name="LevelPointUpButton" value="Поднять точку" tabindex="' . (++$TabIndex) . '">' . "\n");
        print('&nbsp; <input type="button" style="margin-left: 20px;" onClick="javascript: {LevelPointDown();}" name="LevelPointDownButton" value="Опустить точку" tabindex="' . (++$TabIndex) . '">' . "\n");
        print('&nbsp; <input type="button" style="margin-left: 30px;" onClick="javascript: if (confirm(\'Вы уверены, что хотите удалить точку: ' . trim(
                $PointName
            ) . '? \')) {HideLevelPoint();}" name="HideLevelPointButton" value="Удалить точку" tabindex="' . (++$TabIndex) . '">' . "\n");
    }

    print("</td></tr>\r\n");
    print('<tr><td>После добавления, дистанцию у точки изменить нельзя - только удалив и добавив точку заново.
	               <br/> Амнистия редактируется в <a href="?amnesty&RaidId=' . $RaidId . '">отдельном окне</a>.</td></tr>' . "\r\n");

    print("</table>\r\n");
    print("</form>\r\n");
}

print("<br/>\n");


if (empty($DistanceId)) {
    return;
}


if ($AllowViewResults == 1) {
    // Список точек
    // 19.06.2015 Добавил level_id чтобы проставить ввести точки по старым ММБ,
    // потом можно и нужно убрать
// 		     left outer join Levels l
//		     on lp.level_id = l.level_id
//
//		       COALESCE(l.level_name, 'Не указан') as level_name


    $sql = "select lp.levelpoint_id, pt.pointtype_id, pt.pointtype_name,  
	               lp.levelpoint_name, lp.levelpoint_penalty, 
		       lp.distance_id, lp.levelpoint_order,
		       DATE_FORMAT(COALESCE(lp.levelpoint_mindatetime, '0000-00-00 00:00:00'), '%m-%d %H:%i') as levelpoint_mindatetime,
		       DATE_FORMAT(COALESCE(lp.levelpoint_maxdatetime, '0000-00-00 00:00:00'), '%m-%d %H:%i') as levelpoint_maxdatetime,
		       COALESCE(lpd.levelpointdiscount_value, 0) as levelpoint_discount,
		       COALESCE(sp.scanpoint_name, 'Не указана') as scanpoint_name,
		       COALESCE(sp.scanpoint_id, 0)  as scanpoint_id,
		       tlp1.teamcount,
		       tlp2.teamusercount
		from LevelPoints lp
		     inner join PointTypes pt
		     on lp.pointtype_id = pt.pointtype_id
		     left outer join LevelPointDiscounts lpd
		     on lp.distance_id = lpd.distance_id
		        and lpd.levelpointdiscount_hide = 0
			and lpd.levelpointdiscount_start <= lp.levelpoint_order
			and lpd.levelpointdiscount_finish >= lp.levelpoint_order
		     left outer join ScanPoints sp
		     on lp.scanpoint_id = sp.scanpoint_id
		     left outer join
		     (select tlp.levelpoint_id, count(t.team_id) as teamcount
		     from TeamLevelPoints tlp
		          inner join Teams t on t.team_id = tlp.team_id
		     where t.distance_id = $DistanceId
		     group by tlp.levelpoint_id
		     ) tlp1
		     on lp.levelpoint_id = tlp1.levelpoint_id
		     left outer join
		     (select tlp.levelpoint_id, count(tu.teamuser_id) as teamusercount
		     from TeamLevelPoints tlp
		          inner join Teams t on t.team_id = tlp.team_id
		          inner join TeamUsers tu on t.team_id = tu.team_id
		     where t.distance_id = $DistanceId
		     group by tlp.levelpoint_id
		     ) tlp2
		     on lp.levelpoint_id = tlp2.levelpoint_id
		where lp.levelpoint_hide = 0 and lp.distance_id = $DistanceId
		order by levelpoint_order";


    $Result = MySqlQuery($sql);

    print("<table class=\"std\">\r\n");

//                         <td width = "100" style = "'.$thstyle.'">Этап</td>
//                         <td width="100">Скан-точка</td>

    print('<tr class="head gray">
		         <td width="50">N п/п</td>
 		         <td width="150">Тип</td>
		         <td width="200">Название (Команд/Участников)</td>
		         <td width="150">Штраф (минуты)</td>
		         <td width="100">с</td>
		         <td width="100">по</td>
		         <td width="100">Амнистия</td>
		         ' . "\r\n");

    if ($AllowEdit == 1) {
        print('<td width="100">&nbsp;</td>' . "\r\n");
    }


    print("</tr>\r\n");

    // Сканируем команды
    while ($Row = mysqli_fetch_assoc($Result)) {
        //   print('<tr class = "'.$TrClass.'">'."\r\n");
//                            <td align = "left" style = "'.$tdstyle.'">'.$Row['level_name'].'</td>

        //                       <td>{$Row['scanpoint_name']}</td>

        print("<tr>\r\n");
        print("<td>{$Row['levelpoint_order']}</td>
     		            <td>{$Row['pointtype_name']}</td>
		            <td>{$Row['levelpoint_name']}({$Row['teamcount']}/{$Row['teamusercount']})</td>
		            <td>{$Row['levelpoint_penalty']}</td>
		            <td>{$Row['levelpoint_mindatetime']}</td>
		            <td>{$Row['levelpoint_maxdatetime']}</td>
		            <td>{$Row['levelpoint_discount']}</td>
		            ");

        if ($AllowEdit == 1) {
            print('<td>');
            print('&nbsp; <input type="button" onClick="javascript: EditLevelPoint(' . $Row['levelpoint_id'] . ');" name="EditLevelPointButton" value="Править" tabindex="' . (++$TabIndex) . '">' . "\n");
            print('</td>' . "\r\n");
        }
        print("</tr>\r\n");
    }

    mysqli_free_result($Result);
    print("</table>\r\n");


    print("</br>\n");
}
// Конец проверки прав на просмотр точек

/*
// ============ Кнопка пересоздания этапов и список этапов
 if ($AllowEdit == 1)
 {

	print('<form name="RecalculateLevelsForm" action="'.$MyPHPScript.'" method="post"  onSubmit="'.$OnSubmitFunction.'">'."\n");
	print('<input type="hidden" name="action" value="">'."\n");
	print('<input type="hidden" name="view" value="ViewLevelPoints">'."\n");
	print('<input type="hidden" name="RaidId" value="'.$RaidId.'">'."\n");
	print('<input type="hidden" name="DistanceId" value="'.$DistanceId.'">'."\n");
	print('<table style="font-size: 80%;" border="0" cellpadding="2" cellspacing="0">'."\n\n");
	print('<tr><td>'."\n\n");
	print('<input type="button" onClick="javascript: if (confirm(\'Вы уверены, что хотите пересоздать этапы? \')) {RecalculateLevels();}" name="RecalculateLevelsButton" value="Пересоздать этапы" tabindex="'.(++$TabIndex).'">'."\n");
	print('</td></tr>'."\n\n");
	print('</table>'."\r\n");
	print('</form>'."\r\n");
	
  
        print('</br>'."\n");

	// Список этапов

	$sql = "select l.level_id, l.level_order, l.level_name,   
                       l.level_starttype, 
      		       DATE_FORMAT(COALESCE(l.level_begtime, '0000-00-00 00:00:00'), '%m-%d %H:%i') as level_begtime,
      		       DATE_FORMAT(COALESCE(l.level_maxbegtime, '0000-00-00 00:00:00'), '%m-%d %H:%i') as level_maxbegtime,
      		       DATE_FORMAT(COALESCE(l.level_minendtime, '0000-00-00 00:00:00'), '%m-%d %H:%i') as level_minendtime,
      		       DATE_FORMAT(COALESCE(l.level_endtime, '0000-00-00 00:00:00'), '%m-%d %H:%i') as level_endtime,
	               l.level_pointnames, l.level_pointpenalties, level_discountpoints, level_discount
		from Levels l
		where l.level_hide = 0 and l.distance_id = ".$DistanceId."
		order by level_order";

	$Result = MySqlQuery($sql);
	
	$tdstyle = 'padding: 5px 0px 2px 5px;';		
        $thstyle = 'padding: 5px 0px 0px 5px;';		


	print('<table border = "1" cellpadding = "0" cellspacing = "0" style = "font-size: 80%">'."\r\n");  

	print('<tr class = "gray">
	         <td width = "50" style = "'.$thstyle.'">N п/п</td>
	         <td width = "200" style = "'.$thstyle.'">Название</td>
	         <td width = "150" style = "'.$thstyle.'">Тип Старта</td>
	         <td width = "150" style = "'.$thstyle.'">Старт с</td>
	         <td width = "150" style = "'.$thstyle.'">по</td>
	         <td width = "150" style = "'.$thstyle.'">Финиш с</td>
	         <td width = "150" style = "'.$thstyle.'">по</td>
	         <td width = "150" style = "'.$thstyle.'">Точки</td>
	         <td width = "150" style = "'.$thstyle.'">Штрафы (минуты)</td>
	         <td width = "150" style = "'.$thstyle.'">КП в амнистии</td>
	         <td width = "100" style = "'.$thstyle.'">Амнистия</td>'."\r\n");

			
	print('</tr>'."\r\n");
		
	// Сканируем команды
	while ($Row = mysqli_fetch_assoc($Result))
	{
	 	//   print('<tr class = "'.$TrClass.'">'."\r\n");
             print('<tr>'."\r\n");
	     print('<td align = "left" style = "'.$tdstyle.'">'.$Row['level_order'].'</td>
	             <td align = "left" style = "'.$tdstyle.'">'.$Row['level_name'].'</td>
	             <td align = "left" style = "'.$tdstyle.'">'.$Row['level_starttype'].'</td>
	             <td align = "left" style = "'.$tdstyle.'">'.$Row['level_begtime'].'</td>
	             <td align = "left" style = "'.$tdstyle.'">'.$Row['level_maxbegtime'].'</td>
	             <td align = "left" style = "'.$tdstyle.'">'.$Row['level_minendtime'].'</td>
	             <td align = "left" style = "'.$tdstyle.'">'.$Row['level_endtime'].'</td>
	             <td align = "left" style = "'.$tdstyle.'">'.$Row['level_pointnames'].'</td>
	             <td align = "left" style = "'.$tdstyle.'">'.$Row['level_pointpenalties'].'</td>
	             <td align = "left" style = "'.$tdstyle.'">'.$Row['level_discountpoints'].'</td>
	             <td align = "left" style = "'.$tdstyle.'">'.$Row['level_discount'].'</td>');

  	                               
	}	

	mysqli_free_result($Result);
	print('</table>'."\r\n");

   } 
   // Конец проверки на право правки
*/
?>

