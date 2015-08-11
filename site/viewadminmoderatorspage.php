<?php
// +++++++++++ Просмотр модераторов ММБ ++++++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

?>
<script language = "JavaScript">

        
	// Функция просмотра данных о команде
	/*function ViewUserInfo(userid)
	{ 
		document.ModeratorsForm.UserId.value = userid;
		document.ModeratorsForm.action.value = "UserInfo";
		document.ModeratorsForm.submit();
	}*/
	
	function HideModerator(raidmoderatorid,userid)
	{ 
		document.ModeratorsForm.RaidModeratorId.value = raidmoderatorid;
		document.ModeratorsForm.UserId.value = userid;
		document.ModeratorsForm.action.value = "HideModerator";
		document.ModeratorsForm.submit();
	}
	
	
</script>

<?php

           	print('<form  name = "ModeratorsForm"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
                print('<input type = "hidden" name = "action" value = "">'."\r\n");
	        print('<input type = "hidden" name = "UserId" value = "0">'."\n");
	        print('<input type = "hidden" name = "RaidModeratorId" value = "0">'."\n");
		
                 
		$sql = "select u.user_id, u.user_name, rm.raidmoderator_id 
		        from  Users u
			      inner join RaidModerators rm
			      on u.user_id = rm.user_id  
			where rm.raidmoderator_hide = 0
			      and rm.raid_id = $RaidId
			order by user_name "; 
                
		//echo 'sql '.$sql;
		
		$Result = MySqlQuery($sql);

                $RowsCount = mysql_num_rows($Result);
	
		
		if ($RowsCount > 0)
		{
		
			while ($Row = mysql_fetch_assoc($Result))
			{
			  print('<div class="team_res">'."\r\n");
			  print('<a href="?UserId='.$Row['user_id'].'">'.CMmbUI::toHtml($Row['user_name']).'</a>'."\r\n");
		          print('<input type="button" onClick="javascript: if (confirm(\'Вы уверены, что хотите снять статус модератора с текущего марш-броска? \')) { HideModerator('.$Row['raidmoderator_id'].','.$Row['user_id'].'); }"  name="ModeratorHideButton" value="Скрыть" tabindex="10">'."\r\n");
	                  print("</div>\r\n");
	  	  
			}

		} else {

			  print('<div class="input" align="left">Не найдено</div>'."\r\n");
		}
	        print("</form>\r\n");
                mysql_free_result($Result);



?>
<!--		
		</td></tr>
		</table>
-->
		

		<br/>



