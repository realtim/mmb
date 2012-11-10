<?php
// +++++++++++ Показ/редактирование данных пользователя +++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

         if ($viewmode == 'Add')
	 {
             // Новый пользователь 
             $pUserId = 0;
	     // Пока не делал возможности регистрировать пользователя уже авторизованному пользователю

	     // Если вернулись после ошибки переменные не нужно инициализировать
	     if ($viewsubmode == "ReturnAfterError") 
	     {

              ReverseClearArrays();

	      $UserEmail = $_POST['UserEmail'];
	      $UserName = str_replace( '"', '&quot;', $_POST['UserName']);
	      $UserBirthYear = (int)$_POST['UserBirthYear'];
	      if (!isset($_POST['UserProhibitAdd'])) $_POST['UserProhibitAdd'] = "";
	      $UserProhibitAdd = ($_POST['UserProhibitAdd'] == 'on' ? 1 : 0);

             } else {

	      $UserEmail = 'E-mail';
	      $UserName = 'Фамилия Имя';
	      $UserBirthYear = 'Год рождения';
	      $UserProhibitAdd = 0;

             }
            
            
             // Всегда разрешаем ввод нового пользователя 
	     $AllowEdit = 1;
	     // Определяем следующее действие
	     $NextActionName = 'AddUser';
             // Действие на текстовом поле по клику
	     $SaveButtonText = 'Зарегистрировать';
             // Кнопка "Сделать модератором" не выводится при добавлении пользователя
             $ModeratorButtonText = '';

         } else {

           // просмотр существующего
                //echo $viewsubmode;

		$pUserId = $_POST['UserId'];

		if ($pUserId <= 0)
		{
		// должны быть определены пользоатель, которого смотрят
		     return;
		}
           
		$sql = "select user_email, user_name, user_birthyear, user_prohibitadd from  Users where user_id = ".$pUserId;
		$rs = MySqlQuery($sql);  
                $row = mysql_fetch_assoc($rs);
                mysql_free_result($rs);

	        // Если вернулись после ошибки переменные не нужно инициализировать
	        if ($viewsubmode == "ReturnAfterError") 
		{

                  ReverseClearArrays();

		  $UserEmail = $_POST['UserEmail'];
		  $UserName = str_replace( '"', '&quot;', $_POST['UserName']);
		  $UserBirthYear = (int)$_POST['UserBirthYear'];
		  $UserProhibitAdd = ($_POST['UserProhibitAdd'] == 'on' ? 1 : 0);

                } else {

		  $UserEmail = $row['user_email'];  
		  $UserName = str_replace( '"', '&quot;', $row['user_name']); 
		  $UserBirthYear = (int)$row['user_birthyear'];  
		  $UserProhibitAdd = $row['user_prohibitadd'];  

                }

	        $NextActionName = 'UserChangeData';
		$AllowEdit = 0;
		$SaveButtonText = 'Сохранить изменения';
		$ModeratorButtonText = 'Сделать модератором';
		

                if (($pUserId == $UserId) || $Administrator)
		{
		  $AllowEdit = 1;
		}

	 }
         // Конец проверки действия с пользователем

	 
         if ($AllowEdit == 0) 
	 {
	    $OnSubmitFunction = 'return false;';
	    $DisabledText = 'disabled';
	 } else {
	    $OnSubmitFunction = 'return ValidateUserDataForm();';
	    $DisabledText = '';
	 }



// Выводим javascrpit
?>

<!-- Выводим javascrpit -->
<script language = "JavaScript">

        // Функция проверки правильности заполнения формы
	function ValidateUserDataForm()
	{ 
		if (document.UserDataForm.UserName.value == '') 
		{
			alert('Не указано имя.');           
			return false;
		} 

		if (document.UserDataForm.UserEmail.value == '') 
		{
			alert('Не указан e-mail.');           
			return false;
		} 


		if (document.UserDataForm.UserBirthYear.value == '') 
		{
			alert('Не указан год.');           
			return false;
		} 
		
		if (!CheckEmail(document.UserDataForm.UserEmail.value)) 
		{
			alert('E-mail не проходит проверку формата.');           
			return false;
		} 
		
		document.UserDataForm.action.value = "<? echo $NextActionName; ?>";
		return true;
	}
        // Конец проверки правильности заполнения формы

	
	// Функция отправки пароля
	function NewPassword()
	{ 
		document.UserDataForm.action.value = "SendEmailWithNewPassword";
		document.UserDataForm.submit();
	}
	// 

        // Функция отмены изменения
	function Cancel()
	{ 
		document.UserDataForm.action.value = "CancelChangeUserData";
		document.UserDataForm.submit();
	}
	// 

	// Функция просмотра данных о команде
	function ViewTeamInfo(teamid, raidid)
	{ 
		document.UserTeamsForm.TeamId.value = teamid;
		document.UserTeamsForm.RaidId.value = raidid;
		document.UserTeamsForm.action.value = "TeamInfo";
		document.UserTeamsForm.submit();
	}


	// Функция создания модератора
	function MakeModerator()
	{ 
		document.UserDataForm.action.value = "MakeModerator";
		document.UserDataForm.submit();
	}
	// 

/*	

        Вынес эти функции в меню
	// Функция проверки e-mail
	function CheckEmail(email) 
	{
		var template = /^[A-Za-z0-9](([_\.\-]?[a-zA-Z0-9]+)*)@([A-Za-z0-9]+)(([\.\-]?[a-zA-Z0-9]+)*)\.([A-Za-z])+$/;
//		email = drop_spaces(email); //функцию drop_spaces() см. выше
		if (template.test(email)) 
		{
		        return true;
		}
		return false; 
	}


	function trimLeft(str) {
	  return str.replace(/^\s+/, '');
	}

	function trimRight(str) {
	  return str.replace(/\s+$/, '');
	}

	function trimBoth(str) {
	  return trimRight(trimLeft(str));
	}

	function trimSpaces(str) {
	  return str.replace(/\s{2,}/g, ' ');
	}
*/	
</script>
<!-- Конец вывода javascrpit -->

<?php

         // выводим форму с данными пользователя
	 
	 print('<form  name = "UserDataForm"  action = "'.$MyPHPScript.'" method = "post" onSubmit = "'.$OnSubmitFunction.'">'."\r\n");
         print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\r\n");
         print('<input type = "hidden" name = "UserId" value = "'.$pUserId.'">'."\r\n");
         print('<input type = "hidden" name = "action" value = "">'."\r\n");

	 if ($AllowEdit == 1) 
	 {
          print('<div style = "margin-top: 10px; margin-bottom: 10px; font-size: 80%; text-align: left">Всплывающие подсказки появляются при наведении курсора мыши:</div>'."\r\n");
	 } 

         print('<table  class = "menu" border = "0" cellpadding = "0" cellspacing = "0" width = "300">'."\r\n");

         // Если не разрешена правка - не показываем адрес почты
         if ($AllowEdit == 1) 
	 {
	  print('<tr><td class = "input"><input type="text" name="UserEmail" size="50" value="'.$UserEmail.'" tabindex = "1"  '.$DisabledText.'
                 '.($viewmode <> 'Add' ? '' : 'onclick = "javascript: if (trimBoth(this.value) == \''.$UserEmail.'\') {this.value=\'\';}"').'
                 '.($viewmode <> 'Add' ? '' : 'onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$UserEmail.'\';}"').'
	         title = "E-mail - Используется для идентификации пользователя"></td></tr>'."\r\n");
         }

         print('<tr><td class = "input"><input type="text" name="UserName" size="50" value="'.$UserName.'" tabindex = "2"   '.$DisabledText.'
                 '.($viewmode <> 'Add' ? '' : 'onclick = "javascript: if (trimBoth(this.value) == \''.$UserName.'\') {this.value=\'\';}"').'
                 '.($viewmode <> 'Add' ? '' : 'onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$UserName.'\';}"').'
                title = "ФИО - Так будет выглядеть информация о пользователе в протоколах и на сайте"></td></tr>'."\r\n");

         print('<tr><td class = "input"><input type="text" name="UserBirthYear" maxlength = "4" size="11" value="'.$UserBirthYear.'" tabindex = "3" '.$DisabledText.'
                 '.($viewmode <> 'Add' ? '' : 'onclick = "javascript: if (trimBoth(this.value) == \''.$UserBirthYear.'\') {this.value=\'\';}"').'
                 '.($viewmode <> 'Add' ? '' : 'onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$UserBirthYear.'\';}"').'
	        title = "Год рождения"></td></tr>'."\r\n");

         print('<tr><td class = "input"><input type="checkbox" name="UserProhibitAdd" '.(($UserProhibitAdd == 1) ? 'checked="checked"' : '').' tabindex = "4" '.$DisabledText.'
	        title = "Даже зная адрес e-mail, другой пользователь не сможет сделать Вас участником своей команды - только Вы сами или модератор ММБ" /> Нельзя включать в команду другим пользователям</td></tr>'."\r\n");

	 if (($AllowEdit == 1) and  ($viewmode <> 'Add'))
        {

	  print('<tr><td class = "input" style =  "padding-top: 15px;">Новый пароль: <input type="password" name="UserNewPassword" size="30" value="" tabindex = "5"   '.$DisabledText.'
                  title = "Новый пароль"></td></tr>'."\r\n");

	  print('<tr><td class = "input">Подтверждение: <input type="password" name="UserConfirmNewPassword" size="30" value="" tabindex = "6"   '.$DisabledText.'
                  title = "Подтверждение нового пароля"></td></tr>'."\r\n");

        }


         // Если не разрешена права - не показываем кнопки
	 if ($AllowEdit == 1) 
	 {
	  print('<tr><td class = "input"  style =  "padding-top: 10px;">'."\r\n");
	  print('<input type="button" onClick = "javascript: if (ValidateUserDataForm()) submit();"  name="RegisterButton" value="'.$SaveButtonText.'" tabindex = "7">'."\r\n");

           // Если регистрация нового пользователя - не нужны кнопки "Отмена" и "Сменить пароль"
          if ($viewmode <> 'Add')
	  {
            print('<input type="button" onClick = "javascript: Cancel();"  name="CancelButton" value="Отмена" tabindex = "8" title = "Заново считывает данные из базы">'."\r\n");		

	    // 15,01,2012 убрал проверку
	    // М.б. проверка лишняя и нужно разрешить и администратору высылать запрос о смене пароля
	    //if ($UserId > 0 and $UserId == $NowUserId)
	    //{
             print('<input type="button" onClick = "javascript: if (confirm(\'Вы уверены, что хотите выслать на адрес '.trim($UserEmail).' новый пароль для '.trim($UserName).' будет создан новый пароль и ? \')) { NewPassword(); }"  name="ChangePasswordButton" value="Создать и выслать новый пароль" tabindex = "9">'."\r\n");		
	    //}
          }

          print('</td></tr>'."\r\n"); 
         }
         // Конец вывода кнопок

         // для Администратора добавляем кнопку "Сделать модератором" в правке пользователя
	 if ($Administrator and $viewmode <> 'Add') 
	 {
	  print('<tr><td class = "input"  style =  "padding-top: 10px;">'."\r\n");
	  	  print('<input type="button" onClick = "javascript: if (confirm(\'Вы уверены, что хотите сделать этого пользователя модератором текущего марш-броска? \')) { MakeModerator(); }"  name="ModeratorButton" value="'.$ModeratorButtonText.'" tabindex = "10">'."\r\n");
          print('</td></tr>'."\r\n"); 
         }

	 print('</table></form>'."\r\n"); 
	 // Конец вывода формы с данными пользователя

	 

          // Выводим спсиок команд, в которых участвовал данный пользователь 
          print('<div style = "margin-top: 20px; margin-bottom: 10px; text-align: left">Участвовал в командах:</div>'."\r\n");
          print('<form  name = "UserTeamsForm"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
          print('<input type = "hidden" name = "action" value = "">'."\r\n");
	  print('<input type = "hidden" name = "RaidId" value = "0">'."\n");
	  print('<input type = "hidden" name = "TeamId" value = "0">'."\n");
 	  print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\n");
	  
		
                 
		$sql = "select tu.teamuser_id, t.team_name, t.team_id, d.distance_name, r.raid_name, t.team_num, r.raid_id 
		        from  TeamUsers tu
			     inner join  Teams t
			     on tu.team_id = t.team_id
			     inner join  Distances d
			     on t.distance_id = d.distance_id
			     inner join  Raids r
			     on d.raid_id = r.raid_id
			where tu.teamuser_hide = 0 and user_id = ".$pUserId."
			order by r.raid_id desc "; 
                //echo 'sql '.$sql;
		$Result = MySqlQuery($sql);

		while ($Row = mysql_fetch_assoc($Result))
		{
		  print('<div align = "left" style = "padding-top: 5px;"><a href = "javascript:ViewTeamInfo('.$Row['team_id'].','.$Row['raid_id'].');"  title = "Переход к карточке команды">'.$Row['team_name'].'</a> 
		         (N '.$Row['team_num'].', дистанция: '.$Row['distance_name'].', ммб: '.$Row['raid_name'].')</div>'."\r\n");
		}

                mysql_free_result($Result);
	        print('</form>'."\r\n");


?>



