<?php

/**
 * +++++++++++ Левое меню +++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) {
    return;
}

?>
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

	// Одинаковые действия при регистрации пользователя и обновлении данных.
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
		
		var template = /^[a-z0-9_\.\-]+@[a-z0-9\-\_\.]+\.[a-z0-9]{2,6}$/i; 
//		var template = /^[A-Za-z0-9_\.\-]+@[A-Za-z0-9\-\_\.]+\.[A-Za-z0-9]{2,6}$/;
//		var template = /^[A-Za-z0-9_\.]+@[A-Za-z0-9]+\.[A-Za-z0-9]{2,6}$/;
//		var template = /^[A-Za-z0-9](([_\.\-]?[a-zA-Z0-9]+)*)@([A-Za-z0-9]+)(([\.\-]?[a-zA-Z0-9]+)*)\.([A-Za-z])+$/;
        if (template.test(trimBoth(email))) {
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

	function _onClick(ctrl, val)
	{
		if (trimBoth(ctrl.value) == val)
			ctrl.value = '';
	}

	function _onBlur(ctrl, val)
	{
		if (trimBoth(ctrl.value) == '')
			ctrl.value = val;
	}


</script>

<?php

        if (!isset($UserId)) { $UserId = 0; }
        if (!isset($RaidId)) { $RaidId = 0; }

	// какое выводить меню
	if ($UserId <= 0)
	{
		print('<form name="UserLoginForm" action="'.$MyPHPScript.'" method="post" onSubmit="return ValidateUserLoginForm();">'."\r\n");
		print('<input type="hidden" name="action" value="UserLogin">'."\r\n");
		print('<input type="hidden" name="view" value="'.$view.'">'."\r\n");
		print('<table class="menu" border="0" cellpadding="0" cellspacing="0">'."\r\n");
		print('<tr><td class="input"><input type="text" name="Login"
			style="width: 185px;" value="E-mail" tabindex="101" ' . CMmbUI::placeholder('E-mail')
			."></td></tr>\r\n");
		print('<tr><td class="input"><input type="password" name="Password" style="width:101px;" size="10" value="" tabindex="102">
			<input type="submit" name="RegisterButton" value="Вход" tabindex="103" style="margin-left: 25px; width: 55px;"></td></tr>'."\r\n");
		print('<tr><td><a href="javascript:RestorePassword();" title="Будет выслан запрос о сменен пароля на указанный выше e-mail">Забыли пароль?</a></td></tr>'."\r\n");
		print('<tr><td><a href="javascript:NewUser();" title="Переход к форме регистрации нового пользователя">Зарегистрироваться</a></td></tr>'."\r\n");
		print('</table>'."\r\n");
		print('</form>'."\r\n");
	} else {
		$UserName = CMmbUi::toHtml(CSql::userName($UserId));
		print('<form name="UserLoginForm" action="'.$MyPHPScript.'" method="post">'."\r\n");
		print('<input type="hidden" name="action" value="">'."\r\n");
		print('<input type="hidden" name="UserId" value="">'."\r\n");
		print('<input type="hidden" name="view" value="'.$view.'">'."\r\n");
		print('<table class="menu" border="0" cellpadding="0" cellspacing="0">'."\r\n");
		print('<tr><td><a href="?UserId='.$UserId.'" title="Переход к Вашей карточке пользователя">'.$UserName.'</a></tr>'."\r\n");
		// !! реализовать показ ссылки на список заявок только если заявки существуют и не отклонены !!
		if (CSql::userUnionLogId($UserId)) {
			print('<tr><td><a href="javascript:ViewUserUnionPage();" title="Заявки на слияние Вас с другими пользователями">Запросы на слияние</a></td></tr>'."\r\n");
		}
		print('<tr><td><a href="javascript:UserLogout();" style="font-size: 80%;">Выход</a></td></tr>'."\r\n");
		print('</table>'."\r\n");
		print('</form>'."\n");
	}
		print('</br>'."\n");
?>

<script language="JavaScript">
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

	function ShowEmail()
	{
		var begstr = "<?php echo substr(trim($_SERVER['SERVER_NAME']), 0, 4); ?>";
		var endstr = "<?php echo substr(trim($_SERVER['SERVER_NAME']), -3); ?>";

		begstr = begstr.replace("\.","site\@");
		endstr = endstr.replace("\.ru","\.com");
		begstr = begstr + "googlegroups" + endstr;
		alert(begstr);
	}

	function NewRaid()
	{
		document.FindTeamForm.action.value = "RegisterNewRaid";
		document.FindTeamForm.submit();
	}

	function ViewRaidInfo()
	{
		document.FindTeamForm.action.value = "RaidInfo";
		document.FindTeamForm.submit();
	}

	function ViewAdminModeratorsPage()
	{
		document.FindTeamForm.action.value = "ViewAdminModeratorsPage";
		document.FindTeamForm.submit();
	}

	function ViewRaidFiles()
	{
		document.FindTeamForm.action.value = "ViewRaidFilesPage";
		document.FindTeamForm.submit();
	}

	function ViewLevelPoints()
	{
		document.FindTeamForm.action.value = "ViewLevelPointsPage";
		document.FindTeamForm.submit();
	}

	function ViewScanPoints()
	{
		document.FindTeamForm.action.value = "ViewScanPointsPage";
		document.FindTeamForm.submit();
	}

	function ViewLevelPointDiscounts()
	{
		document.FindTeamForm.action.value = "ViewLevelPointDiscountsPage";
		document.FindTeamForm.submit();
	}

	function ViewAdminDataPage()
	{
		document.FindTeamForm.action.value = "ViewAdminDataPage";
		document.FindTeamForm.submit();
	}

	function ViewAdminUnionPage()
	{
		document.FindTeamForm.action.value = "ViewAdminUnionPage";
		document.FindTeamForm.submit();
	}

	function ChangeRaid()
	{
		document.FindTeamForm.action.value = "ViewRaidTeams";
		document.FindTeamForm.submit();
	}

	function ViewUserUnionPage()
	{
		document.FindTeamForm.action.value = "ViewUserUnionPage";
		document.FindTeamForm.submit();
	}

	function ViewRaidsUsersLinksPage()
	{
		document.FindTeamForm.action.value = "ViewUsersLinksPage";
		document.FindTeamForm.submit();
	}

	function ViewAllRaidsBadgesPage()
	{
		document.FindTeamForm.action.value = "ViewAllBadgesPage";
		document.FindTeamForm.submit();
	}
</script>

<?php
	// выводим окно для поиска команды
	print('<form name="FindTeamForm" action="'.$MyPHPScript.'" method="post" onSubmit="return ValidateFindTeamForm();">'."\r\n");
	print('<input type="hidden" name="action" value="FindTeam">'."\r\n");
	print('<input type="hidden" name="view" value="'.$view.'">'."\r\n");
	// Эта переменная нужна только тогда, когда из спиcка марш-бросков выбирают дистанцию
	print('<input type="hidden" name="DistanceId" value="0">'."\r\n");
	print('<table class="menu" border="0" cellpadding="0" cellspacing="0">'."\r\n");
	print('<tr><td class="input">ММБ'."\r\n");
	print('<select name="RaidId" style="width: 141px; margin-left: 5px;" tabindex="201" title="Список марш-бросков"  onchange="ChangeLogo(this.value); ChangeRaid();">'."\r\n");
		$notAdmin = $Administrator ? "true" : "raid_registrationenddate is not null";
		$sql = "select raid_id, raid_name from Raids where $notAdmin order by raid_id desc ";
		$Result = MySqlQuery($sql);
		while ($Row = mysqli_fetch_assoc($Result))
		{
			print('<option value="'.$Row['raid_id'].'" '.(($Row['raid_id'] == $RaidId) ? 'selected' : '').' onclick="/* javascript: ChangeRaid(); */">'.$Row['raid_name']."</option>\r\n");
		}
		mysqli_free_result($Result);
	print("</select>\r\n");
	print("</td></tr>\r\n");


	// 21/03/2016 Новая логика показа ссылки "Новая команда"
	// интервал: от регистрации до закрытия протокола
	//  кому: всем, у кого ещё нет команды, модератору, администратору.
	// Комментарий: реально пользователь без спец.ю пав может создать команду только "Вне зачета" - это должно проверяться уже на этапе записи данных
	// модератор и администратор могут указать не себя, а другого пользователя, поэтому им нужно дать возможность создавать команду, даже когда они сами уже участвуют в какой-то
	// и - опять же - проверка при записи данных, что пользователь может быть только в одной команде
	// 19/06/2015 Пользователь должен быть авторизован и иметь права
	// Создание новой команды возможно, пока не закрыт протокол 
		//CanCreateTeam($Administrator, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange)

    if ($UserId and $RaidId and CRights::canCreateTeam($UserId, $RaidId)) {
        print('<tr><td><a href="javascript:NewTeam();" title="Регистрация новой команды на выбранный выше ММБ">Заявить команду</a></td></tr>' . "\r\n");
    }
    // !! реализовать показ ссылки на свою команду, если она существует !!


    $teamId = CSql::userTeamId($UserId, $RaidId);
    if ($teamId) {
        print("<tr><td><a href=\"$MyPHPScript?TeamId=$teamId\" title=\"Просмотр карточки Вашей команды\">Моя команда</a></td></tr>\r\n");
    }

	// Команды
	print('<tr><td><a href="?protocol&RaidId='.$RaidId.'" title="Список команд для выбранного выше ММБ">Команды</a></td></tr>'."\r\n");

	// Волонтёры
	print('<tr><td><a href="?developers&RaidId='.$RaidId.'" title="Команда подготовки и проведения для выбранного выше ММБ">Судьи</a></td></tr>'."\r\n");

	// Файлы
	print('<tr><td><a href="?files&RaidId='.$RaidId.'" title="Карты, легенды, описания и т.п. для выбранного выше ММБ">Материалы</a></td></tr>'."\r\n");

	// Впечатления
	print('<tr><td><a href="?links&RaidId='.$RaidId.'" title="Список впечатлений для выбранного выше ММБ">Впечатления</a></td></tr>'."\r\n");

	// Все ММБ
	print('<tr><td><a href="?raids" title="Список всех ММБ">Все марш-броски</a></td></tr>'."\r\n");


	// Ввод/Правка марш-броска
	if ($Administrator)
	{
		print('<tr><td><a href="javascript:NewRaid();" title="Создание марш-броска">Новый марш-бросок</a></td></tr>'."\r\n");
		print('<tr><td><a href="javascript:ViewRaidInfo();" title="Параметры марш-броска">Марш-бросок</a></td></tr>'."\r\n");
		print('<tr><td><a href="javascript:ViewAdminModeratorsPage();" title="Список модераторов">Модераторы</a></td></tr>'."\r\n");
		print('<tr><td><a href="javascript:ViewUserUnionPage();" title="Администрирование заявок на слияние">Слияние пользователей</a></td></tr>'."\r\n");
	}
	// КП, точки сканирования, задание амнистий, импорт-экспорт, пересчет, объединение команд
	if ($Administrator || $Moderator )
	{
		print('<tr><td><a href="javascript:ViewLevelPoints();" title="Список точек (КП) для выбранного выше ММБ">Точки (КП)</a></td></tr>'."\r\n");
	//	print('<tr><td><a href="javascript:ViewScanPoints();" title="Список точек сканирования для выбранного выше ММБ">Скан-точки</a></td></tr>'."\r\n");
		print('<tr><td><a href="javascript:ViewLevelPointDiscounts();" title="Интервалы КП с амнистией">Амнистия</a></td></tr>'."\r\n");
		print('<tr><td><a href="javascript:ViewAdminDataPage();" title="Экспорт-импорт данных с планшетов">Обмен данными</a></td></tr>'."\r\n");
		print('<tr><td><a href="javascript:ViewAdminUnionPage();" title="Управление объединением команд">Объединение команд</a></td></tr>'."\r\n");
	}

	// Поиск команды
	print('<tr><td style="padding-top: 15px;"><input type="text" name="TeamNum" style="width: 125px;" value="Номер команды" tabindex="206" title="Будет выведена карточка команды с указанным номером для выбранного выше ММБ"'.
			CMmbUI::placeholder('Номер команды') . '>
		<input type="submit" name="FindButton" value="Найти" style="width: 55px; margin-left: 5px;" tabindex="207"></td></tr>'."\r\n");
	print('</table>'."\r\n");
	print('</form>'."\r\n");

	// Поиск участника
	print('<form name="FindUserForm" action="'.$MyPHPScript.'" method="post" onSubmit="return ValidateFindUserForm();">'."\r\n");
	print('<input type="hidden" name="action" value="FindUser">'."\r\n");
	print('<input type="hidden" name="view" value="'.$view.'">'."\r\n");
	print('<input type="hidden" name="RaidId" value="'.$RaidId.'">'."\r\n");
	print('<input type="hidden" name="DistanceId" value="0">'."\r\n");
	print('<table class="menu" border="0" cellpadding="0" cellspacing="0">'."\r\n");
	print('<tr><td class="input"><input type="text" name="FindString" style="width:125px;" value="Часть ФИО" tabindex="301"
		title="Будет выведен список пользователей, чьи ФИО содержат указанный текст. Для вывода всех наберите: все-все (можно и все-все-все)."'
			.CMmbUI::placeholder('Часть ФИО') . '>
		<input type="submit" name="FindButton" value="Найти" style="width: 55px; margin-left: 5px;" tabindex="302"></td></tr>'."\r\n");
	print('</table>'."\r\n");
	print('</form>'."\r\n");
	print('</br>'."\r\n");

	// Внешние ссылки
	print('<table class="menu" border="0" cellpadding="0" cellspacing="0">'."\r\n");
	print('<tr><td><a href="https://github.com/realtim/mmb/wiki/%D0%92%D0%BE%D0%BF%D1%80%D0%BE%D1%81%D1%8B-%D0%B8-%D0%BE%D1%82%D0%B2%D0%B5%D1%82%D1%8B">Вопросы и ответы</a></td></tr>'."\r\n");
	print('<tr><td><a href="https://community.livejournal.com/-mmb-/" title="Сообщество ММБ в Живом Журнале" target = "_blank">Сообщество в ЖЖ</a></td></tr>'."\r\n");
	print('<tr><td><a href="https://slazav.xyz/maps/podm.htm" title="Карты ММБ для просмотра и загрузки в GPS" target = "_blank">Карты</a></td></tr>'."\r\n");
	print('<tr><td><a href="?rating" title="Страница рейтинга участников">Рейтинг</a></td></tr>'."\r\n");
	print('<tr><td><a href="/museum/" title="Музей ММБ">Музей</a></td></tr>'."\r\n");
	print('<tr><td><a href="?badges" title="Значки со всех ММБ">Все значки</a></td></tr>'."\r\n");
	print('<tr><td><a href="'.$MyLocation.'vp_old.html" title="Ручная подборка впечатлений за 2003-2013гг" target = "_blank">Архив впечатлений</a></td></tr>'."\r\n");

    if (CRights::canViewLogs($UserId)) {
        print('<tr><td><a href="?logs" title="Просмотр логов">Логи</a></td></tr>' . "\r\n");
    }

	print("</table>\r\n");
	print("</br>\r\n");

	// Почта
	print('<table class="menu" border="0" cellpadding="0" cellspacing="0">'."\r\n");
	print('<tr><td><a style="font-family: Times New Roman, Serif; font-size: 100%;" href="javascript: ShowEmail();" title="Адрес латинскими буквами или кликните мышкой">ммбсайт@googlegroups.com</a></td></tr>'."\r\n");
	print('<tr><td><a href="https://github.com/realtim/mmb/wiki/%D0%90%D0%B2%D1%82%D0%BE%D1%80%D1%8B">Авторы</a></td></tr>'."\r\n");
	print('<tr><td><div style="float:left; vertical-align: top; margin-top: 4px; margin-right: 15px;">Используется</div> <a href="https://www.jetbrains.com/phpstorm/" target="_blank"><img name="icon_PhpStorm" style="margin-top: 0px; border: none" alt="PhpStorm" width="64" height="64" src="'.trim($MyStoreHttpLink).'icon_PhpStorm.png"></a></td></tr>'."\r\n");

	print('</table>'."\r\n");
?>
