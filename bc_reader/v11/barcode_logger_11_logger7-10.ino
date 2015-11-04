/*
РќРѕРІРѕРІРІРµРґРµРЅРёСЏ:
 - РїСЂРѕРІРµСЂР° РєРѕСЂСЂРµРєС‚РЅРѕСЃС‚Рё РІСЂРµРјРµРЅРё СЃ RTC
 - РєРѕРЅС‚СЂРѕР»СЊРЅС‹Рµ СЃСѓРјРјС‹ CRC8 РїРµСЂРµРґР°РІР°РµРјС‹С… РїРѕ Bluetooth Р»РѕРіРѕРІ
 - Р·Р°РєРѕРјРјРµРЅС‚РёСЂРѕРІР°РЅР° РІРѕР·РјРѕР¶РЅРѕСЃС‚СЊ РїРѕР»СѓС‡РµРЅРёСЏ Р»РѕРіР° РїРѕСЃС‚СЂРѕС‡РЅРѕ (РІС‹РґРµР»РёС‚СЊ CRC8 РІ РїРѕРґРїСЂРѕРіСЂР°РјРјСѓ Рё СЂР°Р±РѕС‚Р°С‚СЊ СЃ РіР»РѕР±Р°Р»СЊРЅС‹РјРё РїРµСЂРµРјРµРЅРЅС‹РјРё)
 
 SD card attached to SPI bus as follows:
 *MOSI - D11
 *MISO - D12
 *CLK  - D13
 *CS   - D10
 
 Software serial port:
 *TX - D4
 *RX - D3
 
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
 CONFIGC - change control point
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
 GETT - get logger time
 
 DELLOG - delete both logfiles
 
 END - return to scanner operation mode
 
 Bluetooth settings:
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

const byte TXPin = 4;
const byte RXPin = 3;
SoftwareSerial ScannerSerial(RXPin, TXPin);

//initial scanner config
char cfgID[3]={
  0,0,0};
char cfgCP[3]={
  0,0,0};
char cfgNumOnly;
char cfgStrLCheck;
int cfgStrSize=0, i=0;
String cfgPattern="";
char cfgFile[]="config.txt";
char logFile[]="datalog.txt";
char debugFile[]="debug.txt";

String dataString = "";
String dataString2 = "";
int strSize=0, strSize2=0;
boolean bc_data, bt_mode=false;
byte flag=0;

RTC_DS1307 rtc;

File dataFile;

void setup()
{
  //Set LED outputs
  pinMode(GLed, OUTPUT);
  pinMode(RLed, OUTPUT);
  Serial.begin(57600);  // Open serial communications and wait for port to open
  Serial.println(F("Barcode Logger v11(logger7-10)"));
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
    Serial.print(cfgFile);
    if (dataFile)
    {
      cfgID[0]=dataFile.read();
      cfgID[1]=dataFile.read();
      dataFile.read();
      cfgCP[0]=dataFile.read();
      cfgCP[1]=dataFile.read();
      dataFile.read();
      cfgStrLCheck=char(dataFile.read());
      dataFile.read();
      cfgNumOnly=char(dataFile.read());
      dataFile.read();
      while (dataFile.available() && i==0)
      {
        cfgPattern+=char(dataFile.read());
        if (cfgPattern[cfgStrSize]==0x0D || cfgPattern[cfgStrSize]==0x0A)
        {
          i++;
        }
        else cfgStrSize++;
      }
      dataFile.close();
      Alarm(GLed, 4000, 200);
    }
    else // if the file isn't open, pop up an error
    {
      Serial.println(F(" not"));
      errOpen(cfgFile);
    }
    Serial.println(F(" accessible"));

    Serial.print(logFile);
    dataFile = SD.open(logFile, FILE_WRITE);
    if (dataFile)  // if the file is available, write to it
    {
      dataFile.close();
      Alarm(GLed, 4000, 200);
    }
    else  // if the file isn't open, pop up an error
    {
      Serial.println(F(" not"));
      errOpen(logFile);
    }
    Serial.println(F(" accessible"));
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
  confPrint();
}

void loop()
{
  boolean bc_data;
  char key=0, key2=0;

  while (key!=0x0D && key!=0x0A && key2!=0x0D && key2!=0x0A)
  {
    //reading BarcodeScanner
    if (ScannerSerial.available())
    {
      key = ScannerSerial.read();
      if (key>31)
      {
        dataString += key;
        if (cfgNumOnly=='Y')
        {
          if (dataString[strSize]<'0' || dataString[strSize]>'9') flag++; //Only numbers are allowed in the barcode
        }
        strSize++;
      }
      else if (key==0x0D || key==0x0A) bc_data=true;
    }
    //reading Bluetooth
    if (Serial.available() && key!=0x0D && key!=0x0A)
    {
      key2 = Serial.read();
      if (key2>31)
      {
        dataString2 += key2;
        strSize2++;
      }
      else if (key2==0x0D || key2==0x0A) bc_data=false;
    }
  }

  // if it's a barcode data
  if (bc_data==true && dataString[0]>31)
  {
    stringToFile(debugFile, dataString);
    //if (strSize==0) return; //only CR/LF was in the buffer
    // make a string for assembling the data to log:
    //dataString = cfgID + F(", ") + cfgCP + F(", ") + dataString + F(", ") + GetDateTime();
    Serial.println(String(cfgID) + F(", ") + String(cfgCP) + F(", ") + dataString + F(", ") + GetDateTime());


    //Command barcode found
    if (dataString[0]=='C'&& dataString[1]=='O'&& dataString[2]=='N'&& dataString[3]=='F'&& dataString[4]=='I'&& dataString[5]=='G')
    {
      //stringToFile(debugFile, dataString);
      switch (dataString[6])
      {
      case 'C':  //Set Control Point
        //cfgCP="";
        if (strSize==9)
        {
          cfgCP[0] = dataString[7];
          cfgCP[1] = dataString[8];
          Serial.print(F("Control Point # set to: "));
          Serial.println(cfgCP);
          confUpdate();
        }
        else Serial.println(F("Control Point # incorrect"));
        break;
      case 'B':  //start BT operation mode
        if (strSize==7)
        {
          bt_mode=true;
          Alarm(GLed, 4000, 200);
          Serial.println(F("BlueTooth operation mode started"));
        }
        break;
      }
      Alarm(GLed, 4000, 200);
    }
    else  //Non config barcode
    {
      if (flag>0) Serial.println(F("Illegal chars"));
      if (cfgStrLCheck=='Y' && strSize!=cfgStrSize && flag==0)  // String length does not match the settings
      {
        Serial.println(F("String size does not match"));
        flag++;
      }
      i=0;
      while(i<cfgStrSize && flag==0)
      {
        if (cfgPattern[i]!='*' && dataString[i]!=cfgPattern[i])  //String does not match the pattern
        {
          Serial.println(F("Pattern does not match"));
          flag++;
        }
        i++;
      }
      if (flag==0) //No mismatches found, string recognized
      {
        stringToFile(logFile, dataString);
        Alarm(GLed, 4000, 500);
      }
      else //Unknown barcode scanned
      {
        Serial.println(F("Barcode not recognized"));
        //stringToFile(debugFile, dataString);
        Alarm(RLed, 2000, 1000);
      }
    }
    dataString = "";
    strSize=0;
    flag=0;
  }
  //end of barcode section


  // if it's a BT command
  else if (dataString2[0]>31)
  {
    Alarm(GLed, 1000, 100);
    if(bt_mode==false)
    {
    digitalWrite(RLed, HIGH);
    digitalWrite(GLed, HIGH);
    }
    Serial.print(F("BT command received: "));
    Serial.println(dataString2);
    stringToFile(debugFile, dataString2);

    if (dataString2[0]=='S'&& dataString2[1]=='E'&& dataString2[2]=='T' && bt_mode==true)  //SETTINGS
    {
      switch (dataString2[3])
      {
      case 'I':  //change scanner ID
        if (strSize2==6)
        {
          cfgID[0]=char(dataString2[4]);
          cfgID[1]=char(dataString2[5]);
          Serial.print(F("Scanner ID set to: "));
          Serial.println(cfgID);
          confUpdate();
        }
        break;

      case 'C':  //change control point
        if (strSize2==6)
        {
          cfgCP[0]=dataString2[4];
          cfgCP[1]=dataString2[5];
          Serial.print(F("Control Point # set to: "));
          Serial.println(cfgCP);
          confUpdate();
        }
        break;

      case 'L':  //Set Barcode length checking
        if (strSize2==5)
        {
          cfgStrLCheck=dataString2[4];
          Serial.print(F("String length check set to: "));
          Serial.println(cfgStrLCheck);
          confUpdate();
        }
        break;

      case 'N':  //Set char presence checking
        if (strSize2==5)
        {
          cfgNumOnly=dataString2[4];
          Serial.print(F("Numbers only set to: "));
          Serial.println(cfgNumOnly);
          confUpdate();
        }
        break;

      case 'P':  //Set Barcode pattern
        //i=0;
        cfgStrSize=0;
        cfgPattern="";
        while (dataString2[4+cfgStrSize]>31)
        {
          cfgPattern+=dataString2[4+cfgStrSize];
          cfgStrSize++;
        }
        Serial.print(F("Barcode pattern set to: "));
        Serial.println(cfgPattern);
        Serial.print(F("Pattern length: "));
        Serial.println(cfgStrSize, DEC);
        confUpdate();
        break;

      case 'T':  //change current time
        char tm[9], dt[13];
        Serial.println(F("Input current date and time \"Mon dd yyyy hh:mm:ss\" (example: \"Dec 06 2014 12:04:00\")"));
        key2=32;
        strSize2=0;
        dataString2="";
        //while (ScannerSerial.available()) ScannerSerial.read();  // clear the BlueTooth buffer
        ScannerSerial.flush();
        delay(100);
        while (key2>31)
        {
          if (Serial.available())
          {
            key2=Serial.read();
            if (key2>31)
            {
              dataString2+=key2;
              strSize2++;
            }
          }
        }
        if (strSize2==20)
        { 
          for(i=0;i<12;i++)
          {
            dt[i]=dataString2[i];
          }
          dt[12]=0;

          for(i=0;i<8;i++)
          {
            tm[i]=dataString2[i+12];
          }
          tm[8]=0;
          rtc.adjust(DateTime(dt, tm));
        }

        break;
      }
    }
    else if (dataString2[0]=='G'&& dataString2[1]=='E'&& dataString2[2]=='T')  //DOWNLOAD data
    {
      switch (dataString2[3])
      {
      case 'T':  //get current time
        Serial.print(F("Current time is: "));
        Serial.println(GetDateTime());
        break;

      case 'S':  //get current settings
        confPrint();

        dataFile = SD.open(logFile, FILE_READ);
        if (dataFile)
        {
          Serial.print(logFile);
          Serial.print(F(" size: "));
          Serial.println(dataFile.size());
          dataFile.close();
        }
        else errOpen(logFile);

        dataFile = SD.open(debugFile, FILE_READ);
        if (dataFile)
        {
          Serial.print(debugFile);
          Serial.print(F(" size: "));
          Serial.println(dataFile.size());
          dataFile.close();
        }
        else errOpen(debugFile);
        break;

      case 'L':  //get all barcode log
        getLog(logFile);
        break;

      case 'D':  //get all debug log
        getLog(debugFile);
        break;

      case '#':  //get selected string by number
        long int linenum;
        String numString="";
        strSize=5;
        while (dataString2[strSize]>='0' && dataString2[strSize]<='9')
        {
          numString+=dataString2[strSize];
          strSize++;
        }
        linenum=numString.toInt();
        if (dataString2[4]=='L') getLogLine(logFile, linenum);  //from LOG file
        if (dataString2[4]=='D') getLogLine(debugFile, linenum);  //from DEBUG file
        break;
      }
    }
    else if (dataString2[0]=='D' && dataString2[1]=='E' && dataString2[2]=='L' && dataString2[3]=='L' && dataString2[4]=='O' && dataString2[5]=='G' && bt_mode==true)  //Remove datalog.txt and errors.txt
    {
      if (SD.remove(logFile)==true) 
      {
        Serial.print(logFile);
        Serial.println(F(" deleted"));
      }
      else 
      {
        Serial.print(F("Error deleting "));
        Serial.println(logFile);
      }
      if (SD.remove(debugFile)==true)
      {
        Serial.print(debugFile);
        Serial.println(F(" deleted"));
      }
      else
      {
        Serial.print(F("Error deleting "));
        Serial.println(debugFile);
      }
    }
    else if (dataString2[0]=='E'&& dataString2[1]=='N'&& dataString2[2]=='D' && bt_mode==true)  //break BT operation mode
    {
      Serial.println(F("BlueTooth operation mode ended."));
      bt_mode=false;
      Alarm(GLed, 4000, 200);
    }
    dataString2 = "";
    strSize2=0;
    if (bt_mode==false)
    {
    digitalWrite(RLed, LOW);
    digitalWrite(GLed, LOW);
    }
  }
  //end of BT section
}


String GetDateTime()  //assemble time+date string
{
  String TimeString = "";
  DateTime now = rtc.now();
  if (now.hour()>23 || now.hour()<0 || now.minute()>59 || now.minute()<0 || now.second()>59 || now.second()<0 || now.month()>12 || now.month()<0 || now.day()>31 || now.day()<0 || now.year()<2015 || now.year()>2020)
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
  TimeString += F(", ");
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
  //save LED status
  boolean r,g;
  r=digitalRead(RLed);
  g=digitalRead(GLed);
  digitalWrite(led, HIGH);
  tone(Spk, freq, pause);
  delay(pause);
  digitalWrite(led, LOW);
  //restore LED status
  digitalWrite(RLed, r);
  digitalWrite(GLed, g);
  if (bt_mode==true)
  {
    digitalWrite(RLed, HIGH);
    digitalWrite(GLed, HIGH);
  }
  else
  {
    digitalWrite(RLed, LOW);
    digitalWrite(GLed, LOW);
  }
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
  Serial.print(F("-Pattern length: "));
  Serial.println(cfgStrSize, DEC);
  Serial.print(F("BT mode: "));
  Serial.println(String(bt_mode));
  //Serial.println(GetDateTime());
}


void getLog(char fileName[])  //send full file to serial with line# and CRC8
{
  char c;
  byte crc=0;
  dataFile = SD.open(fileName, FILE_READ);
  if (dataFile)
  {
    long int linenum=0;
    Serial.print(fileName);
    Serial.println(F(" sending"));
    Serial.print(F("File size: "));
    Serial.println(dataFile.size());
    Serial.println(F("===="));
    Serial.print("#" + String(linenum) + ", ");
    while(dataFile.available())
    {
      c = dataFile.read();
      if (c!=0x0D && c!=0x0A) Serial.print(c);
      else
      {
        linenum++;
        dataFile.read();
        Serial.println("");
        if(dataFile.available()) Serial.print("#" + String(linenum) + ", ");
      }
    }
    dataFile.close();
    Serial.println(F("===="));
    Serial.print(fileName);
    Serial.println(F(" sent"));
  }
  else
  {
    errOpen(fileName);
  }
}


void getLogLine(char fileName[], long int line)  //send line from file to serial with line# and CRC8
{
  long int linenum=0;
  char c;
  dataFile = SD.open(fileName, FILE_READ);
  if (dataFile)
  {
    Serial.print(fileName);
    Serial.print(F(" sending, string #"));
    Serial.println(line);
    Serial.println(F("===="));
    while(linenum<line && dataFile.available())
    {
      c=dataFile.read();
      if (c==0x0D || c==0x0A)
      {
        linenum++;
        dataFile.read();
      }
    }
    Serial.print(F("#"));
    Serial.print(String(linenum) + F(", "));
    do {
      c = dataFile.read();
      if (c>31)
      {
        Serial.print(c);
      }
    } 
    while (c!=0x0D && c!=0x0A && dataFile.available());
    Serial.println(F("\r\n===="));
    dataFile.close();
  }
  else errOpen(fileName);
}


void stringToFile(char fileName[], String logString)  //print string to file with write verification
{
  String logString2="";
  char c=0;
  dataFile = SD.open(fileName, FILE_WRITE);
  if (dataFile)
  {
    logString = String(cfgID) + F(", ") + String(cfgCP) + F(", ") + logString + F(", ") + GetDateTime();
    logString = logString + F(", CRC8=") + String(crcCalc(logString));
    dataFile.println(logString);
    dataFile.seek(dataFile.size()-logString.length()-2);  //Check if the written data is the same as scanned.
    while (dataFile.available())
    {
      c=dataFile.read();
      if (c>31) logString2 += c;
    }
    dataFile.close();
    if (logString != logString2) //Record is not corect
    {
      Serial.print(fileName);
      Serial.println(F(" write verification error!"));
      Alarm(RLed, 2000, 5000);
      return;
    }
    return;
  }
  else
  {
    errOpen(fileName);
    return;
  }
}


byte crcCalc(String inString)
{
  byte crc=0;
  int i=0;
  while (i<inString.length())
  {
    for (byte tempI = 8; tempI; tempI--)
    {
      byte sum = (crc ^ inString[i]) & 0x01;
      crc >>= 1;
      if (sum)
      {
        crc ^= 0x8C;
      }
      inString[i] >>= 1;
    }
    i++;
  }
  return(crc);
}



