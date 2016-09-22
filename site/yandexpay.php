<?php

 /*
 Скрипт опознания денег
 Документация на http://api.yandex.ru/money/doc/dg/reference/notification-p2p-incoming.xml
 
 Сделал на http, хотя рекомендуют https
 */

 // Общие настройки
  include("settings.php");
  // Библиотека функций
  include("functions.php");

  //notification_type&operation_id&amount&currency&datetime&sender&codepro&notification_secret&label


   if (isset($_POST['amount']) && is_numeric($_POST['amount']))
      $PaymentSum = floatval($_POST['amount']);
   else
      $PaymentSum = -1;

	if ($PaymentSum <= 0)
	{
	print('ff');
        return;
	}

   
   $Sql = "insert into Payments (paymentsource_id, payment_sum) values (1, $PaymentSum)";
   $Result = MySqlQuery($Sql);
   mysql_free_result($Result);


    print('qqq');
?>
