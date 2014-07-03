<?php
// +++++++++++ Просмотр объединения пользователей ++++++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

?>
<script language = "JavaScript">

        


	// Посмотреть профиль пользователя
	function ViewUserInfo(userid)
	{
		document.UnionUsersForm.UserId.value = userid;
		document.UnionUsersForm.action.value = "UserInfo";
		document.UnionUsersForm.submit();
	}
	
	
	

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
		print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\n");
		
		

	        print('</form>'."\r\n");

               print('</br>'."\r\n");
 	       print('<div style = "margin-top: 15px;" align = "left">Журнал заявок на объединение пользователей</div>'."\r\n");
               print('</br>'."\r\n");

           // Пользователю показываем то, что он запрашивал или где он участвовал
          $WhereString = ' where (uul.user_id = '.$UserId.' or uul.user_parentid = '.$UserId.' )';
	  
	  // Администратору показываем все записи
	  if ($Administrator) {$WhereString = '';} 

	  $sql = "  select u.user_name, 
			   CASE WHEN uul.union_status = 1 THEN 'Запрос'
			        WHEN uul.union_status = 2 THEN 'Объединены'
			        WHEN uul.union_status = 3 THEN 'Отмена объединения'
			        WHEN uul.union_status = 0 THEN 'Отклонено'
				ELSE ''
			   END as unionstatus,
			   CASE WHEN COALESCE(u2.user_noshow, 0) = 1 and u2.user_id <> ".$UserId."  and 0 = ".(int)($Administrator)." THEN '".$Anonimus."' ELSE u2.user_name END as user_parentname, 
			   uul.user_parentid  as user_parentid, 
			   uul.user_id  as user_id, 
			   uul.userunionlog_id as log_id,
			   DATE_FORMAT(uul.userunionlog_dt, '%d.%m %H:%i:%s') as log_dt
	            from  UserUnionLogs uul
		          inner join Users u
			  on u.user_id = uul.user_id
		          inner join Users u2
			  on u2.user_id = uul.user_parentid 
		    ".$WhereString." 
 		    order by log_id DESC
		  "; 
	
	  	//echo 'sql '.$sql;
                $Result = MySqlQuery($sql);
	

		$tdstyle = 'padding: 5px 0px 2px 5px;';		
              //  $tdstyle = '';		
                $thstyle = 'padding: 5px 0px 0px 5px;';		
               // $thstyle = '';		

                $ColumnWidth = 350;


		print('<table border = "1" cellpadding = "0" cellspacing = "0" style = "font-size: 80%">'."\r\n");  

		print('<tr class = "gray">
 	                 <td width = "150" style = "'.$thstyle.'">Пользовтаель</td>
		         <td width = "300" style = "'.$thstyle.'">Объединение с</td>
 	                 <td width = "200" style = "'.$thstyle.'">Статус</td>
 	                 <td width = "150" style = "'.$thstyle.'">Создана</td>
			 <td width = "400" style = "'.$thstyle.'">Возможные действия</td>
			 </tr>'."\r\n");
		
	        // Сканируем команды
		while ($Row = mysql_fetch_assoc($Result))
		{
	 	//   print('<tr class = "'.$TrClass.'">'."\r\n");
                     print('<tr>'."\r\n");
		     print('<td align = "left" style = "'.$tdstyle.'">'.$Row['user_name'].'</td>'."\r\n");
		     print('<td align = "left" style = "'.$tdstyle.'">'.$Row['user_parentname'].'</td>'."\r\n");
                     print('<td align = "center" style = "'.$tdstyle.'">'.$Row['unionstatus'].'</td>'."\r\n");
		     print('<td align = "left" style = "'.$tdstyle.'">'.$Row['log_dt'].'</td>'."\r\n");

		     print('<td align = "left" style = "'.$tdstyle.'">'."\r\n");


                     if (CanApproveUserUnion($Administrator, $Row['log_id'], $UserId)) {
		     
		       print('<input type="button" style = "margin-left: 15px;" onClick = "javascript: ApproveUnion('.$Row['log_id'].', '.$UserId.'); "  name="ApproveButton" value="Подтвердить" tabindex = "'.++$TabIndex.'">'."\r\n");

		     }

                     if (CanRejectUserUnion($Administrator, $Row['log_id'], $UserId)) {
		     
		       print('<input type="button" style = "margin-left: 15px;" onClick = "javascript: RejectUnion('.$Row['log_id'].', '.$UserId.'); "  name="RejectButton" value="Отклонить" tabindex = "'.++$TabIndex.'">'."\r\n");

		     }

                     if (CanRollBackUserUnion($Administrator, $Row['log_id'], $UserId)) {
		     
		       print('<input type="button" style = "margin-left: 15px;" onClick = "javascript: RollBackUnion('.$Row['log_id'].', '.$UserId.'); "  name="RollBackButton" value="Восстановить" tabindex = "'.++$TabIndex.'">'."\r\n");

		     }

                     print('&nbsp;</tr>'."\r\n");
			
		}	
                // Конец циклда по журанлу объединений

		mysql_free_result($Result);
		
		print('</table>'."\r\n");
		
  	  
?>
		

		</br>



