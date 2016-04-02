<?php
// +++++++++++ Просмотр объединения пользователей ++++++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

?>
<script language = "JavaScript">

        
	// Подтвердить объединение
	function ApproveUnion(logid, userid)
	{
		document.UnionUsersForm.action.value = "ApproveUnion";
		document.UnionUsersForm.UserUnionLogId.value = logid;
		document.UnionUsersForm.UserId.value = userid;
		document.UnionUsersForm.submit();
	}

	// Отклонить объединение
	function RejectUnion(logid, userid)
	{
		document.UnionUsersForm.action.value = "RejectUnion";
		document.UnionUsersForm.UserUnionLogId.value = logid;
		document.UnionUsersForm.UserId.value = userid;
		document.UnionUsersForm.submit();
	}


	// Откатить (вернуть всё, как было)  объединение
	function RollBackUnion(logid, userid)
	{
		document.UnionUsersForm.action.value = "RollBackUnion";
		document.UnionUsersForm.UserUnionLogId.value = logid;
		document.UnionUsersForm.UserId.value = userid;
		document.UnionUsersForm.submit();
	}



</script>

<?php


               $TabIndex = 0;
	       
	       // Выводим список команд из текущего объединения
           	print('<form  name = "UnionUsersForm"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
                print('<input type = "hidden" name = "action" value = "">'."\r\n");
	        print('<input type = "hidden" name = "UserUnionLogId" value = "0">'."\n");
	        print('<input type = "hidden" name = "UserId" value = "0">'."\n");

		

	        print("</form>\r\n");

               print("</br>\r\n");
		print('<div style = "margin-top: 15px;" align = "left">Журнал заявок на слияние пользователей</div>'."\r\n");
               print("</br>\r\n");

           // Администратору показываем все записи
           // Пользователю -- то, что он запрашивал или где он участвовал
          $WhereString = $Administrator ? '' :  " where (uul.user_id = $UserId or uul.user_parentid = $UserId )";

	  $sql = "  select u.user_name, 
			   CASE WHEN uul.union_status = 1 THEN 'Запрос'
			        WHEN uul.union_status = 2 THEN 'Слияние произведено'
			        WHEN uul.union_status = 3 THEN 'Отмена слияния'
			        WHEN uul.union_status = 0 THEN 'Отклонено'
				ELSE ''
			   END as unionstatus,
			   CASE WHEN COALESCE(u2.user_noshow, 0) = 1 and u2.user_id <> $UserId  and 0 = ".(int)($Administrator)." THEN '$Anonimus' ELSE u2.user_name END as user_parentname,
			   uul.user_parentid  as user_parentid, 
			   uul.user_id  as user_id, 
			   uul.userunionlog_id as log_id,
			   DATE_FORMAT(uul.userunionlog_dt, '%d.%m %H:%i:%s') as log_dt
	            from  UserUnionLogs uul
		          inner join Users u
			  on u.user_id = uul.user_id
		          inner join Users u2
			  on u2.user_id = uul.user_parentid 
		    $WhereString
 		    order by log_id DESC";
	
	  	//echo 'sql '.$sql;
                $Result = MySqlQuery($sql);
	
		print("<table class=\"std\">\r\n");
		print('<tr class="head gray">
 	                 <td width="150">Пользователь</td>
		         <td width="300">Слияние с</td>
 	                 <td width="200">Статус</td>
 	                 <td width="150">Создана</td>
			 <td width="400">Возможные действия</td>
			 </tr>'."\r\n");
		
	        // Сканируем команды
		while ($Row = mysql_fetch_assoc($Result))
		{
	 	//   print('<tr class = "'.$TrClass.'">'."\r\n");
                     print("<tr>\r\n");
		     print("<td>" . CMmbUI::toHtml($Row['user_name']) . "</td>\r\n");
		     print("<td>" . CMmbUI::toHtml($Row['user_parentname']) . "</td>\r\n");
                     print("<td align=\"center\">{$Row['unionstatus']}</td>\r\n");
		     print("<td>{$Row['log_dt']}</td>\r\n");

		     print("<td>\r\n");


                     if (CanApproveUserUnion($Administrator, $Row['log_id'], $UserId)) {
		     
		       print('<input type="button" style = "margin-left: 15px;" onClick = "javascript: ApproveUnion('.$Row['log_id'].', '.$UserId.'); "  name="ApproveButton" value="Подтвердить" tabindex = "'.++$TabIndex.'">'."\r\n");

		     }

                     if (CanRejectUserUnion($Administrator, $Row['log_id'], $UserId)) {
		     
		       print('<input type="button" style = "margin-left: 15px;" onClick = "javascript: RejectUnion('.$Row['log_id'].', '.$UserId.'); "  name="RejectButton" value="Отклонить" tabindex = "'.++$TabIndex.'">'."\r\n");

		     }

                     if (CanRollBackUserUnion($Administrator, $Row['log_id'], $UserId)) {
		     
		       print('<input type="button" style = "margin-left: 15px;" onClick = "javascript: RollBackUnion('.$Row['log_id'].', '.$UserId.'); "  name="RollBackButton" value="Восстановить" tabindex = "'.++$TabIndex.'">'."\r\n");

		     }

                     print("&nbsp;</tr>\r\n");
		}
                // Конец циклда по журанлу объединений

		mysql_free_result($Result);
		
		print("</table>\r\n");
?>
		
		<br/>



