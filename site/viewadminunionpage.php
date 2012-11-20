<?php
// +++++++++++ Просмотр объединения команд ++++++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

?>
<script language = "JavaScript">

        
	
	// Посмотреть профиль команды
	function ViewTeamInfo(teamid)
	{ 
	  document.UnionTeamsForm.TeamId.value = teamid;
	  document.UnionTeamsForm.action.value = "TeamInfo";
	  document.UnionTeamsForm.submit();
	}
	
	function HideTeamInUnion(teamunionlogid,teamid)
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


               print('</br>'."\r\n");
 	       print('<div style = "margin-top: 15px;" align = "left">История объединений</div>'."\r\n");
               print('</br>'."\r\n");



	  $sql = "  select  t.team_num, t.team_name, 
	                   COALESCE(t2.team_num, '') as team_parentnum, 
			   COALESCE(t2.team_name, '') as team_parentname, 
			   CASE WHEN tul.union_status = 2 THEN 'Объединены'
			        WHEN tul.union_status = 3 THEN 'Отмена объединения'
			        WHEN tul.union_status = 0 THEN 'Не объединена'
				ELSE ''
			  END as unionstatus,
			  tul.team_parentid,
			  tul.team_id,
			  COALESCE((select count(*) from  TeamUnionLogs where team_parentid = tul.team_parentid), 0) as teamcount
	          from  TeamUnionLogs tul
		        inner join Teams t
			on t.team_id = tul.team_id
			left outer join Teams t2
			on tul.team_parentid = t2.team_id
                  where  tul.union_status <> 1
		  order by tul.team_parentid DESC, tul.teamunionlog_id DESC 
		  "; 
	
	  //	echo 'sql '.$sql;
                $Result = MySqlQuery($sql);
	



		$tdstyle = 'padding: 5px 0px 2px 5px;';		
                $tdstyle = '';		
                $thstyle = 'border-color: #000000; border-style: solid; border-width: 1px 1px 1px 1px; padding: 5px 0px 2px 5px;';		
                $thstyle = '';		

                $ColumnWidth = 350;


		print('<table border = "1" cellpadding = "10" style = "font-size: 80%">'."\r\n");  

		print('<tr class = "gray">
		         <td width = "200" style = "'.$thstyle.'">Номер и название исходной команды</td>
			 <td width = "200" style = "'.$thstyle.'">Участники</td>
		         <td width = "200" style = "'.$thstyle.'">Номер и название объединённой команды</td>
			 <td width = "200" style = "'.$thstyle.'">Участники</td>
 	                 <td width = "50" style = "'.$thstyle.'">Статус</td>
			 </tr>'."\r\n");
		
		$TeamsCount = mysql_num_rows($Result);


                $PredParentTeamId = 0; 
		
		while ($Row = mysql_fetch_assoc($Result))
		{


/*
			if ($TeamsCount%2 == 0) {
			  $TrClass = 'yellow';
			} else {
			  $TrClass = 'green';
			} 

			$TeamsCount--;
  
  */            

	      
			print('<tr class = "'.$TrClass.'"><td style = "'.$tdstyle.'">'.$Row['team_num'].' <a href = "javascript:ViewTeamInfo('.$Row['team_id'].');">'.
			          $Row['team_name'].'</a> </td><td style = "'.$tdstyle.'">'."\r\n");


			$sql = "select tu.teamuser_id, u.user_name, u.user_birthyear,
                                       tu.level_id, u.user_id, l.level_name 
			        from  TeamUsers tu
				     inner join  Users u
				     on tu.user_id = u.user_id
                                     left outer join Levels l
 				     on tu.level_id = l.level_id
 				where tu.teamuser_hide = 0 and team_id = ".$Row['team_id'];

			
			if (!empty($Row['team_parentid']))
			{
			$sql = $sql."	
				UNION 
				select tu.teamuser_id, u.user_name, u.user_birthyear,
                                       tu.level_id, u.user_id, l.level_name 
			        from  TeamUsers tu
				     inner join  Teams t
				     on tu.team_id = t.team_id
				     inner join  Users u
				     on tu.user_id = u.user_id
                                     left outer join Levels l
 				     on tu.level_id = l.level_id
 				where tu.teamuser_hide = 1 and tu.team_id = ".$Row['team_id']."
				       and t.team_parentid = ".$Row['team_parentid'];

			}			
				
//			echo 'sql '.$sql;
			$UserResult = MySqlQuery($sql);

			while ($UserRow = mysql_fetch_assoc($UserResult))
			{
			  print('<div><a href = "javascript:ViewUserInfo('.$UserRow['user_id'].');">'.$UserRow['user_name'].'</a> '.$UserRow['user_birthyear']."\r\n");
			  print('</div>'."\r\n");
			}  
		        mysql_free_result($UserResult);
	
			print('</td>'."\r\n");

			

			if (!empty($Row['team_parentid'])) 
			{
          
	                    if ($Row['team_parentid'] <>  $PredParentTeamId )
			    {
	                        $PredParentTeamId = $Row['team_parentid'];
	
	                         
				 
				print('<td rowspan = "'.$Row['teamcount'].'" style = "'.$tdstyle.'">'.$Row['team_parentnum'].' <a href = "javascript:ViewTeamInfo('.$Row['team_parentid'].');">'.
			          $Row['team_parentname'].'</a> </td><td rowspan = "'.$Row['teamcount'].'" style = "'.$tdstyle.'">'."\r\n");

				$sql = "select tu.teamuser_id, u.user_name, u.user_birthyear,
	                                       tu.level_id, u.user_id, l.level_name 
				        from  TeamUsers tu
					     inner join  Users u
					     on tu.user_id = u.user_id
	                                     left outer join Levels l
	 				     on tu.level_id = l.level_id
	 				where tu.teamuser_hide = 0 and team_id = ".$Row['team_parentid']; 
				//echo 'sql '.$sql;
				$UserResult = MySqlQuery($sql);


				while ($UserRow = mysql_fetch_assoc($UserResult))
				{
				  print('<div class= "input"><a href = "javascript:ViewUserInfo('.$UserRow['user_id'].');">'.$UserRow['user_name'].'</a> '.$UserRow['user_birthyear']."\r\n");
					  print('</div>'."\r\n");
				}  
			        mysql_free_result($UserResult);

				print('</td>'."\r\n");
 			        print('<td rowspan = "'.$Row['teamcount'].'" style = "'.$tdstyle.'">'.$Row['unionstatus'].'</td>'."\r\n");

			     }	

			} else {
			
				print('<td>&nbsp;</td><td>&nbsp;</td>'."\r\n");
 			        print('<td style = "'.$tdstyle.'">'.$Row['unionstatus'].'</td>'."\r\n");

			}
			print('</tr>'."\r\n");

			  
		
			
		}	

		mysql_free_result($Result);
		
		print('</table>'."\r\n");
		
  	  
?>
<!--		
		</td></tr>
		</table>
-->
		

		</br>



