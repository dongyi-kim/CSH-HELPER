#include <SoftwareSerial.h>

const int PIN_LED = 13;
const int PIN_BLE_RX = 3;
const int PIN_BLE_TX = 4;

const int PIN_TEMPER = 0;
const int PIN_SOUND  = 1;
const int PIN_WATER  = 2;

const int TYPE_TEMP    = 1;
const int TYPE_SOUND   = 2;
const int TYPE_WATER   = 3;

const int LEVEL_NONE   = 1;
const int LEVEL_INFO   = 2;
const int LEVEL_WARN   = 3;
const int LEVEL_DANGER = 4;


const int COMMAND_GET_ALL = 0x001;

#define LED(X) digitalWrite(PIN_LED, X)
#define LOG(X) Serial.println(X)
#define ON HIGH
#define OFF LOW

SoftwareSerial ble( PIN_BLE_RX, PIN_BLE_TX ); //RX, TX

void setup() {
  // initialize the LED pin as an output:
  Serial.begin(9600);
  while(!Serial){;}
  LOG("Serial Connected");

  pinMode(PIN_LED, OUTPUT);

  ble.begin(9600);

  LED(OFF);
  LOG("Init Done");
}

int getTemp()
{
  int reading =   analogRead(PIN_TEMPER);

  float sensorV = reading*5.0/1024.0;

  float sensorR = (( 5.0 * 10000.0 )/ sensorV ) - 10000.0;

  float kT = 1.0 / ((1.0 / (273.15 + 25.0)) + (1.0 / 4200.0) * log (sensorR / 10000.0));
 
  float cT = kT-273.15;
 
  if(cT >= 25)
    return LEVEL_DANGER;
  else if(cT >= 60)
    return LEVEL_WARN;
  else if(cT >= 40)
    return LEVEL_INFO;
  return LEVEL_NONE;
}

int getWater()
{
  int value = analogRead(PIN_WATER);
  if(value <= 460)
    return LEVEL_NONE;
  else if(value <= 550)
    return LEVEL_INFO;
  else
    return LEVEL_DANGER;
}

int getSound()
{
  Serial.println(analogRead(PIN_SOUND));
  return LEVEL_NONE;
}

void send_all()
{
  char buff[1024];
  sprintf(buff,"%d,%d,%d", getTemp(), getWater(), getSound() );
  ble.write(buff);
}

void send_command()
{
  char buff[1024];
  int index = 0 ;
  while(Serial.available())
  {
    char c = Serial.read();
    buff[index++] = c;
    buff[index] = 0;
  }
  ble.write(buff);
}

void send_noti(int type, int level)
{
  LED(ON);
  char buff[3];
  buff[2] = 0;
  buff[0] = type;
  buff[1] = level;
  ble.write(buff);
}
void loop() {
  LOG("Loop Begin");
  LED(OFF);
  Serial.println(getTemp());
  Serial.println(getWater());
  Serial.println(getSound());

  if(ble.available())
  {
    int command = ble.read();
    switch(command)
    {
      case COMMAND_GET_ALL:
        send_all();
        break;
    }
  }  

  if(getTemp() >= LEVEL_INFO)
    send_noti(TYPE_TEMP, getTemp());
  else if(getSound() >= LEVEL_WARN)
    send_noti(TYPE_SOUND, getSound());
  else if(getWater() >= LEVEL_WARN)
    send_noti(TYPE_WATER, getWater());

  LOG("Loop End");
  delay(1000);
}
