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

	
</script>

<?php


	       // Выводим список команд из текущего объединения
           	print('<form  name = "UnionTeamsForm"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
                print('<input type = "hidden" name = "action" value = "">'."\r\n");
	        print('<input type = "hidden" name = "TeamUnionLogId" value = "0">'."\n");
	        print('<input type = "hidden" name = "TeamId" value = "0">'."\n");
		print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\n");
		
		
                 
		$sql = "select tul.teamunionlog_id, t.team_id, t.team_num,
		               t.team_name, tul.teamunionlog_dt
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
			  
			  print('<div align = "left" style = "padding-top: 5px;">'."\r\n");
			  print(''.$Row['team_num'].' <a href = "javascript:TeamUserInfo('.$Row['team_id'].');">'.$Row['team_name'].'</a>'."\r\n");
                          print('<input type="button" onClick = "javascript: if (confirm(\'Вы уверены, что хотите исключить команду из текущего объединения? \')) { HideTeamInUnion('.$Row['teamunionlog_id'].','.$Row['team_id'].'); }"  name="TeamHideButton" value="Скрыть" tabindex = "10">'."\r\n");
	                  print('</div>'."\r\n");
	  	  			}

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



