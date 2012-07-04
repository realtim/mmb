<?php
// +++++++++++ Обработчик действий, связанных с пользователем +++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

  //echo $action;
   
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
           $pUserBirthYear = $_POST['UserBirthYear'];
           if (!isset($_POST['UserProhibitAdd'])) $_POST['UserProhibitAdd'] = "";
           $pUserProhibitAdd = ($_POST['UserProhibitAdd'] == 'on' ? 1 : 0);
           $pUserId = $_POST['UserId']; 
           $pUserNewPassword = $_POST['UserNewPassword']; 
           $pUserConfirmNewPassword = $_POST['UserConfirmNewPassword']; 
         
   
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



           $sql = "select count(*) as resultcount from  Users where trim(user_email) = '".$pUserEmail."' and user_id <> ".$pUserId;
      //     echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
	   if ($Row['resultcount'] > 0)
	   {
   		$statustext = "Уже есть пользователь с таким email.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
                return; 
	   }


           $sql = "select count(*) as resultcount from  Users where  trim(user_name) = '".$pUserName."' and user_birthyear = ".$pUserBirthYear." and user_id <> ".$pUserId;
           //echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
	   if ($Row['resultcount'] > 0)
	   {
   		$statustext = "Уже есть пользователь с таким именем и годом рождения.";
	        $alert = 1; 
                $viewsubmode = "ReturnAfterError"; 
                return; 
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
		                             user_sessionfornewpassword, user_sendnewpasswordrequestdt, user_prohibitadd)
		                     values ('".$pUserEmail."', '".$pUserName."', ".$pUserBirthYear.", '', now(),
				             '".$ChangePasswordSessionId."', now(), '.$pUserProhibitAdd.')";
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



	         $sql = "update  Users set   user_email = trim('".$pUserEmail."'),
		                             user_name = trim('".$pUserName."'),
		                             user_prohibitadd = ".$pUserProhibitAdd.",
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




   } else {
   // если никаких действий не требуется

   //  $statustext = "<br/>";
   }

//	print('view = '.$view.' action = '.$action);
   
?>
