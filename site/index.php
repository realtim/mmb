<?php

$tmSt = microtime(true);

    // Общие настройки
	include("settings.php");
	// Библиотека функций
	include("functions.php");

	CMmbLogger::enable(isset($_GET['time']) || isset($_COOKIE['time']));

    ini_set('display_errors', 1);
    if (isset($DebugMode) && $DebugMode === 1) {
        // Устанавливаем режим отображения ошибок
        error_reporting(E_ALL);
    } else {
        error_reporting(E_ERROR);
    }

    // Пробегаем по массивам POST GET REQUEST COOKIE  и чистим возможные sql инъекции и мусор
    ClearArrays();

	// Устанавливаем часовой пояс по умолчанию
	date_default_timezone_set("Europe/Moscow");

    // Флаг ошибки (1 - цвет текста statustext становится красным и всплывает окно с сообщением)
	// и текст, который выводится в статусную строку (под логотип)
	$alert = 0; 
	$statustext = ""; //"Сегодня: ".date("d.m.Y")."  &nbsp; Время: ".date("H:i:s");

	// Инициализируем права доступа пользователя
	$SessionId = mmb_validate($_COOKIE, CMmb::CookieName, '');
	$OldSessionId = $SessionId;
	$RaidId = (int) mmb_validateInt($_REQUEST, 'RaidId', 0);
	$TeamId = (int) mmb_validateInt($_REQUEST, 'TeamId', 0);
	$DistanceId = (int) mmb_validateInt($_REQUEST, 'DistanceId', 0);

	// 21/03/2016 получаем $UserId из сессии.
	// эта инициализация сейчас перекрываетcя в GetPrivileges, но если в будущем захочется отказаться от GetPrivileges
	// то полезно пользователя определять здесь и остальные необходимые операции тоже здесь делать
	$UserId = (int) CSql::userId($SessionId);

	// обновляем данные сессии (удаляем закрытые, пишем в cookies	
	UpdateSession($SessionId);

    // 27/12/2013 Заменил на сортировку по ключу
    // Находим последний ММБ, если ММБ не указан, чтобы определить привилегии
    if (empty($RaidId)) {
        //GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange);

        $orderBy = CSql::userAdmin($UserId) ? 'raid_id' : 'raid_registrationenddate';
        $sql = "select raid_id
                   from Raids
                   order by $orderBy desc
                   LIMIT 0,1 ";
        $RaidId = CSql::singleValue($sql, 'raid_id');
    }
	// Конец определения ММБ

	// 21/03/2016 хочется потихоньку перетащить всю существенную часть из GetPrivileges и убрать этот вызов
	GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange);

	// Инициализируем переменные сессии, если они отсутствуют
	if (!isset($view)) {
        $view = "";
    }
	if (!isset($viewsubmode)) {
        $viewsubmode = "";
    }

    // если action не задан -- угадываем его по параметрам $_GET
    if (isset($_REQUEST['action'])) {
        $action = $_REQUEST['action'];
    } elseif (mmb_validateInt($_GET, 'UserId', '') !== false) {
        $action = "UserInfo";
    } elseif (mmb_validateInt($_GET, 'TeamId', '') !== false) {
        $action = "TeamInfo";
    } elseif (isset($_GET['protocol'])) {
        $action = "ViewRaidTeams";
    } elseif (isset($_GET['rating'])) {
        $action = "ViewRankPage";
    } elseif (isset($_GET['logs'])) {
        $action = "viewLogs";
    } elseif (isset($_GET['badges'])) {
        $action = "ViewAllBadgesPage";
    } elseif (isset($_GET['amnesty'])) {
        $action = "ViewLevelPointDiscountsPage";
    } elseif (isset($_GET['files'])) {
        $action = "ViewRaidFilesPage";
    } elseif (isset($_GET['links'])) {
        $action = "ViewUsersLinksPage";
    } elseif (isset($_GET['raids'])) {
        $action = "";
    } elseif (isset($_GET['changepasswordsessionid'])) {
        $action = "sendpasswordafterrequest";
    } elseif (isset($_GET['developers'])) {
        $action = "ViewRaidDevelopersPage";
    } elseif (mmb_validateInt($_GET, 'RaidId', '') !== false)        // должно идти предпоследним
    {
        $action = "ViewRaidTeams";
    } else {
        $action = "";
    }

	$tmAction = CMmbLogger::addInterval('before action', $tmSt);

    if ($action === '') {
        // Действие не указано
        $view = "MainPage";
    } elseif ($action === 'StartPage') {
        $view = $_REQUEST['view'];
    } else {
        // Обработчик событий, связанных с пользователем
        include("useraction.php");
        // Если у нас новая сессия после логина, логаута или
        // прихода по ссылке - заново определяем права доступа
        if ($SessionId <> $OldSessionId) {
            $UserId = (int)CSql::userId($SessionId);
            // обновляем данные сессии (удаляем закрытые, пишем в cookies
            UpdateSession($SessionId);
            // 21/03/2016 хочется потихоньку перетащить всю существенную часть из GetPrivileges  и убрать этот вызов
            GetPrivileges($SessionId, $RaidId, $TeamId, $UserId, $Administrator, $TeamUser, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange);
        }

        // Обработчик событий, связанных с командой
        include("teamaction.php");

        // Обработчик событий, связанных с результатами команды
        include("teamresultaction.php");

        // Обработчик событий, связанных с администрированием
        include("adminaction.php");

        // Обработчик событий, связанных с марш-броском
        include("raidaction.php");
    }

    $tmActionEn = CMmbLogger::addInterval('---- action', $tmAction);

    // 15,01,2012 Сбрасываем действие в самом конце, а не здесь
    //$action = "";

    // 17-02-2016 Прописал файлы в относительную папку
    $FavIconFile = $MyLocation . 'styles/mmb_favicon.png';
    $CssFile = $MyLocation . 'styles/mmb.css';
?>

<html lang="ru">
     <head>
          <link rel="stylesheet" type="text/css"  href="<?= $CssFile ?>" />
          <link rel="icon" type="image/png" href="<?= $FavIconFile ?>" />
          <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
         <title>ММБ</title>
     </head>

 <?php
 $mmbLogos = [];
 $Sql = "select r.raid_id, COALESCE(f.raidfile_name, '') as logo_file
		from Raids r
		left outer join (
			select raidfile_name, raid_id
	                        from RaidFiles
	                        where filetype_id = 2 -- logo link
	     			order by raidfile_id desc
	     			limit 0, 1
		) f on r.raid_id = f.raid_id";
 $Result = MySqlQuery($Sql);
 while (($Row = mysqli_fetch_assoc($Result))) {
     // 08.12.2013 Ищем ссылку на логотип
     $LogoFile = trim($Row['logo_file']);
     if ($LogoFile <> '' && file_exists($MyStoreFileLink . $LogoFile)) {
         $link = trim($MyStoreHttpLink) . $LogoFile;
         $mmbLogos[] = "                {$Row['raid_id']}: '$link'";
     }
 }
 mysqli_free_result($Result);
 ?>

 <script language="JavaScript">
     function ChangeLogo(raidid) {
         let links = {
             <?php echo implode(",\r\n", $mmbLogos); ?>};

         if (console)
             console.log("change logo called");
         document.mmblogo.src = links[raidid] || '';
     }
 </script>

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

               <form name="StartPageForm" action="<?= $MyPHPScript; ?>" method="post">
                   <input type="hidden" name="action" value="StartPage">
                   <input type="hidden" name="view" value="MainPage">
                   <input type="hidden" name="RaidId" value="<?php
                   echo $RaidId; ?>">
                   <div align="center">
                       <a href="javascript:document.StartPageForm.submit();">
                           <img name="mmblogo" style="margin-bottom: 15px; border: none" width="160" height="140" alt="ММБ" src="<?= GetMmbLogo($RaidId) ?>">
                       </a>
                   </div>
               </form>

                <!-- вставка меню на php -->
                <?php  include("menu.php"); ?>
                <!-- конец вставки меню на php -->

               </div>
            <!--Конец левой колонки -->
            </td>
            <td align="left" valign="top">
                <!--Правая колонка -->

                <div style="padding-left: 20px; padding-right: 10px;">

                    <!-- сообщение  -->
                    <?php

                    if (!empty($statustext)) {
                        print('<div class = "ErrorText">' . $statustext . '</div>' . "\n");
                        //print('<table width = "100%"><tr><td>'.$statustext.'</td><td style = "border-top-style: dotted; border-top-width: 2px; border-top-color: #CC0000;">&nbsp;</td></tr></table>'."\n");
                    }

                    $tmRn = microtime(true);
                    // вставляем основную часть
                    include('mainpart.php');
                    $tmRne = CMmbLogger::addInterval('---- render', $tmRn);

                    // сбрасываем действие
                    $action = "";
                    // м.б. нужно и view сбрасывать
                    $viewsubmode = "";

                    // закрываем соединение с базой
                    CSql::closeConnection();
                    $tmEnd = CMmbLogger::addInterval('Total: ', $tmSt);

                    print("<div><small>" . CMmbLogger::getText() . "</small></div>");
                    ?>
                </div>
                <!--Конец правой колонки -->
            </td>
        </tr>
	</table>

 </body>
</html>
