<?php

/**
 * Дамп таблицы SportiduinoRecords для тестирования
 */

// Проверка того, что скрипт запущен напрямую, а не включен куда-то через include
if (realpath(__FILE__) != realpath($_SERVER['DOCUMENT_ROOT'] . $_SERVER['SCRIPT_NAME'])) {
    die("Некорректный запуск скрипта");
}

// Подключаемся к базе данных
include("settings.php");
try {
    $pdo = new PDO("mysql:host=$ServerName;dbname=$DBName;charset=utf8", $WebUserName, $WebUserPassword);
} catch (Exception $e) {
    die("Не удалось подключиться к базе ММБ: " . $e->getMessage());
}

// Устанавливаем часовой пояс по умолчанию
date_default_timezone_set("Etc/GMT+3");

// Модифицируем заголовки
header("Content-Type: application/force-download");
header("Content-Type: application/octet-stream");
header("Content-Type: application/download");
header('Content-Type: text/x-csv');
header("Content-Disposition: attachment;filename=SportiduinoRecords.csv");
header("Connection: close");

// Выводим всю таблицу в виде csv
$sql = $pdo->prepare("SELECT * FROM SportiduinoRecords ORDER BY sportiduinorecord_id ASC");
$sql->execute();
$header_line = 1;

while ($row = $sql->fetch(PDO::FETCH_ASSOC)) {
    // Выводим шапку с названиями полей
    if ($header_line) {
        foreach (array_keys($row) as $name) {
            echo "$name;";
        }
        echo "\n";
        $header_line = 0;
    }
    // Выводим очередную строку таблицы
    foreach ($row as $key => $value) {
        echo "$value;";
    }
    echo "\n";
}
?>