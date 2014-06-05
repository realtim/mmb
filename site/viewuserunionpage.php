<?php
// +++++++++++ Просмотр объединения пользователей ++++++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

?>
<script language = "JavaScript">

        
	
	
	function HideUserTeamInUnion(teamunionlogid,teamid)
	{ 
		document.UnionTeamsForm.TeamUnionLogId.value = teamunionlogid;
		document.UnionTeamsForm.TeamId.value = teamid;
		document.UnionTeamsForm.action.value = "HideTeamInUnion";
		document.UnionTeamsForm.submit();
	}


	// Посмотреть профиль пользователя
	function ViewUserInfo(userid)
	{
		document.UnionTeamsForm.UserId.value = userid;
		document.UnionTeamsForm.action.value = "UserInfo";
		document.UnionTeamsForm.submit();
	}
	
	
	function ValidateUnionUsersForm()
	{
		document.UnionUsersForm.action.value = "UnionUsers";
		return true;
	}
	// Конец проверки правильности заполнения формы


	// ОтклонитьФункция очистки  текущего объединения
	function ClearUnionTeams()
	{
		document.UnionTeamsForm.action.value = "ClearUnionTeams";
		document.UnionTeamsForm.submit();
	}

	// Функция отмены текущего объединения
	function CancelUnionUsers(parentteamid)
	{
		document.UnionUsersForm.action.value = "CancelUnionUsers";
		document.UnionUsersForm.UserId.value = parentuserid;
		document.UnionUsersForm.submit();
	}



</script>

<?php


               $TabIndex = 0;
	       
	       // Выводим список команд из текущего объединения
           	print('<form  name = "UnionTeamsForm"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
                print('<input type = "hidden" name = "action" value = "">'."\r\n");
	        print('<input type = "hidden" name = "TeamUnionLogId" value = "0">'."\n");
	        print('<input type = "hidden" name = "TeamId" value = "0">'."\n");
	        print('<input type = "hidden" name = "UserId" value = "0">'."\n");
		print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\n");
		
		
/*                 
		$sql = "select tul.teamunionlog_id, t.team_id, t.team_num,
		               t.team_name, tul.teamunionlog_dt, 
			       t.team_result, t.team_progress
		        from  TeamUnionLogs tul
			      inner join Teams t
			      on t.team_id = tul.team_id
			where tul.teamunionlog_hide = 0 
                              and tul.union_status = 1
			order by t.team_id "; 
                
		//echo 'sql '.$sql;
		
		$Result = MySqlQuery($sql);

                $RowsCount = mysql_num_rows($Result);
	
		
		if ($RowsCount > 0)
		{

	 	       print('<div style = "margin-top: 5px;" align = "left">Текущее объединение</div>'."\r\n");
	               print('</br>'."\r\n");
		
			while ($Row = mysql_fetch_assoc($Result))
			{
			  
			  print('<div align = "left" style = "margin-top: 5px;">'."\r\n");
			  print(' '.$Row['team_num'].' <a href = "javascript:ViewTeamInfo('.$Row['team_id'].');">'.$Row['team_name'].'</a>  '.$Row['team_result'].' '."\r\n");
                          print('<input type="button" style = "margin-left: 15px;" onClick = "javascript: if (confirm(\'Вы уверены, что хотите исключить команду из текущего объединения? \')) { HideTeamInUnion('.$Row['teamunionlog_id'].','.$Row['team_id'].'); }"  name="TeamHideButton" value="Скрыть" tabindex = "'.++$TabIndex.'">'."\r\n");
	                  print('</div>'."\r\n");
	  	  			
			  $sql = "select tu.teamuser_id, u.user_name, u.user_birthyear, tu.level_id, u.user_id
				  from TeamUsers tu
					inner join Users u
					on tu.user_id = u.user_id
				   where tu.teamuser_hide = 0 and team_id = ".$Row['team_id'];
			  $TeamUsersResult = MySqlQuery($sql);

			  while ($TeamUsersRow = mysql_fetch_assoc($TeamUsersResult))
			  {
				print('<div style="margin-top: 5px; margin-left: 15px;">'."\n");
				print('<a href="javascript:ViewUserInfo('.$TeamUsersRow['user_id'].');">'.$TeamUsersRow['user_name'].'</a> '.$TeamUsersRow['user_birthyear']."\n");
				print('</div>'."\n");
			  }
				mysql_free_result($TeamUsersResult);

			}

                        print('</br>'."\r\n");
			
			if ($viewsubmode == "ReturnAfterError")
			{
				$TeamName = $pTeamName;
		
			} else {
				$TeamName = 'Название объединённой команды';
			}	
			$UnionButtonText = 'Объединить';
			
			print('<input type="text" name="TeamName" size="50" value="'.$TeamName.'" tabindex="'.(++$TabIndex)
				.'" onclick="javascript: if (trimBoth(this.value) == \''.$TeamName.'\') {this.value=\'\';}"'
				.' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$TeamName.'\';}" title="Название объединённой команды">'."\n\n");

                        print('</br>'."\r\n");

			print('<div style="margin-top: 5px;">'."\n");
				print('<input type="button" onClick="javascript: if (ValidateUnionTeamsForm()) submit();" name="UnionButton" value="'.$UnionButtonText.'" tabindex="'.(++$TabIndex).'">'."\n");
				print('<input type="button" onClick="javascript: if (confirm(\'Вы уверены, что хотите убрать все команды из текущего объединения? \')) {ClearUnionTeams();}" name="CancelButton" value="Очистить объединение" tabindex="'.(++$TabIndex).'">'."\n");
			print('</div>'."\n");


		} else {

			  print('<div class= "input" align = "left">Нет команд в текущем объединении</div>'."\r\n");
		}
	        print('</form>'."\r\n");
                mysql_free_result($Result);
*/

	        print('</form>'."\r\n");

               print('</br>'."\r\n");
 	       print('<div style = "margin-top: 15px;" align = "left">Журнал заявок на объединение пользователей</div>'."\r\n");
               print('</br>'."\r\n");



	  $sql = "  select u.user_name, 
			   CASE WHEN uul.union_status = 1 THEN 'Запрос'
			        WHEN uul.union_status = 2 THEN 'Объединены'
			        WHEN uul.union_status = 3 THEN 'Отмена объединения'
			        WHEN uul.union_status = 0 THEN 'Отклонено'
				ELSE ''
			   END as unionstatus,
			   u2.user_name as user_parentname, 
			   uul.user_parentid  as user_parentid, 
			   uul.user_id  as user_id, 
			   uul.userunionlog_id as log_id,
			   DATE_FORMAT(uul.userunionlog_dt, '%d.%m %H:%i:%s') as log_dt
	            from  UserUnionLogs uul
		          inner join Users u
			  on u.user_id = uul.user_id
		          inner join Users u2
			  on u2.user_id = uul.user_parentid
 		    order by log_id DESC
		  "; 
	
	  	//echo 'sql '.$sql;
                $Result = MySqlQuery($sql);
	

			  //DATE_FORMAT(tul.teamunionlog_dt, '%d.%m %H:%i:%s') as log_dt,
			 // tul.team_id,
			 // COALESCE((select count(*) from  TeamUnionLogs where team_parentid = tul.team_parentid), 0) as teamcount




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
		     print('<td align = "left" style = "'.$tdstyle.'">'.$Row['log_dt'].'</td>'."\r\n");
                     print('</tr>'."\r\n");

/*
			print('<a href = "javascript:ViewTeamInfo('.$Row['team_id'].');">'.
		     	       $Row['team_name'].'</a> '."\r\n");
*/

  /*
                     if ($Row['unionstatus'] == 'Объединены') 
		     {
                        // Команда объединена
			$sql = "select tu.teamuser_id, u.user_name, u.user_birthyear,	
					       tu.level_id, u.user_id, l.level_name,
					       t.team_name as oldteam_name,
					       t.team_num as  oldteam_num,
			                       DATE_FORMAT(tul.teamunionlog_dt, '%d.%m %H:%i:%s')  as unionlog_dt  
				        from  TeamUsers tu
					      inner join  Users u
					      on tu.user_id = u.user_id
		                              left outer join Levels l
					      on tu.level_id = l.level_id
					      inner join  TeamUsers tu2
					      on tu2.user_id = tu.user_id
					      inner join  Teams t
					      on t.team_id = tu2.team_id
					      inner join TeamUnionLogs tul
					      on t.team_id = tul.team_id
					         and  t.team_parentid = tul.team_parentid
					where tu.teamuser_hide = 0 
					      and tu.team_id = ".$Row['team_id']."
					      and t.team_parentid = ".$Row['team_id']."
					      order by unionlog_dt";

		     } elseif ($Row['unionstatus'] == 'Отмена объединения')  {

				$sql = "select null as teamuser_id,
				               '' as user_name,
					       '' as user_birthyear,	
					       '' as level_id, 
					       null as user_id,
					       '' as level_name,
					       t.team_name as oldteam_name,
					       t.team_num as  oldteam_num,
			                       DATE_FORMAT(tul.teamunionlog_dt, '%d.%m %H:%i:%s')  as unionlog_dt  
				        from  Teams t
					      inner join TeamUnionLogs tul
					      on t.team_id = tul.team_id
					where tul.team_parentid = ".$Row['team_id']."
					      order by unionlog_dt";

			
		     } else {

                        // Команда не объединена
			 
				$sql = "select tu.teamuser_id, u.user_name, u.user_birthyear,	
					       tu.level_id, u.user_id, l.level_name,
					       '' as oldteam_name,
					       '' as oldteam_num,
					       '' as  unionlog_dt  
				        from  TeamUsers tu
					      inner join  Users u
					      on tu.user_id = u.user_id
		                              left outer join Levels l
					      on tu.level_id = l.level_id
					where tu.teamuser_hide = 0 and team_id = ".$Row['team_id'];
			 
			 
		      }			
			
	
	*/

/*
			//echo 'sql '.$sql;
		      $UserResult = MySqlQuery($sql);

                      $UserCount = 0;
                      // Сканируем состав
		      while ($UserRow = mysql_fetch_assoc($UserResult))
		      {
			$UserCount++;
			  print('<div>'.$UserRow['unionlog_dt'].'  '.$UserRow['oldteam_name'].'  '.
			                $UserRow['oldteam_num'].'  '. "\r\n"); 
			  
			  if ($Row['unionstatus'] <> 'Отмена объединения') 
			  {
			        print('<a href = "javascript:ViewUserInfo('.$UserRow['user_id'].');">'.
			                 $UserRow['user_name'].'</a> '.$UserRow['user_birthyear']."\r\n");
			  }
			  print('</div>'."\r\n");
		      }  
		      mysql_free_result($UserResult);
	
	              if ($UserCount == 0)
		      {
			print('&nbsp;'."\r\n");
		      }
		      print('</td>'."\r\n");
*/
			
		}	

		mysql_free_result($Result);
		
		print('</table>'."\r\n");
		
  	  
?>
		

		</br>



