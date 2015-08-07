<?php
// +++++++++++ Поиск участников в базе ++++++++++++++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

?>
<script language = "JavaScript">

        
	// Функция просмотра данных о команде
	/*function ViewUserInfo(userid)
	{ 
		document.UsersForm.UserId.value = userid;
		document.UsersForm.action.value = "UserInfo";
		document.UsersForm.submit();
	}*/
	

	
</script>

<?php

		if (trim($FindString) == '' or trim($FindString) == 'Часть ФИО')
                {
                  CMmb::setShortResult('Не указан критерий поиска.', '');
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

	        if ($Administrator)
		{

			$sql = "select u.user_id, CASE WHEN COALESCE(u.user_noshow, 0) = 1 THEN '$Anonimus' ELSE u.user_name END as user_name,
			                COALESCE(u.user_city, '') as user_city,
			                CASE WHEN u.user_email like '%@mmb.ru' THEN 'импорт' ELSE '' END as  import, 
			                CASE WHEN COALESCE(u.user_password, '') = '' THEN 'не активирован' ELSE '' END as noactive,   
			                CASE WHEN COALESCE(u.user_hide, 0) = 1 THEN 'удалён' ELSE '' END as hide, 
			                CASE WHEN COALESCE(u.user_noshow, 0) = 1 THEN 'скрыл данные' ELSE '' END as noshow
			        from  Users u
				where  u.user_name like '%".trim($sqlFindString)."%'
				order by user_name "; 


 		} else {

			$sql = "select u.user_id, CASE WHEN COALESCE(u.user_noshow, 0) = 1 THEN '$Anonimus' ELSE u.user_name END as user_name, COALESCE(u.user_city, '') as user_city
			        from  Users u
				where ltrim(COALESCE(u.user_password, '')) <> '' 
	                              and u.user_hide = 0
	                              and COALESCE(u.user_noshow, 0) = 0
	                              and user_name like '%".trim($sqlFindString)."%'
				order by user_name "; 
		}
		// Конец проверки на администратора                 
		//echo 'sql '.$sql;
		
		$Result = MySqlQuery($sql);

                $RowsCount = mysql_num_rows($Result);
	
		
		if ($RowsCount > 0)
		{
		
			while ($Row = mysql_fetch_assoc($Result))
			{
			  print('<div align="left" style="padding-top: 5px;"><a href="?UserId='.$Row['user_id'].'">'.$Row['user_name'].'</a> '.$Row['user_city']."\r\n");
			  if ($Administrator)
			  {
				  print(' '.$Row['import'].' '.$Row['noactive'].' '.$Row['hide'].' '.$Row['noshow']."\r\n");
			  }

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
		

		</br>



