<?php

        // Общие настройки
	include("settings.php");
	// Библиотека функций
	include("functions.php");

        // Пробегаем помассивам POST GET REAUEST COOKIE  и чистим возможные sql инъекции и мусор
        ClearArrays();

	// Устанавливаем часовой пояс по умолчанию
	date_default_timezone_set("Europe/Moscow");

        // Флаг ошибки (1 - цвет текста statustext становится красным и всплывает окно с сообщением) 
	// и текст, который выводится в статусную строку (под логотип)
	$alert = 0; 
	$statustext = ""; //"Сегодня: ".date("d.m.Y")."  &nbsp; Время: ".date("H:i:s");

	// Временная инициализация переменной, которая пока больше нигде не устанавливается
	$TeamModeratorConfirmResult = 0;

	// Инициализуем переменные окружения, если они отсутствуют
	if (!isset($_POST['sessionid'])) $_POST['sessionid'] = "";
	if (!isset($_REQUEST['RaidId'])) $_REQUEST['RaidId'] = "";
	if (!isset($_REQUEST['action'])) $_REQUEST['action'] = "";

	// Инициализуем переменные сессии, если они отсутствуют
	if (!isset($view)) $view = "";
	if (!isset($viewsubmode)) $viewsubmode = "";
	$action = $_REQUEST['action'];
	$RaidId = $_REQUEST['RaidId'];

	if ($action == "") 
	{
	// Действие не указано
		$view = "MainPage";

	} elseif ($action == "StartPage") {

                $view = $_REQUEST['view'];

	} else {	
	        // Обработчик событий, связанных с пользователем
		include ("useraction.php");

	        // Обработчик событий, связанных с командой
		include ("teamaction.php");

	        // Обработчик событий, связанных с результатами команды
		include ("teamresultaction.php");

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
				<input type = "hidden" name = "sessionid" value = "<? echo (!empty($SessionId) ? $SessionId : $_POST['sessionid']); ?>">
				<input type = "hidden" name = "RaidId" value = "<? echo (!empty($RaidId) ? $RaidId : $_REQUEST['RaidId']); ?>">
				<a href="javascript:document.StartPageForm.submit();"><img  style = "margin-bottom: 15px;" width = "157" height = "139" border = "0" alt = "ММБ"  src = "http://mmb.progressor.ru/mmbicons/mmb2012v-logo-s_4.png"></a>
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
			?>
		   </div>
		<!--Конец правой колонки -->
		</td>
	</tr>
	</table>

 </body>
</html>
