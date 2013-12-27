<?php
// +++++++++++ Поиск участников в базе ++++++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

?>
<script language = "JavaScript">

        
	// Функция просмотра данных о команде
	function ViewUserInfo(userid)
	{ 
		document.UsersForm.UserId.value = userid;
		document.UsersForm.action.value = "UserInfo";
		document.UsersForm.submit();
	}
	

	
</script>

<?php

		if (trim($FindString) == '' or trim($FindString) == 'Часть ФИО')
                {
                  $statustext = 'Не указан критерий поиска.';				     
                  $view = "";
 		  return;
                }


                if (trim($FindString) == 'все-все' or trim($FindString) == 'все-все-все')
                {
		  $sqlFindString = '';
                  $FindText = 'Пользователи:';
                } else {
		  $sqlFindString = trim($FindString);
                  $FindText = 'Пользователи, чьи ФИО содержат '.trim($FindString).':';
                }
   
	//$FindString = trim($_POST['FindString']); 

                // Выводим спсиок пользователей, которые подошли
                print('<div style = "margin-top: 10px; margin-bottom: 10px; text-align: left">'.$FindText.'</div>'."\r\n");
           	print('<form  name = "UsersForm"  action = "'.$MyPHPScript.'" method = "post">'."\r\n");
                print('<input type = "hidden" name = "action" value = "">'."\r\n");
	        print('<input type = "hidden" name = "UserId" value = "0">'."\n");
	        print('<input type = "hidden" name = "RaidId" value = "'.$RaidId.'">'."\n");
		print('<input type = "hidden" name = "sessionid" value = "'.$SessionId.'">'."\n");
		
		
                 
		$sql = "select u.user_id, u.user_name 
		        from  Users u
			where ltrim(COALESCE(u.user_password, '')) <> '' 
                              and u.user_hide = 0
                              and user_name like '%".trim($sqlFindString)."%'
			order by user_name "; 
                
		//echo 'sql '.$sql;
		
		$Result = MySqlQuery($sql);

                $RowsCount = mysql_num_rows($Result);
	
		
		if ($RowsCount > 0)
		{
		
			while ($Row = mysql_fetch_assoc($Result))
			{
			  print('<div align = "left" style = "padding-top: 5px;"><a href = "javascript:ViewUserInfo('.$Row['user_id'].');">'.$Row['user_name'].'</a></div>'."\r\n");
			}

		} else {

			  print('<div class= "input" align = "left">Не найдено</div>'."\r\n");
		}
	        print('</form>'."\r\n");
                mysql_free_result($Result);



?>
<!--		
		</td></tr>
		</table>
-->
		

		</br>



