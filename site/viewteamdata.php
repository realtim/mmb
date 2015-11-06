<?php
// +++++++++++ Показ/редактирование данных команды ++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

if (!isset($viewmode)) $viewmode = "";
if (!isset($viewsubmode)) $viewsubmode = "";

// ================ Добавляем новую команду ===================================
if ($viewmode == 'Add')
{
	if (($RaidId <= 0) || ($UserId <= 0))
	{
		CMmb::setErrorMessage('Для регистрации новой команды обязателен идентификатор пользователя и ММБ');
		return;
	}

	// Если запрещено создавать команду - молча выходим, сообщение уже выведено в teamaction.php
	if (!CanCreateTeam($Administrator, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange)) return;


	$Sql = "select user_email from Users where user_id = $UserId";
	$UserEmail = CSql::singleValue($Sql, 'user_email');

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();
		$TeamNum = (int) $_POST['TeamNum'];
		$TeamName = CMmbUi::toHtml($_POST['TeamName']);
		$DistanceId = $_POST['DistanceId'];
		$TeamUseGPS = mmb_isOn($_POST, 'TeamUseGPS');
		$TeamMapsCount = (int)$_POST['TeamMapsCount'];
		$TeamRegisterDt = 0;
		$TeamGreenPeace = mmb_isOn($_POST, 'TeamGreenPeace');
	}
	else
	// Пробуем создать команду первый раз
	{
		$TeamNum = 'Номер';
		$TeamName = 'Название команды';
		$DistanceId = 0;
		$TeamUseGPS = 0;
		$TeamMapsCount = 0;
		$TeamRegisterDt = 0;
		$TeamGreenPeace = 0;
	}

	// Определяем следующее действие
	$NextActionName = 'AddTeam';
	// Действие на текстовом поле по клику
	$OnClickText = ' onClick="javascript:this.value = \'\';"';
	// Надпись на кнопке
	$SaveButtonText = 'Зарегистрировать';
        $UnionButtonText = 'Объединить';
}

else

// ================ Редактируем/смотрим существующую команду =================
{
	// Проверка нужна только для случая регистрация новой команды
	// Только тогда Id есть в переменной php, но нет в вызывающей форме
	if ($TeamId <= 0)
	{
		// Должна быть определена команда, которую смотрят
		return;
	}

	$sql = "select t.team_num, t.distance_id, t.team_usegps, t.team_name,
		t.team_mapscount, t.team_registerdt,
		t.team_greenpeace, 
		TIME_FORMAT(t.team_result, '%H:%i') as team_result,
		CASE WHEN DATE(t.team_registerdt) > r.raid_registrationenddate
			THEN 1
			ELSE 0
		END as team_late,
		d.distance_resultlink
		from Teams t
			inner join Distances d on t.distance_id = d.distance_id
			inner join Raids r on d.raid_id = r.raid_id
		where t.team_id = $TeamId";
	$Row = CSql::singleRow($sql);

	$TeamRegisterDt = $Row['team_registerdt'];
	$TeamResult = $Row['team_result'];
	$TeamLate = (int)$Row['team_late'];
	$DistanceResultLink = $Row['distance_resultlink'];

	// Если вернулись после ошибки переменные не нужно инициализировать
	if ($viewsubmode == "ReturnAfterError")
	{
		ReverseClearArrays();
		$TeamNum = (int) $_POST['TeamNum'];
		$TeamName = $_POST['TeamName'];
		$DistanceId = $_POST['DistanceId'];
		$TeamUseGPS = mmb_isOn($_POST, 'TeamUseGPS');
		$TeamMapsCount = (int)$_POST['TeamMapsCount'];
		$TeamGreenPeace = mmb_isOn($_POST, 'TeamGreenPeace');
	}
	else
	{
		$TeamNum = $Row['team_num'];
		$TeamName = $Row['team_name'];
		$DistanceId = $Row['distance_id'];
		$TeamUseGPS = $Row['team_usegps'];
		$TeamMapsCount = (int)$Row['team_mapscount'];
		$TeamGreenPeace = $Row['team_greenpeace'];
	}

	$TeamName = CMmbUi::toHtml($TeamName);

	$NextActionName = 'TeamChangeData';
	$AllowEdit = 0;
	$OnClickText = '';
	$SaveButtonText = 'Сохранить данные команды';
	$UnionButtonText = 'Объединить';
}
// ================ Конец инициализации переменных команды =================

// Определяем права по редактированию команды
if (($viewmode == "Add") || CanEditTeam($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange))
{
	$AllowEdit = 1;
	$DisabledText = '';
	$OnSubmitFunction = 'return ValidateTeamDataForm();';
}
else
{
	$AllowEdit = 0;
	$DisabledText = ' disabled';
	$OnSubmitFunction = 'return false;';
}
// Определяем права по просмотру результатов
if (($viewmode <> "Add") && CanViewResults($Administrator, $Moderator, $RaidStage))
	$AllowViewResults = 1;
else $AllowViewResults = 0;

// Выводим javascrpit
?>

<script language="JavaScript" type="text/javascript">
	// Функция проверки правильности заполнения формы
	function ValidateTeamDataForm()
	{
		document.TeamDataForm.action.value = "<? echo $NextActionName; ?>";
		return true;
	}
	// Конец проверки правильности заполнения формы

	// Удалить команду
	function HideTeam()
	{
		document.TeamDataForm.action.value = 'HideTeam';
		document.TeamDataForm.submit();
	}

	// Удалить пользователя
	function HideTeamUser(teamuserid)
	{
		document.TeamDataForm.HideTeamUserId.value = teamuserid;
		document.TeamDataForm.action.value = 'HideTeamUser';
		document.TeamDataForm.submit();
	}

	// Функция отмены изменения
	function Cancel()
	{
		document.TeamDataForm.action.value = "CancelChangeTeamData";
		document.TeamDataForm.submit();
	}

	// Посмотреть профиль пользователя
	/*function ViewUserInfo(userid)
	{
		document.TeamDataForm.UserId.value = userid;
		document.TeamDataForm.action.value = 'UserInfo';
		document.TeamDataForm.submit();
	}*/

         // 25.11.2013 Для совместимости оставил 
	// Указать этап схода пользователя
	function TeamUserOut(teamuserid, levelid)
	{
		document.TeamDataForm.HideTeamUserId.value = teamuserid;
		document.TeamDataForm.UserOutLevelId.value = levelid;
		document.TeamDataForm.action.value = 'TeamUserOut';
		document.TeamDataForm.submit();
	}
	

         // 25.11.2013 Новый вариант с точкой
	// Указать точку неявки участника
	function TeamUserNotInPoint(teamuserid, levelpointid)
	{
               // alert(levelpointid);
		document.TeamDataForm.HideTeamUserId.value = teamuserid;
		document.TeamDataForm.UserNotInLevelPointId.value = levelpointid;
		document.TeamDataForm.action.value = 'TeamUserNotInPoint';
		document.TeamDataForm.submit();
	}
	
	
	// Функция объединения команд
	function AddTeamInUnion()
	{ 
		document.TeamDataForm.action.value = "AddTeamInUnion";
		document.TeamDataForm.submit();
	}
	// 
</script>

<?php
// Выводим начало формы с командой
print('<form name="TeamDataForm" action="'.$MyPHPScript.'#'.$TeamNum.'" method="post" onSubmit="'.$OnSubmitFunction.'">'."\n");
print('<input type="hidden" name="action" value="">'."\n");
print('<input type="hidden" name="view" value="ViewRaidTeams">'."\n");
//print('<input type="hidden" name="view" value="'.(($viewmode == "Add") ? 'ViewRaidTeams' : 'ViewTeamData').'">'."\n");
print('<input type="hidden" name="TeamId" value="'.$TeamId.'">'."\n");
print('<input type="hidden" name="RaidId" value="'.$RaidId.'">'."\n");
print('<input type="hidden" name="HideTeamUserId" value="0">'."\n");
print('<input type="hidden" name="UserOutLevelId" value="0">'."\n");
print('<input type="hidden" name="UserNotInLevelPointId" value="0">'."\n");
print('<input type="hidden" name="UserId" value="0">'."\n\n");

print('<table style="font-size: 80%;" border="0" cellpadding="2" cellspacing="0">'."\n\n");
$TabIndex = 0;

print('<tr><td class="input">'."\n");

// ============ Номер команды
if ($viewmode=="Add")
// Добавляем новую команду
// Если старый ММБ - открываем редактирование номера, иначе номер не передаём
{
	if ($OldMmb == 1)
	{
		print('Команда N <input type="text" name="TeamNum" size="5" value="0" tabindex="'.(++$TabIndex).'" title="Для прошлых ММБ укажите номер команды">'."\n");
	}
	else
	{
		print('<b>Новая команда!</b> <input type="hidden" name="TeamNum" value="0">'."\n");
	}
}
else
// Уже существующая команда
{
	print('Команда N <b>'.$TeamNum.'</b> <input type="hidden" name="TeamNum" value="'.$TeamNum.'">'."\n");
}

// ============ Дистанция
if ($OldMmb == 1)
// Для старых ММБ выводим ссылки на старые статические результаты
{
	$sql = "select distance_resultlink, distance_name from Distances where raid_id = $RaidId";
	$Result = MySqlQuery($sql);
	while ($Row = mysql_fetch_assoc($Result))
	{
		print('&nbsp; Дистанция <a href="'.trim($Row['distance_resultlink']).'" target="_blank">' . $Row['distance_name'] . '</a> &nbsp; '."\n");
	}
	mysql_free_result($Result);
}
else
// Для новых ММБ перед выпадающим списком ничего не выводим
{
	print(' <span style="margin-left: 30px;"> &nbsp; Дистанция</span>'."\n");
}
// Показываем выпадающий список дистанций
print('<select name="DistanceId" class="leftmargin" tabindex="'.(++$TabIndex).'"'.$DisabledText.'>'."\n");
$sql = "select distance_id, distance_name from Distances where distance_hide = 0 and raid_id = $RaidId";
$Result = MySqlQuery($sql);
while ($Row = mysql_fetch_assoc($Result))
{
	$distanceselected = ($Row['distance_id'] == $DistanceId ? 'selected' : '');
	print('<option value="'.$Row['distance_id'].'" '.$distanceselected.' >'.$Row['distance_name']."</option>\n");
}
mysql_free_result($Result);
print('</select>'."\n");

// ============ Кнопка удаления всей команды для тех, кто имеет право
if (($viewmode <> "Add") && ($AllowEdit == 1))
{
	print('&nbsp; <input type="button" style="margin-left: 30px;" onClick="javascript: if (confirm(\'Вы уверены, что хотите удалить команду: '.trim($TeamName).'? \')) {HideTeam();}" name="HideTeamButton" value="Удалить команду" tabindex="'.(++$TabIndex).'">'."\n");
}

print('</td></tr>'."\n\n");

// ============ Дата регистрации команды
if ($viewmode <> "Add")
{
	$RegisterDtFontColor = ($TeamLate == 1) ? '#BB0000' : '#000000';
	print('<tr><td class="input">Зарегистрирована: <span style="color: '.$RegisterDtFontColor.';">'.$TeamRegisterDt.'</span></td></tr>'."\n\n");
}
else
{
	$sql = "select r.raid_registrationenddate from Raids r where r.raid_id = $RaidId";
	$RaidRegistrationEndDate = CSql::singleValue($sql, 'raid_registrationenddate');
	print('<tr><td class="input">Время окончания регистрации: '.$RaidRegistrationEndDate."</td></tr>\r\n");
}

// ============ Информация для тех, кто может править "ВНе зачета!" о лимитах

if ($viewmode <> "Add" and CanEditOutOfRange($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange)) 
{

      // Получаем информацию о лимите и о зарегистированных командах
		$sql = "select count(*) as teamscount, COALESCE(r.raid_teamslimit, 0) as teamslimit
			from Raids r 
				inner join Distances d
				on r.raid_id = d.raid_id
				inner join Teams t
				on d.distance_id = t.distance_id
			where r.raid_id=$RaidId
				and t.team_hide = 0
				and t.team_outofrange = 0
			";
		$Row = CSql::singleRow($sql);
        	// Если указан лимит и он уже достигнут или превышен и команда "в зачете". то нельзя создавать
		print("<tr><td class=\"input\">В зачете команд  {$Row['teamslimit']} лимит {$Row['teamslimit']}</td></tr>\r\n");

}

// ============ Название команды
print('<tr><td class="input"><input type="text" name="TeamName" size="50" value="'.$TeamName.'" tabindex="'.(++$TabIndex)
	.'"'.$DisabledText.($viewmode <> 'Add' ? '' : CMmbUI::placeholder($TeamName))
	.' title="Название команды"></td></tr>'."\n\n");

print('<tr><td class="input">'."\n");


// ============ Вне зачета
// Если регистрация команды, то атрибут "вне зачёта" не может изменпить  даже администратор - этот параметр рассчитывается по времени
// администратор может поменять флаг при правке
if ($viewmode <> "Add" and CanEditOutOfRange($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange)) 
{
     $DisabledTextOutOfRange = ''; 
} else {
     $DisabledTextOutOfRange = 'disabled'; 
}

	print('Вне зачета! <input type="checkbox" name="TeamOutOfRange" value="on"'.(($TeamOutOfRange == 1) ? ' checked="checked"' : '')
	.' tabindex="'.(++$TabIndex).'" '.$DisabledTextOutOfRange.'  title="Команда вне зачета"/> &nbsp;'."\n");

// ============ Использование GPS
print('GPS <input type="checkbox" name="TeamUseGPS" value="on"'.(($TeamUseGPS == 1) ? ' checked="checked"' : '')
	.' tabindex="'.(++$TabIndex).'"'.$DisabledText
	.' title="Отметьте, если команда использует для ориентирования GPS"/> &nbsp;'."\n");

// ============ Число карт
print('&nbsp; Число карт <input type="text" name="TeamMapsCount" size="2" maxlength="2" value="'.$TeamMapsCount.'" tabindex="'.(++$TabIndex).'"'
	.$OnClickText.$DisabledText.' title="Число заказанных на команду карт на каждый из этапов"> &nbsp;'."\n");

// ============ расчет стоимости
print('&nbsp; К оплате: <b>'.CalcualteTeamPayment($TeamId).'</b> руб. &nbsp;'."\n");

print('</td></tr>'."\n\n");

// ============ Нет сломанным унитазам!
print('<tr><td class="input">'."\n");

print('<a href="http://community.livejournal.com/_mmb_/2010/09/24/" target="_blank">Нет сломанным унитазам!</a> - прочитали и поддерживаем <input type="checkbox" name="TeamGreenPeace" value="on"'.(($TeamGreenPeace >= 1) ? ' checked="checked"' : '')
	.' tabindex="'.(++$TabIndex).'"'.$DisabledText.' title="Отметьте, если команда берёт повышенные экологические обязательства"/>'."\n");

print("</td></tr>\r\n");

// ============ Участники
print('<tr><td class="input">'."\n");


$sql = "select tu.teamuser_id, 
               tu.teamuser_notstartraidid, 
	       r.raid_nostartprice,
               CASE WHEN COALESCE(u.user_noshow, 0) = 1 THEN '$Anonimus' ELSE u.user_name END as user_name, u.user_birthyear, u.user_id, COALESCE(tld.levelpoint_id, 0) as levelpoint_id
	from TeamUsers tu
		inner join Users u
		on tu.user_id = u.user_id
                left outer join TeamLevelDismiss tld
		on tu.teamuser_id = tld.teamuser_id
                left outer join Raids r
		on tu.teamuser_notstartraidid = r.raid_id
	where tu.teamuser_hide = 0 and team_id = $TeamId";
$Result = MySqlQuery($sql);

while ($Row = mysql_fetch_assoc($Result))
{
	$userName = CMmbUI::toHtml($Row['user_name']);
	print('<div style="margin-top: 5px;">'."\n");
	if ($AllowEdit)
	{
		print('<input type="button" style="margin-right: 15px;" onClick="javascript:if (confirm(\'Вы уверены, что хотите удалить участника: '.$userName.'? \')) { HideTeamUser('.$Row['teamuser_id'].'); }" name="HideTeamUserButton" tabindex="'.(++$TabIndex).'" value="Удалить">'."\n");
	}

	// Показываем только если можно смотреть результаты марш-броска
	// (так как тут есть список этапов)
	if ($AllowViewResults)
	{
  
		print('Неявка в: <select name="UserNotInPoint'.$Row['teamuser_id'].'" style="width: 100px; margin-right: 15px;" title="Точка, в которую не явился участник" onChange="javascript:if (confirm(\'Вы уверены, что хотите отметить неявку участника: '.$userName.'? \')) { TeamUserNotInPoint('.$Row['teamuser_id'].', this.value); }" tabindex="'.(++$TabIndex).'"'.$DisabledText.'>'."\n");
		$sqllevelpoints = "select levelpoint_id, levelpoint_name from LevelPoints lp where lp.distance_id = $DistanceId order by levelpoint_order";
		$ResultLevelPoints = MySqlQuery($sqllevelpoints);
		$userlevelpointselected = ($Row['levelpoint_id'] == 0 ? ' selected' : '');
		print('<option value="0"'.$userlevelpointselected.'>-</option>'."\n");
		while ($RowLevelPoints = mysql_fetch_assoc($ResultLevelPoints))
		{
			$userlevelpointselected = ($RowLevelPoints['levelpoint_id'] == $Row['levelpoint_id'] ? 'selected' : '');
			print('<option value="'.$RowLevelPoints['levelpoint_id'].'"'.$userlevelpointselected.'>'.$RowLevelPoints['levelpoint_name']."</option>\n");
		}
		mysql_free_result($ResultLevelPoints);
		print('</select>'."\n");
	   
	}

	// ФИО и год рождения участника
	print("<a href=\"?UserId={$Row['user_id']}\">$userName</a> {$Row['user_birthyear']}\n");
 
        // Отметка невыходна на старт в предыдущем ММБ                          
        if ($Row['teamuser_notstartraidid'] > 0) {
	    print(' <a title = "Участник был заявлен, но не вышел на старт в прошлый раз" href = "#comment">(?!)</a> ');
        } 

	print("</div>\n");
}
mysql_free_result($Result);
print("</td></tr>\n");
// Закончили вывод списка участников

// ============ Новый участник
// Возможность добавлять участников заканчивается вместе с возсожностью создавать команды
if (($AllowEdit == 1) && CanCreateTeam($Administrator, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange))
{
	print('<tr><td class="input" style="padding-top: 10px;">'."\n");
	if (($viewmode == "Add") && !$Moderator)
	{
		// Новая команда и заводит не модератор
		print($UserEmail.'<input type="hidden" name="NewTeamUserEmail" size="50" value="'.$UserEmail.'" >'."\n");
	}
	else
	{
		print('<input type="text" name="NewTeamUserEmail" size="50" value="Email нового участника" tabindex="'.(++$TabIndex) .'"'
			. CMmbUI::placeholder('Email нового участника') . 'title="Укажите e-mail пользователя, которого Вы хотите добавить в команду. Пользователь может запретить добавлять себя в команду в настройках своей учетной записи.">'."\n");
	}
	print("</td></tr>\n");
}

// 20/02/2014 Пользовательское соглашение
if (($viewmode == "Add") && ($AllowEdit == 1) )
{


print('<tr><td class="input" style="padding-top: 20px;">'."\n");



      // 21.03.2014 Ищем ссылку на положение в загруженных файлах 
        $RulesFile = CSql::raidFileName($RaidId, 1, false);
        $RaidRulesLink = '';
        if ($RulesFile <> '' && file_exists($MyStoreFileLink.$RulesFile))
	{
          $RaidRulesLink = $MyStoreHttpLink.$RulesFile;
        }
        //  Конец получения ссылки на положение

       print('<b>Условия участия (выдержка из <a href="'.$RaidRulesLink.'" target="_blank">положения</a>): </b><br/>'."\n");
//print('<div style="padding-top: 10px;">&nbsp;</div>'."\n");

// Ищем последнее пользовательское соглашение
        $sql = "select rf.raidfile_id, rf.raidfile_name
		from RaidFiles rf
		where rf.raidfile_hide = 0 
		      and rf.filetype_id = 8
		order by rf.raid_id DESC, rf.raidfile_id DESC 
		LIMIT 0,1    ";

        $ConfirmFile = trim($MyStoreHttpLink).trim(CSql::singleValue($sql, 'raidfile_name'));
	
	$Fp = fopen($ConfirmFile, "r");
	$i = 0;
        while ((!feof($Fp)) && (!strpos(trim(fgets($Fp, 4096)),'body'))) 
	{
           $i++;
        }

        $NowStr = '';
        while ((!feof($Fp)) && (!strpos(trim($NowStr),'/body'))) 
	{
           print(trim($NowStr)."\r\n");
           $NowStr = fgets($Fp, 4096);
        }

        fclose($Fp);

	

print("</td></tr>\r\n");



print('<tr><td class="input">'."\n");
print("<a href=\"$RaidRulesLink\" target=\"_blank\">Полный текст положения</a>:<br/>\n");
print('Прочитал и согласен с условиями участия в ММБ <input type="checkbox" name="Confirmation" value="on" tabindex="'.(++$TabIndex).'"'.$DisabledText.' title="Прочитал и согласен с условиями участия в ММБ"/>'."\n");
print("</td></tr>\r\n");


}
// конец блока пользовательского соглашения


// ================ Submit для формы ==========================================
if ($AllowEdit == 1)
{
	print('<tr><td class="input" style="padding-top: 20px;">'."\n");
	print('<input type="button" onClick="javascript: if (ValidateTeamDataForm()) submit();" name="RegisterButton" value="'.$SaveButtonText.'" tabindex="'.(++$TabIndex).'">'."\n");
	print('<select name="CaseView" onChange="javascript:document.TeamDataForm.view.value = document.TeamDataForm.CaseView.value;" class="leftmargin" tabindex="'.(++$TabIndex).'">'."\n");
	print('<option value="ViewTeamData" >и остаться на этой странице</option>'."\n");
	print('<option value="ViewRaidTeams" selected>и перейти к списку команд</option>'."\n");
	print('</select>'."\n");
	print('<input type="button" onClick="javascript: Cancel();" name="CancelButton" value="Отмена" tabindex="'.(++$TabIndex).'">'."\n");
	print('</td></tr>'."\n\n");

	// для Администратора добавляем кнопку "Сделать модератором" в правке пользователя
	if ($Administrator and $viewmode <> 'Add' and $TeamOutOfRange == 0) 
	{
	  print('<tr><td class = "input"  style =  "padding-top: 10px;">'."\r\n");
	  	  print('<input type="button" onClick = "javascript: AddTeamInUnion();"  name="UnionButton" value="'.$UnionButtonText.'" tabindex = "'.(++$TabIndex).'">'."\r\n");
          print('</td></tr>'."\r\n"); 
        }


}

print("</table></form>\n");
?>
