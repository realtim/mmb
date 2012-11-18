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


	// Функция отмены изменения
	function Cancel()
	{
		document.UnionTeamsForm.action.value = "CancelUnionTeams";
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
			
			$TeamName = 'Название объединённой команды';
			$UnionButtonText = 'Объединить';
			
			print('<input type="text" name="TeamName" size="50" value="'.$TeamName.'" tabindex="'.(++$TabIndex)
				.'" onclick="javascript: if (trimBoth(this.value) == \''.$TeamName.'\') {this.value=\'\';}"'
				.' onblur="javascript: if (trimBoth(this.value) == \'\') {this.value=\''.$TeamName.'\';}" title="Название объединённой команды">'."\n\n");

                        print('</br>'."\r\n");

			print('<div style="margin-top: 5px;">'."\n");
				print('<input type="button" onClick="javascript: if (ValidateUnionTeamsForm()) submit();" name="UnionButton" value="'.$UnionButtonText.'" tabindex="'.(++$TabIndex).'">'."\n");
				print('<input type="button" onClick="javascript: if (confirm(\'Вы уверены, что хотите убрать все команды из текущего объединения? \')) {Cancel();}" name="CancelButton" value="Очистить объединение" tabindex="'.(++$TabIndex).'">'."\n");
			print('</div>'."\n");


		} else {

			  print('<div class= "input" align = "left">Нет команд в текущем объединении</div>'."\r\n");
		}
	        print('</form>'."\r\n");
                mysql_free_result($Result);



?>
<!--		
		</td></tr>
		</table>
-->
		

		</br>



