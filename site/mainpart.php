<?php
// +++++++++++ Содержание правого "фрейма" ++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

	//print('view = ^'.$view.'^ action = '.$action);

         
	if ($view == ""  or  $view == "MainPage") {
		// Стартовая страница
		include("mainpage.php");
	} elseif ($view == "ViewUserData") {
		//Данные о пользователе
		include("viewuserdata.php");
	} elseif ($view == "ViewTeamData") {
		// Данные о команде 
		include("viewteamdata.php");
		include("viewteamlevelpoints.php");
	} elseif ($view == "ViewUsers") {
		// результаты поиска пользователя 
		include("viewusers.php");
	} elseif ($view == "ViewRaidTeams") {
		// Результаты ММБ (по всем командам)
		include("viewraidteams.php");
	} elseif ($view == "ViewRaidData") {
		// Результаты ММБ (по всем командам)
		include("viewraiddata.php");
	} elseif ($view == "ViewRaidFiles") {
		// Файлы 
		include("viewraidfiles.php");
	} elseif ($view == "ViewScanPoints") {
		// Точки сканирования (скан-точки) 
		include("viewscanpoints.php");
	} elseif ($view == "ViewLevelPoints") {
		// Точки (КП) 
		include("viewlevelpoints.php");
	} elseif ($view == "ViewLevelPointDiscounts") {
		// Амнистия 
		include("viewlevelpointdiscounts.php");
	} elseif ($view == "ViewAdminModeratorsPage") {
		// Мордераторы
		include("viewadminmoderatorspage.php");
	} elseif ($view == "ViewAdminDataPage") {
		// Обмен данными 
		include("viewadmindatapage.php");
	} elseif ($view == "ViewAdminUnionPage") {
		// Объединение команд
		include("viewadminunionpage.php");
	} elseif ($view == "ViewUserUnionPage") {
		// Связь пользователей
		include("viewuserunionpage.php");
	} elseif ($view == "ViewRankPage") {
		// Рейтинг
		include("viewrankpage.php");
	} elseif ($view == "ViewAllBadges") {
		// Все значки
		include("viewallbadges.php");
	} elseif ($view == "ViewUsersLinks") {
		// Впечатления
		include("viewuserslinks.php");
	}
	elseif ($view == "viewLogs") {			// просмотр логов
		include("viewlogs.php");
	} elseif ($view == "ViewRaidDevelopersPage") {
		// Мордераторы
		include("viewraiddevelopers.php");
	}
	// Очищаем переменную
	$view = "";

?>
