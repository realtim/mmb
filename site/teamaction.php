<?php
   // Обработчик действий, связанных с командой   

   
// Для всех обработок кроме просмотра результатов команды (а может и для неё)
// требуем, чтобы пользователь вошёл в систему
   

	   if (empty($SessionId))
	   {
           $SessionId =  $_POST['sessionid'];
	   } 

	   $UserId = GetSession($SessionId);
/*
	   if (empty($UserId))
	   {
		//$statustext = "Не определён пользователь.";
	       // $alert = 0; 
		//return; 
	   }
*/
	   if (empty($TeamId))
	   {
             $TeamId = 0;
	   }  
	   if (empty($RaidId))
	   {
             $RaidId = 0;
	   }  

   

   if ($action == "RegisterNewTeam")  {
    // обработка регистрации команды
        $view = "ViewTeamData";
        $viewmode = "Add";	
	//$statustext = "Пользователь: ".$UserId.", сессия: ".$SessionId;


	   if (empty($_POST['RaidId']))	   
	   {
		$statustext = "Не указан ММБ (выберите из списка).";
	        $alert = 0; 
		return; 
	   }

           // Находим ММБ и получаем информацию о нём, необходимую для проверки возможности регистрировтаь команду
	   	   $sql = "select raid_id, raid_name, raid_registrationbegdate, raid_registrationbegdate, 
		                  raid_registrationenddate, raid_resultpublicationdate, now() as nowdt,
				  CASE WHEN raid_registrationenddate is not null and YEAR(raid_registrationenddate) <= 2011 
				      THEN 1 
				      ELSE 0 
				  END as oldmmb 
		           from  Raids where  raid_id = ".$_POST['RaidId'];
           //echo $sql;
	   $rs = MySqlQuery($sql);  
           $Row = mysql_fetch_assoc($rs);
           mysql_free_result($rs);      
           $RaidId =  $Row['raid_id'];
   	   $RaidPublicationResultDate = $Row['raid_resultpublicationdate'];
           $RaidRegistrationEndDate = $Row['raid_registrationenddate'];
           //echo $RaidId.' '.$RaidRegistrationEndDate;
           $OldMmb = $Row['oldmmb'];
 	   $NowDt = $row['nowdt'];
 
	   if (empty($RaidId) or empty($RaidRegistrationEndDate))
           {
            $statustext = "ММБ не найден";
	    $alert = 0;		
	    return;
	   }

           // Проверка, что пользователь не включен в команду на выбранном ММБ 
	   $sql = "select t.team_num 
	           from  TeamUsers tu 
		         inner join  Teams  t on  tu.team_id = t.team_id 
		         inner join  Distances d on  t.distance_id = d.distance_id
		   where d.raid_id = '.$RaidId.' and  tu.teamuser_hide = 0 and tu.user_id = ".$UserId;
	   $rs = MySqlQuery($sql);  
           $row = mysql_fetch_assoc($rs);
           mysql_free_result($rs);      
           $TeamNum =  $row['team_num'];


	   if ($TeamNum > 0)
	   {
            $statustext = "Уже есть команда c Вашим участием (N ".$row['team_num'].")";
	    $alert = 0;		
	    return;
	   }

         //   echo 'ok';

   } elseif  ($action == 'TeamChangeData' or $action == "AddTeam")  {
   // Изменение данных команды

       
        if ($action == "AddTeam")
        {
            $viewmode = "Add";
        } else {
	    $viewmode = "";
        } 
 
	$view = "ViewTeamData";

           // пока валим всё в одну кучу - проверяем ниже
           $pDistanceId = $_POST['DistanceId'];
           $RaidId = $_POST['RaidId'];
           $pTeamNum = (int) $_POST['TeamNum'];
           $pTeamName = $_POST['TeamName'];
           $pTeamUseGPS = ($_POST['TeamUseGPS'] == 'on' ? 1 : 0);
           $pTeamMapsCount = $_POST['TeamMapsCount'];
           $pTeamGreenPeace = ($_POST['TeamGreenPeace'] == 'on' ? 1 : 0);
           $pTeamConfirmResult = ($_POST['TeamConfirmResult'] == 'on' ? 1 : 0);
           $pModeratorConfirmResult = ($_POST['ModeratorConfirmResult'] == 'on' ? 1 : 0);
           $pNewTeamUserEmail = $_POST['NewTeamUserEmail'];
           $pTeamNotOnLevelId = (int)$_POST['TeamNotOnLevelId'];

          //  echo $pTeamUseGPS;

	   if ($action <> "AddTeam" and $TeamId <= 0)
	   {
		$statustext = "Не найден идентификатор команды.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
		return; 
	   }  

	   if (empty($pDistanceId))
	   {
		$statustext = "Не указана дистанция.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
		return; 
	   }  


	   if (empty($RaidId))
	   {
		$statustext = "Не указан ММБ.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
		return; 
	   }  

           if (trim($pTeamName) == '')
	   {
		$statustext = "Не указано название.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
		return; 
	   }

           if ($pTeamMapsCount <= 0 or $pTeamMapsCount > 15)
	   {
		$statustext = "Не указано число карт или недопустимое число карт.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
		return; 
	   }


           $sql = "select count(*) as resultcount 
                   from  Teams t
                         inner join Distances d
                         on t.distance_id = d.distance_id 
                    where d.raid_id = ".$RaidId." and trim(team_name) = '".$pTeamName."' and team_hide = 0 and team_id <> ".$TeamId;
           //echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
           mysql_free_result($rs);      
	   if ($Row['resultcount'] > 0)
	   {
   		$statustext = "Уже есть команда с таким названием.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
                return; 
	   }

           // проверяем номер команды: если новая - 0 и такого номера не должно быть
           $sql = "select count(*) as resultcount 
                   from  Teams t inner join Distances d on t.distance_id = d.distance_id 
                   where d.raid_id = '.$RaidId.' and  team_num = '.$pTeamNum.' and team_hide = 0 and team_id <> ".$TeamId;
           //echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
           mysql_free_result($rs);      
	   if ($Row['resultcount'] > 0)
	   {
   		$statustext = "Уже есть команда с таким номером.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
                return; 
	   }

    
    
  	   $sql = "select r.raid_resultpublicationdate, r.raid_registrationenddate, 
                        CASE WHEN r.raid_registrationenddate is not null and YEAR(r.raid_registrationenddate) <= 2011 
                             THEN 1 
                             ELSE 0 
                        END as oldmmb
		 from   Raids r 
		 where r.raid_id = ".$RaidId; 
		//echo 'sql '.$sql;
                $Result = MySqlQuery($sql);
		$Row = mysql_fetch_assoc($Result);
                mysql_free_result($Result);
		$RaidPublicationResultDate = $Row['raid_resultpublicationdate'];
                $RaidRegistrationEndDate = $Row['raid_registrationenddate'];
                $OldMmb = $Row['oldmmb'];

	    if (empty($RaidRegistrationEndDate))
	    {
	      // должна быть определена дата окончания регистрации
		return;
	    }

	 
	    if (CheckModerator($SessionId, $RaidId))
	    {
	      $Moderator = 1;
	    } else {
	      $Moderator = 0;
	    }
      
	    if  ($action <> "AddTeam" and CheckTeamUser($SessionId, $TeamId))
	    {
		  $TeamUser = 1;
	    } else {
		  $TeamUser = 0;
	    }



	    if ($OldMmb and $pTeamNum <= 0)
 	    {
   		$statustext = "Для ММБ до 2012 года нужно укзывать номер команды.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
                return; 
	    }


               // echo $pNewTeamUserEmail;
           // Проверяем email нового участника команды
	   if (!empty($pNewTeamUserEmail) and trim($pNewTeamUserEmail) <> 'Email нового участника')
	   {

                 
	        $sql = "select user_id, user_prohibitadd from  Users where ltrim(COALESCE(user_password, '')) <> '' and user_hide = 0 and trim(user_email) = trim('".$pNewTeamUserEmail."')";
		//   echo $sql;
		$rs = MySqlQuery($sql);  
		$Row = mysql_fetch_assoc($rs);
	        mysql_free_result($rs);
                $NewUserId = $Row['user_id']; 
                $UserProhibitAdd = $Row['user_prohibitadd']; 
		
		if (empty($NewUserId))
		{
	                $statustext = 'Пользователь с таким email не найден.';
			$alert = 1;
		        $viewsubmode = "ReturnAfterError"; 
			return;
		}

                // Проверка на запрет включения в команду
		if ($UserProhibitAdd and $NewUserId <> $UserId and !$Moderator)
		{
			$NewUserId = 0;
	                $statustext = 'Пользователь запретил добавлять себя в команду другим пользователям.';
			$alert = 1;
		        $viewsubmode = "ReturnAfterError"; 
			return;
		}

	        $sql = "select count(*) as result 
		        from  TeamUsers tu 
		            inner join  Teams t  on tu.team_id = t.team_id
			    inner join  Distances d  on t.distance_id = d.distance_id
		        where teamuser_hide = 0 and d.raid_id = ".$RaidId." and user_id = ".$NewUserId;

//		echo 's1 '.$sql;
		$rs = MySqlQuery($sql);  
		$Row = mysql_fetch_assoc($rs);
	        mysql_free_result($rs);
                if ($Row['result'] > 0)
		{
			$NewUserId = 0;
	                $statustext = 'Пользователь с таким email уже включен в команду.';
			$alert = 1;
		        $viewsubmode = "ReturnAfterError"; 
			return;
		} 

            } else  {
	    
	        // Проверяем, что для новой команды передали email участника
		if ($action == "AddTeam")
		{
			$NewUserId = 0;
			$statustext = "Для новой команды должен быть указан email участника.";
		        $alert = 1; 
		        $viewsubmode = "ReturnAfterError"; 
	                return; 
		}
		
		$NewUserId = 0;

	    } // Конец проверки на корректную передачу адреса
	    

            // Общая проверка возможности редактирования

	    if ($viewmode == "Add" or $Moderator or ($TeamUser and !$TeamModeratorConfirmResult))
	    {
	      $AllowEdit = 1;
	    } else { 
	      $AllowEdit = 0;
	    }
 
            // Ещё на предыдущем этапе это должно быть выполнено, но на всякий случай проверяем
            if (!$AllowEdit)
            {
   	       $statustext = "запрещённое изменение.";
               return;

	    }

            $TeamActionTextForEmail = "";

	    if ($action == "AddTeam")
	    {
	      // Новая команда
                 
		 $sql = "insert into  Teams (team_num, team_name, team_usegps, team_mapscount, distance_id, 
                                             team_registerdt, team_greenpeace, team_confirmresult, 
                                             team_moderatorconfirmresult, level_id) 
		                       values  (";

                 if ($OldMmb)
                 {
                    $sql = $sql.$pTeamNum;
		 } else {
		    $sql = $sql."	       (
						 select COALESCE(MAX(t.team_num), 0) +1
						 from  Teams t
						      inner join  Distances d on t.distance_id = d.distance_id
						 where d.raid_id = ".$RaidId."
						)";
                 }
                 $sql = $sql.", '".$pTeamName."',".$pTeamUseGPS.",".$pTeamMapsCount.", ".$DistanceId.",
                                NOW(), ".$pTeamGreenPeace.", ".$pTeamConfirmResult.",
                               ".$pModeratorConfirmResult.", ".$pTeamNotOnLevelId." )";


              //   echo $sql;  
                 // При insert должен вернуться послений id - это реализовано в  MySqlQuery
		 $TeamId = MySqlQuery($sql);  
	
//	         echo $TeamId; 
//                 $UserId = mysql_insert_id($Connection);
		 if ($TeamId <= 0)
		 {
                        $statustext = 'Ошибка записи новой команды.';
			$alert = 1;
		        $viewsubmode = "ReturnAfterError"; 
			return;
		 } else {

		         $sql = "insert into  TeamUsers (team_id, user_id) values  (".$TeamId.", ".$NewUserId.")";
			 $TeamUserId = MySqlQuery($sql);  
   
                         $TeamActionTextForEmail = "создана команда";

		   // Теперь нужно открыть на просмотр
		   $viewmode = "";

		 }	     
	

   	    } else {
              // изменения в уже существующей команде

                 $TeamActionTextForEmail = "изменение данных команды";
		 
	         $sql = "update  Teams set team_name = trim('".$pTeamName."'), 
		                              distance_id = ".$pDistanceId.", 
					      team_usegps = ".$pTeamUseGPS.", 
					      team_greenpeace = ".$pTeamGreenPeace.", 
					      team_confirmresult = ".$pTeamConfirmResult.", 
					      team_moderatorconfirmresult = ".$pModeratorConfirmResult.", 
					      level_id = ".$pTeamNotOnLevelId.", 
					      team_mapscount = ".$pTeamMapsCount."
	                 where team_id = ".$TeamId;
         
	        // echo $sql;
	 
                 $rs = MySqlQuery($sql);  
 	         mysql_free_result($rs);

                 // Обработка схода команды в плане влияния на результаты
  		 $sql = "SELECT level_order FROM Levels where level_id = ".$pTeamNotOnLevelId;
                 $rs = MySqlQuery($sql);  
                 $Row = mysql_fetch_assoc($rs);
 	         mysql_free_result($rs);
               

                 if ($Row['level_order'] > 0)
                 {   

		    $sql = "update TeamLevels tl
				   join Levels l1
				   on tl.level_id = l1.level_id 
			    set teamlevel_hide = 0 
			    where team_id = ".$TeamId." 
				  and l1.level_order < ".$Row['level_order'];
                       
		 //   echo $sql;
		    $rs = MySqlQuery($sql);  
		    mysql_free_result($rs);

                    // Очищаем поля дат и КП на тот случай, если после восстановления что-то изменится
                    // Например, граничные условия
		    $sql = "update  TeamLevels tl
				    join Levels l1
				    on tl.level_id = l1.level_id 
                            set teamlevel_hide = 1, 
                                teamlevel_begtime = NULL,
                                teamlevel_endtime = NULL,
                                teamlevel_points = NULL,
                                teamlevel_penalty = NULL,
                                teamlevel_comment = NULL
			    where team_id = ".$TeamId." 
				  and l1.level_order >= ".$Row['level_order'];
                       
		   // echo $sql;
		    $rs = MySqlQuery($sql);  
		    mysql_free_result($rs);


                  } else {

		    $sql = "update  TeamLevels set teamlevel_hide = 0 
			    where team_id = ".$TeamId; 

		   // echo $sql;
		    $rs = MySqlQuery($sql);  
		    mysql_free_result($rs);


                  }
                  // Конец обработки  схода команды и влияния этого на результаты
                


	         // Если добавляли участника
		 if ($NewUserId > 0)
		 {
		   $sql = " insert into  TeamUsers (team_id, user_id) values  (".$TeamId.", ".$NewUserId.")";
  		   //echo $sql;
		   $TeamUserId = MySqlQuery($sql);  

		   $Sql = "select user_name from  Users where user_id = ".$NewUserId;
		   $Result = MySqlQuery($Sql);  
		   $Row = mysql_fetch_assoc($Result);
  	           mysql_free_result($Result);
		   $NewUserName = $Row['user_name'];
		 
		   $TeamActionTextForEmail = "добавлен участник ".$NewUserName;
		 }        


            }
	    // Конец проверки новая или существующая команда

            // Обновляем результат команды (реально нужно только при изменения этапа невыхода команды)
	    if ($UserId > 0 and $TeamId > 0)
	    {
	      RecalcTeamResult($TeamId);
            }

            // Отправить письмо всем участникам команды об изменениях
	    if ($UserId > 0 and $TeamId > 0)
	    {
	    	    $Sql = "select user_name from  Users where user_id = ".$UserId;
		    $Result = MySqlQuery($Sql);  
		    $Row = mysql_fetch_assoc($Result);
    	            mysql_free_result($Result);
		    $ChangeDataUserName = $Row['user_name'];
		   
		    
		    $sql = "select u.user_email, u.user_name, t.team_num, d.distance_name, r.raid_name
		            from  Users u 
			         inner join  TeamUsers tu
				 on tu.user_id = u.user_id
				 inner join  Teams t
				 on tu.team_id =  t.team_id
				 inner join  Distances d
				 on t.distance_id = d.distance_id
				 inner join  Raids r
				 on d.raid_id = r.raid_id
			    where tu.teamuser_hide = 0 and tu.team_id = ".$TeamId."
			    order by  tu.teamuser_id asc"; 
                //echo 'sql '.$sql;
		$Result = MySqlQuery($sql);

		while ($Row = mysql_fetch_assoc($Result))
		{
                   // Формируем сообщение
                   $Msg = "Уважаемый участник ".$Row['user_name']."!\r\n\r\n";
		   $Msg =  $Msg."Действие: ".$TeamActionTextForEmail.".\r\n";
		   $Msg =  $Msg."Команда N ".$Row['team_num'].", Дистанция: ".$Row['distance_name'].", ММБ: ".trim($Row['raid_name']).".\r\n";
		   $Msg =  $Msg."Автор изменений: ".$ChangeDataUserName.".\r\n";
		   $Msg =  $Msg."Вы можете увидеть результат на сайте и при необходимости внести свои изменения.\r\n\r\n";
		   $Msg =  $Msg."P.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
			   
			    
                  // Отправляем письмо
		  SendMail($Row['user_email'], $Msg, $Row['user_name']);
		}
 	        mysql_free_result($Result);

	    }
	    // Конец проверки пользователя вносившего изменения

	    // Если передали альтернативную страницу, на которую переходить (пока только одна возможность - на список команд)
	    $view = $_POST['view'];
	
	    if (empty($view))
	    {
		$view = "ViewTeamData";
	    } 	



   } elseif  ($action == 'FindTeam')  {
   // Информация о команде по номеру
   
	
		$sql = "select team_id 
		        from  Teams t
			     inner join  Distances d on t.distance_id = d.distance_id
		        where d.raid_id = ".$RaidId." and t.team_hide = 0 and t.team_num = ".$TeamNum;
          // echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
           mysql_free_result($rs);
           $TeamId = $Row['team_id'];
	   
		 if ($TeamId <= 0)
		 {
                        
                        $statustext = 'Команда с номером '.$TeamNum.' не найдена.';
			$alert = 1;
			return;


		 }  else {

			$view = "ViewTeamData";
		 }


   } elseif  ($action == 'TeamInfo')  {
   // Информация о команде по Id
   
		 if ($TeamId <= 0)
		 {
                        
                        $statustext = 'Команда не найдена.';
			$alert = 1;
			return;


		 }  else {

			$view = "ViewTeamData";
                        $viewmode = ""; 
		 }


   } elseif  ($action == 'HideTeamUser')  {
   // Удаление участника команды
   
	     $TeamUserId = $_POST['HideTeamUserId'];
	     if ($TeamUserId <= 0)
	     {
	         $statustext = 'Участник не найден.';
		 $alert = 1;
		 return;
	     }	 

	     $TeamId = $_POST['TeamId'];
	     if ($TeamId <= 0)
	     {
	         $statustext = 'Команда не найдена.';
		 $alert = 1;
		 return;
	     }	 

	     $RaidId = $_POST['RaidId'];
	     if ($RaidId <= 0)
	     {
	         $statustext = 'Не найден ММБ.';
		 $alert = 1;
		 return;
	     }	 

	     $SessionId = $_POST['sessionid'];
	     if ($SessionId <= 0)
	     {
	         $statustext = 'Не найдена сесия.';
		 $alert = 1;
		 return;
	     }	 

	      if (CheckModerator($SessionId, $RaidId))
	      {
	        $Moderator = 1;
	      } else {
	        $Moderator = 0;
	      }
      
	      if  (CheckTeamUser($SessionId, $TeamId))
	      {
		  $TeamUser = 1;
	      } else {
		  $TeamUser = 0;
	      }

             // Проверка прав. Если нет - выходим
             if ($Moderator or $TeamUser)
             {
               $AllowEdit = 1;
             } else {
               $AllowEdit = 0;     
                              
               return;
             }


             // смотрим, был ли это последний участник или нет
 	     $sql = "select count(*) as result from  TeamUsers  where teamuser_hide = 0 and team_id = ".$TeamId;
	          // echo $sql;
	     $rs = MySqlQuery($sql);  
	     $Row = mysql_fetch_assoc($rs);
             mysql_free_result($rs);
	     $TeamUserCount = $Row['result'];
             if ($TeamUserCount > 1)
	     {
                // Кто-то ещё остается 
		$sql = "update  TeamUsers set teamuser_hide = 1 where teamuser_id = ".$TeamUserId; 
        	// echo $sql;
	        $rs = MySqlQuery($sql);  
                mysql_free_result($rs);
		
		$view = "ViewTeamData";

		
	     } else {
	       // Это был последний участник
	   
	   
		$sql = "update  TeamUsers set teamuser_hide = 1 where teamuser_id = ".$TeamUserId; 
		$rs = MySqlQuery($sql);  
                mysql_free_result($rs);
		$sql = "update  Teams set team_hide = 1 where team_id = ".$TeamId;
		$rs = MySqlQuery($sql);  
                mysql_free_result($rs);

		$view = "";
	   
	     } // Конец проверки на последнего участника

	     // Отправить письмо всем участникам команды об удалении
	    if ($UserId > 0 and $TeamId > 0)
	    {
	    	    $Sql = "select user_name from  Users where user_id = ".$UserId;
		    $Result = MySqlQuery($Sql);  
		    $Row = mysql_fetch_assoc($Result);
		    $ChangeDataUserName = $Row['user_name'];
		    mysql_free_result($Result);
		    
	    	    $Sql = "select user_name from  Users u inner join  TeamUsers tu on tu.user_id = u.user_id where tu.teamuser_id = ".$TeamUserId;
		    $Result = MySqlQuery($Sql);  
		    $Row = mysql_fetch_assoc($Result);
		    $DelUserName = $Row['user_name'];
		    mysql_free_result($Result);
                    

		    $sql = "select u.user_email, u.user_name, t.team_num, d.distance_name, r.raid_name
		            from  Users u 
			         inner join  TeamUsers tu
				 on tu.user_id = u.user_id
				 inner join  Teams t
				 on tu.team_id =  t.team_id
				 inner join  Distances d
				 on t.distance_id = d.distance_id
				 inner join  Raids r
				 on d.raid_id = r.raid_id
			    where tu.teamuser_id = ".$TeamUserId." or (tu.teamuser_hide = 0 and tu.team_id = ".$TeamId.")
			    order by  tu.teamuser_id asc"; 
                     //echo 'sql '.$sql;
		     $Result = MySqlQuery($sql);

		     if ($TeamUserCount > 1)
		     {
			     while ($Row = mysql_fetch_assoc($Result))
			     {
		                   // Формируем сообщение
			           if (trim($DelUserName) <> trim($Row['user_name']))
				   {
					$Msg = "Уважаемый участник ".$Row['user_name']."!\r\n\r\nИз Вашей команды (N ".$Row['team_num'].", Дистанция: ".trim($Row['distance_name']).", ММБ: ".trim($Row['raid_name']).") был удален участник: ".$DelUserName.".\r\nАвтор изменений: ".$ChangeDataUserName.".\r\nВы можете увидеть результат на сайте и при необходимости внести свои изменения.\r\n\r\nP.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
				   } else {
					$Msg = "Уважаемый участник ".$Row['user_name']."!\r\n\r\nВы были удалены из команды (N ".$Row['team_num'].", Дистанция: ".trim($Row['distance_name']).", ММБ: ".trim($Row['raid_name']).")\r\nАвтор изменений: ".$ChangeDataUserName.".\r\nВы можете увидеть результат на сайте и при необходимости внести свои изменения.\r\n\r\nP.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
				   }	
	 	                   // Отправляем письмо
				   SendMail($Row['user_email'], $Msg, $Row['user_name']);
			     }
		    } else {
			$Row = mysql_fetch_assoc($Result);
	                $Msg = "Уважаемый участник ".$Row['user_name']."!\r\n\r\nВаша команда (N ".$Row['team_num'].", Дистанция: ".trim($Row['distance_name']).", ММБ: ".trim($Row['raid_name']).") была удалена.\r\nАвтор изменений: ".$ChangeDataUserName.".\r\nВы можете увидеть результат на сайте и при необходимости внести свои изменения.\r\n\r\nP.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
	 	        // Отправляем письмо
			SendMail($Row['user_email'], $Msg, $Row['user_name']);
		    }			     
		    mysql_free_result($Result);

	    }
	    // Конец проверки пользователя вносившего изменения


 } elseif  ($action == 'TeamUserOut')  {
   // Смена этапа схода  участника команды
   
	     $TeamUserId = $_POST['HideTeamUserId'];
	     if ($TeamUserId <= 0)
	     {
	         $statustext = 'Участник не найден.';
		 $alert = 1;
		 return;
	     }	 

             // Здесь может быть 0 этап - значит, что участник нигде не сходил
	     $LevelId = $_POST['UserOutLevelId'];
	     if ($LevelId < 0)
	     {
	         $statustext = 'Не найден этап.';
		 $alert = 1;
		 return;
	     }	 

	     $TeamId = $_POST['TeamId'];
	     if ($TeamId <= 0)
	     {
	         $statustext = 'Команда не найдена.';
		 $alert = 1;
		 return;
	     }	 

	     $RaidId = $_POST['RaidId'];
	     if ($RaidId <= 0)
	     {
	         $statustext = 'Не найден ММБ.';
		 $alert = 1;
		 return;
	     }	 

	     $SessionId = $_POST['sessionid'];
	     if ($SessionId <= 0)
	     {
	         $statustext = 'Не найдена сесия.';
		 $alert = 1;
		 return;
	     }	 

	      if (CheckModerator($SessionId, $RaidId))
	      {
	        $Moderator = 1;
	      } else {
	        $Moderator = 0;
	      }
      
	      if  (CheckTeamUser($SessionId, $TeamId))
	      {
		  $TeamUser = 1;
	      } else {
		  $TeamUser = 0;
	      }

             // Проверка прав. Если нет - выходим
             if ($Moderator or $TeamUser)
             {
               $AllowEdit = 1;
             } else {
               $AllowEdit = 0;     
                              
               return;
             }

           
		$sql = "update  TeamUsers set level_id = ".($LevelId > 0 ?  $LevelId : 'null' )." where teamuser_id = ".$TeamUserId; 
        	// echo $sql;
	        $rs = MySqlQuery($sql);  
                mysql_free_result($rs);

	
		$view = "ViewTeamData";

                // Письмо об изменениях		

		    $Sql = "select user_name from  Users where user_id = ".$UserId;
		    $Result = MySqlQuery($Sql);  
		    $Row = mysql_fetch_assoc($Result);
    	            mysql_free_result($Result);
		    $ChangeDataUserName = $Row['user_name'];
		   
		    
		    $sql = "select u.user_email, u.user_name, t.team_num, d.distance_name, r.raid_name
		            from  Users u 
			         inner join  TeamUsers tu
				 on tu.user_id = u.user_id
				 inner join  Teams t
				 on tu.team_id =  t.team_id
				 inner join  Distances d
				 on t.distance_id = d.distance_id
				 inner join  Raids r
				 on d.raid_id = r.raid_id
			    where tu.teamuser_hide = 0 and tu.team_id = ".$TeamId."
			    order by  tu.teamuser_id asc"; 
                //echo 'sql '.$sql;
		$Result = MySqlQuery($sql);

		while ($Row = mysql_fetch_assoc($Result))
		{
                   // Формируем сообщение
                   $Msg = "Уважаемый участник ".$Row['user_name']."!\r\n\r\n";
		   $Msg =  $Msg."Действие: изменение данных команды.\r\n";
		   $Msg =  $Msg."Команда N ".$Row['team_num'].", Дистанция: ".$Row['distance_name'].", ММБ: ".trim($Row['raid_name']).".\r\n";
		   $Msg =  $Msg."Автор изменений: ".$ChangeDataUserName.".\r\n";
		   $Msg =  $Msg."Вы можете увидеть результат на сайте и при необходимости внести свои изменения.\r\n\r\n";
		   $Msg =  $Msg."P.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
			   
		   	    
                  // Отправляем письмо
		   SendMail($Row['user_email'], $Msg, $Row['user_name']);
		}
 	        mysql_free_result($Result);


   } elseif  ($action == 'HideTeam')  {
   // Информация о команде по номеру
     

	     $TeamId = $_POST['TeamId'];
	     if ($TeamId <= 0)
	     {
	         $statustext = 'Команда не найдена.';
		 $alert = 1;
		 return;
	     }	 

	  if ($RaidId <= 0)
	     {
	         $statustext = 'Не найден ММБ.';
		 $alert = 1;
		 return;
	     }	 

	     $SessionId = $_POST['sessionid'];
	     if ($SessionId <= 0)
	     {
	         $statustext = 'Не найдена сесия.';
		 $alert = 1;
		 return;
	     }	 

	      if (CheckModerator($SessionId, $RaidId))
	      {
	        $Moderator = 1;
	      } else {
	        $Moderator = 0;
	      }
      
	      if  (CheckTeamUser($SessionId, $TeamId))
	      {
		  $TeamUser = 1;
	      } else {
		  $TeamUser = 0;
	      }

             // Проверка прав. Если нет - выходим
             if ($Moderator or $TeamUser)
             {
               $AllowEdit = 1;
             } else {
               $AllowEdit = 0;     
                              
               return;
             }



	     if ($UserId > 0 and $TeamId > 0)
	     {
		    $Sql = "select user_name from  Users where user_id = ".$UserId;
		    $Result = MySqlQuery($Sql);  
		    $Row = mysql_fetch_assoc($Result);
		    $ChangeDataUserName = $Row['user_name'];
		    mysql_free_result($Result);
	     }

		    $sql = "select u.user_email, u.user_name, t.team_num, d.distance_name, r.raid_name
		            from  Users u 
			         inner join  TeamUsers tu
				 on tu.user_id = u.user_id
				 inner join  Teams t
				 on tu.team_id =  t.team_id
				 inner join  Distances d
				 on t.distance_id = d.distance_id
				 inner join  Raids r
				 on d.raid_id = r.raid_id
			    where tu.teamuser_hide = 0 and tu.team_id = ".$TeamId."
			    order by  tu.teamuser_id asc"; 
                     //echo 'sql '.$sql;
		     $Result = MySqlQuery($sql);

		     while ($Row = mysql_fetch_assoc($Result))
		     {
	                   // Формируем сообщение
	                   $Msg = "Уважаемый участник ".$Row['user_name']."!\r\n\r\nВаша команда (N ".$Row['team_num'].", Дистанция: ".trim($Row['distance_name']).", ММБ: ".trim($Row['raid_name']).") была удалена.\r\nАвтор изменений: ".$ChangeDataUserName.".\r\n\r\n\r\nP.S. Изменения может вносить любой из участников команды, а также модератор ММБ.";
 	                   // Отправляем письмо
			   SendMail($Row['user_email'], $Msg, $Row['user_name']);
		     }
                     mysql_free_result($Result);

		$sql = "update  TeamUsers set teamuser_hide = 1 where team_id = ".$TeamId; 
		$rs = MySqlQuery($sql);  
                mysql_free_result($rs);
                $sql = "update  Teams set team_hide = 1 where team_id = ".$TeamId;
		$rs = MySqlQuery($sql);  
                mysql_free_result($rs);
		
	      $view = "ViewRaidTeams";

   } elseif ($action == "CancelChangeTeamData")  {
    // Действие вызывается ссылкой Отмена

           $view = "ViewTeamData";

   } elseif ($action == "ViewRaidTeams")  {
    // Действие вызывается ссылкой Отмена

           $view = "ViewRaidTeams";


   } else {
   // если никаких действий не требуется

   //  $statustext = "<br/>";
   }

	//print('view = *'.$view.'* action = '.$action);
   
?>