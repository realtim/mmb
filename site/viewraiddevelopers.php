<?php

/**
 * +++++++++++ Просмотр волонтёров ММБ ++++++++++++++++++++++++++++++++++++++++
 *
 * @var int|null $Administrator
 * @var string $Anonimus
 * @var int $UserId
 * @var int $RaidId
 */

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) {
    return;
}
?>
<script language="JavaScript">
    function HideDeveloper(raiddeveloperid, userid) {
        document.DevelopersForm.RaidDeveloperId.value = raiddeveloperid;
        document.DevelopersForm.UserId.value = userid;
        document.DevelopersForm.action.value = "HideDeveloper";
        document.DevelopersForm.submit();
    }
</script>

<?php
print('<form  name = "DevelopersForm"  action = "' . $MyPHPScript . '" method = "post">' . "\r\n");
print('<input type = "hidden" name = "action" value = "">' . "\r\n");
print('<input type = "hidden" name = "UserId" value = "0">' . "\n");
print('<input type = "hidden" name = "RaidDeveloperId" value = "0">' . "\n");


$sql = "select u.user_id, 
       CASE WHEN COALESCE(u.user_noshow, 0) = 1 and u.user_id <> $UserId THEN '$Anonimus' ELSE u.user_name END as user_name		 , 
       rd.raiddeveloper_id 
		        from  Users u
			      inner join RaidDevelopers rd
			      on u.user_id = rd.user_id  
			where rd.raiddeveloper_hide = 0
			      and rd.raid_id = $RaidId
			order by user_name ";


$Result = MySqlQuery($sql);
$RowsCount = mysqli_num_rows($Result);


if ($RowsCount > 0) {
    while ($Row = mysqli_fetch_assoc($Result)) {
        print('<div class="team_res">' . "\r\n");
        print('<a href="?UserId=' . $Row['user_id'] . '">' . CMmbUI::toHtml($Row['user_name']) . '</a>' . "\r\n");
        if ($Administrator) {
            print('<input type="button" onClick="javascript: if (confirm(\'Вы уверены, что хотите снять статус волонтёра с текущего марш-броска? \')) { HideDeveloper(' . $Row['raiddeveloper_id'] . ',' . $Row['user_id'] . '); }"  name="DeveloperHideButton" value="Скрыть" tabindex="10">' . "\r\n");
        }
        print("</div>\r\n");
    }
} else {
    print('<div class="input" align="left">Не найдено</div>' . "\r\n");
}
print("</form>\r\n");
mysqli_free_result($Result);
?>

<br/>
