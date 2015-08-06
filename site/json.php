<?php

// Проверка, как именно используем скрипт: из интерфейса или отдельно
if (isset($MyPHPScript) and $action == 'JSON')
{
  if (!$Administrator and !$Moderator)
  {
    $statustext = 'Нет прав на экспорт';				     
    $view = "";
    return;
   }
  
   
  if (!isset($_REQUEST['RaidId'])) {$_REQUEST['RaidId'] = "";}
  $RaidId = $_REQUEST['RaidId'];


}
else 
{

  // Общие настройки
  include("settings.php");
  // Библиотека функций
  include("functions.php");

  // Первичная проверка данных авторизации
  if ($_GET['Login'] == "") 
  {
    print("Login is missing");
    return;

  } elseif ($_GET['Password']== "") {
    print("Password is missing");
    return;
  } 

  $RaidId = $_GET['RaidId'];

  // Аутентификация и авторизация -- проверка прав на получение дампа (администратор)
  $Sql = "select user_id, user_admin from Users where user_hide = 0 and trim(user_email) = trim('{$_GET['Login']}') and user_password = '".md5(trim($_GET['Password']))."'";
  $Result = MySqlQuery($Sql);  
  $Row = mysql_fetch_assoc($Result);

  if ($Row['user_id'] <= 0) 
  {
    print("Autenthication failed");
    return;  
  } 

  if ($Row['user_admin'] == 0)
  {
    print("Authorization failed");
    return;
  }

}
// Конец проверки, как именно используем скрипт: из интерфейса или отдельно


// Проверяем, что передали идентификатор ММБ


if (empty($RaidId))
{
	CMmb::setShortResult('Не указан ММБ', '');
	return;

 
/*
  $sql = "select raid_id
 	  from Raids 
 	  order by raid_registrationenddate desc
	  LIMIT 0,1 ";

  $Result = MySqlQuery($sql);
  $Row = mysql_fetch_assoc($Result);
  $RaidId = $Row['raid_id'];
  mysql_free_result($Result);
*/
}



// Сбор данных для дампа
$data = array();

// Raids: raid_id, raid_name, raid_registrationenddate
$Sql = "select raid_id, raid_name, raid_registrationenddate from Raids where raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Raids"][] = $Row; }
mysql_free_result($Result);

// Distances: distance_id, raid_id, distance_name
$Sql = "select distance_id, raid_id, distance_name from Distances d where d.distance_hide = 0 and d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Distances"][] = $Row; }
mysql_free_result($Result);

/*
// Levels: level_id, distance_id, level_name, level_order, level_starttype, level_pointnames, level_pointpenalties, level_begtime, level_maxbegtime, level_minendtime, level_endtime
$Sql = "select level_id, l.distance_id, level_name, level_order, level_starttype, level_pointnames, level_pointpenalties, level_begtime, level_maxbegtime, level_minendtime, level_endtime from Levels l inner join Distances d on l.distance_id = d.distance_id where l.level_hide = 0 and d.distance_hide = 0 and d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Levels"][] = $Row; }
mysql_free_result($Result);
*/


// 18/02/2014 добавил 'экспорт scanpoints
// ScanPoints: 
$Sql = "select sp.scanpoint_id, sp.raid_id, sp.scanpoint_name, sp.scanpoint_order  from ScanPoints sp where sp.scanpoint_hide = 0 and sp.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["ScanPoints"][] = $Row; }
mysql_free_result($Result);


// 18/02/2014 добавил scanpoint_id
// LevelPoints: levelpoint_id, level_id, pointtype_id
$Sql = "select lp.levelpoint_id, lp.pointtype_id, lp.distance_id, lp.levelpoint_name, lp.levelpoint_order, lp.levelpoint_penalty, lp.levelpoint_mindatetime, lp.levelpoint_maxdatetime, scanpoint_id from LevelPoints lp inner join Distances d on lp.distance_id = d.distance_id where lp.levelpoint_hide = 0 and d.distance_hide = 0 and d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["LevelPoints"][] = $Row; }
mysql_free_result($Result);

// LevelPointDiscounts: 
$Sql = "select lpd.levelpointdiscount_id, lpd.distance_id, lpd.levelpointdiscount_value, lpd.levelpointdiscount_start, lpd.levelpointdiscount_finish from LevelPointDiscounts lpd inner join Distances d on lpd.distance_id = d.distance_id where lpd.levelpointdiscount_hide = 0 and d.distance_hide = 0 and d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["LevelPointDiscounts"][] = $Row; }
mysql_free_result($Result);


// Teams: team_id, distance_id, team_name, team_num // *
$Sql = "select team_id, t.distance_id, team_name, team_num from Teams t inner join Distances d on t.distance_id = d.distance_id where t.team_hide = 0 and COALESCE(t.team_outofrange, 0) = 0 and d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Teams"][] = $Row; }
mysql_free_result($Result);

// Users: user_id, user_name, user_birthyear // *
$Sql = "select user_id, user_name, user_birthyear from Users where user_hide = 0";
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Users"][] = $Row; }
mysql_free_result($Result);

// TeamUsers: teamuser_id, team_id, user_id, teamuser_hide
$Sql = "select teamuser_id, tu.team_id, user_id, teamuser_hide from TeamUsers tu inner join Teams t on tu.team_id = t.team_id inner join Distances d on t.distance_id = d.distance_id where t.team_hide = 0  and COALESCE(t.team_outofrange, 0) = 0  and d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["TeamUsers"][] = $Row; }
mysql_free_result($Result);

// 27/03/2013 Отключил экспорт TeamLevelPoints т.к. эта таблица формируется исключительно при импорте данных.
// Пока на планшетах не производится сведение всех данных - не нужна

// TeamLevelDismiss: user_id, levelpoint_id, team_id, teamuser_id, teamleveldismiss_date, device_id

/*
$Sql = "select user_id, levelpoint_id, tld.team_id, teamuser_id, teamleveldismiss_date, device_id from TeamLevelDismiss tld inner join Teams t on tld.team_id = t.team_id inner join Distances d on t.distance_id = d.distance_id where t.team_hide = 0  and d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["TeamLevelDismiss"][] = $Row; }
mysql_free_result($Result);
*/


// 27/03/2013 Отключил экспорт TeamLevelPoints т.к. эта таблица формируется исключительно при импорте данных.
// Пока на планшетах не производится сведение всех данных - не нужна

// TeamLevelPoints: user_id, levelpoint_id, team_id, teamlevelpoint_date, device_id, teamlevelpoint_datetime, teamlevelpoint_comment
/*
$Sql = "select user_id, levelpoint_id, tlp.team_id, teamlevelpoint_date, device_id, teamlevelpoint_datetime,  teamlevelpoint_comment from TeamLevelPoints tlp inner join Teams t on tlp.team_id = t.team_id inner join Distances d on t.distance_id = d.distance_id where t.team_hide = 0  and d.raid_id = ".$RaidId;
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["TeamLevelPoints"][] = $Row; }
mysql_free_result($Result);
*/

// Заголовки, чтобы скачивать можно было и на мобильных устройствах просто браузером (который не умеет делать Save as...)
header("Content-Type: application/octet-stream");
header("Content-Disposition: attachment; filename=\"update.json\"");

// Вывод json
print json_encode( $data );

?>
