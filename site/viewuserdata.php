<?php
// +++++++++++ Показ/редактирование данных пользователя +++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

       // 03/04/2014  Добавил значения по умолчанию, чтобы подсказки в полях были не только при добавлении, 
        //но и при правке, если не былди заполнены поля при добавлении
	 $UserCityPlaceHolder = 'Город';
       


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
	      $UserCity = str_replace( '"', '&quot;', $_POST['UserCity']);

             } else {

	      $UserEmail = 'E-mail';
	      $UserName = 'Фамилия Имя';
	      $UserBirthYear = 'Год рождения';
	      $UserProhibitAdd = 0;
	      $UserCity =  $UserCityPlaceHolder;

             }
            
            
             // Всегда разрешаем ввод нового пользователя 
	     $AllowEdit = 1;
	     // Определяем следующее действие
	     $NextActionName = 'AddUser';
             // Действие на текстовом поле по клику
	     $SaveButtonText = 'Зарегистрировать';
             // Кнопка "Сделать модератором" не выводится при добавлении пользователя
             $ModeratorButtonText = '';
             // Кнопка "Объединить" не выводится при добавлении пользователя
	     $UnionButtonText = '';

         } else {

           // просмотр существующего
                //echo $viewsubmode;
		//

		$pUserId = $_REQUEST['UserId'];

		if ($pUserId <= 0)
		{
		// должны быть определены пользоатель, которого смотрят
		     return;
		}
           
		$sql = "select user_email, user_name, user_birthyear, user_prohibitadd, user_city from  Users where user_id = ".$pUserId;
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
		  $UserCity = str_replace( '"', '&quot;', $_POST['UserCity']);

                } else {

		  $UserEmail = $row['user_email'];  
		  $UserName = str_replace( '"', '&quot;', $row['user_name']); 
		  $UserBirthYear = (int)$row['user_birthyear'];  
		  $UserProhibitAdd = $row['user_prohibitadd'];  
		  $UserCity = str_replace( '"', '&quot;', $row['user_city']); 

                }

	        $NextActionName = 'UserChangeData';
		$AllowEdit = 0;
		$SaveButtonText = 'Сохранить изменения';
		$ModeratorButtonText = 'Сделать модератором';
		$UnionButtonText = 'Объединить';
		

                if (($pUserId == $UserId) || $Administrator)
		{
		  $AllowEdit = 1;
		}

	 }
         // Конец проверки действия с пользователем

         // Заменяем пустое значение на подсказку
	 if (empty($UserCity)) {$UserCity =  $UserCityPlaceHolder; } 

	 
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

	// Функция создания запроса на объединение
	function MakeUnionRequest()
	{ 
		document.UserDataForm.action.value = "AddUserInUnion";
		document.UserDataForm.submit();
	}
	// 

	// Удалить пользователя
	function HideUser()
	{
               if  (!document.UserDataForm.UserHideConfirm.checked)
	       {
	         document.UserDataForm.UserHideConfirm.disabled = false;
	   	 retrun;
	       }  
	 
		document.UserDataForm.action.value = 'HideUser';
		document.UserDataForm.submit();
	}


	// Функция получения конфигурации
	function GetDeviceId(deviceid)
	{ 
		document.UserDevicesForm.DeviceId.value = deviceid;
		document.UserDevicesForm.action.value = "GetDeviceId";
		document.UserDevicesForm.submit();
	}

	// Функция добавления устройства
	function AddDevice()
	{ 
		document.UserDevicesForm.action.value = "AddDevice";
		document.UserDevicesForm.submit();
	}

	// Функция отправки сообщения
	function SendMessage()
	{ 
		document.UserSendMessageForm.action.value = "SendMessage";
		document.UserSendMessageForm.submit();
	}

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
	 $TabIndex = 0;
	 
         // Если не разрешена правка - не показываем адрес почты
         if ($AllowEdit == 1) 
	 {
	  print('<tr><td class = "input"><input type="text" autocomplete = "off" name="UserEmail" size="50" value="'.$UserEmail.'" tabindex = "'.(++$TabIndex).'"  '.$DisabledText.'
                 '.($viewmode <> 'Add' ? '' : 'onclick = "javascript: if (trimBoth(this.value) == \''.$UserEmail.'\') {this.value=\'\';}"').'
                 '.($viewmode <> 'Add' ? '' : 'onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$UserEmail.'\';}"').'
	         title = "E-mail - Используется для идентификации пользователя"></td></tr>'."\r\n");
         }

         print('<tr><td class = "input"><input type="text" autocomplete = "off" name="UserName" size="50" value="'.$UserName.'" tabindex = "'.(++$TabIndex).'"   '.$DisabledText.'
                 '.($viewmode <> 'Add' ? '' : 'onclick = "javascript: if (trimBoth(this.value) == \''.$UserName.'\') {this.value=\'\';}"').'
                 '.($viewmode <> 'Add' ? '' : 'onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$UserName.'\';}"').'
                title = "ФИО - Так будет выглядеть информация о пользователе в протоколах и на сайте"></td></tr>'."\r\n");

         print('<tr><td class = "input"><input type="text" autocomplete = "off" name="UserBirthYear" maxlength = "4" size="11" value="'.$UserBirthYear.'" tabindex = "'.(++$TabIndex).'" '.$DisabledText.'
                 '.($viewmode <> 'Add' ? '' : 'onclick = "javascript: if (trimBoth(this.value) == \''.$UserBirthYear.'\') {this.value=\'\';}"').'
                 '.($viewmode <> 'Add' ? '' : 'onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$UserBirthYear.'\';}"').'
	        title = "Год рождения"></td></tr>'."\r\n");

         // Пустой $UserCity  выше  заменяется на подсказку
         print('<tr><td class = "input"><input type="text" autocomplete = "off" name="UserCity" size="50" value="'.$UserCity.'" tabindex = "'.(++$TabIndex).'"   '.$DisabledText.'
                 '.($UserCity <> $UserCityPlaceHolder ? '' : 'onclick = "javascript: if (trimBoth(this.value) == \''.$UserCityPlaceHolder.'\') {this.value=\'\';}"').'
                 '.($UserCity <> $UserCityPlaceHolder ? '' : 'onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$UserCityPlaceHolder.'\';}"').'
                title = "Город"></td></tr>'."\r\n");


//                 '.( $UserCity <> $UserCityPlaceHolder ? '' : 'onclick = "javascript: this.value=\'\';" onblur = "javascript: this.value=\''.$UserCityPlaceHolder.'\';"').'

         print('<tr><td class = "input"><input type="checkbox"  autocomplete = "off" name="UserProhibitAdd" '.(($UserProhibitAdd == 1) ? 'checked="checked"' : '').' tabindex = "'.(++$TabIndex).'" '.$DisabledText.'
	        title = "Даже зная адрес e-mail, другой пользователь не сможет сделать Вас участником своей команды - только Вы сами или модератор ММБ" /> Нельзя включать в команду другим пользователям</td></tr>'."\r\n");

	 if (($AllowEdit == 1) and  ($viewmode <> 'Add'))
        {

	  print('<tr><td class = "input" style =  "padding-top: 15px;">Новый пароль: <input type="password" autocomplete = "off" name="UserNewPassword" size="30" value="" tabindex = "'.(++$TabIndex).'"   '.$DisabledText.'
                  title = "Новый пароль"></td></tr>'."\r\n");

	  print('<tr><td class = "input">Подтверждение: <input type="password" autocomplete = "off" name="UserConfirmNewPassword" size="30" value="" tabindex = "'.(++$TabIndex).'"   '.$DisabledText.'
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

         $ModeratorUnionString = '';

         // для Администратора добавляем кнопку "Сделать модератором" в правке пользователя
	 if ($Administrator and $viewmode <> 'Add') 
	 {
	   $ModeratorUnionString .= '<input type="button" onClick = "javascript: if (confirm(\'Вы уверены, что хотите сделать этого пользователя модератором текущего марш-броска? \')) { MakeModerator(); }"  name="ModeratorButton" value="'.$ModeratorButtonText.'" tabindex = "'.(++$TabIndex).'">';
         }
	 

	 if (CanUnionRequest($Administrator, $UserId, $pUserId)) {

	   $ModeratorUnionString .= '<input type="button" onClick = "javascript: if (confirm(\'Вы уверены, что хотите оставить запрос на объединение с этим пользователем? \')) { MakeUnionRequest(); }"  name="UnionButton" value="'.$UnionButtonText.'" tabindex = "'.(++$TabIndex).'">';
	 
	 }

	 
	 if (trim($ModeratorUnionString) <> '') {
	    print('<tr><td class = "input"  style =  "padding-top: 10px;">'.$ModeratorUnionString.'</td></tr>'."\r\n");
	 }
	 // Конец проверки, что есть кнопка сделать модератором или объединить

         print('</tr>'."\r\n"); 

	 if ($AllowEdit == 1) 
	 {
	  print('<tr><td class = "input"  style =  "padding-top: 10px;">Ключ пользователя: '.$pUserId.'</td></tr>'."\r\n");
	 }
         
	 
	 // ============ Кнопка удаления всей команды для тех, кто имеет право
 	 if ($Administrator  and $viewmode <> 'Add') 
	 {
	  print('<tr><td class = "input"  style =  "padding-top: 10px;">'."\r\n");
	           print('<input type="button" style="padding-left: 30px;padding-right: 30px;margin-right:15px;" onClick="javascript: if (confirm(\'Вы уверены, что хотите удалить пользователя: '.trim($UserName).'? \')) {HideUser();}" name="HideUserButton" value="Удалить пользователя" tabindex="'.(++$TabIndex).'">'."\n");

           print(' Подтверждение удаления <input type="checkbox" name="UserHideConfirm" tabindex = "'.(++$TabIndex).'" disabled 
	        title = "Защита от случайного удаления" /> '."\r\n");

	  
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

	  if ($viewmode <> 'Add' and $AllowEdit == 1)
	  {
		// Выводим спсиок устройств, которые относятся к данному пользователю 
	        print('<div style = "margin-top: 20px; margin-bottom: 10px; text-align: left">Устройства, принадлежащие пользователю:</div>'."\r\n");
		print('<form  name = "UserDevicesForm"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
		print('<input type = "hidden" name = "action" value = "">'."\r\n");
		print('<input type = "hidden" name = "UserId" value = "'.$pUserId.'">'."\n");
		print('<input type = "hidden" name = "DeviceId" value = "0">'."\n");
		print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\n");
	  
		
                 
		$sql = "select d.device_id, d.device_name
		        from  Devices d
			where d.user_id = ".$pUserId."
			order by device_id desc "; 
                //echo 'sql '.$sql;
		$Result = MySqlQuery($sql);

		while ($Row = mysql_fetch_assoc($Result))
		{
		  print('<div align = "left" style = "padding-top: 5px;">'.$Row['device_name'].' <a href = "javascript:GetDeviceId('.$Row['device_id'].');" 
		          title = "Получить файл конфмгурации">Конфигурация</a></div>'."\r\n");
		}

                mysql_free_result($Result);

                $TabIndex = 1;
	        $DisabledText = '';
                $NewDeviceName = 'Название нового устройства';
		print('<div align = "left" style = "padding-top: 5px;"><input type="text" name="NewDeviceName" size="50" value="'.$NewDeviceName.'" tabindex = "'.(++$TabIndex).'"  '.$DisabledText.'
                onclick = "javascript: if (trimBoth(this.value) == \''.$NewDeviceName.'\') {this.value=\'\';}" 
                onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$NewDeviceName.'\';}"
	        title = "Название нового устройства">'."\r\n");
    	        print('<input type="button" onClick = "javascript: AddDevice();"  name="AddDeviceButton" value="Добавить" tabindex = "'.(++$TabIndex).'">'."\r\n");
                   
	        print('</div></form>'."\r\n");

	   }
	   // Конец проверки на режим правки
	   
	  
	  // 23/04/2014 Отправка сообщения через почту 
	  if ($viewmode <> 'Add' and !empty($UserId))
	  {
		// Выводим спсиок устройств, которые относятся к данному пользователю 
	        print('<div style = "margin-top: 20px; margin-bottom: 10px; text-align: left">Cообщение для пользователя '.$UserName.':</div>'."\r\n");
		print('<form  name = "UserSendMessageForm"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
		print('<input type = "hidden" name = "action" value = "">'."\r\n");
		print('<input type = "hidden" name = "UserId" value = "'.$pUserId.'">'."\n");
		print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\n");
	  

                $TabIndex = 1;
	        $DisabledText = '';
		print('<div align = "left" style = "padding-top: 5px;"><textarea name="MessageText"  rows="4" cols="50" tabindex = "'.(++$TabIndex).'"  '.$DisabledText.'
	        title = "Текст сообщения">Текст сообщения</textarea></div>'."\r\n");
    	        print('</br><input type="button" onClick = "javascript: SendMessage();"  name="SendMessageButton" value="Отправить" tabindex = "'.(++$TabIndex).'">'."\r\n");
                   
	        print('</form>'."\r\n");

	   }
	   // Конец проверки на режим правки и авторизованного пользоватлея

	   
	   
?>



