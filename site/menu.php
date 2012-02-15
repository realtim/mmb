

<script language = "JavaScript">

        // Функция проверки правильности заполнения формы
	function ValidateUserLoginForm()
	{ 
		if (document.UserLoginForm.Login.value == '') 
		{
			alert('Не указан e-mail.');           
			return false;
		} 

		if (!CheckEmail(document.UserLoginForm.Login.value)) 
		{
			alert('E-mail не проходит проверку формата.');           
			return false;
		} 
		

		if (document.UserLoginForm.Password.value == '') 
		{
			alert('Не указан пароль.');           
			return false;
		} 



		return true;
	}
        // Конец проверки правильности заполнения формы

	
	function UserLogout()
	{ 
		document.UserLoginForm.action.value = "UserLogout";
		document.UserLoginForm.submit();
	}

	function ViewUserInfo(userid)
	{ 
		document.UserLoginForm.action.value = "UserInfo";
		document.UserLoginForm.UserId.value = userid;
		document.UserLoginForm.submit();
	}

        // Одинаковые действия при  регистрации поьзователя и обновленииданных.
	// Не уверен, что правильно так
	function NewUser()
	{ 
		document.UserLoginForm.action.value = "ViewNewUserForm";
		document.UserLoginForm.submit();
	}


	// Функция отправки письма
	function RestorePassword()
	{ 
		document.UserLoginForm.action.value = "RestorePasswordRequest";
		document.UserLoginForm.submit();
	}


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
	

</script>

<?php


	 $UserId = 0;

         if (empty($SessionId))
	 {
		$SessionId =  $_POST['sessionid'];
	 } 
	 
	 $UserId = GetSession($SessionId);
	 
//        echo "Пользователь: ".$UserId.", сессия: ".$SessionId;
        // какое выводить меню
	if ($UserId <= 0)
	{
		print('<form  name = "UserLoginForm"  action = "'.$MyPHPScript.'" method = "post" onSubmit = "return ValidateUserLoginForm();">'."\r\n");
                print('<input type = "hidden" name = "action" value = "UserLogin">'."\r\n"); 
		print('<input type = "hidden" name = "view" value = "'.$view.'">'."\r\n");
		print('<table  class = "menu" border = "0" cellpadding = "0" cellspacing = "0">'."\r\n");
		print('<tr><td class = "input"><input type="text" name="Login"
                       size="21" value="E-mail" tabindex = "101"
                       onclick = "javascript: if (trimBoth(this.value) == \'E-mail\') {this.value=\'\';}"
		       onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\'E-mail\';}"
                       ></td></tr>'."\r\n"); 
		print('<tr><td class = "input"><input type="password" name="Password"  style = "width:106px;" size="10" value="" tabindex = "102">
                            <input type="submit" name="RegisterButton" value="Вход" tabindex = "103"  style = "margin-left:5px; width:70px;"></td></tr>'."\r\n"); 
		print('<tr><td><a href = "javascript:RestorePassword();"  title = "Будет выслан запрос о сменен пароля на указанный выше e-mail">Забыли пароль?</a></td></tr>'."\r\n"); 
		print('<tr><td><a href = "javascript:NewUser();"  title = "Переход к форме регистрации нового пользователя">Новый пользователь</a></td></tr>'."\r\n"); 
		print('</table>'."\r\n");
		print('</form>'."\r\n");
	} else {

                $Result = MySqlQuery('select user_name from  Users where user_id = '.$UserId);
		$Row = mysql_fetch_assoc($Result);
		$UserName = $Row['user_name'];
                print('<form  name = "UserLoginForm"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
                print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\r\n"); 
                print('<input type = "hidden" name = "action" value = "">'."\r\n"); 
                print('<input type = "hidden" name = "UserId" value = "">'."\r\n"); 
		print('<input type = "hidden" name = "view" value = "'.$view.'">'."\r\n");
		print('<table  class = "menu" border = "0" cellpadding = "0" cellspacing = "0">'."\r\n");
		print('<tr><td><a href = "javascript:ViewUserInfo('.$UserId.');"  title = "Переход к Вашей карточке пользователя">'.$UserName.'</a></tr>'."\r\n"); 
		print('<tr><td><a href = "javascript:UserLogout();" style = "font-size: 80%;">Выход</a></td></tr>'."\r\n"); 
		print('</table>'."\r\n");
		print('</form>'."\n");
	}
		print('</br>'."\n");


?>


<script language = "JavaScript">


	        // Функция проверки правильности заполнения формы
	function ValidateFindTeamForm()
	{ 
		if (document.FindTeamForm.TeamNum.value == '') 
		{
			alert('Не указан номер команды.');           
			return false;
		} 

		if (document.FindTeamForm.RaidId.value <= 0) 
		{
			alert('Не выбран ММБ.');           
			return false;
		} 

		return true;
	}

	function NewTeam()
	{ 
		document.FindTeamForm.action.value = "RegisterNewTeam";
		document.FindTeamForm.submit();
	}


	function RaidTeams()
	{ 
		document.FindTeamForm.action.value = "ViewRaidTeams";
		document.FindTeamForm.submit();
	}


	function RecalcRaidResults()
	{ 
		document.AdminServiceForm.action.value = "RecalcRaidResults";
                document.AdminServiceForm.RaidId.value = document.FindTeamForm.RaidId.value; 
		document.AdminServiceForm.submit();
	}

        function ClearTables()
	{ 
		document.AdminServiceForm.action.value = "ClearTables";
		document.AdminServiceForm.submit();
	}


        function ShowEmail()
	{
         var begstr = "<? echo substr(trim($_SERVER['SERVER_NAME']), 0, 4); ?>";

	  begstr = begstr.replace("\.","\@");
	  begstr = begstr + "progressor.ru";
	  alert(begstr);
	}

</script>

<?
	
	// выводим окно для поиска команды 
	print('<form  name = "FindTeamForm"  action = "'.$MyPHPScript.'" method = "post" onSubmit = "return ValidateFindTeamForm();">'."\r\n");

	if ($UserId > 0)
	{
                print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\r\n"); 
	}                
	print('<input type = "hidden" name = "action" value = "FindTeam">'."\r\n"); 
	print('<input type = "hidden" name = "view" value = "'.$view.'">'."\r\n");
	print('<table  class = "menu" border = "0" cellpadding = "0" cellspacing = "0">'."\r\n");
	print('<tr><td class = "input">ММБ'."\r\n"); 
	print('<select name="RaidId"  style = "width:141px;" class = "leftmargin" tabindex = "201"  title = "Список марш-бросков">'."\r\n"); 

                $Result = MySqlQuery('select raid_id, raid_name from  Raids where raid_registrationenddate is not null order by 1 desc');

		while ($Row = mysql_fetch_assoc($Result))
		{
		  print('<option value = "'.$Row['raid_id'].'"  '.(($Row['raid_id'] == $_POST['RaidId']) ? 'selected' : '').' >'.$Row['raid_name']."\r\n");
		}

                mysql_free_result($Result);
	print('</select>'."\r\n");  
	print('</td></tr>'."\r\n");
	if ($UserId > 0)
	{
		print('<tr><td><a href = "javascript:NewTeam();" title = "Переход к форме регистрации новой команды на выбранный выше ММБ">Новая команда</a></td></tr>'."\r\n"); 
	}
	print('<tr><td><a href = "javascript:RaidTeams();" title = "Список команд по убыванию номеров для выбранного выше ММБ">Команды</a></td></tr>'."\r\n"); 
        //print('<tr><td class = "input">Поиск:</td></tr>'."\r\n"); 
	print('<tr><td style = "padding-top:15px;"><input  type="text" name="TeamNum" size="12" value="Номер команды" tabindex = "206"  title = "Карточка команды с указанным номером для выбранного выше ММБ"
                       onclick = "javascript: if (trimBoth(this.value) == \'Номер команды\') {this.value=\'\';}"
		       onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\'Номер команды\';}"
                 > 
	       <input type="submit"  name="FindButton" value="Найти"   class = "leftmargin" tabindex = "207"></td></tr>'."\r\n"); 
	print('</table>'."\r\n");
	print('</form>'."\r\n");
	// Поиск участника
	print('<form  name = "FindUserForm"  action = "'.$MyPHPScript.'" method = "post" onSubmit = "return ValidateFindUserForm();">'."\r\n");
	if ($UserId > 0)
	{
                print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\r\n"); 
	}                
	print('<input type = "hidden" name = "action" value = "FindUser">'."\r\n"); 
	print('<input type = "hidden" name = "view" value = "'.$view.'">'."\r\n");
	print('<table  class = "menu" border = "0" cellpadding = "0" cellspacing = "0">'."\r\n");
	//print('<tr><td class = "input">Поиск пользователя</td></tr>'."\r\n"); 
	print('<tr><td class = "input"><input  type="text" name="FindString" size="12" value="Часть ФИО" tabindex = "301" 
	       title = "Список пользователей по алфавиту, чьи ФИО содержат введённый текст. Для вывода всех наберите: все-все (можно и все-все-все)."
                       onclick = "javascript: if (trimBoth(this.value) == \'Часть ФИО\') {this.value=\'\';}"
		       onblur = "javascript: if (trimBoth(this.value) == \'\') {this.value=\'Часть ФИО\';}"
		>
	       <input type="submit"  name="FindButton" value="Найти"   class = "leftmargin" tabindex = "302"></td></tr>'."\r\n"); 
	print('</table>'."\r\n");
	print('</form>'."\r\n");
	print('</br>'."\r\n");

       // Форма сервисов администратора
        if (CheckAdmin($SessionId) == 1) 
	{
	  print('<form  name = "AdminServiceForm"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
	  print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\r\n"); 
	  print('<input type = "hidden" name = "action" value = "">'."\r\n"); 
	  print('<input type = "hidden" name = "view" value = "'.$view.'">'."\r\n");
	  print('<input type = "hidden" name = "RaidId" value = "0">'."\r\n");


          //  показываем кнопку "Пересчёт результатов" 
          print('<input type="button" style = "width:185px;" name="RecalcRaidResults" value="Пересчитать результаты" 
                          onclick = "javascript: RecalcRaidResults();"
                          tabindex = "205">'."\r\n"); 
	  print('</br>'."\r\n");

          //  показываем кнопку "Очистить таблицы" 
	  print('<input type="button" style = "width:185px; margin-top:10px;" name="ClearTables" value="Очистить таблицы" 
                          onclick = "javascript: ClearTables();"
                          tabindex = "205">'."\r\n"); 

	  print('</form>'."\r\n");
	  print('</br>'."\r\n");

	} 
		
	// Внешние сылки
	print('<table  class = "menu" border = "0" cellpadding = "0" cellspacing = "0">'."\r\n");
	print('<tr><td><a href="http://www.livejournal.com/community/_mmb_" target = "_blank">ЖЖ</a></td></tr>'."\r\n"); 
	print('<tr><td><a href="http://slazav.mccme.ru/maps/" target = "_blank">Карты</a></td></tr>'."\r\n"); 
	print('<tr><td><a href="http://mmb.progressor.ru/vp.html" target = "_blank">Впечатления</a></td></tr>'."\r\n"); 
	print('<tr><td><a href="http://mmb.progressor.ru/icons.html" target = "_blank">Значки</a></td></tr>'."\r\n"); 
	print('<tr><td> <a href="http://slazav.mccme.ru/mmb/ludir.htm" target = "_blank">Рейтинг</a></td></tr>'."\r\n"); 
	print('<tr><td><a href="http://slazav.mccme.ru/mmb/ludi.htm" target = "_blank">Участники</a></td></tr>'."\r\n"); 
	print('</table>'."\r\n");

	print('</br>'."\r\n");

        // Почта
	print('<table  class = "menu" border = "0" cellpadding = "0" cellspacing = "0">'."\r\n");
	print('<tr><td><a style = "font-family: Times New Roman, Serif; font-size: 100%;" href="javascript: ShowEmail();" title = "Адрес латинскими буквами, как в названии сайта">ммб@прогрессор.ру</a> <small>(en)</small></td></tr>'."\r\n");
        print('<tr><td><a href="https://github.com/realtim/mmb/wiki/%D0%A1%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D1%8F-%D0%BE-%D1%81%D0%B5%D1%80%D0%B2%D0%B8%D1%81%D0%B5-%D0%9C%D0%9C%D0%91"  target = "_blank">О сервисе</a>, 
                       <a href="https://github.com/realtim/mmb/wiki/%D0%90%D0%B2%D1%82%D0%BE%D1%80%D1%8B" target = "_blank">Авторы</a>
               </td></tr>'."\r\n"); 
	print('</table>'."\r\n");

	
?>
