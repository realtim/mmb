<?php
/**
 * Created by PhpStorm.
 * User: Serge Titov
 * Date: 10.04.2016
 * Time: 17:46
 */

// Выходим, если файл был запрошен напрямую, а не через include
if (!isset($MyPHPScript)) return;

print("<h3>Просмотр логов системы</h3>\n");


?>
<form name="LogsForm" action="<? echo $MyPHPScript; ?>" method="post">
    <div>
    Типы сообщений: <select name="levels"  multiple style="margin-left: 10px; margin-right: 5px;"
            onchange="document.LogsForm.submit();">
        <option value="trace">trace</option>
        <option value="debug">debug</option>
        <option value="info">info</option>
        <option value="error">error</option>
        <option value="critical">critical</option>
        </select>

    Количество: <select name="num_rec"  style="margin-left: 10px; margin-right: 5px;"
            onchange="document.LogsForm.submit();">
        <option value="100">100</option>
        <option value="500">500</option>
        <option value="5000">5000</option>
    </select>

    <input type="hidden" value="viewLogs" name="action"/>
    </div>


<?php


    $limit = mmb_validateInt($_REQUEST, 'num_rec', 100); 
    $cond = 'true';

    if (isset($_REQUEST['levels']))
        print('<span>'.count($_REQUEST['levels']).'</span>');

    $sql = "select logs_id, logs_level, user_id, logs_operation, logs_message, logs_dt from Logs 
        where $cond 
        order by logs_id desc 
        limit $limit";

    $Result = MySqlQuery($sql);

    print("<table class='std'>\n");
    print("<tr><th width='50'>id</th><th>Время</th><th>Уровень</th><th>Пользователь</th><th>Опреация</th><th>Сообщение</th><th>Длительность</th></tr>\n");
    while ($Row = mysql_fetch_assoc($Result))
        print("<tr><td>{$Row['logs_id']}</td><td>" . date("Y-m-d hh:mm:ss", $Row['logs_dt']). "</td><td>{$Row['logs_level']}</td><td>{$Row['user_id']}</td><td>{$Row['logs_operation']}</td><td>{$Row['logs_message']}</td><td>{$Row['logs_duration']}</td></tr>\n");

    mysql_free_result($Result);
    print("</table>");

?>
    </form>

