#include <Wire.h>
#include "ds3231.h"
#include <SPI.h>
#include <MFRC522.h>
#include <EEPROM.h>
#include <SPIFlash.h>

//#define DEBUG

#define UART_SPEED 9600

#define FW_VERSION						104 //версия прошивки, номер пишется в чипы
#define LED_PIN							4 //светодиод
#define BUZZER_PIN						3 //пищалка
#define RTC_ENABLE_PIN					5 //питание часов кроме батарейного
#define RFID_RST_PIN					9 //рфид модуль reset
#define RFID_SS_PIN						10 //рфид модуль chip_select
#define FLASH_ENABLE_PIN				7 //SPI enable pin
#define FLASH_SS_PIN					8 //SPI SELECT pin
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
#define COMMAND_GET_STATION_CLONES	0x87
#define COMMAND_UPDATE_TEAM_MASK	0x88
#define COMMAND_WRITE_MASTER_CHIP	0x89

//размеры данных для команд
#define DATA_LENGTH_SET_MODE			3
#define DATA_LENGTH_SET_TIME			7
#define DATA_LENGTH_RESET_STATION		9
#define DATA_LENGTH_GET_STATUS			1
#define DATA_LENGTH_INIT_CHIP			15
#define DATA_LENGTH_GET_LAST_TEAM		1
#define DATA_LENGTH_GET_CHIP_HISTORY	5
#define DATA_LENGTH_GET_STATION_CLONES	1
#define DATA_LENGTH_UPDATE_TEAM_MASK	11
#define DATA_LENGTH_WRITE_MASTER_CHIP	16

//ответы станции
#define REPLY_SET_MODE				0x90
#define REPLY_SET_TIME				0x91
#define REPLY_RESET_STATION			0x92
#define REPLY_GET_STATUS			0x93
#define REPLY_INIT_CHIP				0x94
#define REPLY_GET_LAST_TEAM			0x95
#define REPLY_GET_CHIP_HISTORY		0x96
#define REPLY_GET_STATION_CLONES	0x97
#define REPLY_UPDATE_TEAM_MASK		0x98
#define REPLY_WRITE_MASTER_CHIP		0x99

//режимы станции
#define MODE_INIT	0
#define MODE_KP		1

//коды ошибок станции
#define OK				0
#define WRONG_STATION	1
#define READ_ERROR		2
#define WRITE_ERROR		3
#define LOW_INIT_TIME	4
#define WRONG_CHIP		5
#define NO_CHIP			6
#define BUFFER_OVERFLOW	7
#define INCORRECT_DATA	8

//страницы в чипе. 0-7 служебные, 8-127 для отметок

//#define PAGE_UID	0
#define PAGE_INIT	4 //номер чипа+режим и тип чипа+версия
#define PAGE_PASS	5 //
#define PAGE_INIT1	6 
#define PAGE_INIT2	7
//#define PAGE_DATA	8

//параметры чипов
#define ntagValue	130 //максимальное число страниц на чипе
#define ntagType	215 //режим работы и тип чипа
#define maxPage		127 //?????

//описание протокола
#define STATION_NUMBER_BYTE	3
#define LENGTH_BYTE			5
#define COMMAND_BYTE		4
#define DATA_START_BYTE		6

#define receiveTimeOut 1000

//станция запоминает последнюю отметку сюда
byte lastChip[16];
unsigned int totalChipsChecked = 0; // количество отмеченных чипов в памяти.
unsigned long lastTimeChecked = 0;

//по умолчанию номер станции и режим.
byte stationMode = MODE_INIT;
byte stationNumber = 0;
const unsigned long maxTimeInit = 600000UL; //одна неделя

byte dump[16];
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

void setup()
{
	Serial.begin(UART_SPEED);

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
	if (c == MODE_INIT)
	{
		stationMode = MODE_INIT;
	}
	else if (c == MODE_KP)
	{
		stationMode = MODE_KP;
	}
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
		Serial.println(F("receive timeout"));
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
		executeCommand();
		uartReady = false;
	}

	//если режим КП то отметить чип автоматом
	if (!receivingData && stationMode != 0 && stationNumber != 0)
	{
		processRfidCard();
	}
}

//Command processing

//поиск функции
void executeCommand()
{
#ifdef DEBUG
	Serial.print(F("Command:"));
	Serial.println(String(uartIncomingMessageData[COMMAND_BYTE], HEX));
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
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_GET_LAST_TEAM) getLastTeams();
		else errorLengthFlag = true;
		break;
	case COMMAND_GET_CHIP_HISTORY:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_GET_CHIP_HISTORY) getChipHistory();
		else errorLengthFlag = true;
		break;
	case COMMAND_GET_STATION_CLONES:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_GET_STATION_CLONES) getStationClones();
		else errorLengthFlag = true;
		break;
	case COMMAND_UPDATE_TEAM_MASK:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_UPDATE_TEAM_MASK) updateTeamMask();
		else errorLengthFlag = true;
		break;
	case COMMAND_WRITE_MASTER_CHIP:
		if (uartBuffer[LENGTH_BYTE] == DATA_LENGTH_WRITE_MASTER_CHIP) writeMasterChip();
		else errorLengthFlag = true;
		break;
	}
#ifdef DEBUG
	if (errorLengthFlag) Serial.println(F("Incorrect data length"));
#endif
}

//установка режима
void setMode()
{
	//Если номер станции не совпадает с присланным в пакете, то режим не меняется
	if (stationNumber != uartBuffer[DATA_START_BYTE + 1])
	{
		sendError(WRONG_STATION, REPLY_SET_MODE);
		return;
	}

	stationMode = uartBuffer[DATA_START_BYTE];
	eepromwrite(EEPROM_STATION_MODE_ADDRESS, stationMode);

	//формирование пакета данных.
	init_package(REPLY_SET_MODE);
	addData(OK);
	sendData();
}

//обновление времени на станции
void setTime()
{
	systemTime.year = uartBuffer[DATA_START_BYTE] + 2000;
	systemTime.mon = uartBuffer[DATA_START_BYTE + 1];
	systemTime.mday = uartBuffer[DATA_START_BYTE + 2];
	systemTime.hour = uartBuffer[DATA_START_BYTE + 3];
	systemTime.min = uartBuffer[DATA_START_BYTE + 4];
	systemTime.sec = uartBuffer[DATA_START_BYTE + 5];

	delay(1);
	DS3231_set(systemTime); //correct time

	DS3231_get(&systemTime);
	unsigned long tmpTime = systemTime.unixtime;

	init_package(REPLY_SET_TIME);
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
	//сброс произойдет только в случае совпадения номера станции в пакете
	if (stationNumber != uartBuffer[DATA_START_BYTE + 1])
	{
		sendError(WRONG_STATION, REPLY_RESET_STATION);
		return;
	}

	//проверить количество отметок
	unsigned int checkCardNumber = uartBuffer[DATA_START_BYTE + 2];
	checkCardNumber <<= 8;
	checkCardNumber += uartBuffer[DATA_START_BYTE + 3];
	if (checkCardNumber != totalChipsChecked)
	{
		sendError(INCORRECT_DATA, REPLY_RESET_STATION);
		return;
	}

	//проверить время последней отметки
	unsigned long checkLastTime = uartBuffer[DATA_START_BYTE + 4];
	checkLastTime <<= 8;
	checkLastTime += uartBuffer[DATA_START_BYTE + 5];
	checkLastTime <<= 8;
	checkLastTime += uartBuffer[DATA_START_BYTE + 6];
	checkLastTime <<= 8;
	checkLastTime += uartBuffer[DATA_START_BYTE + 7];
	if (checkLastTime != lastTimeChecked)
	{
		sendError(INCORRECT_DATA, REPLY_RESET_STATION);
		return;
	}

	stationMode = 0;
	eepromwrite(EEPROM_STATION_MODE_ADDRESS, stationMode);
	stationNumber = uartBuffer[DATA_START_BYTE];
	eepromwrite(EEPROM_STATION_NUMBER_ADDRESS, stationNumber);

	SPIflash.eraseChip();

	init_package(REPLY_RESET_STATION);
	addData(OK);
	sendData();
}

// выдает статус: время на станции, номер станции, номер режима, число отметок, время последней страницы
void getStatus()
{
	DS3231_get(&systemTime);
	unsigned long tmpTime = systemTime.unixtime;

	init_package(REPLY_GET_STATUS);
	addData(OK);
	addData((tmpTime & 0xFF000000) >> 24);
	addData((tmpTime & 0x00FF0000) >> 16);
	addData((tmpTime & 0x0000FF00) >> 8);
	addData(tmpTime & 0x000000FF);

	addData(stationMode);

	addData(stationNumber);

	addData((totalChipsChecked & 0xFF00) >> 8);
	addData(totalChipsChecked & 0x00FF);

	addData((lastTimeChecked & 0xFF000000) >> 24);
	addData((lastTimeChecked & 0x00FF0000) >> 16);
	addData((lastTimeChecked & 0x0000FF00) >> 8);
	addData(lastTimeChecked & 0x000000FF);

	/*unsigned int batteryLevel = getBatteryLevel();
	addData((batteryLevel & 0xFF00) >> 8);
	addData(batteryLevel & 0x00FF);
	addData(FW_VERSION);
	*/

	sendData();
}

//инициализация чипа
void initChip()
{
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

	if (!ntagRead(PAGE_PASS))
	{
		sendError(READ_ERROR, REPLY_INIT_CHIP);
		return;
	}

	unsigned long initTime = dump[0];
	initTime <<= 8;
	initTime += dump[1];
	initTime <<= 8;
	initTime += dump[2];
	initTime <<= 8;
	initTime += dump[3];

	DS3231_get(&systemTime);

	//инициализация сработает только если время инициализации записанное уже на чипе превышает неделю с нанешнего времени
	if ((systemTime.unixtime - initTime) < maxTimeInit)
	{
		sendError(LOW_INIT_TIME, REPLY_INIT_CHIP);
		return;
	}

	//заполняем карту 0xFF
	byte Wbuff[] = { 255,255,255,255 };
	for (byte page = PAGE_INIT; page < ntagValue; page++)
	{
		if (!ntagWrite(Wbuff, page))
		{
			sendError(WRITE_ERROR, REPLY_INIT_CHIP);
			return;
		}
	}

	//заполняем карту 0x00
	for (byte i = 0; i < 4; i++) Wbuff[i] = 0;
	for (byte page = PAGE_INIT; page < ntagValue; page++)
	{
		if (!ntagWrite(Wbuff, page))
		{
			sendError(WRITE_ERROR, REPLY_INIT_CHIP);
			return;
		}
	}

	//пишем данные на карту
	//номер команды, тип чипа, версия прошивки станции
	byte dataBlock5[] = { uartBuffer[DATA_START_BYTE], uartBuffer[DATA_START_BYTE + 1], ntagType, FW_VERSION };
	if (!ntagWrite(dataBlock5, PAGE_INIT))
	{
		sendError(WRITE_ERROR, REPLY_INIT_CHIP);
		return;
	}

	//текущее время
	unsigned long tmpTime = systemTime.unixtime;
	byte t1 = (tmpTime & 0xFF000000) >> 24;
	byte t2 = (tmpTime & 0x00FF0000) >> 16;
	byte t3 = (tmpTime & 0x0000FF00) >> 8;
	byte t4 = tmpTime & 0x000000FF;
	byte dataBlock2[] = { t1, t2, t3, t4 };
	if (!ntagWrite(dataBlock2, PAGE_PASS))
	{
		sendError(WRITE_ERROR, REPLY_INIT_CHIP);
		return;
	}

	//маска участников
	byte dataBlock3[] = { uartBuffer[DATA_START_BYTE + 6], uartBuffer[DATA_START_BYTE + 7], uartBuffer[DATA_START_BYTE + 8], uartBuffer[DATA_START_BYTE + 9] };
	if (!ntagWrite(dataBlock3, PAGE_INIT1))
	{
		sendError(WRITE_ERROR, REPLY_INIT_CHIP);
		return;
	}

	//резерв
	byte dataBlock4[] = { uartBuffer[DATA_START_BYTE + 10], uartBuffer[DATA_START_BYTE + 11], uartBuffer[DATA_START_BYTE + 12], uartBuffer[DATA_START_BYTE + 13] };
	if (!ntagWrite(dataBlock4, PAGE_INIT2))
	{
		sendError(WRITE_ERROR, REPLY_INIT_CHIP);
		return;
	}

	//получаем UID карты
	if (!ntagRead(0))
	{
		sendError(READ_ERROR, REPLY_INIT_CHIP);
		return;
	}
	SPI.end();

	init_package(REPLY_INIT_CHIP);
	addData(OK);
	for (byte h = 0; h < 7; h++)
	{
		addData(dump[h]);
	}
	sendData();
}

void getLastTeams()
{
	init_package(REPLY_GET_LAST_TEAM);
	addData(OK);

	for (byte r = 0; r < 14; r++)
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
	init_package(REPLY_GET_CHIP_HISTORY);
	addData(OK);
	unsigned int commandNumberFrom = uartBuffer[DATA_START_BYTE];
	commandNumberFrom <<= 8;
	commandNumberFrom += uartBuffer[DATA_START_BYTE + 1];
	unsigned int commandNumberTo = uartBuffer[DATA_START_BYTE];
	commandNumberTo <<= 8;
	commandNumberTo += uartBuffer[DATA_START_BYTE + 1];

	for (int chipN = commandNumberFrom; chipN < commandNumberTo; chipN++)
	{
		unsigned long shortF = chipN * 20;

		if (SPIflash.readByte(shortF) != 255)
		{
			unsigned long markTime = SPIflash.readByte(shortF + 16);
			markTime <<= 8;
			markTime += SPIflash.readByte(shortF + 17);
			markTime <<= 8;
			markTime += SPIflash.readByte(shortF + 18);
			markTime <<= 8;
			markTime += SPIflash.readByte(shortF + 19);

			unsigned long pageF = chipN;
			pageF = pageF * 1000 + 100000UL;

			addData(SPIflash.readByte(pageF + 4 * 4));
			addData(SPIflash.readByte(pageF + 4 * 4 + 1));

			byte startTime = SPIflash.readByte(pageF + 5 * 4);
			byte startTime2 = SPIflash.readByte(pageF + 5 * 4 + 1);
			for (byte gh = 24; gh < 32; gh++)
			{
				addData(SPIflash.readByte(pageF + gh));
			}
			for (byte p = 8; p < 130; p++)
			{
				byte st = SPIflash.readByte(pageF + p * 4);
				if (st != 0 && st != 255)
				{
					addData(st);
					byte t2 = SPIflash.readByte(pageF + p * 4 + 1);
					if (t2 < startTime2) addData(startTime + 1);
					else addData(startTime);
					addData(t2);
					addData(SPIflash.readByte(pageF + p * 4 + 2));
					addData(SPIflash.readByte(pageF + p * 4 + 3));
				}
			}
		}
	}
	sendData();
}

//????? снимаем дамп с карты, на которой записан лог другой станции????? разобраться и отрефакторить
void getStationClones()
{
	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522
	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		sendError(NO_CHIP, REPLY_GET_STATION_CLONES);
		return;
	}
	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		sendError(NO_CHIP, REPLY_GET_STATION_CLONES);
		return;
	}

	if (!ntagRead(PAGE_INIT))
	{
		sendError(READ_ERROR, REPLY_GET_STATION_CLONES);
		return;
	}

	init_package(REPLY_GET_STATION_CLONES);
	addData(dump[0]);

	for (byte page = 5; page < ntagValue; page++)
	{
		if (!ntagRead(page))
		{
			sendError(READ_ERROR, REPLY_GET_STATION_CLONES);
			return;
		}

		for (byte i = 0; i < 4; i++)
		{
			for (byte y = 0; y < 8; y++)
			{
				byte temp = dump[i];
				temp = temp >> y;
				if (temp % 2 == 1)
				{
					int num = (page - 5) * 32 + i * 8 + y;
					byte first = (num & 0xFF00) >> 8;
					byte second = num & 0x00FF;
					addData(first);
					addData(second);
				}
			}
		}
	}

	addData(OK);
	sendData();
}

void updateTeamMask()
{
	//if (stationMode == 1) return;

	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522

   // Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		sendError(NO_CHIP, REPLY_UPDATE_TEAM_MASK);
		return;
	}

	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		sendError(NO_CHIP, REPLY_UPDATE_TEAM_MASK);
		return;
	}

	if (!ntagRead(PAGE_INIT))
	{
		sendError(READ_ERROR, REPLY_UPDATE_TEAM_MASK);
		return;
	}

	if (dump[0] == uartBuffer[DATA_START_BYTE] && dump[1] == uartBuffer[DATA_START_BYTE + 1])
	{
		byte dataBlock[4] = { uartBuffer[DATA_START_BYTE + 2], uartBuffer[DATA_START_BYTE + 3], uartBuffer[DATA_START_BYTE + 4], uartBuffer[DATA_START_BYTE + 5] };
		if (!ntagWrite(dataBlock, PAGE_INIT1))
		{
			sendError(WRITE_ERROR, REPLY_UPDATE_TEAM_MASK);
			return;
		}

		byte dataBlock2[] = { uartBuffer[DATA_START_BYTE + 6], uartBuffer[DATA_START_BYTE + 7], uartBuffer[DATA_START_BYTE + 8], uartBuffer[DATA_START_BYTE + 9] };
		if (!ntagWrite(dataBlock2, PAGE_INIT2))
		{
			sendError(WRITE_ERROR, REPLY_UPDATE_TEAM_MASK);
			return;
		}
	}
	else
	{
		sendError(WRONG_CHIP, REPLY_UPDATE_TEAM_MASK);
		return;
	}
	SPI.end();

	init_package(REPLY_UPDATE_TEAM_MASK);
	addData(OK);
	sendData();
}

void writeMasterChip()
{
	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522

	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		sendError(NO_CHIP, REPLY_WRITE_MASTER_CHIP);
		return;
	}

	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		sendError(NO_CHIP, REPLY_WRITE_MASTER_CHIP);
		return;
	}

	byte pass0 = uartBuffer[DATA_START_BYTE + 11];
	byte pass1 = uartBuffer[DATA_START_BYTE + 12];
	byte pass2 = uartBuffer[DATA_START_BYTE + 13];
	byte setting = uartBuffer[DATA_START_BYTE + 14];



	if (uartBuffer[DATA_START_BYTE] == 253)
	{
		byte Wbuff[] = { 255, 255, 255, 255 };

		for (byte page = PAGE_INIT; page < ntagValue; page++)
		{
			if (!ntagWrite(Wbuff, page))
			{
				sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
				return;
			}
		}

		byte Wbuff2[] = { 0,0,0,0 };

		for (byte page = PAGE_INIT; page < ntagValue; page++)
		{
			if (!ntagWrite(Wbuff2, page))
			{
				sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
				return;
			}
		}
		byte dataBlock[4] = { 0, 253, 255, FW_VERSION };
		if (!ntagWrite(dataBlock, PAGE_INIT))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}


		byte dataBlock2[] = { pass0, pass1, pass2, 0 };
		if (!ntagWrite(dataBlock2, PAGE_PASS))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}
	}

	if (uartBuffer[DATA_START_BYTE] == 252)
	{
		byte dataBlock[4] = { 0, 252, 255, FW_VERSION };
		if (!ntagWrite(dataBlock, PAGE_INIT))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}

		byte dataBlock2[] = { pass0, pass1, pass2, 0 };
		if (!ntagWrite(dataBlock2, PAGE_PASS))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}
	}

	if (uartBuffer[DATA_START_BYTE] == 251)
	{
		byte dataBlock[4] = { 0, 251, 255, FW_VERSION };
		if (!ntagWrite(dataBlock, PAGE_INIT))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}

		byte dataBlock2[] = { pass0, pass1, pass2,0 };
		if (!ntagWrite(dataBlock2, PAGE_PASS))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}

		byte dataBlock3[] = { uartBuffer[DATA_START_BYTE + 7], 0, 0, 0 };
		if (!ntagWrite(dataBlock3, PAGE_INIT1))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}
	}

	if (uartBuffer[DATA_START_BYTE] == 254)
	{
		byte dataBlock[4] = { 0, 254, 255, FW_VERSION };
		if (!ntagWrite(dataBlock, PAGE_INIT))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}

		byte dataBlock2[] = { uartBuffer[DATA_START_BYTE + 8], uartBuffer[DATA_START_BYTE + 9], uartBuffer[DATA_START_BYTE + 10], 0 };
		if (!ntagWrite(dataBlock2, PAGE_INIT1))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}

		byte dataBlock3[] = { pass0, pass1, pass2, setting };
		if (!ntagWrite(dataBlock3, PAGE_PASS))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}
	}

	if (uartBuffer[DATA_START_BYTE] == 250)
	{
		byte dataBlock[4] = { 0, 250, 255, FW_VERSION };
		if (!ntagWrite(dataBlock, PAGE_INIT))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}

		byte dataBlock2[] = { pass0, pass1, pass2, 0 };
		if (!ntagWrite(dataBlock2, PAGE_PASS))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}

		byte dataBlock3[] = { uartBuffer[DATA_START_BYTE + 2], uartBuffer[DATA_START_BYTE + 1], uartBuffer[DATA_START_BYTE + 3], 0 };
		if (!ntagWrite(dataBlock3, PAGE_INIT1))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}

		byte dataBlock4[] = { uartBuffer[DATA_START_BYTE + 4], uartBuffer[DATA_START_BYTE + 5], uartBuffer[DATA_START_BYTE + 6], 0 };
		if (!ntagWrite(dataBlock4, PAGE_INIT2))
		{
			sendError(WRITE_ERROR, REPLY_WRITE_MASTER_CHIP);
			return;
		}
	}

	init_package(REPLY_WRITE_MASTER_CHIP);
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
	uartBuffer[0] = uartBuffer[1] = uartBuffer[2] = uartBuffer[3] = 0xFE;
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
	uartBuffer[LENGTH_BYTE] = uartBufferPosition - COMMAND_BYTE;
	addData(crcCalc(uartBuffer, COMMAND_BYTE, uartBuffer[LENGTH_BYTE]));
#ifdef DEBUG
	Serial.print(F("Sending:"));
	for (int i = 0; i < uartIncomingMessagePosition; i++)
	{
		Serial.print(F(" "));
		if (uartIncomingMessageData[i] < 0x10) Serial.print(F("0"));
		Serial.print(String(uartIncomingMessageData[i], HEX));
	}
	Serial.println();
#endif
	Serial.write(uartBuffer, uartBufferPosition);
}

//MFRC522::StatusCode MFRC522::MIFARE_Read
bool ntagWrite(byte *dataBlock, byte pageAdr)
{
	const byte sizePageNtag = 4;
	status = (MFRC522::StatusCode) mfrc522.MIFARE_Ultralight_Write(pageAdr, dataBlock, sizePageNtag);
	if (status != MFRC522::STATUS_OK)
	{
		return false;
	}

	byte buffer[18];
	byte size = sizeof(buffer);

	status = (MFRC522::StatusCode) mfrc522.MIFARE_Read(pageAdr, buffer, &size);
	if (status != MFRC522::STATUS_OK)
	{
		return false;
	}

	for (byte i = 0; i < 4; i++)
	{
		if (buffer[i] != dataBlock[i]) return false;
	}

	return true;
}

bool ntagRead(byte pageAdr)
{
	byte buffer[18];
	byte size = sizeof(buffer);

	status = (MFRC522::StatusCode) mfrc522.MIFARE_Read(pageAdr, buffer, &size);
	if (status != MFRC522::STATUS_OK)
	{
		return false;
	}

	for (byte i = 0; i < 16; i++)
	{
		dump[i] = buffer[i];
	}
	return true;
}

//Writing the chip
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
		return;
	}

	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		return;
	}
#ifdef DEBUG
	Serial.println(F("card found"));
#endif

	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	//читаем блок информации
	if (!ntagRead(PAGE_INIT))
	{
#ifdef DEBUG
		Serial.println(F("can't read card"));
#endif
		return;
	}

	//Заменяем маску, хранящуюся в буфере при совпадении маски
	if (tempNumTeam0 != 0 || tempNumTeam1 != 0)
	{
		if (tempNumTeam0 == dump[0] && tempNumTeam1 == dump[1])
		{
			bool updateMask = false;

			for (int i = 0; i < 8; i++)
			{
				if (tempTeamMask[i] != dump[i + 9]) updateMask = true;
			}

			if (updateMask)
			{
				byte dataBlock[4] = { tempTeamMask[0],tempTeamMask[1],tempTeamMask[2],tempTeamMask[3] };
				if (!ntagWrite(dataBlock, PAGE_INIT1))
				{
					//sendError(WRITE_ERROR, REPLY_UPDATE_TEAM_MASK);
					return;
				}

				byte dataBlock2[] = { tempTeamMask[4],tempTeamMask[5],tempTeamMask[6],tempTeamMask[7] };
				if (!ntagWrite(dataBlock2, PAGE_INIT2))
				{
					//sendError(WRITE_ERROR, REPLY_UPDATE_TEAM_MASK);
					return;
				}
			}
		}
		else
		{
			//sendError(WRONG_CHIP, REPLY_UPDATE_TEAM_MASK);
			return;
		}
		tempNumTeam0 = 0;
		tempNumTeam1 = 0;

		for (byte i = 0; i < 8; i++)
		{
			tempTeamMask[i] = 0;
		}



		/*init_package(REPLY_UPDATE_TEAM_MASK);
		addData(OK);
		addData((tempT & 0xFF000000) >> 24);
		addData((tempT & 0x00FF0000) >> 16);
		addData((tempT & 0x0000FF00) >> 8);
		addData(tempT & 0x000000FF);
		sendData();*/
	}

	//в первых трех байтах находятся нули для обычных чипов и заданные числа для мастер-чипов
	byte info = 0;
	if (dump[2] == 255)
	{
		return;
	}

	unsigned long timeInit = dump[4];
	timeInit = timeInit << 8;
	timeInit += dump[5];
	timeInit = timeInit << 8;
	timeInit += dump[6];
	timeInit = timeInit << 8;
	timeInit += dump[7];

	if ((systemTime.unixtime - timeInit) > maxTimeInit)
	{
		return;
	}

	tempDump[0] = 0;

	chipNum = (dump[0] << 8) + dump[1];
	if (chipNum == 0) return;

	newPage = findNewPage(128);

	if (newPage == 0) return;

	if (stationNumber == tempDump[0] && stationNumber != 245)
	{
		beep(500, 1);
		return;
	}

	if (newPage > maxPage)
	{
		return;
	}

	if (!writeTimeToCard(newPage))
	{
		return;
	}

	writeFlash(chipNum);
	totalChipsChecked++;
	beep(200, 1);

	SPI.end();
} // end of rfid()

bool writeTimeToCard(int newPage)
{
	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	byte dataBlock2[4] = { 0, 0, 0, 0 };
	dataBlock2[0] = stationNumber;
	dataBlock2[1] = (tempT & 0x00FF0000) >> 16;
	dataBlock2[2] = (tempT & 0x0000FF00) >> 8;
	dataBlock2[3] = (tempT & 0x000000FF);

	if (ntagWrite(dataBlock2, newPage))
	{
		return true;
	}
	else
	{
		return false;
	}
}

byte findNewPage(byte finishpage)
{
	byte startpage = 8;
	byte page = (finishpage + startpage) / 2;

	while (1)
	{
		if (finishpage == startpage)
		{
			return (finishpage);
		}

		page = (finishpage + startpage) / 2;

		if (!ntagRead(page))
		{
			for (byte i = 0; i < 4; i++) tempDump[i] = 0;
			return 0;
		}

		if (dump[0] == 0)
		{
			finishpage = (finishpage - startpage) / 2 + startpage;
		}
		else
		{
			for (byte i = 0; i < 4; i++) tempDump[i] = dump[i];
			startpage = finishpage - (finishpage - startpage) / 2;
		}
	}
}

void writeFlash(int chipNum)
{
	DS3231_get(&systemTime);
	lastTimeChecked = systemTime.unixtime;
	unsigned long pageFlash = chipNum * 1000 + 100000UL;
	unsigned long shortFlash = chipNum;
	shortFlash = shortFlash * 20;
	for (byte page = 0; page < maxPage; page = page + 4)
	{
		if (!ntagRead(page))
		{
			return;
		}

		for (byte dop = 0; dop < 16; dop++)
		{
			if (dump[(dop / 4) * 4] != 0 || page == 4)
			{
				SPIflash.writeByte(pageFlash + page * 4 + dop, dump[dop]);
			}
		}

		if (page == 4)
		{
			for (byte i = 0; i < 16; i++)
			{
				SPIflash.writeByte(shortFlash + i, dump[i]);
				lastChip[i] = dump[i];
			}


			byte byteTime[4];
			byteTime[0] = (lastTimeChecked & 0xFF000000) >> 24;
			byteTime[1] = (lastTimeChecked & 0x00FF0000) >> 16;
			byteTime[2] = (lastTimeChecked & 0x0000FF00) >> 8;
			byteTime[3] = (lastTimeChecked & 0x000000FF);

			SPIflash.writeByte(shortFlash + 16, byteTime[0]);
			SPIflash.writeByte(shortFlash + 17, byteTime[1]);
			SPIflash.writeByte(shortFlash + 18, byteTime[2]);
			SPIflash.writeByte(shortFlash + 19, byteTime[3]);
		}
	}
}

int refreshChipCounter()
{
	int chips = 0;
	for (int i = 0; i < 20000; i += 20)
	{
		if (SPIflash.readByte(i) != 255) chips++;
	}
#ifdef DEBUG
	Serial.print(F("chip counter="));
	Serial.println(String(chips));
#endif
	return chips;
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
			Serial.println(F("read error"));
#endif
			uartBufferPosition = 0;
			receivingData = false;
			return false;
		}
		//0 byte = FE
		else if (uartBufferPosition == 0 && c == 0xfe)
		{
#ifdef DEBUG
			Serial.print(F("byte0="));
			if (c < 0x10) Serial.print(F("0"));
			Serial.println(String(byte(c), HEX));
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
			Serial.print(F("byte1"));
			if (c < 0x10) Serial.print(F("0"));
			Serial.println(String(byte(c), HEX));
#endif
			uartBuffer[uartBufferPosition] = (byte)c;
			uartBufferPosition++;
		}
		//2nd byte = FE
		else if (uartBufferPosition == 2 && c == 0xfe)
		{
#ifdef DEBUG
			Serial.print(F("byte2"));
			if (c < 0x10) Serial.print(F("0"));
			Serial.println(String(byte(c), HEX));
#endif
			uartBuffer[uartBufferPosition] = (byte)c;
			uartBufferPosition++;
		}
		//3rd byte = FE
		else if (uartBufferPosition == 3 && c == 0xfe)
		{
#ifdef DEBUG
			Serial.print(F("byte3"));
			if (c < 0x10) Serial.print(F("0"));
			Serial.println(String(byte(c), HEX));
#endif
			uartBuffer[uartBufferPosition] = (byte)c;
			uartBufferPosition++;
		}
		//4th byte = command, length and data
		else if (uartBufferPosition >= COMMAND_BYTE)
		{
			uartBuffer[uartBufferPosition] = (byte)c;
#ifdef DEBUG
			Serial.print(F("byte"));
			Serial.print(String(uartIncomingMessagePosition));
			Serial.print(F("="));
			if (c < 0x10) Serial.print(F("0"));
			Serial.println(String(byte(c), HEX));
#endif
			//incorrect length
			if (uartBufferPosition == LENGTH_BYTE && uartBuffer[LENGTH_BYTE] > (255 - DATA_START_BYTE - 1))
			{
#ifdef DEBUG
				Serial.println(F("incorrect length"));
#endif
				uartBufferPosition = 0;
				receivingData = false;
				return false;
			}

			//packet is received
			if (uartBufferPosition >= DATA_START_BYTE + uartBuffer[LENGTH_BYTE])
			{
				//crc matching
#ifdef DEBUG
				Serial.print(F("received packet expected CRC="));
				Serial.println(String(crcCalc(uartIncomingMessageData, COMMAND_BYTE, uartIncomingMessageData[LENGTH_BYTE]), HEX));
#endif
				if (uartBuffer[uartBufferPosition] == crcCalc(uartBuffer, COMMAND_BYTE, uartBuffer[LENGTH_BYTE])) // CRC not correct
				{
#ifdef DEBUG
					Serial.print(F("Command received:"));
					for (int i = 0; i <= uartIncomingMessagePosition; i++)
					{
						Serial.print(F(" "));
						if (uartIncomingMessageData[i] < 0x10) Serial.print(F("0"));
						Serial.print(String(uartIncomingMessageData[i], HEX));
					}
					Serial.println();
#endif
					uartBufferPosition = 0;
					receivingData = false;
					return true;
				}
				else
				{
#ifdef DEBUG
					Serial.println(F("incorrect crc"));
#endif
					uartBufferPosition = 0;
					receivingData = false;
					return false;
				}
			}
			uartBufferPosition++;
		}
		else
		{
#ifdef DEBUG
			Serial.println(F("unexpected byte"));
#endif
			uartBufferPosition = 0;
		}
	}
	return false;
}

byte crcCalc(byte* dataArray, int startPosition, int dataLength)
{
	byte crc = 0x00;
	int i = startPosition;
	while (i < dataLength)
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
