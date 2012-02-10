

<? 



 function MySqlQuery($SqlString,$SessionId,$NonSecure) {
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

	      mysql_free_result($Result); 

              // Записываем время послденей авторизации
	      $Result = MySqlQuery("update Users set user_lastauthorizationdt = now()
	                            where user_id = ".$UserId);

	      mysql_free_result($Result); 



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
       mysql_free_result($Result); 


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
       mysql_free_result($Result); 
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

       mysql_free_result($Result); 

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
		  'CP1251',  // кодировка, в которой находятся передаваемые строки
		  'CP1251', // кодировка, в которой будет отправлено письмо
		  $Subject,
		  $Message);


    return ;
    }



    // Проверка, что текущая сессия принадлежит администратору
    function CheckAdmin($SessionId) {

        if ($SessionId <= 0) 
	{
	  return 0;
	}
   
	$UserId = GetSession($SessionId);

        if ($UserId <= 0) 
	{
	  return 0;
	}

	
	$sql = "select user_admin  
		        from  Users
			where user_hide = 0 and user_id = ".$UserId; 
      //  echo 'sql '.$sql;
	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	mysql_free_result($Result); 
		

    return $Row['user_admin'];
    }


    // Проверка, что текущая сессия принадлежит модератору текущего ММБ
    function CheckModerator($SessionId, $RaidId) {
   
   
        if ($RaidId <= 0) 
	{
	  return 0;
	}

        if ($SessionId <= 0) 
	{
	  return 0;
	}

	$UserId = GetSession($SessionId);

        if ($UserId <= 0) 
	{
	  return 0;
	}
        
	$sql = "select CASE WHEN count(*) > 0 THEN 1 ELSE 0 END as user_moderator 
		        from  RaidModerators
			where raidmoderator_hide = 0 and raid_id = ".$RaidId." and user_id = ".$UserId; 

	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	mysql_free_result($Result); 

    return $Row['user_moderator'];
    }


    // Проверка, что текущая сессия принадлежит участнику команды
    function CheckTeamUser($SessionId, $TeamId) {
   
   
        if ($TeamId <= 0) 
	{
	  return 0;
	}

        if ($SessionId <= 0) 
	{
	  return 0;
	}

	$UserId = GetSession($SessionId);

        if ($UserId <= 0) 
	{
	  return 0;
	}
        
	$sql = "select CASE WHEN count(*) > 0 THEN 1 ELSE 0 END as userinteam 
		        from  TeamUsers tu
			where teamuser_hide = 0 and team_id = ".$TeamId." and user_id = ".$UserId; 

	$Result = MySqlQuery($sql);
	$Row = mysql_fetch_assoc($Result);
	mysql_free_result($Result); 

    return $Row['userinteam'];
    }


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

     // функция пересчитывает результат команды
     function RecalcTeamResult($teamid)
     {
	  // Если хотя бы один этап некорректный = общий резултат - пустой
           // MySql при агрегировании строк функцией SUM() просто игнорирует строик с NULL, но остальные - считает
           //  из-за этого приходится делать повторный запрос с корректировокой
 	   $sql = "update Teams t
			  inner join
			  (
			    select  tl.team_id,
				    SUM(TIME_TO_SEC(timediff(tl.teamlevel_endtime, 
					CASE l.level_starttype 
					    WHEN 1 THEN tl.teamlevel_begtime 
					    WHEN 2 THEN l.level_begtime 
					    WHEN 3 THEN (select MAX(tl2.teamlevel_endtime) 
							 from TeamLevels tl2
							      inner join Levels l2 
							      on tl2.level_id = l2.level_id
							 where tl2.team_id = tl.team_id 
							       and l2.level_order < l.level_order
							) 
					    ELSE NULL 
					END
				      )) + COALESCE(tl.teamlevel_penalty, 0)*60) as team_resultinsec 
			    from  TeamLevels tl 
				  inner join Levels l 
				  on tl.level_id = l.level_id 
			    where tl.teamlevel_hide = 0 and tl.team_id = ".$teamid." 
			    group by  tl.team_id
			   )  a
			  on  t.team_id = a.team_id
		  set t.team_result = SEC_TO_TIME(a.team_resultinsec)";

             // echo $sql;
              MySqlQuery($sql);  

              // Запрос сбрасывает в NULL результаты для команды, у которой, хоть один из этапов даёт NULL
	      $sql = "update Teams t
			  inner join
			  (
			    select  tl.team_id
			    from  TeamLevels tl 
				  inner join Levels l 
				  on tl.level_id = l.level_id 
			    where tl.teamlevel_hide = 0 and tl.team_id = ".$teamid." 
				  and timediff(tl.teamlevel_endtime, 
					CASE l.level_starttype 
					    WHEN 1 THEN tl.teamlevel_begtime 
					    WHEN 2 THEN l.level_begtime 
					    WHEN 3 THEN (select MAX(tl2.teamlevel_endtime) 
							 from TeamLevels tl2
							      inner join Levels l2 
							      on tl2.level_id = l2.level_id
							 where tl2.team_id = tl.team_id 
							       and l2.level_order < l.level_order
							) 
					    ELSE NULL 
					END
				      ) is NULL
			   )  a
			  on  t.team_id = a.team_id
		  set t.team_result = NULL";

             // echo $sql;
              MySqlQuery($sql);  


     }
     // конец функции пересчёта результата команды 
?>

