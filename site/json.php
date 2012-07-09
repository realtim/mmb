<?php

// Проверка, как именно используем скрипт: из интерфейса или отдельно
if (isset($MyPHPScript) and $action == 'JSON')
{
  if (!$Administrator) return;
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

  // Аутентификация и авторизация -- проверка прав на получение дампа (администратор)
  $Sql = "select user_id, user_admin from Users where user_hide = 0 and trim(user_email) = trim('".$_GET['Login']."') and user_password = '".md5(trim($_GET['Password']))."'";
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

// Сбор данных для дампа
$data = array();

// Raids: raid_id, raid_name, raid_registrationenddate
$Sql = "select raid_id, raid_name, raid_registrationenddate from Raids";
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Raids"][] = $Row; }
mysql_free_result($Result);

// Distances: distance_id, raid_id, distance_name
$Sql = "select distance_id, raid_id, distance_name from Distances";
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Distances"][] = $Row; }
mysql_free_result($Result);

// Levels: level_id, distance_id, level_name, level_order, level_starttype, level_pointnames, level_pointpenalties, level_begtime, level_maxbegtime, level_minendtime, level_endtime
$Sql = "select level_id, distance_id, level_name, level_order, level_starttype, level_pointnames, level_pointpenalties, level_begtime, level_maxbegtime, level_minendtime, level_endtime from Levels";
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Levels"][] = $Row; }
mysql_free_result($Result);

// LevelPoints: levelpoint_id, level_id, pointtype_id
$Sql = "select levelpoint_id, level_id, pointtype_id from LevelPoints";
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["LevelPoints"][] = $Row; }
mysql_free_result($Result);

// Teams: team_id, distance_id, team_name, team_num // *
$Sql = "select team_id, distance_id, team_name, team_num from Teams where team_hide = 0";
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Teams"][] = $Row; }
mysql_free_result($Result);

// Users: user_id, user_name, user_birthyear // *
$Sql = "select user_id, user_name, user_birthyear from Users where user_hide = 0";
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Users"][] = $Row; }
mysql_free_result($Result);

// TeamUsers: teamuser_id, team_id, user_id, teamuser_hide
$Sql = "select teamuser_id, team_id, user_id, teamuser_hide from TeamUsers";
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["TeamUsers"][] = $Row; }
mysql_free_result($Result);

// TeamLevelDismiss: user_id, levelpoint_id, team_id, teamuser_id, teamleveldismiss_date, device_id
$Sql = "select user_id, levelpoint_id, team_id, teamuser_id, teamleveldismiss_date, device_id from TeamLevelDismiss";
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["TeamLevelDismiss"][] = $Row; }
mysql_free_result($Result);

// TeamLevelPoints: user_id, levelpoint_id, team_id, teamlevelpoint_date, device_id, teamlevelpoint_datetime, teamlevelpoint_points, teamlevelpoint_comment
$Sql = "select user_id, levelpoint_id, team_id, teamlevelpoint_date, device_id, teamlevelpoint_datetime, teamlevelpoint_points, teamlevelpoint_comment from TeamLevelPoints";
$Result = MySqlQuery($Sql);
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["TeamLevelPoints"][] = $Row; }
mysql_free_result($Result);

// Заголовки, чтобы скачивать можно было и на мобильных устройствах просто браузером (который не умеет делать Save as...)
header("Content-Type: application/octet-stream");
header("Content-Disposition: attachment; filename=\"update.json\"");

// Вывод json
print json_encode( $data );

?>
