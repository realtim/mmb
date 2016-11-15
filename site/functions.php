<?php
// +++++++++++ Библиотека функций +++++++++++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

include("rights.php");

class CMmb
{
	const SessionTimeout = 20; // minutes
	const CookieName = "mmb_session";

	public static function setSessionCookie($sessionId)
	{
		setcookie(CMmb::CookieName, $sessionId, time() + 60 * CMmb::SessionTimeout, '/', null, false, true);
	}

	public static function clearSessionCookie()
	{
		setcookie(CMmb::CookieName, "", time() - 24 * 3600, '/', null, false, true);    // a day ago
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
 	// вызов  MySqlQuery('...',&$ConnectionId, ...);

  	// echo $ConnectionId;

	$ConnectionId = CSql::getConnection();

	$t1 = microtime(true);
	$rs = mysql_query($SqlString, $ConnectionId);
	CMmbLogger::addInterval('query', $t1);


	if (!$rs)
	{
		$err = mysql_error();
		CMmbLogger::e('MySqlQuery', "sql error: '$err' \r\non query: '$SqlString'");
		echo $err;

		CSql::closeConnection();
		die();
		return -1;
	}

	// Если был insert - возвращаем последний id
	if (strpos($SqlString, 'insert') !== false)
	{
		$rs = mysql_insert_id($ConnectionId);
	//  echo ' NewId '.$rs;
	}

	//CSql::closeConnection(); // try not closing, use global

	return $rs;
}


class CSql {
	protected static $connection = null;

	public static function getConnection()
	{
		if (self::$connection === null)
			self::$connection = self::createConnection();

		return self::$connection;
	}

	// returns: well-quoted and escaped string
	public static function quote($str)
	{
		return "'" . mysql_real_escape_string($str) . "'";
	}

	// закрывает переданное соединение. При вызове без параметра -- закрывает общее.
	public static function closeConnection($conn = null)
	{
		if ($conn !== null)
		{
			mysql_close($conn);
			return;	
		}
		
		if (self::$connection !== null)
			mysql_close(self::$connection);
		self::$connection = null;
	}
	
	// returns: sql connection on success, die() on error
	// вызывать только если вам действительно нужен личный коннекшен. Например, такой нужен логгеру
	// логировать ошибки мимо sql
	public static function createConnection()
	{
		$t1 = microtime(true);
		// Данные берём из settings
		include("settings.php");

		$connection = mysql_connect($ServerName, $WebUserName, $WebUserPassword);

		// Ошибка соединения
		if ($connection <= 0)
			self::dieOnSqlError(null, 'createConnection', 'mysql_connect: ', mysql_error());

		//  15/05/2015  Убрал установку, т.к. сейчас в mysql всё правильно, а зона GMT +3
		//  устанавливаем временную зону
		//		 mysql_query('set time_zone = \'+4:00\'', $ConnectionId);
		//  устанавливаем кодировку для взаимодействия

		mysql_query('set names \'utf8\'', $connection);

		// Выбираем БД ММБ
		//		echo $DBName;
		$rs = mysql_select_db($DBName, $connection);

		if (!$rs)
		{
			$err = mysql_error();
			self::closeConnection($connection);
			self::dieOnSqlError(null, 'createConnection', "mysql_select_db '$DBName'", $err);
		}
	//	CMmbLogger::addInterval('getConnection', $t1);
		
		return $connection;
	}

	public static function dieOnSqlError($user, $op, $message, $err)
	{
		CMmbLogger::fatal($user, $op, $message . " error: $err");
		echo $err;
		die();
	}

	public static function singleRow($query)
	{
		$result = MySqlQuery($query);
		$row = mysql_fetch_assoc($result);
		mysql_free_result($result);
		return $row;
	}

	public static function singleValue($query, $key, $strict = true)
	{
		$result = MySqlQuery($query);
		$row = mysql_fetch_assoc($result);
		mysql_free_result($result);

		if (!isset($row[$key]) && $strict == true)
			CMmbLogger::w('singleValue', "Field '$key' doesn't exist, query:\n" . trim($query));
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

	// пустой $raidId ищет последний доступный файл такого типа
	public static function raidFileName($raidId, $fileType, $justVisible)
	{
		$condition = $justVisible ? "raidfile_hide = 0" : "true";

		$raidCond = empty($raidId) ? 'true' :  "raid_id = $raidId";
		$raidOrder = empty($raidId) ? 'raid_id desc, ': '';

		$sql = "select raidfile_name
	                        from RaidFiles
	                        where $raidCond and filetype_id = $fileType and $condition
	     			order by $raidOrder raidfile_id desc";

		return trim(self::singleValue($sql, 'raidfile_name', false));
	}

	// types:
	// 1 - ссылка на положение
	// 2 - ссылка на логотип
	// 8 - пользовательское соглашение
	// 10 - информация о старте
	public static function raidFileLink($raidId, $fileType, $justVisible = false)
	{
		global $MyStoreFileLink, $MyStoreHttpLink;

		$name = self::raidFileName($raidId, $fileType, $justVisible);
		if (empty($name))
			return '';

		if (!file_exists($MyStoreFileLink.$name))
		{
			CMmbLogger::e('raidFileLink', "File '$name' doesn't exist");
			return '';
		}

		return trim($MyStoreHttpLink).$name;
	}


	// date -- строка ddmm
	// time -- строка hhmmss
	public static function timeString($year, $date, $time, $noSeconds = true)
	{
		$tDate = trim($date);
		$tTime = trim($time);
		
		// Если передали без секунд, то устанавливаем принудительно флаг
		if (!$noSeconds && strlen($tTime) <> 6)
		{
		   $noSeconds = true;
		}
		$seconds = $noSeconds ? '00' : substr($tTime, -2);

		return  "'$year-".substr($tDate, -2)."-".substr($tDate, 0, 2)." ".substr($tTime, 0, 2).":".substr($tTime, 2, 2).":$seconds'";
	}

	public static function timeString2($arr, $yearKey, $dateKey, $timeKey, $noSeconds = true)
	{
		$year = $arr[$yearKey];
		$date = $arr[$dateKey];
		$time = $arr[$timeKey];

		// Если день и время пустые, то и год пустой считаем
		if (((int)$date) == 0 and ((int)$time) == 0)
			return "'0000-00-00 00:00:00'";

		return self::timeString($year, $date, $time, $noSeconds);
	}

	// 21.03.2016 Добавляю сервисные функции в этот класс, хотя может нужно  потом разбивать на  отдельные классы
	public static function userId($sessionId)
	{
		$sql = "select user_id from Sessions where session_id = " . CSql::quote($sessionId);	// см комментарий про эскейпинг к CMmbAuth::getUserId

		return self::singleValue($sql, 'user_id', false);
	}

	// 21.03.2016 возвращает teamId команды для заданного пользователя и ММБ
	public static function userTeamId($userId, $raidId)
	{

	 	//$userId = mmb_validate($userId, 0, 0);
	 	//$raidId = mmb_validate($raidId, 0, 0);
	 	
	 	$userId  = !isset($userId) ? 0 : $userId;
	 	$raidId  = !isset($raidId) ? 0 : $raidId;

		$sql = "select tu.team_id
			from TeamUsers tu
				inner join Teams t on tu.team_id = t.team_id
				inner join Distances d on t.distance_id = d.distance_id
			where tu.teamuser_hide = 0 and t.team_hide = 0 and d.raid_id = $raidId and tu.user_id = $userId";

	//	echo $sql; 
		return self::singleValue($sql, 'team_id', false);
	}


	// 21.03.2016 возвращает userUnionLogId записи о попытке объедиенения с другим пользователем  для заданного пользователя
	// только для ситуации запроса или успешного объединения (если отклонено или отмена, то функция не вернёт id)
	public static function userUnionLogId($userId)
	{
		$sql = "select uul.userunionlog_id
			from UserUnionLogs uul
			where uul.union_status in (1,2) and (uul.user_id = $userId or uul.user_parentid = $userId)";

		return self::singleValue($sql, 'userunionlog_id', false);
	}

	// 21.03.2016 возвращает teamId команды для заданного пользователя и ММБ
	public static function teamOutOfRange($teamId)
	{
		$sql = "select COALESCE(t.team_outofrange, 0) as team_outofrange
			from  Teams t
			where t.team_hide = 0 and t.team_id = $teamId";

		return self::singleValue($sql, 'team_outofrange');
	}



	// 20/03/2016 ДОбавил фильтрацию точек с нулеым или NULL  мимнимальным и максимальным временем точки, так как для обычных 
        // КП это время решили не вносить
        // 21/11/2013  Добавил RaidStage (финиш закрыт, но нельзя показывать результаты и сместил 6 на 7)
        // 30.10.2013 Для трёхдневного ММБ  изменил INTERVAL 12 на INTERVAL 24  
	public static function raidStage($raidId)
	{

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
		(select count(*) from LevelPoints lp
			inner join Distances d on lp.distance_id = d.distance_id
			where (d.raid_id = r.raid_id) and (NOW() >= DATE_SUB(lp.levelpoint_mindatetime, INTERVAL COALESCE(r.raid_readonlyhoursbeforestart, 8) HOUR))
				and  COALESCE(lp.levelpoint_mindatetime, 0) > 0			
		)
		as cantdelete,
		(select count(*) from LevelPoints lp
			inner join Distances d on lp.distance_id = d.distance_id
			where (d.raid_id = r.raid_id) and (NOW() >= lp.levelpoint_mindatetime)
				and  COALESCE(lp.levelpoint_mindatetime, 0) > 0			

		)
		as started,
		(select count(*) from LevelPoints lp
			inner join Distances d on lp.distance_id = d.distance_id
			where (d.raid_id = r.raid_id) and (NOW() < lp.levelpoint_maxdatetime)
				and  COALESCE(lp.levelpoint_maxdatetime, 0) > 0			
		)
		as notfinished,
		CASE
			WHEN (r.raid_closedate IS NULL) OR (r.raid_closedate >= DATE(NOW())) THEN 0
			ELSE 1
		END as closed,
		COALESCE(r.raid_noshowresult, 0) as noshowresult
		from Raids r where r.raid_id = $raidId";

		$Row = self::singleRow($sql);

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
		// Конец разбора стадии ММБ

		return $RaidStage;
	}
	// Конец функции raidStageId


	// 21.03.2016 возвращает признак модератора
	public static function userModerator($userId, $raidId)
	{
		$sql = "select count(*) as moderator
			from  RaidModerators rm
			where rm.raidmoderator_hide = 0 and rm.raid_id = $raidId and rm.user_id = $userId ";

		return self::singleValue($sql, 'moderator');
	}

	// 21.03.2016 возвращает признак администратора
	public static function userAdmin($userId)
	{
		$sql = "select count(*) as admin
			from  Users u
			where u.user_id = $userId and user_admin = 1 ";

		return self::singleValue($sql, 'admin');
	}

	// 21.05.2016 возвращает число участников в команде
	public static function teamUserCount($teamId)
	{
		$sql = "select count(*) as tucount
			from TeamUsers tu
			where tu.teamuser_hide = 0 and tu.team_id = $teamId";

	//	echo $sql; 
		return self::singleValue($sql, 'tucount', false);
	}

	// 09.06.2016 возвращает число доступных приглашений на текущий момент
	public static function  availableInvitationsCount($raidId)
	{
		$sql = "select count(*) as teamsinrangecount
			from Teams t
				inner join Distances d
				on t.distance_id = d.distance_id
			where t.team_hide = 0
				and t.team_outofrange = 0 
				and d.distance_hide = 0 
				and d.raid_id = $raidId";

	//	echo $sql; 
		$teamsInRangeCount =  self::singleValue($sql, 'teamsinrangecount', false);

		$sql = "select COALESCE(r.raid_teamslimit) as teamslimit
			from Raids r
			where r.raid_id = $raidId";
		
		$raidTeamsLimit =  self::singleValue($sql, 'teamslimit', false);

		// считаем число неиспользованных приглашений, которые активны
		// если команда удалена, то приглашение можно активироватьснова
		$sql = "select count(*) as activeinvitationscount
			from Invitations inv
				inner join InvitationDeliveries idev
				on inv.invitationdelivery_id = idev.invitationdelivery_id
				left outer join Teams t
				on inv.invitation_id = t.invitation_id
				   and t.team_hide = 0
			where idev.raid_id = $raidId
				and inv.invitation_begindt <= NOW()
				and inv.invitation_enddt >= NOW()
				and t.team_id is null
				";
	//	echo $sql; 
		$activeInvitationsCount =  self::singleValue($sql, 'activeinvitationscount', false);

		return ($raidTeamsLimit - $teamsInRangeCount - $activeInvitationsCount);
	}


	// 09.06.2016 возвращает число участий
	public static function userRaidsCount($userId)
	{
		$sql = "select count(*) as tucount
			from TeamUsers tu
				inner join Teams t
				on tu.team_id = t.team_id
			where t.team_hide = 0 and tu.teamuser_hide = 0 and tu.user_id = $userId";

		return self::singleValue($sql, 'tucount', false);
	}

	// 15.09.2016 возвращает дедлайн для активирования приглашений
	public static function invitationDeadline($RaidId)
	{
		// получаем дату окончания регистрации
		$sql = "select ADDTIME(r.raid_registrationenddate, '23:59:59') as regenddt, NOW() as currentdt
				from Raids r
				where r.raid_id = $RaidId
					and r.raid_registrationenddate is not null";
		$regenddt = self::singleValue($sql, 'regenddt', false);
		$currentdt = self::singleValue($sql, 'currentdt', false);
		// если она наступила, то проверять дедлайн для приглашений нет смысла
		if (!empty($regenddt) && ($regenddt < $currentdt)) return "REG_END";

		// смотрим максимальное время завершения действия приглашений по рейтингу, выданных на указанный ммб
		$sql = "select MAX(inv.invitation_enddt) as maxinvdt, NOW() as currentdt
				from InvitationDeliveries invd
					inner join Invitations inv
					on invd.invitationdelivery_id = inv.invitationdelivery_id
				where invd.raid_id = $RaidId
					and invd.invitationdelivery_type = 1";
		$maxinvdt = self::singleValue($sql, 'maxinvdt', false);
		$currentdt = self::singleValue($sql, 'currentdt', false);
		if (empty($maxinvdt)) return "NO_RATING";		// приглашения по рейтингу еще не выдавались
		if ($maxinvdt > $currentdt) return $maxinvdt;		// приглашения по рейтингу выданы и еще активны

		// дедлайн для приглашений по рейтингу наступил, проверяем, проводилась ли лотерея
		$sql = "select MAX(invitationdelivery_dt) as lotterydt
				from InvitationDeliveries invd
				where invd.raid_id = $RaidId
					and invd.invitationdelivery_type = 2";
		$lotterydt = self::singleValue($sql, 'lotterydt', false);
		if (empty($lotterydt)) return "NO_LOTTERY";		// приглашения по итогам лотереи еще не выдавались
		else return "LOT_END";					// приглашения по итогам лотереи выданы
	}
}
// Конец описания класса cSql

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

  // 21.03.2016 Обновляем данные сессии
  function UpdateSession($SessionId) {

      if (empty($SessionId))
      {
        return 0;
      } 

      // Закрываем все сессии, которые неактивны 20 минут
      CloseInactiveSessions(CMmb::SessionTimeout);

      // Очищаем таблицу
      ClearSessions();
  
      $Result = MySqlQuery("update  Sessions set session_updatetime = now()
			    where session_status = 0 and session_id = '$SessionId'");

      CMmb::setSessionCookie($SessionId);

      return;

    }


class CMmbAuth {
	const MinPasswordLen = 8;

	public static function setAutoPassword($userId)
	{
		// пишем в базу пароль и время отправки письма с паролем
		//  обнуляем сессию для восстановления и её время
		$password = self::generatePassword();

		$sql = "update   Users  set user_password = ". self::hashAndQuote($password).",
                                     user_sendnewpassworddt = now(),
                         user_sessionfornewpassword = null,
                         user_sendnewpasswordrequestdt = null
                 where user_id = $userId";
		//   echo $sql;
		$rs = MySqlQuery($sql);

		return $password;
	}

	/// returns: description on error, true on success
	public static function setPassword($userId, $pwd)
	{
		$err = self::isValidPassword($pwd);
		if ($err !== true)
			return $err;

		$sql = "update Users set user_password = ".self::hashAndQuote(trim($pwd))."
			  		where user_id = $userId";

		$rs = MySqlQuery($sql);
		return true;
	}

	/// returns: description on error, true on success
	public static function isValidPassword($pwd)
	{
		$err = array();

		if ($pwd === null || !is_string($pwd) || strlen($pwd) < self::MinPasswordLen)
			$err[] = "пароль должен быть не короче "  . self::MinPasswordLen . " символов";

		// todo добавить проверку на  low letter, caps letters, non-letter


		return count($err) == 0 ? true : implode(", ", $err);
	}

	// returns: userId on success, 0 on error
	public static function getUserId($login, $pwd)
	{
		$qLogin = CSql::quote(trim($login));		// а откуда приходят логин и пароль? они уже заэскейплены или нет?
		$sql = "select user_id, user_password from  Users
                	where user_hide = 0 and trim(user_email) = $qLogin";
		$row = CSql::singleRow($sql);

		if (!isset($row['user_id']))
			return 0;

		// return password_verify($pwd, $row['user_password']) ? $row['user_id'] : 0; // todo uncomment on migration

		if ($row['user_password'] === md5(trim($pwd)))
			return $row['user_id'];

		return 0;
	}

	// Гененрируем пароль
	private static function generatePassword()
	{
		$PasswordLength = self::MinPasswordLen;
		// Количество символов в пароле.$PasswordLength
		// todo проверка обязательности low letter, caps letter, non-letter

		$CharsArr = "qazxswnhyujmedcvfrtgbkiolp1234567890QAZCVFXSWEDRTGBNHYUJMKIOLP";

		// Определяем количество символов в $chars
		$CharsArrLen = StrLen($CharsArr) - 1;

		// Определяем пустую переменную, в которую и будем записывать символы.
		$Password = '';

		// Создаём пароль.
		while ($PasswordLength--) {
			$Password .= $CharsArr[rand(0, $CharsArrLen)];		// todo think on random_int
		}

		// echo $Password;
		return $Password;
	}

	private static function hashAndQuote($pwd)
	{
		$hash = md5($pwd);
		//$hash = password_hash($hash);  // todo uncomment on migration

		return CSql::quote($hash);
	}
}



  // Отсылаем пароль
    function SendMail($Email, $Message, $ToName='', $Subject='Информация с сайта ММБ') {
   //
   
    // 20.01.2012 Заменил штатную функцию на более удобную  send_mime_mail (см. ниже)
    return send_mime_mail('mmbsite',
 		   'mmb@progressor.ru',
		    $ToName,
		    $Email,
		    'UTF-8',  // кодировка, в которой находятся передаваемые строки
		    'UTF-8', // кодировка, в которой будет отправлено письмо
		    $Subject,
		    $Message."\r\n".'Используйте для вопросов адрес mmbsite@googlegroups.com'."\r\n".'Ответ на это письмо будет проигнорирован.'."\r\n");
    }


    function SendMailForAll($raidId, $msgSubject, $msgText, $sendingType)
    {
	global $DebugMode;
	// $debagCond;
	

	$SessionId = mmb_validate($_COOKIE, CMmb::CookieName, '');
	$UserId = (int) CSql::userId($SessionId);
	$Admin = (int) CSql::userAdmin($UserId);

	
	// тут нужны проверки
	if ($raidId <= 0)
	{
		CMmb::setShortResult('Марш-бросок не найден', '');
		return(-1);
	}

	if (empty($msgSubject) or trim($msgSubject) == 'Тема рассылки')
	{
		CMmb::setShortResult('Укажите тему сообщения', '');
                return(-1); 
	}

	if (empty($msgText) or trim($msgText) == 'Текст сообщения')
	{
		CMmb::setShortResult('Укажите текст сообщения', '');
                return(-1); 
	}
     
	if (empty($sendingType) or $sendingType == 0)
	{
		CMmb::setShortResult('Укажите тип рассылки', '');
                return(-1); 
	}


//	echo $raidId, $msgSubject, $msgText, $sendingType, $Admin;

	// рассылать всем может только администратор
	if (!$Admin) return (-1);


	if (isset($DebugMode) and ($DebugMode == 1))
	{
		$debugCond = " and u.user_admin = 1 ";
	} else {
		$debugCond = "";
	}
	
	
        if ($sendingType == 1)
        {

		// информационное сообщение
	     $sql = "  select tu.user_id, u.user_name, u.user_email 
             		from TeamUsers tu
             			inner join Teams t
             			on tu.team_id = t.team_id
             			inner join Users u
             			on tu.user_id = u.user_id
             			inner join Distances d
             			on t.distance_id = d.distance_id
             		where d.raid_id = $raidId
             			and t.team_hide = 0
             			and tu.teamuser_hide = 0
             			$debugCond
             			and u.user_allowsendorgmessages = 1
             		order by tu.user_id ";
	    
        }
        elseif ($sendingType == 2)
        {

		// экстренная рассылка
	     $sql = "  select tu.user_id, u.user_name, u.user_email 
             		from TeamUsers tu
             			inner join Teams t
             			on tu.team_id = t.team_id
             			inner join Users u
             			on tu.user_id = u.user_id
             			inner join Distances d
             			on t.distance_id = d.distance_id
             		where d.raid_id = $raidId
             			and t.team_hide = 0
             			and tu.teamuser_hide = 0
             			$debugCond
             		order by tu.user_id ";


        }
        elseif ($sendingType == 3)
        {

		// информаия об открытии регистрации или общая рассылка по всем пользователям вне ММБ
	     $sql = "  select u.user_id, u.user_name, u.user_email 
             		from  Users u
             		where u.user_hide = 0
            		      $debugCond
             		      and u.user_allowsendorgmessages = 1
             		order by u.user_id ";

        }
        else
        {
	       $sql = "";
        }
             
 	//echo $sql;
		
	$UserResult = MySqlQuery($sql);
		
	while ($UserRow = mysql_fetch_assoc($UserResult))
	{
		        $UserEmail = $UserRow['user_email'];
		        $UserName = $UserRow['user_name'];
		
	
			$Msg = '';
		        $pTextArr = explode('\r\n', $msgText); 
		       	foreach ($pTextArr as $NowString) {
			   $Msg .= $NowString."\r\n";
			}
		       $Msg .= " \r\n";

			// добавляем комментарий	
			if ($sendingType == 1 or $sendingType == 3)
			{
			   $Msg .= "\r\n Если Вы не хотите получать информационные письма, то снимите соответствующую отметку в карточке пользователя на сайте ММБ \r\n \r\n";
			}
		
		        //echo $sendingType." ".
		        // Отправляем письмо
			SendMail(trim($UserEmail),  $Msg, $UserName,  $msgSubject);
	}
  	mysql_free_result($UserResult);
  	
  	return (1);
 
    }
    // конец рассылки сообщений по всем пользователям
	
	



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
	// Если ($TeamId == 0) && ($RaidId != 0), то сделать $TeamId равным команде пользователя, если он участвует в RaidId
	// !! реализовать алгоритм !!

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

	// 2015-10-24 Отключаем проверку на старые ммб - всё уже в базе
	// Определяем, проводился ли марш-бросок до 2012 года
       
	//$sql = "select CASE WHEN raid_registrationenddate is not null and YEAR(raid_registrationenddate) <= 2011
	//		THEN 1
	//		ELSE 0
	//	END as oldmmb
	//	from Raids where raid_id = $RaidId";
	//
	//$OldMmb = CSql::singleValue($sql, 'oldmmb');
       
       // 20/03/2016 ДОбавил фильтрацию точек с нулеым или NULL  мимнимальным и максимальным временем точки, так как для обычных 
       // КП это время решили не вносить

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
		(select count(*) from LevelPoints lp
			inner join Distances d on lp.distance_id = d.distance_id
			where (d.raid_id = r.raid_id) and (NOW() >= DATE_SUB(lp.levelpoint_mindatetime, INTERVAL COALESCE(r.raid_readonlyhoursbeforestart, 8) HOUR))
				and  COALESCE(lp.levelpoint_mindatetime, 0) > 0			
		)
		as cantdelete,
		(select count(*) from LevelPoints lp
			inner join Distances d on lp.distance_id = d.distance_id
			where (d.raid_id = r.raid_id) and (NOW() >= lp.levelpoint_mindatetime)
				and  COALESCE(lp.levelpoint_mindatetime, 0) > 0			

		)
		as started,
		(select count(*) from LevelPoints lp
			inner join Distances d on lp.distance_id = d.distance_id
			where (d.raid_id = r.raid_id) and (NOW() < lp.levelpoint_maxdatetime)
				and  COALESCE(lp.levelpoint_maxdatetime, 0) > 0			
		)
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

        // Если команда не определена, и регистрация не закончена, то нужно проверить лимит
	if ($RaidStage < 2 && empty($TeamId) && $TeamOutOfRange == 0)
	{
        	// Если достигнут лимит или есть команды в списке ожидания, то "вне зачета"
		if (IsOutOfRaidLimit($RaidId) == 1 OR FindFirstTeamInWaitList($RaidId) > 0) 
		{
			$TeamOutOfRange = 1;
		}
	}
	// Конец проверки на лимиты

	
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
	//if ($OldMmb) return(1);

	// Модератор может до закрытия редактирования через raid_closedate
	if ($Moderator && ($RaidStage < 7)) return(1);

	// Если пользователь уже состоит в команде, то новую нельзя
	// !! реализовать алгоритм !!

        // Если стоит признак, что команда вне зачета, то можно
	// !! ошибка - если пользователь в момент вызова функции просматривает чужую команду,
	//    то $TeamOutOfRange берется из свойств этой команды
        if ($TeamOutOfRange == 1) return(1);

        // Если не стоит признак, что команда вне зачета, то только до закрытия регистрации
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
// Проверка возможности запросить слияние с пользователем

function CanRequestUserUnion($Administrator, $UserId, $ParentUserId)
{


	// Оба пользователя должны быть определеные
	if (!$UserId || !$ParentUserId) return(0);

	// Нельзя запросить слияние с самим собой
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
// Конец проверки возможности запросить слияние с пользователем

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
		if (is_array($str))
		{
			foreach($str as $k => $v)
				$str[$k] = EscapeString($v); 
			return $str;
		}
		
		$str = (string) $str;
		$search=array("\\","\0","\n","\r","\x1a","'",'"');
		$replace=array("\\\\","\\0","\\n","\\r","\Z","\'",'\"');
		return str_replace($search,$replace,$str);
	}

	function ReverseEscapeString($str)
	{
		if (is_array($str))
		{
			foreach($str as $k => $v)
				$str[$k] = ReverseEscapeString($v);
			return $str;
		}
		
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
			$_POST[$key] = EscapeString($value);

		foreach ($_GET as $key => $value)
			$_GET[$key] = EscapeString($value);

		foreach ($_REQUEST as $key => $value)
			$_REQUEST[$key] = EscapeString($value);
		
		foreach ($_COOKIE as $key => $value)
			$_COOKIE[$key] = EscapeString($value);
	}
        // Конец очистки специальных массивов от возможных инъекций


	// функция экранирует спец.символы в массивах переменных
	// POST GET
	function ReverseClearArrays()
	{
		foreach ($_POST as $key => $value)
			$_POST[$key] = ReverseEscapeString($value);
		
		foreach ($_GET as $key => $value)
			$_GET[$key] = ReverseEscapeString($value);
		
		foreach ($_REQUEST as $key => $value)
			$_REQUEST[$key] = ReverseEscapeString($value);
		
		foreach ($_COOKIE as $key => $value)
			$_COOKIE[$key] = ReverseEscapeString($value);
	}
	// Конец очистки специальных массивов от возможных инъекций

     // функция получает ссылку на  логотип
    function GetMmbLogo($raidid)
    {
		return CSql::raidFileLink($raidid, 2, false);
    }


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



     // функция получения общего рейтинга пользователя
	// параметр нужен только для того, чтобы указать текцщий ММБ
	// по умолчанию рейтинг считается только по закрытым.
	// считаем, что передан можеть быть только последний ММБ
     function RecalcUsersRank($raidid)
     {

	  // Находим 
	$sql = " select MAX(r.raid_id) as maxraidid
	         from Raids r 
	         where r.raid_closedate IS NOT NULL";
	$maxRaidId = CSql::singleValue($sql, 'maxraidid');
	
        // Обнуляем рейтинг  по всем пользовтелям
  	$sql = " update Users u	SET user_rank = NULL, user_r6 = NULL, 
  				user_minraidid = NULL,  user_maxraidid = NULL,
  				user_noinvitation = NULL
  		";

	 $rs = MySqlQuery($sql);

  	// Добавил проверку на сход
  	// проверка на ошибки идёт по полю, посчитанному в пересчете результатов
 
	$sql = "
		update Users u
		inner join 
		(select tu.user_id, SUM(COALESCE(tu.teamuser_rank, 0.00)) as rank, 
			SUM(COALESCE(tu.teamuser_rank, 0.00) * POW(0.9, $maxRaidId - d.raid_id)) as r6
	        from TeamUsers tu 
			inner join Teams t
			on tu.team_id = t.team_id	
			inner join Distances d
	        	on t.distance_id = d.distance_id
			left outer join 
			(
		 	select tld.teamuser_id,  MIN(lp.levelpoint_order) as minorder
		 	from TeamLevelDismiss tld
			 	inner join LevelPoints lp
			 	on tld.levelpoint_id = lp.levelpoint_id
			 group by tld.teamuser_id
                        ) c
			on tu.teamuser_id = c.teamuser_id
		where d.distance_hide = 0 
		       and tu.teamuser_hide = 0
		       and t.team_hide = 0 
		       and  COALESCE(t.team_outofrange, 0) = 0
		       and  COALESCE(t.team_result, 0) > 0
		       and COALESCE(t.team_minlevelpointorderwitherror, 0) = 0
		       and  COALESCE(c.minorder, 0) = 0
		       and  d.raid_id <= $maxRaidId
		group by tu.user_id
 		) a
		on a.user_id = u.user_id
		SET u.user_rank = a.rank, u.user_r6 = a.r6
                ";

		$rs = MySqlQuery($sql);


		// теперь считаем  минимальный и максимальный ключ ММБ по всем пользователям
	     	// добавил учет невыходов на старт
	
	$sql = "
		update Users u
		inner join 
		(select tu.user_id, MIN(d.raid_id) as minraidid, 
			MAX(d.raid_id) as maxraidid 
	        from TeamUsers tu 
			inner join Teams t
			on tu.team_id = t.team_id	
			inner join Distances d
	        	on t.distance_id = d.distance_id
			left outer join 
			(
				select tld.teamuser_id
				from TeamLevelDismiss tld
					inner join LevelPoints lp
				 	on tld.levelpoint_id = lp.levelpoint_id
				group by tld.teamuser_id
				having MIN(lp.levelpoint_order) = 1
			) dismiss
			on  tu.teamuser_id = dismiss.teamuser_id
 		where d.distance_hide = 0 
		       and dismiss.teamuser_id is null
		       and tu.teamuser_hide = 0
		       and t.team_hide = 0 
		       and  COALESCE(t.team_outofrange, 0) = 0
		       and  d.raid_id <= $maxRaidId
		group by tu.user_id
		) a
		on a.user_id = u.user_id
		SET u.user_minraidid = a.minraidid, u.user_maxraidid = a.maxraidid
                ";

	$rs = MySqlQuery($sql);

	     
	     	// теперь считаем  максимальный ключ ММБ по всем пользователям по невыходу на старт

	
	$sql = "
		update Users u
		inner join 
		(select tu.user_id, 
			MAX(d.raid_id) as maxnonstartraidid 
	        from TeamUsers tu 
			inner join Teams t
			on tu.team_id = t.team_id	
			inner join Distances d
	        	on t.distance_id = d.distance_id
			inner join 
			(
				select tld.teamuser_id
				from TeamLevelDismiss tld
					inner join LevelPoints lp
				 	on tld.levelpoint_id = lp.levelpoint_id
				group by tld.teamuser_id
				having MIN(lp.levelpoint_order) = 1
			) dismiss
			on  tu.teamuser_id = dismiss.teamuser_id
 		where d.distance_hide = 0 
		       and tu.teamuser_hide = 0
		       and t.team_hide = 0 
		       and  COALESCE(t.team_outofrange, 0) = 0
		       and  d.raid_id <= $maxRaidId
		group by tu.user_id
		) a
		on a.user_id = u.user_id
		SET u.user_maxnotstartraidid = a.maxnonstartraidid
                ";

	
	     
	     

		// теперь считаем флаг у тех кто не вышел (?!) или дисквалифицирован 
	$sql = "
		update Users u
		inner join 
		(select tu.user_id, COALESCE(dismiss.minorder, 0) as minorder, 
			COALESCE(disq.disqualification, 0) as disqualification, 
			COALESCE(points.pointscount, 0) as pointscount
	        from TeamUsers tu 
			inner join Teams t
			on tu.team_id = t.team_id	
			inner join Distances d
	        	on t.distance_id = d.distance_id
			left outer join 
			(
		 	select tlp.team_id,  count(*) as disqualification
		 	from TeamLevelPoints tlp
			where COALESCE(tlp.error_id, 0) = 15 
			group by tlp.team_id
                        ) disq
                        on t.team_id = disq.team_id
			left outer join 
			(
		 	select tlp.team_id,  count(*) as pointscount
		 	from TeamLevelPoints tlp
			group by tlp.team_id
                        ) points
                        on t.team_id = points.team_id
			left outer join 
			(
			select tld.teamuser_id,  MIN(lp.levelpoint_order) as minorder
		 	from TeamLevelDismiss tld
			 	inner join LevelPoints lp
			 	on tld.levelpoint_id = lp.levelpoint_id
			 group by tld.teamuser_id
                        ) dismiss
                        on tu.teamuser_id = dismiss.teamuser_id
 		where d.distance_hide = 0 
		       and tu.teamuser_hide = 0
		       and t.team_hide = 0 
		       and  COALESCE(t.team_outofrange, 0) = 0
		       and  d.raid_id = $maxRaidId
		group by tu.user_id
		) a
		on a.user_id = u.user_id
		SET u.user_noinvitation = 1
		WHERE a.minorder = 1 
			or a.disqualification > 1 
			or a.pointscount = 0
                ";

		 $rs = MySqlQuery($sql);

  
		// теперь ставим флаг у тех кто не участоввал в последних 8 ммб
	$sql = "
		update Users u
		SET u.user_noinvitation = 1
		WHERE $maxRaidId - 8 >= COALESCE(u.user_maxraidid, 0)
		      and  u.user_maxraidid is not null
                ";

		 $rs = MySqlQuery($sql);


         return (1);
     }
     // конец функции получения общего рейтинга пользователя


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
		       and t.team_outofrange = 0
		       and tu.teamuser_hide = 0 
		 order  by d.raid_id DESC
		 LIMIT 0,1";
	$PredRaidId = CSql::singleValue($sql, 'raid_id', false);

        return ($PredRaidId);
     }
     //Конец получения предыдущго ММБ

     // Проверка неявки на старт в прошлое участие
     function CheckNotStart($userid, $raidid)
     {

        $NotStart = 0;     
        $PredRaidId = GetPredRaidForUser($userid, $raidid);
	
	// Проверяем  что участник не явился на старт и при этом команда была в зачете
	if ($PredRaidId) {

		$sql = " select count(*) as result
		         from TeamUsers tu
			       inner join Teams t
			       on t.team_id = tu.team_id
			       inner join Distances d
			       on t.distance_id = d.distance_id
		         where d.raid_id = $PredRaidId
		               and d.raid_id >= 19
			       and tu.user_id = $userid
			       and COALESCE(t.team_maxlevelpointorderdone, 0) = 0 
			       and t.team_hide = 0
			       and t.team_outofrange = 0
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
	$sql = " select COALESCE(SUM(r.raid_nostartprice), 0.) as notstartpayment
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

	 $teamRaidCondition1 = (!empty($teamid)) ? " tlp1.team_id = $teamid" : "d1.raid_id = $raidid";
	 $teamRaidCondition2 = (!empty($teamid)) ? " tlp2.team_id = $teamid" : "d2.raid_id = $raidid";

	// Не знаю, как лучше - можно по порядку точек, а можно по времени прохождения
	// по времени проще
	// 			 on lp1.levelpoint_order > a.levelpoint_order	


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
		       			 and $teamRaidCondition2
			      ) a
	 		      on tlp1.team_id = a.team_id 
			         and lp1.levelpoint_order > a.levelpoint_order
			 where tlp1.teamlevelpoint_datetime > 0
			       and  lp1.pointtype_id <> 1
	       		   and $teamRaidCondition1
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

	 $teamRaidCondition = (!empty($teamid)) ? " t.team_id = $teamid" : "d.raid_id = $raidid";

         
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
				 where  $teamRaidCondition
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

	$teamRaidCondition = (!empty($teamid)) ? " t1.team_id = $teamid" : "d1.raid_id = $raidid";
	
		/*
        правильный вариант с штрафом в следующей точке со временем
        */
/*
   правильно сдедлать удаление, но нет прав.

	$sql = " DROP TABLE IF EXISTS tmp_tlp1 ";
	$rs = MySqlQuery($sql);

	$sql = " DROP TABLE IF EXISTS tmp_tlp2 ";
	$rs = MySqlQuery($sql);

	$sql = " DROP TABLE IF EXISTS tmp_tlp3 ";
	$rs = MySqlQuery($sql);
*/


	// можно наверное ещ ускорить, если вторую временную таблицу из первой копирваниемполучать, а не запросом

	$sql = " CREATE TEMPORARY TABLE IF NOT EXISTS 
				tmp_rtlppwd1 (
                 team_id INT, INDEX (team_id),
                 levelpoint_order INT,  INDEX (levelpoint_order) 
			     ) 
				ENGINE=MEMORY ";
	$rs = MySqlQuery($sql);
				
	$sql = " DELETE FROM tmp_rtlppwd1  ";
	$rs = MySqlQuery($sql);

	$sql = " INSERT INTO tmp_rtlppwd1 (team_id, levelpoint_order)
			 select t1.team_id, lp1.levelpoint_order 
                                  from TeamLevelPoints tlp1 
                                           inner join LevelPoints lp1 on tlp1.levelpoint_id = lp1.levelpoint_id 
                                           inner join Teams t1 on t1.team_id = tlp1.team_id 
                                           inner join Distances d1 on t1.distance_id = d1.distance_id 
                                  where $teamRaidCondition
			";
	$rs = MySqlQuery($sql);
			 
	$sql = " CREATE TEMPORARY TABLE IF NOT EXISTS 
				tmp_rtlppwd2 (
		             team_id INT, INDEX (team_id),
		             levelpoint_order INT,  INDEX (levelpoint_order) 
			     ) 
				ENGINE=MEMORY ";
	$rs = MySqlQuery($sql);
				
	$sql = " DELETE FROM tmp_rtlppwd2  ";
	$rs = MySqlQuery($sql);

	$sql = " INSERT INTO tmp_rtlppwd2 (team_id, levelpoint_order)
			 select t1.team_id, lp1.levelpoint_order 
                                  from TeamLevelPoints tlp1 
                                           inner join LevelPoints lp1 on tlp1.levelpoint_id = lp1.levelpoint_id 
                                           inner join Teams t1 on t1.team_id = tlp1.team_id 
                                           inner join Distances d1 on t1.distance_id = d1.distance_id 
                                  where $teamRaidCondition
			";
	$rs = MySqlQuery($sql);

	$sql = " CREATE TEMPORARY TABLE IF NOT EXISTS 
				tmp_rtlppwd3 (
                                 team_id INT, INDEX (team_id),
                                 up  INT,  INDEX (up) ,
                                 penalty INT
				        ) 
				ENGINE=MEMORY ";
	$rs = MySqlQuery($sql);
				
	$sql = " DELETE FROM tmp_rtlppwd3  ";
	$rs = MySqlQuery($sql);

	$sql = " INSERT INTO tmp_rtlppwd3 (team_id, up, penalty)
			 select c.team_id, c.up, SUM(COALESCE(lp.levelpoint_penalty, 0)) as penalty
         			from
		         		(select a.team_id, a.levelpoint_order as up, MAX(b.levelpoint_order) as down
                                         from   tmp_rtlppwd1 a 
                                                  inner join tmp_rtlppwd2  b 
                                                  on a.team_id = b.team_id and a.levelpoint_order > b.levelpoint_order 
                                         group by a.team_id, a.levelpoint_order 
                                         having a.levelpoint_order > MAX(b.levelpoint_order) + 1
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
			";
	$rs = MySqlQuery($sql);

	$sql = "update TeamLevelPoints tlp0
				 inner join LevelPoints lp0
				 on tlp0.levelpoint_id = lp0.levelpoint_id
				 inner join tmp_rtlppwd3 d
				 on tlp0.team_id = d.team_id
					and lp0.levelpoint_order = d.up
			set  tlp0.teamlevelpoint_penalty = COALESCE(tlp0.teamlevelpoint_penalty, 0) + COALESCE(d.penalty, 0) 
			";
	$rs = MySqlQuery($sql);

	$sql = " DELETE FROM tmp_rtlppwd1  ";
	$rs = MySqlQuery($sql);

	$sql = " DELETE FROM tmp_rtlppwd2  ";
	$rs = MySqlQuery($sql);

	$sql = " DELETE FROM tmp_rtlppwd3  ";
	$rs = MySqlQuery($sql);



	
     }
     // Конец функции расчета штрафа для КП без амнистий		
     
     
        // функция пересчитывает результат команды в точке 
     function RecalcTeamLevelPointsResult($raidid, $teamid)
     {

	 if (empty($teamid) and empty($raidid)) {     	 
	    return;
	 }

	$teamRaidCondition = (!empty($teamid)) ? " t1.team_id = $teamid" : "d1.raid_id = $raidid";





	$sql = " CREATE TEMPORARY TABLE IF NOT EXISTS 
				tmp_rtlpr1 (
                  team_id INT, INDEX (team_id),
                  levelpoint_order INT,  INDEX (levelpoint_order) 
			     ) 
				ENGINE=MEMORY ";

//echo $sql.";";

	$rs = MySqlQuery($sql);
				
	$sql = " DELETE FROM tmp_rtlpr1  ";
//echo $sql.";";


	$rs = MySqlQuery($sql);

	
	$sql = " INSERT INTO tmp_rtlpr1 (team_id, levelpoint_order)
			 select t1.team_id, lp1.levelpoint_order 
                                  from TeamLevelPoints tlp1 
                                           inner join LevelPoints lp1 on tlp1.levelpoint_id = lp1.levelpoint_id 
                                           inner join Teams t1 on t1.team_id = tlp1.team_id 
                                           inner join Distances d1 on t1.distance_id = d1.distance_id 
                                  where lp1.pointtype_id <> 1 
								         and $teamRaidCondition
			";
  //   echo $sql.";";

	 $rs = MySqlQuery($sql);


			

	 $sql = " CREATE TEMPORARY TABLE IF NOT EXISTS 
				tmp_rtlpr2 (
		              team_id INT, INDEX (team_id),
		              levelpoint_order INT,  INDEX (levelpoint_order),
                      durationinsec INT, 
					  penaltyinmin INT
			     ) 
				ENGINE=MEMORY ";
//	echo $sql.";";
	$rs = MySqlQuery($sql);
				
	$sql = " DELETE FROM tmp_rtlpr2  ";
//echo $sql.";";

	$rs = MySqlQuery($sql);
	
	$sql = " INSERT INTO tmp_rtlpr2 (team_id, levelpoint_order, durationinsec, penaltyinmin)
			 select t1.team_id, lp1.levelpoint_order,  
				    TIME_TO_SEC(COALESCE(tlp1.teamlevelpoint_duration, 0)) as durationinsec, 
					COALESCE(tlp1.teamlevelpoint_penalty, 0) as penaltyinmin  
                                  from TeamLevelPoints tlp1 
                                           inner join LevelPoints lp1 on tlp1.levelpoint_id = lp1.levelpoint_id 
                                           inner join Teams t1 on t1.team_id = tlp1.team_id 
                                           inner join Distances d1 on t1.distance_id = d1.distance_id 
                                  where lp1.pointtype_id <> 1 
								         and $teamRaidCondition
			";
	
//	echo $sql.";";

	$rs = MySqlQuery($sql);


	
	

	$sql = " CREATE TEMPORARY TABLE IF NOT EXISTS 
				tmp_rtlpr3 (
                                 team_id INT, INDEX (team_id),
                                 up  INT, INDEX (up),
								 totaldurationinsec INT,
                                 totalpenaltyinsec INT
				        ) 
				ENGINE=MEMORY";

//	echo $sql.";";
	$rs = MySqlQuery($sql);
				
	$sql = " DELETE FROM tmp_rtlpr3  ";
//echo $sql.";";

	$rs = MySqlQuery($sql);
	
	$sql = " INSERT INTO tmp_rtlpr3 (team_id, up, totaldurationinsec, totalpenaltyinsec)
			 select a.team_id as team_id, 
			        a.levelpoint_order as up, 
			        SUM(b.durationinsec) as totaldurationinsec,
					SUM(b.penaltyinmin)*60 as totalpenaltyinsec 
         			from
		         		(select team_id, levelpoint_order from tmp_rtlpr1) a 
                        inner join
						(select team_id, levelpoint_order, durationinsec, penaltyinmin from tmp_rtlpr2) b 
                        on a.team_id = b.team_id and a.levelpoint_order >= b.levelpoint_order 
                    group by a.team_id, a.levelpoint_order 
			";
//	echo $sql.";";
	$rs = MySqlQuery($sql);




	$sql = " CREATE TEMPORARY TABLE IF NOT EXISTS 
				tmp_rtlpr4 (
                                 team_id INT, INDEX (team_id),
                                 levelpoint_id  INT, INDEX (levelpoint_id),
								 result TIME
				        ) 
				ENGINE=MEMORY";

//	echo $sql.";";
	$rs = MySqlQuery($sql);


	$sql = " DELETE FROM tmp_rtlpr4  ";
//echo $sql.";";
	$rs = MySqlQuery($sql);
	
	$sql = " INSERT INTO tmp_rtlpr4 (team_id, levelpoint_id, result)
			 select c.team_id as team_id, 
			        lp0.levelpoint_id as levelpoint_id, 
					SEC_TO_TIME(c.totaldurationinsec + c.totalpenaltyinsec) as result
    		 from tmp_rtlpr3  c
                  inner join Teams t1 on t1.team_id = c.team_id 
			      inner join LevelPoints lp0
				  on t1.distance_id = lp0.distance_id 
				     and lp0.levelpoint_order = c.up
			";
    

  //   echo $sql.";";
	 
	$rs = MySqlQuery($sql);






	$sql = "update TeamLevelPoints tlp0
				 inner join tmp_rtlpr4 d
				 on tlp0.team_id = d.team_id
					and tlp0.levelpoint_id = d.levelpoint_id
			set  tlp0.teamlevelpoint_result = d.result
			";

//	echo $sql.";";
	 
	$rs = MySqlQuery($sql);




// Правильно сдлеать удаление, но нет прав

	$sql = " DELETE FROM tmp_rtlpr1  ";
	$rs = MySqlQuery($sql);

	$sql = " DELETE FROM tmp_rtlpr2  ";
	$rs = MySqlQuery($sql);

	$sql = " DELETE FROM tmp_rtlpr3  ";
	$rs = MySqlQuery($sql);

	$sql = " DELETE FROM tmp_rtlpr4  ";
	$rs = MySqlQuery($sql);

     }
     // Конец функции расчета результата в точке		
 

// Функция поиска ошибок для марш-броска/команды
function FindErrors($raid_id, $team_id)
{
	// Получаем список команд с ошибками, выставленными вручную (дисквалификации и т.д.)
	$teams_with_manualerrors = array();
	$sql = "SELECT DISTINCT team_id FROM TeamLevelPoints, Errors WHERE TeamLevelPoints.error_id <> 0 AND TeamLevelPoints.error_id = Errors.error_id AND Errors.error_manual = 1";
	$Result = MySqlQuery($sql);
	while ($Row = mysql_fetch_assoc($Result)) $teams_with_manualerrors[] = $Row['team_id'];
	mysql_free_result($Result);

	$distances = array();
	// Если нужно проверить отдельную команду, то находим дистанцию, по которой она бежала
	if ($team_id)
	{
		if (in_array($team_id, $teams_with_manualerrors)) return(0);
		$sql = "SELECT distance_id FROM Teams WHERE team_id = $team_id AND team_hide = 0";
		$Result = MySqlQuery($sql);
		if (mysql_num_rows($Result) <> 1) die('Команда $team_id отсутствует или удалена, проверка невозможна');
		$Row = mysql_fetch_assoc($Result);
		if ($Row) $distances[] = $Row['distance_id'];
		mysql_free_result($Result);
	}
	// Если нужно проверить весь марш-бросок, то находим все дистанции, принадлежащие данному марш-броску
	if ($raid_id)
	{
		$sql = "SELECT distance_id FROM Distances WHERE raid_id = $raid_id AND distance_hide = 0";
		$Result = MySqlQuery($sql);
		while ($Row = mysql_fetch_assoc($Result)) $distances[] = $Row['distance_id'];
		mysql_free_result($Result);
	}
	if (!count($distances)) die('Отсутствуют дистанции для проверки');
	$teamRaidCondition = (!empty($team_id)) ? "t.team_id = $team_id" : "d.raid_id = $raid_id";

	// Проверяем в цикле все дистанции (для всех команд с данной дистанции или для конкретной команды
	$total_errors = 0;
	foreach ($distances as $distance_id)
	{
		// Составляем список команд для проверки на данной дистанции
		$teams = array();
		if ($team_id) $teams[] = $team_id;
		else
		{
			$sql = "SELECT DISTINCT TeamLevelPoints.team_id FROM TeamLevelPoints, Teams WHERE TeamLevelPoints.team_id = Teams.team_id AND Teams.distance_id = $distance_id";
			$Result = MySqlQuery($sql);
			while ($Row = mysql_fetch_assoc($Result))
				if (!in_array($Row['team_id'], $teams_with_manualerrors))
					$teams[] = $Row['team_id'];
			mysql_free_result($Result);
		}
		if (!count($teams)) continue;

		// На данной дистанции есть команды, которые нужно проверить, выясняем параметры дистанции и проверяем их
		$points = array();
		$order = array();
		$mandatory = array();
		$scanners = array();
		$cards = array();
		$ncard = 0;
		$sql = "SELECT * FROM LevelPoints WHERE distance_id = $distance_id AND levelpoint_hide = 0 ORDER BY levelpoint_order ASC";
		$Result = MySqlQuery($sql);
		while ($Row = mysql_fetch_assoc($Result))
		{
			// Проверяем уникальность levelpoint_order
			if (isset($order[$Row['levelpoint_order']])) die("Дублирующийся levelpoint_order = {$Row['levelpoint_order']}");
			$order[$Row['levelpoint_order']] = 1;
			// Запоминаем параметры точки
			$id = $Row['levelpoint_id'];
			$points[$id]['order'] = $Row['levelpoint_order'];
			$points[$id]['pointtype'] = $Row['pointtype_id'];
			$points[$id]['penalty'] = $Row['levelpoint_penalty'];
			if ($Row['levelpoint_mindatetime'] && ($Row['levelpoint_mindatetime'] != "0000-00-00 00:00:00"))
				$points[$id]['min_time'] = $Row['levelpoint_mindatetime'];
			if ($Row['levelpoint_maxdatetime'] && ($Row['levelpoint_maxdatetime'] != "0000-00-00 00:00:00"))
				$points[$id]['max_time'] = $Row['levelpoint_maxdatetime'];
			if ($Row['scanpoint_id']) $points[$id]['scanpoint'] = $Row['scanpoint_id'];
			// У точек С, Ф, ОКП (со сканерами) и СК должно быть корретное время работы
			if (($points[$id]['pointtype'] == 1) || ($points[$id]['pointtype'] == 2) || (($points[$id]['pointtype'] == 3) && isset($points[$id]['scanpoint'])) || ($points[$id]['pointtype'] == 4))
			{
				if (!isset($points[$id]['min_time'])) die("У точки $id не указано время начала работы");
				if (!isset($points[$id]['max_time'])) die("У точки $id не указано время окончания работы");
				if ($points[$id]['max_time'] < $points[$id]['max_time']) die("У точки $id некорректный диапазон работы");
			}
			// На точках С, Ф и СК должно быть сканеры
			if (($points[$id]['pointtype'] == 1) || ($points[$id]['pointtype'] == 2) || ($points[$id]['pointtype'] == 4))
			{
				if (!isset($points[$id]['scanpoint'])) die("У точки $id не указан scanpoint_id");
			}
			// На обычном КП не должно быть сканеров и времени работы точки
			if ($points[$id]['pointtype'] == 5)
			{
				if (isset($points[$id]['min_time']) || isset($points[$id]['max_time'])) die("У точки $id не должно быть времени работы");
				if (isset($points[$id]['scanpoint'])) die("На точке $id не должно быть сканеров");
			}
			// Остальные типы точек не поддерживаем
			if (($points[$id]['pointtype'] < 1) || ($points[$id]['pointtype'] > 5)) die("Неподдерживаемый тип точки $id");
			// Добавляем обязательные точки в отдельный массив
			if (($points[$id]['pointtype'] >= 1) && ($points[$id]['pointtype'] <= 4)) $mandatory[$id] = $points[$id];
			// Добавляем точки со сканерами в отдельный массив
			if (isset($points[$id]['scanpoint'])) $scanners[$id] = $points[$id];
			// Формируем массив с "карточками" по виртуальным этапам
			if ($points[$id]['pointtype'] == 1)
			{
				$ncard++;
				$cards[$ncard]['start'] = $id;
			}
			if ($points[$id]['pointtype'] == 2)
			{
				if (isset($cards[$ncard]['finish'])) die("Двойной финиш на этапе в точке $id");
				$cards[$ncard]['finish'] = $id;
			}
			if ($points[$id]['pointtype'] == 4)
			{
				// СК одновременно является финишем одного этапа и стартом следующего
				if (isset($cards[$ncard]['finish'])) die("Двойной финиш на этапе в точке $id");
				$cards[$ncard]['finish'] = $id;
				$ncard++;
				$cards[$ncard]['start'] = $id;
			}
		}
		unset($order);
		mysql_free_result($Result);
		// Проверяем получившиеся карточки
		foreach ($cards as $card)
			if (!isset($card['finish'])) die("Этап со стартом в точке {$card['start']} не имеет финиша");	

		// Данные о точках дистанции сформированы, проверяем в цикле все команды с дистанции
		foreach ($teams as $team_id)
		{
			// Извлекаем все результаты команды на точках
			$results = array();
			$errors = array();
			$sql = "SELECT * FROM TeamLevelPoints WHERE team_id = $team_id";
			$Result = MySqlQuery($sql);
			while ($Row = mysql_fetch_assoc($Result))
			{
				// У команды не может быть несколько записей результата на одной точке
				$id = $Row['levelpoint_id'];
				if (isset($results[$id])) $errors[$id] = 20; else $errors[$id] = 0;
				// Запоминаем данные на точке
				$results[$id]['edit_time'] = $Row['teamlevelpoint_date'];
				$results[$id]['operator'] = $Row['user_id'];
				$results[$id]['device'] = $Row['device_id'];
				$team_time = $Row['teamlevelpoint_datetime'];
				if ($team_time == "0000-00-00 00:00:00") $team_time = "";
				$results[$id]['team_time'] = $team_time;
				// Команда посетила точку, не принадлежащую дистанции
				if (!$errors[$id] && !isset($points[$id])) $errors[$id] = 21;
				// На КП без сканера не может быть записи со временем посещения точки
				if (!$errors[$id] && !isset($scanners[$id]) && $team_time) $errors[$id] = 22;
				// На КП со сканером не может быть записи о посещении без времени
				if (!$errors[$id] && isset($scanners[$id]) && !$team_time) $errors[$id] = 23;
				// Время посещения точки должно быть в интервале работы точки
				if (!$errors[$id] && $team_time &&
					(($team_time < $points[$id]['min_time']) || ($team_time > $points[$id]['max_time']))) $errors[$id] = 24;
				// Время ввода результата меньше времени команды на точке
				if (!$errors[$id] && $team_time && ($results[$id]['edit_time'] < $team_time)) $errors[$id] = 25;
			}
			mysql_free_result($Result);

			// Проверяем последовательность обязательных точек
			$mandatory_skipped = "";
			$mandatory_visited = "";
			foreach ($mandatory as $id => $point)
			{
				if (!isset($results[$id]))
					// Обязательная точка пропущена, запоминаем ее
					$mandatory_skipped = $point;
				else
				{
					// Обязательная точка посещена, запоминаем ее
					$mandatory_visited = $point;
					if ($mandatory_skipped <> "")
					{
						// Предыдущая обязательная точка пропущена, ставим на текущей ошибку
						if (!$errors[$id]) $errors[$id] = 26;
						$mandatory_skipped = "";
					}
				}
			}

			// Определяем, какую точку команда посетила последней
			$last_visited_id = "";
			foreach ($points as $id => $point)
				if (isset($results[$id])) $last_visited_id = $id;
			// Последняя посещенная точка должна быть с судьями, которые зарегистрируют посещение
			// То есть она не может иметь тип 5 (обычный компостер)
			if (($last_visited_id <> "") && !$errors[$last_visited_id] && ($points[$last_visited_id]['pointtype'] == 5))
				$errors[$last_visited_id] = 27;

			// Время посещение точек со сканерами должно возрастать от точки к точке
			$prev_time = -1;
			foreach ($scanners as $id => $point)
				if (isset($results[$id]))
				{
					if ($results[$id]['team_time'] == "") continue;
					$curr_time = strtotime($results[$id]['team_time']);
					if (($prev_time <> -1) && !$errors[$id] && ($curr_time <= $prev_time)) $errors[$id] = 28;
					$prev_time = $curr_time;
				}

			// Анализируем КП, расположенные на одной карточке (то есть на одном этапе в старой идеологии)
			foreach ($cards as $card)
			{
				$start_id = $card['start'];
				$finish_id = $card['finish'];
				// Если команда не прошла этап, то проверять нечего
				if (!isset($results[$start_id]) || !isset($results[$finish_id])) continue;
				// Если у команды уже есть ошибки на старте/финише этапа, то не проверяем
				if ($errors[$start_id] || $errors[$finish_id]) continue;
				// Считаем время в пути от старта этапа до финиша
				$delta = strtotime($results[$finish_id]['team_time']) - strtotime($results[$start_id]['team_time']);
				// Оно не должно быть слишком маленьким или большим
				if ($delta < 3 * 60 * 60) $errors[$finish_id] = -1;
				if ($delta > 25 * 60 * 60) $errors[$finish_id] = -2;
/*
				// Анализируем время редактирования для всех точек, введенных на планшетах
				$start_order = $points[$start_id]['order'];
				$finish_order = $points[$finish_id]['order'];
				$tablet_edit_time = "";
				foreach ($points as $id => $point)
				{
					// Анализируем только точки с текущей карточки
					$point_order = $point['order'];
					if (($point_order <= $start_order) || ($point_order > $finish_order)) continue;
					// Проверяем точки, у которых есть результат и он не отредактирован через сайт
					if (isset($results[$id]) && ($results[$id]['device'] <> 1))
					{
						$curr_tablet_edit_time = $results[$id]['edit_time'];
						if ($tablet_edit_time == "") $tablet_edit_time = $curr_tablet_edit_time;
						// Все точки с одной карточки, введенные не на сайте, должны иметь одно время редактирования
						else if (($tablet_edit_time <> $curr_tablet_edit_time) && !$errors[$id]) $errors[$id] = 29;
					}
				}
*/
			}

			// Проверка времени команды на дистанции с учетом штрафов, хранящегося в Team.team_result
			// !!
			// Проверка порядкового номер точки, до которой на дистанции добралась команда, хранящегося в Team.team_maxlevelpointorderdone
			// !!

			// Обновляем в базе error_id для всех проверенных точек с результатами команды
			$sql = "UPDATE TeamLevelPoints SET error_id = 0 WHERE team_id = $team_id";
			MySqlQuery($sql);
			foreach ($errors as $point_id => $error_id)
			{
				if (!$error_id) continue;
				$total_errors++;
				$sql = "UPDATE TeamLevelPoints SET error_id = $error_id WHERE team_id = $team_id AND levelpoint_id = $point_id";
				MySqlQuery($sql);
			}
		}
	}

	// Обновление поля team_minlevelpointorderwitherror, используемого для быстрого показа результатов команд
	$sql = "UPDATE Teams t
			INNER JOIN
			(SELECT tlp.team_id, MIN(COALESCE(lp.levelpoint_order, 0)) AS error FROM TeamLevelPoints tlp
				INNER JOIN Teams t ON tlp.team_id = t.team_id
				INNER JOIN Distances d ON t.distance_id = d.distance_id
				INNER JOIN LevelPoints lp ON tlp.levelpoint_id = lp.levelpoint_id
				WHERE COALESCE(tlp.error_id, 0) > 0 AND $teamRaidCondition
				GROUP BY tlp.team_id
			) a
			ON t.team_id = a.team_id
		SET team_minlevelpointorderwitherror = COALESCE(a.error, 0)";
	MySqlQuery($sql);

	// Результат поиска ошибок
	// if ($raid_id) echo "Проверка данных завершена, найдено ошибок: $total_errors<br>\n";
	return($total_errors);
}
// Конец функции поиска ошибок для марш-броска/команды


	// функция проверяет ошибки SQL запросами.
	// Не используется, аналогичный функционал дожен быть в валидаторе.
	function RecalcErrors($raidid, $teamid)
	{

		if (empty($teamid) and empty($raidid)) 
		{
			return;
		}

		$teamRaidCondition = (!empty($teamid)) ? " t.team_id = $teamid" : "d.raid_id = $raidid";
		$teamRaidCondition1 = (!empty($teamid)) ? " t1.team_id = $teamid" : "d1.raid_id = $raidid";
		$teamRaidCondition2 = (!empty($teamid)) ? " t2.team_id = $teamid" : "d2.raid_id = $raidid";


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
						and $teamRaidCondition";
	//	echo $sql;
		$rs = MySqlQuery($sql);

	// Устанавливаем невзятие обязательных КП



	//Ставим ощибку на следующую точку за невзятым обязательным КП
	$sql = " update TeamLevelPoints tlp0
			 inner join LevelPoints lp0
			 on tlp0.levelpoint_id = lp0.levelpoint_id
			 inner join
				(select c.team_id, c.up
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
						where  $teamRaidCondition1
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
							 where $teamRaidCondition2
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
				   and lp.levelpoint_order > c.down
				   and lp.pointtype_id = 3
				left outer join LevelPointDiscounts lpd
				on t.distance_id = lpd.distance_id
				   and lp.levelpoint_order <= lpd.levelpointdiscount_finish
				   and  lp.levelpoint_order >= lpd.levelpointdiscount_start
				where lpd.levelpointdiscount_id is null
				group by c.team_id, c.up
			) d
			on tlp0.team_id = d.team_id
			   and lp0.levelpoint_order = d.up
	set tlp0.error_id = 14";


		//    echo $sql;

		$rs = MySqlQuery($sql);


	}
	// Конец функции проверки ошибок
 
 
 
     
     // функция пересчитывает результат команды по данным в точках
     function RecalcTeamResultFromTeamLevelPoints($raidid, $teamid)
     {


	if (empty($teamid) and empty($raidid)) {
	    return;
	}

	$teamRaidCondition = (!empty($teamid)) ? " t.team_id = $teamid" : "d.raid_id = $raidid";


	if (empty($teamid)) {
		CMmbLogger::enable(0);
		//CMmbLogger::enable(1);
	} 


		 $tm0 = microtime(true);

		 // Обнуляем данные расчета
	$sql = " update  TeamLevelPoints tlp
		         join Teams t
			 on tlp.team_id = t.team_id
  		         inner join Distances d
			 on t.distance_id = d.distance_id
		  set  tlp.teamlevelpoint_penalty = NULL,
		       tlp.teamlevelpoint_duration = NULL,
		       t.team_maxlevelpointorderdone = NULL,
		       t.team_minlevelpointorderwitherror = NULL,
		       t.team_donelevelpoint = NULL,
		       t.team_comment = NULL
 		  where $teamRaidCondition" ;
				 
	$rs = MySqlQuery($sql);

		 $tm1 = CMmbLogger::addInterval(' 1 ', $tm0);

	RecalcTeamLevelPointsDuration($raidid, $teamid);

		 $tm2 = CMmbLogger::addInterval(' 2', $tm1);

	RecalcTeamLevelPointsPenaltyWithDiscount($raidid, $teamid);

		 $tm3 = CMmbLogger::addInterval(' 3', $tm2);

	RecalcTeamLevelPointsPenaltyWithoutDiscount($raidid, $teamid);

		 $tm4 = CMmbLogger::addInterval(' 4', $tm3);

// echo 'RecalcTeamLevelPointsResult '.$raidid.','.$teamid;

	RecalcTeamLevelPointsResult($raidid, $teamid);

		 $tm5 = CMmbLogger::addInterval(' 5', $tm4);

//	return;
	
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



	//Ставим ошибки
	//FindErrors($raidid, $teamid);

		 $tm6 = CMmbLogger::addInterval(' 6', $tm5);


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

		 $tm7 = CMmbLogger::addInterval(' 7', $tm6);


	 // теперь можно посчитать рейтинг
	RecalcTeamUsersRank($raidid);


		 $tm8 = CMmbLogger::addInterval(' 8', $tm7);

		 //
     // Находим минимальную точку с ошибкой (COALESCE(tlp.error_id, 0) > 0)
     // < 0 - это не ошибка - предупреждение
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
						where  COALESCE(tlp.error_id, 0) > 0
		        			and $teamRaidCondition
						group by tlp.team_id
						) a
		  			on t.team_id = a.team_id
          	set  team_minlevelpointorderwitherror = COALESCE(a.error, 0)";

	//     echo $sql;
	 
	$rs = MySqlQuery($sql);

		 $tm9 = CMmbLogger::addInterval(' 9', $tm8);


	 // Находим невзятые КП
	 // расчет закомментирован, так ка Сергей строит список другим способом
	     /*
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

		 $rs = MySqlQuery($sql);*/



	// Обновляем комментарий у команды
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

		 $tm10 = CMmbLogger::addInterval(' 10', $tm9);


		 //Теперь в это поле добавляем ошибки  tlp.error_id  > 0
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
						where  COALESCE(tlp.error_id, 0) > 0
		                       and $teamRaidCondition
						group by tlp.team_id
                      ) a
		  		on t.team_id = a.team_id
		  set  t.team_comment = CASE WHEN a.team_error <> '' THEN CONCAT('Ошибки: ', a.team_error, '; ',  COALESCE(t.team_comment, ''))  ELSE t.team_comment END";

        //   echo $sql;
	$rs = MySqlQuery($sql);

		 $tm11 = CMmbLogger::addInterval(' 11', $tm10);


		 //Теперь в это поле добавляем предупреждения  tlp.error_id  < 0
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
						where  COALESCE(tlp.error_id, 0) < 0
		                       and $teamRaidCondition
						group by tlp.team_id
                      ) a
		  		on t.team_id = a.team_id
		  set  t.team_comment = CASE WHEN a.team_error <> '' THEN CONCAT('Предупреждения: ', a.team_error, '; ',  COALESCE(t.team_comment, ''))  ELSE t.team_comment END";
        //   echo $sql;
	$rs = MySqlQuery($sql);


		 $tm12 = CMmbLogger::addInterval(' 12', $tm11);


		 //Теперь в это поле добавляем предупреждения  tlp.error_id  < 0
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
						where  COALESCE(tlp.error_id, 0) < 0
		                       and $teamRaidCondition
						group by tlp.team_id
                      ) a
		  		on t.team_id = a.team_id
		  set  t.team_comment = CASE WHEN a.team_error <> '' THEN CONCAT('Предупреждения: ', a.team_error, '; ',  COALESCE(t.team_comment, ''))  ELSE t.team_comment END";
        //   echo $sql;
	$rs = MySqlQuery($sql);

		 $tm13 = CMmbLogger::addInterval(' 13', $tm12);


		 //считаем (только для интерфейса список вщзятых КП
	$sql = " update  Teams t
                inner join
                      (select tlp.team_id 
			,group_concat(DATE_FORMAT(COALESCE(tlp.teamlevelpoint_datetime, ''),'%H:%i')  order by teamlevelpoint_datetime  separator ', ') as team_donelevelpoint
						from TeamLevelPoints tlp
							    inner join Teams t
							    on t.team_id = tlp.team_id
							    inner join Distances d
							    on t.distance_id = d.distance_id
						where  COALESCE(tlp.teamlevelpoint_datetime, 0) > 0
		                       and $teamRaidCondition
						group by tlp.team_id
                      ) a
		  		on t.team_id = a.team_id
		  set  t.team_donelevelpoint = a.team_donelevelpoint ";
        //   echo $sql;
	$rs = MySqlQuery($sql);

		 $tm14 = CMmbLogger::addInterval(' 14', $tm13);


	 // теперь можно посчитать рейтинг
	RecalcUsersRank($raidid);


		 $tm14 = CMmbLogger::addInterval(' 15', $tm13);


		 $msg = CMmbLogger::getText();

		 //CMmb::setShortResult($msg, '');
		 CMmb::setMessage($msg);

     }
     // Конец функции пересчета результат команды по данным в точках	


	// Функция проверяет, превышен ли лимит заявок на ММБ
	function IsOutOfRaidLimit($RaidId)
	{
		 // Получаем информацию о лимите и о зарегистированных командах
		$sql = "select count(*) as teamscount, COALESCE(r.raid_teamslimit, 0) as teamslimit
			from Raids r 
				inner join Distances d
				on r.raid_id = d.raid_id
				inner join Teams t
				on d.distance_id = t.distance_id
			where r.raid_id=$RaidId
				and t.team_hide = 0
				and t.team_outofrange = 0
			";
		$Row = CSql::singleRow($sql);
        	// Если указан лимит и он уже достигнут или превышен и команда "в зачете". то нельзя создавать
		if ($Row['teamslimit'] > 0 && $Row['teamscount'] >= $Row['teamslimit']) 
		{
			return 1;
		} else {
			return 0;
		}
	}
	// Конец проверки лимита заявок

	// Функция находит первую команду в спсике ожидания	
	function FindFirstTeamInWaitList($RaidId)
	{
		return(0);
/*
		 // Получаем информацию о лимите и о зарегистированных командах
		$sql = "select t.team_id
			from Raids r 
				inner join Distances d
				on r.raid_id = d.raid_id
				inner join Teams t
				on d.distance_id = t.distance_id
			where r.raid_id=$RaidId
				and t.team_hide = 0
				and t.team_outofrange = 1
				and t.team_waitdt is not null
			order by  t.team_waitdt ASC
			LIMIT 0,1
			";
		$Row = CSql::singleRow($sql);
		return $Row['team_id'];
		*/
	}
	// Конец поиска первой командлы в списке ожидания
	

	// Функция перводит все команды из списка ожидания вне зачета
	function ClearWaitList($RaidId)
	{
/*
		$sql = " update  Teams t
                	inner join
				(
				select t.team_id
				from Raids r 
					inner join Distances d
					on r.raid_id = d.raid_id
					inner join Teams t
					on d.distance_id = t.distance_id
				where r.raid_id=$RaidId
					and t.team_hide = 0
					and t.team_outofrange = 1
					and t.team_waitdt is not null
				) a
			on t.team_id = a.team_id
			set  t.team_waitdt = NULL, t.team_outofrange = 1
			";
		$rs = MySqlQuery($sql);
*/
		return;
	}
	// Конец поиска первой командлы в списке ожидания
	

	function mmb_validate($var, $key, $default = "")
	{
		return isset($var[$key]) ? $var[$key] : $default;
	}

	// returns value on success, false otherwise
	function mmb_validateInt($var, $key, $default = 0)
	{
		$val = mmb_validate($var, $key, $default);
		return is_numeric($val) ? ((int)$val) : false;
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
	const MailingInterval = 300; // seconds. i.e. 5 minutes

	protected static $enabled = false;
	protected static $records = array();
	
	protected static $sqlConn = null;
	protected static $minLevelCode = null;
	protected static $fatalErrorMail = null;
	protected static $fatalLogFile = null;
	protected static $timestampFile = null;

	const Trace = 	'trace';
	const Debug = 	'debug';
	const Info = 	'info';
	const Warn = 	'warning';
	const Error = 	'error';
	const Critical = 'critical';

	public static function enable($on)
	{
		self::$enabled = ($on == true) ? true : false;
	}

	public static function addRecord($record)
	{
		if (self::$enabled && !empty($record))
			self::$records[] = $record;
	}

	public static function addInterval($text, $stTime)
	{
		$en = microtime(true);
		if (self::$enabled)
			self::addRecord("$text: " . round($en - $stTime, 5));
		return $en;
	}

	public static function getText($asHtml = true)
	{
		if (!$asHtml)
			return implode("\r\n", self::$records);

		$res = '';
		foreach (self::$records as $rec)
			$res .= CMmbUI::toHtml($rec) . "<br/>";

		return $res;
	}

	public static function t($operation, $message, $user = null)
	{
		self::addLogRecord($user, self::Trace, $operation, $message);
	}
	public static function d($operation, $message, $user = null)
	{
		self::addLogRecord($user, self::Debug, $operation, $message);
	}
	public static function i($operation, $message, $user = null)
	{
		self::addLogRecord($user, self::Info, $operation, $message);
	}
	public static function w($operation, $message, $user = null)
	{
		self::addLogRecord($user, self::Warn, $operation, $message);
	}
	public static function e($operation, $message, $user = null)
	{
		self::addLogRecord($user, self::Error, $operation, $message);
	}
	public static function c($operation, $message, $user = null)
	{
		self::addLogRecord($user, self::Critical, $operation, $message);
	}
	
	// fatal.  use, when no sql connection given
	public static function fatal($user, $operation, $message)
	{
		if (self::$fatalErrorMail == null)
			self::initVars();

		global $DBName;
		$dbname = (isset($DBName) && $DBName != null) ? $DBName : '<неизвестна>';
		$user = self::tryGuessUser($user);

		self::updateLogFile(true, self::makeFatalMessage($dbname, $user, $operation, $message));

		$mtime = !empty(self::$timestampFile) ? filemtime(self::$timestampFile) : false;
		if (empty(self::$timestampFile) || $mtime != false && time() > $mtime + self::MailingInterval)		// при массовом сбое отправится столько писем, сколько будет ошибок между этим if и отработкой первого SendMail - updateLogFile
		{
			$mail = self::$fatalErrorMail == null ? 'mmbsite@googlegroups.com' : self::$fatalErrorMail;
			$msg = self::makeFatalMessage($dbname, $user, $operation, $message, true);
			
			if (SendMail(trim($mail), $msg, 'Админы и разработчики', "Критическая ошибка на сайте с базой $dbname"))
				self::updateLogFile(false);
		}
	}

	private static function makeFatalMessage($dbname, $user, $operation, $message, $forMail = false)
	{
		if ($user === null)
			$user = '<неизвестен>';
		if ($operation === null)
			$operation = '<не указана>';
		if ($message === null)
			$message = '<неизвестна>';
		
		$time = date("Y-m-d H:i:s");

		// $stack = debug_print_backtrace(DEBUG_BACKTRACE_PROVIDE_OBJECT);  // think of depth limit. 0 means infinity -- no limits so far. We still run on php 5.3
		$e = new Exception();
		$stack = $e->getTraceAsString();

		if (!$forMail)
		{
			$re = '/[\r\n\t]+/mu';  // gmu worked on test machines
			$msg = preg_replace($re, ' ', $message);
			$stack = preg_replace($re, ' ', $stack);

			return "$time\t$dbname\t$user\t$operation\t$msg\t$stack\r\n";
		}
		
		return "Критическая ошибка при работе с базой '$dbname'!\r\n\r\n"
			."$time\r\n"
			."id пользователя: $user\r\n"
			."Операция: $operation\r\n"
			."Текст ошибки: $message\r\n"
			."\r\nСтек: $stack\r\n\r\n"
			."Cообщения о других критических ошибках ищите в логе: ".self::$fatalLogFile."\r\n\r\n";
	}
	
	private static function addLogRecord($user, $level, $operation, $message)
	{
		if (self::$minLevelCode === null)
			self::initVars();
		if (self::levelCode($level) <  self::$minLevelCode)
			return;

		$conn = self::getConnection();

		$user = self::tryGuessUser($user);
		$uid = ($user == null || !is_numeric($user)) ? 'null' : $user;

		$level = CSql::quote($level);
		$qOperation = $operation == null ? 'null' : CSql::quote($operation);
		$qMessage = $message == null ? 'null' : CSql::quote($message);

		$query = "insert into Logs (logs_level, user_id, logs_operation, logs_message, logs_dt)
                values ($level, $uid, $qOperation, $qMessage, UTC_TIMESTAMP)";

		$rs = mysql_query($query, $conn);	// потому что надо работать со своим соединением :(
		if (!$rs)
		{
			$err = mysql_error($conn);
//			self::fatal($user, $operation, $message);		// sql не работает -- кого волнует исходное сообщение!
			CSql::closeConnection($conn);
			self::$sqlConn = null;
			CSql::dieOnSqlError($user, 'addLogRecord', "adding record: '$query'", $err);
		}
	}

	protected static function tryGuessUser($user)
	{
		global $UserId;
		return ($user == null && isset($UserId)) ? $UserId : $user;
	}

	// add record for log, update modification time for timestamp
	private static function updateLogFile($log, $message = null)
	{
		if ($log)
			$f = fopen(self::$fatalLogFile, "a");
		else
			$f = fopen(self::$timestampFile, "w");

		if ($f != FALSE)
		{
			fwrite($f, $log ? $message : ' ');
			fclose($f);
		}
		else
			die("error opening file. log = '$log', file: " . ($log ? self::$fatalLogFile : self::$timestampFile));
		// syslog otherwise ???
	}

	protected static function getConnection()
	{
		if (self::$sqlConn === null)
		{
			self::initVars();
			self::$sqlConn = CSql::createConnection();
		}

		return self::$sqlConn;
	}

	private static function levelCode($level)
	{
		switch($level)
		{
			case self::Trace: return 0;
			case self::Debug: return 1;
			case self::Info: return 2;
			case self::Warn: return 3;
			case self::Error: return 4;
			case self::Critical: return 5;
			default: return -1;
		}
	}

	// по факту это д.б. статический конструктор
	private static function initVars()
	{
		if (self::$minLevelCode !== null && self::$fatalErrorMail !== null)
			return;

		include("settings.php");

		self::$minLevelCode = self::levelCode($MinLogLevel);
		self::$fatalErrorMail = $FatalErrorMail;

		$catalog = trim($MyLogCatalog);
		self::$fatalLogFile = isset($MyLogDefaultFileName) ? $catalog.trim($MyLogDefaultFileName) : '';	// todo генерировать. например 1 числа текущего месяца
		self::$timestampFile = isset($MyLogTimestampFileName) ? $catalog.trim($MyLogTimestampFileName) : '';
	}
}


// Изменение размера
function image_resize($src, $dst, $width, $height, $crop=0)
{
	if(!list($w, $h) = getimagesize($src)) return "Unsupported picture type!";

	$type = strtolower(substr(strrchr($src,"."),1));
	// инициализируем интерфейс
	switch($type)
	{
		case 'bmp': $image = array('mime' => 'image/bmp',  'create' => 'imagecreatefromwbmp', 'write' => 'imagewbmp'); break;
		case 'gif': $image = array('mime' => 'image/gif',  'create' => 'imagecreatefromgif',  'write' => 'imagegif');  break;
		case 'jpeg':
		case 'jpg': $image = array('mime' => 'image/jpeg', 'create' => 'imagecreatefromjpeg', 'write' => 'imagejpeg'); break;
		case 'png': $image = array('mime' => 'image/png',  'create' => 'imagecreatefrompng',  'write' => 'imagepng');  break;
		default : return "Unsupported picture type!";
	}

	// resize
	if($crop)
	{
		if($w < $width or $h < $height) return "Picture is too small!";
		$ratio = max($width/$w, $height/$h);
		$h = $height / $ratio;
		$x = ($w - $width / $ratio) / 2;
		$w = $width / $ratio;
	}
	else
	{
		if($w < $width and $h < $height) return "Picture is too small!";
		$ratio = min($width/$w, $height/$h);
		$width = $w * $ratio;
		$height = $h * $ratio;
		$x = 0;
	}

	$new = imagecreatetruecolor($width, $height);

	// preserve transparency
	if($type == "gif" or $type == "png")
	{
		imagecolortransparent($new, imagecolorallocatealpha($new, 0, 0, 0, 127));
		imagealphablending($new, false);
		imagesavealpha($new, true);
	}

	$src_image = $image['create']($src);		// а если не откроется?
	imagecopyresampled($new, $src_image, 0, 0, $x, 0, $width, $height, $w, $h);
	imagedestroy($src_image);

	if  (!empty($dst))
		$image['write']($new, $dst);
	else
	{
		header('Content-Type: ' . $image['mime']);
		$image['write']($new);
	}


	// конец разбора, как выводить изображение
	imagedestroy($new);
	return true;
}

?>
