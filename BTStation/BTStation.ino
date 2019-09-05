//#define DEBUG

#include <Wire.h>
#include "ds3231.h"
#include <SPI.h>
#include <MFRC522.h>
#include <EEPROM.h>
#include <SPIFlash.h>

#ifdef DEBUG
//#include <SoftwareSerial.h>
#endif

#define UART_SPEED 38400

// версия прошивки, номер пишется в чипы
#define FW_VERSION					106

#ifdef DEBUG
//#define DEBUG_RX					2 //
//#define DEBUG_TX					6 //
#endif

#define BUZZER_PIN					3 // пищалка
#define LED_PIN						4 // светодиод синий
#define RTC_ENABLE_PIN				5 // питание часов кроме батарейного

#define FLASH_ENABLE_PIN			7 // SPI enable pin
#define FLASH_SS_PIN				8 // SPI SELECT pin
#define RFID_RST_PIN				9 // рфид модуль reset
#define RFID_SS_PIN					10 // рфид модуль chip_select
//#define RFID_MOSI_PIN				11 // рфид модуль
//#define RFID_MISO_PIN				12 // рфид модуль
//#define RFID_SCK_PIN				13 // рфид модуль
#define BATTERY_PIN					A0 // замер напряжения батареи
#define ERROR_LED_PIN				A1 // светодиод ошибки (красный)

// номер станции в eeprom памяти
#define EEPROM_STATION_NUMBER		00
// номер режима в eeprom памяти
#define EEPROM_STATION_MODE			10
// коэфф. пересчета значения ADC в вольты = 0,00587
#define EEPROM_VOLTAGE_KOEFF		20
// усиление сигнала RFID
#define EEPROM_GAIN					40
// тип чипа, с которым должна работать станция
#define EEPROM_CHIP_TYPE			50
// размер блока на флэше под данные команды
#define EEPROM_TEAM_BLOCK_SIZE				60
// размер стираемого блока на флэше
#define EEPROM_FLASH_BLOCK_SIZE				70

// команды
#define COMMAND_SET_MODE			0x80
#define COMMAND_SET_TIME			0x81
#define COMMAND_RESET_STATION		0x82
#define COMMAND_GET_STATUS			0x83
#define COMMAND_INIT_CHIP			0x84
#define COMMAND_GET_LAST_TEAMS		0x85
#define COMMAND_GET_TEAM_RECORD		0x86
#define COMMAND_READ_CARD_PAGE		0x87
#define COMMAND_UPDATE_TEAM_MASK	0x88
#define COMMAND_WRITE_CARD_PAGE		0x89
#define COMMAND_READ_FLASH			0x8a
#define COMMAND_WRITE_FLASH			0x8b
#define COMMAND_ERASE_FLASH_SECTOR	0x8c
#define COMMAND_GET_CONFIG			0x8d
#define COMMAND_SET_KOEFF			0x8e
#define COMMAND_SET_GAIN			0x8f
#define COMMAND_SET_CHIP_TYPE		0x90
#define COMMAND_SET_TEAM_FLASH_SIZE	0x91
#define COMMAND_SET_FLASH_BLOCK_SIZE	0x92

// размеры данных для команд
#define DATA_LENGTH_SET_MODE			1
#define DATA_LENGTH_SET_TIME			6
#define DATA_LENGTH_RESET_STATION		7
#define DATA_LENGTH_GET_STATUS			0
#define DATA_LENGTH_INIT_CHIP			4
#define DATA_LENGTH_GET_LAST_TEAMS		0
#define DATA_LENGTH_GET_TEAM_RECORD		2
#define DATA_LENGTH_READ_CARD_PAGE		2
#define DATA_LENGTH_UPDATE_TEAM_MASK	8
#define DATA_LENGTH_WRITE_CARD_PAGE		13
#define DATA_LENGTH_READ_FLASH			5
#define DATA_LENGTH_WRITE_FLASH			4  // and more according to data length
#define DATA_LENGTH_ERASE_FLASH_SECTOR	2
#define DATA_LENGTH_GET_CONFIG			0
#define DATA_LENGTH_SET_KOEFF			4
#define DATA_LENGTH_SET_GAIN			1
#define DATA_LENGTH_SET_CHIP_TYPE		1
#define DATA_LENGTH_SET_TEAM_FLASH_SIZE	2
#define DATA_LENGTH_SET_FLASH_BLOCK_SIZE	2

// ответы станции
#define REPLY_SET_MODE				0x90
#define REPLY_SET_TIME				0x91
#define REPLY_RESET_STATION			0x92
#define REPLY_GET_STATUS			0x93
#define REPLY_INIT_CHIP				0x94
#define REPLY_GET_LAST_TEAMS		0x95
#define REPLY_GET_TEAM_RECORD		0x96
#define REPLY_READ_CARD_PAGE		0x97
#define REPLY_UPDATE_TEAM_MASK		0x98
#define REPLY_WRITE_CARD_PAGE		0x99
#define REPLY_READ_FLASH			0x9a
#define REPLY_WRITE_FLASH			0x9b
#define REPLY_ERASE_FLASH_SECTOR	0x9c
#define REPLY_GET_CONFIG			0x9d
#define REPLY_SET_KOEFF				0x9e
#define REPLY_SET_GAIN				0x9f
#define REPLY_SET_CHIP_TYPE			0xa0
#define REPLY_SET_TEAM_FLASH_SIZE	0xa1
#define REPLY_SET_FLASH_BLOCK_SIZE	0xa2

// режимы станции
#define MODE_INIT					0
#define MODE_START_KP				1
#define MODE_FINISH_KP				2

// коды ошибок станции
#define OK							0
#define WRONG_STATION				1
#define READ_ERROR					2
#define WRITE_ERROR					3
#define LOW_INIT_TIME				4
#define WRONG_CHIP					5
#define NO_CHIP						6
#define BUFFER_OVERFLOW				7
#define WRONG_DATA					8
#define WRONG_UID					9
#define WRONG_TEAM					10
#define NO_DATA						11
#define WRONG_COMMAND				12
#define ERASE_ERROR					13
#define WRONG_CHIP_TYPE				14
#define WRONG_MODE					15
#define WRONG_SIZE					16

// страницы в чипе. 0-7 служебные, 8-... для отметок
#define PAGE_UID		0
// номер_чипа + тип_чипа + версия_прошивки
#define PAGE_CHIP_NUM	4
// время инициализации
#define PAGE_INIT_TIME	5
#define PAGE_TEAM_MASK	6
#define PAGE_RESERVED2	7
#define PAGE_DATA_START	8

// тип чипа
uint8_t chipType = 0x3e;
// отметка для чипа
uint8_t NTAG_MARK = 215;
// размер чипа в страницах
uint8_t TAG_MAX_PAGE = 130;

// размер записи лога (на 1 чип)
uint16_t TEAM_FLASH_SIZE = 1024;
uint16_t FLASH_BLOCK_SIZE = 4096;

// максимальное кол-во записей в логе
uint32_t maxTeamNumber = 1; // = 4 * 1024 - FLASH_BLOCK_SIZE / TEAM_FLASH_SIZE;

// описание протокола
#define PACKET_ID	2
#define STATION_NUMBER_BYTE	3
#define LENGTH_BYTE			4
#define COMMAND_BYTE		5
#define DATA_START_BYTE		6

// тайм-аут приема команды с момента начала
#define receiveTimeOut 1000

// размер буфера последних команд
const uint8_t lastTeamsLength = 10;

// станция запоминает последние команды сюда
uint8_t lastTeams[lastTeamsLength * 2];
uint32_t lastTimeChecked = 0;

// количество отмеченных чипов в памяти.
uint16_t totalChipsChecked = 0;

// по умолчанию номер станции и режим.
uint8_t stationNumber = 0;
uint8_t stationMode = MODE_INIT;
const uint32_t maxTimeInit = 600000UL; // одна неделя

// коэфф. перевода значения АЦП в напряжение для делителя 10кОм/2.2кОм
float voltageCoeff = 0.00578;

uint8_t ntag_page[16]; // буфер для чтения из чипа через ntagRead4pages()

SPIFlash SPIflash(FLASH_SS_PIN); // флэш-память

// рфид-модуль
MFRC522 mfrc522(RFID_SS_PIN, RFID_RST_PIN);
// коэфф. усиления антенны - работают только биты 4,5,6
byte gainCoeff = 0x70;

// хранение времени
struct ts systemTime;

// UART command buffer
uint8_t uartBuffer[256];
uint8_t uartBufferPosition = 0;
bool uartReady = false;
uint32_t uartTimeout = 1000;
bool receivingData = false;
uint32_t receiveStartTime = 0;

// новая маска для замены в чипе
uint8_t newTeamMask[8];

// флаг последней команды для отсечки двойной отметки
uint16_t lastTeamFlag = 0;

#ifdef DEBUG
//SoftwareSerial DebugSerial(DEBUG_RX, DEBUG_TX);
#endif

void(*resetFunc) (void) = 0; // declare reset function @ address 0

void setup()
{
	Serial.begin(UART_SPEED);
#ifdef DEBUG
	//DebugSerial.begin(9600);
#endif

	analogReference(INTERNAL);

	pinMode(LED_PIN, OUTPUT);
	pinMode(ERROR_LED_PIN, OUTPUT);
	pinMode(BUZZER_PIN, OUTPUT);

	//даем питание на флэш
	pinMode(FLASH_ENABLE_PIN, OUTPUT);
	digitalWrite(FLASH_ENABLE_PIN, HIGH);
	delay(1);
	SPIflash.begin();

	//даем питание на часы
	pinMode(RTC_ENABLE_PIN, OUTPUT);
	digitalWrite(RTC_ENABLE_PIN, HIGH);
	delay(1);
	DS3231_init(DS3231_INTCN);
	DS3231_get(&systemTime);

	//читаем номер станции из памяти
	stationNumber = eepromread(EEPROM_STATION_NUMBER);
	if (stationNumber == 255 || stationNumber == -1)
	{
		stationNumber = 0;
	}

	//читаем номер режима из памяти
	int c = eepromread(EEPROM_STATION_MODE);
	if (c == -1)
	{
		beep(200, 20);
	}
	if (c == MODE_START_KP) stationMode = MODE_START_KP;
	else if (c == MODE_FINISH_KP) stationMode = MODE_FINISH_KP;
	else stationMode = MODE_INIT;

	//читаем коэфф. пересчета напряжения
	union Convert
	{
		float number;
		byte byte[4];
	} p;
	byte flag = 0;
	for (byte i = 0; i < 4; i++)
	{
		p.byte[i] = eepromread(EEPROM_VOLTAGE_KOEFF + i * 3);
		if (p.byte[i] == 0xff) flag++;
	}
	voltageCoeff = p.number;

	//читаем коэфф. усиления
	gainCoeff = eepromread(EEPROM_GAIN);
	if (gainCoeff == 255 || gainCoeff == -1)
	{
		gainCoeff = 0x70;
	}

	//читаем тип чипа
	chipType = eepromread(EEPROM_CHIP_TYPE);
	selectChipType(chipType);

	uint32_t flashSize = SPIflash.getCapacity();

	//читаем размер блока команды
	byte n[2];
	for (byte i = 0; i < 2; i++)
	{
		n[i] = eepromread(EEPROM_TEAM_BLOCK_SIZE + i * 3);
	}
	TEAM_FLASH_SIZE = n[0] * 256 + n[1];

	//читаем размер стираемого блока
	for (byte i = 0; i < 2; i++)
	{
		n[i] = eepromread(EEPROM_FLASH_BLOCK_SIZE + i * 3);
	}
	FLASH_BLOCK_SIZE = n[0] * 256 + n[1];

	maxTeamNumber = (flashSize - FLASH_BLOCK_SIZE) / TEAM_FLASH_SIZE - 1;

	totalChipsChecked = refreshChipCounter();

	beep(800, 1);
}

void loop()
{
	/*
	 * В цикле сначала считывааем данные из порта, если получили полный пакет,
	 * то ищем соответсвующую функцию и обабатываем её
	 * Если режим станци не нулевой, то станция работает также как кп - автоматически делает отметки на чипы
	 */

	 // check receive timeout
	if (receivingData && millis() - receiveStartTime > receiveTimeOut)
	{
#ifdef DEBUG
		//Serial.println(F("!!!receive timeout"));
#endif
		uartBufferPosition = 0;
		uartReady = false;
		receivingData = false;
		//errorBeepMs(50, 1);
	}

	// check UART for data
	if (Serial.available())
	{
		uartReady = readUart();
	}

	//обработать пришедшую команду
	if (uartReady)
	{
		uartReady = false;
		executeCommand();
	}

	//если режим КП то отметить чип автоматом
	if (stationMode != 0)
	{
		processRfidCard();
	}
}

// Commands processing

// поиск функции
void executeCommand()
{
#ifdef DEBUG
	Serial.print(F("!!!Command: "));
	Serial.println(String(uartBuffer[COMMAND_BYTE], HEX));
#endif
	bool errorLengthFlag = false;
	switch (uartBuffer[COMMAND_BYTE])
	{
	case COMMAND_SET_MODE:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_SET_MODE) setMode();
		else errorLengthFlag = true;
		break;
	case COMMAND_SET_TIME:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_SET_TIME) setTime();
		else errorLengthFlag = true;
		break;
	case COMMAND_RESET_STATION:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_RESET_STATION) resetStation();
		else errorLengthFlag = true;
		break;
	case COMMAND_GET_STATUS:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_GET_STATUS) getStatus();
		else errorLengthFlag = true;
		break;
	case COMMAND_INIT_CHIP:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_INIT_CHIP) initChip();
		else errorLengthFlag = true;
		break;
	case COMMAND_GET_LAST_TEAMS:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_GET_LAST_TEAMS) getLastTeams();
		else errorLengthFlag = true;
		break;
	case COMMAND_GET_TEAM_RECORD:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_GET_TEAM_RECORD) getTeamRecord();
		else errorLengthFlag = true;
		break;
	case COMMAND_READ_CARD_PAGE:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_READ_CARD_PAGE) readCardPages();
		else errorLengthFlag = true;
		break;
	case COMMAND_UPDATE_TEAM_MASK:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_UPDATE_TEAM_MASK) updateTeamMask();
		else errorLengthFlag = true;
		break;
	case COMMAND_WRITE_CARD_PAGE:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_WRITE_CARD_PAGE) writeCardPage();
		else errorLengthFlag = true;
		break;
	case COMMAND_READ_FLASH:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_READ_FLASH) readFlash();
		else errorLengthFlag = true;
		break;
	case COMMAND_WRITE_FLASH:
		if (uartBuffer[LENGTH_BYTE] >= DATA_LENGTH_WRITE_FLASH) writeFlash();
		else errorLengthFlag = true;
		break;
	case COMMAND_ERASE_FLASH_SECTOR:
		if (uartBuffer[LENGTH_BYTE] >= DATA_LENGTH_ERASE_FLASH_SECTOR) eraseTeamFlash();
		else errorLengthFlag = true;
		break;
	case COMMAND_GET_CONFIG:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_GET_CONFIG) getConfig();
		else errorLengthFlag = true;
		break;
	case COMMAND_SET_KOEFF:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_SET_KOEFF) setVCoeff();
		else errorLengthFlag = true;
		break;
	case COMMAND_SET_GAIN:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_SET_GAIN) setGain();
		else errorLengthFlag = true;
		break;
	case COMMAND_SET_CHIP_TYPE:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_SET_CHIP_TYPE) setChipType();
		else errorLengthFlag = true;
		break;
	case COMMAND_SET_TEAM_FLASH_SIZE:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_SET_TEAM_FLASH_SIZE) setTeamFlashSize();
		else errorLengthFlag = true;
		break;
	case COMMAND_SET_FLASH_BLOCK_SIZE:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_SET_FLASH_BLOCK_SIZE) setFlashBlockSize();
		else errorLengthFlag = true;
		break;
	}

	uartBufferPosition = 0;
	if (errorLengthFlag) sendError(WRONG_COMMAND, 0);

#ifdef DEBUG
	if (errorLengthFlag) Serial.println(F("!!!Incorrect data length"));
#endif
}

// установка режима
void setMode()
{
	if (stationNumber == 0 || stationNumber == 0xff)
	{
		sendError(WRONG_MODE, REPLY_SET_MODE);
		return;
	}
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_SET_MODE);
		return;
	}

	// 0: новый номер режима
	stationMode = uartBuffer[DATA_START_BYTE];
	if (!eepromwrite(EEPROM_STATION_MODE, stationMode)) errorBeep(1);

	// формирование пакета данных.
	init_package(REPLY_SET_MODE);

	// 0: код ошибки
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_SET_MODE);
		return;
	}
	sendData();
}

// обновление времени на станции
void setTime()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_SET_MODE);
		return;
	}

	systemTime.year = uartBuffer[DATA_START_BYTE] + 2000;
	systemTime.mon = uartBuffer[DATA_START_BYTE + 1];
	systemTime.mday = uartBuffer[DATA_START_BYTE + 2];
	systemTime.hour = uartBuffer[DATA_START_BYTE + 3];
	systemTime.min = uartBuffer[DATA_START_BYTE + 4];
	systemTime.sec = uartBuffer[DATA_START_BYTE + 5];

#ifdef DEBUG
	// Serial.print(F("!!!Time: "));
	// for (uint8_t i = 0; i < 6; i++) Serial.println(String(uartBuffer[DATA_START_BYTE + i]) + " ");
#endif

	// 0-3: дата и время в unixtime
	// systemTime.unixtime = uartBuffer[DATA_START_BYTE] * 16777216 + uartBuffer[DATA_START_BYTE] * 65536 + uartBuffer[DATA_START_BYTE] * 256 + uartBuffer[DATA_START_BYTE];

	delay(1);
	DS3231_set(systemTime); // correct time

	DS3231_get(&systemTime);
	uint32_t tmpTime = systemTime.unixtime;

	init_package(REPLY_SET_TIME);

	// 0: код ошибки
	// 1-4: текущее время
	bool flag = true;
	flag &= addData(OK);
	flag &= addData((tmpTime & 0xFF000000) >> 24);
	flag &= addData((tmpTime & 0x00FF0000) >> 16);
	flag &= addData((tmpTime & 0x0000FF00) >> 8);
	flag &= addData(tmpTime & 0x000000FF);
	if (!flag)
	{
		sendError(BUFFER_OVERFLOW, REPLY_SET_TIME);
		return;
	}
	sendData();
}

// сброс настроек станции
void resetStation()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_RESET_STATION);
		return;
	}

	// 0-1: кол-во отмеченных чипов (для сверки)
	// 2-5: время последней отметки(для сверки)
	// 6 : новый номер станции
	// проверить количество отметок
	uint16_t checkCardNumber = uartBuffer[DATA_START_BYTE];
	checkCardNumber <<= 8;
	checkCardNumber += uartBuffer[DATA_START_BYTE + 1];

	if (checkCardNumber != totalChipsChecked)
	{
		sendError(WRONG_DATA, REPLY_RESET_STATION);
		return;
	}

	// проверить время последней отметки
	uint32_t checkLastTime = uartBuffer[DATA_START_BYTE + 2];
	checkLastTime <<= 8;
	checkLastTime += uartBuffer[DATA_START_BYTE + 3];
	checkLastTime <<= 8;
	checkLastTime += uartBuffer[DATA_START_BYTE + 4];
	checkLastTime <<= 8;
	checkLastTime += uartBuffer[DATA_START_BYTE + 5];
	if (checkLastTime != lastTimeChecked)
	{
		sendError(WRONG_DATA, REPLY_RESET_STATION);
		return;
	}

	stationMode = 0;
	if (!eepromwrite(EEPROM_STATION_MODE, stationMode)) errorBeep(1);
	stationNumber = uartBuffer[DATA_START_BYTE + 6];
	if (!eepromwrite(EEPROM_STATION_NUMBER, stationNumber)) errorBeep(1);

	SPIflash.eraseChip();

	lastTimeChecked = 0;
	totalChipsChecked = 0;

	init_package(REPLY_RESET_STATION);

	// 0: код ошибки
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_RESET_STATION);
		return;
	}

	sendData();
	delay(100);
	resetFunc();
}

// выдает статус: время на станции, номер станции, номер режима, число отметок, время последней страницы
void getStatus()
{
	DS3231_get(&systemTime);
	uint32_t tmpTime = systemTime.unixtime;

	// 0: код ошибки
	// 1 - 4: текущее время
	// 5 - 6 : количество отметок на станции
	// 7 - 10 : время последней отметки на станции
	// 11 - 12 : напряжение батареи в условных единицах[0..1023] ~[0..1.1В]
	// 13 - 14 : температура чипа DS3231(чуть выше окружающей среды)
	init_package(REPLY_GET_STATUS);

	bool flag = true;
	flag &= addData(OK);

	flag &= addData((tmpTime & 0xFF000000) >> 24);
	flag &= addData((tmpTime & 0x00FF0000) >> 16);
	flag &= addData((tmpTime & 0x0000FF00) >> 8);
	flag &= addData(tmpTime & 0x000000FF);

	flag &= addData((totalChipsChecked & 0xFF00) >> 8);
	flag &= addData(totalChipsChecked & 0x00FF);

	flag &= addData((lastTimeChecked & 0xFF000000) >> 24);
	flag &= addData((lastTimeChecked & 0x00FF0000) >> 16);
	flag &= addData((lastTimeChecked & 0x0000FF00) >> 8);
	flag &= addData(lastTimeChecked & 0x000000FF);

	uint16_t batteryLevel = getBatteryLevel();
	flag &= addData((batteryLevel & 0xFF00) >> 8);
	flag &= addData(batteryLevel & 0x00FF);

	int temperature = (int)DS3231_get_treg();
	flag &= addData((temperature & 0xFF00) >> 8);
	flag &= addData(temperature & 0x00FF);

	if (!flag)
	{
		sendError(BUFFER_OVERFLOW, REPLY_GET_STATUS);
		return;
	}

	sendData();
}

// инициализация чипа
void initChip()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_INIT_CHIP);
		return;
	}

	digitalWrite(LED_PIN, HIGH);
	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522
	mfrc522.PCD_SetAntennaGain(gainCoeff);

	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(NO_CHIP, REPLY_INIT_CHIP);
		return;
	}
	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(NO_CHIP, REPLY_INIT_CHIP);
		return;
	}

	// инициализация сработает только если время инициализации записанное уже на чипе превышает неделю до текущего времени
	if (!ntagRead4pages(PAGE_INIT_TIME))
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(READ_ERROR, REPLY_INIT_CHIP);
		return;
	}
	uint32_t initTime = ntag_page[0];
	initTime <<= 8;
	initTime += ntag_page[1];
	initTime <<= 8;
	initTime += ntag_page[2];
	initTime <<= 8;
	initTime += ntag_page[3];
	DS3231_get(&systemTime);
	if ((systemTime.unixtime - initTime) < maxTimeInit)
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(LOW_INIT_TIME, REPLY_INIT_CHIP);
		return;
	}

	// читаем блок информации
	if (!ntagRead4pages(PAGE_UID))
	{
		SPI.end();
		return;
	}

	// Фильтруем неправильный тип чипа
	if (ntag_page[14] != chipType)
	{
		SPI.end();
		errorBeep(2);
		return;
	}

	// читаем блок из чипа
	if (!ntagRead4pages(PAGE_CHIP_NUM))
	{
		SPI.end();
		return;
	}

	// Фильтруем неправильный тип чипа
	if (ntag_page[2] != NTAG_MARK)
	{
		SPI.end();
		errorBeep(2);
		return;
	}

	// 0-1: номер команды
	// 2-3 : маска участников

	uint8_t dataBlock[4] = { 255,255,255,255 };

	// заполняем чип 0xFF
	/*for (uint8_t page = PAGE_CHIP_NUM; page < NTAG215_MAX_PAGE; page++)
	{
		if (!ntagWritePage(dataBlock, page))
		{
			SPI.end();
	digitalWrite(LED_PIN, LOW);
			sendError(WRITE_ERROR, REPLY_INIT_CHIP);
			return;
		}
	}*/

	// заполняем чип 0x00
	for (uint8_t i = 0; i < 4; i++) dataBlock[i] = 0;
	for (uint8_t page = PAGE_CHIP_NUM; page < TAG_MAX_PAGE; page++)
	{
		if (!ntagWritePage(dataBlock, page))
		{
			SPI.end();
			digitalWrite(LED_PIN, LOW);
			sendError(WRITE_ERROR, REPLY_INIT_CHIP);
			return;
		}
	}

	// пишем данные на чип
	// номер команды, тип чипа, версия прошивки станции	
	dataBlock[0] = uartBuffer[DATA_START_BYTE];
	dataBlock[1] = uartBuffer[DATA_START_BYTE + 1];
	dataBlock[2] = NTAG_MARK;
	dataBlock[3] = FW_VERSION;
	if (!ntagWritePage(dataBlock, PAGE_CHIP_NUM))
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(WRITE_ERROR, REPLY_INIT_CHIP);
		return;
	}

	// пишем на чип текущее время
	uint32_t tmpTime = systemTime.unixtime;
	dataBlock[0] = (tmpTime & 0xFF000000) >> 24;
	dataBlock[1] = (tmpTime & 0x00FF0000) >> 16;
	dataBlock[2] = (tmpTime & 0x0000FF00) >> 8;
	dataBlock[3] = tmpTime & 0x000000FF;
	if (!ntagWritePage(dataBlock, PAGE_INIT_TIME))
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(WRITE_ERROR, REPLY_INIT_CHIP);
		return;
	}

	// маска участников
	dataBlock[0] = uartBuffer[DATA_START_BYTE + 2];
	dataBlock[1] = uartBuffer[DATA_START_BYTE + 3];
	dataBlock[2] = 0;
	dataBlock[3] = 0;
	if (!ntagWritePage(dataBlock, PAGE_TEAM_MASK))
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(WRITE_ERROR, REPLY_INIT_CHIP);
		return;
	}

	// получаем UID чипа
	if (!ntagRead4pages(PAGE_UID))
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(READ_ERROR, REPLY_INIT_CHIP);
		return;
	}
	SPI.end();
	digitalWrite(LED_PIN, LOW);

	init_package(REPLY_INIT_CHIP);
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_INIT_CHIP);
		return;
	}

	// добавляем в ответ время инициализации
	bool flag = true;
	flag &= addData((tmpTime & 0xFF000000) >> 24);
	flag &= addData((tmpTime & 0x00FF0000) >> 16);
	flag &= addData((tmpTime & 0x0000FF00) >> 8);
	flag &= addData(tmpTime & 0x000000FF);
	if (!flag)
	{
		sendError(BUFFER_OVERFLOW, REPLY_INIT_CHIP);
		return;
	}

	// добавляем в ответ UID
	for (uint8_t i = 0; i <= 7; i++)
	{
		if (!addData(ntag_page[i]))
		{
			sendError(BUFFER_OVERFLOW, REPLY_INIT_CHIP);
			return;
		}
	}
	sendData();
}

// получить последнюю отметившуюся команду
void getLastTeams()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_GET_LAST_TEAMS);
		return;
	}

	init_package(REPLY_GET_LAST_TEAMS);

	bool flag = true;
	// 0: код ошибки
	flag &= addData(OK);

	// номера последних команд
	for (uint8_t i = 0; i < lastTeamsLength * 2; i++)
	{
		flag &= addData(lastTeams[i]);
	}
	if (!flag)
	{
		sendError(BUFFER_OVERFLOW, REPLY_GET_LAST_TEAMS);
		return;
	}

	sendData();
}

// получаем запись о команде из флэша
void getTeamRecord()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_GET_TEAM_RECORD);
		return;
	}

	// 0-1: какую запись
	uint16_t recordNumber = uartBuffer[DATA_START_BYTE];
	recordNumber <<= 8;
	recordNumber += uartBuffer[DATA_START_BYTE + 1];

	if (recordNumber < 1 || recordNumber > maxTeamNumber)
	{
		sendError(WRONG_TEAM, REPLY_GET_TEAM_RECORD);
		return;
	}

	init_package(REPLY_GET_TEAM_RECORD);
	// 0: код ошибки                            
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_GET_TEAM_RECORD);
		return;
	}

	// если ячейка лога пуста
	if (!readTeamFromFlash(recordNumber))
	{
		sendError(NO_DATA, REPLY_GET_TEAM_RECORD);
		return;
	}

	// 1-2: номер команды
	// 3-6 : время инициализации
	// 7-8: маска команды	
	// 9-12 : время последней отметки на станции
	// 13-14: счетчик сохраненных байт for 2 byte version
	for (uint8_t i = 0; i < 14; i++)
	{
		if (!addData(ntag_page[i]))
		{
			sendError(BUFFER_OVERFLOW, REPLY_GET_LAST_TEAMS);
			return;
		}
	}

	sendData();
}

// читаем страницы с чипа
void readCardPages()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_READ_CARD_PAGE);
		return;
	}

	digitalWrite(LED_PIN, HIGH);
	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522
	mfrc522.PCD_SetAntennaGain(gainCoeff);

	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(NO_CHIP, REPLY_READ_CARD_PAGE);
		return;
	}
	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(NO_CHIP, REPLY_READ_CARD_PAGE);
		return;
	}

	uint8_t pageFrom = uartBuffer[DATA_START_BYTE];
	uint8_t pageTo = uartBuffer[DATA_START_BYTE + 1];

	init_package(REPLY_READ_CARD_PAGE);

	// 0: код ошибки
	// 1-8: UID чипа
	// 9-12: данные из страницы чипа(4 байта)
	if (!addData(OK))
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(BUFFER_OVERFLOW, REPLY_READ_CARD_PAGE);
		return;
	}

	if (!ntagRead4pages(PAGE_UID))
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(READ_ERROR, REPLY_READ_CARD_PAGE);
		return;
	}
	for (uint8_t i = 0; i <= 7; i++)
	{
		if (!addData(ntag_page[i]))
		{
			SPI.end();
			digitalWrite(LED_PIN, LOW);
			sendError(BUFFER_OVERFLOW, REPLY_READ_CARD_PAGE);
			return;
		}
	}

	while (pageFrom <= pageTo)
	{
		// 0: какую страницу чипа
		if (!ntagRead4pages(pageFrom))
		{
			SPI.end();
			digitalWrite(LED_PIN, LOW);
			sendError(READ_ERROR, REPLY_READ_CARD_PAGE);
			return;
		}
		uint8_t n = (pageTo - pageFrom + 1);
		if (n > 4) n = 4;
		for (uint8_t i = 0; i < n; i++)
		{
			if (!addData(pageFrom))
			{
				SPI.end();
				digitalWrite(LED_PIN, LOW);
				sendError(BUFFER_OVERFLOW, REPLY_READ_CARD_PAGE);
				return;
			}
			for (uint8_t j = 0; j < 4; j++)
			{
				if (!addData(ntag_page[i * 4 + j]))
				{
					SPI.end();
					digitalWrite(LED_PIN, LOW);
					sendError(BUFFER_OVERFLOW, REPLY_READ_CARD_PAGE);
					return;
				}
			}
			pageFrom++;
		}
	}
	SPI.end();
	digitalWrite(LED_PIN, LOW);

	sendData();
}

// Обновить маску команды в буфере
void updateTeamMask()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_UPDATE_TEAM_MASK);
		return;
	}

	// 0-1: номер команды
	// 2-5: время выдачи чипа
	// 6-7: маска участников
	saveNewMask();

	init_package(REPLY_UPDATE_TEAM_MASK);
	// 0: код ошибки
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_UPDATE_TEAM_MASK);
		return;
	}
	sendData();

	if (stationMode == MODE_INIT)
	{
		// включаем SPI ищем чип вблизи. Если не находим выходим из функции чтения чипов
		SPI.begin();      // Init SPI bus
		mfrc522.PCD_Init();    // Init MFRC522
		mfrc522.PCD_SetAntennaGain(gainCoeff);

		// Look for new cards
		if (!mfrc522.PICC_IsNewCardPresent())
		{
			SPI.end();
			lastTeamFlag = 0;
			return;
		}
#ifdef DEBUG
		Serial.println(F("!!!chip found"));
#endif

		// Select one of the cards
		if (!mfrc522.PICC_ReadCardSerial())
		{
			SPI.end();
#ifdef DEBUG
			Serial.println(F("!!!fail to select card"));
#endif

			return;
		}

		// читаем блок информации
		if (!ntagRead4pages(PAGE_UID))
		{
			SPI.end();
			return;
		}

		//неправильный тип чипа	
		if (ntag_page[14] != chipType)
		{
			SPI.end();
#ifdef DEBUG
			Serial.println(F("!!!incorrect chip"));
#endif

			errorBeep(2);
			return;
		}

		/*
		Фильтруем
		1 - неправильный тип чипа
		2 - чип от другой прошивки
		3 - чип более недельной давности инициализации
		4 - чипы с командой №0 или >maxTeamNumber
		5 - чип, который совпадает с уже отмеченным (в lastTeams[])
		*/

		// читаем блок информации
		if (!ntagRead4pages(PAGE_CHIP_NUM))
		{
			SPI.end();
			return;
		}

		// неправильный тип чипа
		if (ntag_page[2] != NTAG_MARK)
		{
			SPI.end();
#ifdef DEBUG
			Serial.println(F("!!!incorrect chip"));
#endif

			errorBeep(2);
			return;
		}

		// чип от другой прошивки
		if (ntag_page[3] != FW_VERSION)
		{
			SPI.end();
#ifdef DEBUG
			Serial.println(F("!!!incorrect fw"));
#endif

			errorBeep(2);
			return;
		}

		// Не слишком ли старый чип? Недельной давности и более
		uint32_t timeInit = ntag_page[4];
		timeInit = timeInit << 8;
		timeInit += ntag_page[5];
		timeInit = timeInit << 8;
		timeInit += ntag_page[6];
		timeInit = timeInit << 8;
		timeInit += ntag_page[7];
		if ((systemTime.unixtime - timeInit) > maxTimeInit)
		{
			SPI.end();
#ifdef DEBUG
			Serial.println(F("!!!outdated chip"));
#endif

			errorBeep(2);
			return;
		}

		uint16_t chipNum = (ntag_page[0] << 8) + ntag_page[1];

		// Если номер чипа =0 или >maxTeamNumber

		if (chipNum < 1 || chipNum > maxTeamNumber)
		{
			SPI.end();
#ifdef DEBUG
			Serial.print(F("!!!incorrect chip #"));
			Serial.println(String(chipNum));
#endif

			errorBeep(3);
			return;
		}

		// не надо ли обновить у чипа маску?
		// 0-1: номер команды
		// 2-5: время выдачи чипа
		// 6-7: маска участников
		if (newTeamMask[0] + newTeamMask[1] != 0
			&& ntag_page[0] == newTeamMask[0]
			&& ntag_page[1] == newTeamMask[1]
			&& ntag_page[4] == newTeamMask[2]
			&& ntag_page[5] == newTeamMask[3]
			&& ntag_page[6] == newTeamMask[4]
			&& ntag_page[7] == newTeamMask[5])
		{
			if (ntag_page[8] != newTeamMask[6] || ntag_page[9] != newTeamMask[7])
			{
#ifdef DEBUG
				Serial.print(F("!!!updating mask"));
#endif
				digitalWrite(LED_PIN, HIGH);
				uint8_t dataBlock[4] = { newTeamMask[6], newTeamMask[7], ntag_page[10], ntag_page[11] };
				if (!ntagWritePage(dataBlock, PAGE_TEAM_MASK))
				{
					SPI.end();
					digitalWrite(LED_PIN, LOW);
					errorBeep(4);
					return;
				}
			}
			SPI.end();
			clearNewMask();
			digitalWrite(LED_PIN, LOW);
		}
	}



}

void saveNewMask()
{
#ifdef DEBUG
	Serial.print(F("!!!New mask set to: "));
#endif

	for (uint8_t i = 0; i < 8; i++)
	{
		newTeamMask[i] = uartBuffer[DATA_START_BYTE + i];
#ifdef DEBUG
		Serial.print(String(newTeamMask[i], HEX));
#endif

	}
#ifdef DEBUG
	Serial.println();
#endif

}

void clearNewMask()
{
#ifdef DEBUG
	Serial.print(F("!!!Mask cleared: "));
#endif

	for (uint8_t i = 0; i < 8; i++)
	{
		newTeamMask[i] = 0;
#ifdef DEBUG
		Serial.print(String(newTeamMask[i], HEX));
#endif

	}
#ifdef DEBUG
	Serial.println();
#endif

}

// пишем присланные с ББ 4 байта в указанную страницу
void writeCardPage()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_WRITE_CARD_PAGE);
		return;
	}

	digitalWrite(LED_PIN, HIGH);
	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522
	mfrc522.PCD_SetAntennaGain(gainCoeff);

	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(NO_CHIP, REPLY_WRITE_CARD_PAGE);
		return;
	}

	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(NO_CHIP, REPLY_WRITE_CARD_PAGE);
		return;
	}

	// 0-7: UID чипа
	// 8: номер страницы
	// 9-12: данные для записи (4 байта)

	// проверить UID
	if (!ntagRead4pages(PAGE_UID))
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(READ_ERROR, REPLY_WRITE_CARD_PAGE);
		return;
	}
	bool flag = false;
	for (uint8_t i = 0; i <= 7; i++)
	{
		if (uartBuffer[DATA_START_BYTE + i] != 0xff && ntag_page[i] != uartBuffer[DATA_START_BYTE + i])
		{
			flag = true;
			break;
		}
	}
	if (flag)
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(WRONG_UID, REPLY_WRITE_CARD_PAGE);
		return;
	}

	// записать страницу
	uint8_t dataBlock[] =
	{
		uartBuffer[DATA_START_BYTE + 9],
		uartBuffer[DATA_START_BYTE + 10],
		uartBuffer[DATA_START_BYTE + 11],
		uartBuffer[DATA_START_BYTE + 12]
	};
	if (!ntagWritePage(dataBlock, uartBuffer[DATA_START_BYTE + 8]))
	{
		SPI.end();
		digitalWrite(LED_PIN, LOW);
		sendError(WRITE_ERROR, REPLY_WRITE_CARD_PAGE);
		return;
	}
	SPI.end();
	digitalWrite(LED_PIN, LOW);

	init_package(REPLY_WRITE_CARD_PAGE);

	// 0: код ошибки
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_WRITE_CARD_PAGE);
		return;
	}
	sendData();
}

// читаем флэш
void readFlash()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_READ_FLASH);
		return;
	}

	// 0-3: адрес начала чтения
	// 4: размер блока
	uint32_t startAddress = uartBuffer[DATA_START_BYTE];
	startAddress <<= 8;
	startAddress += uartBuffer[DATA_START_BYTE + 1];
	startAddress <<= 8;
	startAddress += uartBuffer[DATA_START_BYTE + 2];
	startAddress <<= 8;
	startAddress += uartBuffer[DATA_START_BYTE + 3];

	uint8_t length = uartBuffer[DATA_START_BYTE + 4];
	if (length > (256 - 7 - DATA_LENGTH_READ_FLASH)) length = 256 - 7 - DATA_LENGTH_READ_FLASH;

#ifdef DEBUG
	Serial.print(F("!!!flash read="));
	Serial.print(String(startAddress));
	Serial.print(F("/"));
	Serial.println(String(length));
#endif

	init_package(REPLY_READ_FLASH);
	/*Serial.print(F("!!!init "));
	Serial.println(String(uartBufferPosition));*/

	// 0: код ошибки
	// 0-3: адрес начала чтения
	// 4-n: данные из флэша
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_READ_FLASH);
		return;
	}
	//Serial.print(F("!!!OK "));
	//Serial.println(String(uartBufferPosition));

	bool flag = true;
	flag &= addData((startAddress & 0xFF000000) >> 24);
	flag &= addData((startAddress & 0x00FF0000) >> 16);
	flag &= addData((startAddress & 0x0000FF00) >> 8);
	flag &= addData(startAddress & 0x000000FF);
	//Serial.print(F("!!!address "));
	//Serial.println(String(uartBufferPosition));

	for (long i = startAddress; i < startAddress + (uint32_t)length; i++)
	{
		uint8_t b = SPIflash.readByte(i);
		/*Serial.print(F("!!!"));
		Serial.print(String(uartBufferPosition));
		Serial.print(F("="));
		Serial.println(String(b));*/
		flag &= addData(b);

#ifdef DEBUG
		Serial.print(String(i));
		Serial.print(F("="));
		if (b < 0x10) Serial.print(F("0"));
		Serial.println(String(b, HEX));
#endif
	}

	if (!flag)
	{
		sendError(BUFFER_OVERFLOW, REPLY_READ_FLASH);
		return;
	}

	sendData();
}

// пишем в флэш
bool writeFlash()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_WRITE_FLASH);
		return;
	}

	// 0-3: адрес начала записи
	// 1: кол-во записанных байт (для проверки)
	uint32_t startAddress = uartBuffer[DATA_START_BYTE];
	startAddress <<= 8;
	startAddress += uartBuffer[DATA_START_BYTE + 1];
	startAddress <<= 8;
	startAddress += uartBuffer[DATA_START_BYTE + 2];
	startAddress <<= 8;
	startAddress += uartBuffer[DATA_START_BYTE + 3];

	uint8_t length = uartBuffer[LENGTH_BYTE] - 4;

	init_package(REPLY_WRITE_FLASH);

	/*uint32_t i = startAddress + uartBuffer[LENGTH_BYTE] - 4;
	uint8_t n = 0;
	while (startAddress < i)
	{
		if (!SPIflash.writeByte(startAddress, uartBuffer[DATA_START_BYTE + 4 + n]))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_FLASH);
			return false;
		}
		startAddress++;
		n++;
	}*/
	if (!SPIflash.writeByteArray(startAddress, &uartBuffer[DATA_START_BYTE + 4], length))
	{
		sendError(WRITE_ERROR, REPLY_WRITE_FLASH);
		return false;
	}

	// 0: код ошибки
	// 1: кол-во записанных байт (для проверки)
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_WRITE_FLASH);
		return false;
	}
	if (!addData(length))
	{
		sendError(BUFFER_OVERFLOW, REPLY_WRITE_FLASH);
		return false;
	}

	sendData();
	return true;
}

// стираем команду с флэша (4096 байт)
void eraseTeamFlash()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_ERASE_FLASH_SECTOR);
		return;
	}

	uint32_t teamNumber = uartBuffer[DATA_START_BYTE];
	teamNumber <<= 8;
	teamNumber += uartBuffer[DATA_START_BYTE + 1];
#ifdef DEBUG
	Serial.print(F("!!!erasing "));
	Serial.println(String(teamNumber));
#endif

	if (!eraseTeamFromFlash(teamNumber))
	{
		sendError(ERASE_ERROR, REPLY_ERASE_FLASH_SECTOR);
		return;
	}

	init_package(REPLY_ERASE_FLASH_SECTOR);
	// 0: код ошибки
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_ERASE_FLASH_SECTOR);
		return;
	}
	sendData();
}

// выдает конфигурацию станции
void getConfig()
{
	// 0: код ошибки
	// 1: версия прошивки
	// 2: номер режима
	// 3: тип чипов (емкость разная, а распознать их программно можно только по ошибкам чтения "дальних" страниц)
	// 4-7: емкость флэш - памяти
	// 8-11: размер сектора флэш - памяти
	// 12-15: коэффициент пересчета напряжения(float, 4 bytes) - просто умножаешь коэффициент на полученное в статусе число и будет температура
	// 16: коэффициент усиления антенны RFID
	// 17-18: размер блока хранения команды
	init_package(REPLY_GET_CONFIG);

	bool flag = true;
	flag &= addData(OK);
	flag &= addData(FW_VERSION);
	flag &= addData(stationMode);
	flag &= addData(NTAG_MARK);

	uint32_t n = SPIflash.getCapacity();
	flag &= addData((n & 0xFF000000) >> 24);
	flag &= addData((n & 0x00FF0000) >> 16);
	flag &= addData((n & 0x0000FF00) >> 8);
	flag &= addData(n & 0x000000FF);

	byte v[4];
	floatToByte(v, voltageCoeff);
	flag &= addData(v[0]);
	flag &= addData(v[1]);
	flag &= addData(v[2]);
	flag &= addData(v[3]);

	flag &= addData(gainCoeff);

	flag &= addData(TEAM_FLASH_SIZE >> 8);
	flag &= addData(TEAM_FLASH_SIZE & 0x00FF);

	flag &= addData(FLASH_BLOCK_SIZE >> 8);
	flag &= addData(FLASH_BLOCK_SIZE & 0x00FF);

	if (!flag)
	{
		sendError(BUFFER_OVERFLOW, REPLY_GET_CONFIG);
		return;
	}

	sendData();
}

// сохранить коэфф. пересчета ADC в напряжение для резисторного делителя 10кОм + 2.2кОм
void setVCoeff()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_SET_KOEFF);
		return;
	}

	// 0-3: коэфф.
	union Convert
	{
		float number;
		byte byte[4];
	} p;

	for (byte i = 0; i < 4; i++)
	{
		p.byte[i] = uartBuffer[DATA_START_BYTE + i];
		if (!eepromwrite(EEPROM_VOLTAGE_KOEFF + i * 3, uartBuffer[DATA_START_BYTE + i]))
		{
			errorBeep(1); // Read the station number from the EEPROM
			sendError(WRITE_ERROR, REPLY_SET_KOEFF);
		}
	}
	voltageCoeff = p.number;

	init_package(REPLY_SET_KOEFF);
	// 0: код ошибки
	// 1...: данные из флэша
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_SET_KOEFF);
		return;
	}

	sendData();
}

// сохранить коэфф. усиления для RFID
void setGain()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_SET_GAIN);
		return;
	}

	// 0: коэфф.
	gainCoeff = uartBuffer[DATA_START_BYTE] & 0x70;
	if (!eepromwrite(EEPROM_GAIN, gainCoeff))
	{
		errorBeep(1);
		sendError(WRITE_ERROR, REPLY_SET_GAIN);
	}

	init_package(REPLY_SET_GAIN);
	// 0: код ошибки
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_SET_GAIN);
		return;
	}

	sendData();
}

// сохранить тип чипа
void setChipType()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_SET_CHIP_TYPE);
		return;
	}

	// 0: тип чипа
	chipType = uartBuffer[DATA_START_BYTE];

	if (!selectChipType(chipType))
	{
		errorBeep(1);
		sendError(WRONG_CHIP_TYPE, REPLY_SET_CHIP_TYPE);
	}
	if (!eepromwrite(EEPROM_CHIP_TYPE, chipType))
	{
		errorBeep(1);
		sendError(WRITE_ERROR, REPLY_SET_CHIP_TYPE);
	}

	init_package(REPLY_SET_CHIP_TYPE);
	// 0: код ошибки
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_SET_CHIP_TYPE);
		return;
	}

	sendData();
}

// сохранить размер блока команды
void setTeamFlashSize()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_SET_TEAM_FLASH_SIZE);
		return;
	}

	// 0-1: размер блока
	uint16_t n = uartBuffer[DATA_START_BYTE] * 256 + uartBuffer[DATA_START_BYTE + 1];

	if (TEAM_FLASH_SIZE < 14)
	{
		errorBeep(1);
		sendError(WRONG_SIZE, REPLY_SET_TEAM_FLASH_SIZE);
	}
	TEAM_FLASH_SIZE = n;

	if (!eepromwrite(EEPROM_TEAM_BLOCK_SIZE, uartBuffer[DATA_START_BYTE]))
	{
		errorBeep(1);
		sendError(WRITE_ERROR, REPLY_SET_TEAM_FLASH_SIZE);
	}
	if (!eepromwrite(EEPROM_TEAM_BLOCK_SIZE + 3, uartBuffer[DATA_START_BYTE + 1]))
	{
		errorBeep(1);
		sendError(WRITE_ERROR, REPLY_SET_TEAM_FLASH_SIZE);
	}

	init_package(REPLY_SET_TEAM_FLASH_SIZE);
	// 0: код ошибки
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_SET_TEAM_FLASH_SIZE);
		return;
	}

	sendData();
}

// сохранить размер стираемого блока
void setFlashBlockSize()
{
	// Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_SET_FLASH_BLOCK_SIZE);
		return;
	}

	// 0-1: размер блока
	uint16_t n = uartBuffer[DATA_START_BYTE] * 256 + uartBuffer[DATA_START_BYTE + 1];

	if (TEAM_FLASH_SIZE < 14)
	{
		errorBeep(1);
		sendError(WRONG_SIZE, REPLY_SET_FLASH_BLOCK_SIZE);
	}
	TEAM_FLASH_SIZE = n;

	if (!eepromwrite(EEPROM_FLASH_BLOCK_SIZE, uartBuffer[DATA_START_BYTE]))
	{
		errorBeep(1);
		sendError(WRITE_ERROR, REPLY_SET_FLASH_BLOCK_SIZE);
	}
	if (!eepromwrite(EEPROM_FLASH_BLOCK_SIZE + 3, uartBuffer[DATA_START_BYTE + 1]))
	{
		errorBeep(1);
		sendError(WRITE_ERROR, REPLY_SET_FLASH_BLOCK_SIZE);
	}

	init_package(REPLY_SET_FLASH_BLOCK_SIZE);
	// 0: код ошибки
	if (!addData(OK))
	{
		sendError(BUFFER_OVERFLOW, REPLY_SET_FLASH_BLOCK_SIZE);
		return;
	}

	sendData();
}

// Обработка поднесенного чипа
void processRfidCard()
{
	if (stationNumber == 0 || stationNumber == 0xff) return;
	DS3231_get(&systemTime);
	uint32_t checkTime = systemTime.unixtime;

	// включаем SPI ищем чип вблизи. Если не находим выходим из функции чтения чипов
	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();    // Init MFRC522
	mfrc522.PCD_SetAntennaGain(gainCoeff);

	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		SPI.end();
		lastTeamFlag = 0;
		return;
	}
#ifdef DEBUG
	Serial.println(F("!!!chip found"));
#endif

	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		SPI.end();
#ifdef DEBUG
		Serial.println(F("!!!fail to select card"));
#endif

		return;
	}

	// читаем блок информации
	if (!ntagRead4pages(PAGE_UID))
	{
		SPI.end();
		return;
	}

	//неправильный тип чипа	
	if (ntag_page[14] != chipType)
	{
		SPI.end();
#ifdef DEBUG
		Serial.println(F("!!!incorrect chip"));
#endif

		errorBeep(2);
		return;
	}

	/*
	Фильтруем
	1 - неправильный тип чипа
	2 - чип от другой прошивки
	3 - чип более недельной давности инициализации
	4 - чипы с командой №0 или >maxTeamNumber
	5 - чип, который совпадает с уже отмеченным (в lastTeams[])
	*/

	// читаем блок информации
	if (!ntagRead4pages(PAGE_CHIP_NUM))
	{
		SPI.end();
		return;
	}

	// неправильный тип чипа
	if (ntag_page[2] != NTAG_MARK)
	{
		SPI.end();
#ifdef DEBUG
		Serial.println(F("!!!incorrect chip"));
#endif

		errorBeep(2);
		return;
	}

	// чип от другой прошивки
	if (ntag_page[3] != FW_VERSION)
	{
		SPI.end();
#ifdef DEBUG
		Serial.println(F("!!!incorrect fw"));
#endif

		errorBeep(2);
		return;
	}

	// Не слишком ли старый чип? Недельной давности и более
	uint32_t timeInit = ntag_page[4];
	timeInit = timeInit << 8;
	timeInit += ntag_page[5];
	timeInit = timeInit << 8;
	timeInit += ntag_page[6];
	timeInit = timeInit << 8;
	timeInit += ntag_page[7];
	if ((systemTime.unixtime - timeInit) > maxTimeInit)
	{
		SPI.end();
#ifdef DEBUG
		Serial.println(F("!!!outdated chip"));
#endif

		errorBeep(2);
		return;
	}

	uint16_t chipNum = (ntag_page[0] << 8) + ntag_page[1];

	// Если номер чипа =0 или >maxTeamNumber

	if (chipNum < 1 || chipNum > maxTeamNumber)
	{
		SPI.end();
#ifdef DEBUG
		Serial.print(F("!!!incorrect chip #"));
		Serial.println(String(chipNum));
#endif

		errorBeep(3);
		return;
	}

	// не надо ли обновить у чипа маску?
	// 0-1: номер команды
	// 2-5: время выдачи чипа
	// 6-7: маска участников
	if (newTeamMask[0] + newTeamMask[1] != 0
		&& ntag_page[0] == newTeamMask[0]
		&& ntag_page[1] == newTeamMask[1]
		&& ntag_page[4] == newTeamMask[2]
		&& ntag_page[5] == newTeamMask[3]
		&& ntag_page[6] == newTeamMask[4]
		&& ntag_page[7] == newTeamMask[5])
	{
#ifdef DEBUG
		Serial.print(F("!!!updating mask"));
#endif

		if (ntag_page[8] != newTeamMask[6] || ntag_page[9] != newTeamMask[7])
		{
			digitalWrite(LED_PIN, HIGH);
			uint8_t dataBlock[4] = { newTeamMask[6], newTeamMask[7], ntag_page[10], ntag_page[11] };
			if (!ntagWritePage(dataBlock, PAGE_TEAM_MASK))
			{
				SPI.end();
				digitalWrite(LED_PIN, LOW);
				errorBeep(4);
				return;
			}
		}
		SPI.end();
		clearNewMask();
		lastTeamFlag = chipNum;
		digitalWrite(LED_PIN, LOW);
		return;
	}

	// Если это повторная отметка
	if (chipNum == lastTeamFlag)
	{
#ifdef DEBUG
		Serial.print(F("!!!chip still attached"));
#endif

		SPI.end();
		return;
	}

	bool flag = false;
	if (stationMode != MODE_FINISH_KP)
	{
		// сравнить с буфером последних команд
		for (uint8_t i = 0; i < lastTeamsLength * 2; i = i + 2)
		{
			if (lastTeams[i] == ntag_page[0] && lastTeams[i + 1] == ntag_page[1])
			{
				flag = true;
				break;
			}
		}
	}

	// Есть ли чип на флэше
	if (!flag && SPIflash.readByte((uint32_t)((uint32_t)chipNum * (uint32_t)TEAM_FLASH_SIZE)) != 255) flag = true;

	// если новый чип или финишный КП
	if (!flag || stationMode == MODE_FINISH_KP)
	{
#ifdef DEBUG
		Serial.print(F("!!!checking chip"));
#endif

		digitalWrite(LED_PIN, HIGH);
		// ищем свободную страницу на чипе
		uint8_t newPage = findNewPage();

		// ошибка чтения или больше максимума... Наверное, переполнен???
		if (newPage < PAGE_DATA_START || newPage >= TAG_MAX_PAGE)
		{
			SPI.end();
			digitalWrite(LED_PIN, LOW);
			errorBeep(5);
			return;
		}

		// Пишем на чип отметку
		if (!writeCheckPointToCard(newPage, checkTime))
		{
			SPI.end();
			digitalWrite(LED_PIN, LOW);
			errorBeep(6);
			return;
		}

		// добавляем в буфер последних команд
		addLastTeam(chipNum);
		lastTimeChecked = checkTime;
		if (!flag) totalChipsChecked++;

		// Пишем дамп чипа во флэш
		if (!writeDumpToFlash(chipNum, checkTime))
		{
			SPI.end();
			digitalWrite(LED_PIN, LOW);
			errorBeep(7);
			return;
		}
		SPI.end();
		lastTeamFlag = chipNum;
		digitalWrite(LED_PIN, LOW);
		beep(200, 1);
#ifdef DEBUG
		Serial.print(F("!!!New record #"));
		Serial.println(String(chipNum));
#endif
	}
}

// обработка входящих команд
bool readUart()
{
	while (Serial.available())
	{
		int c = Serial.read();
		if (c == -1) // can't read stream
		{
#ifdef DEBUG
			Serial.println(F("!!! UARTread error"));
#endif
			uartBufferPosition = 0;
			receivingData = false;
			// errorBeepMs(50, 1);
			return false;
		}
		// 0 byte = FE
		else if (uartBufferPosition == 0 && c == 0xfe)
		{
#ifdef DEBUG
			Serial.print(F("!!!byte0="));
			if (c < 0x10) Serial.print(F("0"));
			Serial.println(String(byte(c), HEX));
#endif
			receivingData = true;
			uartBuffer[uartBufferPosition] = (byte)c;
			uartBufferPosition++;
			// refresh timeout
			receiveStartTime = millis();
		}
		// 1st byte = FE
		else if (uartBufferPosition == 1 && c == 0xfe)
		{
#ifdef DEBUG
			Serial.print(F("!!!byte1"));
			if (c < 0x10) Serial.print(F("0"));
			Serial.println(String(byte(c), HEX));
#endif
			uartBuffer[uartBufferPosition] = (byte)c;
			uartBufferPosition++;
		}
		// 2nd byte = command, length and data
		else if (uartBufferPosition >= PACKET_ID)
		{
			uartBuffer[uartBufferPosition] = (byte)c;
#ifdef DEBUG
			Serial.print(F("!!!byte"));
			Serial.print(String(uartBufferPosition));
			Serial.print(F("="));
			if (c < 0x10) Serial.print(F("0"));
			Serial.println(String(byte(c), HEX));
#endif
			// incorrect length
			if (uartBufferPosition == LENGTH_BYTE && uartBuffer[LENGTH_BYTE] > (254 - DATA_START_BYTE))
			{
#ifdef DEBUG
				Serial.println(F("!!!incorrect length"));
#endif
				uartBufferPosition = 0;
				receivingData = false;
				beep(50, 2);
				return false;
			}

			// packet is received
			if (uartBufferPosition > LENGTH_BYTE && uartBufferPosition >= DATA_START_BYTE + uartBuffer[LENGTH_BYTE])
			{
				// crc matching
#ifdef DEBUG
				Serial.print(F("!!!received packet expected CRC="));
				Serial.println(String(crcCalc(uartBuffer, STATION_NUMBER_BYTE, uartBufferPosition - 1), HEX));
#endif
				if (uartBuffer[uartBufferPosition] == crcCalc(uartBuffer, PACKET_ID, uartBufferPosition - 1))
				{
#ifdef DEBUG
					Serial.print(F("!!!Command received:"));
					for (uint8_t i = 0; i <= uartBufferPosition; i++)
					{
						Serial.print(F(" "));
						if (uartBuffer[i] < 0x10) Serial.print(F("0"));
						Serial.print(String(uartBuffer[i], HEX));
					}
					Serial.println();
#endif
					uartBufferPosition = 0;
					receivingData = false;
					return true;
				}
				else // CRC not correct
				{
#ifdef DEBUG
					Serial.println(F("!!!incorrect crc"));
#endif
					uartBufferPosition = 0;
					receivingData = false;
					beep(50, 2);
					return false;
				}
			}
			uartBufferPosition++;
		}
		else
		{
#ifdef DEBUG
			Serial.println(F("!!!unexpected byte"));
#endif
			receivingData = false;
			uartBufferPosition = 0;
			beep(50, 3);
		}
	}
	return false;
}

// Internal functions

// чтение заряда батареи
uint16_t getBatteryLevel()
{
	const uint8_t MeasurementsToAverage = 16;
	uint16_t val = 0;
	uint32_t AverageValue = analogRead(BATTERY_PIN);
	for (uint8_t i = 1; i < MeasurementsToAverage; ++i)
	{
		val = analogRead(BATTERY_PIN);
		AverageValue = (AverageValue + val) / 2;
		delay(5);
	}
	return AverageValue;
}

// запись в память с мажоритарным резервированием
bool eepromwrite(uint16_t adr, uint8_t val)
{
	for (uint8_t i = 0; i < 3; i++)
	{
		EEPROM.write(adr + i, val);
		if (EEPROM.read(adr + i) != val) return false;
	}
	return true;
}

// считывание из памяти с учетом мажоритального резервирования
int eepromread(uint16_t adr)
{
	uint8_t byte1 = EEPROM.read(adr);
	uint8_t byte2 = EEPROM.read(adr + 1);
	uint8_t byte3 = EEPROM.read(adr + 2);

	// возвращаем при совпадении два из трех
	if (byte1 == byte2 && byte1 == byte3)
	{
		return byte1;
	}
	if (byte1 == byte2)
	{
		return byte1;
	}
	if (byte1 == byte3)
	{
		return byte1;
	}
	if (byte2 == byte3)
	{
		return byte2;
	}
	return -1;
}

// сигнал станции, длительность сигнала и задержки в мс и число повторений
void beep(uint16_t ms, uint8_t n)
{
	for (uint8_t i = 0; i < n; i++)
	{
		digitalWrite(LED_PIN, HIGH);
		tone(BUZZER_PIN, 4000, ms);
		delay(ms);
		digitalWrite(LED_PIN, LOW);
		if ((n - i) != 0)
		{
			delay(ms);
		}
	}
}

// сигнал станции, длительность сигнала и задержки в мс и число повторений
void errorBeepMs(uint16_t ms, uint8_t n)
{
	for (uint8_t i = 0; i < n; i++)
	{
		digitalWrite(ERROR_LED_PIN, HIGH);
		tone(BUZZER_PIN, 500, ms);
		digitalWrite(ERROR_LED_PIN, LOW);
		if ((n - i) > 0)
		{
			delay(ms);
		}
	}
}

void errorBeep(uint8_t n)
{
	uint16_t ms = 200;
	for (uint8_t i = 0; i < n; i++)
	{
		digitalWrite(ERROR_LED_PIN, HIGH);
		tone(BUZZER_PIN, 500, ms);
		digitalWrite(ERROR_LED_PIN, LOW);
		if ((n - i) > 0)
		{
			delay(ms);
		}
	}
}

// инициализация пакета данных
void init_package(uint8_t command)
{
	uartBuffer[0] = uartBuffer[1] = 0xFE;
	uartBuffer[STATION_NUMBER_BYTE] = stationNumber;
	uartBuffer[COMMAND_BYTE] = command;
	uartBufferPosition = DATA_START_BYTE;
}

// добавление данных в буфер
bool addData(uint8_t data)
{
	if (uartBufferPosition > 254)
	{
		return false;
	}
	uartBuffer[uartBufferPosition] = data;
	uartBufferPosition++;
	return true;
}

// передача пакета данных
void sendData()
{
	uartBuffer[LENGTH_BYTE] = uartBufferPosition - COMMAND_BYTE - 1;
	uartBuffer[uartBufferPosition] = crcCalc(uartBuffer, PACKET_ID, uartBufferPosition - 1);
	uartBufferPosition++;
#ifdef DEBUG
	Serial.print(F("!!!Sending:"));
	for (uint8_t i = 0; i < uartBufferPosition; i++)
	{
		Serial.print(F(" "));
		if (uartBuffer[i] < 0x10) Serial.print(F("0"));
		Serial.print(String(uartBuffer[i], HEX));
	}
	Serial.println();
#endif
	Serial.write(uartBuffer, uartBufferPosition);
	uartBufferPosition = 0;
}

// запись страницы (4 байта) в чип
bool ntagWritePage(uint8_t* dataBlock, uint8_t pageAdr)
{
	const uint8_t sizePageNtag = 4;

	uint8_t n = 0;
	MFRC522::StatusCode status;

	status = MFRC522::STATUS_ERROR;
	while (status != MFRC522::STATUS_OK && n < 3)
	{
		status = (MFRC522::StatusCode) mfrc522.MIFARE_Ultralight_Write(pageAdr, dataBlock, sizePageNtag);
		n++;
		if (status != MFRC522::STATUS_OK)
		{
			mfrc522.PCD_Init();
			mfrc522.PICC_IsNewCardPresent();
			mfrc522.PICC_ReadCardSerial();
		}
	}

	if (status != MFRC522::STATUS_OK)
	{
#ifdef DEBUG
		Serial.println(F("!!!chip write failed"));
#endif

		return false;
	}

	uint8_t buffer[18];
	uint8_t size = sizeof(buffer);


	n = 0;
	status = MFRC522::STATUS_ERROR;
	while (status != MFRC522::STATUS_OK && n < 3)
	{
		status = (MFRC522::StatusCode) mfrc522.MIFARE_Read(pageAdr, buffer, &size);
		n++;
	}
	if (status != MFRC522::STATUS_OK)
	{
#ifdef DEBUG
		Serial.println(F("!!!chip verify read failed"));
#endif

		return false;
	}

	for (uint8_t i = 0; i < 4; i++)
	{
		if (buffer[i] != dataBlock[i])
		{
#ifdef DEBUG
			Serial.println(F("!!!chip verify failed"));
#endif

			return false;
		}
	}

	return true;
}

// чтение 4-х страниц (16 байт) из чипа
bool ntagRead4pages(uint8_t pageAdr)
{
	uint8_t size = 18;
	uint8_t buffer[18];

	uint8_t n = 0;
	MFRC522::StatusCode status;

	status = MFRC522::STATUS_ERROR;
	while (status != MFRC522::STATUS_OK && n < 3)
	{
		status = (MFRC522::StatusCode) mfrc522.MIFARE_Read(pageAdr, buffer, &size);
		if (status != MFRC522::STATUS_OK)
		{
			mfrc522.PCD_Init();
			mfrc522.PICC_IsNewCardPresent();
			mfrc522.PICC_ReadCardSerial();
		}
		n++;
	}

	if (status != MFRC522::STATUS_OK)
	{
#ifdef DEBUG
		Serial.println(F("!!!card read failed"));
#endif
		return false;
	}

	for (uint8_t i = 0; i < 16; i++)
	{
		ntag_page[i] = buffer[i];
	}
	return true;
}

// пишет на чип время и станцию отметки
bool writeCheckPointToCard(uint8_t newPage, uint32_t checkTime)
{
	uint8_t dataBlock[4];
	dataBlock[0] = stationNumber;
	dataBlock[1] = (checkTime & 0x00FF0000) >> 16;
	dataBlock[2] = (checkTime & 0x0000FF00) >> 8;
	dataBlock[3] = (checkTime & 0x000000FF);

	if (!ntagWritePage(dataBlock, newPage))
	{
		return false;
	}
	return true;
}

// Поиск последней записанной страницы на чипе.
// !!! разобраться в алгоритме Саши или сделать свой бинарный поиск
uint8_t findNewPage()
{
	/*uint8_t page = TAG_MAX_PAGE - 4;
	while (page >= PAGE_DATA_START)
	{
		if (!ntagRead4pages(page))
		{
			return 0;
		}
		for (int n = 3; n >= 0; n--)
		{
			if (ntag_page[0 + n * 4] != 0)
			{
				if (stationMode == MODE_FINISH_KP && ntag_page[0 + n * 4] == stationNumber)
				{
					return page;
				}
				else
				{
					return ++page;
				}
			}
			page--;
		}
	}
	return TAG_MAX_PAGE;*/

	uint8_t page = PAGE_DATA_START;
	while (page < TAG_MAX_PAGE)
	{
		if (!ntagRead4pages(page))
		{
			return 0;
		}
		for (uint8_t n = 0; n < 4; n++)
		{
			// 1) текущая страница пустая
			// 2) равна номеру станции
			if (ntag_page[n * 4] == 0)
			{
				if (!ntagRead4pages(page - 1))
				{
					return 0;
				}
				if (ntag_page[0] == stationNumber) page--;
				return page;
			}
			// 2) равна номеру станции, а след. страница пустая
			// if (ntag_page[0 + n * 4] == 0 || (ntag_page[0 + n * 4] == stationNumber && ntag_page[0 + 4 + n * 4] == 0)) return page;
			page++;
		}
	}
	return TAG_MAX_PAGE;


	/*uint8_t finishpage = TAG_MAX_PAGE - 1;
	uint8_t startpage = PAGE_DATA_START;
	uint8_t page = (finishpage + startpage) / 2;

	while (1)
	{
		if (finishpage == startpage)
		{
			return (finishpage);
		}

		page = (finishpage + startpage) / 2;

		if (!ntagRead4pages(page))
		{
			return 0;
		}

		if (ntag_page[0] == 0)
		{
			finishpage = (finishpage - startpage) / 2 + startpage;
		}
		else
		{
			startpage = finishpage - (finishpage - startpage) / 2;
		}
	}*/
}

// пишем дамп чипа в лог
byte writeDumpToFlash(uint16_t teamNumber, uint32_t checkTime)
{
	// адрес хранения в каталоге
	uint32_t teamFlashAddress = (uint32_t)(uint32_t(teamNumber) * (uint32_t)TEAM_FLASH_SIZE);
#ifdef DEBUG
	Serial.print(F("!!!Write flash address: "));
	Serial.println(String(teamFlashAddress));
#endif

	// если режим финишной станции, то, возможно, надо переписать содержимое.
	if (stationMode == MODE_FINISH_KP && SPIflash.readByte(teamFlashAddress) != 255)
	{
		eraseTeamFromFlash(teamNumber);
#ifdef DEBUG
		Serial.print(F("!!!erased team #"));
		Serial.println(String((uint32_t)((uint32_t)teamNumber / (uint32_t)256)));
#endif
	}

	// save basic parameters
	if (!ntagRead4pages(PAGE_CHIP_NUM))
	{
		return false;
	}
	bool flag = true;
	byte basic_record[12];
	// 1-2: номер команды
	basic_record[0] = ntag_page[0];
	basic_record[1] = ntag_page[1];
	//3-6: время инициализации
	basic_record[2] = ntag_page[4];
	basic_record[3] = ntag_page[5];
	basic_record[4] = ntag_page[6];
	basic_record[5] = ntag_page[7];
	//7-8: маска команды
	basic_record[6] = ntag_page[8];
	basic_record[7] = ntag_page[9];
	//9-12: время последней отметки на станции
	basic_record[8] = (checkTime & 0xFF000000) >> 24;
	basic_record[9] = (checkTime & 0x00FF0000) >> 16;
	basic_record[10] = (checkTime & 0x0000FF00) >> 8;
	basic_record[11] = checkTime & 0x000000FF;
	flag &= SPIflash.writeByteArray(teamFlashAddress, basic_record, 12);

#ifdef DEBUG
	Serial.println(F("!!!basics wrote"));
#endif

	// copy card content to flash. все страницы не начинающиеся с 0
	uint16_t checkCount = 0;
	uint8_t block = 0;
	while (block < TAG_MAX_PAGE)
	{
#ifdef DEBUG
		Serial.print(F("!!!reading page #"));
		Serial.println(String(block));
#endif
		if (!ntagRead4pages(block))
		{
			flag = false;
			break;
		}
		else
		{
			//4 pages in one read block
			for (byte i = 0; i < 4; i++)
			{
				if (block < 8 || ntag_page[i * 4]>0)
				{
					flag = true;
					flag &= SPIflash.writeByteArray(teamFlashAddress + 16 + (uint32_t)block * (uint32_t)4 + (uint32_t)i * (uint32_t)4 + (uint32_t)0, &ntag_page[0 + i * 4], 4);
					//flag &= SPIflash.writeByte(teamFlashAddress + 16 + (uint32_t)block * (uint32_t)4 + (uint32_t)i * (uint32_t)4 + (uint32_t)0, ntag_page[0 + i * 4]);
					//flag &= SPIflash.writeByte(teamFlashAddress + 16 + (uint32_t)block * (uint32_t)4 + (uint32_t)i * (uint32_t)4 + (uint32_t)1, ntag_page[1 + i * 4]);
					//flag &= SPIflash.writeByte(teamFlashAddress + 16 + (uint32_t)block * (uint32_t)4 + (uint32_t)i * (uint32_t)4 + (uint32_t)2, ntag_page[2 + i * 4]);
					//flag &= SPIflash.writeByte(teamFlashAddress + 16 + (uint32_t)block * (uint32_t)4 + (uint32_t)i * (uint32_t)4 + (uint32_t)3, ntag_page[3 + i * 4]);
					checkCount++;
#ifdef DEBUG
					Serial.print(F("!!!write: "));
					for (uint8_t i = 0; i < 16; i++)Serial.print(String(ntag_page[i], HEX) + " ");
					Serial.println();
#endif
				}
				else
				{
#ifdef DEBUG
					Serial.print(F("!!!chip last block: "));
					Serial.println(String(block));
#endif
					block = TAG_MAX_PAGE;
					break;
				}
			}
		}
		block += 4;
	}
	// add dump pages number
	if (checkCount > 0)
	{
		checkCount = checkCount * 4 + 16;
		//SPIflash.writeShort(teamFlashAddress + 12, checkCount);
		SPIflash.writeByte(teamFlashAddress + 12, checkCount >> 8);
		SPIflash.writeByte(teamFlashAddress + 13, checkCount & 0x00FF);
	}
	return flag;
}

// сохраняем весь блок, стираем весь блок и возвращаем назад все, кроме переписываемой команды
bool eraseTeamFromFlash(uint16_t teamNumber)
{
	bool flag = true;
	uint32_t tmpBufferStart = (maxTeamNumber + 1) * TEAM_FLASH_SIZE;
	uint32_t blockFlashAddress = (uint32_t)(teamNumber * TEAM_FLASH_SIZE / FLASH_BLOCK_SIZE);
	blockFlashAddress *= (uint32_t)FLASH_BLOCK_SIZE;
	uint8_t teamInBlock = ((uint32_t)(uint32_t(teamNumber) * (uint32_t)TEAM_FLASH_SIZE) - blockFlashAddress) / TEAM_FLASH_SIZE;
#ifdef DEBUG
	Serial.print(F("!!!tmpBufferStart="));
	Serial.println(String(tmpBufferStart));
	Serial.print(F("!!!blockFlashAddress="));
	Serial.println(String(blockFlashAddress));
	Serial.print(F("!!!teamInBlock="));
	Serial.println(String(teamInBlock));
#endif

	// erase sector
	flag &= SPIflash.eraseSector(tmpBufferStart);
	byte b;
	// backup Flash Block
	for (uint32_t i = 0; i < FLASH_BLOCK_SIZE; i++)
	{
		// не копировать перезаписываемую запись
		if (i < teamInBlock * TEAM_FLASH_SIZE || i >= (teamInBlock + 1) * TEAM_FLASH_SIZE)
		{
			b = SPIflash.readByte(blockFlashAddress + i);
			flag &= SPIflash.writeByte(tmpBufferStart + i, b);
		}
	}

	// erase sector
	flag &= SPIflash.eraseSector(blockFlashAddress);

	// restore Flash Block
	for (uint32_t i = 0; i < FLASH_BLOCK_SIZE; i++)
	{
		// не копировать перезаписываемую запись
		//!!!! if (i < teamInBlock * TEAM_FLASH_SIZE && i >= (teamInBlock + 1) * TEAM_FLASH_SIZE)
		{
			b = SPIflash.readByte(tmpBufferStart + i);
			if (b != 0xff) flag &= SPIflash.writeByte(blockFlashAddress + i, b);
		}
	}

	return flag;
}

// получаем сведения о команде из лога
bool readTeamFromFlash(uint16_t recordNum)
{
	uint32_t addr = (uint32_t)((uint32_t)recordNum * (uint32_t)TEAM_FLASH_SIZE);
	if (SPIflash.readByte(addr) == 0xff) return false;
	// #команды
	// время инициализации
	// маска
	// время отметки
	// счетчик страниц на чипе
	for (uint8_t i = 0; i < 14; i++)
	{
		ntag_page[i] = SPIflash.readByte(addr + (uint32_t)i);
	}
	return true;
}

// подсчет записанных в флэш отметок
uint16_t refreshChipCounter()
{
#ifdef DEBUG
	Serial.print(F("!!!team records found="));
#endif

	uint16_t chips = 0;

	uint32_t addr;
	for (uint16_t i = 1; i <= maxTeamNumber; i++)
	{
		addr = (uint32_t)((uint32_t)i * (uint32_t)TEAM_FLASH_SIZE);
		if (SPIflash.readByte(addr) != 255)
		{
			chips++;
			uint32_t time = SPIflash.readByte(addr + 8);
			time <<= 8;
			time += SPIflash.readByte(addr + 9);
			time <<= 8;
			time += SPIflash.readByte(addr + 10);
			time <<= 8;
			time += SPIflash.readByte(addr + 11);
			if (time > lastTimeChecked)
			{
				lastTimeChecked = time;
				lastTeams[0] = SPIflash.readByte(addr);
				lastTeams[1] = SPIflash.readByte(addr + 1);
			}
#ifdef DEBUG
			Serial.print(String(i));
			Serial.print(F(", "));
#endif
		}
	}
#ifdef DEBUG
	Serial.println();
	Serial.print(F("!!!checked chip counter="));
	Serial.println(String(chips));
#endif
	return chips;
}

// обработка ошибок. формирование пакета с сообщением о ошибке
void sendError(uint8_t errorCode, uint8_t commandCode)
{
	init_package(commandCode);
	uartBuffer[DATA_START_BYTE] = errorCode;
	uartBufferPosition = DATA_START_BYTE + 1;
	sendData();
}

// добавляем номер в буфер последних команд
void addLastTeam(uint16_t number)
{
	// фильтровать дубли
	if (lastTeams[0] == (byte)(number >> 8) && lastTeams[1] == (byte)number) return;

	for (uint8_t i = lastTeamsLength * 2 - 1; i > 1; i = i - 2)
	{
		lastTeams[i] = lastTeams[i - 2];
		lastTeams[i - 1] = lastTeams[i - 3];
	}
	lastTeams[0] = (byte)(number >> 8);
	lastTeams[1] = (byte)number;

}

uint8_t crcCalc(uint8_t* dataArray, uint16_t startPosition, uint16_t dataEnd)
{
	uint8_t crc = 0x00;
	uint16_t i = startPosition;
	while (i <= dataEnd)
	{
		uint8_t tmpByte = dataArray[i];
		for (uint8_t tempI = 8; tempI; tempI--)
		{
			uint8_t sum = (crc ^ tmpByte) & 0x01;
			crc >>= 1;
			if (sum)
			{
				crc ^= 0x8C;
			}
			tmpByte >>= 1;
		}
		i++;
	}
	return (crc);
}

void floatToByte(byte* bytes, float f)
{
	int length = sizeof(float);

	for (int i = 0; i < length; i++)
	{
		bytes[i] = ((byte*)& f)[i];
	}
}

bool selectChipType(byte type)
{
	if (chipType == 0x12)
	{
		chipType = type;
		NTAG_MARK = 213;
		TAG_MAX_PAGE = 40;
	}
	else if (chipType == 0x6d)
	{
		chipType = type;
		NTAG_MARK = 216;
		TAG_MAX_PAGE = 226;
	}
	else
	{
		chipType = 0x3e;
		NTAG_MARK = 215;
		TAG_MAX_PAGE = 130;
		if (chipType != 0x3e) return false;
	}
	return true;
}
