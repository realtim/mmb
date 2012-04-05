<?php

         // По идее, для всех действий должно передаваться SessionId через post
	// Исключение действия UserLogin и переход по ссылке из письма - там стартует сессия прям на этой странице
         // и передачи через форму не происходит
          // м.б. стоит яано прописать для каких action м.б.пустая сессия
         if (empty($SessionId))
	 {
		$SessionId =  $_POST['sessionid'];
	 } 

	 // Текущий пользователь
	 $NowUserId = GetSession($SessionId);


         if ($viewmode == 'Add')
	 {

		$RaidId = $_POST['RaidId'];
		if (empty($RaidId) or empty($NowUserId))
		{
	            $statustext = 'Для регистрации новой команды обязателен идентификатор пользователя и ММБ';
	  	    $alert = 1;
		    return;
		}

		$Sql = "select user_email from  Users where user_id = ".$NowUserId;
		$Result = MySqlQuery($Sql);  
		$Row = mysql_fetch_assoc($Result);
		mysql_free_result($Result);
		$UserEmail = $Row['user_email'];

                // Новая команда 
                $TeamId = 0;

		// Если вернулись после ошибки переменные не нужно инициализировать
		if ($viewsubmode == "ReturnAfterError") 
		{
                  ReverseClearArrays();


		  $TeamNum = (int) $_POST['TeamNum'];
		//  $TeamName = $_POST['TeamName'];
		  $TeamName = str_replace( '"', '&quot;', $_POST['TeamName']);
		  $DistanceId = $_POST['DistanceId'];
		  $TeamUseGPS = ($_POST['TeamUseGPS'] == 'on' ? 1 : 0);
		  $TeamMapsCount = (int)$_POST['TeamMapsCount'];
		  $TeamRegisterDt = 0;
		  $TeamConfirmResult = ($_POST['TeamConfirmResult'] == 'on' ? 1 : 0);
		  $ModeratorConfirmResult = ($_POST['ModeratorConfirmResult'] == 'on' ? 1 : 0);
		  $TeamGreenPeace = ($_POST['TeamGreenPeace'] == 'on' ? 1 : 0);
                  $TeamNotOnLevelId = $_POST['TeamNotOnLevelId'];

                } else {

		  $TeamNum = 'Номер';
		  $TeamName = 'Название команды';
		  $DistanceId = 0;
		  $TeamUseGPS = 0;
		  $TeamMapsCount = 0;
		  $TeamRegisterDt = 0;
		  $TeamConfirmResult = 0;
		  $ModeratorConfirmResult = 0;
		  $TeamGreenPeace = 0;
                  $TeamNotOnLevelId = 0;

		}

                $TeamUser = 0;

		// Всегда разрешаем ввод новой команды?
		//$AllowEdit = 1;
		// Определяем следующее действие
		$NextActionName = 'AddTeam';
		// Действие на текстовом поле по клику
		$OnClickText =  'onClick = "javascript:this.value = \'\';"';
		// Надпись на кнопке
		$SaveButtonText = 'Зарегистрировать';


         } else {

           // просмотр существующего
               // Проверка нужна только для случая регистрация новой команды
                 // только тогда Id есть в переменной php, но нет в вызывающей форме
		if (empty($TeamId))
		{
			$TeamId = $_POST['TeamId']; 
                }

		if ($TeamId <= 0)
		{
		// должны быть определена команда, которую смотрят
		     return;
		}

		$sql = "select t.team_num, t.distance_id, t.team_usegps, t.team_name, 
		               t.team_mapscount, d.raid_id, t.team_registerdt, 
			       t.team_confirmresult, t.team_moderatorconfirmresult,
                               t.team_greenpeace, t.level_id,
                               TIME_FORMAT(t.team_result, '%H:%i') as team_result,
			       CASE WHEN t.team_registerdt >= r.raid_registrationenddate
                                    THEN 1
                                    ELSE 0
                                END as team_late
		        from  Teams t
			      inner join  Distances d on t.distance_id = d.distance_id
			      inner join  Raids r on d.raid_id = r.raid_id
			where t.team_id = ".$TeamId; 
//		echo 'sql '.$sql;
                $Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
                mysql_free_result($Result);

                // Эти данные всегда берём из базы
		  $RaidId = $Row['raid_id'];
		  $TeamRegisterDt = $Row['team_registerdt'];
                  $TeamResult = $Row['team_result'];
                  $TeamLate = (int)$Row['team_late'];

		// Если вернулись после ошибки переменные не нужно инициализировать
		if ($viewsubmode == "ReturnAfterError") 
		{
                  ReverseClearArrays();

		  $TeamNum = (int) $_POST['TeamNum'];
//		  $TeamName = $_POST['TeamName'];
		  $TeamName = str_replace( '"', '&quot;', $_POST['TeamName']);
		  $DistanceId = $_POST['DistanceId'];
		  $TeamUseGPS = ($_POST['TeamUseGPS'] == 'on' ? 1 : 0);
		  $TeamMapsCount = (int)$_POST['TeamMapsCount'];
		  $TeamConfirmResult = ($_POST['TeamConfirmResult'] == 'on' ? 1 : 0);
		  $ModeratorConfirmResult = ($_POST['ModeratorConfirmResult'] == 'on' ? 1 : 0);
		  $TeamGreenPeace = ($_POST['TeamGreenPeace'] == 'on' ? 1 : 0);
                  $TeamNotOnLevelId = $_POST['TeamNotOnLevelId'];

                } else {

		  $TeamNum = $Row['team_num'];
		  $TeamName = str_replace( '"', '&quot;', $Row['team_name']);
		  $DistanceId = $Row['distance_id'];
		  $TeamUseGPS = $Row['team_usegps'];
		  $TeamMapsCount = (int)$Row['team_mapscount'];
		  $TeamConfirmResult = $Row['team_confirmresult'];
		  $ModeratorConfirmResult = $Row['team_moderatorconfirmresult'];
		  $TeamGreenPeace = $Row['team_greenpeace'];
                  $TeamNotOnLevelId = $Row['level_id'];
		}



		if (CheckTeamUser($SessionId, $TeamId))
		{
		  $TeamUser = 1;
		} else {
		  $TeamUser = 0;
		}


	        $NextActionName = 'TeamChangeData';
		$AllowEdit = 0;
		$OnClickText = '';
		$SaveButtonText = 'Сохранить данные команды';
		

	 }
         // Конец проверки действия с командой

         // Определяем статус пользователя
	 // К этому моменту, что для новой команды, что для существующей уже известен ммб


         // Заготовка под будущие проверки 
	 $sql = "select r.raid_resultpublicationdate, r.raid_registrationenddate, 
                        CASE WHEN r.raid_registrationenddate is not null and YEAR(r.raid_registrationenddate) <= 2011 
                             THEN 1 
                             ELSE 0 
                        END as oldmmb,
                        CASE WHEN r.raid_registrationenddate is not null and r.raid_registrationenddate <= NOW() 
                             THEN 1 
                             ELSE 0 
                        END as showresultfield
		 from   Raids r 
		 where r.raid_id = ".$RaidId; 
		//echo 'sql '.$sql;
                $Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
                mysql_free_result($Result);
		$RaidPublicationResultDate = $Row['raid_resultpublicationdate'];
                $RaidRegistrationEndDate = $Row['raid_registrationenddate'];
                $OldMmb = $Row['oldmmb'];
                $RaidShowResultField = $Row['showresultfield'];

                
	 if (empty($RaidRegistrationEndDate))
         {
	  // должна быть определена дата окончания регистрации
	     return;
         }
	 
	 if (CheckModerator($SessionId, $RaidId))
	 {
	   $Moderator = 1;
	 } else {
	   $Moderator = 0;
         }


        // Общее правило для возможности редактирования
	if ($viewmode == "Add" or $Moderator or ($TeamUser and !$TeamModeratorConfirmResult))
        {
          $AllowEdit = 1;
	  $DisabledText = '';
          $OnSubmitFunction = 'return ValidateTeamDataForm();';
        } else { 
	  $AllowEdit = 0;
	  $DisabledText = 'disabled';
          $OnSubmitFunction = 'return false;';
        }
 

// Выводим javascrpit
?>

<script language = "JavaScript">

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
	function ViewUserInfo(userid)
	{ 
	  document.TeamDataForm.UserId.value = userid;
	  document.TeamDataForm.action.value = 'UserInfo';
	  document.TeamDataForm.submit();
	}


        // Указать этап схода пользователя
	function TeamUserOut(teamuserid, levelid)
	{ 
          document.TeamDataForm.HideTeamUserId.value = teamuserid;
          document.TeamDataForm.UserOutLevelId.value = levelid;
	  document.TeamDataForm.action.value = 'TeamUserOut';
	  document.TeamDataForm.submit();
          
	}

</script>


<?

  print('<form  name = "TeamDataForm"  action = "'.$MyPHPScript.'" method = "post" onSubmit = "'.$OnSubmitFunction.'">'."\r\n");
  print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\r\n");
  print('<input type = "hidden" name = "action" value = "">'."\r\n");
  print('<input type = "hidden" name = "view" value = "'.(($viewmode == "Add") ? 'ViewRaidTeams' : 'ViewTeamData').'">'."\r\n");
  print('<input type = "hidden" name = "TeamId" value = "'.$TeamId.'">'."\r\n");
  print('<input type = "hidden" name = "RaidId" value = "'.$RaidId.'">'."\r\n");
  print('<input type = "hidden" name = "HideTeamUserId" value = "0">'."\r\n");
  print('<input type = "hidden" name = "UserOutLevelId" value = "0">'."\r\n");
  print('<input type = "hidden" name = "UserId" value = "0">'."\r\n");
  //print('<table  style = "font-size: 80%; padding-right: 20px; border-right-style: solid; border-right-width: 1px; border-right-color: #000000;" border = "0" cellpadding = "5" cellspacing = "0">'."\r\n");
 print('<table  style = "font-size: 80%;" border = "0" cellpadding = "2" cellspacing = "0">'."\r\n");



        $TabIndex = 0;
        print('<tr><td class = "input">'."\r\n");

         // Номер команды
	 if ($viewmode=="Add")
	 {
             // Добавляем новую команду
             // Если старый ММБ - открываем редактирование номера, иначе номер не передаём
             if ($OldMmb == 1)
             {
                print('Команда N <input type="text" name="TeamNum" size="10" 
                         value="0" tabindex = "'.(++$TabIndex).'" 
                         title = "Для прошлых ММБ укажите номер команды">'."\r\n");
              } else {
		print('<b>Новая команда!</b>
                            <input type="hidden" name="TeamNum" value="0">'."\r\n");
              } 

         } else {
              // Уже существующая команда
 	     print('Команда N <b>'.$TeamNum.'</b>
                            <input type="hidden" name="TeamNum" value="'.$TeamNum.'">'."\r\n");



	 }

         
    	      // дистанция 
	  print(' <span style = "margin-left: 30px;"> &nbsp; Дистанция</span>'."\r\n"); 
	  print('<select name="DistanceId"  class = "leftmargin" tabindex = "'.(++$TabIndex).'" '.$DisabledText.'>'."\r\n"); 

	  //echo 'RaidId '.$RaidId;

	  $sql = "select distance_id, distance_name from  Distances where raid_id = ".$RaidId; 
	  //echo 'sql '.$sql;
	  $Result = MySqlQuery($sql);

	  while ($Row = mysql_fetch_assoc($Result))
	  {
	    $distanceselected = ($Row['distance_id'] == $DistanceId ? 'selected' : '');
	    print('<option value = "'.$Row['distance_id'].'" '.$distanceselected.' >'.$Row['distance_name']."\r\n");
	  }
	  mysql_free_result($Result);
	  print('</select>'."\r\n");  

          

             // Проверяем права на правку чтобы показать кнопку удаления всей команды
             if ($viewmode<>"Add" and $AllowEdit == 1) 
             {
  	        print(' &nbsp; <input type = "button" style = "margin-left: 30px;" onClick = "javascript: if (confirm(\'Вы уверены, что хотите удалить команду: '.trim($TeamName).'? \')) {HideTeam();}" name="HideTeamButton" 
                                value = "Удалить команду"  tabindex = "'.(++$TabIndex).'">'."\r\n");
             }


        print('</td></tr>'."\r\n");

	 if ($viewmode<>"Add")
	 {
            if ($TeamLate == 1)
            {
                $RegisterDtFontColor = '#BB0000';
             } else {
                $RegisterDtFontColor = '#000000';
	     }

            print('<tr><td class = "input">Зарегистрирована: <span style = "color: '.$RegisterDtFontColor.';">'.$TeamRegisterDt.'</span></td></tr>'."\r\n");
         }  else {

             print('<tr><td class = "input">Время окончания регистрации: '.$RaidRegistrationEndDate.'</td></tr>'."\r\n");
        
          }     

        // Название команды
        print('<tr><td class = "input"><input type="text" name="TeamName" size="50" value="'.$TeamName.'" 
                                        tabindex = "'.(++$TabIndex).'" '.$DisabledText.' 
					'.($viewmode <> 'Add' ? '' : 'onclick = "javascript: if (trimBoth(this.value) == \''.$TeamName.'\') {this.value=\'\';}"').'
                                        '.($viewmode <> 'Add' ? '' : 'onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$TeamName.'\';}"').'
	                                title = "Название команды"></td></tr>'."\r\n");

        print('<tr><td class = "input">'."\r\n");

        // Использование GPS
        print('GPS <input type="checkbox" name="TeamUseGPS" '.(($TeamUseGPS == 1) ? 'checked="checked"' : '').'
                  tabindex = "'.(++$TabIndex).'" '.$DisabledText.'
  	           title = "Отметьте, если команда использует для ориентирования GPS"/> &nbsp; '."\r\n");


        // Число карт
        print('  &nbsp;  Число карт <input type="text" name="TeamMapsCount" size="5" value="'.$TeamMapsCount.'" 
                                        tabindex = "'.(++$TabIndex).'" '.$OnClickText.' '.$DisabledText.' 
                                        title = "">  &nbsp;  '."\r\n");


        // Нет сломанным унитазам!
        print('  &nbsp; Нет <a href = "http://community.livejournal.com/_mmb_/2010/09/24/">сломанным унитазам</a>! <input type="checkbox" name="TeamGreenPeace" '.(($TeamGreenPeace >= 1) ? 'checked="checked"' : '').'
                  tabindex = "'.(++$TabIndex).'" '.$DisabledText.'
  	           title = "Отметьте, если команда берёт повышенные экологические обязательства"/>'."\r\n");

        print('</td></tr>'."\r\n");

       
        // Участники 
	print('<tr><td class = "input">'."\r\n");
        //print('<div style = "margin-top: 20px; margin-bottom: 5px;">Участники:</div>'."\r\n");
                 
                 
		$sql = "select tu.teamuser_id, u.user_name, u.user_birthyear, tu.level_id, u.user_id 
		        from  TeamUsers tu
			     inner join  Users u
			     on tu.user_id = u.user_id
			where tu.teamuser_hide = 0 and team_id = ".$TeamId; 
                //echo 'sql '.$sql;
		$Result = MySqlQuery($sql);

		while ($Row = mysql_fetch_assoc($Result))
		{
		  //print('<div class= "input"><a href = "javascript:ViewUserInfo('.$Row['user_id'].');">'.$Row['user_name'].'</a> '.$Row['user_birthyear']."\r\n");
                 //    echo 'eee'. $Row['teamuser_id'].','.$Row['level_id'];

		  print('<div style = "margin-top: 5px;">'."\r\n");
                  // Ссылку удалить ставим только в том случае, если работает модератор или участник команды
                  if ($Moderator or $TeamUser) 
		  {
			  //print('<a style = "margin-left: 20px;" href = "javascript:if (confirm(\'Вы уверены, что хотите удалить участника: '.$Row['user_name'].' \')) {HideTeamUser('.c.');}">Удалить</a>'."\r\n");
			  print('<input type = "button" style = "margin-right: 15px;" 
                                  onClick = "javascript:if (confirm(\'Вы уверены, что хотите удалить участника: '.$Row['user_name'].'? \')) { HideTeamUser('.$Row['teamuser_id'].'); }" 
                                  name = "HideTeamUserButton" tabindex = "'.(++$TabIndex).'" value = "Удалить">'."\r\n");


		  }

			  // Если текущая дата больше времени окончания регистрации - появляются поля схода
			  if ($viewmode<>"Add" and $RaidShowResultField == 1)
			  {

			    // Список этапов, чтобы выбрать, на каком сошёл участник
			    print('Сход: <select name="UserOut'.$Row['teamuser_id'].'" style = "width: 100px; margin-right: 15px;"
                                    title = "Этап, на котором сошёл участник"
                                    onChange = "javascript:if (confirm(\'Вы уверены, что хотите отметить сход участника: '.$Row['user_name'].'? \')) { TeamUserOut('.$Row['teamuser_id'].', this.value); }" 
                                    tabindex = "'.(++$TabIndex).'" '.$DisabledText.'>'."\r\n"); 
                     

			    $sqllevels = "select level_id, level_name from  Levels where distance_id = ".$DistanceId." order by level_order"; 
			    //echo 'sql '.$sql;
			    $ResultLevels = MySqlQuery($sqllevels);

			    $userlevelselected =  ($Row['level_id'] == 0 ? 'selected' : '');
			    print('<option value = "0" '.$userlevelselected.' >-'."\r\n");

			    while ($RowLevels = mysql_fetch_assoc($ResultLevels))
			    {
			      $userlevelselected = ($RowLevels['level_id'] == $Row['level_id'] ? 'selected' : '');
			      print('<option value = "'.$RowLevels['level_id'].'" '.$userlevelselected.' >'.$RowLevels['level_name']."\r\n");
			    }
			    mysql_free_result($ResultLevels);
			    print('</select>'."\r\n");  
                         
                          }
                          // Конец проверки на текущее время

                 // Конец проверки на правку (дополнительные кнопки Удалить, Этапы
		  print('<a href = "javascript:ViewUserInfo('.$Row['user_id'].');">'.$Row['user_name'].'</a> '.$Row['user_birthyear']."\r\n");
		  print('</div>'."\r\n");
		}
                mysql_free_result($Result);

	print('</td></tr>'."\r\n");

        if ($AllowEdit == 1) 
        {
  	  print('<tr><td class = "input"  style =  "padding-top: 10px;">'."\r\n");

	  if ($viewmode == "Add" and  !$Moderator)
	  {
            // Новая команда и заводит не модератор
	     print($UserEmail.'<input type="hidden" name="NewTeamUserEmail" size="50" value="'.$UserEmail.'" >'."\r\n");
          } else {
	     print('<input type="text" name="NewTeamUserEmail" size="50" value="Email нового участника"
                      tabindex = "'.(++$TabIndex).'"
		      onclick = "javascript: if (trimBoth(this.value) == \'Email нового участника\') {this.value=\'\';}"
                      onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\'Email нового участника\';}"
                      title = "Укажите e-mail пользователя, которого Вы хотите добавить в команду. Пользователь может запретить добавлять себя в команду в настройках своей учетной записи.">'."\r\n");
          }

	  print('</td></tr>'."\r\n"); 

       // Проверка на отображение секции Результатов
       if ($viewmode<>"Add" and $RaidShowResultField == 1)
       {


          // Список этапов, чтобы выбрать, на какой команда не вышла (по умолчанию считается, что вышла на всё)
      	    print('<tr><td style = "padding-top: 15px;"><b>Результаты:</b></td></tr>'."\r\n");
            print('<tr><td class = "input">Не вышла на этап: &nbsp; '."\r\n");
	    print('<select name="TeamNotOnLevelId"  style = "width: 100px; margin-left: 10px;margin-right: 10px;" tabindex = "'.(++$TabIndex).'" '.$DisabledText.'
                     title = "Будьте аккуратны: изменение этого поля влияет на число отображаемых ниже этапов для ввода данных.">'."\r\n"); 
	    $sql = "select level_id, level_name from  Levels where distance_id = ".$DistanceId." order by level_order"; 
	    //echo 'sql '.$sql;
	    $Result = MySqlQuery($sql);

            $teamlevelselected =  ($TeamNotOnLevelId == 0 ? 'selected' : '');
	     print('<option value = "0" '.$teamlevelselected.' >-'."\r\n");

	    while ($Row = mysql_fetch_assoc($Result))
	    {
	      $teamlevelselected = ($Row['level_id'] == $TeamNotOnLevelId ? 'selected' : '');
	      print('<option value = "'.$Row['level_id'].'" '.$teamlevelselected.' >'.$Row['level_name']."\r\n");
	    }
	    mysql_free_result($Result);
	    print('</select>'."\r\n");  
            print(' &nbsp; Общее время: '.$TeamResult.'</td></tr>'."\r\n");



	    print('<tr><td class = "input"> Подтверждение:  &nbsp; '."\r\n");

	    // Подтверждение правильности результатов командой
	    print(' команды
             <input type="checkbox" name="TeamConfirmResult" '.(($TeamConfirmResult == 1) ? 'checked="checked"' : '').'
                  tabindex = "'.(++$TabIndex).'" '.$DisabledText.'
  	           title = "Заполняется после ввода результатов. Отметьте, если команда проверила все данные и согласна с ними"/>  &nbsp; '."\r\n");

	    if ($Moderator)
	    {
	      $ModeratorConfirmResultDisabledText = '';
	    } else {
	      $ModeratorConfirmResultDisabledText = 'disabled';
            }

	    // Подтверждение правильности результатов модератором
  	    print(' модератора
		<input type="checkbox" name="ModeratorConfirmResult" '.(($ModeratorConfirmResult == 1) ? 'checked="checked"' : '').'
		    tabindex = "'.(++$TabIndex).'" '.$ModeratorConfirmResultDisabledText.'
		    title = "Заполняется модератором после проверки результатов."/>'."\r\n");
	 

            print('</td></tr>'."\r\n");


       }
       // Конец проверки на отображение секции Результатов
       


          print('<tr><td class = "input"  style =  "padding-top: 20px;">'."\r\n");
	  print('<input type="button" onClick = "javascript: if (ValidateTeamDataForm()) submit();"
                   name="RegisterButton" value="'.$SaveButtonText.'" tabindex = "'.(++$TabIndex).'">'."\r\n");
          print('<select name="CaseView" onChange = "javascript:document.TeamDataForm.view.value = document.TeamDataForm.CaseView.value;"  
                    class = "leftmargin" tabindex = "'.(++$TabIndex).'">'."\r\n"); 
	  print('<option value = "ViewTeamData"  '.(($viewmode <> "Add") ? 'selected' : '').'>и остаться на этой странице'."\r\n"); 
	  print('<option value = "ViewRaidTeams"  '.(($viewmode == "Add") ? 'selected' : '').'>и перейти к списку команд'."\r\n"); 
	  print('</select>'."\r\n"); 
          print('<input type="button" onClick = "javascript: Cancel();"  name="CancelButton" value="Отмена"
                     tabindex = "'.(++$TabIndex).'">'."\r\n"); 

	

           print('</td></tr>'."\r\n"); 

        }

        print('</table></form>'."\r\n"); 

?>


