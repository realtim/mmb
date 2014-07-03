<?php
// +++++++++++ Обработчик действий, связанных с пользователем +++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

  //echo $action;
   
   // 03/04/2014  Добавил значения по умолчанию, чтобы подсказки в полях были не только при добавлении, 
        //но и при правке, если не былди заполнены поля при добавлении
	 $UserCityPlaceHolder = 'Город';
       
   
   
   if ($action == "") 
   {
     // Действие не указано
       $view = "MainPage";
       // $statustext = "Сотрудник: ".$employeename.", табельный номер: ".$tabnum ;

   } elseif ($action == "UserLogin")  {
    // обработка регистрации
         
        // первичная проверка данных 
	if ($_POST['Login'] == "") 
	{
           $statustext = "Не указан e-mail.";
           $alert = 1; 
           return;

        } elseif ($_POST['Password']== "") {

           $statustext = "Не указан пароль.";
           $alert = 1; 
           return;
        } 
         // конец первичной проверки входных данных

        $Sql = "select user_id, user_name from  Users where trim(user_email) = trim('".$_POST['Login']."') and user_password = '".md5(trim($_POST['Password']))."'";
		
	//echo $Sql;
		
	$Result = MySqlQuery($Sql);  
	$Row = mysql_fetch_assoc($Result);
	$UserId = $Row['user_id'];
		
	if ($UserId <= 0) 
	{
		$statustext = "Неверный email или пароль.";
		  //.$login." не найден!";
		$password = "";
		mysql_close();
		$alert = 1; 
		return;  
	} 
		//Конец проверки пользователя и пароля

	$SessionId = StartSession($UserId);

	$view = "MainPage";
	//$statustext = "ua Пользователь: ".$UserId.", сессия: ".$SessionId;
		

   } elseif ($action == "UserInfo")  {
    // Действие вызывается ссылкой под имененм пользователя
   
	$view = "ViewUserData";
	$viewmode = "";


   } elseif ($action == "ViewNewUserForm")  {
    // Действие вызывается ссылкой Новый пользователь

           $view = "ViewUserData";
	   $viewmode = "Add";	

   } elseif ($action == "UserChangeData" or $action == "AddUser")  {
     // Действие вызывается либо при регистрации нового пользователя лиюо при сменен данных старого

   	   $view = "ViewUserData";
           

           $pUserEmail = $_POST['UserEmail'];
           $pUserName = $_POST['UserName'];
           $pUserCity = $_POST['UserCity'];
           $pUserBirthYear = $_POST['UserBirthYear'];
           if (!isset($_POST['UserProhibitAdd'])) $_POST['UserProhibitAdd'] = "";
           $pUserProhibitAdd = ($_POST['UserProhibitAdd'] == 'on' ? 1 : 0);
           $pUserId = $_POST['UserId']; 

           if (!isset($_POST['UserNewPassword'])) $_POST['UserNewPassword'] = "";
           if (!isset($_POST['UserConfirmNewPassword'])) $_POST['UserConfirmNewPassword'] = "";

           $pUserNewPassword = $_POST['UserNewPassword']; 
           $pUserConfirmNewPassword = $_POST['UserConfirmNewPassword']; 
         
	   if ($pUserCity == $UserCityPlaceHolder) { $pUserCity = ''; }  

           // 03/07/2014  Скрываем ФИО	 
           if (!isset($_POST['UserNoShow'])) $_POST['UserNoShow'] = "";
           $pUserNoShow = ($_POST['UserNoShow'] == 'on' ? 1 : 0);


   
	   if ($action == 'AddUser')
	   {
             // Новый пользователь 
             $pUserId = 0;
	     $viewmode = "Add";
           } else {
	     $viewmode = "";
	   }
 
           if (trim($pUserEmail) == '')
	   {
		$statustext = "Не указан e-mail.";
	        $alert = 1;
                $viewsubmode = "ReturnAfterError"; 
		return; 
	   }

           if (trim($pUserName) == '')
	   {
		$statustext = "Не указано ФИО.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
		return; 
	   }

           if ($pUserBirthYear < 1930 or $pUserBirthYear > date("Y"))
	   {
		$statustext = "Год не указан или указан некорректный.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
		return; 
	   }


	   if ((trim($pUserNewPassword) <> '' or trim($pUserConfirmNewPassword) <> '') and trim($pUserNewPassword) <> trim($pUserConfirmNewPassword))
	   {
		$statustext = "Не совпадает новый пароль и его подтверждение.";
	        $alert = 1;
                $viewsubmode = "ReturnAfterError"; 
		return; 
	   }


           // Прверяем, что нет активной учетной записи с таким e-mail
           $sql = "select count(*) as resultcount from  Users where COALESCE(user_password, '') <> '' and trim(user_email) = '".$pUserEmail."' and user_id <> ".$pUserId;
      //     echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
           mysql_free_result($rs);
	   if ($Row['resultcount'] > 0)
	   {
   		$statustext = "Уже есть пользователь с таким email.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
                return; 
	   }


	   $sql = "select count(*) as resultcount
	           from  Users 
		   where  trim(user_name) = '".$pUserName."' 
		          and user_birthyear = ".$pUserBirthYear." 
			  and user_id <> ".$pUserId." 
			  and userunionlog_id is null ";
           //echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
           mysql_free_result($rs);
	   if ($Row['resultcount'] > 0)
	   {
   		$statustext = "Уже есть пользователь с таким именем и годом рождения.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
                return; 
	   }


	    // Если есть неактивная учетная запись - высылаем на почту ссылку с активацией
           $sql = "select user_id from  Users where COALESCE(user_password, '') = '' and trim(user_email) = '".$pUserEmail."' and user_id <> ".$pUserId;
           //echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
           mysql_free_result($rs);
	   if ($Row['user_id'] > 0)
	   {
               if ($action == 'AddUser')
               {

		 $ChangePasswordSessionId = uniqid();
                 $sql = "update  Users set   user_sessionfornewpassword = '".$ChangePasswordSessionId."', user_sendnewpasswordrequestdt = now()
		         where user_id = ".$Row['user_id'];         
//                 echo $sql;  
		 MySqlQuery($sql);

		   // Решил не писать здесь имя - м.б. и в адресе не надо
		   $Msg = "Здравствуйте!\r\n\r\n";
		   $Msg =  $Msg."Кто-то (возможно, это были Вы) пытается зарегистрировать учетную запись на сайте ММБ, связанную с этим адресом e-mail.".".\r\n";
		   $Msg =  $Msg."Запись помечена, как неактивная, поэтому повтороно высылается ссылка для активации:".".\r\n";
		   $Msg =  $Msg."Для активации пользователя и получения пароля необходимо перейти по ссылке:".".\r\n";
		   $Msg =  $Msg.$MyHttpLink.$MyPHPScript.'?action=sendpasswordafterrequest&changepasswordsessionid='.$ChangePasswordSessionId."\r\n\r\n";
		   $Msg =  $Msg."Учетные записи без активации могут быть удалены.".".\r\n";
		   //$Msg =  $Msg."P.S. Если Вас зарегистрировали без Вашего желания - просто проигнорируйте письмо - приносим извинения за доставленные неудобства."."\r\n";
			    
                   // Отправляем письмо
		   SendMail(trim($pUserEmail), $Msg, $pUserName);

                   $statustext = 'Повторная ссылка для активации пользователя и получения пароля выслана на указанный адрес. 
		                  Если письмо не пришло - проверьте спам. Учетные записи без активации могут быть удалены.';				     

		   $view = "MainPage";
	           return; 
               } else {
		  $statustext = "Уже есть пользователь с таким email.";
		  $alert = 1; 
		  $viewsubmode = "ReturnAfterError"; 
		  return; 
               }
	   }


	   if ($action == 'AddUser')
	   {
             // Новый пользователь 

                 // Для более жёсткой проверки можно делать дополнительную активацию:
		 // не генерировать сразу пароль, а высылать на почту запрос
		 // Потребуется, видимо, ещё дополнительное поле (например, время отправки запроса), чтобы удалять неактивные записи

                 // Создаём пароь
                  //$NewPassword = GeneratePassword(6);

                //  echo         $NewPassword;           

                  // Пароль выводим на экран, высылаем по почте и пишем его хэш в базу


		 $ChangePasswordSessionId = uniqid();

                 // записываем нового пользователя
                 // пароль пустой, сессия для смены пароля и время отправки запроса

		 $sql = "insert into  Users (user_email, user_name, user_birthyear, user_password, user_registerdt,
		                             user_sessionfornewpassword, user_sendnewpasswordrequestdt, 
					     user_prohibitadd, user_city, user_noshow)
		                     values ('".$pUserEmail."', '".$pUserName."', ".$pUserBirthYear.", '', now(),
				             '".$ChangePasswordSessionId."', now(), 
					      ".$pUserProhibitAdd.", '".$pUserCity."', ".$pUserNoShow.")";
//                 echo $sql;  
                 // При insert должен вернуться послений id - это реализовано в  MySqlQuery
		 $newUserId = MySqlQuery($sql);
	
//	         echo $UserId; 
//                 $UserId = mysql_insert_id($Connection);
		 if ($newUserId <= 0)
		 {
                       $statustext = 'Ошибка записи нового пользователя.';
			$alert = 1;
			$viewsubmode = "ReturnAfterError"; 
			return;
		 } else {

                   // Решил не писать здесь имя - м.б. и в адресе не надо
		   $Msg = "Здравствуйте!\r\n\r\n";
		   $Msg =  $Msg."Кто-то (возможно, это были Вы) зарегистрировал учетную запись на сайте ММБ, связанную с этим адресом e-mail.".".\r\n";
		   $Msg =  $Msg."Для активации пользователя и получения пароля необходимо перейти по ссылке:".".\r\n";
		   $Msg =  $Msg.$MyHttpLink.$MyPHPScript.'?action=sendpasswordafterrequest&changepasswordsessionid='.$ChangePasswordSessionId."\r\n\r\n";
		   $Msg =  $Msg."Учетные записи без активации могут быть удалены.".".\r\n";
		   $Msg =  $Msg."P.S. Если Вас зарегистрировали без Вашего желания - просто проигнорируйте письмо - приносим извинения за доставленные неудобства."."\r\n";
			    
                   // Отправляем письмо
		   SendMail(trim($pUserEmail), $Msg, $pUserName);

                   $statustext = 'Ссылка для активации пользователя и получения пароля выслана на указанный адрес. 
		                  Если письмо не пришло - проверьте спам. Учетные записи без активации могут быть удалены.';				     

		   $view = "MainPage";

		 }	     
	   
              // Конец обработки нового пользователя            
	
           } elseif ($action == 'UserChangeData') {

              // Правка текущего пользователя
	   
             // Если вызвали с таким действием, должны быть определны оба пользователя
             if ($pUserId <= 0 or $UserId <= 0)
	     {
	      return;
	     }
	   
	     $AllowEdit = 0;
	     // Права на редактирование
             if (($pUserId == $UserId) || $Administrator)
	     {
		  $AllowEdit = 1;
	     } else {

	       $AllowEdit = 0;
               // выходим
	       return;
	     }

             if ($AllowEdit == 1)
	     {

                  // 03/07/2014  Добавляем признак анонимности (скрывать ФИО)

	         $sql = "update  Users set   user_email = trim('".$pUserEmail."'),
		                             user_name = trim('".$pUserName."'),
		                             user_city = trim('".$pUserCity."'),
		                             user_prohibitadd = ".$pUserProhibitAdd.",
		                             user_noshow = ".$pUserNoShow.",
					     user_birthyear = ".$pUserBirthYear."
	                 where user_id = ".$pUserId;
                 
          		// echo $sql;
		 $rs = MySqlQuery($sql);  

                  // Обновление пароля джелаем только, когда просят
		  if (trim($pUserNewPassword) <> '' and trim($pUserConfirmNewPassword) <> '' and trim($pUserNewPassword) == trim($pUserConfirmNewPassword))
		  {
		      $sql = "update  Users set user_password = md5('".trim($pUserNewPassword)."')
	                      where user_id = ".$pUserId;
                 
          		// echo $sql;
		      $rs = MySqlQuery($sql);  

                     $statustext = 'Сохранён новый пароль.';


		  }



		 // Формируем сообщение

	         $Sql = "select user_name from  Users where user_id = ".$UserId;
		 $Result = MySqlQuery($Sql);  
		 $Row = mysql_fetch_assoc($Result);
		 $ChangeDataUserName = $Row['user_name'];
		 mysql_free_result($Result);
		    
                 $Msg = "Уважаемый пользователь ".$pUserName."!\r\n\r\n";
		 $Msg =  $Msg."В Вашей учетной записи произошли изменения - их можно увидеть в карточке пользователя."."\r\n";
		 $Msg =  $Msg."Автор изменений: ".$ChangeDataUserName.".\r\n\r\n";
		 $Msg =  $Msg."P.S. Изменения можете вносить Вы, а также администратор сайта ММБ.";
			   
			    
                  // Отправляем письмо
		  SendMail(trim($pUserEmail), $Msg, $pUserName);

             } 

	     // Конец сохранений изменений текущего пользователя            
	      	   
	   } else {
	   
	     // других вариантов не должно быть
             return;
	   }

     // Конец добавления нового или сохарнений изменений теущего пользователя

	   
   } elseif ($action == "SendEmailWithNewPassword")  {
    // Действие вызывается ссылкой из формы просмотра данных пользователя
  
  	     $view = "ViewUserData";

             $pUserId = $_POST['UserId'];

        //     echo 'pUserId '.$pUserId.'now  '.$NowUserId;
	
             // Если вызвали с таким действием, должны быть определны оба пользователя
             if ($pUserId <= 0 or $UserId <= 0)
	     {
	      return;
	     }
	   
	     $AllowEdit = 0;
	     // Права на редактирование
             if (($pUserId == $UserId) || $Administrator)
	     {
		  $AllowEdit = 1;
	     } else {

	       $AllowEdit = 0;
               // выходим
	       return;
	     }

             if ($AllowEdit == 1)
	     {
	   
		$sql = "select user_email, user_name, user_birthyear from  Users where user_id = ".$pUserId;
		$rs = MySqlQuery($sql);  
                $row = mysql_fetch_assoc($rs);
                mysql_free_result($rs);
     		$UserEmail = $row['user_email'];  
		$UserName = $row['user_name']; 

  		$NewPassword = GeneratePassword(6);
		
		// пишем в базу пароль и время отправки письма с паролем
		//  обнуляем сессию для восстановления и её время
		$sql = "update   Users  set user_password = '".md5($NewPassword)."',
		                             user_sendnewpassworddt = now(),
					     user_sessionfornewpassword = null,
					     user_sendnewpasswordrequestdt = null
		         where user_id = ".$pUserId;
              //   echo $sql;
	        $rs = MySqlQuery($sql);  

		$statustext = 'Пароль '.$NewPassword.' выслан.';
                $view = "";

	        $Sql = "select user_name from  Users where user_id = ".$UserId;
		$Result = MySqlQuery($Sql);  
		$Row = mysql_fetch_assoc($Result);
		$ChangeDataUserName = $Row['user_name'];
		mysql_free_result($Result);

		$Msg = "Уважаемый пользователь ".$UserName."!\r\n\r\n";
		$Msg =  $Msg."У Вашей учетной записи изменён пароль: ".$NewPassword."\r\n";
		$Msg =  $Msg."Автор изменений: ".$ChangeDataUserName.".\r\n\r\n";
		$Msg =  $Msg."P.S. Изменения можете вносить Вы, а также администратор сайта ММБ.";
			    
                // Отправляем письмо
		SendMail(trim($UserEmail), $Msg, $UserName);

            }
             // Конец проверки на возможность отправки пароля

   } elseif ($action == "RestorePasswordRequest")  {
   // Действие вызывается ссылкой "Забыли пароль"
  
	   $view = "";

           $pUserEmail = $_POST['Login'];

           //echo $pUserEmail;
           if (trim($pUserEmail) == '' or trim($pUserEmail) == 'E-mail') 
	   {
	              $statustext = 'Не указан e-mail.';
		      $alert = 1;
		      return;
	   }


           $sql = "select user_id 
                   from  Users 
                   where user_hide = 0 and user_email = '".$pUserEmail."'";

         //  echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
	   mysql_free_result($rs); 
	   $pUserId = $Row['user_id'];
 	
	   if ($pUserId <= 0)
	   {
	              $statustext = 'Пользователь с  e-mail '.$pUserEmail.' не найден ';
		      $alert = 1;
		      return;
	   }


	   $ChangePasswordSessionId = uniqid();
	   
           // пишем в базу сессию для восстановления пароля
           $sql = "update   Users  set user_sessionfornewpassword = '".$ChangePasswordSessionId."',
	                               user_sendnewpasswordrequestdt = now()
	           where user_id = '".$pUserId."'";
           //echo $sql;
	   $rs = MySqlQuery($sql);  

	   $Msg = "Здравствуйте!\r\n\r\n";
	   $Msg =  $Msg."Кто-то (возможно, это были Вы) запросил восстановление пароля на сайте ММБ для этого адреса e-mail."."\r\n";
	   $Msg =  $Msg."Для получения нового пароля необходимо перейти по ссылке:"."\r\n";
	   $Msg =  $Msg.$MyHttpLink.$MyPHPScript.'?action=sendpasswordafterrequest&changepasswordsessionid='.$ChangePasswordSessionId."\r\n\r\n";
	   $Msg =  $Msg."P.S. Если Вы не запрашивали восстановление пароля - просто проигнорируйте письмо - приносим извинения за доставленные неудобства."."\r\n";

	   //echo $Message;				     
           // Чтобы вежливо написать "кому", нужен доп. запрос с получением имени по enail
           // пока не стал делать
	   SendMail($pUserEmail, $Msg);	

           $statustext = 'Ссылка для получения нового пароля выслана на указанный адрес. Если письмо не пришло - проверьте спам.';				     



   } elseif ($action == "sendpasswordafterrequest")  {
     // Действие вызывается из письма переходом по ссылке
	   $view = "";

	   if (isset($_REQUEST['changepasswordsessionid'])) $changepasswordsessionid = $_REQUEST['changepasswordsessionid'];
	   else $changepasswordsessionid = "";
	   if (empty($changepasswordsessionid))
	   {
              $action = "";
	      return;
	   }
	   

           $sql = "select user_id, user_email, user_name from  Users where user_sessionfornewpassword = trim('".$changepasswordsessionid."')";
         //  echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
	   mysql_free_result($rs); 
 	   $UserId = $Row['user_id'];
 	   $UserEmail = $Row['user_email'];
 	   $UserName = $Row['user_name'];

            // echo $UserEmail; 
           // Если идентификаторы совпали - меняем пароль
	   // Возможно здесь стоит сразу стартовать сессию...
	   if ($UserId > 0)
	   {

		$NewPassword = GeneratePassword(6);
		
		// пишем в базу пароль и время отправки письма с паролем
		//  обнуляем сессию для восстановления и её время
		$sql = "update   Users  set user_password = '".md5($NewPassword)."',
		                             user_sendnewpassworddt = now(),
					     user_sessionfornewpassword = null,
					     user_sendnewpasswordrequestdt = null
		         where user_id = ".$UserId;
              //   echo $sql;
	        $rs = MySqlQuery($sql);  

		$statustext = 'Пароль '.$NewPassword.' выслан.';

		$Msg = "Уважаемый пользователь ".$UserName."!\r\n\r\n";
		$Msg =  $Msg."Согласно подтверждённому запросу с Вашего адреса e-mail,"."\r\n";
		$Msg =  $Msg."для Вашей учетной записи на сайте ММБ создан пароль: ".$NewPassword."\r\n";
			    
                // Отправляем письмо
		SendMail(trim($UserEmail), $Msg, $UserName);

                // и вот тут м.б. стоит активировать сессию, чтобы автоматом войти на сайт
		$SessionId = StartSession($UserId);
		$view = "MainPage";
              
            }

            
            $changepasswordsessionid = "";
            $action = "";
                
   } elseif ($action == "UserLogout")  {
     // Выход 

	        CloseSession($SessionId, 3);
                $SessionId = ""; 
		$action = "";
		$view = "MainPage";
	
   } elseif ($action == "CancelChangeUserData")  {
    // Действие вызывается ссылкой Отмена

           $view = "ViewUserData";
	   $viewmode = "";

   } elseif ($action == "FindUser")  {
    // Действие вызывается поиском участника

		if (isset($_POST['FindString'])) $FindString = $_POST['FindString']; else $FindString = "";
                if (trim($FindString) == '' or trim($FindString) == 'Часть ФИО')
                {
                  $statustext = 'Не указан критерий поиска.';				     
                  $view = "";
 		  return;
                }


                if (trim($FindString) == 'все-все' or trim($FindString) == 'все-все-все')
                {
		  $sqlFindString = '';
                } else {
		  $sqlFindString = trim($FindString);
                }

         
        	$sql = "select count(*) as FindUsersCount
		        from  Users u
			where ltrim(COALESCE(u.user_password, '')) <> '' 
                              and u.user_hide = 0 
			      and COALESCE(u.user_noshow, 0) = 0
                              and user_name like '%".$sqlFindString."%'";
                
		//echo 'sql '.$sql;
		
		$Result = MySqlQuery($sql);
	        $Row = mysql_fetch_assoc($Result);
		$RowCount = $Row['FindUsersCount'];
	        mysql_free_result($Result);
		
		if ($RowCount > 0)
		{
		   $view = "ViewUsers";
		
		} else {

                    $statustext = 'Не найдено пользователей, чьи ФИО содержат '.trim($FindString);				     
                    $view = "";
                }


   } elseif ($action == "MakeModerator")  {
    // Действие вызывается нажатием кнопки "Сделать модератором"

             $pUserId = $_POST['UserId']; 

             // Если вызвали с таким действием, должны быть определны оба пользователя
             if ($pUserId <= 0 or $UserId <= 0)
	     {
	      return;
	     }
	   
	     // Права на редактирование
             if (!$Administrator)
	     {
	      return;
	     } 


			$Sql = "select raidmoderator_id,  raidmoderator_hide
 		                from RaidModerators 
			        where raid_id = ".$RaidId."
			              and user_id = ".$pUserId."
			        LIMIT 0,1 "  ;
				
			 $Result = MySqlQuery($Sql);  
			 $Row = mysql_fetch_assoc($Result);
	                 mysql_free_result($Result);
			 $RaidModeratorId =  $Row['raidmoderator_id'];	
			 $RaidModeratorHide =  $Row['raidmoderator_hide'];	
	         
		 $ModeratorAdd = 0;
		 
		 if (empty($RaidModeratorId))
		 {
			 $Sql = "insert into RaidModerators (raid_id, user_id, raidmoderator_hide) values (".$RaidId.", ".$pUserId.", 0)";
			 MySqlQuery($Sql);  
			 $ModeratorAdd = 1;

		 } else {	 
			 
			 if ($RaidModeratorHide == 0)
			 {
			    $ModeratorAdd = 0;
			 
			 } else {
			   
			  // Есть и модератор скрыт -  обновляем
 		          $Sql = "update RaidModerators set raidmoderator_hide = 0 where raidmoderator_id = ".$RaidModeratorId;
			  MySqlQuery($Sql);  
   		          $ModeratorAdd = 1;

			 }
                         // Конец проверки существующей записи
		 
		 } 
                 // Конец разбора ситуации с модераторами		 
                 

             if ($ModeratorAdd)
	     {

                 $statustext = 'Добавлен модератор';				     


	         $Sql = "select user_name from  Users where user_id = ".$UserId;
		 $Result = MySqlQuery($Sql);  
		 $Row = mysql_fetch_assoc($Result);
		 $ChangeDataUserName = $Row['user_name'];
		 mysql_free_result($Result);

	         $Sql = "select user_name, user_email from  Users where user_id = ".$pUserId;
		 $Result = MySqlQuery($Sql);  
		 $Row = mysql_fetch_assoc($Result);
		 $pUserName = $Row['user_name'];
		 $pUserEmail = $Row['user_email'];
		 mysql_free_result($Result);


	         $Sql = "select raid_name from  Raids where raid_id = ".$RaidId;
		 $Result = MySqlQuery($Sql);  
		 $Row = mysql_fetch_assoc($Result);
		 $RaidName = $Row['raid_name'];
		 mysql_free_result($Result);



                 $Msg = "Уважаемый пользователь ".$pUserName."!\r\n\r\n";
		 $Msg =  $Msg."Вы получили статус модератора марш-броска ".$RaidName."\r\n";
		 $Msg =  $Msg."Автор изменений: ".$ChangeDataUserName.".\r\n\r\n";
		 	   
			    
                  // Отправляем письмо
		  SendMail(trim($pUserEmail), $Msg, $pUserName);
		  $view = "ViewAdminModeratorsPage";
	      	  $viewmode = "";


             } else {
	     
	        $statustext = 'Пользователь уже имеет статус модератора!';				     
		   $view = "ViewUserData";
		   $viewmode = "";


	     }


   } elseif ($action == "HideModerator")  {
    // Действие вызывается нажатием кнопки "Удалить" на странице со списком модераторов

             $RaidModeratorId = $_POST['RaidModeratorId']; 
             $pUserId = $_POST['UserId']; 

             // Если вызвали с таким действием, должны быть определны оба пользователя
             if ($RaidModeratorId <= 0 or !$Administrator)
	     {
	      return;
	     }
	   
	   
	          $Sql = "update RaidModerators set raidmoderator_hide = 1 where raidmoderator_id = ".$RaidModeratorId;
		  MySqlQuery($Sql);  
		  

                  $statustext = 'Удален модератор';				     

	         $Sql = "select user_name from  Users where user_id = ".$UserId;
		 $Result = MySqlQuery($Sql);  
		 $Row = mysql_fetch_assoc($Result);
		 $ChangeDataUserName = $Row['user_name'];
		 mysql_free_result($Result);

	         $Sql = "select user_name, user_email from  Users where user_id = ".$pUserId;
		 $Result = MySqlQuery($Sql);  
		 $Row = mysql_fetch_assoc($Result);
		 $pUserName = $Row['user_name'];
		 $pUserEmail = $Row['user_email'];
		 mysql_free_result($Result);

	         $Sql = "select raid_name from  Raids where raid_id = ".$RaidId;
		 $Result = MySqlQuery($Sql);  
		 $Row = mysql_fetch_assoc($Result);
		 $RaidName = $Row['raid_name'];
		 mysql_free_result($Result);

		    
                 $Msg = "Уважаемый пользователь ".$pUserName."!\r\n\r\n";
		 $Msg =  $Msg."Вы потеряли статус модератора марш-броска ".$RaidName."\r\n";
		 $Msg =  $Msg."Автор изменений: ".$ChangeDataUserName.".\r\n\r\n";
		 	   
			    
                 // Отправляем письмо
		 SendMail(trim($pUserEmail), $Msg, $pUserName);

               // Остаемся на той же странице

		$view = "ViewAdminModeratorsPage";
		$viewmode = "";
 
   }
   // ============ Обратимое удаление пользователя ====================================
   elseif ($action == 'HideUser')
   {

	$pUserId = $_POST['UserId']; 

	if ($pUserId <= 0)
	{
		$statustext = 'Пользователь не найден';
		$alert = 1;
		return;
	}

	if ($SessionId <= 0)
	{
		$statustext = 'Сессия не найдена';
		$alert = 1;
		return;
	}

	// Проверка возможности удалить пользоваиеля
	if (!$Administrator)
	{
		$statustext = "Удаление пользователя запрещено";
		$alert = 1;
		return;
	}

/*
	if ($pUserId == $UserId)
	{
		$statustext = 'Нельзя удалить самого себя';
		$alert = 1;
		return;
	}
*/

        // Проверяем, что пользователя нет ни в одной команде
	$sql = " select tu.teamuser_id
	         from TeamUsers tu 
		 inner join Teams t
		 on tu.team_id = t.team_id     
		 where tu.teamuser_hide = 0 
		       and t.team_hide = 0
                       and tu.user_id = ".$pUserId; 
 
	$Result = MySqlQuery($sql);
        $RowsCount = mysql_num_rows($Result);

	if ($RowsCount > 0)
	{
	        $statustext = 'Пользователь уже является участником по крайней мере одной команды';				     
		$alert = 1;
		return;
	}

	         $Sql = "select user_name, user_email from  Users where user_id = ".$pUserId;
		 $Result = MySqlQuery($Sql);  
		 $Row = mysql_fetch_assoc($Result);
		 $pUserName = $Row['user_name'];
		 $pUserEmail = $Row['user_email'];
		 mysql_free_result($Result);



	$sql = "update Users set user_hide = 1 where user_id = ".$pUserId;
	$rs = MySqlQuery($sql);
	     $AllowEdit = 0;
	     // Права на редактирование
             if (($pUserId == $UserId) || $Administrator)
	     {
		  $AllowEdit = 1;
	     } else {

	       $AllowEdit = 0;
               // выходим
	       return;
	     }

        $statustext = 'Пользователь '.$pUserName.' ключ '.$pUserId.' удален ';				     
	$view = "ViewRaidTeams";
   }
   // ============ Добавление устройства ====================================
   elseif ($action == 'AddDevice')
   {
   
   
	$pUserId = $_POST['UserId']; 

	if ($pUserId <= 0)
	{
		$statustext = 'Пользователь не найден';
		$alert = 1;
		return;
	}

	if ($SessionId <= 0)
	{
		$statustext = 'Сессия не найдена';
		$alert = 1;
		return;
	}


	     $AllowEdit = 0;
	     // Права на редактирование
             if (($pUserId == $UserId) || $Administrator)
	     {
		  $AllowEdit = 1;
	     } else {
	     $AllowEdit = 0;
	     // Права на редактирование
             if (($pUserId == $UserId) || $Administrator)
	     {
		  $AllowEdit = 1;
	     } else {

	       $AllowEdit = 0;
               // выходим
	       return;
	     }

	       $AllowEdit = 0;
               // выходим
	       return;
	     }

		$pNewDeviceName = trim($_POST['NewDeviceName']); 
		
		if (!isset($pNewDeviceName)) {
		  $pNewDeviceName  = '';
		}

	if (empty($pNewDeviceName) or $pNewDeviceName == 'Название нового устройства')
	{
		$statustext = 'Не указано название устройства';
		$alert = 1;    
		   $view = "ViewUserData";
		   $viewmode = "";

		return;
	}

	// Прверяем, что нет устройства с таким именем
           $sql = "select count(*) as resultcount from  Devices where trim(device_name) = '".$pNewDeviceName."'";
      //     echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
           mysql_free_result($rs);
	   if ($Row['resultcount'] > 0)
	   {
   		$statustext = "Уже есть устройство с таким именем.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
                return; 
	   }
	   
    		 $Sql = "insert into Devices (device_name, user_id) values ('".$pNewDeviceName."', ".$pUserId.")";
		 MySqlQuery($Sql);  

	   
	           $statustext = 'Добавлено устройство';				     
		   $view = "ViewUserData";
		   $viewmode = "";



   }
   // ============ Получение конфигурации ====================================
   elseif ($action == 'GetDeviceId')
   {
   
   
	$pUserId = $_POST['UserId']; 
	$pDeviceId = $_POST['DeviceId']; 

	if ($pUserId <= 0)
	{
		$statustext = 'Пользователь не найден';
		$alert = 1;
		return;
	}

	if ($pDeviceId <= 0)
	{
		$statustext = 'Устройство не найден';
		$alert = 1;
		return;
	}



	if ($SessionId <= 0)
	{
		$statustext = 'Сессия не найдена';
		$alert = 1;
		return;
	}


	     $AllowEdit = 0;
	     // Права на редактирование
             if (($pUserId == $UserId) || $Administrator)
	     {
		  $AllowEdit = 1;
	     } else {
	     $AllowEdit = 0;
	     // Права на редактирование
             if (($pUserId == $UserId) || $Administrator || $Moderator)
	     {
		  $AllowEdit = 1;
	     } else {

	       $AllowEdit = 0;
               // выходим
	       return;
	     }

	       $AllowEdit = 0;
               // выходим
	       return;
	     }

	// Прверяем, что есть устройство для пользователя
           $sql = "select count(*) as resultcount from  Devices where user_id  = ".$pUserId." and device_id = ".$pDeviceId;
      //     echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
           mysql_free_result($rs);
	   if ($Row['resultcount'] <> 1)
	   {
   		$statustext = "Нет устройства.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
                return; 
	   }
	   
		// Сбор данных для конфигурации
	  $data = array();

	// Raids: raid_id, raid_name, raid_registrationenddate
	$Sql = "select d.device_id, d.device_name, d.user_id, u.user_name, u.user_password from Devices d inner join Users u on d.user_id = u.user_id  where d.device_id = ".$pDeviceId;
	$Result = MySqlQuery($Sql);
	while ( ( $Row = mysql_fetch_assoc($Result) ) ) { $data["Devices"][] = $Row; }
	mysql_free_result($Result);

	// Заголовки, чтобы скачивать можно было и на мобильных устройствах просто браузером (который не умеет делать Save as...)
	header("Content-Type: application/octet-stream");
	header("Content-Disposition: attachment; filename=\"device.json\"");

	// Вывод json
	print json_encode( $data );
 
	// Можно не прерывать, но тогда нужно написать обработчик в index, чтобы не выводить дальше ничего
	die();
	return;

     } 
     // ============ Отправка сообщения другому пользователю ====================================
     elseif ($action == "SendMessage")  {
    // 
  
  	     $view = "ViewUserData";
	     $viewmode = "";

             $pUserId = $_POST['UserId'];
             $pText = $_POST['MessageText'];

	     if (empty($pText) or trim($pText) == 'Текст сообщения')
	     {
   		$statustext = "Укажите тексто сообщения.";
	        $alert = 1; 
                $viewsubmode = ""; 
                return; 
	     }

        //     echo 'pUserId '.$pUserId.'now  '.$NowUserId;
	
             // Если вызвали с таким действием, должны быть определны оба пользователя
             if ($pUserId <= 0 or $UserId <= 0)
	     {
	      return;
	     }
	   
	   
		$sql = "select user_email, user_name, user_birthyear from  Users where user_id = ".$pUserId;
		$rs = MySqlQuery($sql);  
                $row = mysql_fetch_assoc($rs);
                mysql_free_result($rs);
     		$UserEmail = $row['user_email'];  
		$UserName = $row['user_name']; 

  	
		$statustext = 'Сообщение выслано.';
                $view = "";

	        $Sql = "select user_name from  Users where user_id = ".$UserId;
		$Result = MySqlQuery($Sql);  
		$Row = mysql_fetch_assoc($Result);
		$SendMessageUserName = $Row['user_name'];
		mysql_free_result($Result);

                $pTextArr = explode('\r\n', $pText); 

		$Msg = "Уважаемый пользователь ".$UserName."!\r\n";
		$Msg =  $Msg."Через сайт ММБ пользователь ".$SendMessageUserName." отправил Вам следующее сообщение:\r\n\r\n";

                foreach ($pTextArr as $NowString) {
 
                 
		   $Msg =  $Msg.$NowString."\r\n";

		}
//		$Msg =  $Msg.$pText;
		$Msg =  $Msg."\r\n"."Для ответа необходимо автооризоваться и открыть карточку пользователя  ".$SendMessageUserName."\r\n";
		$Msg =  $Msg.$MyHttpLink.$MyLocation."?action=UserInfo&UserId=".$UserId."\r\n";
		
			    
                // Отправляем письмо
		SendMail(trim($UserEmail), $Msg, $UserName);

	   
   }     
   // ============ Добавить пользхователя в объединение ====================================
   
   elseif ($action == "AddUserInUnion")  {
	// Действие вызывается нажатием кнопки "Объединить"

	if ($UserId <= 0)
	{
		$statustext = 'Пользователь не найден';
		$alert = 1;
		return;
	}
	if ($SessionId <= 0)
	{
		$statustext = 'Сессия не найдена';
		$alert = 1;
		return;
	}

        $pUserId = $_POST['UserId']; 
       

        if ($UserId == $pUserId) {
		$statustext = 'Нельзя объединить с самим собой';
		$alert = 1;
		return;
	}

     
        // Проверяем, что пользователя нет в объединении
	$sql = " select userunionlog_id
	         from UserUnionLogs 
		 where union_status <> 0
		       and union_status <> 3
		       and user_id = ".$UserId; 

 
	$Result = MySqlQuery($sql);
        $RowsCount = mysql_num_rows($Result);

	if ($RowsCount > 0)
	{
	        $statustext = 'Пользователь уже есть в объединении';				     

		$view = "ViewUserUnionPage";
		$viewmode = "";
		$viewsubmode = "ReturnAfterError";

	       return;
	}


	// Проверяем, что пользователь не скрыт
	// здесь можно ещё проверить, что пользователь импортирован или любое другое условие
	$sql = " select user_id 
	         from Users 
		 where user_hide = 0 
		       and user_id = ".$pUserId; 


 
	$Result = MySqlQuery($sql);
        $RowsCount = mysql_num_rows($Result);

	if ($RowsCount <= 0)
	{
	        $statustext = 'Пользователь скрыт';				     

		$view = "ViewAdminUnionPage";
		$viewmode = "";
		$viewsubmode = "ReturnAfterError";

	       return;
	}

        $UnionRequestId = 0;
   	$Sql = "insert into UserUnionLogs (user_id, userunionlog_dt, 
		         user_parentid, union_status)
			  values (".$UserId.", now(), ".$pUserId.",  1)";
	$UnionRequestId = MySqlQuery($Sql);  
			 
        if ($UnionRequestId)
        {

                 $statustext = 'Создан запрос на объединение пользователей';				     

	         $Sql = "select user_name, user_email, user_importattempt  from  Users where user_id = ".$pUserId;
		 $Result = MySqlQuery($Sql);  
		 $Row = mysql_fetch_assoc($Result);
		 $pUserName = $Row['user_name'];
		 $pUserEmail = $Row['user_email'];
		 $Import = $Row['user_importattempt'];
		 mysql_free_result($Result);

                 // Проверяем, что пользовтельский email не является автогенерированным
                 if (substr(trim($pUserEmail), -7) <> '@mmb.ru' && !empty($pUserName))
		 {

			$Sql = "select user_name from  Users where user_id = ".$UserId;
			$Result = MySqlQuery($Sql);  
			$Row = mysql_fetch_assoc($Result);
			$pRequestUserName = $Row['user_name'];
			mysql_free_result($Result);


			$Msg = "Уважаемый пользователь ".$pUserName."!\r\n\r\n";
			$Msg =  $Msg."Сделан запрос на объединения Вас с пользователем ".$pRequestUserName."\r\n";
			$Msg =  $Msg."После подтверждения запроса администраторм сервиса, все ваши участия в командах буду перенесены на пользователя, который запросил объединение, а Ваша учетная запись скрыта"."\r\n";
			$Msg =  $Msg."Если Вы считаете это неправильным, необходимо авторизоваться на сервисе ММБ, перейти на старницу 'Связь пользователей' и отклонить запрос."."\r\n\r\n";
		 	   
			// Отправляем письмо
			SendMail(trim($pUserEmail), $Msg, $pUserName);
		}
		// Конец проверки, что пользователь не импортирован

           }
	   // Конец проверки на успешное добавление запроса
	   $view = "ViewUserUnionPage";
	   $viewmode = "";


  } elseif ($action == "RejectUnion")  {
	// Действие вызывается нажатием кнопки "Отклонить" 
    
       $UserUnionLogId = $_POST['UserUnionLogId']; 

       if (!CanRejectUserUnion($Administrator, $UserUnionLogId, $UserId)) {  

		$statustext = 'Нет прав на отклонение запроса';
		$alert = 1;
		return;
	      return;
       }
       
       // ПРосто ставим статус в журнале - ничего больше делать не надол
       $sql = " update UserUnionLogs set union_status = 0 
			 where userunionlog_id = ".$UserUnionLogId;
		       
	MySqlQuery($sql);

	   $view = "ViewUserUnionPage";
	   $viewmode = "";
       

  } elseif ($action == "ApproveUnion")  {


       $UserUnionLogId = $_POST['UserUnionLogId']; 

       if (!CanApproveUserUnion($Administrator, $UserUnionLogId, $UserId)) {  

		$statustext = 'Нет прав на подтверждение запроса';
		$alert = 1;
		return;
	      return;
       }


	         $Sql = "select user_id, user_parentid  from  UserUnionLogs where userunionlog_id = ".$UserUnionLogId;
		 $Result = MySqlQuery($Sql);  
		 $Row = mysql_fetch_assoc($Result);
		 $pUserId = $Row['user_id'];
		 $pUserParentId = $Row['user_parentid'];
		 mysql_free_result($Result);

//echo $Sql;
       // Перебрасываем ссылки, ставим признак скрытия пользователя

        
       // Скрываем старого пользователя
       // Ключ журнала нужен исключительно для возможности потом переименовать пользователя - сделан уникальный ключ, который не допускаетодинаковое ФИО и год, но теперь я туда добавил ещё поле userunionlog_id
       // Тонкость в том, что при отмене объеддинения наод проверять, что польщзователь не свопадает, иначе будет ошибка ключа
        $sql = " update Users set user_hide = 1, userunionlog_id = ".$UserUnionLogId."  
		 where user_id = ".$pUserParentId;


//echo $sql;
		       
	MySqlQuery($sql);
	
       // Меняем ссылку в комнадах 
        $sql = " update TeamUsers set user_id = ". $pUserId.", userunionlog_id = ".$UserUnionLogId." 
		 where user_id = ". $pUserParentId;
	
	MySqlQuery($sql);
         

       // Меняем статус в журнале 
       $sql = " update UserUnionLogs set union_status = 2 
			 where userunionlog_id = ".$UserUnionLogId;
		       

	MySqlQuery($sql);

	   $view = "ViewUserUnionPage";
	   $viewmode = "";
  
  } elseif ($action == "RollBackUnion")  {


       $UserUnionLogId = $_POST['UserUnionLogId']; 

       if (!CanRollBackUserUnion($Administrator, $UserUnionLogId, $UserId)) {  

		$statustext = 'Нет прав на откат объединения';
		$alert = 1;
		return;
	      return;
       }


	         $Sql = "select user_id, user_parentid  from  UserUnionLogs where userunionlog_id = ".$UserUnionLogId;
		 $Result = MySqlQuery($Sql);  
		 $Row = mysql_fetch_assoc($Result);
		 $pUserId = $Row['user_id'];
		 $pUserParentId = $Row['user_parentid'];
		 mysql_free_result($Result);


           // Проверяем что новый пользователь не успел переименоваться в старого
	   $sql = "select user_name
	           from  Users 
		   where  user_id = ".$pUserId;
           //echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
           $UserName = $Row['user_name'];
	   mysql_free_result($rs);

           // Проверяем что новый пользователь не успел переименоваться в старого
	   $sql = "select user_name
	           from  Users 
		   where  user_id = ".$pUserParentId;
           //echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
           $ParentUserName = $Row['user_name'];
	   mysql_free_result($rs);

           // если успел - нового переименовываем
           if (trim($UserName) == trim($ParentUserName)) {
	   
	        $sql = " update Users set user_name =  '".trim($UserName).'_'.$UserUnionLogId."'
			 where user_id = ".$pUserId;
	
	//echo $sql;	       
		MySqlQuery($sql);
	   
	   }

       // Перебрасываем ссылки, ставим признак скрытия пользователя

        
       // Скрываем старого пользователя
       // Ключ журнала нужен исключительно для возможности потом переименовать пользователя - сделан уникальный ключ, который не допускаетодинаковое ФИО и год, но теперь я туда добавил ещё поле userunionlog_id
       // Тонкость в том, что при отмене объеддинения наод проверять, что польщзователь не свопадает, иначе будет ошибка ключа
        $sql = " update Users set user_hide = 0, userunionlog_id = NULL 
		 where userunionlog_id = ".$UserUnionLogId;
		       
	MySqlQuery($sql);
	
       // Меняем ссылку в комнадах 
        $sql = " update TeamUsers set user_id = ". $pUserParentId.", userunionlog_id = NULL 
		 where userunionlog_id = ".$UserUnionLogId;
	
	MySqlQuery($sql);
         

       // Меняем статус в журнале 
       $sql = " update UserUnionLogs set union_status = 3 
			 where userunionlog_id = ".$UserUnionLogId;
		       

	MySqlQuery($sql);

	   $view = "ViewUserUnionPage";
	   $viewmode = "";
  

   }  else {
   // если никаких действий не требуется

   //  $statustext = "<br/>";
   }

//	print('view = '.$view.' action = '.$action);
   
?>
