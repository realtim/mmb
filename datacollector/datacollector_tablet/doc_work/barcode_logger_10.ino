/*
Нововведения:
- провера корректности времени с RTC
- контрольные суммы CRC8 передаваемых по Bluetooth логов
- закомментирована возможность получения лога построчно (выделить CRC8 в подпрограмму и работать с глобальными переменными)

SD card attached to SPI bus as follows:
  *MOSI - D11
  *MISO - D12
  *CLK  - D13
  *CS   - D10

Software serial port:
  *TX - D3
  *RX - D4

DS1307 RTC connected via I2C:
  *SCL - A5
  *SDA - A4
  
Speaker:
  *D08
Green LED
  *D09
Red LED
  *D07

Libraries used:
RTClib.h - adafruit library
		https://github.com/adafruit/RTClib

Barcode commands:
//CONFIGI - change scanner ID
CONFIGC - change control point
//CONFIGL - change BarCode string length check enable [Y/N]
//CONFIGN - change BarCode string numbers only [Y/N]
//CONFIGP - change BarCode pattern
CONFIGB - Bluetooth operation mode

Bluetooth commands:
SETI - change scanner ID
SETC - change control point
SETL - change BarCode string length check enable [Y/N]
SETN - change BarCode string numbers only [Y/N]
SETP - change BarCode pattern
SETT - change current time

GETS - get current settings and log sizes
GETL - get all barcode log
GETD - get all debug log
GET#Lxxx - get line#xxx from barcode log
GET#Dxxx - get line#xxx from debug log

DELLOG - delete both logfiles

END - return to scanner operation mode

Параметры подключения по Bluetooth:
Baudrate - 57600
Stop bit - 1
Parity - None
PIN - 2014
*/

#include <SPI.h>
#include <SD.h>
#include <SoftwareSerial.h>
#include <Wire.h>
#include <RTClib.h>

const byte chipSelect = 10; // SD-card pin

const byte Spk=8;  // BUZZER pin set
const byte GLed=9;  // GREEN LED pin set
const byte RLed=7;  // RED LED pin set

const byte TXPin = 3;
const byte RXPin = 4;
SoftwareSerial ScannerSerial(RXPin, TXPin);

//initial scanner config
String cfgID="";
String cfgCP="";
char cfgNumOnly;
char cfgStrLCheck;
int cfgStrLength=0, i=0;
String cfgPattern="";
char cfgFile[]="config.txt";
char logFile[]="datalog.txt";
char errFile[]="errors.txt";

RTC_DS1307 rtc;

File dataFile;

void setup()
{
  //Set LED outputs
  pinMode(GLed, OUTPUT);
  pinMode(RLed, OUTPUT);
  Serial.begin(57600);  // Open serial communications and wait for port to open
  Serial.println(F("Barcode Logger v10"));
  //Initializing SD card and file
  Serial.print(F("SD card-"));
  pinMode(chipSelect, OUTPUT);  // make sure that the default chip select pin is set to output, even if you don't use it
  if (!SD.begin(chipSelect))  // see if the card is present and can be initialized:
  {
    Serial.println(F("failed!"));
    Alarm(RLed, 2000, 5000);
  }
  else
  {
    Serial.println(F("success."));
    //Reading configuration file
    dataFile = SD.open(cfgFile, FILE_READ);
    if (dataFile)
    {
      cfgID+=char(dataFile.read());
      cfgID+=char(dataFile.read());
      dataFile.read();
      cfgCP+=char(dataFile.read());
      cfgCP+=char(dataFile.read());
      dataFile.read();
      cfgStrLCheck=char(dataFile.read());
      dataFile.read();
      cfgNumOnly=char(dataFile.read());
      dataFile.read();
      while (dataFile.available() && i==0)
      {
        cfgPattern+=char(dataFile.read());
        if (cfgPattern[cfgStrLength]==0x0D || cfgPattern[cfgStrLength]==0x0A)
        {
          i++;
        }
        else cfgStrLength++;
      }
      dataFile.close();
      Serial.print(cfgFile);
      Serial.println(F(" accessible"));
      Alarm(GLed, 4000, 200);
    }
    else // if the file isn't open, pop up an error
    {
      errOpen(cfgFile);
    }
    confPrint();
    
    dataFile = SD.open(logFile, FILE_WRITE);
    if (dataFile)  // if the file is available, write to it
    {
      Serial.print(logFile);
      Serial.println(F(" accessible"));
      dataFile.close();
      Alarm(GLed, 4000, 200);
    }
    else  // if the file isn't open, pop up an error
    {
      errOpen(logFile);
    }
  }
  
  ScannerSerial.begin(9600);  //Initializing scanner serial port
  
  //Initializing RTC DS1307
  Serial.print(F("RTC "));
  Wire.begin();
  rtc.begin();
  if (! rtc.isrunning())
  {
    Serial.println(F("error!"));
    Alarm(RLed, 2000, 5000);
  }
  else
  {
    Serial.println(F("running."));
    Alarm(GLed, 4000, 200);
  }
  Serial.println(GetDateTime());
}


void loop()
{
  String dataString = "";
  String dataString2 = "";
  int strSize=0;
  char key=0, flag=0;
  
  // make a string for assembling the data to log:
  dataString += cfgID;
  dataString += ", ";
  dataString += cfgCP;
  dataString += ", ";
  
  while (key!=0x0D && key!=0x0A)
  {
    if (ScannerSerial.available())
    {
      key = ScannerSerial.read();
      if (key!=0x0D && key!=0x0A)
      {
        dataString += char(key);
        if (cfgNumOnly=='Y')
        {
          if (dataString[strSize+8]<'0' || dataString[strSize+8]>'9') flag++; //Only numbers are allowed in the barcode
        }
        strSize++;
      }
    }
  }
  if (strSize==0) return; //only CR/LF was in the buffer
  dataString += ", ";
  dataString += GetDateTime();
  Serial.println(dataString);
  
  //Command barcode found
  if (dataString[8]=='C'&& dataString[9]=='O'&& dataString[10]=='N'&& dataString[11]=='F'&& dataString[12]=='I'&& dataString[13]=='G')
  {
    stringToFile(errFile, dataString);
    switch (dataString[14])
    {
      /*
      case 'I':  //Set Scanner ID
        cfgID="";
        cfgID+=char(dataString[15]);
        cfgID+=char(dataString[16]);
        Serial.print(F("Scanner ID set to: "));
        Serial.println(cfgID);
        confUpdate();
      break;
      */
      case 'C':  //Set Control Point
        cfgCP="";
        cfgCP+=char(dataString[15]);
        cfgCP+=char(dataString[16]);
        Serial.print(F("Control Point # set to: "));
        Serial.println(cfgCP);
        confUpdate();
      break;
      /*
      case 'L':  //Set Barcode length checking
        cfgStrLCheck=char(dataString[15]);
        Serial.print(F("String length check set to: "));
        Serial.println(cfgStrLCheck);
        confUpdate();
      break;
      
      case 'N':  //Set char presence checking
        cfgNumOnly=char(dataString[15]);
        Serial.print(F("Numbers only set to: "));
        Serial.println(cfgNumOnly);
        confUpdate();
      break;
      
      case 'P':  //Set Barcode pattern
        i=0;
        cfgStrLength=0;
        cfgPattern="";
        while (dataString[15+cfgStrLength]!=',')
        {
          cfgPattern+=char(dataString[15+cfgStrLength]);
          cfgStrLength++;
        }
        Serial.print(F("Barcode pattern set to: "));
        Serial.println(cfgPattern);
        Serial.print(F("Barcode length: "));
        Serial.println(cfgStrLength, DEC);
        confUpdate();
      break;
      */
      case 'B':  //start BT operation mode
        Alarm(GLed, 4000, 200);
        digitalWrite(RLed, HIGH);
        digitalWrite(GLed, HIGH);
        Serial.println(F("BlueTooth operation mode started"));
        flag=0;
        while(flag==0)
        {
          dataString = "";
          dataString2 = "";
          strSize=-1;
          while (Serial.available()) Serial.read();  // !!! need to clear the serial buffer
          do
          {
            if (Serial.available())
            {
              dataString+=char(Serial.read());
              strSize++;
            }
          } while (dataString[strSize]!=0x0D && dataString[strSize]!=0x0A);  //Getting the string from BT until CR+LF
          //strSize--;
          //dataString[strSize]=0;  //?????????????????????
          //stringToFile(errFile, dataString);
          Serial.print(F("Command received: "));
          Serial.println(dataString);

          if (dataString[0]=='S'&& dataString[1]=='E'&& dataString[2]=='T')  //SETTINGS
          {
            switch (dataString[3])
            {
              case 'I':  //change scanner ID
                cfgID="";
                cfgID+=char(dataString[4]);
                cfgID+=char(dataString[5]);
                Serial.print(F("Scanner ID set to: "));
                Serial.println(cfgID);
                confUpdate();
              break;
              
              case 'C':  //change control point
                cfgCP="";
                cfgCP+=char(dataString[4]);
                cfgCP+=char(dataString[5]);
                Serial.print(F("Control Point # set to: "));
                Serial.println(cfgCP);
                confUpdate();
              break;
      
              case 'L':  //Set Barcode length checking
                cfgStrLCheck=char(dataString[4]);
                Serial.print(F("String length check set to: "));
                Serial.println(cfgStrLCheck);
                confUpdate();
              break;
      
              case 'N':  //Set char presence checking
                cfgNumOnly=char(dataString[4]);
                Serial.print(F("Numbers only set to: "));
                Serial.println(cfgNumOnly);
                confUpdate();
              break;
      
              case 'P':  //Set Barcode pattern
                i=0;
                cfgStrLength=0;
                cfgPattern="";
                while (dataString[4+cfgStrLength]!=0x0D && dataString[4+cfgStrLength]!=0x0A && dataString[4+cfgStrLength]!=0x00)
                {
                  cfgPattern+=char(dataString[4+cfgStrLength]);
                  cfgStrLength++;
                }
                Serial.print(F("Barcode pattern set to: "));
                Serial.println(cfgPattern);
                Serial.print(F("Barcode length: "));
                Serial.println(cfgStrLength, DEC);
                confUpdate();
              break;
              
              case 'T':  //change current time
                char tm[9], dt[13];
                Serial.println(F("Input current date and time \"Mon dd yyyy hh:mm:ss\" (example: \"Dec 06 2014 12:04:00\")"));
                key=0;
                while (ScannerSerial.available()) ScannerSerial.read();  // clear the BlueTooth buffer
                while (key!=0x0D && key!=0x0A)
                {
                  if (Serial.available())
                  {
                    key=Serial.read();
                    if (key>31 && key<128) dataString2+=char(key);
                  }
                }
                //stringToFile(errFile, dataString2);
                for(key=0;key<12;key++)
                {
                  dt[key]=dataString2[key];
                }
                dt[12]=0;
                
                for(key=0;key<8;key++)
                {
                  tm[key]=dataString2[key+12];
                }
                tm[8]=0;
                
                rtc.adjust(DateTime(dt, tm));
                delay (4000);
                Serial.print(F("Current time (+4 sec.) is: "));
                Serial.println(GetDateTime());
              break;
            }
          }
          else if (dataString[0]=='G'&& dataString[1]=='E'&& dataString[2]=='T')  //DOWNLOAD data
          {
            switch (dataString[3])
            {
              case 'S':  //get current settings
                confPrint();
                Serial.println(GetDateTime());
                
                dataFile = SD.open(logFile, FILE_READ);
                if (dataFile)
                {
                  Serial.print(F("datalog.txt size: "));
                  Serial.println(dataFile.size());
                  dataFile.close();
                }
                else errOpen(logFile);
                
                dataFile = SD.open(errFile, FILE_READ);
                if (dataFile)
                {
                  Serial.print(F("errors.txt size: "));
                  Serial.println(dataFile.size());
                  dataFile.close();
                }
                else errOpen(errFile);
              break;
              
              case 'L':  //get all barcode log
                getLog(logFile);
              break;
              
              case 'D':  //get all errors log
                getLog(errFile);
              break;
              
              case '#':  //get selected string by number
                long int linenum2;
                dataString2="";
                strSize=5;
                  while (dataString[strSize]>='0' && dataString[strSize]<='9')
                  {
                    dataString2+=dataString[strSize];
                    strSize++;
                  }
                  linenum2=dataString2.toInt();
                  if (dataString[4]=='L') getLogLine(logFile, linenum2);  //from LOG file
                  if (dataString[4]=='D') getLogLine(errFile, linenum2);  //from DEBUG file
              break;
              
            }
          }
          else if (dataString[0]=='D' && dataString[1]=='E' && dataString[2]=='L' && dataString[3]=='L' && dataString[4]=='O' && dataString[5]=='G')  //Remove datalog.txt and errors.txt
          {
            if (SD.remove("datalog.txt")==true) Serial.println(F("datalog.txt deleted"));
            else Serial.println(F("Error deleting datalog.txt!"));
            if (SD.remove("errors.txt")==true) Serial.println(F("errors.txt deleted"));
            else Serial.println(F("Error deleting errors.txt!"));
          }
          else if (dataString[0]=='E'&& dataString[1]=='N'&& dataString[2]=='D')  //break BT operation mode
          {
            Serial.println(F("BlueTooth operation mode ended."));
            Alarm(GLed, 4000, 200);
            flag++;
            while (ScannerSerial.available()) ScannerSerial.read();  // clear the scanner buffer
            digitalWrite(RLed, LOW);
            digitalWrite(GLed, LOW);
          }
        }
      break;  //end of BT operation mode code
    }
    Alarm(GLed, 4000, 200);
  }
  else  //Non config barcode
  {
    if (flag>0) Serial.println(F("Illegal chars"));
    if (cfgStrLCheck=='Y' && strSize!=cfgStrLength && flag==0)  // String length does not match the settings
    {
      Serial.println(F("String size does not match"));
      flag++;
    }
    i=0;
    while(i<cfgStrLength && flag==0)
    {
      if (cfgPattern[i]!='*' && dataString[8+i]!=cfgPattern[i])  //String does not match the pattern
      {
        Serial.println(F("Pattern does not match"));
        flag++;
      }
      i++;
    }
    if (flag==0) //No mismatches found, string recognized
    {
      if (stringToFile(logFile, dataString)==0)
      {
        Alarm(GLed, 4000, 100);  //String written in the log file
        delay(200);
        Alarm(GLed, 4000, 500);
      }
    }
    else //Unknown barcode scanned
    {
      Serial.println(F("Barcode not recognized"));
      if (stringToFile(errFile, dataString)==0)
      {
        Alarm(RLed, 2000, 1000);  //String written in the log file
      }
    }
  }
}


String GetDateTime()  //assemble time+date string
{
  String TimeString = "";
  DateTime now = rtc.now();
  if (now.hour()>23 || now.hour()<0 || now.minute()>59 || now.minute()<0 || now.second()>59 || now.second()<0 || now.month()>12 || now.month()<0 || now.day()>31 || now.day()<0 || now.year()<2000 || now.year()>2100)
  {
    Serial.println(F("RTC clock incorrect!"));
    Alarm(RLed, 2000, 5000);
  }
  if (now.hour()<10) TimeString += "0";
  TimeString += String(now.hour(), DEC);
  TimeString += ":";
  if (now.minute()<10) TimeString += "0";
  TimeString += String(now.minute(), DEC);
  TimeString += ":";
  if (now.second()<10) TimeString += "0";
  TimeString += String(now.second(), DEC);
  TimeString += ", ";
  TimeString += String(now.year(), DEC);
  TimeString += "/";
  if (now.month()<10) TimeString += "0";
  TimeString += String(now.month(), DEC);
  TimeString += "/";
  if (now.day()<10) TimeString += "0";
  TimeString += String(now.day(), DEC);
  return(TimeString);
}


void confUpdate()  //Config file update
{
  dataFile = SD.open(cfgFile, FILE_WRITE);
  if (dataFile)
  {
    dataFile.seek(0);
    dataFile.print(cfgID);
    dataFile.print(F(" "));
    dataFile.print(cfgCP);
    dataFile.print(F(" "));
    dataFile.print(cfgNumOnly);
    dataFile.print(F(" "));
    dataFile.print(cfgStrLCheck);
    dataFile.print(F(" "));
    dataFile.println(cfgPattern);
    dataFile.close();
  }
  else
  {
    errOpen(cfgFile);
  }
}


void Alarm(int led, int freq, int pause)  //Alarm light and sound
{
  digitalWrite(led, HIGH);
  tone(Spk, freq, pause);
  delay(pause+100);
  digitalWrite(led, LOW);
}


void errOpen(char fileName[])  //give an error and string in case of file opening error
{
  Serial.print(F("Error opening "));
  Serial.println(fileName);
  Alarm(RLed, 2000, 5000);
}


void confPrint()  //settings print on screen
{
  Serial.println(F("Current config:"));
  Serial.print(F("-Scanner ID: "));
  Serial.println(cfgID);
  Serial.print(F("-Control Point: "));
  Serial.println(cfgCP);
  Serial.print(F("-Barcode string length checking: "));
  Serial.println(cfgStrLCheck);
  Serial.print(F("-Numbers only: "));
  Serial.println(cfgNumOnly);
  Serial.print(F("-Barcode pattern: "));
  Serial.println(cfgPattern);
  Serial.print(F("-Barcode length: "));
  Serial.println(cfgStrLength, DEC);
}


void getLog(char fileName[])  //send full file to serial with line# and CRC8
{
  char key, key2;
  dataFile = SD.open(fileName, FILE_READ);
  if (dataFile)
  {
    byte crc = 0x00, key2;
    long int linenum=0;
    Serial.print(fileName);
    Serial.println(F(" sending"));
    Serial.println(F("===="));
    while(dataFile.available())
    {
      key = char(dataFile.read());
      key2=key;
      if (key2==0x0D || key2==0x0A)
      {
        Serial.print(F(", Line#="));
        Serial.print(String(linenum));
        Serial.print(F(", CRC8="));
        Serial.print(String(crc));
        crc = 0x00;
        linenum++;
        dataFile.read();
        Serial.println("");
      }
      else
      {
        for (byte tempI = 8; tempI; tempI--)
        {
          byte sum = (crc ^ key) & 0x01;
          crc >>= 1;
          if (sum)
          {
            crc ^= 0x8C;
          }
          key >>= 1;
        }
        Serial.write(char(key2));
      }
    }
    Serial.println(F("===="));
    Serial.print(F("File size: "));
    Serial.println(dataFile.size());
    Serial.print(fileName);
    Serial.println(F(" sent"));
    dataFile.close();
  }
  else
  {
    errOpen(fileName);
  }
}


void getLogLine(char fileName[], long int line)  //send line from file to serial with line# and CRC8
{
  long int linenum=0;
  char key;
  byte crc = 0x00, key2;
  dataFile = SD.open(fileName, FILE_READ);
  if (dataFile)
  {
    Serial.print(fileName);
    Serial.print(F(" sending, string #"));
    Serial.println(line);
    Serial.println(F("===="));
    while(linenum<line)
    {
      key=char(dataFile.read());
      if (key==0x0D || key==0x0A)
      {
        linenum++;
        dataFile.read();
      }
    }
    do {
      key = char(dataFile.read());
      key2 = char(key);
      if (key2!=0x0D && key2!=0x0A && key2!=0x00)
      {
        for (byte tempI = 8; tempI; tempI--)
        {
          byte sum = (crc ^ key) & 0x01;
          crc >>= 1;
          if (sum)
          {
            crc ^= 0x8C;
          }
          key >>= 1;
        }
        Serial.write(char(key2));
      }
    } while (key2!=0x0D && key2!=0x0A && key2!=0x00);
    
    Serial.print(F(", Line#="));
    Serial.print(String(linenum));
    Serial.print(F(", CRC8="));
    Serial.println(String(crc));
    Serial.println("====");
    dataFile.close();
  }
  else errOpen(fileName);
}


byte stringToFile(char fileName[], String logString)  //print string to file with write verification
{
  String logString2="";
  char key=0;
  dataFile = SD.open(fileName, FILE_WRITE);
  if (dataFile)
  {
    dataFile.println(logString);
    dataFile.seek(dataFile.size()-logString.length()-2);  //Check if the written data is the same as scanned.
    while (dataFile.available())
    {
      key=char(dataFile.read());
      if (key!=0x0D && key!=0x0A) logString2 += key;
    }
    dataFile.close();
    if (logString != logString2) //Record is not corect
    {
      Serial.print(fileName);
      Serial.println(F(" write verification error!"));
      Alarm(RLed, 2000, 5000);
      return(1);
    }
    return(0);
  }
  else
  {
    errOpen(fileName);
    return(1);
  }
}
