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
		include("viewteamresultdata.php");
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
		// Результаты ММБ (по всем командам)
		include("viewadminmoderatorspage.php");
	} elseif ($view == "ViewAdminDataPage") {
		// Результаты ММБ (по всем командам)
		include("viewadmindatapage.php");
	} elseif ($view == "ViewAdminUnionPage") {
		// Результаты ММБ (по всем командам)
		include("viewadminunionpage.php");
	}

	// Очищаем переменную
	$view = "";

?>
