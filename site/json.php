<?php

// Общие настройки
include("settings.php");
// Библиотека функций
include("functions.php");

// Первичная проверка данных авторизации
if ($_POST['Login'] == "") 
{
   print("Login is missing");
   return;

} elseif ($_POST['Password']== "") {
   print("Password is missing");
   return;
} 

// Аутентификация и авторизация -- проверка прав на получение дампа (администратор)
$Sql = "select user_id, user_admin from Users where user_hide = 0 and trim(user_email) = trim('".$_POST['Login']."') and user_password = '".md5(trim($_POST['Password']))."'";
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

// Сбор данных для дампа

$data = array();

$Sql = "select raid_id, raid_name, raid_registrationenddate from Raids";
$Result = MySqlQuery($Sql);  
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Raids"][] = $Row; }

$Sql = "select distance_id, raid_id, distance_name from Distances";
$Result = MySqlQuery($Sql);  
while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Distances"][] = $Row; }

print json_encode( $data );

?>
