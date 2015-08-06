<?php
// +++++++++++ Просмотр объединения команд ++++++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

?>
<script language = "JavaScript">

        
	
	// Посмотреть профиль команды
	/*function ViewTeamInfo(teamid)
	{ 
	  document.UnionTeamsForm.TeamId.value = teamid;
	  document.UnionTeamsForm.action.value = "TeamInfo";
	  document.UnionTeamsForm.submit();
	}*/
	
	function HideTeamInUnion(teamunionlogid,teamid)
	{ 
		document.UnionTeamsForm.TeamUnionLogId.value = teamunionlogid;
		document.UnionTeamsForm.TeamId.value = teamid;
		document.UnionTeamsForm.action.value = "HideTeamInUnion";
		document.UnionTeamsForm.submit();
	}


	// Посмотреть профиль пользователя
	/*function ViewUserInfo(userid)
	{
		document.UnionTeamsForm.UserId.value = userid;
		document.UnionTeamsForm.action.value = "UserInfo";
		document.UnionTeamsForm.submit();
	}*/
	
	
	function ValidateUnionTeamsForm()
	{
		document.UnionTeamsForm.action.value = "UnionTeams";
		return true;
	}
	// Конец проверки правильности заполнения формы


	// Функция очистки  текущего объединения
	function ClearUnionTeams()
	{
		document.UnionTeamsForm.action.value = "ClearUnionTeams";
		document.UnionTeamsForm.submit();
	}

	// Функция отмены текущего объединения
	function TryCancelTeamUnion(parentteamid)
	{
		if (!confirm('Вы уверены, что хотите отменить объединение?'))
			return false;

		document.UnionTeamsForm.action.value = "CancelUnionTeams";
		document.UnionTeamsForm.TeamId.value = parentteamid;
		document.UnionTeamsForm.submit();
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

                 
		$sql = "select tul.teamunionlog_id, t.team_id, t.team_num,
		               t.team_name, tul.teamunionlog_dt, 
			       t.team_result
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
	               print("</br>\r\n");
		
			while ($Row = mysql_fetch_assoc($Result))
			{
			  
			  print('<div align="left" style="margin-top: 5px;">'."\r\n");
			  print(' '.$Row['team_num'].' <a href="?TeamId='.$Row['team_id'].'">'.$Row['team_name']."</a>  {$Row['team_result']} \r\n");
                          print('<input type="button" style="margin-left: 15px;" onClick="javascript: if (confirm(\'Вы уверены, что хотите исключить команду из текущего объединения? \')) { HideTeamInUnion('.$Row['teamunionlog_id'].','.$Row['team_id'].'); }"  name="TeamHideButton" value="Скрыть" tabindex = "'.++$TabIndex.'">'."\r\n");
	                  print('</div>'."\r\n");
	  	  			
			  $sql = "select tu.teamuser_id, CASE WHEN COALESCE(u.user_noshow, 0) = 1 THEN '$Anonimus' ELSE u.user_name END as user_name, u.user_birthyear, u.user_id
				  from TeamUsers tu
					inner join Users u
					on tu.user_id = u.user_id
				   where tu.teamuser_hide = 0 and team_id = {$Row['team_id']}";
			  $TeamUsersResult = MySqlQuery($sql);

			  while ($TeamUsersRow = mysql_fetch_assoc($TeamUsersResult))
			  {
				print('<div style="margin-top: 5px; margin-left: 15px;">'."\n");
				print('<a href="?UserId='.$TeamUsersRow['user_id'].'">'.$TeamUsersRow['user_name']."</a> {$TeamUsersRow['user_birthyear']}\n");
				print('</div>'."\n");
			  }
				mysql_free_result($TeamUsersResult);

			}

                        print("</br>\r\n");
			
			if ($viewsubmode == "ReturnAfterError")
			{
				$TeamName = $pTeamName;
		
			} else {
				$TeamName = 'Название объединённой команды';
			}	
			$UnionButtonText = 'Объединить';
			
			print('<input type="text" name="TeamName" size="50" value="'.$TeamName.'" tabindex="'.(++$TabIndex). '" '
				. CMmbUI::placeholder($TeamName) . ' title="Название объединённой команды">'."\n\n");

                        print("</br>\r\n");

			print('<div style="margin-top: 5px;">'."\n");
				print('<input type="button" onClick="javascript: if (ValidateUnionTeamsForm()) submit();" name="UnionButton" value="'.$UnionButtonText.'" tabindex="'.(++$TabIndex).'">'."\n");
				print('<input type="button" onClick="javascript: if (confirm(\'Вы уверены, что хотите убрать все команды из текущего объединения? \')) {ClearUnionTeams();}" name="CancelButton" value="Очистить объединение" tabindex="'.(++$TabIndex).'">'."\n");
			print("</div>\n");


		} else {

			  print('<div class= "input" align = "left">Нет команд в текущем объединении</div>'."\r\n");
		}
	        print("</form>\r\n");
                mysql_free_result($Result);


               print("<br/>\r\n");
 	       print('<div style="margin-top: 15px;" align = "left">История объединений</div>'."\r\n");
               print("<br/>\r\n");



	  $sql = "  select MAX(t.team_num) as team_num, 
			   MAX(t.team_name) as team_name, 
			   CASE WHEN MAX(tul.union_status) = 2 THEN 'Объединены'
			        WHEN MAX(tul.union_status) = 3 THEN 'Отмена объединения'
			        WHEN MAX(tul.union_status) = 0 THEN 'Не объединена'
				ELSE ''
			   END as unionstatus,
			   tul.team_parentid  as team_id, 
			   1 as union_flag,
			   MAX(tul.teamunionlog_id) as log_id,
			   DATE_FORMAT(MAX(tul.teamunionlog_dt), '%d.%m %H:%i:%s') as log_dt,
			  d.distance_name, r.raid_name 
	            from  TeamUnionLogs tul
		          inner join Teams t
			  on t.team_id = tul.team_parentid
  			  inner join Distances d
			  on t.distance_id = d.distance_id
			  inner join Raids r
			  on d.raid_id = r.raid_id    
                    where tul.union_status <> 1
		          and tul.team_parentid is not null
		    group by tul.team_parentid 
		    union all
		    select t.team_num as team_num, 
			   t.team_name as team_name,
			   CASE WHEN tul.union_status = 2 THEN 'Объединены'
			        WHEN tul.union_status = 3 THEN 'Отмена объединения'
			        WHEN tul.union_status = 0 THEN 'Не объединена'
				ELSE ''
			  END as unionstatus,
			  tul.team_id  as team_id, 
			  0 as union_flag,
			  tul.teamunionlog_id as log_id,
			  DATE_FORMAT(tul.teamunionlog_dt, '%d.%m %H:%i:%s') as log_dt,
			  d.distance_name, r.raid_name 
	            from  TeamUnionLogs tul
		          inner join Teams t
			  on t.team_id = tul.team_id
			  inner join Distances d
			  on t.distance_id = d.distance_id
			  inner join Raids r
			  on d.raid_id = r.raid_id    
                    where tul.union_status <> 1
		          and tul.team_parentid is null
 		    order by log_id DESC
		  "; 
	
	  //	echo 'sql '.$sql;
                $Result = MySqlQuery($sql);
	

			  //DATE_FORMAT(tul.teamunionlog_dt, '%d.%m %H:%i:%s') as log_dt,
			 // tul.team_id,
			 // COALESCE((select count(*) from  TeamUnionLogs where team_parentid = tul.team_parentid), 0) as teamcount


		print("<table class=\"std\">\r\n");

		print('<tr class="head gray">
 	                 <td width="150">Дистанция</td>
 	                 <td width="200">Статус</td>
		         <td width="300">Номер и название команды</td>
			 <td width="400">Участники и команды до объединения</td>
			 </tr>'."\r\n");
		
	        // Сканируем команды
		while ($Row = mysql_fetch_assoc($Result))
		{
	 	//   print('<tr class = "'.$TrClass.'">'."\r\n");
                     print("<tr>\r\n");
		     print("<td>{$Row['raid_name']} {$Row['distance_name']}</td>\r\n");
                     print('<td align="center">'.$Row['unionstatus']."\r\n");
                                
	  	     // Вставляем кнопку отмены объединения
		     if ($Row['unionstatus'] == 'Объединены') 
		     {
			   print('<br/><input type="button" onClick="javascript: TryCancelTeamUnion('.$Row['team_id'].');" name="CancelUnionButton" value="Отменить объединение" tabindex="'.(++$TabIndex).'">'."\n");
		     }
                     print("</td>\r\n");
		     print("<td>{$Row['team_num']}\r\n");
		     
		     if ($Row['unionstatus'] <> 'Отмена объединения') 
		     {
			print('<a href="?TeamId='.$Row['team_id'].'">'.
		     	       $Row['team_name']."</a> \r\n");
                     } else {
			print("<b>{$Row['team_name']}</b>\r\n");
		     }	

                     // Для успрешно объединённых колманд есть подробное время в участниках
		     if ($Row['unionstatus'] == 'Не объединена') 
		     {
			   print($Row['log_dt']."\n");
		     }

		     print("</td>\r\n");

		     print("<td>\r\n");

                     if ($Row['unionstatus'] == 'Объединены') 
		     {
                        // Команда объединена
			$sql = "select         tu.teamuser_id, 
			                       CASE WHEN COALESCE(u.user_noshow, 0) = 1 THEN '$Anonimus' ELSE u.user_name END as user_name,
					       u.user_birthyear,	
					       u.user_id, 
					       t.team_name as oldteam_name,
					       t.team_num as  oldteam_num,
			                       DATE_FORMAT(tul.teamunionlog_dt, '%d.%m %H:%i:%s')  as unionlog_dt  
				        from  TeamUsers tu
					      inner join  Users u
					      on tu.user_id = u.user_id
					      inner join  TeamUsers tu2
					      on tu2.user_id = tu.user_id
					      inner join  Teams t
					      on t.team_id = tu2.team_id
					      inner join TeamUnionLogs tul
					      on t.team_id = tul.team_id
					         and  t.team_parentid = tul.team_parentid
					where tu.teamuser_hide = 0 
					      and tu.team_id = {$Row['team_id']}
					      and t.team_parentid = {$Row['team_id']}
					      order by unionlog_dt";

		     } elseif ($Row['unionstatus'] == 'Отмена объединения')  {

				$sql = "select null as teamuser_id,
				               '' as user_name,
					       '' as user_birthyear,	
					       null as user_id,
					       t.team_name as oldteam_name,
					       t.team_num as  oldteam_num,
			                       DATE_FORMAT(tul.teamunionlog_dt, '%d.%m %H:%i:%s')  as unionlog_dt  
				        from  Teams t
					      inner join TeamUnionLogs tul
					      on t.team_id = tul.team_id
					where tul.team_parentid = {$Row['team_id']}
					      order by unionlog_dt";

		     } else {

                        // Команда не объединена
			 
				$sql = "select tu.teamuser_id, CASE WHEN COALESCE(u.user_noshow, 0) = 1 THEN '$Anonimus' ELSE u.user_name END as user_name, u.user_birthyear,
					       u.user_id, 
					       '' as oldteam_name,
					       '' as oldteam_num,
					       '' as  unionlog_dt  
				        from  TeamUsers tu
					      inner join  Users u
					      on tu.user_id = u.user_id
					where tu.teamuser_hide = 0 and team_id = {$Row['team_id']}";
		      }
			
			//echo 'sql '.$sql;
		      $UserResult = MySqlQuery($sql);

                      $UserCount = 0;
                      // Сканируем состав
		      while ($UserRow = mysql_fetch_assoc($UserResult))
		      {
			  $UserCount++;
			  print("<div>{$UserRow['unionlog_dt']}  {$UserRow['oldteam_name']}  ".
			               $UserRow['oldteam_num']."  \r\n");
			  
			  if ($Row['unionstatus'] <> 'Отмена объединения') 
			  {
			        print('<a href="?UserId='.$UserRow['user_id'].'">'.
			                 "{$UserRow['user_name']}</a> {$UserRow['user_birthyear']}\r\n");
			  }
			  print("</div>\r\n");
		      }  
		      mysql_free_result($UserResult);
	
	              if ($UserCount == 0)
		      {
			print('&nbsp;');
		      }
		      print("</td>\r\n</tr>\r\n");
		}

		mysql_free_result($Result);
		
		print("</table>\r\n");
?>
		
		<br/>



