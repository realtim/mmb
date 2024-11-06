<?php
// +++++++++++ Загрузка файла/показ списка файлов ++++++++++++++++++++++++++++

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) {
    return;
}

if (!isset($viewmode)) {
    $viewmode = "";
}

if (!isset($viewsubmode)) {
    $viewsubmode = "";
}

?>

<div>
Внимание! Ссылки ведут на сторонние ресурсы и не проверяются. Если вы сомневаетесь в надежности источника, то не переходите по ссылке.	
</div>

<?php

$sql = "select ul.userlink_id, ul.userlink_name, lt.linktype_name,
		               ul.userlink_url, r.raid_name, r.raid_id,
			       u.user_name,   a.team_name, a.team_num,
			       a.distance_name, a.distance_id,
			       lt.linktype_textonly, lt.linktype_order
		        from  UserLinks ul
			      inner join LinkTypes lt  on ul.linktype_id = lt.linktype_id
			      inner join Raids r on ul.raid_id = r.raid_id 
			      inner join Users u on ul.user_id = u.user_id 
			      left outer join (select tu.user_id, d.raid_id, t.team_name, t.team_num,
			                              d.distance_name, d.distance_id
			                       from TeamUsers tu
			                            inner join Teams t on tu.team_id = t.team_id and t.team_hide = 0
			                            inner join Distances d on t.distance_id = d.distance_id and d.distance_hide = 0
					       where tu.teamuser_hide = 0	    
					      ) a
			       on ul.user_id = a.user_id and ul.raid_id = a.raid_id
			where ul.userlink_hide = 0 
			      and r.raid_id =  $RaidId
			order by r.raid_id desc, lt.linktype_order asc,
				CASE lt.linktype_id 
					WHEN 6 THEN  CAST(userlink_url as decimal(5,2))
					ELSE userlink_id  
				END  asc";

//order by r.raid_id desc, lt.linktype_order asc, userlink_id  asc";

$Result = MySqlQuery($sql);

$PredRaid = '';
$PredLinkType = '';
$LinkTextOnly = 0;

while ($Row = mysqli_fetch_assoc($Result)) {
    // сменился ММБ
    if ($PredRaid <> $Row['raid_name']) {
        //	print('<div align = "left" style = "margin-left: 15px; margin-top: 25px;"><b>'.$Row['raid_name'].'</b></div>'."\r\n");
        $PredRaid = $Row['raid_name'];
    }

    if ($PredLinkType <> $Row['linktype_name']) {
        $PredLinkType = $Row['linktype_name'];
        $LinkTextOnly = $Row['linktype_textonly'];
        print("<div align = \"left\" style = \"margin-left: 15px; margin-top: 25px;\"><b>{$Row['linktype_name']}</b></div>\r\n");
    }

    $Label = (empty($Row['userlink_name'])) ? $Row['userlink_url'] : CMmbUI::toHtml($Row['userlink_name']);
    $TeamDist = (empty($Row['team_name']) ? '' : ', команда ' . CMmbUI::toHtml($Row['team_name']) . ', N ' . $Row['team_num'] . ', дистанция ' . $Row['distance_name']);

    if (empty($LinkTextOnly)) {
        print("<div class=\"impress\"><a href=\"{$Row['userlink_url']}\" title=\"" . CMmbUI::toHtml($Row['userlink_name']) . "\">$Label</a> " . CMmbUI::toHtml($Row['user_name']) . "$TeamDist \r\n");
    } else {
        print("<div class=\"impress\">{$Row['userlink_url']} " . CMmbUI::toHtml($Row['user_name']) . "$TeamDist \r\n");
    }
    print("</div>\r\n");
}

mysqli_free_result($Result);
?>
