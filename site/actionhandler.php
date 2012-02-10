<?php


   $alert = 0; 
   $statustext = ""; //"Сегодня: ".date("d.m.Y")."  &nbsp; Время: ".date("H:i:s");
  echo $action;
   // Обработчик всех действий   
   

   // проверяем корректность сессии и определяем права на просмотр и бронирование
  
   if ($action == "UserLogin") {
       // обработка регистрации

	 $action = "";
          
         // первичная проверка данных 
	 if ($_POST['Login'] == "") {

           $statustext = "Не указан e-mail.";
           $alert = 1; 
           return;

         } elseif ($_POST['Password']== "") {

           $statustext = "Не указан пароль.";
           $alert = 1; 
           return;

         } 
         // конец первичной проверки входных данных

                $Sql = "select user_id, user_name from mmb.Users where trim(user_email) = trim('".$_POST['Login']."') and user_password = '".md5($_POST['Password'])."'";
		
		//echo $Sql;
		
		$Result = MySqlQuery($Sql);  
		$Row = mysql_fetch_assoc($Result);
		$UserId = $Row['user_id'];
		
		if ($UserId <= 0) 
		 {
			$statustext = "Неверный email или пароль.";
			  //.$login." не найден!";
			$password = "";
			mysql_close($Connection);
			$alert = 1; 
			return;  
		} 
		//Конец проверки пользователя и пароля



		$SessionId = StartSession($UserId);
		$view = "MainPage";
		//$statustext = "Пользователь: ".$UserId.", сессия: ".$SessionId;
		

   } elseif ($action == "")  {

         $view = "MainPage";
//         $statustext = "Сотрудник: ".$employeename.", табельный номер: ".$tabnum ;

   } elseif ($action == "UserInfo")  {
    // Действие вызывается ссылкой под имененм пользователя 

  	  $view = "ViewUserData";
	  //$statustext = "Пользовталель: ".$_SESSION['user_name'].", ключ: ".$_SESSION['user_id']. " сессия". session_id(); 

   } elseif ($action == "ViewNewUserForm")  {
    // Действие вызывается ссылкой Новый пользователь

           $view = "ViewUserData";
		

   } elseif ($action == "UserChangeData")  {
     // Действие вызывается либо при регистрации нового пользователя лиюо при сменен данных старого

   	   $view = "ViewUserData";

           $pUserEmail = $_POST['UserEmail'];
           $pUserName = $_POST['UserName'];
           $pUserBirthYear = $_POST['UserBirthYear'];


           if (trim($pUserEmail) == '')
	   {
		$statustext = "Не указан e-mail.";
	        $alert = 1; 
		return; 
	   }

           if (trim($pUserName) == '')
	   {
		$statustext = "Не указано ФИО.";
	        $alert = 1; 
		return; 
	   }

           if ($pUserBirthYear < 1930 or $pUserBirthYear > date("Y"))
	   {
		$statustext = "Год не указан или указан некорректный.";
	        $alert = 1; 
		return; 
	   }

	   $UserId = 0;

	   if (empty($SessionId))
	   {
	        $SessionId =  $_POST['sessionid'];
	   } 

	   $UserId = GetSession($SessionId);

           $sql = "select count(*) as resultcount from mmb.Users where trim(user_email) = '".$pUserEmail."' and user_id <> ".$UserId;
           //echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
	   if ($Row['resultcount'] > 0)
	   {
   		$statustext = "Уже есть пользователь с таким email.";
	        $alert = 1; 
                return; 
	   }


           $sql = "select count(*) as resultcount from mmb.Users where trim(user_name) = '".$pUserName."' and user_birthyear = ".$pUserBirthYear." and user_id <> ".$UserId;
           //echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
	   if ($Row['resultcount'] > 0)
	   {
   		$statustext = "Уже есть пользователь с таким именем и годом рождения.";
	        $alert = 1; 
                return; 
	   }


      //     echo $UserId; 
	   if ($UserId > 0)
	   {
 	    // Изменения к уже существующему пользователю

             // Проверка, что нет таких данных
	         $sql = "update mmb.Users set user_email = trim('".$pUserEmail."'), 
		                             user_name = trim('".$pUserName."'),
					     user_birthyear = ".$pUserBirthYear."
	                 where user_id = ".$UserId;
                 
	//	 echo $sql;
		 $rs = MySqlQuery($sql);  
	   } else {
	     // Новый пользователь

                 // Создаём пароь
                  $NewPassword = GeneratePassword(6);

                //  echo         $NewPassword;           
                  // Пароль выводим на экран, высылаем по почте и пишем его хэш в базу
                 // записываем нового пользователя
		 $sql = "insert into mmb.Users (user_email, user_name, user_birthyear, user_password) values ('".$pUserEmail."', '".$pUserName."', ".$pUserBirthYear.", '".md5($NewPassword)."')";
//                 echo $sql;  
                 // При insert должен вернуться послений id - это реализовано в  MySqlQuery
		 $UserId = MySqlQuery($sql);  
	
//	         echo $UserId; 
//                 $UserId = mysql_insert_id($Connection);
		 if ($UserId <= 0)
		 {
                        $statustext = 'Ошибка записи нового пользователя.';
			$alert = 1;
			return;
		 } else {

                   $statustext = 'Пароль: '.$NewPassword;
		   SendMail($pUserEmail,'New password: '.$NewPassword);
		 
		   $SessionId = StartSession($UserId);
		 }	     
	   
	    }
	   
   } elseif ($action == "SendEmailWithNewPassword")  {
    // Действие вызывается ссылкой из формы просмотра данных пользователя
  
	   $view = "ViewUserData";

           $pUserEmail = $_POST['UserEmail'];

	   $UserId = 0;

	   if (empty($SessionId))
	   {
	        $SessionId =  $_POST['sessionid'];
	   } 

	   $UserId = GetSession($SessionId);

         //  echo $UserId; 
	   if ($UserId > 0)
	   {
  		$NewPassword = GeneratePassword(6);
		
                 // пишем в базу
	         $sql = "update  mmb.Users  set user_password = '".md5($NewPassword)."' where user_id = ".$UserId;
              //   echo $sql;
		 $rs = MySqlQuery($sql);  

		$statustext = 'Пароль: '.$NewPassword;
		
		SendMail($pUserEmail,'New password: '.$NewPassword);

            }
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

	   $ChangePasswordSessionId = StartSession();

           // пишем в базу сессию для восстановления пароля
           $sql = "update  mmb.Users  set user_sessionfornewpassword = '".$ChangePasswordSessionId."' where user_email = '".$pUserEmail."'";
           //echo $sql;
	   $rs = MySqlQuery($sql);  
	
	   $Message =  'Если Вы хотите поменять пароль - перейдите по ссылке: '.
	               'http://mmb.progressor.ru'.$MyPHPScript.'?action=sendpasswordafterrequest&changepasswordsessionid='.$ChangePasswordSessionId;

	   //echo $Message;				     
	   SendMail($pUserEmail, $Message);	

           $statustext = 'Ссылка для получения нового пароля выслана на указанный адрес. Если письмо не пришло - проверьте спам.';				     



   } elseif ($action == "sendpasswordafterrequest")  {
     // Действие вызывается из письма переходом по ссылке
	   $view = "";

	   $UserId = 0;

           $sql = "select user_id, user_email from mmb.Users where user_sessionfornewpassword = '".$changepasswordsessionid."'";
         //  echo $sql;
	   $rs = MySqlQuery($sql);  
	   $Row = mysql_fetch_assoc($rs);
 	   $UserId = $Row['user_id'];
 	   $UserEmail = $Row['user_email'];

            // echo $UserEmail; 
           // Если идентификаторы совпали - меняем пароль
	   if ($UserId > 0)
	   {
  		$NewPassword = GeneratePassword(6);
		
                 // пишем в базу
	         $sql = "update  mmb.Users  set user_password = '".md5($NewPassword)."', user_sessionfornewpassword = '' where user_id = ".$UserId;
              //   echo $sql;
		 $rs = MySqlQuery($sql);  

		$statustext = 'Пароль: '.$NewPassword;
		
		SendMail($UserEmail,'New password: '.$NewPassword);

                
            }

            
            $changepasswordsessionid = "";
            $action = "";
                
   } elseif ($action == "UserLogout")  {
     // Выход 

	        CloseSession($_POST['sessionid'], 3);
                $SessionId = ""; 
                $_POST['sessionid'] = "";
		$action = "";
		$view = "MainPage";
	

   } elseif ($action == "SendRequestForSendPassword")  {
   // Обработка случая отправки пароля пользователю

         if ($login == "") {

            $statustext = "Вы не задали имя пользователя!";
            $alert = 1; 
            return; 

         } 

         $Connection = mssql_connect($ServerName, $WebUserName, $WebUserPassword);

         if ($Connection <= 0) {
               $statustext = "Ошибка соединения с сервером.";
               $alert = 1; 
               return; 
         }

         $mail = trim($login).'@garant.ru';
 	     $tempemployeeid = uniqid(""); 
         mssql_select_db($ScifDBName, $Connection);
         $sql = "declare @nResult tinyint exec p_SendRequestForSendPassword '".$login."', 
		            '".$MyPHPScript."', '".$tempemployeeid."', @nResult OUTPUT select @nResult as result";

//		 echo $sql;

		 $rs = mssql_query($sql, $Connection);  
         $Result =  mssql_result($rs, 0, 'result');  

         // Смотрим результат
         if ($Result <= 0) {

           $statustext = "Пользователь ".$login." не найден!" ;
           $alert = 1; 

         } elseif ($Result == 1 ) {

              $statustext = "Не указан почтовый адрес!";
              $alert = 1; 
  
         } elseif ($Result == 2 ) {

           $statustext = "Отключен режим отправки пароля по почте" ;
           $alert = 1; 

         } elseif ($Result == 3 ) {

           $statustext = "Письмо выслано." ;
           $alert = 1; 

         } 
         // Конец проверки

         mssql_close($Connection);

         // сбрасываем режим 
	 $action = "";


   } elseif ($action == "SendPassword")  {
   // Обработка случая отправки пароля пользователю

         if ($tempemployeeid == "") {

            $statustext = "Нет временного идентификатора пользователя!";
            $alert = 1; 
            return; 

         } 

         $Connection = mssql_connect($ServerName, $WebUserName, $WebUserPassword);

         if ($Connection <= 0) {
               $statustext = "Ошибка соединения с сервером.";
               $alert = 1; 
               return; 
         }

         $mail = trim($login).'@garant.ru';
         mssql_select_db($ScifDBName, $Connection);
         $sql = "declare @nResult tinyint exec p_SendPassword '".$tempemployeeid."', NULL, @nResult OUTPUT select @nResult as result";
         $rs = mssql_query($sql, $Connection);  
         $Result =  mssql_result($rs, 0, 'result');  

         // Смотрим результат
         if ($Result <= 0) {

           $statustext = "Пользователь ".$login." не найден!" ;
           $alert = 1; 

         } elseif ($Result == 1 ) {

              $statustext = "Не указан почтовый адрес!";
              $alert = 1; 
  
         } elseif ($Result == 2 ) {

           $statustext = "Отключен режим отправки пароля по почте" ;
           $alert = 1; 

         } elseif ($Result == 3 ) {

           $statustext = "Ошибка генерации пароля! " ;
           $alert = 1; 

         } elseif ($Result == 4 ) {

           $statustext = "Пароль выслан. Рекомендуется удалить это письмо после прочтения." ;
           $alert = 1; 

         } elseif ($Result == 5) {

           $statustext = "Новый пароль создан и выслан. Рекомендуется удалить это письмо после прочтения." ;
           $alert = 1; 

         } 
         // Конец проверки

         mssql_close($Connection);

         // сбрасываем режим 
	 $action = "";



   } elseif ($action == "ViewFormForChangePassword")  {

         // Обработка случая вывода формы для смены пароля
        
         $view = "ChangePassword";
         $menupad = "ChangePassword";
         $statustext = "Сотрудник: ".$employeename.", табельный номер: ".$tabnum ;



   } elseif ($action == "ChangePassword")  {

         // Обработка случая смены пароля пользователем
        

         if ($password == "") {

           $statustext = "Вы не ввели пароль!";
           $alert = 1; 
           return;

         } elseif ($newpassword == "") {

           $statustext = "Пароль не может быть пустым!";
           $alert = 1; 
           return;

         } elseif ($newpassword == $login) {

           $statustext = "Пароль не может совпадать с имененм пользователя!";
           $alert = 1; 
           return;

         } elseif (strlen(trim($newpassword)) < 6) {

           $statustext = "Пароль не может содержать меньше 6 символов!";
           $alert = 1; 
           return;

         } 
         // Конец первичной проверки данных

         $Connection = mssql_connect($ServerName, $WebUserName, $WebUserPassword);

         if ($Connection <= 0) {
              $statustext = "Ошибка соединения с сервером.";
              $alert = 1; 
              return; 
         }

         mssql_select_db($ScifDBName, $Connection);
         $sql = "declare @nResult tinyint  exec p_ChangePassword '".$sessionid."', '".$password."', '".$newpassword."', @nResult OUTPUT select @nResult as result";

		 $rs = mssql_query($sql, $Connection);  


         $Result =  mssql_result($rs, 0, 'result');  

         // Смотрим результат
         if ($Result <= 0) {

              $statustext = "Пользователь ".$login." не найден!";
              $alert = 1; 
 
         } elseif ($Result == 1 ) {

           $statustext = "Неправильный пароль" ;
           $alert = 1; 

         } elseif ($Result >= 2 and $Result <= 4) {

           $statustext = "Пароль изменен." ;
           $alert = 1; 

         } elseif ($Result >= 5) {

           $statustext = "Пароль изменен. Выслано письмо с новым паролем." ;
           $alert = 1; 
         } 
         // Конец проверки

         $password = ""; 
         $newpassword = ""; 

         mssql_close($Connection);

   } elseif ($action == "ViewFormForSecurity")  {

         // Обработка случая вывода формы для настроек безопасности
        
         $view = "ViewSecurity";
         $menupad = "Security";
         $statustext = "Сотрудник: ".$employeename.", табельный номер: ".$tabnum ;



   } elseif ($action == "ChangeIp")  {

         // Обработка случая смены ip пользователем
        


         $Connection = mssql_connect($ServerName, $WebUserName, $WebUserPassword);

         if ($Connection <= 0) {
              $statustext = "Ошибка соединения с сервером.";
              $alert = 1; 
              return; 
         }

         mssql_select_db($ScifDBName, $Connection);
         $sql = "declare @nResult tinyint  exec p_ChangeIp '".$sessionid."', '".$employeeip."', @nResult OUTPUT select @nResult as result";
         $rs = mssql_query($sql, $Connection);  


         $Result =  mssql_result($rs, 0, 'result');  

         // Смотрим результат
         if ($Result <= 0) {

              $statustext = "Пользователь не найден!";
              $alert = 1; 
  
         } elseif ($Result == 1 ) {


//         $statustext = "Действие выполнено. " ;

         } 
         // Конец проверки


         mssql_close($Connection);

         $view = "ViewSecurity";
         $menupad = "Security";
         $statustext = "Сотрудник: ".$employeename.", табельный номер: ".$tabnum ;


   } elseif ($action == "ChangeSendPasswordFlag")  {

         // Обработка случая смены запрета на отправка пароля по почте
        


         $Connection = mssql_connect($ServerName, $WebUserName, $WebUserPassword);

         if ($Connection <= 0) {
              $statustext = "Ошибка соединения с сервером.";
              $alert = 1; 
              return; 
         }

         mssql_select_db($ScifDBName, $Connection);
         $sql = "declare @nResult tinyint  exec p_ChangeSendPasswordFlag '".$sessionid."', ".$employeenosendpassword.", @nResult OUTPUT select @nResult as result";
         $rs = mssql_query($sql, $Connection);  


         $Result =  mssql_result($rs, 0, 'result');  

         // Смотрим результат
         if ($Result <= 0) {

              $statustext = "Пользователь не найден!";
              $alert = 1; 
 
         } elseif ($Result == 1 ) {


//         $statustext = "Действие выполнено. " ;

         } 
         // Конец проверки


         mssql_close($Connection);

         $view = "ViewSecurity";
         $menupad = "Security";
         $statustext = "Сотрудник: ".$employeename.", табельный номер: ".$tabnum ;


   } else {
   // если никаких действий не требуется

   //  $statustext = "<br/>";
   }

//	print('view = '.$view.' action = '.$action);
   
?>