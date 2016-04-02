# DCXMonitor
Android + Arduino monitoring of a DCX/AXE motor controller output

The Arduino side consists of a Teensy 3.2 with a MAX232 to convert Serial2. This then connects to the DCX/AXE.
Values can only be read when the DCX/AXE is powered on in forward or reverse (key switch and foot pedal).
There is also an HC-05 or HC-06 Bluetooth module connected to Serial3. When it recieves any data, it replies with the current reads line by line.

The Android app has a refresh button that sends "1" to the Bluetooth module. It splits the reply lines and sets the main display to the readings.
