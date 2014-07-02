<?php
// +++++++++++ Просмотр рейтинга пользователей ++++++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

?>
<script language = "JavaScript">

        


	// Посмотреть профиль пользователя
	function ViewUserInfo(userid)
	{
		document.RankUsersForm.UserId.value = userid;
		document.RankUsersForm.action.value = "UserInfo";
		document.RankUsersForm.submit();
	}
	
	
	



</script>

<?php


               $TabIndex = 0;
	       
           	print('<form  name = "RankUsersForm"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
                print('<input type = "hidden" name = "action" value = "">'."\r\n");
	        print('<input type = "hidden" name = "UserId" value = "0">'."\n");
		print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\n");
		
		

	        print('</form>'."\r\n");

               print('</br>'."\r\n");
 	       print('<div style = "margin-top: 15px;" align = "left">Рейтинг по версии slazav: по всем ММБ суммируется отношение времени лидера к времени участника</div>'."\r\n");
               print('</br>'."\r\n");


          // Возможно здесь нужно вызвать пересчёт рейтинга по всем ММБ и это не долго
	  // RecalcTeamUsersRank(0);
	  

	  $sql = "  select tu.user_id, u.user_name,  SUM(tu.teamuser_rank) as userrank, 
	                   COUNT(tu.teamuser_id) as userrankcount, 
			   0 as distance_id, '&nbsp;' as distance_name,  'Итог' as raid_name,
			   '&nbsp;' as team_num, '&nbsp;' as team_name
	            from  TeamUsers tu
		          inner join Users u
			  on u.user_id = tu.user_id
		          inner join Teams t
			  on t.team_id = tu.team_id
		          inner join Distances d
			  on t.distance_id = d.distance_id
		    where d.distance_hide = 0 
			  and t.team_hide = 0 
		          and  COALESCE(t.team_outofrange, 0) = 0
		          and  COALESCE(t.team_result, 0) > 0
			  and  COALESCE(tu.teamuser_rank, 0) > 0
		    group by tu.user_id
		    order by userrank DESC 
		  "; 


/*

		    ) a
		    inner join 
		    (
                    select tu.user_id, u.user_name,  tu.teamuser_rank as userrank, null as  userrankcount,
		           d.distance_id, d.distance_name, r.raid_name,
			   t.team_num, t.team_name
	            from  Raids r
		          inner join Distances d
			  on r.raid_id = d.distance_id 
		          left outer join Teams t
			  on t.distance_id = d.distance_id
			     and t.team_hide = 0 
		             and  COALESCE(t.team_outofrange, 0) = 0
		             and  COALESCE(t.team_result, 0) > 0
                          left outer join TeamUsers tu
			  on t.team_id = tu.team_id			   
		          left outer join Users u
			  on u.user_id = tu.user_id
		    where d.distance_hide = 0 
		    ) b
		    on a.user_id = b.user_id

*/	
	  	//echo 'sql '.$sql;
                $Result = MySqlQuery($sql);
	

		$tdstyle = 'padding: 5px 0px 2px 5px;';		
              //  $tdstyle = '';		
                $thstyle = 'padding: 5px 0px 0px 5px;';		
               // $thstyle = '';		

                $ColumnWidth = 350;


		print('<table border = "1" cellpadding = "0" cellspacing = "0" style = "font-size: 80%">'."\r\n");  

		print('<tr class = "gray">
 	                 <td width = "100" style = "'.$thstyle.'">N строки</td>
 	                 <td width = "350" style = "'.$thstyle.'">Пользователь</td>
		         <td width = "100" style = "'.$thstyle.'">Рейтинг</td>
			 </tr>'."\r\n");
	
	        $LineNum = 0;	
	        // Сканируем команды
		while ($Row = mysql_fetch_assoc($Result))
		{
	 	//   print('<tr class = "'.$TrClass.'">'."\r\n");
                     $LineNum++;

                     print('<tr>'."\r\n");
		     print('<td align = "left" style = "'.$tdstyle.'">'.$LineNum.'</td>'."\r\n");
		     print('<td align = "left" style = "'.$tdstyle.'"><a href = "javascript:ViewUserInfo('.$Row['user_id'].');">'.$Row['user_name'].'</a></td>'."\r\n");
		     print('<td align = "left" style = "'.$tdstyle.'">'.$Row['userrank'].'</td>'."\r\n");
                     print('</tr>'."\r\n");
			
		}	
                // Конец циклда по журанлу объединений

		mysql_free_result($Result);
		
		print('</table>'."\r\n");
		
  	  
?>
		

		</br>



