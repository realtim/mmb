<?php

// Содержание правого "фрейма"

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
	}

	// Очищаем переменную
	$view = "";

?>
