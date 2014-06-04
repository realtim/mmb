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

   // Удаляем закрытые сессии
  function ClearSessions() {
         // права на  delete у пользователя есть только на  таблицу Sessions
       $Result = MySqlQuery("delete from Sessions where session_status = 1");
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

      // Очищаем таблицу
   //   ClearSessions();
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
		$sql = "select team_id, COALESCE(team_outofrange, 0) as team_outofrange from Teams where team_id = ".$TeamId;
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
		from Raids r where r.raid_id=".$RaidId;
	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
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
	mysql_free_result($Result);


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

function CanUnionRequest($Administrator, $User, $ParentUser)
{

	// Администратор может всегда
	if ($Administrator) return(1);

	// Оба пользователя должны быть определеные
	if (!$User || !$ParentUser) return(0);

        // Тут добавить проверку наличия записей в журнале объединения
}
// Конец проверки возможности объединиться с пользователем


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


     // функция пересчитывает время нахождения команды на этапах
     function RecalcTeamLevelDuration($teamid)
     {
       
       // 20/05/2014 Добавил ограничение на неудаленный этап
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
                                                                         and l2.level_hide = 0 
							      )  as tl3
					      on  tl3.team_id = tl.team_id
						  and  tl3.level_order = l.level_order - 1

				      where tl.teamlevel_hide = 0 
				            and l.level_hide = 0 
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
	$sql = "select l.level_pointpenalties, l.level_discount, l.level_discountpoints,
		       tl.teamlevel_points,
		       tl.teamlevel_id		        
		from TeamLevels tl
		     inner join Levels l
		     on l.level_id = tl.level_id
		where tl.teamlevel_hide = 0 and l.level_hide = 0 and tl.team_id = ".$teamid;

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
                // Добавляем разбор неорбязательных КП 
		$DiscountPoints = explode(',', $Row['level_discountpoints']);
		$Discount = (int)$Row['level_discount'];
		$DiscountPenalty = 0;

		for ($i = 0; $i < $ArrLen; $i++)
		{
                   if (empty($TeamLevelPoints[$i])) 
		   {
		      $NowTeamLevelPoint = 0;
		   } else {
		      $NowTeamLevelPoint = (int)$TeamLevelPoints[$i];
		   }

		   $PenaltyTime += (int)$Penalties[$i]*(1 - $NowTeamLevelPoint);
	   	
		   if ($Discount > 0 and (int)$DiscountPoints[$i] > 0)
		   {
		     $DiscountPenalty += (int)$Penalties[$i]*(1 - $NowTeamLevelPoint);
		   }

		}

                // Если есть амнистия, смотрим, больше ли штраф на необязательных КП, чем амнистия.
		// Если больше штраф - вычитаем амнистию из общего результата, больше амнистия - вычитаем штраф на необязательных КП
		// 
                if ($Discount > 0)
		{
                  if ($DiscountPenalty > $Discount)
		  { 
		   $PenaltyTime =  $PenaltyTime - $Discount;
		  } else {
		   $PenaltyTime =  $PenaltyTime - $DiscountPenalty;
		  }  
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
		 where l.level_hide = 0 and t.team_id = ".$teamid;


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
	    		          inner join Levels l on l.level_id = tl.level_id
			    where tl.teamlevel_hide = 0 and l.level_hide = 0 and tl.team_id = ".$teamid."
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


     // 2014-06-04 Добавил условие, что команда должнабыть в зачете
     // функция вычисляет место команды на этапе (заготовка)
     function GetTeamLevelPlace($teamid, $levelid)
     {
       
       
           // На самом деле  можно вычислять "на лету" хитрым подзапросом, но тут делаем простой вариант
 
        // В отличие от расчета результата здесь важно отсеять удаленные команды, т.к. иначе местобудлет неправильным       
        // Определяем результат на этапе для команды
        $sql = "  		      select (TIME_TO_SEC(COALESCE(tl.teamlevel_duration,0)) + COALESCE(tl.teamlevel_penalty, 0)*60) as result_in_sec
				      from   TeamLevels  tl 
                                             inner join Teams t
                                             on t.team_id = tl.team_id
					     inner join Levels l 
					     on l.level_id = tl.level_id
 				      where tl.teamlevel_hide = 0 
				            and l.level_hide = 0 
                                            and t.team_hide = 0 
					    and COALESCE(t.team_outofrange, 0) = 0
					    and tl.teamlevel_progress = 2 
					    and COALESCE(tl.teamlevel_duration,0) > 0
					    and tl.team_id = ".$teamid."
					    and tl.level_id = ".$levelid;


	$Result  = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
        mysql_free_result($Result);

        $TeamLevelResult =  $Row['result_in_sec'];
        $TeamLevelPlace = 0;
        if ($TeamLevelResult > 0) {

	        // Смотрим сколько команд имеют результат лучше и прибавляем 1
	        // Нельзя ставить <=, т.к. на одном месте может быть несколько команд
		$sql_place = "  	      select  count(*) + 1 as result_place
					      from   TeamLevels  tl 
			                             inner join Teams t
			                             on t.team_id = tl.team_id
						     inner join Levels l 
						     on l.level_id = tl.level_id
					      where tl.teamlevel_hide = 0 
			                            and t.team_hide = 0 
						    and COALESCE(t.team_outofrange,0) = 0
						    and l.level_hide = 0 
						    and tl.teamlevel_progress = 2 
						    and COALESCE(tl.teamlevel_duration,0) > 0
			                            and (TIME_TO_SEC(COALESCE(tl.teamlevel_duration,0)) + COALESCE(tl.teamlevel_penalty, 0)*60) < ". $TeamLevelResult."
						    and tl.level_id = ".$levelid;

		//	echo $sql_place;

		$Result_place  = MySqlQuery($sql_place);
		$Row_place = mysql_fetch_assoc($Result_place);
		mysql_free_result($Result_place);
		$TeamLevelPlace  = (int)$Row_place['result_place'];
        }
        return ($TeamLevelPlace);
     }
     // конец функции расчета места команды на этапе



     // функция вычисляет место команды в общем зачёте
     function GetTeamPlace($teamid)
     {
       
         // Здесь не проверяется прогресс команды,т.е. делается предположение (см. код расчета результата), что результат только для финишировавших команд
	 // если это будет не так, то и алгоритм здесь нужно менять.
        $sql = "  		      select TIME_TO_SEC(COALESCE(t.team_result,0)) as result_in_sec, t.distance_id 
				      from   Teams t
 				      where  t.team_hide = 0 
    					     and COALESCE(t.team_outofrange, 0) = 0
					     and COALESCE(t.team_result,0) > 0
					     and t.team_id = ".$teamid;

	    //    echo $sql;

	$Result  = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
        mysql_free_result($Result);

        
        $TeamResult =  $Row['result_in_sec'];
        $DistanceId =  $Row['distance_id'];

        $TeamPlace = 0;
        if ($TeamResult > 0 and  $DistanceId > 0) {
	        // Смотрим сколько команд имеют результат лучше и прибавляем 1
	        // Нельзя ставить <=, т.к. на одном месте может быть несколько команд
		$sql_place = "  	      select  count(*) + 1 as result_place
					      from   Teams  t 
					      where t.team_hide = 0 
					            and t.distance_id = ".$DistanceId."
					            and COALESCE(t.team_outofrange, 0) = 0
						    and COALESCE(t.team_result,0) > 0
	                                            and TIME_TO_SEC(COALESCE(t.team_result,0)) < ".$TeamResult;
	   //     echo $sql_place;

		$Result_place  = MySqlQuery($sql_place);
		$Row_place = mysql_fetch_assoc($Result_place);
	        mysql_free_result($Result_place);
		$TeamPlace = (int)$Row_place['result_place'];
		
	}
        return ($TeamPlace);
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
        // Конец очистик специальных массивов от возможных инъекций

     // функция получает ссылку на  логотип
     function GetMmbLogo($raidid)
     {
    
        // Данные берём из settings
	include("settings.php");
    
         $LogoLink = "";      

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
		where  raid_id = ".$raidid."
		LIMIT 0,1 ";
	
	    // 08.12.2013 Ищем ссылку на логотип  
        $sqlFile = "select rf.raidfile_name
	     from RaidFiles rf
	     where rf.raid_id = ".$raidid." and rf.filetype_id = 2 
	     order by rf.raidfile_id desc
	     LIMIT 0,1";

	
	}

        $Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	$LogoLink = $Row['raid_logolink'];
	mysql_free_result($Result);


	 
       	$ResultFile = MySqlQuery($sqlFile);  
	$RowFile = mysql_fetch_assoc($ResultFile);
        mysql_free_result($ResultFile);
        $LogoFile =  trim($RowFile['raidfile_name']);

        if ($LogoFile <> '' && file_exists($MyStoreFileLink.$LogoFile))
	{
          $LogoLink = $MyStoreHttpLink.$LogoFile;
        }
        //  Конец получения ссылки на информацию о старте

        return($LogoLink);

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
			  where  distance_id = ".$distanceid."
			         and  levelpoint_hide = 0
				 and  levelpoint_order =  MAX(b.levelpoint_order)
			  LIMIT 0,1
			 ) as predpointtype_id
		 from 
			(
			 select  levelpoint_id, levelpoint_name, levelpoint_order, pointtype_id
			 from LevelPoints lp
			 where distance_id = ".$distanceid."
			       and  levelpoint_hide = 0
			) a
			left outer join
			(
			 select  levelpoint_id, levelpoint_name, levelpoint_order, pointtype_id
			 from LevelPoints lp
			 where distance_id = ".$distanceid."
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

        $Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	$LevelPointId = $Row['levelpoint_id'];
	$LevelPointName = $Row['levelpoint_name'];
	mysql_free_result($Result);

        if (!empty($LevelPointId))
	{
	  $CheckString = "Некорректность в точке ".$LevelPointName;
	}
	
	$sql = " select lpd.levelpointdiscount_id, lpd.levelpointdiscount_start, lpd.levelpointdiscount_finish 
	         from LevelPoints lp
		      inner join  LevelPointDiscounts lpd
		      on lp.levelpoint_order >= lpd.levelpointdiscount_start
		         and lp.levelpoint_order <= lpd.levelpointdiscount_finish
	         where lp.levelpoint_hide = 0  
		       and lp.distance_id = ".$distanceid."
		       and lpd.distance_id = ".$distanceid."
	               and lp.pointtype_id in (1,2,4) 
		       and lpd.levelpointdiscount_hide = 0";
				 
	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	$LevelPointId = $Row['levelpointdiscount_id'];
	$LevelPointDiscountStart = $Row['levelpointdiscount_start'];
	$LevelPointDiscountFinish = $Row['levelpointdiscount_finish'];
	mysql_free_result($Result);

        if (!empty($LevelPointId))
	{
	  $CheckString .= "Некорректность в интервале ".$LevelPointDiscountStart." - ".$LevelPointDiscountFinish;
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
	mysql_free_result($rs);
	if ($Comment == '') {
	 $Comment = "&nbsp;";
	}
        return ($Comment);
     }
     // конец функции получения общего комментария для команды 


?>
