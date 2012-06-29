<?php
// +++++++++++ Библиотека функций +++++++++++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;


 function MySqlQuery($SqlString, $SessionId = "", $NonSecure = "") {
 // Можно передавать соединение по ссылке &$ConnectionId  MySqlQuery($SqlString,&$ConnectionId, $SessionId,$NonSecure);
 //
 // вызов  MySqlQuety('...',&$ConnectionId, ...);

   $NewConnection = 0;
   if (empty($ConnectionId))
   {
	$NewConnectionId = 1;
    
        // Данные берём из settings
	include("settings.php");
	
    	$ConnectionId = mysql_connect($ServerName, $WebUserName, $WebUserPassword);

         // Ошибка соединения
         if ($ConnectionId <= 0)
	 {
	    echo mysql_error();
            die(); 
	    return -1; 
	 }

	 //  устанавливаем временную зону
	 mysql_query('set time_zone = \'+4:00\'', $ConnectionId);  
	 //  устанавливаем кодировку для взаимодействия
	 mysql_query('set names \'utf8\'', $ConnectionId);  
         // Выбираем БД ММБ

     //    echo $DBName;
	 
	 $rs = mysql_select_db($DBName, $ConnectionId);

	 if (!$rs)
	 {
	    echo mysql_error();
	    die(); 
	    return -1; 
	 }
	 
   }
 
  // echo $ConnectionId;
   
   $rs = mysql_query($SqlString, $ConnectionId);  
 
 
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
	mysql_close($ConnectionId);
   } 

   return $rs;	
 
 }


  function StartSession($UserId) {

      if ($UserId > 0) 
      {
	      $SessionId = uniqid();
	      $Result = MySqlQuery("insert into  Sessions (session_id, user_id, session_starttime, session_updatetime, session_status)
	                            values ('".$SessionId ."',".$UserId.", now(), now(), 0)");

              // Записываем время послденей авторизации
	      $Result = MySqlQuery("update Users set user_lastauthorizationdt = now()
	                            where user_id = ".$UserId);
      }  else {
          $SessionId = '';
      }   

      return $SessionId;

  }


   // Закрываем неактивные сессии
  function CloseInactiveSessions($TimeOutInMinutes) {
  //  $TimeOut Время в минутах с последнего обновления, для которого закрываются сессии 
  
        // м.б. потом ещё нужно будет закрывать открытые соединения с БД
       $Result = MySqlQuery("update  Sessions set session_status = 1 
			    where session_status = 0 and session_updatetime < date_add(now(), interval -".$TimeOutInMinutes." MINUTE)");
      return;
  }

  // Получаем данные сессии
  function GetSession($SessionId) {


      if (empty($SessionId))
      {
        return 0;
      } 

      // Закрываем все сессии, которые неактивны 20 минут
      CloseInactiveSessions(20);
      
  //   echo $SessionId;
      $Result = MySqlQuery("select user_id, connection_id, session_updatetime, session_starttime
                            from   Sessions 
			    where session_id = '".$SessionId ."'");

      $Row = mysql_fetch_assoc($Result);
      mysql_free_result($Result); 

      // Тут нужна проверка на превышение времени, на отсутствие сессии и т.п.
      
      $UserId = $Row['user_id'];

      // Обновляем время сессии, если всё ок
      if ($UserId > 0)
      {
       $Result = MySqlQuery("update  Sessions set session_updatetime = now()
			    where session_status = 0 and session_id = '".$SessionId ."'");
      }
      
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
       $Result = MySqlQuery("update  Sessions set session_updatetime = now(), session_status = ".$CloseStatus."
			    where session_status = 0 and session_id = '".$SessionId ."'");

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
	//$Headers = 'From: mmb@progressor.ru' . "\r\n" .
	//		'Reply-To: mmb@progressor.ru' . "\r\n" .
	//	    'X-Mailer:  /';
   
//           mail($Email, 'Информация с сайта ММБ', $Message, $Headers);

    send_mime_mail('mmbsite',
		  'mmb@progressor.ru',
		  $ToName,
		  $Email,
		  'UTF-8',  // кодировка, в которой находятся передаваемые строки
		  'UTF-8', // кодировка, в которой будет отправлено письмо
		  $Subject,
		  $Message);


    return ;
    }

// ----------------------------------------------------------------------------
// Инициализация всех переменных, отвечающих за уровни доступа

function GetPrivileges($SessionId, &$RaidId, &$TeamId, &$UserId, &$Administrator, &$TeamUser, &$Moderator, &$OldMmb, &$RaidStage)
{
	// Инициализируем переменные самым низким уровнем доступа
	$UserId = 0;
	$Administrator = 0;
	$TeamUser = 0;
	$Moderator = 0;
	$OldMmb = 0;
	$RaidStage = 0;

	$UserId	= GetSession($SessionId);

	// Проверяем, не является ли пользователь администратором
	if ($UserId > 0)
	{
		$sql = "select user_admin from Users where user_hide = 0 and user_id = ".$UserId;
		$Result = MySqlQuery($sql);
		if (!$Result) return;
		$Row = mysql_fetch_assoc($Result);
		$Administrator = $Row['user_admin'];
		mysql_free_result($Result);
	}

	// Контролируем, что команда есть в базе
	if ($TeamId > 0)
	{
		$sql = "select team_id from Teams where team_id = ".$TeamId;
		$Result = MySqlQuery($sql);
		if (mysql_num_rows($Result) == 0) $TeamId = 0;
		mysql_free_result($Result);
	}

	// Проверяем, является ли пользователь членом команды
	if (($UserId > 0) && ($TeamId > 0))
	{
		$sql = "select CASE WHEN count(*) > 0 THEN 1 ELSE 0 END as userinteam
				from TeamUsers tu
			where teamuser_hide = 0 and team_id = ".$TeamId." and user_id = ".$UserId;
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
		$TeamUser = $Row['userinteam'];
		mysql_free_result($Result);
	}

	// Если известна команда, то все дальнейшие действия проводим с тем ММБ,
	// в который записана команда
	if ($TeamId > 0)
	{
		$sql = "select raid_id from Distances d
				inner join Teams t on t.distance_id = d.distance_id
			where t.team_id = ".$TeamId;
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
		$RaidId = (int)$Row['raid_id'];
		mysql_free_result($Result);
	}

	// Контролируем, что маршбросок существует в базе
	if ($RaidId > 0)
	{
		$sql = "select raid_id from Raids where raid_id = ".$RaidId;
		$Result = MySqlQuery($sql);
		if (mysql_num_rows($Result) == 0) $RaidId = 0;
		mysql_free_result($Result);
	}

	// Если неизвестен маршбросок
	// то модератор и период маршброска считаются по умолчанию
	if ($RaidId <= 0) return;

	// Проверяем, является ли пользователь модератором марш-броска
	if ($UserId > 0)
	{
		$sql = "select CASE WHEN count(*) > 0 THEN 1 ELSE 0 END as user_moderator
			from RaidModerators
			where raidmoderator_hide = 0 and raid_id = ".$RaidId." and user_id = ".$UserId;
		$Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
		$Moderator = $Row['user_moderator'];
		mysql_free_result($Result);
	}

	// Определяем, проводился ли марш-бросок до 2012 года
	$sql = "select CASE WHEN raid_registrationenddate is not null and YEAR(raid_registrationenddate) <= 2011
			THEN 1
			ELSE 0
		END as oldmmb
		from Raids where raid_id = ".$RaidId;
	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	$OldMmb = $Row['oldmmb'];
	mysql_free_result($Result);

	// RaidStage указывает на то, на какой временной стадии находится ммб
	// 0 - raid_registrationenddate IS NULL, марш-бросок не показывать
	// 1 - raid_registrationenddate еще не наступил
	// 2 - raid_registrationenddate наступил, но первый этап не стартовал
	// 3 - первый этап стартовал, финиш еще не закрылся
	// 4 - финиш закрылся, raid_closedate IS NULL или не наступил
	// 5 - raid_closedate наступил
	$sql = "select
		CASE
			WHEN r.raid_registrationenddate IS NULL THEN 0
			WHEN r.raid_registrationenddate > NOW() THEN 1
			ELSE 2
		END as registration,
		(select count(*) from Levels l
			inner join Distances d on l.distance_id = d.distance_id
			where (d.raid_id = r.raid_id) and (NOW() >= l.level_begtime))
		as started,
		(select count(*) from Levels l
			inner join Distances d on l.distance_id = d.distance_id
			where (d.raid_id = r.raid_id) and (NOW() < l.level_endtime))
		as notfinished,
		CASE
			WHEN (r.raid_closedate IS NULL) OR (NOW() < r.raid_closedate) THEN 0
			ELSE 1
		END as closed
		from Raids r where r.raid_id=".$RaidId;
	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	if ($Row['registration'] == 0) $RaidStage = 0;
	elseif ($Row['registration'] == 1) $RaidStage = 1;
	else
	{
		if ($Row['started'] == 0) $RaidStage = 2;
		elseif ($Row['notfinished'] > 0) $RaidStage = 3;
		else
		{
			if ($Row['closed'] == 0) $RaidStage = 4;
			else $RaidStage = 5;
		}
	}
	mysql_free_result($Result);
}

// ----------------------------------------------------------------------------
// Проверка возможности создавать команду

function CanCreateTeam($Administrator, $Moderator, $OldMmb, $RaidStage)
{
	// Если марш-бросок еще не открыт - никаких созданий команд
	if ($RaidStage == 0) return(0);

	// Администратор может всегда
	if ($Administrator) return(1);

	// Если марш-бросок закрыт через raid_closedate - остальным нельзя
	// (включая модераторов)
	if ($RaidStage == 5) return(0);

	// В старом марш-броске можно всем, если он открыт через raid_closedate
	if ($OldMmb) return(1);

	// Модератор может до закрытия редактирования через raid_closedate
	if ($Moderator && ($RaidStage < 5)) return(1);

	// Обычные пользователи могут до начала марш-броска
	if ($RaidStage < 3) return(1); else return(0);
}

// ----------------------------------------------------------------------------
// Проверка возможности редактировать команду

function CanEditTeam($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage)
{
	// Если марш-бросок еще не открыт - никаких редактирований
	if ($RaidStage == 0) return(0);

	// Администратор может всегда
	if ($Administrator) return(1);

	// Модератор может до закрытия редактирования через raid_closedate
	if ($Moderator && ($RaidStage < 5)) return(1);

	// Посторонний участник не может никогда
	if (!$TeamUser) return(0);

	// Здесь и ниже остались только члены команды

	// В старом марш-броске можно, если он открыт через raid_closedate
	if ($OldMmb && ($RaidStage < 5)) return(1);

	// А в обычном только до начала марш-броска
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
	if (($Administrator || $Moderator) && ($RaidStage > 2)) return(1);

	// Все остальные могут после финиша марш-броска
	if ($RaidStage > 3) return(1); else return(0);
}

// ----------------------------------------------------------------------------
// Проверка возможности редактировать результаты

function CanEditResults($Administrator, $Moderator, $TeamUser, $OldMmb, $RaidStage)
{
	// Если марш-бросок еще не открыт - никаких редактирований
	if ($RaidStage == 0) return(0);

	// Администратор может всегда
	if ($Administrator) return(1);

	// Посторонний участник, не являющийся модератором, не может никогда
	if (!$TeamUser && !$Moderator) return(0);

	// После наступления raid_closedate нельзя
	if ($RaidStage == 5) return(0);

	// В старом марш-броске можно всегда
	if ($OldMmb) return(1);

	// Модератор может после старта марш-броска
	if ($Moderator && ($RaidStage > 2)) return(1);

	// Члены команды могут после финиша марш-броска
	// и до закрытия через raid_closedate
	if ($RaidStage > 3) return(1); else return(0);
}

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
		  . ' <' . $email_to . '>';
      $subject = mime_header_encode($subject, $data_charset, $send_charset);
      $from =  mime_header_encode($name_from, $data_charset, $send_charset)
                     .' <' . $email_from . '>';
      if($data_charset != $send_charset) 
      {
	$body = iconv($data_charset, $send_charset, $body);
      }
      $headers = "From: $from\r\n";
      $type = ($html) ? 'html' : 'plain';
      $headers .= "Content-type: text/$type; charset=$send_charset\r\n";
      $headers .= "Mime-Version: 1.0\r\n";

      return mail($to, $subject, $body, $headers);
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


     // функция пересчитывает время нахождения команды на этапах
     function RecalcTeamLevelDuration($teamid)
     {
       
           // Несколько тонкостей: timediff возвращает тип time - он автоматом не преобразуется в daettime, поэтому
           // поле teamlevel_duration  должно иметь формат TIME_TO_SEC
           // Похоже, что в MySql UPDATE не может содержать WHERE  - приходится убирать в подзапрос 
           // Нельзя применять SUM к типу TIME и DATETIME
           
	$sql = "  update TeamLevels tl0
			  inner join 
				    (select   tl.teamlevel_id, timediff(tl.teamlevel_endtime, 
									  CASE l.level_starttype 
									    WHEN 1 THEN tl.teamlevel_begtime 
									    WHEN 2 THEN l.level_begtime 
									    WHEN 3 THEN tl3.teamlevel_endtime
									    ELSE NULL 
									  END
									)  as duration
				      from    TeamLevels  tl 
					      inner join Levels l 
					      on tl.level_id = l.level_id 
					      left outer join  (select tl2.team_id,  l2.level_order,  tl2.teamlevel_endtime 
								from TeamLevels tl2
								      inner join Levels l2 
								      on tl2.level_id = l2.level_id
                                                                     
							      )  as tl3
					      on  tl3.team_id = tl.team_id
						  and  tl3.level_order = l.level_order - 1

				      where tl.teamlevel_hide = 0 
					    and tl.teamlevel_progress = 2 
					    and tl.teamlevel_endtime > 0
					    and tl.team_id = ".$teamid."
				  ) as a
			  on tl0.teamlevel_id = a.teamlevel_id
		  SET teamlevel_duration = a.duration ";
				    
	$rs = MySqlQuery($sql);


     }
     // конец функции пересчёта времени нахождения 


      // функция пересчитывает штраф команды на этапах
     function RecalcTeamLevelPenalty($teamid)
     {
       
	// Получаем информацию об этапах, которые могла проходить команда
	$sql = "select l.level_pointpenalties,
		       tl.teamlevel_points,
		       tl.teamlevel_id
		from TeamLevels tl
			inner join Levels l on l.level_id = tl.level_id
		where tl.teamlevel_hide = 0 and tl.team_id = ".$teamid;

	$rs = MySqlQuery($sql);

	// ================ Цикл обработки данных по этапам
	$statustext = "";
	while ($Row = mysql_fetch_assoc($rs))
	{
		$TeamLevelId = $Row['teamlevel_id'];

		// Получаем отметки о невзятых КП переводим его в строку и считаем штраф
		$ArrLen = count(explode(',', $Row['level_pointpenalties']));
		$Penalties = explode(',', $Row['level_pointpenalties']);
		$TeamLevelPoints = explode(',', $Row['teamlevel_points']);
		$PenaltyTime = 0;
		for ($i = 0; $i < $ArrLen; $i++)
		{
  		   $PenaltyTime += (int)$Penalties[$i]*(1 - (int)$TeamLevelPoints[$i]);
		}

		$sql = "update TeamLevels set 	teamlevel_penalty = ".$PenaltyTime."
				where teamlevel_id = ".$TeamLevelId;
		MySqlQuery($sql);

         }
	// Конец цикла по этапам
	mysql_free_result($rs);

     }
     // конец функции пересчёта штрафа 

     // функция пересчитывает результат команды
     function RecalcTeamResult($teamid)
     {


	//Новый вариант с учетом _duratgion
        //Считаем число этапов на дистанцуии (можно вынести этот запрос и передавать число этапов, как параметр)

	$sql = " select count(l.level_id) as levelscount
		 from Teams t 
		      inner join Distances d 
		      on d.distance_id = t.distance_id
		      inner join Levels l
		      on l.distance_id = d.distance_id
		 where t.team_id = ".$teamid;


	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	$LevelsOnDistanceCount = $Row['levelscount'];
	
	// Запрос можно сделать сразу для всех команд дистанции, если потиом понадобится
	// Итоговый результат пишем только в том случае, если число этапов, по которым внесена информация сопало с общим, на каждом этапе есть положительное время и 
	// команда финишировала на каждом этапе 
	$sql = "update Teams t
			  inner join
			  (
			    select  tl.team_id,
				    SUM(TIME_TO_SEC(COALESCE(tl.teamlevel_duration, 0)) + COALESCE(tl.teamlevel_penalty, 0)*60) as team_resultinsec,
                                   SUM(COALESCE(tl.teamlevel_progress, 0)) as totalprogress,
                                   MIN(COALESCE(tl.teamlevel_duration, 0)) as minduration,
                                   MIN(COALESCE(tl.teamlevel_progress, 0)) as minprogress,
                                   count(*) as levelscount
			    from  TeamLevels tl 
			    where tl.teamlevel_hide = 0 and tl.team_id = ".$teamid."
			    group by  tl.team_id
			   )  a
			  on  t.team_id = a.team_id
		  set t.team_result = CASE WHEN a.levelscount = ".$LevelsOnDistanceCount." and a.minduration > 0 and  a.minprogress = 2 
                                              THEN  SEC_TO_TIME(a.team_resultinsec)
                                              ELSE  NULL
                                          END, 
                      t.team_progress = totalprogress ";


         // echo $sql;
         MySqlQuery($sql);  

     }
     // конец функции пересчёта результата команды 



     // функция вычисляет место команды на этапе (заготовка)
     function GetTeamLevelPlace($teamid, $levelid)
     {
       
           // На самом деле  можно вычислять "на лету" хитрым подзапросом, но тут делаем простой вариант
 
        // В отличие от расчета результата здесь важно отсеять удаленные команды, т.к. иначе местобудлет неправильным       
        // Определяем результат на этапе для команды
        $sql = "  		      select (TIME_TO_SEC(COALESCE(tl.teamlevel_duration,0)) + COALESCE(tl.teamlevel_penalty, 0)*60) as result_in_sec
				      from    TeamLevels  tl 
                                             inner join Teams t
                                             on t.team_id = tl.team_id
				      where tl.teamlevel_hide = 0 
                                           and t.team_hide = 0 
					    and tl.teamlevel_progress = 2 
					    and COALESCE(tl.teamlevel_duration,0) > 0
					    and tl.team_id = ".$teamid."
					    and tl.level_id = ".$levelid;

	$Result  = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
        mysql_free_result($Result);

        $TeamLevelResult =  $Row['result_in_sec'];

        // Смотрим сколько команд имеют такой же результат или лучше   
	$sql_place = "  	      select  count(*) as result_place
				      from    TeamLevels  tl 
                                             inner join Teams t
                                             on t.team_id = tl.team_id
				      where tl.teamlevel_hide = 0 
                                           and t.team_hide = 0 
					    and tl.teamlevel_progress = 2 
					    and COALESCE(tl.teamlevel_duration,0) > 0
                                           and (TIME_TO_SEC(COALESCE(tl.teamlevel_duration,0)) + COALESCE(tl.teamlevel_penalty, 0)*60) <= ". $TeamLevelResult."
					    and tl.level_id = ".$levelid;

	$Result_place  = MySqlQuery($sql_place);
	$Row_place = mysql_fetch_assoc($Result_place);
        mysql_free_result($Result_place);

        return ((int)$Row_place['result_place']);
     }
     // конец функции расчета места команды


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
        // Конец очистик специальных массивов от возможных инъекций

?>
