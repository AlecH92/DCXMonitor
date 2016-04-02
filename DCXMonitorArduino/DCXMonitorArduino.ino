double voltsPerBit = 0.1025;
double BitsPerDegree = 2.0485;
int readBytes = 0x04;//Command: Read three sequential bytes from RAM
int fromPCToAxe = 0x5B;//Address: we are sending from the Host (hex 5) to the AXE (hex B)
double kelvinToCelcius = 273.15;
double maxPercent = 100;
double maxThrottleBits = 255;
double mVoltage;
double mCurrent;
double mTemperature;
double mThrottle;

int ControllerThrottle = 0x20;
int ControllerTemperature = 0x2C;
int ControllerVoltage = 0x39;
int ControllerCurrent = 0x60;


void setup() {
  delay(1000);
  Serial.begin(9600);
  Serial2.begin(9600);
  Serial3.begin(9600);
  pinMode(13, OUTPUT);
  digitalWrite(13, HIGH);
  //updateInformation();
}

void loop() {
  //delay(5000);
  //updateInformation();
}

double getThrottle() {
  return mThrottle;
}
double getTemperature() {
  return mTemperature;
}
double getVoltage() {
  return mVoltage;
}
double getCurrent() {
  return mCurrent;
}

void sendRequest(byte requestValue) {
  byte commandbytes[7];
  commandbytes[0] = (byte) (fromPCToAxe);
  commandbytes[1] = (byte) (readBytes);
  commandbytes[2] = (byte) (requestValue); //e.g. Controller.VOLTAGE
  commandbytes[3] = (byte) (0);
  commandbytes[4] = (byte) (0);
  commandbytes[5] = (byte) (0);
  commandbytes[6] = (byte) (0); //checksum goes in here
  int checksum = 0;
  for (int i = 0; i < sizeof(commandbytes); i++) {
    checksum += commandbytes[i] & 0xFF;
  }
  checksum = (((checksum ^= 0xFF) + 1) & 0xFF); // XOR checksum and truncate to 1 byte
  commandbytes[6] = (byte) (checksum);
  //Serial.println(commandbytes);
  /*Serial.println("Debug, write: ");
  for(int i=0;i<7;i++) {
    Serial.println(commandbytes[i]);
  }
  Serial.println(""); //DEBUG*/
  for(int i=0;i<7;i++) {
    while(!Serial2.availableForWrite()) {
      delay(100);
      Serial.print("not avail for write ");Serial.println(i);
    }
    Serial2.write(commandbytes[i]);
  }
}

void readResult(byte requestValue) {
  byte readbuf[7];
  /*for(int i=0;i<7;i++) {
    while(!Serial2.available()) {
      delay(100);
      Serial.print("not avail for read ");Serial.println(i);
    }
    readbuf[i] = Serial2.read();
  }*/
  Serial2.readBytes(readbuf, 7);
  /*Serial.println("Debug, read: ");
  for(int i=0;i<7;i++) {
    Serial.println(readbuf[i]);
  }
  Serial.println();*/
  int lowbyte = (byte) readbuf[3] & 0xFF;
  int highbyte = (byte) readbuf[4] & 0xFF;
  if(requestValue == 0x39) {
    double voltage = (double) (((highbyte * 0xFF) + lowbyte) * voltsPerBit);
    voltage = (double) round(voltage * 100) / 100; // round to 2 decimal places
    mVoltage = voltage;
  }
  else if(requestValue == 0x2C) {
    double temperature = (double) (((highbyte * 0xFF) + lowbyte) / BitsPerDegree) - kelvinToCelcius;
    temperature = (double) round(temperature * 100) / 100; // round to 2 decimal places
    mTemperature = temperature;
  }
  else if(requestValue == 0x20) {
    double throttlePosition = (double) ((lowbyte * maxPercent) / maxThrottleBits);
    throttlePosition = (double) round(throttlePosition * 100) / 100; // round to 2 decimal places
    mThrottle = throttlePosition;
  }
  else if(requestValue == 0x60) {
    double current = (double) ((highbyte * 0xFF) + lowbyte);
    current = (double) round(current * 100) / 100; // round to 2 decimal places
    mCurrent = current;
  }
}

void updateInformation() {
  sendRequest(ControllerTemperature);
  delay(100);
  readResult(ControllerTemperature);
  delay(100);
  sendRequest(ControllerVoltage);
  delay(100);
  readResult(ControllerVoltage);
  delay(100);
  sendRequest(ControllerCurrent);
  delay(100);
  readResult(ControllerCurrent);
  delay(100);
  sendRequest(ControllerThrottle);
  delay(100);
  readResult(ControllerThrottle);
  delay(100);
  Serial.print("Voltage:     ");Serial.println(mVoltage);
  Serial.print("Current:     ");Serial.println(mCurrent);
  Serial.print("Temperature: ");Serial.println(mTemperature);
  Serial.print("Throttle:    ");Serial.println(mThrottle);
  Serial3.println(mVoltage);
  Serial3.println(mCurrent);
  Serial3.println(mTemperature);
  Serial3.println(mThrottle);
}

void serialEvent3() {
  Serial.println("3event");
  while(Serial3.available() > 0) {
    Serial3.read();
  }
  updateInformation();
}
