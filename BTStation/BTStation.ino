#define DEBUG

#include <Wire.h>
#include "ds3231.h"
#include <SPI.h>
#include <MFRC522.h>
#include <EEPROM.h>
#include <SPIFlash.h>

#ifdef DEBUG
//#include <NeoSWSerial.h>
#include <SoftwareSerial.h>
#endif


#define UART_SPEED 9600

#define FW_VERSION						104 //версия прошивки, номер пишется в чипы

#ifdef DEBUG
#define DEBUG_RX						2 //
#define DEBUG_TX						6 //
#endif

#define BUZZER_PIN						3 //пищалка
#define LED_PIN							4 //светодиод
#define RTC_ENABLE_PIN					5 //питание часов кроме батарейного

#define FLASH_ENABLE_PIN				7 //SPI enable pin
#define FLASH_SS_PIN					8 //SPI SELECT pin
#define RFID_RST_PIN					9 //рфид модуль reset
#define RFID_SS_PIN						10 //рфид модуль chip_select
//#define RFID_MOSI_PIN					11 //рфид модуль chip_select
//#define RFID_MISO_PIN					12 //рфид модуль chip_select
//#define RFID_SCK_PIN					13 //рфид модуль chip_select
#define BATTERY_PIN						A0 //замер напряжения батареи

#define EEPROM_STATION_NUMBER_ADDRESS	800 //номер станции в eeprom памяти
#define EEPROM_STATION_MODE_ADDRESS		860 //номер режима в eeprom памяти

//команды
#define COMMAND_SET_MODE			0x80
#define COMMAND_SET_TIME			0x81
#define COMMAND_RESET_STATION		0x82
#define COMMAND_GET_STATUS			0x83
#define COMMAND_INIT_CHIP			0x84
#define COMMAND_GET_LAST_TEAM		0x85
#define COMMAND_GET_CHIP_HISTORY	0x86
#define COMMAND_READ_CARD_PAGE		0x87
#define COMMAND_UPDATE_TEAM_MASK	0x88
#define COMMAND_WRITE_CARD_PAGE		0x89

//размеры данных для команд
#define DATA_LENGTH_SET_MODE			1
#define DATA_LENGTH_SET_TIME			6
#define DATA_LENGTH_RESET_STATION		7
#define DATA_LENGTH_GET_STATUS			0
#define DATA_LENGTH_INIT_CHIP			4
#define DATA_LENGTH_GET_LAST_TEAM		0
#define DATA_LENGTH_GET_CHIP_HISTORY	4
#define DATA_LENGTH_READ_CARD_PAGE		2
#define DATA_LENGTH_UPDATE_TEAM_MASK	8
#define DATA_LENGTH_WRITE_CARD_PAGE		13

//ответы станции
#define REPLY_SET_MODE				0x90
#define REPLY_SET_TIME				0x91
#define REPLY_RESET_STATION			0x92
#define REPLY_GET_STATUS			0x93
#define REPLY_INIT_CHIP				0x94
#define REPLY_GET_LAST_TEAM			0x95
#define REPLY_GET_CHIP_HISTORY		0x96
#define REPLY_READ_CARD_PAGE		0x97
#define REPLY_UPDATE_TEAM_MASK		0x98
#define REPLY_WRITE_CARD_PAGE		0x99

//режимы станции
#define MODE_INIT		0
#define MODE_START_KP	1
#define MODE_FINISH_KP	2

//коды ошибок станции
#define OK				0
#define WRONG_STATION	1
#define READ_ERROR		2
#define WRITE_ERROR		3
#define LOW_INIT_TIME	4
#define WRONG_CHIP		5
#define NO_CHIP			6
#define BUFFER_OVERFLOW	7
#define WRONG_DATA		8
#define WRONG_UID		9
#define WRONG_TEAM		10
#define NO_DATA			11

//страницы в чипе. 0-7 служебные, 8-127 для отметок
#define PAGE_UID		0
#define PAGE_CHIP_NUM	4 //номер_чипа + тип_чипа + версия_прошивки
#define PAGE_INIT_TIME	5 //время инициализации
#define PAGE_RESERVED1	6
#define PAGE_RESERVED2	7
#define PAGE_DATA_START	8

//параметры чипов
//#define NTAG213_MAX_PAGE	40 //максимальное число страниц на чипе
#define NTAG215_MAX_PAGE	130 //максимальное число страниц на чипе
//#define NTAG216_MAX_PAGE	226 //максимальное число страниц на чипе

#define NTAG_TYPE			215 //режим работы и тип чипа
//#define maxPage		127 //?????

//описание протокола
#define STATION_NUMBER_BYTE	3
#define LENGTH_BYTE			4
#define COMMAND_BYTE		5
#define DATA_START_BYTE		6

#define receiveTimeOut 1000

//станция запоминает последнюю отметку сюда
byte lastChip[16];
unsigned long lastTimeChecked = 0;
unsigned int totalChipsChecked = 0; // количество отмеченных чипов в памяти.
bool lastTeamEnable = false; //читалось ли что-нибудь с момента включения


//по умолчанию номер станции и режим.
byte stationMode = MODE_INIT;
byte stationNumber = 0;
const unsigned long maxTimeInit = 600000UL; //одна неделя

byte ntag_page[16];
byte tempDump[4] = { 255, 0, 0, 0 };

//параметры для хранения маски в режиме 1.
byte tempTeamMask[8];
byte tempNumTeam0;
byte tempNumTeam1;

SPIFlash SPIflash(FLASH_SS_PIN); //флэш-память

//рфид-модуль
MFRC522::StatusCode status;
MFRC522 mfrc522(RFID_SS_PIN, RFID_RST_PIN); // Create MFRC522 instance

//хранение времени
struct ts systemTime; //time

//UART command buffer
byte uartBuffer[256];
byte uartBufferPosition = 0;
bool uartReady = false;
unsigned long uartTimeout = 1000;
bool receivingData = false;
unsigned long receiveStartTime = 0;

#ifdef DEBUG
SoftwareSerial DebugSerial(DEBUG_RX, DEBUG_TX);
#endif


void setup()
{
	Serial.begin(UART_SPEED);
#ifdef DEBUG
	DebugSerial.begin(9600);
#endif

	analogReference(INTERNAL);

	pinMode(LED_PIN, OUTPUT);
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
	stationNumber = eepromread(EEPROM_STATION_NUMBER_ADDRESS); //Read the station number from the EEPROM
	if (stationNumber == 255 || stationNumber == -1)
	{
		stationNumber = 0;
	}

	//читаем номер режима из памяти
	int c = eepromread(EEPROM_STATION_MODE_ADDRESS);
	if (c == -1)
	{
		beep(200, 200);
	}
	if (c == MODE_START_KP) stationMode = MODE_START_KP;
	else if (c == MODE_FINISH_KP) stationMode = MODE_FINISH_KP;
	else stationMode = MODE_INIT;

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
		DebugSerial.println(F("receive timeout"));
#endif
		uartBufferPosition = 0;
		uartReady = false;
		receivingData = false;
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
	if (!receivingData && stationMode != 0)
	{
		processRfidCard();
	}
}

//Command processing

//поиск функции
void executeCommand()
{
#ifdef DEBUG
	DebugSerial.print(F("Command:"));
	DebugSerial.println(String(uartBuffer[COMMAND_BYTE], HEX));
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
	case COMMAND_GET_LAST_TEAM:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_GET_LAST_TEAM) getLastTeam();
		else errorLengthFlag = true;
		break;
	case COMMAND_GET_CHIP_HISTORY:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_GET_CHIP_HISTORY) getChipHistory();
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
	}
#ifdef DEBUG
	if (errorLengthFlag) DebugSerial.println(F("Incorrect data length"));
#endif
}

//установка режима
void setMode()
{
	//Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_SET_MODE);
		return;
	}

	//0: новый номер режима
	stationMode = uartBuffer[DATA_START_BYTE];
	eepromwrite(EEPROM_STATION_MODE_ADDRESS, stationMode);

	//формирование пакета данных.
	init_package(REPLY_SET_MODE);

	//0: код ошибки
	addData(OK);
	sendData();
}

//обновление времени на станции
void setTime()
{
	//Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_SET_MODE);
		return;
	}

	//0-5: дата и время [yy.mm.dd hh:mm:ss]
	/*int ss= uartBuffer[DATA_START_BYTE + 5] - 4;
	int mm= uartBuffer[DATA_START_BYTE + 4] + 8;
	int hh=uartBuffer[DATA_START_BYTE + 3];
	int dd= uartBuffer[DATA_START_BYTE + 2];
	int mon= uartBuffer[DATA_START_BYTE + 1];
	int yy= uartBuffer[DATA_START_BYTE] + 2000;
	if (ss < 0)
	{
		mm--;
		ss = 60 - ss;
	}
	if (mm > 59)
	{
		hh++;
		mm = mm-60;
	}*/

	systemTime.year = uartBuffer[DATA_START_BYTE] + 2000;
	systemTime.mon = uartBuffer[DATA_START_BYTE + 1];
	systemTime.mday = uartBuffer[DATA_START_BYTE + 2];
	systemTime.hour = uartBuffer[DATA_START_BYTE + 3];
	systemTime.min = uartBuffer[DATA_START_BYTE + 4];
	systemTime.sec = uartBuffer[DATA_START_BYTE + 5];

#ifdef DEBUG
	DebugSerial.print(F("Time: "));
	for (int i = 0; i < 6; i++) DebugSerial.println(String(uartBuffer[DATA_START_BYTE + i]) + " ");
#endif

	//0-3: дата и время в unixtime
	//systemTime.unixtime = uartBuffer[DATA_START_BYTE] * 16777216 + uartBuffer[DATA_START_BYTE] * 65536 + uartBuffer[DATA_START_BYTE] * 256 + uartBuffer[DATA_START_BYTE];

	delay(1);
	DS3231_set(systemTime); //correct time

	DS3231_get(&systemTime);
	unsigned long tmpTime = systemTime.unixtime;// +488;

	init_package(REPLY_SET_TIME);

	//0: код ошибки
	//1-4: текущее время
	addData(OK);
	addData((tmpTime & 0xFF000000) >> 24);
	addData((tmpTime & 0x00FF0000) >> 16);
	addData((tmpTime & 0x0000FF00) >> 8);
	addData(tmpTime & 0x000000FF);
	sendData();
}

//сброс настроек станции
void resetStation()
{
	//Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_RESET_STATION);
		return;
	}

	//0-1: кол-во отмеченных карт (для сверки)
	//2-5: время последней отметки(для сверки)
	//6 : новый номер станции
	//проверить количество отметок
	unsigned int checkCardNumber = uartBuffer[DATA_START_BYTE];
	checkCardNumber <<= 8;
	checkCardNumber += uartBuffer[DATA_START_BYTE + 1];
	if (checkCardNumber != totalChipsChecked)
	{
		sendError(WRONG_DATA, REPLY_RESET_STATION);
		return;
	}

	//проверить время последней отметки
	unsigned long checkLastTime = uartBuffer[DATA_START_BYTE + 2];
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
	eepromwrite(EEPROM_STATION_MODE_ADDRESS, stationMode);
	stationNumber = uartBuffer[DATA_START_BYTE + 6];
	eepromwrite(EEPROM_STATION_NUMBER_ADDRESS, stationNumber);

	SPIflash.eraseChip();

	lastTimeChecked = 0;
	totalChipsChecked = 0;
	lastTeamEnable = false;


	init_package(REPLY_RESET_STATION);

	//0: код ошибки
	addData(OK);
	sendData();
}

// выдает статус: время на станции, номер станции, номер режима, число отметок, время последней страницы
void getStatus()
{
	DS3231_get(&systemTime);
	unsigned long tmpTime = systemTime.unixtime;

	//0: код ошибки
	//1: версия прошивки
	//2: номер станции
	//3: номер режима
	//4-7 : текущее время
	//8-9 : количество отметок на станции
	//10-13 : время последней отметки на станции
	//14-15 : напряжение батареи в условных единицах[0..1023] ~[0..1.1В]
	//16-17 : температура
	init_package(REPLY_GET_STATUS);
	addData(OK);
	addData(FW_VERSION);
	addData(stationNumber);
	addData(stationMode);

	addData((tmpTime & 0xFF000000) >> 24);
	addData((tmpTime & 0x00FF0000) >> 16);
	addData((tmpTime & 0x0000FF00) >> 8);
	addData(tmpTime & 0x000000FF);

	addData((totalChipsChecked & 0xFF00) >> 8);
	addData(totalChipsChecked & 0x00FF);

	addData((lastTimeChecked & 0xFF000000) >> 24);
	addData((lastTimeChecked & 0x00FF0000) >> 16);
	addData((lastTimeChecked & 0x0000FF00) >> 8);
	addData(lastTimeChecked & 0x000000FF);

	unsigned int batteryLevel = getBatteryLevel();
	addData((batteryLevel & 0xFF00) >> 8);
	addData(batteryLevel & 0x00FF);

	int temperature = (int)DS3231_get_treg();
	addData((temperature & 0xFF00) >> 8);
	addData(temperature & 0x00FF);

	sendData();
}

//проверить!!!
//инициализация чипа
void initChip()
{
	//Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_INIT_CHIP);
		return;
	}

	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522

   // Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		sendError(NO_CHIP, REPLY_INIT_CHIP);
		return;
	}
	// Select one of the cards
	else if (!mfrc522.PICC_ReadCardSerial())
	{
		sendError(NO_CHIP, REPLY_INIT_CHIP);
		return;
	}

	//проверить!!!
	//инициализация сработает только если время инициализации записанное уже на чипе превышает неделю с текущего времени
	/*if (!ntagRead4pages(PAGE_INIT_TIME))
	{
		sendError(READ_ERROR, REPLY_INIT_CHIP);
		SPI.end();
		return;
	}
	unsigned long initTime = ntag_page[0];
	initTime <<= 8;
	initTime += ntag_page[1];
	initTime <<= 8;
	initTime += ntag_page[2];
	initTime <<= 8;
	initTime += ntag_page[3];
	DS3231_get(&systemTime);
	if ((systemTime.unixtime - initTime) < maxTimeInit)
	{
		sendError(LOW_INIT_TIME, REPLY_INIT_CHIP);
		SPI.end();
		return;
	}*/

	//заполняем карту 0xFF
	byte dataBlock[4] = { 255,255,255,255 };
	/*for (byte page = PAGE_CHIP_NUM; page < NTAG215_MAX_PAGE; page++)
	{
		if (!ntagWritePage(dataBlock, page))
		{
			sendError(WRITE_ERROR, REPLY_INIT_CHIP);
			SPI.end();
			return;
		}
	}*/

	//заполняем карту 0x00
	for (byte i = 0; i < 4; i++) dataBlock[i] = 0;
	for (byte page = PAGE_CHIP_NUM; page < NTAG215_MAX_PAGE; page++)
	{
		if (!ntagWritePage(dataBlock, page))
		{
			sendError(WRITE_ERROR, REPLY_INIT_CHIP);
			SPI.end();
			return;
		}
	}

	//0-1: номер команды
	//2-3 : маска участников
	//пишем данные на карту
	//номер команды, тип чипа, версия прошивки станции
	dataBlock[0] = uartBuffer[DATA_START_BYTE];
	dataBlock[1] = uartBuffer[DATA_START_BYTE + 1];
	dataBlock[2] = NTAG_TYPE;
	dataBlock[3] = FW_VERSION;
	if (!ntagWritePage(dataBlock, PAGE_CHIP_NUM))
	{
		sendError(WRITE_ERROR, REPLY_INIT_CHIP);
		SPI.end();
		return;
	}

	//текущее время
	unsigned long tmpTime = systemTime.unixtime;
	dataBlock[0] = (tmpTime & 0xFF000000) >> 24;
	dataBlock[1] = (tmpTime & 0x00FF0000) >> 16;
	dataBlock[2] = (tmpTime & 0x0000FF00) >> 8;
	dataBlock[3] = tmpTime & 0x000000FF;
	if (!ntagWritePage(dataBlock, PAGE_INIT_TIME))
	{
		sendError(WRITE_ERROR, REPLY_INIT_CHIP);
		SPI.end();
		return;
	}

	//маска участников
	dataBlock[0] = uartBuffer[DATA_START_BYTE + 2];
	dataBlock[1] = uartBuffer[DATA_START_BYTE + 3];
	dataBlock[2] = 0;
	dataBlock[3] = 0;
	if (!ntagWritePage(dataBlock, PAGE_RESERVED1))
	{
		sendError(WRITE_ERROR, REPLY_INIT_CHIP);
		SPI.end();
		return;
	}

	//получаем UID карты
/*	if (!ntagRead4pages(PAGE_UID))
	{
		sendError(READ_ERROR, REPLY_INIT_CHIP);
		SPI.end();
		return;
	}
	SPI.end();*/

	init_package(REPLY_INIT_CHIP);
	addData(OK);
/*	for (byte i = 0; i <= 7; i++)
	{
		addData(ntag_page[i]);
	}*/
	addData((tmpTime & 0xFF000000) >> 24);
	addData((tmpTime & 0x00FF0000) >> 16);
	addData((tmpTime & 0x0000FF00) >> 8);
	addData(tmpTime & 0x000000FF);
	sendData();
}

//получить последнюю отметившуюся команду
void getLastTeam()
{
	if (!lastTeamEnable)
	{
		sendError(NO_DATA, REPLY_GET_LAST_TEAM);
		return;
	}

	//Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_GET_LAST_TEAM);
		return;
	}

	init_package(REPLY_GET_LAST_TEAM);

	//0: код ошибки
	//1-2: номер чипа
	//3-6 : время инициализации
	//7-14 : информация из страниц 6 - 7
	//15-18 : время последней отметки на станции
	addData(OK);

	addData(lastChip[0]);
	addData(lastChip[1]);
	for (byte r = 4; r < 16; r++)
	{
		addData(lastChip[r]);
	}

	addData((lastTimeChecked & 0xFF000000) >> 24);
	addData((lastTimeChecked & 0x00FF0000) >> 16);
	addData((lastTimeChecked & 0x0000FF00) >> 8);
	addData(lastTimeChecked & 0x000000FF);

	sendData();
}

//разобраться и отрефакторить
void getChipHistory()
{
	//Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_GET_CHIP_HISTORY);
		return;
	}

	//0-1: с какой записи
	//2-3: по какую запись включительно
	unsigned int commandNumberFrom = uartBuffer[DATA_START_BYTE];
	commandNumberFrom <<= 8;
	commandNumberFrom += uartBuffer[DATA_START_BYTE + 1];
	unsigned int commandNumberTo = uartBuffer[DATA_START_BYTE + 2];
	commandNumberTo <<= 8;
	commandNumberTo += uartBuffer[DATA_START_BYTE + 3];

	init_package(REPLY_GET_CHIP_HISTORY);
	addData(OK);
	//: код ошибки
	//1-2: номер команды
	//3-4 : маска участников
	//5-8 : время отметки команды

	for (unsigned int chipN = commandNumberFrom; chipN <= commandNumberTo; chipN++)
	{
		if (readTeamFromFlash(chipN))
		{
			for (int i = 0; i < 8; i++)
			{
				addData(ntag_page[i]);
			}
		}
		else
		{
			sendError(READ_ERROR, REPLY_GET_CHIP_HISTORY);
			return;
		}
	}
	sendData();
}

//снимаем постраничный дамп с карты. разобраться и отрефакторить
void readCardPages()
{
	//Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_READ_CARD_PAGE);
		return;
	}

	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522
	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		sendError(NO_CHIP, REPLY_READ_CARD_PAGE);
		SPI.end();
		return;
	}
	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		sendError(NO_CHIP, REPLY_READ_CARD_PAGE);
		SPI.end();
		return;
	}

	byte pageFrom = uartBuffer[DATA_START_BYTE];
	byte pageTo = uartBuffer[DATA_START_BYTE + 1];

	init_package(REPLY_READ_CARD_PAGE);

	//0: код ошибки
	//1-8: UID чипа
	//9-12: данные из страницы карты(4 байта)
	addData(OK);

	if (!ntagRead4pages(PAGE_UID))
	{
		sendError(READ_ERROR, REPLY_READ_CARD_PAGE);
		SPI.end();
		return;
	}
	for (int i = 0; i <= 7; i++) addData(ntag_page[i]);

	while (pageFrom <= pageTo)
	{
		//0: какую страницу карты
		if (!ntagRead4pages(pageFrom))
		{
			sendError(READ_ERROR, REPLY_READ_CARD_PAGE);
			SPI.end();
			return;
		}
		int n = (pageTo - pageFrom + 1);
		if (n > 4) n = 4;
		for (int i = 0; i < n; i++)
		{
			addData(pageFrom);
			for (int j = 0; j < 4; j++) addData(ntag_page[i * 4 + j]);
			pageFrom++;
		}
	}
	SPI.end();

	sendData();
}

//Обновить маску команды
void updateTeamMask()
{
	//Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_UPDATE_TEAM_MASK);
		return;
	}

	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522

	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		sendError(NO_CHIP, REPLY_UPDATE_TEAM_MASK);
		SPI.end();
		return;
	}

	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		sendError(NO_CHIP, REPLY_UPDATE_TEAM_MASK);
		SPI.end();
		return;
	}

	//0-1: номер команды
	//2-5: время выдачи чипа
	//6-7: маска участников

	//проверить номер команды
	if (!ntagRead4pages(PAGE_CHIP_NUM))
	{
		sendError(READ_ERROR, REPLY_UPDATE_TEAM_MASK);
		SPI.end();
		return;
	}
	if (ntag_page[0] != uartBuffer[DATA_START_BYTE] || ntag_page[1] != uartBuffer[DATA_START_BYTE + 1])
	{
		sendError(WRONG_TEAM, REPLY_UPDATE_TEAM_MASK);
		SPI.end();
		return;
	}

	//проверить время выдачи чипа
	if (!ntagRead4pages(PAGE_INIT_TIME))
	{
		sendError(READ_ERROR, REPLY_UPDATE_TEAM_MASK);
		SPI.end();
		return;
	}
	if (ntag_page[0] != uartBuffer[DATA_START_BYTE + 2] || ntag_page[1] != uartBuffer[DATA_START_BYTE + 3] || ntag_page[2] != uartBuffer[DATA_START_BYTE + 4] || ntag_page[3] != uartBuffer[DATA_START_BYTE + 5])
	{
#ifdef DEBUG
		DebugSerial.println(F("chip init time wrong"));
		DebugSerial.print(String(ntag_page[0], HEX));
		DebugSerial.print(F("="));
		DebugSerial.println(String(uartBuffer[DATA_START_BYTE + 2], HEX));

		DebugSerial.print(F("upd mask chip error"));
		DebugSerial.print(String(ntag_page[1], HEX));
		DebugSerial.print(F("="));
		DebugSerial.println(String(uartBuffer[DATA_START_BYTE + 3], HEX));

		DebugSerial.print(F("upd mask chip error"));
		DebugSerial.print(String(ntag_page[2], HEX));
		DebugSerial.print(F("="));
		DebugSerial.println(String(uartBuffer[DATA_START_BYTE + 4], HEX));

		DebugSerial.print(F("upd mask chip error"));
		DebugSerial.print(String(ntag_page[3], HEX));
		DebugSerial.print(F("="));
		DebugSerial.println(String(uartBuffer[DATA_START_BYTE + 5], HEX));
#endif
		sendError(WRONG_CHIP, REPLY_UPDATE_TEAM_MASK);
		SPI.end();
		return;
	}

	//записать страницу на карту
	byte dataBlock[4] = { uartBuffer[DATA_START_BYTE + 6], uartBuffer[DATA_START_BYTE + 7], 0,0 };
	if (!ntagWritePage(dataBlock, PAGE_RESERVED1))
	{
		sendError(WRITE_ERROR, REPLY_UPDATE_TEAM_MASK);
		SPI.end();
		return;
	}
	SPI.end();

	init_package(REPLY_UPDATE_TEAM_MASK);

	//0: код ошибки
	addData(OK);
	sendData();
}

//пишем присланные с ББ 4 байта в указанную страницу
void writeCardPage()
{
	//Если номер станции не совпадает с присланным в пакете, то отказ
	if (stationNumber != uartBuffer[STATION_NUMBER_BYTE])
	{
		sendError(WRONG_STATION, REPLY_WRITE_CARD_PAGE);
		return;
	}

	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522

	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		sendError(NO_CHIP, REPLY_WRITE_CARD_PAGE);
		SPI.end();
		return;
	}

	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		sendError(NO_CHIP, REPLY_WRITE_CARD_PAGE);
		SPI.end();
		return;
	}

	//0-7: UID чипа
	//8: номер страницы
	//9-12: данные для записи (4 байта)

	//проверить UID
	if (!ntagRead4pages(PAGE_UID))
	{
		sendError(READ_ERROR, REPLY_READ_CARD_PAGE);
		SPI.end();
		return;
	}
	bool flag = false;
	for (int i = 0; i <= 7; i++)
	{
		if (ntag_page[i] != uartBuffer[DATA_START_BYTE + i])
		{
			flag = true;
			break;
		}
	}
	if (flag)
	{
		sendError(WRONG_UID, REPLY_READ_CARD_PAGE);
		SPI.end();
		return;
	}

	//записать страницу
	byte dataBlock[] =
	{
		uartBuffer[DATA_START_BYTE + 9],
		uartBuffer[DATA_START_BYTE + 10],
		uartBuffer[DATA_START_BYTE + 11],
		uartBuffer[DATA_START_BYTE + 12]
	};
	if (!ntagWritePage(dataBlock, uartBuffer[DATA_START_BYTE + 8]))
	{
		sendError(WRITE_ERROR, REPLY_WRITE_CARD_PAGE);
		SPI.end();
		return;
	}
	SPI.end();

	init_package(REPLY_WRITE_CARD_PAGE);

	//0: код ошибки
	addData(OK);
	sendData();
}



// Internal functions

//чтение заряда батареи
unsigned int getBatteryLevel()
{
	unsigned int AverageValue = 0;
	int MeasurementsToAverage = 16;
	for (int i = 0; i < MeasurementsToAverage; ++i)
	{
		AverageValue += analogRead(BATTERY_PIN);
		delay(1);
	}
	return AverageValue / MeasurementsToAverage;
}

//запись в память с мажоритальным резервированием
void eepromwrite(int adr, byte val)
{
	for (byte i = 0; i < 3; i++)
	{
		EEPROM.write(adr + i, val);
	}
}

//считывание из памяти с учетом мажоритального резервирования
int eepromread(int adr)
{
	int byte1 = EEPROM.read(adr);
	int byte2 = EEPROM.read(adr + 1);
	int byte3 = EEPROM.read(adr + 2);

	// возвращаем при совпадении два из трех
	if (byte1 == byte2 && byte1 == byte3)
	{
		return byte1;
	}
	else if (byte1 == byte2)
	{
		return byte1;
	}
	else if (byte1 == byte3)
	{
		return byte1;
	}
	else if (byte2 == byte3)
	{
		return byte2;
	}
	else
	{
		return -1;
	}

}

//сигнал станции, длительность в мс и число повторений
void beep(int ms, byte n)
{
	for (byte i = 0; i < n; i++)
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

//инициализация пакета данных
void init_package(byte command)
{
	uartBuffer[0] = uartBuffer[1] = uartBuffer[2] = 0xFE;
	uartBuffer[3] = stationNumber;
	uartBuffer[COMMAND_BYTE] = command;
	uartBufferPosition = DATA_START_BYTE;
}

//добавление данных в буфер
void addData(byte data)
{
	if (uartBufferPosition > 254)
	{
		sendError(BUFFER_OVERFLOW, uartBuffer[COMMAND_BYTE]);
		return;
	}
	uartBuffer[uartBufferPosition] = data;
	uartBufferPosition++;
}

//передача пакета данных.
void sendData()
{
	uartBuffer[LENGTH_BYTE] = uartBufferPosition - COMMAND_BYTE - 1;
	addData(crcCalc(uartBuffer, STATION_NUMBER_BYTE, uartBufferPosition - 1));
#ifdef DEBUG
	DebugSerial.print(F("Sending:"));
	for (int i = 0; i < uartBufferPosition; i++)
	{
		DebugSerial.print(F(" "));
		if (uartBuffer[i] < 0x10) DebugSerial.print(F("0"));
		DebugSerial.print(String(uartBuffer[i], HEX));
	}
	DebugSerial.println();
#endif
	Serial.write(uartBuffer, uartBufferPosition);
	uartBufferPosition = 0;
}

//запись страницы (4 байта) в чип
bool ntagWritePage(byte *dataBlock, byte pageAdr)
{
	/*SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522
	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		sendError(NO_CHIP, REPLY_READ_CARD_PAGE);
		SPI.end();
		return false;
	}
	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		sendError(NO_CHIP, REPLY_READ_CARD_PAGE);
		SPI.end();
		return false;
	}*/

	const byte sizePageNtag = 4;
	status = (MFRC522::StatusCode) mfrc522.MIFARE_Ultralight_Write(pageAdr, dataBlock, sizePageNtag);
	if (status != MFRC522::STATUS_OK)
	{
		//SPI.end();
		return false;
	}

	byte buffer[18];
	byte size = sizeof(buffer);

	status = (MFRC522::StatusCode) mfrc522.MIFARE_Read(pageAdr, buffer, &size);
	if (status != MFRC522::STATUS_OK)
	{
		//SPI.end();
		return false;
	}
	//SPI.end();

	for (byte i = 0; i < 4; i++)
	{
		if (buffer[i] != dataBlock[i]) return false;
	}

	return true;
}

//чтение 4-х страниц (16 байт) из чипа
bool ntagRead4pages(byte pageAdr)
{
	/*SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522
	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		sendError(NO_CHIP, REPLY_READ_CARD_PAGE);
		SPI.end();
		return false;
	}
	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		sendError(NO_CHIP, REPLY_READ_CARD_PAGE);
		SPI.end();
		return false;
	}*/
#ifdef DEBUG
	DebugSerial.println(F("card found"));
#endif
	byte buffer[18];
	byte size = sizeof(buffer);

	status = (MFRC522::StatusCode) mfrc522.MIFARE_Read(pageAdr, buffer, &size);
	if (status != MFRC522::STATUS_OK)
	{
		//SPI.end();
		return false;
	}
	//SPI.end();

	for (byte i = 0; i < 16; i++)
	{
		ntag_page[i] = buffer[i];
	}
	return true;
}

//Обработка поднесенного чипа
void processRfidCard()
{
	//инициализируем переменные
	byte lastNum = 0; //last station number written in memory of chip
	byte newPage = 0;
	int chipNum = 0; //number of chip from 1-st block

	//включаем SPI ищем карту вблизи. Если не находим выходим из функции чтения чипов
	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();    // Init MFRC522

	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		SPI.end();
		return;
	}

	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		SPI.end();
		return;
	}


	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	//читаем блок информации
	if (!ntagRead4pages(PAGE_CHIP_NUM))
	{
#ifdef DEBUG
		DebugSerial.println(F("can't read card"));
#endif
		SPI.end();
		return;
	}

	//Заменяем маску, хранящуюся в буфере при совпадении маски
	/*if (tempNumTeam0 != 0 || tempNumTeam1 != 0)
	{
		if (tempNumTeam0 == ntag_page[0] && tempNumTeam1 == ntag_page[1])
		{
			bool updateMask = false;

			for (int i = 0; i < 8; i++)
			{
				if (tempTeamMask[i] != ntag_page[i + 9]) updateMask = true;
			}

			if (updateMask)
			{
				byte dataBlock[4] = { tempTeamMask[0],tempTeamMask[1],tempTeamMask[2],tempTeamMask[3] };
				if (!ntagWritePage(dataBlock, PAGE_RESERVED1))
				{
					//sendError(WRITE_ERROR, REPLY_UPDATE_TEAM_MASK);
					SPI.end();
					return;
				}

				byte dataBlock2[] = { tempTeamMask[4],tempTeamMask[5],tempTeamMask[6],tempTeamMask[7] };
				if (!ntagWritePage(dataBlock2, PAGE_RESERVED2))
				{
					//sendError(WRITE_ERROR, REPLY_UPDATE_TEAM_MASK);
					SPI.end();
					return;
				}
			}
		}
		else
		{
			//sendError(WRONG_CHIP, REPLY_UPDATE_TEAM_MASK);
			SPI.end();
			return;
		}
		tempNumTeam0 = 0;
		tempNumTeam1 = 0;

		for (byte i = 0; i < 8; i++)
		{
			tempTeamMask[i] = 0;
		}



		//init_package(REPLY_UPDATE_TEAM_MASK);
		//addData(OK);
		//addData((tempT & 0xFF000000) >> 24);
		//addData((tempT & 0x00FF0000) >> 16);
		//addData((tempT & 0x0000FF00) >> 8);
		//addData(tempT & 0x000000FF);
		//sendData();
	}*/

	//в первых трех байтах находятся нули для обычных чипов и заданные числа для мастер-чипов
	byte info = 0;
	if (ntag_page[2] == 255)
	{
		SPI.end();
		return;
	}

	//Не слишком ли старый чип? Недельной давности и более
	unsigned long timeInit = ntag_page[4];
	timeInit = timeInit << 8;
	timeInit += ntag_page[5];
	timeInit = timeInit << 8;
	timeInit += ntag_page[6];
	timeInit = timeInit << 8;
	timeInit += ntag_page[7];
	if ((systemTime.unixtime - timeInit) > maxTimeInit)
	{
		SPI.end();
		return;
	}


	tempDump[0] = 0;

	//Не равен ли номер чипа 0
	chipNum = (ntag_page[0] << 8) + ntag_page[1];
	if (chipNum == 0)
	{
		SPI.end();
		return;
	}

	//ищем свободную страницу на карте
	newPage = findNewPage(NTAG215_MAX_PAGE - 2);

	//ошибка чтения
	if (newPage == 0)
	{
		SPI.end();
		return;
	}

	//Не служебные ли это станции
	if (stationNumber == tempDump[0] && stationNumber != 245)
	{
		beep(500, 1);
		SPI.end();
		return;
	}

	//Номер страницы в чипе больше максимума... Наверное, переполнен???
	if (newPage > NTAG215_MAX_PAGE - 3)
	{
		SPI.end();
		return;
	}

	//Пишем на карту отметку
	if (!writeCheckPointToCard(newPage))
	{
		SPI.end();
		return;
	}
	SPI.end();

	//Пишем в лог карту
	writeFlash(chipNum);
	totalChipsChecked++;
	beep(200, 1);
}

//пишет на карту время и станцию отметки
bool writeCheckPointToCard(int newPage)
{
	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	byte dataBlock[4];
	dataBlock[0] = stationNumber;
	dataBlock[1] = (tempT & 0x00FF0000) >> 16;
	dataBlock[2] = (tempT & 0x0000FF00) >> 8;
	dataBlock[3] = (tempT & 0x000000FF);

	if (!ntagWritePage(dataBlock, newPage))
	{
		return false;
	}
	lastTeamEnable = true;
	return true;
}

//Поиск последней записанной страницы? Переписать эту черную магию...
byte findNewPage(byte finishpage)
{
	byte startpage = PAGE_DATA_START;
	byte page = (finishpage + startpage) / 2;

	while (1)
	{
		if (finishpage == startpage)
		{
			return (finishpage);
		}

		page = (finishpage + startpage) / 2;

		if (!ntagRead4pages(page))
		{
			for (byte i = 0; i < 4; i++) tempDump[i] = 0;
			return 0;
		}

		if (ntag_page[0] == 0)
		{
			finishpage = (finishpage - startpage) / 2 + startpage;
		}
		else
		{
			for (byte i = 0; i < 4; i++) tempDump[i] = ntag_page[i];
			startpage = finishpage - (finishpage - startpage) / 2;
		}
	}
}

//разобраться с адресами хранения
void writeFlash(int chipNum)
{
	DS3231_get(&systemTime);
	lastTimeChecked = systemTime.unixtime;
	//адрес хранения в каталоге дампов (всех страниц чипов)
	unsigned long pageFlash = chipNum * 1000 + 100000UL;
	//адрес хранения в каталоге отметок (4-х страниц чипов)
	unsigned long shortFlash = chipNum * 20;

	//copy card content to flash. все страницы не на чинающиеся с 0
	for (byte page = 0; page < NTAG215_MAX_PAGE - 3; page = page + 4)
	{
		if (!ntagRead4pages(page))
		{
			return;
		}


		for (byte dop = 0; dop < 16; dop++)
		{
			if (ntag_page[(dop / 4) * 4] != 0 || page == 4)
			{
				SPIflash.writeByte(pageFlash + page * 4 + dop, ntag_page[dop]);
			}
		}

		if (page == 4)
		{
			for (byte i = 0; i < 16; i++)
			{
				SPIflash.writeByte(shortFlash + i, ntag_page[i]);
				lastChip[i] = ntag_page[i];
			}

			lastTeamEnable = true;
			byte byteTime[4];
			byteTime[0] = (lastTimeChecked & 0xFF000000) >> 24;
			byteTime[1] = (lastTimeChecked & 0x00FF0000) >> 16;
			byteTime[2] = (lastTimeChecked & 0x0000FF00) >> 8;
			byteTime[3] = (lastTimeChecked & 0x000000FF);

			//SPIflash.writeByteArray(shortFlash + 16, byteTime, 4, true);
			SPIflash.writeByte(shortFlash + 16, byteTime[0]);
			SPIflash.writeByte(shortFlash + 17, byteTime[1]);
			SPIflash.writeByte(shortFlash + 18, byteTime[2]);
			SPIflash.writeByte(shortFlash + 19, byteTime[3]);
		}
	}
}

//подсчет записанных в флэш отметок
int refreshChipCounter()
{
	int chips = 0;
	for (int i = 0; i < 20000; i += 20)
	{
		if (SPIflash.readByte(i) != 255) chips++;
	}
#ifdef DEBUG
	DebugSerial.print(F("chip counter="));
	DebugSerial.println(String(chips));
#endif
	return chips;
}

bool readTeamFromFlash(unsigned int num)
{
	unsigned long addr = num * 20;
	SPIflash.readByte(addr);
	//if (SPIflash.readByte(addr) != 255)
	//{
		//#команда
	ntag_page[0] = SPIflash.readByte(addr + 0);
	ntag_page[1] = SPIflash.readByte(addr + 1);
	//маска
	ntag_page[2] = SPIflash.readByte(addr + 8);
	ntag_page[3] = SPIflash.readByte(addr + 9);
	//время отметки
	ntag_page[4] = SPIflash.readByte(addr + 16);
	ntag_page[5] = SPIflash.readByte(addr + 17);
	ntag_page[6] = SPIflash.readByte(addr + 18);
	ntag_page[7] = SPIflash.readByte(addr + 19);
	return true;
	//}
	return false;
}

//обработка ошибок. формирование пакета с сообщением о ошибке
void sendError(byte errorCode, byte commandCode)
{
	init_package(commandCode);
	addData(errorCode);
	sendData();
}

bool readUart()
{
	while (Serial.available())
	{
		int c = Serial.read();
		if (c == -1) // can't read stream
		{
#ifdef DEBUG
			DebugSerial.println(F("read error"));
#endif
			uartBufferPosition = 0;
			receivingData = false;
			beep(50, 2);
			return false;
		}
		//0 byte = FE
		else if (uartBufferPosition == 0 && c == 0xfe)
		{
#ifdef DEBUG
			DebugSerial.print(F("byte0="));
			if (c < 0x10) DebugSerial.print(F("0"));
			DebugSerial.println(String(byte(c), HEX));
#endif
			receivingData = true;
			uartBuffer[uartBufferPosition] = (byte)c;
			uartBufferPosition++;
			// refresh timeout
			receiveStartTime = millis();
		}
		//1st byte = FE
		else if (uartBufferPosition == 1 && c == 0xfe)
		{
#ifdef DEBUG
			DebugSerial.print(F("byte1"));
			if (c < 0x10) DebugSerial.print(F("0"));
			DebugSerial.println(String(byte(c), HEX));
#endif
			uartBuffer[uartBufferPosition] = (byte)c;
			uartBufferPosition++;
		}
		//2nd byte = FE
		else if (uartBufferPosition == 2 && c == 0xfe)
		{
#ifdef DEBUG
			DebugSerial.print(F("byte2"));
			if (c < 0x10) DebugSerial.print(F("0"));
			DebugSerial.println(String(byte(c), HEX));
#endif
			uartBuffer[uartBufferPosition] = (byte)c;
			uartBufferPosition++;
		}
		//4th byte = command, length and data
		else if (uartBufferPosition >= STATION_NUMBER_BYTE)
		{
			uartBuffer[uartBufferPosition] = (byte)c;
#ifdef DEBUG
			DebugSerial.print(F("byte"));
			DebugSerial.print(String(uartBufferPosition));
			DebugSerial.print(F("="));
			if (c < 0x10) DebugSerial.print(F("0"));
			DebugSerial.println(String(byte(c), HEX));
#endif
			//incorrect length
			if (uartBufferPosition == LENGTH_BYTE && uartBuffer[LENGTH_BYTE] > (254 - DATA_START_BYTE))
			{
#ifdef DEBUG
				DebugSerial.println(F("incorrect length"));
#endif
				uartBufferPosition = 0;
				receivingData = false;
				beep(50, 2);
				return false;
			}

			//packet is received
			if (uartBufferPosition >= DATA_START_BYTE + uartBuffer[LENGTH_BYTE])
			{
				//crc matching
#ifdef DEBUG
				DebugSerial.print(F("received packet expected CRC="));
				DebugSerial.println(String(crcCalc(uartBuffer, STATION_NUMBER_BYTE, uartBufferPosition - 1), HEX));
#endif
				if (uartBuffer[uartBufferPosition] == crcCalc(uartBuffer, STATION_NUMBER_BYTE, uartBufferPosition - 1))
				{
#ifdef DEBUG
					DebugSerial.print(F("Command received:"));
					for (int i = 0; i <= uartBufferPosition; i++)
					{
						DebugSerial.print(F(" "));
						if (uartBuffer[i] < 0x10) DebugSerial.print(F("0"));
						DebugSerial.print(String(uartBuffer[i], HEX));
					}
					DebugSerial.println();
#endif
					uartBufferPosition = 0;
					receivingData = false;
					return true;
				}
				else // CRC not correct
				{
#ifdef DEBUG
					DebugSerial.println(F("incorrect crc"));
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
			DebugSerial.println(F("unexpected byte"));
#endif
			uartBufferPosition = 0;
			beep(50, 3);
		}
	}
	beep(50, 2);
	return false;
}

byte crcCalc(byte* dataArray, int startPosition, int dataEnd)
{
	byte crc = 0x00;
	int i = startPosition;
	while (i <= dataEnd)
	{
		byte tmpByte = dataArray[i];
		for (byte tempI = 8; tempI; tempI--)
		{
			byte sum = (crc ^ tmpByte) & 0x01;
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
