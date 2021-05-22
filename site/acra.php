<?
// Устанавливаем часовой пояс по умолчанию
date_default_timezone_set("Europe/Moscow");

// Сохраняем ошибки php в лог
ini_set("log_errors", 1);
ini_set("log_warnings", 1);
ini_set("error_log", "/usr/share/nginx/html/logs/acra.log");

// Проверка того, что скрипт запущен напрямую, а не включен куда-то через include
if (realpath(__FILE__) != realpath($_SERVER['DOCUMENT_ROOT'] . $_SERVER['SCRIPT_NAME']))
    logError("Некорректный запуск скрипта");

// Получаем еще не расшифрованную строку JSON из POST
$fp = fopen("php://input", "r");
$post = html_entity_decode(stream_get_contents($fp));
if ($post == "") logError("Скрипт ничего не получил на вход");
fclose($fp);

// Парсим JSON в массив
$json = json_decode($post, true);
if (is_null($json)) logError("Некорректный JSON на входе");

// Подключаемся к базе данных
include("settings.php");
try {
    $pdo = new PDO("mysql:host=$ServerName;dbname=$DBName;charset=utf8", $WebUserName, $WebUserPassword);
} catch (Exception $e) {
    logError("Не удалось подключиться к базе ММБ: " . $e->getMessage());
}
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

// Подготавливаем данные для записи в базу
$sql = $pdo->prepare("INSERT IGNORE INTO ACRA (REPORT_ID, APP_VERSION_CODE, APP_VERSION_NAME, PACKAGE_NAME, FILE_PATH, PHONE_MODEL, BRAND,"
    ." PRODUCT, ANDROID_VERSION, BUILD, TOTAL_MEM_SIZE, AVAILABLE_MEM_SIZE, CUSTOM_DATA, STACK_TRACE, INITIAL_CONFIGURATION,"
    ." CRASH_CONFIGURATION, DISPLAY, USER_APP_START_DATE, USER_CRASH_DATE, LOGCAT, EVENTSLOG, RADIOLOG, INSTALLATION_ID, DEVICE_FEATURES,"
    ." ENVIRONMENT, SETTINGS_SYSTEM, SETTINGS_SECURE, SETTINGS_GLOBAL, THREAD_DETAILS, BUILD_CONFIG)"
    ." VALUES (:REPORT_ID, :APP_VERSION_CODE, :APP_VERSION_NAME, :PACKAGE_NAME, :FILE_PATH, :PHONE_MODEL, :BRAND, :PRODUCT,"
    ." :ANDROID_VERSION, :BUILD, :TOTAL_MEM_SIZE, :AVAILABLE_MEM_SIZE, :CUSTOM_DATA, :STACK_TRACE, :INITIAL_CONFIGURATION,"
    ." :CRASH_CONFIGURATION, :DISPLAY, :USER_APP_START_DATE, :USER_CRASH_DATE, :LOGCAT, :EVENTSLOG, :RADIOLOG, :INSTALLATION_ID,"
    ." :DEVICE_FEATURES, :ENVIRONMENT, :SETTINGS_SYSTEM, :SETTINGS_SECURE, :SETTINGS_GLOBAL, :THREAD_DETAILS, :BUILD_CONFIG)");
$sql ->bindParam("REPORT_ID", fetchParam($json, "REPORT_ID", "value"), PDO::PARAM_STR);
$sql ->bindParam("APP_VERSION_CODE", fetchParam($json, "APP_VERSION_CODE", "value"), PDO::PARAM_INT);
$sql ->bindParam("APP_VERSION_NAME", fetchParam($json, "APP_VERSION_NAME", "value"), PDO::PARAM_STR);
$sql ->bindParam("PACKAGE_NAME", fetchParam($json, "PACKAGE_NAME", "value"), PDO::PARAM_STR);
$sql ->bindParam("FILE_PATH", fetchParam($json, "FILE_PATH", "value"), PDO::PARAM_STR);
$sql ->bindParam("PHONE_MODEL", fetchParam($json, "PHONE_MODEL", "value"), PDO::PARAM_STR);
$sql ->bindParam("BRAND", fetchParam($json, "BRAND", "value"), PDO::PARAM_STR);
$sql ->bindParam("PRODUCT", fetchParam($json, "PRODUCT", "value"), PDO::PARAM_STR);
$sql ->bindParam("ANDROID_VERSION", fetchParam($json, "ANDROID_VERSION", "value"), PDO::PARAM_STR);
$sql ->bindParam("BUILD", fetchParam($json, "BUILD", "array"), PDO::PARAM_STR);
$sql ->bindParam("TOTAL_MEM_SIZE", fetchParam($json, "TOTAL_MEM_SIZE", "value"), PDO::PARAM_INT);
$sql ->bindParam("AVAILABLE_MEM_SIZE", fetchParam($json, "AVAILABLE_MEM_SIZE", "value"), PDO::PARAM_INT);
$sql ->bindParam("CUSTOM_DATA", fetchParam($json, "CUSTOM_DATA", "array"), PDO::PARAM_STR);
$sql ->bindParam("STACK_TRACE", fetchParam($json, "STACK_TRACE", "text"), PDO::PARAM_STR);
$sql ->bindParam("INITIAL_CONFIGURATION", fetchParam($json, "INITIAL_CONFIGURATION", "array"), PDO::PARAM_STR);
$sql ->bindParam("CRASH_CONFIGURATION", fetchParam($json, "CRASH_CONFIGURATION", "array"), PDO::PARAM_STR);
$sql ->bindParam("DISPLAY", fetchParam($json, "DISPLAY", "array"), PDO::PARAM_STR);
$sql ->bindParam("USER_APP_START_DATE", fetchParam($json, "USER_APP_START_DATE", "value"), PDO::PARAM_STR);
$sql ->bindParam("USER_CRASH_DATE", fetchParam($json, "USER_CRASH_DATE", "value"), PDO::PARAM_STR);
$sql ->bindParam("LOGCAT", fetchParam($json, "LOGCAT", "text"), PDO::PARAM_STR);
$sql ->bindParam("EVENTSLOG", fetchParam($json, "EVENTSLOG", "text"), PDO::PARAM_STR);
$sql ->bindParam("RADIOLOG", fetchParam($json, "RADIOLOG", "text"), PDO::PARAM_STR);
$sql ->bindParam("INSTALLATION_ID", fetchParam($json, "INSTALLATION_ID", "value"), PDO::PARAM_STR);
$sql ->bindParam("DEVICE_FEATURES", fetchParam($json, "DEVICE_FEATURES", "array"), PDO::PARAM_STR);
$sql ->bindParam("ENVIRONMENT", fetchParam($json, "ENVIRONMENT", "array"), PDO::PARAM_STR);
$sql ->bindParam("SETTINGS_SYSTEM", fetchParam($json, "SETTINGS_SYSTEM", "array"), PDO::PARAM_STR);
$sql ->bindParam("SETTINGS_SECURE", fetchParam($json, "SETTINGS_SECURE", "array"), PDO::PARAM_STR);
$sql ->bindParam("SETTINGS_GLOBAL", fetchParam($json, "SETTINGS_GLOBAL", "array"), PDO::PARAM_STR);
$sql ->bindParam("THREAD_DETAILS", fetchParam($json, "THREAD_DETAILS", "array"), PDO::PARAM_STR);
$sql ->bindParam("BUILD_CONFIG", fetchParam($json, "BUILD_CONFIG", "array"), PDO::PARAM_STR);

// Пишем в базу
try {
    $sql->execute();
} catch (Exception $e) {
    logError("Ошибка записи в базу: " . $e->getMessage());
}
if ($sql->rowCount() != 1) logError("Данные не записались в базу: ".$post);
$sql = null;


// Запись в лог об ошибке
function logError($message)
{
  file_put_contents("/usr/share/nginx/html/logs/acra.log", date("Y-m-d H:i:s ") . $message . "\n", FILE_APPEND);
  die($message);
}

// Форматирование JSON-объекта в строку для записи в базу
function fetchParam($json, $param, $type)
{
  if (!isset($json[$param])) return null;
  $value = json_encode($json[$param], JSON_NUMERIC_CHECK);
  $value = str_replace("\\/", "/", $value);
  if ($type == "value") return trim($value, '"');
  elseif ($type == "array") return prettyPrint($value);
  elseif ($type == "text")
  {
    $value = str_replace("\\n", "\n", $value);
    $value = str_replace("\\t", "\t", $value);
    return trim($value, '"');
  }
  logError("Неизвестный тип '$type'");
}

// Эмуляция JSON_PRETTY_PRINT в json_encode, так как на сервере старая версия php, которая не поддерживает этот флаг
function prettyPrint( $json )
{
    $result = '';
    $level = 0;
    $in_quotes = false;
    $in_escape = false;
    $ends_line_level = NULL;
    $json_length = strlen( $json );

    for( $i = 0; $i < $json_length; $i++ ) {
        $char = $json[$i];
        $new_line_level = NULL;
        $post = "";
        if( $ends_line_level !== NULL ) {
            $new_line_level = $ends_line_level;
            $ends_line_level = NULL;
        }
        if ( $in_escape ) {
            $in_escape = false;
        } else if( $char === '"' ) {
            $in_quotes = !$in_quotes;
        } else if( ! $in_quotes ) {
            switch( $char ) {
                case '}': case ']':
                    $level--;
                    $ends_line_level = NULL;
                    $new_line_level = $level;
                    break;

                case '{': case '[':
                    $level++;
                case ',':
                    $ends_line_level = $level;
                    break;

                case ':':
                    $post = " ";
                    break;

                case " ": case "\t": case "\n": case "\r":
                    $char = "";
                    $ends_line_level = $new_line_level;
                    $new_line_level = NULL;
                    break;
            }
        } else if ( $char === '\\' ) {
            $in_escape = true;
        }
        if( $new_line_level !== NULL ) {
            $result .= "\n".str_repeat( "\t", $new_line_level );
        }
        $result .= $char.$post;
    }

    return $result;
}
?>