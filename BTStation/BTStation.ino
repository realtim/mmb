#include <Wire.h>
#include "ds3231.h"
#include <SPI.h>
#include <MFRC522.h>
#include <EEPROM.h>
#include <SPIFlash.h>

//#define DEBUG

#define FW_VERSION						104 //версия прошивки, номер пишется в чипы
#define LED_PIN							4 //светодиод
#define BUZZER_PIN						3 //пищалка
#define RTC_ENABLE_PIN					5 //питание часов кроме батарейного
#define RFID_RST_PIN					9 //рфид модуль reset
#define RFID_SS_PIN						10 //рфид модуль chip_select
#define EEPROM_STATION_NUMBER_ADDRESS	800 //номер станции в eeprom памяти
#define EEPROM_STATION_MODE_ADDRESS		860 //номер режима в eeprom памяти
#define FLASH_ENABLE_PIN				7 //SPI enable pin
#define FLASH_SS_PIN					8 //SPI SELECT pin

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
#define DATA_LENGTH_SET_MODE			2
#define DATA_LENGTH_SET_TIME			6
#define DATA_LENGTH_RESET_STATION		8
#define DATA_LENGTH_GET_STATUS			0
#define DATA_LENGTH_INIT_CHIP			14
#define DATA_LENGTH_GET_LAST_TEAM		0
#define DATA_LENGTH_GET_CHIP_HISTORY	4
#define DATA_LENGTH_GET_STATION_CLONES	0
#define DATA_LENGTH_UPDATE_TEAM_MASK	10
#define DATA_LENGTH_WRITE_MASTER_CHIP	15

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
#define MODE_INIT			0
#define MODE_KP				1

//реакция на команды от станции
#define OK				0
#define WRONG_STATION	1
#define READ_ERROR		2
#define WRITE_ERROR		3
#define LOW_INIT_TIME	4
#define WRONG_CHIP		5
#define NO_CHIP			6

//страницы в чипе. 0-7 служебные, 8-127 для отметок

//#define PAGE_UID	0
#define PAGE_INIT	4
#define PAGE_PASS	5
#define PAGE_INIT1	6
#define PAGE_INIT2	7
//#define PAGE_DATA	8

//параметры чипов
#define ntagValue	130
#define ntagType	215
#define maxPage		127

//описание протокола
//#define STATION_NUMBER_BYTE	3
#define LENGTH_BYTE			5
#define COMMAND_BYTE		4
#define DATA_START_BYTE		6

#define receiveTimeOut 1000

//параметры для обработки входящих и исходящих данных
byte replyCode = 0;
byte uartIncomingMessageData[256];
unsigned int dataPosition = 2;

//станция запоминает последнюю отметку сюда
byte lastChip[16];
unsigned int totalChip = 0; // количество отмеченных чипов в памяти.
unsigned long lastTime = 0;

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
int uartIncomingMessagePosition = 0;
bool uartReady = false;
unsigned long uartTimeout = 1000;
bool receivingData = false;
unsigned long receiveStartTime = 0;

void setup()
{
	Serial.begin(9600);

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
	if (c == MODE_INIT)
	{
		stationMode = MODE_INIT;
	}
	else if (c == MODE_KP)
	{
		stationMode = MODE_KP;
	}
	else stationMode = MODE_INIT;

	totalChip = refreshChipCounter();

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
		uartIncomingMessagePosition = 0;
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
	switch (uartIncomingMessageData[COMMAND_BYTE])
	{
	case COMMAND_SET_MODE:
		if (uartIncomingMessageData[LENGTH_BYTE] == DATA_LENGTH_SET_MODE) setMode();
		else errorLengthFlag = true;
		break;
	case COMMAND_SET_TIME:
		if (uartIncomingMessageData[LENGTH_BYTE] == DATA_LENGTH_SET_TIME) setTime();
		else errorLengthFlag = true;
		break;
	case COMMAND_RESET_STATION:
		if (uartIncomingMessageData[LENGTH_BYTE] == DATA_LENGTH_RESET_STATION) resetStation();
		else errorLengthFlag = true;
		break;
	case COMMAND_GET_STATUS:
		if (uartIncomingMessageData[LENGTH_BYTE] == DATA_LENGTH_GET_STATUS) getStatus();
		else errorLengthFlag = true;
		break;
	case COMMAND_INIT_CHIP:
		if (uartIncomingMessageData[LENGTH_BYTE] == DATA_LENGTH_INIT_CHIP) initChip();
		else errorLengthFlag = true;
		break;
	case COMMAND_GET_LAST_TEAM:
		if (uartIncomingMessageData[LENGTH_BYTE] == DATA_LENGTH_GET_LAST_TEAM) getLastTeams();
		else errorLengthFlag = true;
		break;
	case COMMAND_GET_CHIP_HISTORY:
		if (uartIncomingMessageData[LENGTH_BYTE] == DATA_LENGTH_GET_CHIP_HISTORY) getChipHistory();
		else errorLengthFlag = true;
		break;
	case COMMAND_GET_STATION_CLONES:
		if (uartIncomingMessageData[LENGTH_BYTE] == DATA_LENGTH_GET_STATION_CLONES) getStationClones();
		else errorLengthFlag = true;
		break;
	case COMMAND_UPDATE_TEAM_MASK:
		if (uartIncomingMessageData[LENGTH_BYTE] == DATA_LENGTH_UPDATE_TEAM_MASK) updateTeamMask();
		else errorLengthFlag = true;
		break;
	case COMMAND_WRITE_MASTER_CHIP:
		if (uartIncomingMessageData[LENGTH_BYTE] == DATA_LENGTH_WRITE_MASTER_CHIP) writeMasterChip();
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
	replyCode = REPLY_SET_MODE;
	//Если номер станции не совпадает с присланным в пакете, то режим не меняется
	if (stationNumber != uartIncomingMessageData[DATA_START_BYTE + 1])
	{
		sendError(WRONG_STATION, replyCode);
		return;
	}

	stationMode = uartIncomingMessageData[DATA_START_BYTE];
	eepromwrite(EEPROM_STATION_MODE_ADDRESS, stationMode);

	//формирование пакета данных.
	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	init_package(replyCode);
	addData(OK);
	addData((systemTime.unixtime & 0xFF000000) >> 24);
	addData((systemTime.unixtime & 0x00FF0000) >> 16);
	addData((systemTime.unixtime & 0x0000FF00) >> 8);
	addData(systemTime.unixtime & 0x000000FF);

	sendData();
}

//обновление времени на станции
void setTime()
{
	replyCode = REPLY_SET_TIME;

	systemTime.year = uartIncomingMessageData[DATA_START_BYTE] + 2000;
	systemTime.mon = uartIncomingMessageData[DATA_START_BYTE + 1];
	systemTime.mday = uartIncomingMessageData[DATA_START_BYTE + 2];
	systemTime.hour = uartIncomingMessageData[DATA_START_BYTE + 3];
	systemTime.min = uartIncomingMessageData[DATA_START_BYTE + 4];
	systemTime.sec = uartIncomingMessageData[DATA_START_BYTE + 5];

	delay(1);
	DS3231_set(systemTime); //correct time

	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	init_package(replyCode);
	addData(OK);
	addData((systemTime.unixtime & 0xFF000000) >> 24);
	addData((systemTime.unixtime & 0x00FF0000) >> 16);
	addData((systemTime.unixtime & 0x0000FF00) >> 8);
	addData(systemTime.unixtime & 0x000000FF);

	sendData();
}

//сброс настроек станции
void resetStation()
{
	replyCode = REPLY_RESET_STATION;

	//сброс произойдет только в случае совпадения номера станции в пакете
	if (stationNumber != uartIncomingMessageData[DATA_START_BYTE + 1])
	{
		sendError(WRONG_STATION, replyCode);
		return;
	}

	//проверить количество отметок и время последней отметки

	stationMode = 0;
	eepromwrite(EEPROM_STATION_MODE_ADDRESS, stationMode);
	stationNumber = uartIncomingMessageData[DATA_START_BYTE];
	eepromwrite(EEPROM_STATION_NUMBER_ADDRESS, stationNumber);

	SPIflash.eraseChip();

	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	init_package(replyCode);
	addData(OK);
	addData((systemTime.unixtime & 0xFF000000) >> 24);
	addData((systemTime.unixtime & 0x00FF0000) >> 16);
	addData((systemTime.unixtime & 0x0000FF00) >> 8);
	addData(systemTime.unixtime & 0x000000FF);

	sendData();
}

// выдает статус: время на станции, номер станции, номер режима, число отметок, время последней страницы
void getStatus()
{
	replyCode = REPLY_GET_STATUS;

	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	init_package(replyCode);
	addData(OK);
	addData((systemTime.unixtime & 0xFF000000) >> 24);
	addData((systemTime.unixtime & 0x00FF0000) >> 16);
	addData((systemTime.unixtime & 0x0000FF00) >> 8);
	addData(systemTime.unixtime & 0x000000FF);

	addData(stationMode);

	addData(stationNumber);

	addData((totalChip & 0xFF00) >> 8);
	addData(totalChip & 0x00FF);

	addData((lastTime & 0xFF000000) >> 24);
	addData((lastTime & 0x00FF0000) >> 16);
	addData((lastTime & 0x0000FF00) >> 8);
	addData(lastTime & 0x000000FF);

	sendData();
}

//инициализация чипа
void initChip()
{
	replyCode = REPLY_INIT_CHIP;

	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522

   // Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		sendError(NO_CHIP, replyCode);
		return;
	}
	// Select one of the cards
	else if (!mfrc522.PICC_ReadCardSerial())
	{
		sendError(NO_CHIP, replyCode);
		return;
	}

	if (!ntagRead(PAGE_PASS))
	{
		sendError(READ_ERROR, replyCode);
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
		sendError(LOW_INIT_TIME, replyCode);
		return;
	}

	byte Wbuff[] = { 255,255,255,255 };

	for (byte page = 4; page < ntagValue; page++)
	{
		if (!ntagWrite(Wbuff, page))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}
	}

	for (byte i = 0; i < 4; i++) Wbuff[i] = 0;

	for (byte page = 4; page < ntagValue; page++)
	{
		if (!ntagWrite(Wbuff, page))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}
	}

	byte dataBlock5[] = { uartIncomingMessageData[DATA_START_BYTE], uartIncomingMessageData[DATA_START_BYTE + 1], ntagType, FW_VERSION };
	if (!ntagWrite(dataBlock5, PAGE_INIT))
	{
		sendError(WRITE_ERROR, replyCode);
		return;
	}

	byte dataBlock2[] = { uartIncomingMessageData[DATA_START_BYTE + 2], uartIncomingMessageData[DATA_START_BYTE + 3], uartIncomingMessageData[DATA_START_BYTE + 4], uartIncomingMessageData[DATA_START_BYTE + 5] };
	if (!ntagWrite(dataBlock2, PAGE_PASS))
	{
		sendError(WRITE_ERROR, replyCode);
		return;
	}

	byte dataBlock3[] = { uartIncomingMessageData[DATA_START_BYTE + 6], uartIncomingMessageData[DATA_START_BYTE + 7], uartIncomingMessageData[DATA_START_BYTE + 8], uartIncomingMessageData[DATA_START_BYTE + 9] };
	if (!ntagWrite(dataBlock3, PAGE_INIT1))
	{
		sendError(WRITE_ERROR, replyCode);
		return;
	}

	byte dataBlock4[] = { uartIncomingMessageData[DATA_START_BYTE + 10], uartIncomingMessageData[DATA_START_BYTE + 11], uartIncomingMessageData[DATA_START_BYTE + 12], uartIncomingMessageData[DATA_START_BYTE + 13] };
	if (!ntagWrite(dataBlock4, PAGE_INIT2))
	{
		sendError(WRITE_ERROR, replyCode);
		return;
	}

	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	if (!ntagRead(0))
	{
		sendError(READ_ERROR, replyCode);
		return;
	}

	init_package(replyCode);
	addData(OK);
	addData((systemTime.unixtime & 0xFF000000) >> 24);
	addData((systemTime.unixtime & 0x00FF0000) >> 16);
	addData((systemTime.unixtime & 0x0000FF00) >> 8);
	addData(systemTime.unixtime & 0x000000FF);

	for (byte h = 0; h < 7; h++)
	{
		addData(dump[h]);
	}

	sendData();
	SPI.end();
}

void getLastTeams()
{
	replyCode = REPLY_GET_LAST_TEAM;

	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	init_package(replyCode);
	addData(OK);
	addData((systemTime.unixtime & 0xFF000000) >> 24);
	addData((systemTime.unixtime & 0x00FF0000) >> 16);
	addData((systemTime.unixtime & 0x0000FF00) >> 8);
	addData(systemTime.unixtime & 0x000000FF);

	addData(lastChip[0]);
	addData(lastChip[1]);
	for (byte r = 2; r < 14; r++)
	{
		addData(lastChip[r]);
	}
	addData((lastTime & 0xFF000000) >> 24);
	addData((lastTime & 0x00FF0000) >> 16);
	addData((lastTime & 0x0000FF00) >> 8);
	addData(lastTime & 0x000000FF);

	sendData();
}

void getChipHistory()
{
	replyCode = REPLY_GET_CHIP_HISTORY;


	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	init_package(replyCode);
	addData(OK);
	addData((systemTime.unixtime & 0xFF000000) >> 24);
	addData((systemTime.unixtime & 0x00FF0000) >> 16);
	addData((systemTime.unixtime & 0x0000FF00) >> 8);
	addData(systemTime.unixtime & 0x000000FF);

	unsigned long timeFrom = uartIncomingMessageData[DATA_START_BYTE];
	timeFrom <<= 8;
	timeFrom += uartIncomingMessageData[DATA_START_BYTE + 1];
	timeFrom <<= 8;
	timeFrom += uartIncomingMessageData[DATA_START_BYTE + 2];
	timeFrom <<= 8;
	timeFrom += uartIncomingMessageData[DATA_START_BYTE + 3];

	for (int chipN = 1; chipN < 2000; chipN++)
	{
		unsigned long shortF = chipN;
		shortF = shortF * 20;

		if (SPIflash.readByte(shortF) != 255)
		{
			unsigned long markTime = SPIflash.readByte(shortF + 16);
			markTime <<= 8;
			markTime += SPIflash.readByte(shortF + 17);
			markTime <<= 8;
			markTime += SPIflash.readByte(shortF + 18);
			markTime <<= 8;
			markTime += SPIflash.readByte(shortF + 19);

			if (markTime > timeFrom)
			{
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
				sendData();
			}
		}
	}
}

void getStationClones()
{
	replyCode = REPLY_GET_STATION_CLONES;

	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522
	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		sendError(NO_CHIP, replyCode);
		return;
	}
	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		sendError(NO_CHIP, replyCode);
		return;
	}

	if (!ntagRead(PAGE_INIT))
	{
		sendError(READ_ERROR, replyCode);
		return;
	}

	init_package(replyCode);
	addData(dump[0]);

	for (byte page = 5; page < ntagValue; page++)
	{
		if (!ntagRead(page))
		{
			sendError(READ_ERROR, replyCode);
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

	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	addData(OK);
	addData((systemTime.unixtime & 0xFF000000) >> 24);
	addData((systemTime.unixtime & 0x00FF0000) >> 16);
	addData((systemTime.unixtime & 0x0000FF00) >> 8);
	addData(systemTime.unixtime & 0x000000FF);

	sendData();
}

void updateTeamMask()
{
	replyCode = REPLY_UPDATE_TEAM_MASK;

	for (byte i = 0; i < 8; i++)
	{
		tempTeamMask[i] = uartIncomingMessageData[i - 4];
	}

	tempNumTeam0 = uartIncomingMessageData[DATA_START_BYTE];
	tempNumTeam1 = uartIncomingMessageData[DATA_START_BYTE + 1];

	for (byte i = 0; i < 8; i++)
	{
		tempTeamMask[i] = uartIncomingMessageData[i + 2];
	}

	if (stationMode == 1) return;

	if (!ntagRead(PAGE_INIT))
	{
		sendError(READ_ERROR, replyCode);
		return;
	}

	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522

   // Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		sendError(NO_CHIP, replyCode);
		return;
	}

	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		sendError(NO_CHIP, replyCode);
		return;
	}

	if (dump[0] == tempNumTeam0 && dump[1] == tempNumTeam1)
	{
		byte dataBlock[4] = { uartIncomingMessageData[DATA_START_BYTE + 2], uartIncomingMessageData[DATA_START_BYTE + 3], uartIncomingMessageData[DATA_START_BYTE + 4], uartIncomingMessageData[DATA_START_BYTE + 5] };
		if (!ntagWrite(dataBlock, PAGE_INIT1))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}

		byte dataBlock2[] = { uartIncomingMessageData[DATA_START_BYTE + 6], uartIncomingMessageData[DATA_START_BYTE + 7], uartIncomingMessageData[DATA_START_BYTE + 8], uartIncomingMessageData[DATA_START_BYTE + 9] };
		if (!ntagWrite(dataBlock2, PAGE_INIT2))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}
	}
	else
	{
		sendError(WRONG_CHIP, replyCode);
		return;
	}

	SPI.end();

	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	init_package(replyCode);
	addData(OK);
	addData((systemTime.unixtime & 0xFF000000) >> 24);
	addData((systemTime.unixtime & 0x00FF0000) >> 16);
	addData((systemTime.unixtime & 0x0000FF00) >> 8);
	addData(systemTime.unixtime & 0x000000FF);

	sendData();
}

void writeMasterChip()
{
	replyCode = REPLY_WRITE_MASTER_CHIP;

	SPI.begin();      // Init SPI bus
	mfrc522.PCD_Init();   // Init MFRC522

	// Look for new cards
	if (!mfrc522.PICC_IsNewCardPresent())
	{
		sendError(NO_CHIP, replyCode);
		return;
	}

	// Select one of the cards
	if (!mfrc522.PICC_ReadCardSerial())
	{
		sendError(NO_CHIP, replyCode);
		return;
	}

	byte pass0 = uartIncomingMessageData[DATA_START_BYTE + 11];
	byte pass1 = uartIncomingMessageData[DATA_START_BYTE + 12];
	byte pass2 = uartIncomingMessageData[DATA_START_BYTE + 13];
	byte setting = uartIncomingMessageData[DATA_START_BYTE + 14];

	if (uartIncomingMessageData[DATA_START_BYTE] == 253)
	{
		byte Wbuff[] = { 255, 255, 255, 255 };

		for (byte page = 4; page < ntagValue; page++)
		{
			if (!ntagWrite(Wbuff, page))
			{
				sendError(WRITE_ERROR, replyCode);
				return;
			}
		}

		byte Wbuff2[] = { 0,0,0,0 };

		for (byte page = 4; page < ntagValue; page++)
		{
			if (!ntagWrite(Wbuff2, page))
			{
				sendError(WRITE_ERROR, replyCode);
				return;
			}
		}
		byte dataBlock[4] = { 0, 253, 255, FW_VERSION };
		if (!ntagWrite(dataBlock, PAGE_INIT))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}


		byte dataBlock2[] = { pass0, pass1, pass2, 0 };
		if (!ntagWrite(dataBlock2, PAGE_PASS))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}
	}

	if (uartIncomingMessageData[DATA_START_BYTE] == 252)
	{
		byte dataBlock[4] = { 0, 252, 255, FW_VERSION };
		if (!ntagWrite(dataBlock, PAGE_INIT))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}

		byte dataBlock2[] = { pass0, pass1, pass2, 0 };
		if (!ntagWrite(dataBlock2, PAGE_PASS))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}
	}

	if (uartIncomingMessageData[DATA_START_BYTE] == 251)
	{
		byte dataBlock[4] = { 0, 251, 255, FW_VERSION };
		if (!ntagWrite(dataBlock, PAGE_INIT))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}

		byte dataBlock2[] = { pass0, pass1, pass2,0 };
		if (!ntagWrite(dataBlock2, PAGE_PASS))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}

		byte dataBlock3[] = { uartIncomingMessageData[DATA_START_BYTE + 7], 0, 0, 0 };
		if (!ntagWrite(dataBlock3, PAGE_INIT1))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}
	}

	if (uartIncomingMessageData[DATA_START_BYTE] == 254)
	{
		byte dataBlock[4] = { 0, 254, 255, FW_VERSION };
		if (!ntagWrite(dataBlock, PAGE_INIT))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}

		byte dataBlock2[] = { uartIncomingMessageData[DATA_START_BYTE + 8], uartIncomingMessageData[DATA_START_BYTE + 9], uartIncomingMessageData[DATA_START_BYTE + 10], 0 };
		if (!ntagWrite(dataBlock2, PAGE_INIT1))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}

		byte dataBlock3[] = { pass0, pass1, pass2, setting };
		if (!ntagWrite(dataBlock3, PAGE_PASS))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}
	}

	if (uartIncomingMessageData[DATA_START_BYTE] == 250)
	{
		byte dataBlock[4] = { 0, 250, 255, FW_VERSION };
		if (!ntagWrite(dataBlock, PAGE_INIT))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}

		byte dataBlock2[] = { pass0, pass1, pass2, 0 };
		if (!ntagWrite(dataBlock2, PAGE_PASS))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}

		byte dataBlock3[] = { uartIncomingMessageData[DATA_START_BYTE + 2], uartIncomingMessageData[DATA_START_BYTE + 1], uartIncomingMessageData[DATA_START_BYTE + 3], 0 };
		if (!ntagWrite(dataBlock3, PAGE_INIT1))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}

		byte dataBlock4[] = { uartIncomingMessageData[DATA_START_BYTE + 4], uartIncomingMessageData[DATA_START_BYTE + 5], uartIncomingMessageData[DATA_START_BYTE + 6], 0 };
		if (!ntagWrite(dataBlock4, PAGE_INIT2))
		{
			sendError(WRITE_ERROR, replyCode);
			return;
		}
	}


	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	init_package(replyCode);
	addData(OK);
	addData((systemTime.unixtime & 0xFF000000) >> 24);
	addData((systemTime.unixtime & 0x00FF0000) >> 16);
	addData((systemTime.unixtime & 0x0000FF00) >> 8);
	addData(systemTime.unixtime & 0x000000FF);

	sendData();
}

// Internal functions

//запись в память с мажоритальным резервированием
void eepromwrite(int adr, byte val)
{
	for (byte i = 0; i < 3; i++)
	{
		EEPROM.write(adr + i, val);
	}
}

//считывание из памяти с учетом мажоритального резервирования
byte eepromread(int adr)
{
	int byte1 = EEPROM.read(adr);
	int byte2 = EEPROM.read(adr + 1);
	int byte3 = EEPROM.read(adr + 2);

	// надо переделать выбор правильно считанного значения или давать ошибку при любом несовпадении
	if (byte1 == byte2
		|| byte1 == byte3)
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
	uartIncomingMessageData[0] = uartIncomingMessageData[1] = uartIncomingMessageData[2] = uartIncomingMessageData[3] = 0xFE;
	uartIncomingMessageData[COMMAND_BYTE] = command;
	dataPosition = DATA_START_BYTE;
}

//добавление данных в буфер
void addData(byte data)
{
	if (dataPosition > 255) return;
	uartIncomingMessageData[dataPosition] = data;
	dataPosition++;
}

//передача пакета данных.
void sendData()
{
	uartIncomingMessageData[LENGTH_BYTE] = dataPosition - COMMAND_BYTE;
	addData(crcCalc(uartIncomingMessageData, COMMAND_BYTE, uartIncomingMessageData[LENGTH_BYTE]));
#ifdef DEBUG
	Serial.print(F("Sending:"));
	for (int i = 0; i <= dataPosition; i++)
	{
		Serial.print(F(" "));
		if (uartIncomingMessageData[i] < 0x10) Serial.print(F("0"));
		Serial.print(String(uartIncomingMessageData[i], HEX));
	}
	Serial.println();
#endif
	Serial.write(uartIncomingMessageData, dataPosition);
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
	byte lastNum = 0; //last writed number of station in memory of chip
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
		replyCode = 0x98;
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
					sendError(WRITE_ERROR, replyCode);
					return;
				}

				byte dataBlock2[] = { tempTeamMask[4],tempTeamMask[5],tempTeamMask[6],tempTeamMask[7] };
				if (!ntagWrite(dataBlock2, PAGE_INIT2))
				{
					sendError(WRITE_ERROR, replyCode);
					return;
				}
			}
		}
		else
		{
			sendError(WRONG_CHIP, replyCode);
			return;
		}
		tempNumTeam0 = 0;
		tempNumTeam1 = 0;

		for (byte i = 0; i < 8; i++)
		{
			tempTeamMask[i] = 0;
		}


		unsigned long tempT = systemTime.unixtime;

		init_package(replyCode);
		addData(OK);
		addData((systemTime.unixtime & 0xFF000000) >> 24);
		addData((systemTime.unixtime & 0x00FF0000) >> 16);
		addData((systemTime.unixtime & 0x0000FF00) >> 8);
		addData(systemTime.unixtime & 0x000000FF);
		sendData();
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
	totalChip++;
	beep(200, 1);

	SPI.end();
} // end of rfid()

bool writeTimeToCard(int newPage)
{
	unsigned long code = stationNumber;
	code = code << 24;
	code += (systemTime.unixtime & 0x00FFFFFF);

	byte toWrite[4] = { 0, 0, 0, 0 };
	toWrite[0] = (code & 0xFF000000) >> 24;
	toWrite[1] = (code & 0x00FF0000) >> 16;
	toWrite[2] = (code & 0x0000FF00) >> 8;
	toWrite[3] = (code & 0x000000FF);

	byte dataBlock2[4] = { toWrite[0], toWrite[1], toWrite[2], toWrite[3] };

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
	unsigned long pageFlash = chipNum;
	pageFlash = pageFlash * 1000 + 100000UL;
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

			lastTime = systemTime.unixtime;

			byte byteTime[4];
			byteTime[0] = (lastTime & 0xFF000000) >> 24;
			byteTime[1] = (lastTime & 0x00FF0000) >> 16;
			byteTime[2] = (lastTime & 0x0000FF00) >> 8;
			byteTime[3] = (lastTime & 0x000000FF);

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
void sendError(byte returnCode, byte replyCode)
{
	DS3231_get(&systemTime);
	unsigned long tempT = systemTime.unixtime;

	init_package(replyCode);
	addData(returnCode);
	addData((systemTime.unixtime & 0xFF000000) >> 24);
	addData((systemTime.unixtime & 0x00FF0000) >> 16);
	addData((systemTime.unixtime & 0x0000FF00) >> 8);
	addData(systemTime.unixtime & 0x000000FF);

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
			uartIncomingMessagePosition = 0;
			receivingData = false;
			return false;
		}
		//0 byte = FE
		else if (uartIncomingMessagePosition == 0 && c == 0xfe)
		{
#ifdef DEBUG
			Serial.print(F("byte0="));
			if (c < 0x10) Serial.print(F("0"));
			Serial.println(String(byte(c), HEX));
#endif
			receivingData = true;
			uartIncomingMessageData[uartIncomingMessagePosition] = (byte)c;
			uartIncomingMessagePosition++;
			// refresh timeout
			receiveStartTime = millis();
		}
		//1st byte = FE
		else if (uartIncomingMessagePosition == 1 && c == 0xfe)
		{
#ifdef DEBUG
			Serial.print(F("byte1"));
			if (c < 0x10) Serial.print(F("0"));
			Serial.println(String(byte(c), HEX));
#endif
			uartIncomingMessageData[uartIncomingMessagePosition] = (byte)c;
			uartIncomingMessagePosition++;
		}
		//2nd byte = FE
		else if (uartIncomingMessagePosition == 2 && c == 0xfe)
		{
#ifdef DEBUG
			Serial.print(F("byte2"));
			if (c < 0x10) Serial.print(F("0"));
			Serial.println(String(byte(c), HEX));
#endif
			uartIncomingMessageData[uartIncomingMessagePosition] = (byte)c;
			uartIncomingMessagePosition++;
		}
		//3rd byte = FE
		else if (uartIncomingMessagePosition == 3 && c == 0xfe)
		{
#ifdef DEBUG
			Serial.print(F("byte3"));
			if (c < 0x10) Serial.print(F("0"));
			Serial.println(String(byte(c), HEX));
#endif
			uartIncomingMessageData[uartIncomingMessagePosition] = (byte)c;
			uartIncomingMessagePosition++;
		}
		//4th byte = command, length and data
		else if (uartIncomingMessagePosition >= COMMAND_BYTE)
		{
			uartIncomingMessageData[uartIncomingMessagePosition] = (byte)c;
#ifdef DEBUG
			Serial.print(F("byte"));
			Serial.print(String(uartIncomingMessagePosition));
			Serial.print(F("="));
			if (c < 0x10) Serial.print(F("0"));
			Serial.println(String(byte(c), HEX));
#endif
			//incorrect length
			if (uartIncomingMessagePosition == LENGTH_BYTE && uartIncomingMessageData[LENGTH_BYTE] > (255 - DATA_START_BYTE - 1))
			{
#ifdef DEBUG
				Serial.println(F("incorrect length"));
#endif
				uartIncomingMessagePosition = 0;
				receivingData = false;
				return false;
			}

			//packet is received
			if (uartIncomingMessagePosition >= DATA_START_BYTE + uartIncomingMessageData[LENGTH_BYTE])
			{
				//crc matching
#ifdef DEBUG
				Serial.print(F("received packet expected CRC="));
				Serial.println(String(crcCalc(uartIncomingMessageData, COMMAND_BYTE, uartIncomingMessageData[LENGTH_BYTE]), HEX));
#endif
				if (uartIncomingMessageData[uartIncomingMessagePosition] == crcCalc(uartIncomingMessageData, COMMAND_BYTE, uartIncomingMessageData[LENGTH_BYTE])) // CRC not correct
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
					uartIncomingMessagePosition = 0;
					receivingData = false;
					return true;
				}
				else
				{
#ifdef DEBUG
					Serial.println(F("incorrect crc"));
#endif
					uartIncomingMessagePosition = 0;
					receivingData = false;
					return false;
				}
			}
			uartIncomingMessagePosition++;
		}
		else
		{
#ifdef DEBUG
			Serial.println(F("unexpected byte"));
#endif
			uartIncomingMessagePosition = 0;
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
