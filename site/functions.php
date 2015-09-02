<?php
// +++++++++++ Библиотека функций +++++++++++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

class CMmb
{
	const SessionTimeout = 20; // minutes
	const CookieName = "mmb_session";

	public static function setSessionCookie($sessionId)
	{
		setcookie(CMmb::CookieName, $sessionId, time() + 60 * CMmb::SessionTimeout, '/');
	}

	public static function clearSessionCookie()
	{
		setcookie(CMmb::CookieName, "", time() - 24 * 3600, '/');    // a day ago
	}


	public static function setMessage($message)
	{
		global $statustext, $alert;
		$statustext = $message;
		$alert = 0;
	}

	public static function setErrorMessage($errMessage)
	{
		global $statustext, $alert;
		$statustext = $errMessage;
		$alert = 1;
	}

	public static function setError($errMessage, $newView, $viewSubMode)
	{
		global $statustext, $view, $viewsubmode, $alert;
		$statustext = $errMessage;

		$view = $newView;
		$viewsubmode = $viewSubMode;

		$alert = 1;
	}

	public static function setErrorSm($errMessage, $viewSubMode = 'ReturnAfterError')
	{
		global $viewsubmode;
		$viewsubmode = $viewSubMode;
		self::setErrorMessage($errMessage);
	}

	public static function setShortResult($message, $newView)
	{
		global $statustext, $view;
		$statustext = $message;
		$view = $newView;
	}

	public static function setResult($message, $newView, $newViewMode = "")
	{
		global $statustext, $view, $viewmode;
		$statustext = $message;
		$view = $newView;
		$viewmode = $newViewMode;
	}

	public static function setViews($newView, $newViewMode)
	{
		global $view, $viewmode;
		$view = $newView;
		$viewmode = $newViewMode;
	}
};


 function MySqlQuery($SqlString, $SessionId = "", $NonSecure = "")
 {
 // Можно передавать соединение по ссылке &$ConnectionId  MySqlQuery($SqlString,&$ConnectionId, $SessionId,$NonSecure);
 //
 // вызов  MySqlQuety('...',&$ConnectionId, ...);

	 $needLog = strpos($SqlString, 'raid_registrationenddate') !== false;
	 global $logger;

	$NewConnection = 0;
	if (empty($ConnectionId))
	{
		$NewConnection = 1;

		$t1 = microtime(true);
		// Данные берём из settings
		include("settings.php");
		if ($needLog)
			$t1 = $logger->AddInterval('include', $t1);

		$ConnectionId = mysql_connect($ServerName, $WebUserName, $WebUserPassword);
		if ($needLog)
			$logger->AddInterval('connect',  $t1);

		// Ошибка соединения
		if ($ConnectionId <= 0)
		{
			echo mysql_error();
			die();
			return -1;
		}

//  15/05/2015  Убрал установку, т.к. сейчас в mysql всё правильно, а зона GMT +3
		//  устанавливаем временную зону
//		 mysql_query('set time_zone = \'+4:00\'', $ConnectionId);
		//  устанавливаем кодировку для взаимодействия

		$t1 = microtime(true);
	        mysql_query('set names \'utf8\'', $ConnectionId);
		if ($needLog)
			$t1 = $logger->AddInterval('set names utf', $t1);

                // Выбираем БД ММБ
//		echo $DBName;
		$rs = mysql_select_db($DBName, $ConnectionId);

		if ($needLog)
			$logger->AddInterval('select db', $t1);

		if (!$rs)
		{
			echo mysql_error();
			die();
			return -1;
		}

	}
 
  // echo $ConnectionId;

	 $t1 = microtime(true);
	$rs = mysql_query($SqlString, $ConnectionId);
	 if ($needLog)
		 $logger->AddInterval('query', $t1);


	if (!$rs)
	{
		echo mysql_error();
		die();
		return -1;
	}

	// Если был insert - возвращаем последний id
	if (strpos($SqlString, 'insert') !== false)
	{
		$rs = mysql_insert_id($ConnectionId);
	//  echo ' NewId '.$rs;
	}



	if ($NewConnection == 1)
	{
		//mysql_close($ConnectionId); // try not closing
	}

	return $rs;
 
}


class CSql {
	public static function singleRow($query)
	{
		$result = MySqlQuery($query);
		$row = mysql_fetch_assoc($result);
		mysql_free_result($result);
		return $row;
	}

	public static function singleValue($query, $key)
	{
		$result = MySqlQuery($query);
		$row = mysql_fetch_assoc($result);
		mysql_free_result($result);
		return $row[$key];
	}

	public static function rowCount($query)
	{
		$result = MySqlQuery($query);
		$rn = mysql_num_rows($result);
		mysql_free_result($result);
		return $rn;
	}

	public static function userName($userId)
	{
		$sql = "select user_name from Users where user_id = $userId";
		return self::singleValue($sql, 'user_name');
	}

	public static function fullUser($userId)
	{
		$sql = "select * from Users where user_id = $userId";
		return self::singleRow($sql);
	}

	// types:
	// 1 - ссылка на положение
	// 2 - ссылка на логотип
	// 10 - информация о старте
	public static function raidFileName($raidId, $fileType, $justVisible = false)
	{
		$condition = $justVisible ? "raidfile_hide = 0" : "true";
		$sql = "select raidfile_name
	                        from RaidFiles
	                        where raid_id = $raidId and filetype_id = $fileType and $condition
	     			order by raidfile_id desc";

		return trim(self::singleValue($sql, 'raidfile_name'));
	}


	// date -- строка ddmm
	// time -- строка hhmmss
	public static function timeString($year, $date, $time, $noSeconds = true)
	{
		$tDate = trim($date);
		$tTime = trim($time);
		$seconds = $noSeconds ? '00' : substr($tTime, -2);

		return  "'$year-".substr($tDate, -2)."-".substr($tDate, 0, 2)." ".substr($tTime, 0, 2).":".substr($tTime, 2, 2).":$seconds'";
	}
}

  function StartSession($UserId) {

      if ($UserId > 0) 
      {
	      $SessionId = uniqid();
	      $Result = MySqlQuery("insert into  Sessions (session_id, user_id, session_starttime, session_updatetime, session_status)
	                            values ('$SessionId', $UserId, now(), now(), 0)");

              // Записываем время последней авторизации
	      $Result = MySqlQuery("update Users set user_lastauthorizationdt = now()
	                            where user_id = $UserId");

	      CMmb::setSessionCookie($SessionId);
      }  else {
          $SessionId = '';
	  CMmb::clearSessionCookie();
      }   

      return $SessionId;

  }


   // Закрываем неактивные сессии
  function CloseInactiveSessions($TimeOutInMinutes) {
  //  $TimeOut Время в минутах с последнего обновления, для которого закрываются сессии 
  
        // м.б. потом ещё нужно будет закрывать открытые соединения с БД
       $Result = MySqlQuery("update  Sessions set session_status = 1 
			    where session_status = 0 and session_updatetime < date_add(now(), interval - $TimeOutInMinutes MINUTE)");
      return;
  }

   // Удаляем закрытые сессии
  function ClearSessions() {
         // права на  delete у пользователя есть только на  таблицу Sessions
       $Result = MySqlQuery("delete from Sessions where session_status = 1 or session_status = 3");
      return;
  }


  // Получаем данные сессии
  function GetSession($SessionId) {
      if (empty($SessionId))
      {
        return 0;
      } 

      // Закрываем все сессии, которые неактивны 20 минут
      CloseInactiveSessions(CMmb::SessionTimeout);

      // Очищаем таблицу
      ClearSessions();
  //   echo $SessionId;
      $sql = "select user_id, connection_id, session_updatetime, session_starttime
                            from   Sessions 
			    where session_id = '$SessionId'";
      // Тут нужна проверка на превышение времени, на отсутствие сессии и т.п.
      
      $UserId = CSql::singleValue($sql, 'user_id');

      // Обновляем время сессии, если всё ок
      if ($UserId > 0)
      {
	      $Result = MySqlQuery("update  Sessions set session_updatetime = now()
			    where session_status = 0 and session_id = '$SessionId'");
	      CMmb::setSessionCookie($SessionId);
      }
      else
	      CMmb::clearSessionCookie();
      
      return $UserId;

  }


  // Закрываем сессию
  function CloseSession($SessionId, $CloseStatus) {
  //  $CloseStatus  1 - превышение временеия с последнего обновления
  //                3 - пользователь вышел из системы

      if (empty($SessionId) or $CloseStatus <= 0)
      {
        return 0;
      } 

        // м.б. потом ещё нужно будет закрывать открытые соединения с БД
       $Result = MySqlQuery("update  Sessions set session_updatetime = now(), session_status = $CloseStatus
			    where session_status = 0 and session_id = '$SessionId'");
	  CMmb::clearSessionCookie();

      return;
   }


    // Гененрируем пароль
    function GeneratePassword($PasswordLength) {
   // Количество символов в пароле.$PasswordLength
	
	 $CharsArr="qazxswnhyujmedcvfrtgbkiolp1234567890QAZCVFXSWEDRTGBNHYUJMKIOLP";

		 // Определяем количество символов в $chars
		 $CharsArrLen = StrLen($CharsArr) - 1;

		 // Определяем пустую переменную, в которую и будем записывать символы.
 		 $Password = '';

		 // Создаём пароль.
		 while($PasswordLength--) {  $Password.=$CharsArr[rand(0, $CharsArrLen)]; }

       // echo $Password;
      return $Password;
    }



  // Отсылаем пароль
    function SendMail($Email, $Message, $ToName='', $Subject='Информация с сайта ММБ') {
   //
   
    // 20.01.2012 Заменил штатную функцию на более удобную  send_mime_mail (см. ниже)
    send_mime_mail('mmbsite',
 		   'mmb@progressor.ru',
		    $ToName,
		    $Email,
		    'UTF-8',  // кодировка, в которой находятся передаваемые строки
		    'UTF-8', // кодировка, в которой будет отправлено письмо
		    $Subject,
		    $Message."\r\n".'Используйте для вопросов адрес mmbsite@googlegroups.com'."\r\n".'Ответ на это письмо будет проигнорирован.'."\r\n");
		    


    return ;
    }

// ----------------------------------------------------------------------------
// Инициализация всех переменных, отвечающих за уровни доступа

function GetPrivileges($SessionId, &$RaidId, &$TeamId, &$UserId, &$Administrator, &$TeamUser, &$Moderator, &$OldMmb, &$RaidStage, &$TeamOutOfRange)
{
	// Инициализируем переменные самым низким уровнем доступа
	$UserId = 0;
	$Administrator = 0;
	$TeamUser = 0;
	$Moderator = 0;
	$OldMmb = 0;
	$RaidStage = 0;
        $TeamOutOfRange = 0;

	$UserId	= GetSession($SessionId);

	// Проверяем, не является ли пользователь администратором
	if ($UserId > 0)
	{
		$sql = "select user_admin from Users where user_hide = 0 and user_id = $UserId";
		$Result = MySqlQuery($sql);
		if (!$Result) return;
		$Row = mysql_fetch_assoc($Result);
		$Administrator = $Row['user_admin'];
		mysql_free_result($Result);
	}

	// Контролируем, что команда есть в базе
	if ($TeamId > 0)
	{
		$sql = "select team_id, COALESCE(team_outofrange, 0) as team_outofrange from Teams where team_id = $TeamId";
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
		if (mysql_num_rows($Result) == 0) $TeamId = 0;
		$TeamOutOfRange = $Row['team_outofrange'];
		mysql_free_result($Result);
	} 

	// Проверяем, является ли пользователь членом команды
	if (($UserId > 0) && ($TeamId > 0))
	{
		$sql = "select CASE WHEN count(*) > 0 THEN 1 ELSE 0 END as userinteam
				from TeamUsers tu
			where teamuser_hide = 0 and team_id = $TeamId and user_id = $UserId";
		$TeamUser = CSql::singleValue($sql, 'userinteam');
	}

	// Если известна команда, то все дальнейшие действия проводим с тем ММБ,
	// в который записана команда
	if ($TeamId > 0)
	{
		$sql = "select raid_id from Distances d
				inner join Teams t on t.distance_id = d.distance_id
			where t.team_id = $TeamId";
		$RaidId = (int)CSql::singleValue($sql, 'raid_id');
	}

	// Контролируем, что маршбросок существует в базе
	if ($RaidId > 0)
	{
		$sql = "select raid_id from Raids where raid_id = $RaidId";
		if (CSql::rowCount($sql) == 0) $RaidId = 0;
	}

	// Если неизвестен маршбросок
	// то модератор и период маршброска считаются по умолчанию
	if ($RaidId <= 0) return;

	// Проверяем, является ли пользователь модератором марш-броска
	if ($UserId > 0)
	{
		$sql = "select CASE WHEN count(*) > 0 THEN 1 ELSE 0 END as user_moderator
			from RaidModerators
			where raidmoderator_hide = 0 and raid_id = $RaidId and user_id = $UserId";
		$Moderator = CSql::singleValue($sql, 'user_moderator');
	}

	// Определяем, проводился ли марш-бросок до 2012 года
	$sql = "select CASE WHEN raid_registrationenddate is not null and YEAR(raid_registrationenddate) <= 2011
			THEN 1
			ELSE 0
		END as oldmmb
		from Raids where raid_id = $RaidId";

	$OldMmb = CSql::singleValue($sql, 'oldmmb');


         // 21/11/2013  Добавил RaidStage (финиш закрыт, но нельзя показывать результаты и сместил 6 на 7)
         // 30.10.2013 Для трёхдневного ММБ  изменил INTERVAL 12 на INTERVAL 24  

	// RaidStage указывает на то, на какой временной стадии находится ммб
	// 0 - raid_registrationenddate IS NULL, марш-бросок не показывать
	// 1 - raid_registrationenddate еще не наступил
	// 2 - raid_registrationenddate наступил, но удалять участников еще можно
	// 3 - удалять участников уже нельзя, но первый этап не стартовал
	// 4 - первый этап стартовал, финиш еще не закрылся
	// 5 - финиш закрылся, но результаты нельзя показывать
	// 6 - результаты можно показывать, но  raid_closedate не наступил или Is NULL
	// 7 - raid_closedate наступил
	$sql = "select
		CASE
			WHEN r.raid_registrationenddate IS NULL THEN 0
			WHEN r.raid_registrationenddate >= DATE(NOW()) THEN 1
			ELSE 2
		END as registration,
		(select count(*) from Levels l
			inner join Distances d on l.distance_id = d.distance_id
			where (d.raid_id = r.raid_id) and (NOW() >= DATE_SUB(l.level_begtime, INTERVAL COALESCE(r.raid_readonlyhoursbeforestart, 8) HOUR)))
		as cantdelete,
		(select count(*) from Levels l
			inner join Distances d on l.distance_id = d.distance_id
			where (d.raid_id = r.raid_id) and (NOW() >= l.level_begtime))
		as started,
		(select count(*) from Levels l
			inner join Distances d on l.distance_id = d.distance_id
			where (d.raid_id = r.raid_id) and (NOW() < l.level_endtime))
		as notfinished,
		CASE
			WHEN (r.raid_closedate IS NULL) OR (r.raid_closedate >= DATE(NOW())) THEN 0
			ELSE 1
		END as closed,
		COALESCE(r.raid_noshowresult, 0) as noshowresult
		from Raids r where r.raid_id=$RaidId";
	$Row = CSql::singleRow($sql);

	if ($Row['registration'] == 0) $RaidStage = 0;
	elseif ($Row['registration'] == 1) $RaidStage = 1;
	else
	{
		if ($Row['cantdelete'] == 0) $RaidStage = 2;
		elseif ($Row['started'] == 0) $RaidStage = 3;
		elseif ($Row['notfinished'] > 0) $RaidStage = 4;
		else
		{
			if ($Row['closed'] == 0)
			{
			  if ($Row['noshowresult'] == 1)  $RaidStage = 5;
			  else $RaidStage = 6;
			}  
			else $RaidStage = 7;
		}
	}


        // Если команда не определена, а регистрация закончена, то команда вне зачета 	
	if ($RaidStage >= 2  &&  empty($TeamId) && $TeamOutOfRange == 0)
	{
		$TeamOutOfRange = 1;
        }
}

// ----------------------------------------------------------------------------
// Проверка возможности создавать команду

function CanCreateTeam($Administrator, $Moderator, $OldMmb, $RaidStage, $TeamOutOfRange)
{
	// Если марш-бросок еще не открыт - никаких созданий команд
	if ($RaidStage == 0) return(0);

	// Администратор может всегда
	if ($Administrator) return(1);

	// Если марш-бросок закрыт через raid_closedate - остальным нельзя
	// (включая модераторов)
	if ($RaidStage == 7) return(0);

	// В старом марш-броске можно всем, если он открыт через raid_closedate
	if ($OldMmb) return(1);

	// Модератор может до закрытия редактирования через raid_closedate
	if ($Moderator && ($RaidStage < 7)) return(1);

        // Если стоит признак, что команла вне зачета, то можно
        if ($TeamOutOfRange == 1) return(1);

        // Если не стоит признак, что команла вне зачета, то только до закрытия регистрации
        if (($TeamOutOfRange == 0) && ($RaidStage < 2)) return(1);

        // Если попали сюда, то нельзя
	return(0);
}




// ----------------------------------------------------------------------------
// Проверка возможности редактировать команду

function CanEditTeam($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange)
{
	// Если марш-бросок еще не открыт - никаких редактирований
	if ($RaidStage == 0) return(0);

	// Администратор может всегда
	if ($Administrator) return(1);

	// Модератор может до закрытия редактирования через raid_closedate
	if ($Moderator && ($RaidStage < 7)) return(1);

	// Посторонний участник не может никогда
	if (!$TeamUser) return(0);

	// Здесь и ниже остались только члены команды

	// В старом марш-броске можно, если он открыт через raid_closedate
	if ($OldMmb && ($RaidStage < 7)) return(1);


	// Тем, кто вне зачета можно редактировать, сколько угодно 
	if ($TeamOutOfRange && ($RaidStage < 7)) return(1);

	// А в обычном только не позже 12 часов до начала марш-броска
	if ($RaidStage < 3) return(1); else return(0);
}

// ----------------------------------------------------------------------------
// Проверка возможности видеть результаты

function CanViewResults($Administrator, $Moderator, $RaidStage)
{
	// Если марш-бросок еще не открыт - никто его не видит
	if ($RaidStage == 0) return(0);

	// Администратор и модератор могут после старта марш-броска
	// (раньше результатов все равно быть не должно)
	// 19.05.2013  поменят ограничение, чтобы можно было тестировать - тогда результаты до старта могут быть
	if (($Administrator || $Moderator) && ($RaidStage > 1)) return(1);

	// Все остальные могут после финиша марш-броска
	if ($RaidStage > 5) return(1); else return(0);
}


// ----------------------------------------------------------------------------
// Проверка возможности видеть этапы
// 21/11/2013

function CanViewLevels($Administrator, $Moderator, $RaidStage)
{
	// Если марш-бросок еще не открыт - никто его не видит
	if ($RaidStage == 0) return(0);

	// Администратор и модератор могут 
	if (($Administrator || $Moderator)) return(1);

	// Все остальные могут после закрытия финиша марш-броска
	if ($RaidStage >= 5) return(1); else return(0);
}

// ----------------------------------------------------------------------------
// Проверка возможности редактировать результаты

function CanEditResults($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange)
{
	// Если марш-бросок еще не открыт - никаких редактирований
	if ($RaidStage == 0) return(0);

	// Администратор может всегда
	if ($Administrator) return(1);

	// Посторонний участник, не являющийся модератором, не может никогда
	if (!$TeamUser && !$Moderator) return(0);

	// После наступления raid_closedate нельзя
	if ($RaidStage == 7) return(0);

	// В старом марш-броске можно всегда
	if ($OldMmb) return(1);

	// Модератор может после старта марш-броска
	if ($Moderator && ($RaidStage > 3)) return(1);

	// Члены команды могут после финиша марш-броска
	// и до закрытия через raid_closedate
	// 19.05.2013 Могут редактировать только команды вне зачета
	if ($TeamOutOfRange && ($RaidStage > 4)) return(1); else return(0);
}

// ----------------------------------------------------------------------------
// Проверка возможности редактировать признак вне зачета

function CanEditOutOfRange($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage, $TeamOutOfRange)
{
	// Если марш-бросок еще не открыт - никаких редактирований
	if ($RaidStage == 0) return(0);

	// Администратор может всегда
	if ($Administrator) return(1);

	// Посторонний участник, не являющийся модератором, не может никогда
	if (!$TeamUser && !$Moderator) return(0);

	// После наступления raid_closedate нельзя
	if ($RaidStage == 7) return(0);

	// В старом марш-броске можно всегда
	if ($OldMmb) return(1);

	// Модератор может 
	if ($Moderator) return(1);

         // Если попали сюда, то нельзя
	return(0);
}


// 04,06,2014
// ----------------------------------------------------------------------------
// Проверка возможности объединиться с пользователем

function CanRequestUserUnion($Administrator, $UserId, $ParentUserId)
{


	// Оба пользователя должны быть определеные
	if (!$UserId || !$ParentUserId) return(0);

	// Нельзя объединить с самим собой
	if ($UserId == $ParentUserId) return(0);

        // Тут добавить проверку наличия записей в журнале объединения
	$Sql = "select count(*) as result from  UserUnionLogs where union_status <> 0 and  union_status <> 3 and user_id = $UserId";
	$InUnion = CSql::singleValue($Sql, 'result');

         // Если есть в запросе, то нельзя
	if ($InUnion) return(0);


         // Если выше проверки не сработали, то  Администратору можно 
	if ($Administrator) return(1);

        // Пока и всем остальным разрешаем делать запросы          
        return(1);
}
// Конец проверки возможности объединиться с пользователем

// region join users
// 06,06,2014
// ----------------------------------------------------------------------------
// Проверка возможности подтвердить запрос на объединение

function CanApproveUserUnion($Administrator, $UserRequestId, $UserId)
{

	if (!$UserRequestId) return(0);

	// Проверить статус запроса
	$Sql = "select user_id, user_parentid, union_status from  UserUnionLogs where userunionlog_id = $UserRequestId";

	// Подтвердить можно только созданный запрос
	if (CSql::singleValue($Sql, 'union_status') <> 1) {return(0);}


         // Если выше проверки не сработали, то  Администратору можно 
	if ($Administrator) return(1);

        // Подтвердить может только администратор
        return(0);
}
// Конец проверки возможности подтвердить объединение


// 06,06,2014
// ----------------------------------------------------------------------------
// Проверка возможности откатить объединение
function CanRollBackUserUnion($Administrator, $UserRequestId, $UserId)
{
	if (!$UserRequestId) return(0);
	// Проверить статус запроса

	$Sql = "select user_id, user_parentid, union_status from  UserUnionLogs where userunionlog_id = $UserRequestId";
	// Откатить можно только подтвержденный (уже объединённый) запрос
	if (CSql::singleValue($Sql, 'union_status') <> 2) {return(0);}


         // Если выше проверки не сработали, то  Администратору можно 
	if ($Administrator) return(1);

        // Подтвердить может только администратор
        return(0);
}
// Конец проверки возможности откатить объединение


// 06,06,2014
// ----------------------------------------------------------------------------
// Проверка возможности отклонить объединение
function CanRejectUserUnion($Administrator, $UserRequestId, $UserId)
{
	// Проверить статус запроса
	$Sql = "select user_id, user_parentid, union_status from  UserUnionLogs where userunionlog_id = $UserRequestId";
	$Row = CSql::singleRow($Sql);
	$NewUserId = $Row['user_id'];
	$ParentUserId = $Row['user_parentid'];
	$UnionStatus = $Row['union_status'];

	//	echo $Sql;
	//	echo 'stat '.$UnionStatus;
          // Отклолнить можно только созданный запрос
        if ($UnionStatus <> 1) {return(0);}
         	 
	// Все пользователя должны быть определеные
	if (!$UserId || !$ParentUserId || !$NewUserId) return(0);

        // Отклонить запрос может любой из двух пользователей
        if ($UserId == $ParentUserId) return(1);

        if ($UserId == $NewUserId) return(1);
        // Тут добавить проверку наличия записей в журнале объединения

         // Если выше проверки не сработали, то  Администратору можно 
	if ($Administrator ) return(1);

        // Пока и всем остальным разрешаем делать запросы          
        return(0);
}
// Конец проверки возможности отклонить объединение
// endregion


// ----------------------------------------------------------------------------

    // функция 
    // Автор: Григорий Рубцов [rgbeast]  
    // Из статьи Отправка e-mail в русской кодировке средствами PHP
/*
Пример:
send_mime_mail('Автор письма',
               'sender@site.ru',
               'Получатель письма',
               'recepient@site.ru',
               'CP1251',  // кодировка, в которой находятся передаваемые строки
               'KOI8-R', // кодировка, в которой будет отправлено письмо
               'Письмо-уведомление',
               "Здравствуйте, я Ваша программа!");
*/
    function send_mime_mail($name_from, // имя отправителя
                        $email_from, // email отправителя
                        $name_to, // имя получателя
                        $email_to, // email получателя
                        $data_charset, // кодировка переданных данных
                        $send_charset, // кодировка письма
                        $subject, // тема письма
                        $body, // текст письма
                        $html = FALSE // письмо в виде html или обычного текста
                        ) 
    {
      $to = mime_header_encode($name_to, $data_charset, $send_charset)
		  . " <$email_to>";
      $subject = mime_header_encode($subject, $data_charset, $send_charset);

      $from =  mime_header_encode($name_from, $data_charset, $send_charset)
                     ." <$email_from>";
 
      if($data_charset != $send_charset) 
      {
	$body = iconv($data_charset, $send_charset, $body);
      }
      $headers = "From: $from\r\n";
      $type = ($html) ? 'html' : 'plain';
      $headers .= "Content-type: text/$type; charset=$send_charset\r\n";
      $headers .= "Mime-Version: 1.0\r\n";

      // 09/09/2013 Добавил атрибут Return-Path (пятым параметром)
      return mail($to, $subject, $body, $headers, "-f".$email_from);
     }

    function mime_header_encode($str, $data_charset, $send_charset)
    {
      if($data_charset != $send_charset) 
      {
	$str = iconv($data_charset, $send_charset, $str);
      }
      return '=?' . $send_charset . '?B?' . base64_encode($str) . '?=';
    }
    // конец функций для отправки письма



	// функция вычисляет место команды в общем зачёте
	function GetTeamPlace($teamid)
	{
	// Здесь не проверяется прогресс команды,т.е. делается предположение (см. код расчета результата), что результат только для финишировавших команд
	// если это будет не так, то и алгоритм здесь нужно менять.

		$sql = "select TIME_TO_SEC(COALESCE(t.team_result,0)) as result_in_sec, t.distance_id 
				from Teams t
				where  t.team_hide = 0 
					and COALESCE(t.team_outofrange, 0) = 0
					and COALESCE(t.team_result, 0) > 0
					and COALESCE(t.team_minlevelpointorderwitherror, 0) = 0
					and t.team_id = $teamid";

		//    echo $sql;

		$Row = CSql::singleRow($sql);

		$TeamResult =  $Row['result_in_sec'];
		$DistanceId =  $Row['distance_id'];

		if ($TeamResult <= 0 || $DistanceId <= 0)
			return 0;
		// Смотрим сколько команд имеют результат лучше и прибавляем 1
		// Нельзя ставить <=, т.к. на одном месте может быть несколько команд
		$sql_place = "select count(*) + 1 as result_place
					from Teams  t
					where t.team_hide = 0
						and t.distance_id = $DistanceId
						and COALESCE(t.team_outofrange, 0) = 0
						and COALESCE(t.team_result,0) > 0
						and COALESCE(t.team_minlevelpointorderwitherror, 0) = 0
						and TIME_TO_SEC(COALESCE(t.team_result,0)) < $TeamResult";

		// echo $sql_place;
		return CSql::singleValue($sql_place, 'result_place');
	}
    // конец функции расчета места команды в общем зачете


        // функция экранирует спец.символы
	function EscapeString($str)
        {
                $str = (string) $str;
                $search=array("\\","\0","\n","\r","\x1a","'",'"');
                $replace=array("\\\\","\\0","\\n","\\r","\Z","\'",'\"');
                return str_replace($search,$replace,$str);
        }

	function ReverseEscapeString($str)
        {
                $str = (string) $str;
                $search=array("\\\\","\\0","\\n","\\r","\Z","\'",'\"');
                $replace=array("\\","\0","\n","\r","\x1a","'",'"');
                return str_replace($search,$replace,$str);
        }



        // функция экранирует спец.символы в массивах переменных
        // POST GET
	function ClearArrays()
        {

	      foreach ($_POST as $key => $value)
              {
		$_POST[$key] = EscapeString($value);
	      }

	      foreach ($_GET as $key => $value)
              {
		$_GET[$key] = EscapeString($value);
	      }  

	      foreach ($_REQUEST as $key => $value)
              {
		$_REQUEST[$key] = EscapeString($value);
	      }  

	      foreach ($_COOKIE as $key => $value)
              {
		$_COOKIE[$key] = EscapeString($value);
	      }  

        }
        // Конец очистик специальных массивов от возможных инъекций


        // функция экранирует спец.символы в массивах переменных
        // POST GET
	function ReverseClearArrays()
        {

	      foreach ($_POST as $key => $value)
              {
		$_POST[$key] = ReverseEscapeString($value);
	      }

	      foreach ($_GET as $key => $value)
              {
		$_GET[$key] = ReverseEscapeString($value);
	      }  

	      foreach ($_REQUEST as $key => $value)
              {
		$_REQUEST[$key] = ReverseEscapeString($value);
	      }  

	      foreach ($_COOKIE as $key => $value)
              {
		$_COOKIE[$key] = ReverseEscapeString($value);
	      }  

        }
        // Конец очистки специальных массивов от возможных инъекций

     // функция получает ссылку на  логотип
    function GetMmbLogo($raidid)
    {
        // Данные берём из settings
	include("settings.php");
    
	if (empty($raidid))
	{
	$sql = "select raid_logolink
	        from Raids 
		order by raid_registrationenddate desc
		LIMIT 0,1 ";


        // 08.12.2013 Ищем ссылку на логотип  
        $sqlFile = "select rf.raidfile_name
	     from RaidFiles rf
	          inner join Raids r
		  on rf.raid_id = r.raid_id
	     where rf.filetype_id = 2 
	     order by r.raid_registrationenddate desc, rf.raidfile_id desc
	     LIMIT 0,1";


	} else {


	$sql = "select raid_logolink
	        from Raids 
		where  raid_id = $raidid
		LIMIT 0,1 ";
	
	    // 08.12.2013 Ищем ссылку на логотип  
        $sqlFile = "select rf.raidfile_name
	     from RaidFiles rf
	     where rf.raid_id = $raidid and rf.filetype_id = 2
	     order by rf.raidfile_id desc
	     LIMIT 0,1";

	}

        $LogoFile = trim(CSql::singleValue($sqlFile, 'raidfile_name'));

        if ($LogoFile <> '' && file_exists($MyStoreFileLink.$LogoFile))
	        return $MyStoreHttpLink.$LogoFile;

        return CSql::singleValue($sql, 'raid_logolink');
    }
      // Конец получения логотипа


     // 26/12/2013 
     // Проверка корректности внесённых точек
    function CheckLevelPoints($distanceid)
    {
     
       $CheckString = "";
       // Важно, что в "правой" таблице берутся только точки с типом 1,2,4 (старт,финиш, смена карт)
       // условия
       // 1. перед стартом не дожно быть ничего или должен быть финиш
       // 2. перед финишем должен быть старт или смена карт
       // 3. перед сменой карт должен быть старт 
       // 4. перед точкой, которая не является стартом не должен быть финиш


       $sql =  "select  c.levelpoint_id, c.levelpoint_name, c.pointtype_id, c.predpointtype_id
		from 
		(
		 select  a.levelpoint_id,  MAX(a.levelpoint_name) as levelpoint_name,   
		         COALESCE(MAX(a.pointtype_id), 0) as  pointtype_id,
			 MAX(b.levelpoint_order),
			 (select  pointtype_id
			  from LevelPoints
			  where  distance_id = $distanceid
			         and  levelpoint_hide = 0
				 and  levelpoint_order =  MAX(b.levelpoint_order)
			  LIMIT 0,1
			 ) as predpointtype_id
		 from 
			(
			 select  levelpoint_id, levelpoint_name, levelpoint_order, pointtype_id
			 from LevelPoints lp
			 where distance_id = $distanceid
			       and  levelpoint_hide = 0
			) a
			left outer join
			(
			 select  levelpoint_id, levelpoint_name, levelpoint_order, pointtype_id
			 from LevelPoints lp
			 where distance_id = $distanceid
			       and  levelpoint_hide = 0
			       and  pointtype_id in (1,2,4)
			) b
			on a.levelpoint_order > b.levelpoint_order
		group by a.levelpoint_id
	       ) c
	       where (c.pointtype_id  =  1 and c.predpointtype_id not in (0,2)) 
			or 
		     (c.pointtype_id  =  2 and c.predpointtype_id  not in (1, 4)) 
			or 
		     (c.pointtype_id  =  4 and c.predpointtype_id  not in (1)) 
			or 
		     (c.pointtype_id  not in (1) and c.predpointtype_id  = 2) 
	       order by 1";

       //  echo $sql;

	$Row = CSql::singleRow($sql);
	$LevelPointId = $Row['levelpoint_id'];
	$LevelPointName = $Row['levelpoint_name'];

        if (!empty($LevelPointId))
	{
	  $CheckString = "Некорректность в точке $LevelPointName";
	}
	
	$sql = " select lpd.levelpointdiscount_id, lpd.levelpointdiscount_start, lpd.levelpointdiscount_finish 
	         from LevelPoints lp
		      inner join  LevelPointDiscounts lpd
		      on lp.levelpoint_order >= lpd.levelpointdiscount_start
		         and lp.levelpoint_order <= lpd.levelpointdiscount_finish
	         where lp.levelpoint_hide = 0  
		       and lp.distance_id = $distanceid
		       and lpd.distance_id = $distanceid
	               and lp.pointtype_id in (1,2,4) 
		       and lpd.levelpointdiscount_hide = 0";
				 
	$Row = CSql::singleRow($sql);
	$LevelPointId = $Row['levelpointdiscount_id'];
	$LevelPointDiscountStart = $Row['levelpointdiscount_start'];
	$LevelPointDiscountFinish = $Row['levelpointdiscount_finish'];

        if (!empty($LevelPointId))
	{
	  $CheckString .= "Некорректность в интервале $LevelPointDiscountStart - $LevelPointDiscountFinish";
	}


    
       return($CheckString);
    }
     //Конец проверки корректности точек
     
     // 17/02/2014 
     // Проверка корректности внесённых точек сканирования
    function CheckScanPoints($raidid)
    {
     
       $CheckString = "";
    
       return($CheckString);
    }
     //Конец проверки корректности точек сканирования
     
     
        // функция получения комментариядля команды 
     function GetTeamComment($teamid)
     {
       
       
       /*
	// Получаем информацию об этапах, которые могла проходить команда
	$sql = "select tl.teamlevel_comment	        
		from TeamLevels tl
		     inner join Levels l
		     on l.level_id = tl.level_id
		where tl.teamlevel_hide = 0 and l.level_hide = 0 and tl.team_id = ".$teamid;

	$rs = MySqlQuery($sql);

	// ================ Цикл обработки данных по этапам
	$Comment = "";
	while ($Row = mysql_fetch_assoc($rs))
	{
		$Comment = trim($Comment.' '.trim($Row['teamlevel_comment']));
         }
	// Конец цикла по этапам
	*/


	// Получаем информацию оточках, которые прошла команда
	$sql = "select tlp.teamlevelpoint_comment	        
		from TeamLevelPoints tlp
		where tlp.team_id = $teamid";

	$rs = MySqlQuery($sql);

	// ================ Цикл обработки данных по этапам
	$Comment = "";
	while ($Row = mysql_fetch_assoc($rs))
	{
		$Comment = trim($Comment.' '.trim($Row['teamlevelpoint_comment']));
         }
	// Конец цикла по этапам

	mysql_free_result($rs);
	if ($Comment == '') {
	 $Comment = "&nbsp;";
	}
        return ($Comment);
     }
     // конец функции получения общего комментария для команды 


     
        // функция получения вклада в рейтинг
     function RecalcTeamUsersRank($raidid)
     {
        // 03/07/2014 ДОбавил условие, чтобы не считать рейтинг для сошедших
              
        $RaidWhereString = $raidid > 0 ? " and d.raid_id = $raidid " : ' ';

        // Обнуляем рейтинг  
  	$sql = "
		update TeamUsers tu
			inner join Teams t
			on tu.team_id = t.team_id	
		        inner join Users u
		        on tu.user_id = u.user_id
			inner join Distances d
		        on t.distance_id = d.distance_id
		 SET teamuser_rank = NULL 
		 where 1= 1 $RaidWhereString ";

		 $rs = MySqlQuery($sql);

  
	$sql = "
		update TeamUsers tu
 			inner join Teams t
			on tu.team_id = t.team_id	
		        inner join Users u
		        on tu.user_id = u.user_id
			inner join Distances d
		        on t.distance_id = d.distance_id
			inner join 
			(
			 select t.distance_id,  MIN(TIME_TO_SEC(COALESCE(t.team_result, 0))) as firstresult_in_sec 
			 from Teams t
		 	      inner join Distances d
			      on t.distance_id = d.distance_id
			      inner join 
			      (
			       select lp.distance_id, MAX(lp.levelpoint_order) as maxorder
			       from LevelPoints lp
			       group by lp.distance_id
			      ) lpmax
			      on t.team_maxlevelpointorderdone = lpmax.maxorder
			         and t.distance_id = lpmax.distance_id
			 where d.distance_hide = 0 
			       and t.team_hide = 0 
		               and  COALESCE(t.team_outofrange, 0) = 0
		               and  COALESCE(t.team_result, 0) > 0
			       and COALESCE(t.team_minlevelpointorderwitherror, 0) = 0
                         group by t.distance_id
                        ) a
                        on a.distance_id = t.distance_id
			inner join 
			(
			 select d.raid_id,  MAX(COALESCE(d.distance_length, 0)) as maxlength 
			 from  Distances d
			 where d.distance_hide = 0 
			 group by d.raid_id
                        ) b
                        on d.raid_id = b.raid_id
			left outer join TeamLevelDismiss tld
			on tu.teamuser_id = tld.teamuser_id
		 SET teamuser_rank  =  (a.firstresult_in_sec + 0.00)/(TIME_TO_SEC(COALESCE(t.team_result, 0)) + 0.00)*(CASE WHEN b.maxlength > 0 THEN  d.distance_length/(b.maxlength + 0.00) ELSE 1.00 END) 
		 where d.distance_hide = 0 
		       and tu.teamuser_hide = 0
		       and tld.levelpoint_id is NULL
		       and t.team_hide = 0 
		       and  COALESCE(t.team_outofrange, 0) = 0
		       and  COALESCE(t.team_result, 0) > 0
		       and COALESCE(t.team_minlevelpointorderwitherror, 0) = 0

                       $RaidWhereString
                ";
		 
		 $rs = MySqlQuery($sql);
       
         return (1);
     }
     // конец функции получения вклада в рейтинг


      // Получить предыдущий ММБ (по отношению к заданному), когда пользователь регистрировался
     function GetPredRaidForUser($userid, $raidid)
     {

        $PredRaidId = 0;

         // Находим предыдущий марш-бросок (по отношению к заданному), когда пользователь регистрировался
	$sql = " select d.raid_id
	         from TeamUsers tu
		       inner join Teams t
		       on t.team_id = tu.team_id
		       inner join Distances d
		       on t.distance_id = d.distance_id
	         where d.raid_id < $raidid
		       and tu.user_id = $userid
		       and t.team_hide = 0
		       and tu.teamuser_hide = 0 
		 order  by d.raid_id DESC
		 LIMIT 0,1";
	$PredRaidId = CSql::singleValue($sql, 'raid_id');

        return ($PredRaidId);
     }
     //Конец получения предыдущго ММБ

     // Проверка неявки на старт в прошлое участие
     function CheckNotStart($userid, $raidid)
     {

        $NotStart = 0;     
        $PredRaidId = GetPredRaidForUser($userid, $raidid);
	
	// Проверяем  что участник не явился на старт
	if ($PredRaidId) {

		$sql = " select count(*) as result
		         from TeamUsers tu
			       inner join Teams t
			       on t.team_id = tu.team_id
			       inner join Distances d
			       on t.distance_id = d.distance_id
		         where d.raid_id = $PredRaidId
			       and tu.user_id = $userid
			       and COALESCE(t.team_maxlevelpointorderdone, 0) = 0 
			       and t.team_hide = 0
			       and tu.teamuser_hide = 0 ";

	      // echo $sql;
				 
		$NotStart = CSql::singleValue($sql, 'result');
	}
	 // Конец проверки, что участник не явился

        // Если явился сбрасываем ММБ
	return $NotStart == 0 ? 0 : $PredRaidId;
     }
     //Конец статуса неявки на старт в прошлй раз

     // Расчет стоимости участия
     function CalcualteTeamPayment($teamid)
     {

        $TeamMapPayment = 0;
	$sql = " select t.team_mapscount * r.raid_mapprice as mappayment
	         from  Teams t
		       inner join Distances d
		       on t.distance_id = d.distance_id
		       inner join Raids r
		       on d.raid_id = r.raid_id
	         where t.team_id = $teamid
		       and t.team_hide = 0
		 LIMIT 0,1";

	$TeamMapPayment = CSql::singleValue($sql, 'mappayment');

  
	$TeamNotStartPayment = 0;
	$sql = " select SUM(r.raid_nostartprice) as notstartpayment
	         from  TeamUsers tu
		       inner join Raids r
		       on tu.teamuser_notstartraidid = r.raid_id
	         where tu.team_id = $teamid
	        ";

	$TeamNotStartPayment = CSql::singleValue($sql, 'notstartpayment');


   /*
	$sql = " select d.raid_id, tu.user_id
	         from TeamUsers tu
		       inner join Teams t
		       on t.team_id = tu.team_id
		       inner join Distances d
		       on t.distance_id = d.distance_id
		       inner join Raids r
		       on d.raid_id = r.raid_id
	         where t.team_id = ".$teamid."
		       and t.team_hide = 0
		       and tu.teamuser_hide = 0 ";

	$Result = MySqlQuery($sql);


        $TeamUsersNoStartPayment = 0;
	// ================ Цикл обработки участников
	while ($Row = mysql_fetch_assoc($Result))
	{

           $PredRaidId =  CheckNotStart($Row['user_id'], $Row['raid_id']);

	   if ($PredRaidId) {

	     $UserNoStartPayment = 0;
	     $sqlUser =   " select r.raid_nostartprice as usernostartpayment
	                from  Raids r
		        where r.raid_id = ".$PredRaidId."
			LIMIT 0,1";
	     $ResultUser = MySqlQuery($sqlUser);
	     $RowUser = mysql_fetch_assoc($ResultUser);
	     $UserNoStartPayment = $RowUser['usernostartpayment'];
	     mysql_free_result($ResultUser);
  	   
	     $TeamUsersNoStartPayment += $UserNoStartPayment; 
	   }
	   
	         
        }
	// Конец цикла обработки участников
	mysql_free_result($Result);
  */
  
        return ($TeamMapPayment + $TeamNotStartPayment); 
   
     }
     // Конец расчета стоимости участия	


     
     




  // Генерация точек КП в TeamLevelPoint по строчке teamlevels_points
     // для данной $teamlevelid
     
     function GenerateTeamLevelPointsFromTeamLevelString($teamlevelid)
     {


       // Данные о КП, а также информация для старта и финиша
	$sql = " select tl.teamlevel_points, tl.level_id, tl.team_id,
	                tl.teamlevel_begtime,  tl.teamlevel_endtime, 
			tl.teamlevel_comment, tl.teamlevel_penalty, 
			tl.teamlevel_duration
	         from TeamLevels tl
	         where tl.teamlevel_id = $teamlevelid";


       // echo $sql;

	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	$LevelPointsString = trim($Row['teamlevel_points']);
	$LevelId = $Row['level_id'];
	$TeamId = $Row['team_id'];
	$StartTime = $Row['teamlevel_begtime'];
        // Дальше идёт информация для записи в точку "финиш"
	$FinishTime = $Row['teamlevel_endtime'];
	$Comment = $Row['teamlevel_comment'];
	$Penalty = $Row['teamlevel_penalty'];
	$Duration = $Row['teamlevel_duration'];
	mysql_free_result($Result);
	
	
	if (empty($LevelId)) {
	  print("Нет данных об этапе $teamlevelid\r\n");
 	  return;
	}
//
  //      echo $teamlevelid.' , '.$LevelPointsString."\r\n";


        $LevelPointsArr = explode(',', $LevelPointsString);

        $StartLevelPointId = 0;

        // Получаем старт этапа
	$sql = " select lp1.levelpoint_id
	         from LevelPoints lp1
	         where lp1.level_id = $LevelId
		       and lp1.pointtype_id in (1,4)
		 order by  lp1.levelpoint_order ASC     
		 LIMIT 0,1";

        //  echo $sql;
	$StartLevelPointId = CSql::singleValue($sql, 'levelpoint_id');

        //   echo 'sr '.$StartLevelPointId;
        if ($StartLevelPointId and $StartTime > 0) {

                $StartTlpExists = 0;

		$sqltlp = " select tlp.teamlevelpoint_id
		    from TeamLevelPoints tlp
		    where tlp.team_id = $TeamId and tlp.levelpoint_id = $StartLevelPointId";

		$StartTlpExists = CSql::singleValue($sqltlp, 'teamlevelpoint_id');
	       //  echo $sqltlp;
	
		// Пишем старт
		if (!$StartTlpExists) {
	
			$sqltlp = " insert into TeamLevelPoints(device_id, levelpoint_id, team_id, 
			                                        teamlevelpoint_datetime)
			         values(1, $StartLevelPointId, $TeamId, '$StartTime')";

                     //   echo $sqltlp;		    
//		      on tlp.levelpoint_id = lp.levelpoint_id


			$ResultTlp = MySqlQuery($sqltlp);
				
		} else {
		   print("Уже есть точка старта $StartLevelPointId $TeamId\r\n");
		} 
	}
	// Конец проверки на существование точки старта

        $FinishLevelPointId = 0;


        // Получаем финиш этапа
	$sql = " select lp1.levelpoint_id
	         from LevelPoints lp1
	         where lp1.level_id = $LevelId
		       and lp1.pointtype_id in (2,4)
		 order by  lp1.levelpoint_order ASC     
		 LIMIT 0,1";

        //  echo $sql;
	$FinishLevelPointId = CSql::singleValue($sql, 'levelpoint_id');

        if ($FinishLevelPointId and $FinishTime > 0) {
	
		$FinishTlpExists = 0;
	
		$sqltlp = " select tlp.teamlevelpoint_id
			    from TeamLevelPoints tlp
			    where tlp.team_id = $TeamId and tlp.levelpoint_id = $FinishLevelPointId";

		$FinishTlpExists = CSql::singleValue($sqltlp, 'teamlevelpoint_id');

		// Пишем финиш
		if (!$FinishTlpExists) {
	
			$sqltlp = " insert into TeamLevelPoints(device_id, levelpoint_id, team_id,
			                                        teamlevelpoint_datetime, teamlevelpoint_comment,
								teamlevelpoint_duration, teamlevelpoint_penalty)
			         values(1, $FinishLevelPointId, $TeamId, '$FinishTime', '".trim($Comment)."', '$Duration', $Penalty)";
                     //   echo $sqltlp;

			$ResultTlp = MySqlQuery($sqltlp);
				
		} else {
		   print("Уже есть точка финиша $FinishLevelPointId $TeamId\r\n");
		}   
	}
	// Конец проверки на существование точки финиша




	if ( $LevelPointsString == '') {
	   print("Нет данных о точках $teamlevelid\r\n");
 	   return;
	}
//
  //      echo $teamlevelid.' , '.$LevelPointsString."\r\n";


        $LevelPointsArr = explode(',', $LevelPointsString);




        // Теперь получаем список КП
	$sql = " select lp1.levelpoint_id, lp1.levelpoint_order 
	         from LevelPoints lp1
		       inner join Levels l2
		       on lp1.level_id = l2.level_id
	         where l2.level_id = $LevelId
		       and lp1.pointtype_id in (3,5)
		 order  by lp1.levelpoint_order ASC";

      //  echo $sql;
	$Result = MySqlQuery($sql);
	$i=0;
	
	// ================ Цикл обработки контрольных точек этапа
	while ($Row = mysql_fetch_assoc($Result))
	{
		$i++;
		$NowLevelPointOrder = $Row['levelpoint_order'];
		$NowLevelPointId = $Row['levelpoint_id'];
		
		$sqltlp = " select tlp.teamlevelpoint_id
			 from TeamLevelPoints tlp
			 where tlp.team_id = $TeamId and tlp.levelpoint_id = $NowLevelPointId";

		$TlpExists = CSql::singleValue($sqltlp, 'teamlevelpoint_id');

		// Вставляем КП в список, если стоит 1
		if ((int)$LevelPointsArr[$i-1] == 1) {
		   if (empty($TlpExists)) {
	
			$sqltlp = " insert into TeamLevelPoints(device_id, levelpoint_id, team_id)
			         values(1, $NowLevelPointId, $TeamId)";

			$ResultTlp = MySqlQuery($sqltlp);
				
		    } else {
		       print("Уже есть точка $NowLevelPointId $TeamId\r\n");
		    }
		}
         }
	// Конец цикла по контрольным точкам этапа

	mysql_free_result($Result);
		    
       return;
     }
     //Конец генерации точек по строке 
     
     


 // Генерация точек для ММБ по этапам
     function GenerateLevelPointsForRaidFromLevels($raidid)
     {
	$Sql = "select tl.teamlevel_id
	        from TeamLevels tl
		     inner join Levels l
		     on tl.level_id = l.level_id
		     inner join Distances d
		     on l.distance_id = d.distance_id
	        where d.raid_id = $raidid
                     and COALESCE(tl.teamlevel_progress, 0) > 0
	       ";

	       
	$Result2 = MySqlQuery($Sql);  

        while ($Row2 = mysql_fetch_assoc($Result2))
        {
                   
          set_time_limit(10);
    	  $TeamLevelId2 = $Row2['teamlevel_id'];
	  	  
	  GenerateTeamLevelPointsFromTeamLevelString($TeamLevelId2);
        }
        mysql_free_result($Result2);
   
        set_time_limit(30);

        return;
     }
     //Конец генерации точек для ММБ 
   
     
     // функция пересчитывает длительнрость прохождения команды в точке
     function RecalcTeamLevelPointsDuration($raidid, $teamid)
     {


	// Для каждой точки с отсечкой времени (исключая точки с типом Старт) 
	//считается длительность нахождения на дистанции команды
	//  относительно предыдущей точки с отсечкой времени.
	 if (empty($teamid) and empty($raidid)) {     	 
	    return;
	 }

	// Не знаю, как лучше - можно по порядку точек, а можно по времени прохождения
	// по времени проще
	// 			 on lp1.levelpoint_order > a.levelpoint_order	

 /*
	$sql = " update  TeamLevelPoints tlp
                   inner join 
	           (select  tlp1.teamlevelpoint_id, 
		            timediff(tlp1.teamlevelpoint_datetime, MAX(a.teamlevelpoint_datetime)) as teamlevelpoint_duration
		    from TeamLevelPoints tlp1 
			 inner join Teams t1
			 on tlp1.team_id = t1.team_id
			 inner join Distances d1
			 on t1.distance_id = d1.distance_id
			 inner join LevelPoints lp1 
			 on tlp1.levelpoint_id = lp1.levelpoint_id
			 left outer join
			 (select tlp2.team_id, 
                                 lp2.levelpoint_order,	
                                 COALESCE(tlp2.teamlevelpoint_datetime, lp2.levelpoint_mindatetime) as teamlevelpoint_datetime			         
			  from  TeamLevelPoints tlp2 
				 inner join Teams t2
				 on tlp2.team_id = t2.team_id
				 inner join Distances d2
				 on t2.distance_id = d2.distance_id
				 inner join LevelPoints lp2 
				 on tlp2.levelpoint_id = lp2.levelpoint_id
		         ) a
			 on tlp1.teamlevelpoint_datetime > a.teamlevelpoint_datetime	
			    and lp1.levelpoint_order > a.levelpoint_order
			    and tlp1.team_id = a.team_id
			    and  a.teamlevelpoint_datetime > 0
	  	     where   tlp1.teamlevelpoint_datetime > 0
		             and  lp1.pointtype_id <> 1 
	      ";
	      
	      
	 if (!empty($teamid)) {     	 
	   $sql = $sql."and tlp1.team_id = ".$teamid;
	 } elseif (!empty($raidid)) {     	 
 	   $sql = $sql."and d1.raid_id = ".$raidid;
	 }				 


    	   $sql = $sql."	      		     
 		     group by tlp1.teamlevelpoint_id	    
		    ) b
		    on tlp.teamlevelpoint_id = b.teamlevelpoint_id   
		 set tlp.teamlevelpoint_duration =  b.teamlevelpoint_duration
	      ";
	
	*/
	
	$sql = " update  
			(select tlp1.teamlevelpoint_id,  tlp1.teamlevelpoint_datetime,  lp1.distance_id, tlp1.team_id,
				MAX(a.levelpoint_order) as maxorder
			 from TeamLevelPoints tlp1
			      inner join LevelPoints lp1
			      on tlp1.levelpoint_id = lp1.levelpoint_id
			      inner join Distances d1
			      on lp1.distance_id = d1.distance_id
			      left outer join
			      (select lp2.levelpoint_order, tlp2.team_id
			       from  TeamLevelPoints tlp2
			             inner join LevelPoints lp2
			             on tlp2.levelpoint_id = lp2.levelpoint_id
			             inner join Distances d2
			             on lp2.distance_id = d2.distance_id
			       where tlp2.teamlevelpoint_datetime > 0
		";			 
	 if (!empty($teamid)) {     	 
	   $sql = $sql." and tlp2.team_id = ".$teamid;
	 } elseif (!empty($raidid)) {     	 
 	   $sql = $sql." and d2.raid_id = ".$raidid;
	 }				 

	  $sql = $sql."				 
			      ) a
	 		      on tlp1.team_id = a.team_id 
			         and lp1.levelpoint_order > a.levelpoint_order
			 where tlp1.teamlevelpoint_datetime > 0
			       and  lp1.pointtype_id <> 1
		";			 
	 if (!empty($teamid)) {     	 
	   $sql = $sql." and tlp1.team_id = ".$teamid;
	 } elseif (!empty($raidid)) {     	 
 	   $sql = $sql." and d1.raid_id = ".$raidid;
	 }				 

	  $sql = $sql."				 
			 group by tlp1.teamlevelpoint_id
			 ) b  
 			inner join LevelPoints lp3
			on  lp3.levelpoint_order = b.maxorder
			    and lp3.distance_id = b.distance_id
			inner join TeamLevelPoints tlp3
			on lp3.levelpoint_id = tlp3.levelpoint_id
			   and tlp3.team_id = b.team_id  
			inner join TeamLevelPoints tlp4
			on tlp4.teamlevelpoint_id = b.teamlevelpoint_id
		  set  tlp4.teamlevelpoint_duration =   timediff(b.teamlevelpoint_datetime, tlp3.teamlevelpoint_datetime)
		";
    //  echo $sql;
      
       	 $rs = MySqlQuery($sql);
	
     }
     // конец функции пересчёта длительности прохождения команды до точки 

          

	// функция пересчитывает штрафы  команды в точке для КП, входящих в амнистию
	function RecalcTeamLevelPointsPenaltyWithDiscount($raidid, $teamid)
	{


	// Важно!  функция прибавляет текущее расчитанное значение к уже имеющемуся в точке,
	//  поэтому в там, где функция вызывается нужно не забыть обнулить штраф!

          /*
	  Для всех невзятых КП, которые входят в амнистию, суммируем штрафы в точку, которая является итоговой для заданного интервала
	  */

	 if (empty($teamid) and empty($raidid)) {     	 
	    return;
	 }

         
	 $sql = " update  TeamLevelPoints tlp
		   inner join 
	          	(select   SUM(lp.levelpoint_penalty) - MAX(lpd.levelpointdiscount_value) as penalty,
				  fulltlp.team_id,   lpd.levelpoint_id
			 from   (select  lp.levelpoint_id,
					 t.team_id 
				 from LevelPoints lp
				      join Teams t
				      inner join Distances d
				      on lp.distance_id = d.distance_id
				         and t.distance_id = lp.distance_id
				 where 
		";			 

	 if (!empty($teamid)) {     	 
	   $sql = $sql." t.team_id = ".$teamid;
	 } elseif (!empty($raidid)) {     	 
 	   $sql = $sql." d.raid_id = ".$raidid;
	 }				 

	  $sql = $sql."				 
				) fulltlp    
				inner join LevelPoints lp
				on  fulltlp.levelpoint_id = lp.levelpoint_id
			        left outer join TeamLevelPoints tlp
				on fulltlp.levelpoint_id = tlp.levelpoint_id
			            and fulltlp.team_id = tlp.team_id
			        inner join LevelPointDiscounts lpd
				on lp.levelpoint_order >= lpd.levelpointdiscount_start 
			           and lp.levelpoint_order <= lpd.levelpointdiscount_finish  
			           and lpd.distance_id = lp.distance_id
			where tlp.teamlevelpoint_id is NULL
			group by fulltlp.team_id, lpd.levelpoint_id
			having SUM(lp.levelpoint_penalty) >  MAX(lpd.levelpointdiscount_value)
			) a
		  on tlp.levelpoint_id = a.levelpoint_id   
		     and tlp.team_id = a.team_id
 		  set  teamlevelpoint_penalty = COALESCE(tlp.teamlevelpoint_penalty, 0) + COALESCE(a.penalty, 0)
		";
          //echo $sql;
	 
	  $rs = MySqlQuery($sql);

     }
     // Конец функции расчета штрафа для КП, входящих в амнистию		
                 
		
	// функция пересчитывает штрафы  команды в точке для КП, не входящих в амнистию
	function RecalcTeamLevelPointsPenaltyWithoutDiscount($raidid, $teamid)
	{

	// Важно!  функция прибавляет текущее расчитанное значение к уже имеющемуся в точке,
	//  поэтому в там, где функция вызывается нужно не забыть обнулить штраф!
	
	if (empty($teamid) and empty($raidid))
	{     	 
		return;
	}


	/*
	правильный вариант с штрафом в следующей точке со временем
	*/

	 $sql = " update TeamLevelPoints tlp0
			 inner join LevelPoints lp0
			 on tlp0.levelpoint_id = lp0.levelpoint_id
			 inner join 
			(
			select c.team_id, c.up, SUM(COALESCE(lp.levelpoint_penalty, 0)) as penalty
			from
				(select a.team_id, a.levelpoint_order as up, MAX(b.levelpoint_order) as down
				from
					(select t1.team_id, lp1.levelpoint_order
					from TeamLevelPoints tlp1
					     inner join LevelPoints lp1
					     on tlp1.levelpoint_id = lp1.levelpoint_id
					     inner join Teams t1
					     on t1.team_id = tlp1.team_id
					     inner join Distances d1
					     on t1.distance_id = d1.distance_id
					where ";			 

	 if (!empty($teamid)) {     	 
	   $sql = $sql." t1.team_id = ".$teamid;
	 } elseif (!empty($raidid)) {     	 
 	   $sql = $sql." d1.raid_id = ".$raidid;
	 }				 
				 
	  $sql = $sql."				 
					) a
					inner join 
					(select t2.team_id, lp2.levelpoint_order
					from TeamLevelPoints tlp2
					     inner join LevelPoints lp2
					     on tlp2.levelpoint_id = lp2.levelpoint_id
					     inner join Teams t2
					     on t2.team_id = tlp2.team_id
					     inner join Distances d2
					     on t2.distance_id = d2.distance_id
					where ";
	 if (!empty($teamid)) {     	 
	   $sql = $sql." t2.team_id = ".$teamid;
	 } elseif (!empty($raidid)) {     	 
 	   $sql = $sql." d2.raid_id = ".$raidid;
	 }				 
				 
	  $sql = $sql."				 
					) b
				on a.team_id = b.team_id
				   and a.levelpoint_order > b.levelpoint_order
				group by a.team_id, a.levelpoint_order
				having  a.levelpoint_order > MAX(b.levelpoint_order) + 1
				) c
				inner join Teams t
				on c.team_id = t.team_id
				inner join LevelPoints lp
				on t.distance_id = lp.distance_id
				   and lp.levelpoint_order < c.up
				   and  lp.levelpoint_order > c.down
				left outer join LevelPointDiscounts lpd
				on t.distance_id = lpd.distance_id 
				   and lp.levelpoint_order <= lpd.levelpointdiscount_finish
				   and  lp.levelpoint_order >= lpd.levelpointdiscount_start
				where lpd.levelpointdiscount_id is null
				group by c.team_id, c.up
			) d
			on tlp0.team_id = d.team_id
			   and lp0.levelpoint_order = d.up
		set tlp0.teamlevelpoint_penalty = COALESCE(tlp0.teamlevelpoint_penalty, 0) + COALESCE(d.penalty, 0)";


       //    echo $sql;
	 
	  $rs = MySqlQuery($sql);
	
     }
     // Конец функции расчета штрафа для КП без амнистий		
     
     
        // функция пересчитывает результат команды в точке 
     function RecalcTeamLevelPointsResult($raidid, $teamid)
     {

	 if (empty($teamid) and empty($raidid)) {     	 
	    return;
	 }


	 $sql = " update TeamLevelPoints tlp0
			 inner join LevelPoints lp0
			 on tlp0.levelpoint_id = lp0.levelpoint_id
			 inner join 
				(select a.team_id, a.levelpoint_order as up, 
					SEC_TO_TIME(SUM(COALESCE(b.teamlevelpoint_penalty, 0))*60 + SUM(TIME_TO_SEC(COALESCE(b.teamlevelpoint_duration, 0)))) as result
				from
					(select t1.team_id, lp1.levelpoint_order
					from TeamLevelPoints tlp1
					     inner join LevelPoints lp1
					     on tlp1.levelpoint_id = lp1.levelpoint_id
					     inner join Teams t1
					     on t1.team_id = tlp1.team_id
					     inner join Distances d1
					     on t1.distance_id = d1.distance_id
					where  lp1.pointtype_id <> 1 and ";			 

	 if (!empty($teamid)) {     	 
	   $sql = $sql." t1.team_id = ".$teamid;
	 } elseif (!empty($raidid)) {     	 
 	   $sql = $sql." d1.raid_id = ".$raidid;
	 }				 
				 
	  $sql = $sql."				 
					) a
					inner join 
					(select t2.team_id, lp2.levelpoint_order,
					        tlp2.teamlevelpoint_duration,
						tlp2.teamlevelpoint_penalty  
					from TeamLevelPoints tlp2
					     inner join LevelPoints lp2
					     on tlp2.levelpoint_id = lp2.levelpoint_id
					     inner join Teams t2
					     on t2.team_id = tlp2.team_id
					     inner join Distances d2
					     on t2.distance_id = d2.distance_id
					where ";
	 if (!empty($teamid)) {     	 
	   $sql = $sql." t2.team_id = ".$teamid;
	 } elseif (!empty($raidid)) {     	 
 	   $sql = $sql." d2.raid_id = ".$raidid;
	 }				 
				 
	  $sql = $sql."				 
					) b
				on a.team_id = b.team_id
				   and a.levelpoint_order >= b.levelpoint_order
				group by a.team_id, a.levelpoint_order
				) c
			on tlp0.team_id = c.team_id
			   and lp0.levelpoint_order = c.up
		set tlp0.teamlevelpoint_result = c.result ";

        //   echo $sql;
	 
	  $rs = MySqlQuery($sql);
	
     }
     // Конец функции расчета результата в точке		
 
 
 
	// функция проверяет ошибки
	function RecalcErrors($raidid, $teamid)
	{

		if (empty($teamid) and empty($raidid)) 
		{
			return;
		}


		// Устанавливаем превышение КВ
		$sql = " update  TeamLevelPoints tlp
							inner join LevelPoints lp
							on tlp.levelpoint_id = lp.levelpoint_id
							
							inner join Teams t
							on t.team_id = tlp.team_id
							inner join Distances d
							on t.distance_id = d.distance_id
				set  tlp.error_id = 4
				where  tlp.teamlevelpoint_datetime > lp.levelpoint_maxdatetime 
		";			 

		if (!empty($teamid)) 
		{ 
			$sql = $sql." and t.team_id = ".$teamid;
		} elseif (!empty($raidid)) {     	 
			$sql = $sql." and d.raid_id = ".$raidid;
		}				 
				 
	//	echo $sql;
		$rs = MySqlQuery($sql);




	// Устанавливаем невзятие обязательных КП
	 // Высчитываем число обязательных точек
	 if (!empty($teamid)) {     	 

		 $sql = "   select count(*) as result
			    from LevelPoints lp
				   inner join Teams t
				   on lp.distance_id = t.distance_id
			    where  t.team_id = $teamid
                                    and lp.pointtype_id = 3 ";

	 } elseif (!empty($raidid)) {     	 

		 $sql = "   select count(*) as result
			    from LevelPoints lp
				   inner join Distances d
				   on lp.distance_id = d.distance_id
			    where  d.raid_id = $raidid
                                    and lp.pointtype_id = 3 ";
	 }				 
				 
	 $Result = MySqlQuery($sql);
	 $Row = mysql_fetch_assoc($Result);
	 $ObligatoryCount = $Row['result'];
	 mysql_free_result($Result);

						 

	// По количеству можно повесить только на последнюю точку, а првильнее - на следующую по порядку
	
	// Тут нужно проверить взятие обязатеьных КП и поставить ошибку на следующую точку	
	//	запрос похож на расчет для кп вне амнистии
	
	/*
	 $sql = " update  Teams t
                  inner join

					(select a.team_id, a.levelpoint_id, a.levelpoint_order
					from
						(select t.team_id, lp.levelpoint_id, lp.levelpoint_order
						from Teams t
								inner join Distances d
								on  t.distance_id = d.distance_id
								join LevelPoints lp
						where lp.pointtype_id = 3 
			";			 

			if (!empty($teamid)) 
			{     	 
				$sql = $sql."  t.team_id = ".$teamid;
			} elseif (!empty($raidid)) {     	 
				$sql = $sql."  d.raid_id = ".$raidid;
			}				 
						
	  $sql = $sql."				 
						) a
						left outer join TeamLevelPoints tlp
						on tlp.levelpoint_id = a.levelpoint_id
							and tlp.team_id  = a.team_id
					where tlp.teamlevelpoint_id is null
					) b	
					left outer join TeamLevelPoints tlp2
						on tlp.levelpoint_id = a.levelpoint_id
							and tlp.team_id  = a.team_id

		            on tlp.levelpoint_id = lp.levelpoint_id
			       and lp.pointtype_id = 3 
			    inner join Teams t
			    on t.team_id = tlp.team_id
			    inner join Distances d
			    on t.distance_id = d.distance_id
                       where  
		";			 

	 if (!empty($teamid)) {     	 
	   $sql = $sql."  t.team_id = ".$teamid;
	 } elseif (!empty($raidid)) {     	 
 	   $sql = $sql."  d.raid_id = ".$raidid;
	 }				 
				 
	  $sql = $sql."				 
                       group by t.team_id
		       having  count(lp.levelpoint_id) < ".$ObligatoryCount." 
                      ) a
		  on t.team_id = a.team_id    
		  set  t.team_progressdetail = 2
		  ";

		// echo $sql;
		$rs = MySqlQuery($sql);

*/
	
	}
	// Конец функции проверки ошибок
 
 
 
     
     // функция пересчитывает результат команды по данным в точках
     function RecalcTeamResultFromTeamLevelPoints($raidid, $teamid)
     {


	if (empty($teamid) and empty($raidid)) {
	    return;
	}

	$teamRaidCondition = (!empty($teamid)) ? " t.team_id = $teamid" : "d.raid_id = $raidid";

         // Обнуляем данные расчета
	$sql = " update  TeamLevelPoints tlp
		         join Teams t
			 on tlp.team_id = t.team_id
  		         inner join Distances d
			 on t.distance_id = d.distance_id
		  set  teamlevelpoint_penalty = NULL,  
		       teamlevelpoint_duration = NULL,
		       t.team_maxlevelpointorderdone = NULL,
		       t.team_minlevelpointorderwitherror = NULL,
		       t.team_comment = NULL			 
 		  where $teamRaidCondition" ;
				 
	$rs = MySqlQuery($sql);

	
	RecalcTeamLevelPointsDuration($raidid, $teamid);
	RecalcTeamLevelPointsPenaltyWithDiscount($raidid, $teamid);
	RecalcTeamLevelPointsPenaltyWithoutDiscount($raidid, $teamid);
	RecalcTeamLevelPointsResult($raidid, $teamid);

	// Находим ключ ММБ, если указана только команда
	if (empty($raidid)) 
	{
		$sql = "select d.raid_id
				from Teams t
					inner join Distances d
					on t.distance_id = d.distance_id
				where t.team_id = $teamid
				LIMIT 0,1";

		$raidid = CSql::singleValue($sql, 'raid_id');
	}

	// Убрал расчёт ошибок
	//RecalcErrors($raidid, $teamid);


	// $raidid мог измениться
	$teamRaidCondition = (!empty($teamid)) ? "t.team_id = $teamid" : "d.raid_id = $raidid";


     // Результат команды - это результат в максимальной точке
     // Перевод в секунды нужен для корректной работы MAX
	$sql = " update  Teams t
		   inner join 
	          	(select tlp.team_id, 
			        MAX(COALESCE(lp.levelpoint_order, 0)) as progress,
					MAX(t.distance_id) as distance_id,
			        MAX(TIME_TO_SEC(COALESCE(tlp.teamlevelpoint_result, 0))) as secresult
			 from TeamLevelPoints tlp
			      inner join Teams t
			      on tlp.team_id = t.team_id
			      inner join Distances d
			      on t.distance_id = d.distance_id
			      inner join LevelPoints lp
			      on tlp.levelpoint_id = lp.levelpoint_id

			 where  $teamRaidCondition

                         group by tlp.team_id
			) a
		  on t.team_id = a.team_id
		  inner join 
			(select  lp.distance_id,
			         MAX(lp.levelpoint_order) as maxlporder
			 from LevelPoints lp
			 group by lp.distance_id 
			) b
		  on a.distance_id = b.distance_id
  	          set  team_result =  CASE WHEN b.maxlporder = COALESCE(a.progress, 0) THEN SEC_TO_TIME(COALESCE(a.secresult, 0)) ELSE NULL END
				, team_maxlevelpointorderdone = COALESCE(a.progress, 0) ";

     //     echo $sql;
	 
	$rs = MySqlQuery($sql);

     // теперь можно посчитать рейтинг
	RecalcTeamUsersRank($raidid);
	

     //
     // Находим минимальную точку с ошибкой
	$sql = " update  Teams t
		   inner join 
	          	(select tlp.team_id, 
			        MIN(COALESCE(lp.levelpoint_order, 0)) as error
			 from TeamLevelPoints tlp
			      inner join Teams t
			      on tlp.team_id = t.team_id
			      inner join Distances d
			      on t.distance_id = d.distance_id
			      inner join LevelPoints lp
			      on tlp.levelpoint_id = lp.levelpoint_id
			 where  COALESCE(tlp.error_id, 0) <> 0
			        and $teamRaidCondition
                         group by tlp.team_id
			) a
		  on t.team_id = a.team_id
          set  team_minlevelpointorderwitherror = COALESCE(a.error, 0)";

	//     echo $sql;
	 
	$rs = MySqlQuery($sql);


	 // Находим невзятые КП
	 $sql = " update  Teams t
		   inner join
	          	(select t.team_id, GROUP_CONCAT(lp.levelpoint_name ORDER BY lp.levelpoint_order, ' ') as skippedlevelpoint
						from  Teams t
								inner join  Distances d
								on t.distance_id = d.distance_id
								join LevelPoints lp
 								on t.distance_id = lp.distance_id
									and  COALESCE(t.team_maxlevelpointorderdone, 0) >= lp.levelpoint_order
								left outer join TeamLevelPoints tlp
								on lp.levelpoint_id = tlp.levelpoint_id
									and t.team_id = tlp.team_id
					 	where 	tlp.levelpoint_id is NULL
								and d.distance_hide = 0 and t.team_hide = 0
								and $teamRaidCondition
						group by t.team_id
				) a
		  on t.team_id = a.team_id
          set  team_skippedlevelpoint = COALESCE(a.skippedlevelpoint, '')";

		 //     echo $sql;

		 $rs = MySqlQuery($sql);



	// Обновляем комментарий у команды, куда включаем и ошибки  
	$sql = " update  Teams t
                  inner join
                      (select tlp.team_id 
						,group_concat(COALESCE(teamlevelpoint_comment, '')) as team_comment
		       from TeamLevelPoints tlp
				left outer join Errors err
				on tlp.error_id = err.error_id
			    inner join Teams t
			    on t.team_id = tlp.team_id
			    inner join Distances d
			    on t.distance_id = d.distance_id
                       where  COALESCE(tlp.teamlevelpoint_comment, '') <> ''
                                and $teamRaidCondition

                       group by tlp.team_id
                      ) a
		  on t.team_id = a.team_id    
		  set  t.team_comment = a.team_comment";

        //   echo $sql;
	$rs = MySqlQuery($sql);

	//Теперь ошибки
	$sql = " update  Teams t
                  inner join
                      (select tlp.team_id 
						,group_concat(COALESCE(error_name, '')) as team_error
		       from TeamLevelPoints tlp
				left outer join Errors err
				on tlp.error_id = err.error_id
			    inner join Teams t
			    on t.team_id = tlp.team_id
			    inner join Distances d
			    on t.distance_id = d.distance_id
                       where  COALESCE(tlp.error_id, 0) <> 0
                                and $teamRaidCondition

                       group by tlp.team_id
                      ) a
		  on t.team_id = a.team_id    
		  set  t.team_comment = CASE WHEN a.team_error <> '' THEN CONCAT('Ошибки: ', a.team_error, '; ',  COALESCE(t.team_comment, ''))  ELSE t.team_comment END";

        //   echo $sql;
	$rs = MySqlQuery($sql);
     }
     // Конец функции расчета штрафа для КП без амнистий		


	function mmb_validate($var, $key, $default = "")
	{
		return isset($var[$key]) ? $var[$key] : $default;
	}

	// returns value on success, false otherwise
	function mmb_validateInt($var, $key, $default = 0)
	{
		$val = mmb_validate($var, $key, $default);
		return is_numeric($val) ? $val : false;
	}

	function mmb_isOn($var, $key)
	{
		return (mmb_validate($var, $key, '') == 'on') ? 1 : 0;
	}

class CMmbUI
{
	public static function toHtml($str)
	{
		$search = array("<", ">", "\"", "'");
		$replace = array("&lt;", "&gt;", "&quot;", "&apos;");

		return str_replace($search, $replace, str_replace("&", "&amp;", (string) $str));
	}

	public static function placeholder($defaultValue)
	{
		$defVal = str_replace("&apos;", "\\&apos;", self::toHtml($defaultValue)); // эскейпимся от апострофов в js
		return " onclick=\"javascript: _onClick(this, '$defVal');\" onblur=\"javascript: _onBlur(this, '$defVal');\" ";
	}
}

class CMmbLogger
{
	protected $records = array();

	function __construct()
	{

	}

	public function AddRecord($record)
	{
		if (!empty($record))
			$this->records[] = $record;
	}

	public function AddTime($text, $time)
	{
		$this->AddRecord($text . ' ' . round($time, 5));
	}

	public function AddInterval($text, $stTime)
	{
		$en = microtime(true);
		$this->AddRecord("$text: " . round($en - $stTime, 5));
		return $en;
	}

	public function GetText($asHtml = true)
	{
		if (!$asHtml)
			return implode("\r\n", $this->records);

		$res = '';
		foreach ($this->records as $rec)
			$res .= CMmbUI::toHtml($rec) . "<br/>";

		return $res;
	}
}

?>
