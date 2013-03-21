<?php


        // Общие настройки
	include("settings.php");
	// Библиотека функций
	include("functions.php");

        if (isset($DebugMode) and ($DebugMode == 1))
	{
		// Устанавливаем режим отображения ошибок
		ini_set('display_errors',1);
		error_reporting(E_ALL);
	} else {
		ini_set('display_errors',1);
		error_reporting(E_ERROR);
	}


        // Пробегаем помассивам POST GET REAUEST COOKIE  и чистим возможные sql инъекции и мусор
        ClearArrays();

	// Устанавливаем часовой пояс по умолчанию
	date_default_timezone_set("Europe/Moscow");

        // Флаг ошибки (1 - цвет текста statustext становится красным и всплывает окно с сообщением) 
	// и текст, который выводится в статусную строку (под логотип)
	$alert = 0; 
	$statustext = ""; //"Сегодня: ".date("d.m.Y")."  &nbsp; Время: ".date("H:i:s");

	// Инициализируем права доступа пользователя
	if (isset($_POST['sessionid'])) $SessionId = $_POST['sessionid']; else $SessionId = "";
	$OldSessionId = $SessionId;
	if (isset($_REQUEST['RaidId'])) $RaidId = (int)$_REQUEST['RaidId']; else $RaidId = "0";
	if (isset($_REQUEST['TeamId'])) $TeamId = (int)$_REQUEST['TeamId']; else $TeamId = "0";

         // Находим последний ММБ, если ММБ не указан, чтобы определить привелегии
        if (empty($RaidId))
	{ 

		  $sql = "select raid_id
			  from Raids 
		 	  order by raid_registrationenddate desc
			  LIMIT 0,1 ";

		  $Result = MySqlQuery($sql);
		  $Row = mysql_fetch_assoc($Result);
		  $RaidId = $Row['raid_id'];
		  mysql_free_result($Result);
        }
	// Конец определения ММБ


	GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage);

	// Инициализуем переменные сессии, если они отсутствуют
	if (!isset($view)) $view = "";
	if (!isset($viewsubmode)) $viewsubmode = "";
	if (!isset($_REQUEST['action'])) $_REQUEST['action'] = "";
	$action = $_REQUEST['action'];

        //Не знаю, относится ли дистанция к переменным сессии, но инициализацию делаем
	if (isset($_REQUEST['DistanceId'])) $DistanceId = (int)$_REQUEST['DistanceId']; else $DistanceId = "0";

	if ($action == "") 
	{
	// Действие не указано
		  $view = "MainPage";
       
	} elseif ($action == "StartPage") {

                $view = $_REQUEST['view'];

	} else {	

              //  echo  $RaidId;
	        // Обработчик событий, связанных с пользователем
		include ("useraction.php");
		// Если у нас новая сессия после логина, логаута или
		// прихода по ссылке - заново определяем права доступа
		if ($SessionId <> $OldSessionId)
			GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage);

	        // Обработчик событий, связанных с командой
		include ("teamaction.php");

	        // Обработчик событий, связанных с результатами команды
		include ("teamresultaction.php");

	        // Обработчик событий, связанных с администрированием
		include ("adminaction.php");


	        // Обработчик событий, связанных с марш-броском
		include ("raidaction.php");


	}

    // 15,01,2012 Сбрасываем действие в самом конце, а не здесь 
    //$action = "";

?>

<html>
 <head>
  <title>ММБ</title>
  <link rel="Stylesheet" type="text/css"  href="styles/mmb.css" />
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8">

 </head>

<?
 
 print('<script language="JavaScript">'."\n");
 print("\r\n");
 print('LogoImgArr=new Array();'."\n");
 print("\r\n");
 
 $Sql = "select raid_logolink, raid_id from Raids"; 
 $Result = MySqlQuery($Sql);
 while ( ( $Row = mysql_fetch_assoc($Result) ) ) 
 { 
   print('LogoImgArr['.$Row['raid_id'].'] = new Image();'."\r\n");
   print('LogoImgArr['.$Row['raid_id'].'].src = "'.$Row['raid_logolink'].'";'."\r\n");
 }
 mysql_free_result($Result);

 print("\r\n");
 print('function ChangeLogo(raidid) '."\r\n");
 print('{document.mmblogo.src=LogoImgArr[raidid].src;}'."\r\n");
 print("\r\n");
 print('</script>'."\n");
?>	


 <body>
 

	<table  width = "100%"  border = "0" cellpadding = "0" cellspacing = "0" valign = "top" align = "left"  >
	<tr>
<!--
		<td  align="left" width = "10">
			</br>
		</td>
-->
		<td  align="left" width = "220" valign = "top" >
		<!--Левая колонка -->
                   <div style = "padding-left: 10px; padding-right: 15px; padding-bottom: 25px;  
		                 border-right-color: #000000;  border-right-style: solid; border-right-width: 1px;
				 border-bottom-color: #000000;  border-bottom-style: solid; border-bottom-width: 1px;">

                        <form name = "StartPageForm" action = "<? echo $MyPHPScript; ?>" method = "post">
				<input type = "hidden" name = "action" value = "StartPage">
				<input type = "hidden" name = "view" value = "MainPage">
				<input type = "hidden" name = "sessionid" value = "<? echo $SessionId; ?>">
				<input type = "hidden" name = "RaidId" value = "<? echo $RaidId; ?>">
				<div align="center"><a href="javascript:document.StartPageForm.submit();"><img name = "mmblogo" style="margin-bottom: 15px; border: none" width="160" height="140" alt="ММБ" src="<? echo GetMmbLogo($RaidId); ?>"></a></div>
                       </form> 

			<!-- вставка меню на php -->
			<?php  include("menu.php"); ?>
			<!-- конец вставки меню на php -->

                   </div>
		<!--Конец левой колонки -->
		</td>
		<td align="left" valign = "top">
		<!--Правая колонка -->

                    <div style = "padding-left: 20px; padding-right: 10px;">

			<!-- сообщение  -->
			<?php 

                         if (!empty($statustext))
                         {
			    print('<div class = "ErrorText">'.$statustext.'</div>'."\n");
                            //print('<table width = "100%"><tr><td>'.$statustext.'</td><td style = "border-top-style: dotted; border-top-width: 2px; border-top-color: #CC0000;">&nbsp;</td></tr></table>'."\n");
                          }

                         // вставляем основную часть			
			 include("mainpart.php"); 

                         // сбрасываем действие
			 $action = "";
                         // м.б. нужно и view сбрасывать 
			 $viewsubmode  = "";

			 // закрываем соединение с базой
			 mysql_close();
			?>
		   </div>
		<!--Конец правой колонки -->
		</td>
	</tr>
	</table>

 </body>
</html>
