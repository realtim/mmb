#include <Wire.h>
#include <ds3231.h>
#include <SPI.h>
#include <MFRC522.h>
#include <EEPROM.h>
#include<SPIFlash.h>

const uint8_t vers = 104; //версия, номер пишется в чипы
const uint8_t LED = 4; //светодиод
const uint8_t BUZ = 3; //пищалка
const uint8_t VCC_C = 5; //питание часов кроме батарейного
const uint8_t RST_PIN = 9; //рфид модуль
const uint8_t SS_PIN = 10; //рфид модуль
const uint16_t eepromAdrStantion = 800; //номер станции в памяти станции
const uint16_t eepromRegime = 860; //номер режима в памяти станции

//страницы в чипе. 0-7 служебные, 8-127 для отметок
const uint8_t pageUID = 0; 
const uint8_t pageInit = 4;
const uint8_t pagePass = 5;
const uint8_t pageInit1 = 6;
const uint8_t pageInit2 = 7;
const uint8_t firstPage = 8;

//параметры чипов
const uint8_t ntagValue = 130;
const uint8_t ntagType = 215;
const uint8_t maxPage = 127;

//размер пакетов
const uint8_t packetSize = 32;
const uint8_t dataSize = 27;

//параметры для обработки входящих и исходящих данных
uint8_t function = 0;
uint8_t serialBuffer[packetSize];
uint8_t dataBuffer[dataSize];
uint8_t dataCount = 2;
uint8_t packetCount = 0;

//станция запоминает последюю отметку сюда
uint8_t lastChip[16];
uint32_t lastTime= 0;

//по умолчанию номер станции и режим.
uint8_t regime = 0;
uint8_t stantion = 0; 
const uint32_t maxTimeInit = 600000UL; //одна неделя

uint8_t dump[16];

//параметры для хранения маски в режиме 1.
uint8_t tempTeamMask[8];
uint8_t tempNumTeam0;
uint8_t tempNumTeam1;

SPIFlash SPIflash(8); //флэш-память

//рфид-модуль
MFRC522::StatusCode status;
MFRC522 mfrc522(SS_PIN, RST_PIN); // Create MFRC522 instance

//хранение времени
struct ts t; //time


void setup () {
  
  Serial.begin(9600);

  //даем питание на флэш
  pinMode(7,OUTPUT);
  digitalWrite(7,HIGH);delay(1);
  SPIflash.begin();

  //даем питание на часы
  pinMode(VCC_C, OUTPUT);
  digitalWrite(VCC_C, HIGH);delay(1);
  DS3231_init(DS3231_INTCN);
  DS3231_get(&t);

  //читаем номер станции из памяти
  stantion = eepromread(eepromAdrStantion); //Read the station number from the EEPROM
  if (stantion==255){
    stantion=0;
  }

  //читаем номер режима из памяти
  regime = eepromread(eepromRegime);
  if (regime==255){
    regime=0;
  }
  
  beep(800,1);

}


void loop ()
{
  /*
   * В цикле сначала считывааем данные из порта, если они есть
   * проверяем, совпадаеют ли стартовые байты. Ищем соответсвующую функцию и обабатываем её
   * Если режим станци не нулевой, то станция работает также как кп - автоматически делает отметки на чипы
   */
  if (Serial.available() > 0){
    
    for (uint8_t y=0; y<packetSize; y++) serialBuffer[y]=0;
    Serial.readBytes(serialBuffer, packetSize);

    for (uint8_t i=0;i<dataSize;i++){
      dataBuffer[i]=serialBuffer[i+4];
    }

    if (serialBuffer[0]==0xFE &&
        serialBuffer[1]==0xFE &&
        serialBuffer[2]==0xFE &&
        serialBuffer[3]==0xFE 
        ){
          findFunc();  
        }
    }
  
  if (regime != 0){ 
      if (stantion!=0) {
      rfid();

    }
  } 
} 

//запись в память с мажоритальным резервированием
void eepromwrite (uint16_t adr, uint8_t val) {
  for (uint8_t i = 0; i < 3; i++) {
    EEPROM.write(adr + i, val);
  }
}

//считывваание из памяти с учетом мажоритального резервирования
uint8_t eepromread(uint16_t adr) {
  if (EEPROM.read(adr) == EEPROM.read(adr + 1) ||
      EEPROM.read(adr) == EEPROM.read(adr + 2)) {
    return EEPROM.read(adr);
  }
  else if (EEPROM.read(adr + 1) == EEPROM.read(adr + 2)) {
    return EEPROM.read(adr + 1);
  }
  else {
   
    return 0;
  }

} 

//сигнал станции, длительность в мс и число повторений
void beep(uint16_t ms, uint8_t n) {

  pinMode (LED, OUTPUT);
  pinMode (BUZ, OUTPUT);

  for (uint8_t i = 0; i < n; i++) {
    digitalWrite (LED, HIGH);
    tone (BUZ, 4000, ms);
    delay (ms);
    digitalWrite (LED, LOW);
    if ((n - i) != 0) {
      delay(ms);
      }
  }

} 

//формирование буфера для передачи. Если размер буфера превысит 32, произойдет передача пакета

void addData(uint8_t data,uint8_t func){
  
   dataBuffer[dataCount] = data;
   
   if (dataCount == dataSize-1){
    sendData(func,packetCount+30);
   }
   else{
    dataCount++;
   }
  
}

//передача пакета данных.
void sendData(uint8_t func, uint8_t leng){
  
  serialBuffer[0]=serialBuffer[1]=serialBuffer[2]=serialBuffer[3]=0xFE;
  
  dataBuffer[0] = func;
  if (dataCount == dataSize-1) dataBuffer[1] = 128;
  else dataBuffer[1]=dataCount-2;

  serialBuffer[packetSize-1] = 0;
  
  for (uint8_t i = 0; i<dataSize;i++){
    serialBuffer[i+4]=dataBuffer[i];
    dataBuffer[i]=0;
  }
  dataCount = 2;
  
  Serial.write(serialBuffer, packetSize);
  
}

//очистка буфера данных
void clearBuffer(){
  dataCount = 2;
  for(uint8_t j=0;j<dataSize;j++) dataBuffer[j]=0; 
}

//поиск функции
void findFunc(){
  switch (serialBuffer[4]){
    case 0x80:
      setMode();
      break;
    case 0x81:
      setTime();
      break;
    case 0x82:
      resetStantion();
      break;
    case 0x83:
      getStatus();
      break;
    case 0x84:
      initChip();
      break;
    case 0x85:
      getLastTeams();
      break;
    case 0x86:
      getChipHistory();
      break;
    case 0x87:
      getStantionClones();
      break;
    case 0x88:
      updateTeamMask();
      break;
    case 0x89:
      writeMasterChip();
      break;
  }

}

//реакция на команды от станции
const uint8_t OK = 0;
const uint8_t WRONG_STATION = 1;
const uint8_t READ_ERROR = 2;
const uint8_t WRITE_ERROR = 3;
const uint8_t LOW_INIT_TIME = 4;
const uint8_t WRONG_CHIP = 5;
const uint8_t NO_CHIP = 6;

//обработка ошибок. формирование пакета с сообщением о ошибке
void sendError(byte erro, byte function){
  clearBuffer();
  DS3231_get(&t);
  uint32_t tempT = t.unixtime;

  addData(erro,function);
  addData((t.unixtime&0xFF000000)>>24,function);
  addData((t.unixtime&0x00FF0000)>>16,function);
  addData((t.unixtime&0x0000FF00)>>8,function);
  addData(t.unixtime&0x000000FF,function);

  sendData (function,dataCount);
  packetCount = 0;
}

//установка режима
void setMode(){
  
  function = 0x90;
  //Если номер станции не совпадает с присланным в пакете, то режим не меняется
  if (stantion != dataBuffer[3]){
    sendError(WRONG_STATION,function);
    return;
  }

  regime = dataBuffer[2];
  eepromwrite(eepromRegime,regime);
  
  
  clearBuffer();

  //формирование пакета данных.
  DS3231_get(&t);
  uint32_t tempT = t.unixtime;
  
  addData(OK,function);
  addData((t.unixtime&0xFF000000)>>24,function);
  addData((t.unixtime&0x00FF0000)>>16,function);
  addData((t.unixtime&0x0000FF00)>>8,function);
  addData(t.unixtime&0x000000FF,function);

  sendData (function,dataCount);
  packetCount = 0;
   
}

//обновление времени на станции
void setTime(){
  function = 0x91;
  
  t.mon = dataBuffer[3];
  t.year = dataBuffer[2]+2000;
  t.mday = dataBuffer[4];
  t.hour = dataBuffer[5];
  t.min = dataBuffer[6];
  t.sec = dataBuffer[7];
  
  delay(1);
  DS3231_set(t); //correct time
  
  clearBuffer();

  DS3231_get(&t);
  uint32_t tempT = t.unixtime;
  
  addData(OK,function);
  addData((t.unixtime&0xFF000000)>>24,function);
  addData((t.unixtime&0x00FF0000)>>16,function);
  addData((t.unixtime&0x0000FF00)>>8,function);
  addData(t.unixtime&0x000000FF,function);

  sendData (function,dataCount);
  packetCount = 0;
  
}

//сброс настроек станции
void resetStantion(){
  function = 0x92;

  //сброс произойдет только в случае совпадения номера станции в пакете
  if (stantion!= dataBuffer[3]){
    sendError(WRONG_STATION,function);
    return;
  }

  regime =0;
  eepromwrite (eepromRegime, regime);
  stantion = dataBuffer[2];
  eepromwrite (eepromAdrStantion, stantion);

  SPIflash.eraseChip();
    
  clearBuffer();
  
  DS3231_get(&t);
  uint32_t tempT = t.unixtime;
  
  addData(OK,function);
  addData((t.unixtime&0xFF000000)>>24,function);
  addData((t.unixtime&0x00FF0000)>>16,function);
  addData((t.unixtime&0x0000FF00)>>8,function);
  addData(t.unixtime&0x000000FF,function);

  sendData (function,dataCount);
  packetCount = 0;
  
}

/*
 * выдает статус: время на станции, номер станции, номер режима, число отметок, время последней страницы
 */
void getStatus(){

  function = 0x93;
  clearBuffer();

  
  DS3231_get(&t);
  uint32_t tempT = t.unixtime;
  
  addData(OK,function);
  addData((t.unixtime&0xFF000000)>>24,function);
  addData((t.unixtime&0x00FF0000)>>16,function);
  addData((t.unixtime&0x0000FF00)>>8,function);
  addData(t.unixtime&0x000000FF,function);

  addData(regime,function);

  addData(stantion,function);

  uint16_t totalChip =0;
  for (uint16_t i=0; i< 20000;i+=20){
    if (SPIflash.readByte(i)!=255) totalChip++;
  }

  addData((totalChip&0xFF00)>>8,function);
  addData(totalChip&0x00FF,function);

  addData ((lastTime&0xFF000000)>>24,function);
  addData ((lastTime&0x00FF0000)>>16,function);
  addData ((lastTime&0x0000FF00)>>8,function);  
  addData (lastTime&0x000000FF,function);

  sendData (function,dataCount);
  packetCount = 0;
}


//инициализация чипа
void initChip(){

  function = 0x94;
  
  SPI.begin();      // Init SPI bus
  mfrc522.PCD_Init();   // Init MFRC522
 // Look for new cards
  if ( ! mfrc522.PICC_IsNewCardPresent()  ) {
    sendError(NO_CHIP,function);
    return;
  }
  // Select one of the cards
  else if ( ! mfrc522.PICC_ReadCardSerial()  ) {
    sendError(NO_CHIP,function);
    return;
  }

  
 
 if (!ntagRead(pagePass)  ){
    sendError(READ_ERROR,function);
    return;
 }

  uint32_t initTime = dump[0];
  initTime <<= 8;
  initTime += dump[1];
  initTime <<= 8;
  initTime += dump[2];
  initTime <<= 8;
  initTime += dump[3];

  DS3231_get(&t);

  //инициализация сработает только если время инициализации записанное уже на чипе превышает неделю с нанешнего времени
  if ((t.unixtime-initTime) < maxTimeInit){
    sendError(LOW_INIT_TIME,function);
    return;
  }
 

  byte Wbuff[] = {255,255,255,255};
  
  for (byte page=4; page < ntagValue;page++){
      
      if (!ntagWrite(Wbuff,page)){
          sendError(WRITE_ERROR,function);
          return;
      }
 }
   

 for (byte i=0;i<4;i++) Wbuff[i]=0;
  
 for (byte page=4; page < ntagValue;page++){
      if (!ntagWrite(Wbuff,page)){
          sendError(WRITE_ERROR,function);
          return;
      }
 }

 byte dataBlock5[] = {dataBuffer[2],dataBuffer[3],ntagType,vers};
  if(!ntagWrite(dataBlock5,pageInit)){
     sendError(WRITE_ERROR,function);
     return;
  }
  
  byte dataBlock2[] = {dataBuffer[4],dataBuffer[5],dataBuffer[6],dataBuffer[7]};
  if(!ntagWrite(dataBlock2,pagePass)){
     sendError(WRITE_ERROR,function);
     return;
  }
  
  byte dataBlock3[] = {dataBuffer[8],dataBuffer[9],dataBuffer[10],dataBuffer[11]};
  if(!ntagWrite(dataBlock3,pageInit1)){
     sendError(WRITE_ERROR,function);
     return;
  }

  byte dataBlock4[] = {dataBuffer[12],dataBuffer[13],dataBuffer[14],dataBuffer[15]};
  if(!ntagWrite(dataBlock4,pageInit2)){
     sendError(WRITE_ERROR,function);
     return;
  }
    
  clearBuffer();

  DS3231_get(&t);
  uint32_t tempT = t.unixtime;

  if (!ntagRead(0)){
    sendError(READ_ERROR,function);
    return;
  }
  
  addData(OK,function);
  addData((t.unixtime&0xFF000000)>>24,function);
  addData((t.unixtime&0x00FF0000)>>16,function);
  addData((t.unixtime&0x0000FF00)>>8,function);
  addData(t.unixtime&0x000000FF,function);


  for (uint8_t h=0;h<7;h++){
    addData(dump[h],function);
  }
  

  sendData (function,dataCount);
  packetCount = 0;
  SPI.end();
}


void  getLastTeams(){

  function = 0x95;
  clearBuffer();

  DS3231_get(&t);
  uint32_t tempT = t.unixtime;
  
  addData(OK,function);
  addData((t.unixtime&0xFF000000)>>24,function);
  addData((t.unixtime&0x00FF0000)>>16,function);
  addData((t.unixtime&0x0000FF00)>>8,function);
  addData(t.unixtime&0x000000FF,function);


  addData(lastChip[0],function);
  addData(lastChip[1],function);
  for (byte r=4; r<16;r++){
    addData(lastChip[r],function);
  }
  addData((lastTime&0xFF000000)>>24,function);
  addData((lastTime&0x00FF0000)>>16,function);
  addData((lastTime&0x0000FF00)>>8,function);
  addData(lastTime&0x000000FF,function);
  

  sendData (function,dataCount);
  packetCount = 0;
}


 void getChipHistory(){

  function = 0x96;
  clearBuffer();


  DS3231_get(&t);
  uint32_t tempT = t.unixtime;
  
  addData(OK,function);
  addData((t.unixtime&0xFF000000)>>24,function);
  addData((t.unixtime&0x00FF0000)>>16,function);
  addData((t.unixtime&0x0000FF00)>>8,function);
  addData(t.unixtime&0x000000FF,function);

  uint32_t timeFrom = dataBuffer[2];
  timeFrom <<=8;
  timeFrom += dataBuffer[3];
  timeFrom <<= 8;
  timeFrom += dataBuffer[4];
  timeFrom <<= 8;
  timeFrom += dataBuffer[5]; 

  for (uint16_t chipN=1;chipN<2000;chipN++){
    
    uint32_t shortF = chipN;
    shortF = shortF*20; 

     
    if (SPIflash.readByte(shortF)!=255){


      uint32_t markTime = SPIflash.readByte(shortF+16);
      markTime <<= 8;
      markTime += SPIflash.readByte(shortF+17);
      markTime <<= 8;
      markTime += SPIflash.readByte(shortF+18);
      markTime <<= 8;
      markTime += SPIflash.readByte(shortF+19);

      if (markTime > timeFrom){
        
        uint32_t pageF = chipN;
        pageF = pageF*1000 + 100000UL;
      
        addData(SPIflash.readByte(pageF+4*4),function);
        addData(SPIflash.readByte(pageF+4*4+1),function);
  
        uint8_t startTime = SPIflash.readByte(pageF+5*4);
        uint8_t startTime2 = SPIflash.readByte(pageF+5*4+1);
        for (byte gh =24;gh<32;gh++){
          addData(SPIflash.readByte(pageF+gh),function);
        }
        for (byte p = 8;p<130;p++){
          uint8_t st = SPIflash.readByte(pageF+p*4);
          if (st!=0 && st!=255){
            addData(st,function);
            uint8_t t2 = SPIflash.readByte(pageF+p*4+1);
            if (t2<startTime2) addData(startTime+1,function);
            else addData (startTime,function);
            addData (t2,function);
            addData(SPIflash.readByte(pageF+p*4+2),function);
            addData(SPIflash.readByte(pageF+p*4+3),function);
            
          }
        }
         sendData (function,dataCount);
         packetCount = 0;
      }
    }
  }

}


 void getStantionClones(){

  function = 0x97;
  clearBuffer();
  
  SPI.begin();      // Init SPI bus
  mfrc522.PCD_Init();   // Init MFRC522
  // Look for new cards
  if ( ! mfrc522.PICC_IsNewCardPresent()) {
    sendError(NO_CHIP,function);
    return;
  }
  // Select one of the cards
  if ( ! mfrc522.PICC_ReadCardSerial()) {
    sendError(NO_CHIP,function);
    return;
  }

  if(!ntagRead(pageInit) ) {
    sendError(READ_ERROR,function);
    return;
  }

  addData(dump[0], function);

  for (uint8_t page = 5; page < ntagValue; page++) {
    if (!ntagRead(page) ) {
      sendError(READ_ERROR,function);
      return;
    }

    for (uint8_t i = 0; i < 4; i++) {
      for (uint8_t y = 0; y < 8; y++) {
        uint8_t temp = dump[i];
        temp = temp >> y;
        if (temp%2 == 1) {
        
          uint16_t num = (page - 5)*32 + i*8 + y;
          uint8_t first = (num&0xFF00)>>8;
          uint8_t second = num&0x00FF; 
          addData(first, function);
          addData(second, function);
        }
      }
    }
  }

  DS3231_get(&t);
  uint32_t tempT = t.unixtime;

  addData(OK,function);
  addData((t.unixtime&0xFF000000)>>24,function);
  addData((t.unixtime&0x00FF0000)>>16,function);
  addData((t.unixtime&0x0000FF00)>>8,function);
  addData(t.unixtime&0x000000FF,function);

  sendData (function,dataCount);
  packetCount = 0;
 }


void updateTeamMask(){
  function = 0x98;
  
  for (byte i=0;i<8;i++){
    tempTeamMask[i] = dataBuffer[i-4];
  }
  tempNumTeam0 = dataBuffer[2];
  tempNumTeam1 = dataBuffer[3];

  for(byte i=0;i<8;i++){
    tempTeamMask[i]=dataBuffer[i+4];
  }

  if (regime==1) return;

  if(!ntagRead(pageInit) ){
    sendError(READ_ERROR,function);
    return;
  }
  
  SPI.begin();      // Init SPI bus
  mfrc522.PCD_Init();   // Init MFRC522
 // Look for new cards
  if ( ! mfrc522.PICC_IsNewCardPresent()) {
    sendError(NO_CHIP,function);
    return;
  }
  // Select one of the cards
  if ( ! mfrc522.PICC_ReadCardSerial()) {
    sendError(NO_CHIP,function);
    return;
  }

  if (dump[0]==tempNumTeam0 && dump[1]==tempNumTeam1){
    
    byte dataBlock[4]    = {dataBuffer[4],dataBuffer[5],dataBuffer[6],dataBuffer[7]};
    if(!ntagWrite(dataBlock,pageInit1) ){
      sendError(WRITE_ERROR,function);
      return;
    }
  
  
    byte dataBlock2[] = {dataBuffer[8],dataBuffer[9],dataBuffer[10],dataBuffer[11]};
    if(!ntagWrite(dataBlock2,pageInit2) ){
      sendError(WRITE_ERROR,function);
      return;
    }

  }
  else{
      sendError(WRONG_CHIP,function);
      return;
  }




  SPI.end();
 
  clearBuffer();


  DS3231_get(&t);
  uint32_t tempT = t.unixtime;

  addData(OK,function);
  addData((t.unixtime&0xFF000000)>>24,function);
  addData((t.unixtime&0x00FF0000)>>16,function);
  addData((t.unixtime&0x0000FF00)>>8,function);
  addData(t.unixtime&0x000000FF,function);

  

  sendData (function,dataCount);
  packetCount = 0;


}

void writeMasterChip(){
  function = 0x99;
  
  SPI.begin();      // Init SPI bus
  mfrc522.PCD_Init();   // Init MFRC522
 // Look for new cards
  if ( ! mfrc522.PICC_IsNewCardPresent()) {
      sendError(NO_CHIP,function);
      return;
  }
  // Select one of the cards
  if ( ! mfrc522.PICC_ReadCardSerial()) {
      sendError(NO_CHIP,function);
      return;
  }

  uint8_t pass0 = dataBuffer[13];
  uint8_t pass1 = dataBuffer[14];
  uint8_t pass2 = dataBuffer[15];
  uint8_t setting = dataBuffer[16];   
        

  if (dataBuffer[2]==253){
          
            byte Wbuff[] = {255,255,255,255};
            
            for (byte page=4; page < ntagValue;page++){
                
                if (!ntagWrite(Wbuff,page) ){
                   sendError(WRITE_ERROR,function);
                   return;
                }
           }
          
           byte Wbuff2[] = {0,0,0,0};
            
           for (byte page=4; page < ntagValue;page++){
                if (!ntagWrite(Wbuff2,page) ){
                    sendError(WRITE_ERROR,function);
                    return;
                }
           }
                    byte dataBlock[4]    = {0, 253, 255,vers};
            if(!ntagWrite(dataBlock,pageInit) ){
               sendError(WRITE_ERROR,function);
               return;
            }
          
          
            byte dataBlock2[] = {pass0, pass1, pass2,0};
            if(!ntagWrite(dataBlock2,pagePass) ){
               sendError(WRITE_ERROR,function);
               return;
            }
  }

  if (dataBuffer[2]==252){
                byte dataBlock[4]    = {0, 252, 255,vers};
          if(!ntagWrite(dataBlock,pageInit) ){
                    sendError(WRITE_ERROR,function);
                   return;
          }
        
        
          byte dataBlock2[] = {pass0, pass1, pass2,0};
          if(!ntagWrite(dataBlock2,pagePass) ){
             sendError(WRITE_ERROR,function);
             return;
          }

  }

  if (dataBuffer[2]==251){
        byte dataBlock[4]    = {0,251,255,vers};
      if(!ntagWrite(dataBlock,pageInit) ){
         sendError(WRITE_ERROR,function);
         return;
      }
    
    
      byte dataBlock2[] = {pass0,pass1,pass2,0};
      if(!ntagWrite(dataBlock2,pagePass) ){
         sendError(WRITE_ERROR,function);
         return;
      }
    
      byte dataBlock3[] = {dataBuffer[9],0,0,0};
      if(!ntagWrite(dataBlock3,pageInit1) ){
         sendError(WRITE_ERROR,function);
         return;
      }
  }

  if (dataBuffer[2]==254){
        byte dataBlock[4]    = {0,254,255,vers};
        if(!ntagWrite(dataBlock,pageInit) ){
           sendError(WRITE_ERROR,function);
           return;
        }
      
      
        byte dataBlock2[] = {dataBuffer[10],dataBuffer[11],dataBuffer[12],0};
        if(!ntagWrite(dataBlock2,pageInit1) ){
           sendError(WRITE_ERROR,function);
           return;
        }
      
        
        byte dataBlock3[] = {pass0,pass1,pass2,setting};
        if(!ntagWrite(dataBlock3,pagePass) ){
           sendError(WRITE_ERROR,function);
           return;
        }
  }

  if (dataBuffer[2]==250){
              byte dataBlock[4]    = {0,250,255,vers};
        if(!ntagWrite(dataBlock,pageInit) ){
           sendError(WRITE_ERROR,function);
           return;
        }
      
      
        byte dataBlock2[] = {pass0,pass1,pass2,0};
        if(!ntagWrite(dataBlock2,pagePass) ){
           sendError(WRITE_ERROR,function);
           return;
        }
      
        byte dataBlock3[] = {dataBuffer[4],dataBuffer[3],dataBuffer[5],0};
        if(!ntagWrite(dataBlock3,pageInit1) ){
           sendError(WRITE_ERROR,function);
           return;
        }
      
        byte dataBlock4[] = {dataBuffer[6],dataBuffer[7],dataBuffer[8],0};
        if(!ntagWrite(dataBlock4,pageInit2) ){
          sendError(WRITE_ERROR,function);
          return;
        }
  }
  
  clearBuffer(); 
  DS3231_get(&t);
  uint32_t tempT = t.unixtime;

  addData(OK,function);
  addData((t.unixtime&0xFF000000)>>24,function);
  addData((t.unixtime&0x00FF0000)>>16,function);
  addData((t.unixtime&0x0000FF00)>>8,function);
  addData(t.unixtime&0x000000FF,function);

  

  sendData (function,dataCount);
  packetCount = 0;

}


//MFRC522::StatusCode MFRC522::MIFARE_Read
bool ntagWrite (uint8_t *dataBlock, uint8_t pageAdr){

  const uint8_t sizePageNtag = 4;
  status = (MFRC522::StatusCode) mfrc522.MIFARE_Ultralight_Write(pageAdr, dataBlock, sizePageNtag);
  if (status != MFRC522::STATUS_OK) {
    return false;
  }

  uint8_t buffer[18];
  uint8_t size = sizeof(buffer);

  status = (MFRC522::StatusCode) mfrc522.MIFARE_Read(pageAdr, buffer, &size);
  if (status != MFRC522::STATUS_OK) {
    return false;
  }
 
  for (uint8_t i = 0; i < 4; i++) {
    if (buffer[i]!=dataBlock[i]) return false;
  }
  
  return true;
}

bool ntagRead (uint8_t pageAdr){
  uint8_t buffer[18];
  uint8_t size = sizeof(buffer);

  status = (MFRC522::StatusCode) mfrc522.MIFARE_Read(pageAdr, buffer, &size);
  if (status != MFRC522::STATUS_OK) {
    return false;
  }
  
  for (uint8_t i = 0; i < 16; i++) {
    dump[i]=buffer[i];
  }
  return true;
}

uint8_t tempDump[4] = {255,0,0,0};

//Writing the chip
void rfid() {
  //инициализируем переменные
  uint8_t lastNum = 0; //last writed number of stantion in memory of chip
  uint8_t newPage = 0;
  uint16_t chipNum = 0; //number of chip from 1-st block
    
  //включаем SPI ищем карту вблизи. Если не находим выходим из функции чтения чипов
  SPI.begin();      // Init SPI bus
  mfrc522.PCD_Init();    // Init MFRC522
  // Look for new cards
  if ( ! mfrc522.PICC_IsNewCardPresent()) {
    return;
  }

  // Select one of the cards
  if ( ! mfrc522.PICC_ReadCardSerial()) {
    return;
  }
  
  DS3231_get(&t);
    
  //читаем блок информации
  if(!ntagRead(pageInit)){
    return;
  }

  //Заменяем маску, хранящуюся в буфере при совпадении маски
  if (tempNumTeam0!=0 || tempNumTeam1!=0){
      function = 0x98;
      if (tempNumTeam0 == dump[0] && tempNumTeam1 == dump[1]){
        bool updateMask = false;
        
        for (int i =0;i<8;i++){
          if (tempTeamMask[i]!=dump[i+9]) updateMask = true;
        }
    
        if (updateMask){
          
          byte dataBlock[4]    = {tempTeamMask[0],tempTeamMask[1],tempTeamMask[2],tempTeamMask[3]};
          if(!ntagWrite(dataBlock,pageInit1)){
             sendError(WRITE_ERROR,function);
              return;
          }
      
      
          byte dataBlock2[] = {tempTeamMask[4],tempTeamMask[5],tempTeamMask[6],tempTeamMask[7]};
          if(!ntagWrite(dataBlock2,pageInit2)){
            sendError(WRITE_ERROR,function);
            return;
          }
        }
      }
      else {
        sendError(WRONG_CHIP,function);
        return;
      }
      tempNumTeam0 = 0;
      tempNumTeam1 = 0;
      for (byte i = 0;i<8;i++){
        tempTeamMask[i]=0;
      }

    
    clearBuffer();
    uint32_t tempT = t.unixtime;
  
    addData(OK,function);
    addData((t.unixtime&0xFF000000)>>24,function);
    addData((t.unixtime&0x00FF0000)>>16,function);
    addData((t.unixtime&0x0000FF00)>>8,function);
    addData(t.unixtime&0x000000FF,function);
    sendData (function,dataCount);
    packetCount = 0;
    
    return;
  }


  
  
  //в первых трех байтах находятся нули для обычных чипов и заданные числа для мастер-чипов
  uint8_t info = 0;
  if (dump[2]==255) {
     return; 
  }

  uint32_t timeInit = dump[4];
  timeInit = timeInit <<8;
  timeInit += dump[5];
  timeInit = timeInit <<8;
  timeInit += dump[6];
  timeInit = timeInit <<8;
  timeInit += dump[7];

  if ((t.unixtime-timeInit) > maxTimeInit){
    return;
  }

  tempDump[0] = 0;
  
  chipNum = (dump[0]<<8) + dump[1];
  if (chipNum==0) return;

  newPage = findNewPage(128);  
  
  if (newPage == 0) return;

  if (stantion == tempDump[0] && stantion!=245){
    beep(500,1);
    return;
  }
 
  if(newPage > maxPage){
    return;
  }

  if (!writeTime(newPage)){
    return;
  }

  writeFlash(chipNum);
  beep(200, 1);
  
  SPI.end();

} // end of rfid()


bool writeTime(int newPage){

   uint32_t code = stantion;
   code = code<<24;
   code += (t.unixtime&0x00FFFFFF);

   uint8_t toWrite[4] = {0,0,0,0};
   toWrite[0]=(code&0xFF000000)>>24;
   toWrite[1]=(code&0x00FF0000)>>16;
   toWrite[2]=(code&0x0000FF00)>>8;
   toWrite[3]=(code&0x000000FF);
   
   uint8_t dataBlock2[4] = {toWrite[0],toWrite[1],toWrite[2],toWrite[3]};
   
    if (ntagWrite(dataBlock2,newPage))
    {
      
      return true;
    }
    else {
      
      return false;
    }
  
}


uint8_t findNewPage(uint8_t finishpage){
  uint8_t startpage = 8;
  uint8_t page = (finishpage+startpage)/2;

 while (1) {
    if (finishpage==startpage) {
      return (finishpage);
    }
       
    page = (finishpage + startpage)/2;
   
    if(!ntagRead(page)){
      for (uint8_t i = 0; i<4 ; i++) tempDump[i] = 0;
      return 0;
     }
     
     if (dump[0]==0){
      finishpage = (finishpage - startpage)/2 + startpage;
     }
     else {
      for (uint8_t i = 0; i<4 ; i++) tempDump[i] = dump[i];
      startpage = finishpage - (finishpage - startpage)/2;
     }
  } 
}

void writeFlash (uint16_t chipNum){
  
  uint32_t pageFlash = chipNum;
  pageFlash = pageFlash*1000 + 100000UL;
  uint32_t shortFlash = chipNum;
  shortFlash = shortFlash*20; 
  for (uint8_t page =  0; page < maxPage; page = page+4){
    if(!ntagRead(page)){
      return; 
    }
   
    for (uint8_t dop =0;dop<16;dop++){
       if(dump[(dop/4)*4]!=0 || page==4){
          SPIflash.writeByte(pageFlash+page*4+dop,dump[dop]);
       }
    }
    
    if (page==4){
      
      for (uint8_t i =0;i<16;i++){
        
        SPIflash.writeByte(shortFlash+i,dump[i]);
        lastChip[i] = dump[i];
        
      }
      
      
      lastTime = t.unixtime;
      
      uint8_t byteTime[4];
      byteTime[0] = (lastTime&0xFF000000)>>24;
      byteTime[1] = (lastTime&0x00FF0000)>>16;
      byteTime[2] = (lastTime&0x0000FF00)>>8;
      byteTime[3] = (lastTime&0x000000FF);  
     
      SPIflash.writeByte(shortFlash+16,byteTime[0]);
      SPIflash.writeByte(shortFlash+17,byteTime[1]);
      SPIflash.writeByte(shortFlash+18,byteTime[2]);
      SPIflash.writeByte(shortFlash+19,byteTime[3]);
    }
  }
}
