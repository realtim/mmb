// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++, C#, and Java: http://www.viva64.com

using RFID_Station_control.Properties;

using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.IO;
using System.IO.Ports;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using RfidStationControl;

namespace RFID_Station_control
{
    public partial class Form1 : Form
    {
        private const int InputCodePage = 866;
        private int _portSpeed = 38400;
        private byte[] _uartBuffer = new byte[256];
        private int _uartBufferPosition;
        private readonly ulong receiveTimeOut = 1000;

        private bool _receivingData;
        private byte _packageId;

        private readonly object _serialReceiveThreadLock = new object();
        private readonly object _serialSendThreadLock = new object();
        private readonly object _textOutThreadLock = new object();

        private volatile int _asyncFlag;
        private volatile bool _noTerminalOutputFlag;
        private volatile bool needMore;


        private int _logLinesLimit = 500;
        private string _logAutoSaveFile = "";
        private bool _logAutoSaveFlag;

        private DateTime _receiveStartTime = DateTime.Now.ToUniversalTime().ToUniversalTime();

        private UInt32 _selectedFlashSize = 4 * 1024 * 1024;
        private UInt32 _bytesPerRow = 1024;

        private static Dictionary<string, long> FlashSizeLimit = new Dictionary<string, long>
        {
            {"32 kb", 32 * 1024},
            { "64 kb" , 64 * 1024},
            { "128 kb" , 128 * 1024},
            { "256 kb" , 256 * 1024},
            { "512 kb" , 512 * 1024},
            { "1 Mb" , 1024 * 1024},
            { "2 Mb" , 2048 * 1024},
            { "4 Mb" , 4096 * 1024},
            { "8 Mb" , 8192 * 1024}
        };

        public class StationSettings
        {
            public byte FwVersion = 0;
            public byte Number = 0;
            public byte Mode = StationMode["Init"];
            public float VoltageCoefficient = 0.00578F;
            public float BatteryLimit = 3.0F;
            public byte AntennaGain = Gain["Level 80"];
            public byte ChipType = RfidContainer.ChipTypes.Types["NTAG215"];
            public UInt32 FlashSize = 4 * 1024 * 1024;
            public UInt32 TeamBlockSize = 1024;
            public int EraseBlockSize = 4096;
            public string BtName = "Sportduino-xx";
            public string BtPin = "1111";

            //режимы станции
            public readonly static Dictionary<string, byte> StationMode = new Dictionary<string, byte>
            {
            {"Init" , 0},
            { "Start" , 1},
            { "Finish" , 2}
            };

            public readonly static Dictionary<string, byte> Gain = new Dictionary<string, byte>
            {
            {"Level 0", 0},
            { "Level 16", 16},
            { "Level 32", 32},
            { "Level 48", 48},
            { "Level 64", 64},
            { "Level 80", 80},
            { "Level 96", 96},
            { "Level 112", 112}
            };
        }

        private DateTime _getStatusTime = DateTime.Now.ToUniversalTime();

        //команды
        private static class Command
        {
            public const byte SET_MODE = 0x80;
            public const byte SET_TIME = 0x81;
            public const byte RESET_STATION = 0x82;
            public const byte GET_STATUS = 0x83;
            public const byte INIT_CHIP = 0x84;
            public const byte GET_LAST_TEAMS = 0x85;
            public const byte GET_TEAM_RECORD = 0x86;
            public const byte READ_CARD_PAGE = 0x87;
            public const byte UPDATE_TEAM_MASK = 0x88;
            public const byte WRITE_CARD_PAGE = 0x89;
            public const byte READ_FLASH = 0x8a;
            public const byte WRITE_FLASH = 0x8b;
            public const byte ERASE_FLASH_SECTOR = 0x8c;
            public const byte GET_CONFIG = 0x8d;
            public const byte SET_V_COEFF = 0x8e;
            public const byte SET_GAIN = 0x8f;
            public const byte SET_CHIP_TYPE = 0x90;
            public const byte SET_TEAM_FLASH_SIZE = 0x91;
            public const byte SET_FLASH_BLOCK_SIZE = 0x92;
            public const byte SET_BT_NAME = 0x93;
            public const byte SET_BT_PIN = 0x94;
            public const byte SET_BATTERY_LIMIT = 0x95;
            public const byte SCAN_TEAMS = 0x96;
            public const byte SEND_BT_COMMAND = 0x97;
        }

        //минимальные размеры данных для команд header[6]+crc[1]+params[?]
        private static class CommandDataLength
        {
            public const byte SET_MODE = 7 + 1;
            public const byte SET_TIME = 7 + 6;
            public const byte RESET_STATION = 7 + 7;
            public const byte GET_STATUS = 7 + 0;
            public const byte INIT_CHIP = 7 + 4;
            public const byte GET_LAST_TEAMS = 7 + 0;
            public const byte GET_TEAM_RECORD = 7 + 2;
            public const byte READ_CARD_PAGE = 7 + 2;
            public const byte UPDATE_TEAM_MASK = 7 + 8;
            public const byte WRITE_CARD_PAGE = 7 + 13;
            public const byte READ_FLASH = 7 + 5;
            public const byte WRITE_FLASH = 7 + 4;
            public const byte ERASE_FLASH_SECTOR = 7 + 2;
            public const byte GET_CONFIG = 7 + 0;
            public const byte SET_V_COEFF = 7 + 4;
            public const byte SET_GAIN = 7 + 1;
            public const byte SET_CHIP_TYPE = 7 + 1;
            public const byte SET_TEAM_FLASH_SIZE = 7 + 2;
            public const byte SET_FLASH_BLOCK_SIZE = 7 + 2;
            public const byte SET_BT_NAME = 7 + 1;
            public const byte SET_BT_PIN = 7 + 1;
            public const byte SET_BATTERY_LIMIT = 7 + 4;
            public const byte SCAN_TEAMS = 7 + 2;
            public const byte SEND_BT_COMMAND = 7 + 1;
        }

        //ответы станции
        private static class Reply
        {
            public const byte SET_MODE = 0x90;
            public const byte SET_TIME = 0x91;
            public const byte RESET_STATION = 0x92;
            public const byte GET_STATUS = 0x93;
            public const byte INIT_CHIP = 0x94;
            public const byte GET_LAST_TEAMS = 0x95;
            public const byte GET_TEAM_RECORD = 0x96;
            public const byte READ_CARD_PAGE = 0x97;
            public const byte UPDATE_TEAM_MASK = 0x98;
            public const byte WRITE_CARD_PAGE = 0x99;
            public const byte READ_FLASH = 0x9a;
            public const byte WRITE_FLASH = 0x9b;
            public const byte ERASE_FLASH_SECTOR = 0x9c;
            public const byte GET_CONFIG = 0x9d;
            public const byte SET_V_COEFF = 0x9e;
            public const byte SET_GAIN = 0x9f;
            public const byte SET_CHIP_TYPE = 0xa0;
            public const byte SET_TEAM_FLASH_SIZE = 0xa1;
            public const byte SET_FLASH_BLOCK_SIZE = 0xa2;
            public const byte SET_BT_NAME = 0xa3;
            public const byte SET_BT_PIN = 0xa4;
            public const byte SET_BATTERY_LIMIT = 0xa5;
            public const byte SCAN_TEAMS = 0xa6;
            public const byte SEND_BT_COMMAND = 0xa7;
        }

        //размеры данных для ответов
        private static class ReplyDataLength
        {
            public const byte SET_MODE = 1;
            public const byte SET_TIME = 5;
            public const byte RESET_STATION = 1;
            public const byte GET_STATUS = 15;
            public const byte INIT_CHIP = 13;
            public const byte GET_LAST_TEAMS = 1;
            public const byte GET_TEAM_RECORD = 14;
            public const byte READ_CARD_PAGE = 14;
            public const byte UPDATE_TEAM_MASK = 1;
            public const byte WRITE_CARD_PAGE = 1;
            public const byte READ_FLASH = 5;
            public const byte WRITE_FLASH = 2;
            public const byte ERASE_FLASH_SECTOR = 1;
            public const byte GET_CONFIG = 21;
            public const byte SET_V_COEFF = 1;
            public const byte SET_GAIN = 1;
            public const byte SET_CHIP_TYPE = 1;
            public const byte SET_TEAM_FLASH_SIZE = 1;
            public const byte SET_FLASH_BLOCK_SIZE = 1;
            public const byte SET_BT_NAME = 1;
            public const byte SET_BT_PIN = 1;
            public const byte SET_BATTERY_LIMIT = 1;
            public const byte SCAN_TEAMS = 1;
            public const byte SEND_BT_COMMAND = 1;
        }

        //текстовые обозначения команд
        private readonly string[] _replyStrings =
        {
            "SET_MODE",
            "SET_TIME",
            "RESET_STATION",
            "GET_STATUS",
            "INIT_CHIP",
            "GET_LAST_TEAMS",
            "GET_TEAM_RECORD",
            "READ_CARD_PAGE",
            "UPDATE_TEAM_MASK",
            "WRITE_CARD_PAGE",
            "READ_FLASH",
            "WRITE_FLASH",
            "ERASE_FLASH_SECTOR",
            "GET_CONFIG",
            "SET_V_COEFF",
            "SET_GAIN",
            "SET_CHIP_TYPE",
            "SET_TEAM_FLASH_SIZE",
            "SET_FLASH_BLOCK_SIZE",
            "SET_BT_NAME",
            "SET_BT_PIN",
            "SET_BATTERY_LIMIT",
            "SCAN_TEAMS",
            "SEND_BT_COMMAND",
        };

        //коды ошибок станции
        private readonly string[] _errorCodesStrings =
        {
            "OK",
            "WRONG_STATION",
            "RFID_READ_ERROR",
            "RFID_WRITE_ERROR",
            "LOW_INIT_TIME",
            "WRONG_CHIP",
            "NO_CHIP",
            "BUFFER_OVERFLOW",
            "WRONG_DATA",
            "WRONG_UID",
            "WRONG_TEAM",
            "NO_DATA",
            "WRONG_COMMAND",
            "ERASE_ERROR",
            "WRONG_CHIP_TYPE",
            "WRONG_MODE",
            "WRONG_SIZE",
            "WRONG_FW_VERSION",
            "WRONG_PACKET_LENGTH",
            "FLASH_READ_ERROR",
            "FLASH_WRITE_ERROR",
            "EEPROM_READ_ERROR",
            "EEPROM_WRITE_ERROR",
            "BT_ERROR",
        };

        //header = 0xFE [0-2] + station#[3] + len[4] + cmd#[5] + uartBuffer[6]... + crc
        private static class PacketBytes
        {
            public const byte PACKET_ID = 2;
            public const byte STATION_NUMBER = 3;
            public const byte LENGTH = 4;
            public const byte COMMAND = 5;
            public const byte DATA_START = 6;
        }

        private class Queue
        {
            public byte packetId;
            public DateTime Time;
            private byte[] data;
        }
        private List<Queue> _commandsList = new List<Queue>();
        private List<Queue> _repliesList = new List<Queue>();

        private StationSettings station;

        private FlashContainer StationFlash;

        private byte _selectedChipType = RfidContainer.ChipTypes.Types["NTAG215"];
        private RfidContainer RfidCard;

        private TeamsContainer Teams;

        #region COM_port_handling

        private void button_refresh_Click(object sender, EventArgs e)
        {
            comboBox_portName.Items.Clear();
            comboBox_portName.Items.Add("None");
            foreach (string portname in SerialPort.GetPortNames())
            {
                comboBox_portName.Items.Add(portname); //добавить порт в список
            }

            if (comboBox_portName.Items.Count == 1)
            {
                comboBox_portName.SelectedIndex = 0;
                button_openPort.Enabled = false;
            }
            else
                comboBox_portName.SelectedIndex = 0;


            Hashtable PortNames;
            string[] ports = SerialPort.GetPortNames();
            if (ports.Length == 0)
            {
                textBox_terminal.Text += "ERROR: No COM ports exist\n\r";
            }
            else
            {
                PortNames = Accessory.BuildPortNameHash(ports);
                foreach (String s in PortNames.Keys)
                {
                    textBox_terminal.Text += "\n\r" + PortNames[s] + ": " + s + "\n\r";
                }
            }



        }

        private void button_openPort_Click(object sender, EventArgs e)
        {
            if (comboBox_portName.SelectedIndex != 0)
            {
                serialPort1.PortName = comboBox_portName.Text;
                serialPort1.BaudRate = _portSpeed;
                serialPort1.DataBits = 8;
                serialPort1.Parity = Parity.None;
                serialPort1.StopBits = StopBits.One;
                serialPort1.Handshake = Handshake.None;
                serialPort1.ReadTimeout = 500;
                serialPort1.WriteTimeout = 500;
                try
                {
                    serialPort1.Open();
                }
                catch (Exception ex)
                {
                    SetText("Error opening port " + serialPort1.PortName + ": " + ex.Message);
                }

                if (!serialPort1.IsOpen)
                    return;

                button_getLastTeam.Enabled = true;
                button_getTeamRecord.Enabled = true;
                button_updTeamMask.Enabled = true;
                button_initChip.Enabled = true;
                button_readChipPage.Enabled = true;
                button_writeChipPage.Enabled = true;

                button_setMode.Enabled = true;
                button_resetStation.Enabled = true;
                button_setTime.Enabled = true;
                button_getStatus.Enabled = true;
                button_getStatus.Enabled = true;
                button_readFlash.Enabled = true;
                button_writeFlash.Enabled = true;
                button_dumpTeams.Enabled = true;
                button_dumpChip.Enabled = true;
                button_dumpFlash.Enabled = true;
                button_eraseFlashSector.Enabled = true;
                button_getConfig.Enabled = true;
                button_setKoeff.Enabled = true;
                button_setGain.Enabled = true;
                button_setChipType.Enabled = true;
                button_eraseChip.Enabled = true;
                button_setTeamFlashSize.Enabled = true;
                button_setEraseBlock.Enabled = true;
                button_SetBtName.Enabled = true;
                button_SetBtPin.Enabled = true;
                button_setBatteryLimit.Enabled = true;
                button_getTeamsList.Enabled = true;
                button_sendBtCommand.Enabled = true;
                button_quickDump.Enabled = true;

                button_closePort.Enabled = true;
                button_openPort.Enabled = false;
                button_refresh.Enabled = false;
                comboBox_portName.Enabled = false;
                //aTimer.Enabled = true;
            }
        }

        private void button_closePort_Click(object sender, EventArgs e)
        {
            if (serialPort1.IsOpen)
            {
                try
                {
                    serialPort1.Close();
                }
                catch (Exception ex)
                {
                    SetText("Error closing port " + serialPort1.PortName + ": " + ex.Message);
                }
            }


            button_getLastTeam.Enabled = false;
            button_getTeamRecord.Enabled = false;
            button_updTeamMask.Enabled = false;
            button_initChip.Enabled = false;
            button_readChipPage.Enabled = false;
            button_writeChipPage.Enabled = false;

            button_setMode.Enabled = false;
            button_resetStation.Enabled = false;
            button_setTime.Enabled = false;
            button_getStatus.Enabled = false;
            button_getStatus.Enabled = false;
            button_readFlash.Enabled = false;
            button_writeFlash.Enabled = false;
            button_dumpTeams.Enabled = false;
            button_dumpChip.Enabled = false;
            button_dumpFlash.Enabled = false;
            button_eraseFlashSector.Enabled = false;
            button_getConfig.Enabled = false;
            button_setKoeff.Enabled = false;
            button_setGain.Enabled = false;
            button_setChipType.Enabled = false;
            button_eraseChip.Enabled = false;
            button_setTeamFlashSize.Enabled = false;
            button_setEraseBlock.Enabled = false;
            button_SetBtName.Enabled = false;
            button_SetBtPin.Enabled = false;
            button_setBatteryLimit.Enabled = false;
            button_getTeamsList.Enabled = false;
            button_sendBtCommand.Enabled = false;
            button_quickDump.Enabled = false;

            button_closePort.Enabled = false;
            button_openPort.Enabled = true;
            button_refresh.Enabled = true;
            comboBox_portName.Enabled = true;
        }

        //rewrite to validate packet runtime
        private void serialPort1_DataReceived(object sender, SerialDataReceivedEventArgs e)
        {
            lock (_serialReceiveThreadLock)
            {
                if (checkBox_portMon.Checked)
                {
                    if (_receivingData && DateTime.Now.ToUniversalTime().Subtract(_receiveStartTime).TotalMilliseconds > receiveTimeOut)
                    {
                        _uartBufferPosition = 0;
                        _uartBuffer = new byte[256];
                        _receivingData = false;
                    }
                    List<byte> input = new List<byte>();
                    while (serialPort1.BytesToRead > 0)
                    {
                        int c;
                        try
                        {
                            c = serialPort1.ReadByte();
                            if (c != -1) input.Add((byte)c);
                        }
                        catch (Exception ex)
                        {
                            SetText("COM port read error: " + ex + "\r\n");
                        }
                    }
                    ParsePackage(input);
                }
            }
        }

        private void ParsePackage(List<byte> input)
        {
            StringBuilder result = new StringBuilder();
            List<byte> unrecognizedBytes = new List<byte>();
            while (input.Count > 0)
            {
                //0 byte = FE
                if (_uartBufferPosition == 0 && input[0] == 0xfe)
                {
                    _receiveStartTime = DateTime.Now.ToUniversalTime();
                    _receivingData = true;
                    _uartBuffer[_uartBufferPosition] = input[0];
                    result.Append("<< ");
                    _uartBufferPosition++;
                }
                //1st byte = FE
                else if (_uartBufferPosition == 1 && input[0] == 0xfe)
                {
                    _uartBuffer[_uartBufferPosition] = input[0];
                    _uartBufferPosition++;
                }
                //2nd byte = ID
                else if (_uartBufferPosition == PacketBytes.PACKET_ID)
                {
                    _uartBuffer[_uartBufferPosition] = input[0];
                    _uartBufferPosition++;
                }
                //3rd byte = station#, length, command, and data
                else if (_uartBufferPosition >= PacketBytes.STATION_NUMBER)
                {
                    _uartBuffer[_uartBufferPosition] = input[0];
                    //incorrect length
                    if (_uartBufferPosition == PacketBytes.LENGTH &&
                        _uartBuffer[PacketBytes.LENGTH] > 256 - 7)
                    {
                        result.Append(Accessory.ConvertByteArrayToHex(_uartBuffer, _uartBufferPosition));
                        result.Append("\r\nIncorrect length\r\n");
                        _receivingData = false;
                        _uartBufferPosition = 0;
                        _uartBuffer = new byte[256];
                        input.RemoveAt(0);
                        continue;
                    }

                    //packet is received
                    if (_uartBufferPosition > PacketBytes.LENGTH && _uartBufferPosition >=
                        PacketBytes.DATA_START + _uartBuffer[PacketBytes.LENGTH])
                    {
                        //crc matching
                        byte crc = Accessory.CrcCalc(_uartBuffer, PacketBytes.PACKET_ID,
                            _uartBufferPosition - 1);
                        if (_uartBuffer[_uartBufferPosition] == crc)
                        {
                            result.Append(Accessory.ConvertByteArrayToHex(_uartBuffer, _uartBufferPosition + 1));
                            result.Append("\r\nExecute time=" +
                                    DateTime.Now.ToUniversalTime().Subtract(_getStatusTime).TotalMilliseconds + " ms.\r\n");
                            string res = ParseReply(_uartBuffer);
                            result.Append(res);
                            _receivingData = true;
                            _uartBufferPosition = 0;
                            _uartBuffer = new byte[256];
                            _asyncFlag--;
                            input.RemoveAt(0);
                            continue;
                        }

                        result.Append(Accessory.ConvertByteArrayToHex(_uartBuffer, _uartBufferPosition));
                        result.Append("\r\nCRC not correct: " + _uartBuffer[_uartBufferPosition].ToString("X2") +
                                " instead of " + crc.ToString("X2") + "\r\n");
                        _receivingData = false;
                        _uartBufferPosition = 0;
                        _uartBuffer = new byte[256];
                        input.RemoveAt(0);
                        continue;
                    }

                    _uartBufferPosition++;
                }
                else
                {
                    if (_uartBufferPosition > 0)
                    {
                        List<byte> error = new List<byte>();
                        for (int i = 0; i < _uartBufferPosition; i++)
                        {
                            error.Add(_uartBuffer[i]);
                        }
                        error.Add(input[0]);
                        _receivingData = false;
                        _uartBufferPosition = 0;
                        _uartBuffer = new byte[256];
                        SetText("\r\nIncorrect bytes: [" + Accessory.ConvertByteArrayToHex(error.ToArray()) + "]\r\n");
                    }
                    else unrecognizedBytes.Add(input[0]);
                }
                input.RemoveAt(0);
            }

            if (unrecognizedBytes.Count > 0)
            {
                SetText("Comment: " + Accessory.ConvertByteArrayToString(unrecognizedBytes.ToArray()) + "\r\n");
            }
            SetText(result.ToString());
        }

        private void serialPort1_ErrorReceived(object sender, SerialErrorReceivedEventArgs e)
        {
            SetText("COM port error: " + e + "\r\n");
        }

        private void SendCommand(byte[] command)
        {
            if (!serialPort1.IsOpen)
                button_closePort_Click(this, EventArgs.Empty);
            lock (_serialSendThreadLock)
            {
                command[0] = 0xFE;
                command[1] = 0xFE;
                command[PacketBytes.PACKET_ID] = _packageId++;
                command[PacketBytes.STATION_NUMBER] = station.Number;
                command[PacketBytes.LENGTH] = (byte)(command.Length - PacketBytes.DATA_START - 1);
                command[command.Length - 1] =
                    Accessory.CrcCalc(command, PacketBytes.PACKET_ID, command.Length - 2);
                _getStatusTime = DateTime.Now.ToUniversalTime();
                try
                {
                    serialPort1.Write(command, 0, command.Length);
                }
                catch (Exception e)
                {
                    SetText("COM port write error: " + e + "\r\n");
                    return;
                }

                SetText("\r\n>> "
                        + Accessory.ConvertByteArrayToHex(command)
                        + "\r\n");
            }
        }

        #endregion

        #region Terminal_window

        private void textBox_terminal_TextChanged(object sender, EventArgs e)
        {
            if (checkBox_autoScroll.Checked)
            {
                textBox_terminal.SelectionStart = textBox_terminal.Text.Length;
                textBox_terminal.ScrollToCaret();
            }
        }

        private delegate void SetTextCallback1(string text);

        private void SetText(string text)
        {
            lock (_textOutThreadLock)
            {
                if (_noTerminalOutputFlag)
                {
                    if (_logAutoSaveFlag) File.AppendAllText(_logAutoSaveFile, text);
                    return;
                }

                //text = Accessory.FilterZeroChar(text);
                // InvokeRequired required compares the thread ID of the
                // calling thread to the thread ID of the creating thread.
                // If these threads are different, it returns true.
                //if (this.textBox_terminal1.InvokeRequired)
                if (textBox_terminal.InvokeRequired)
                {
                    SetTextCallback1 d = SetText;
                    BeginInvoke(d, text);
                }
                else
                {
                    if (_logAutoSaveFlag)
                    {
                        File.AppendAllText(_logAutoSaveFile, text);
                    }

                    int pos = textBox_terminal.SelectionStart;
                    textBox_terminal.AppendText(text);
                    if (textBox_terminal.Lines.Length > _logLinesLimit)
                    {
                        StringBuilder tmp = new StringBuilder();
                        for (int i = textBox_terminal.Lines.Length - _logLinesLimit;
                            i < textBox_terminal.Lines.Length;
                            i++)
                        {
                            tmp.Append(textBox_terminal.Lines[i] + "\r\n");
                        }

                        textBox_terminal.Text = tmp.ToString();
                    }

                    if (checkBox_autoScroll.Checked)
                    {
                        textBox_terminal.SelectionStart = textBox_terminal.Text.Length;
                        textBox_terminal.ScrollToCaret();
                    }
                    else
                    {
                        textBox_terminal.SelectionStart = pos;
                        textBox_terminal.ScrollToCaret();
                    }
                }
            }
        }

        #endregion

        #region Generate commands

        private void button_setMode_Click(object sender, EventArgs e)
        {
            byte[] setMode = new byte[CommandDataLength.SET_MODE];
            setMode[PacketBytes.COMMAND] = Command.SET_MODE;

            //0: новый номер режима

            if (!StationSettings.StationMode.TryGetValue(comboBox_mode.SelectedItem.ToString(), out var item)) return;

            setMode[PacketBytes.DATA_START] = (byte)item;
            SendCommand(setMode);
        }

        private void button_setTime_Click(object sender, EventArgs e)
        {
            byte[] setTime = new byte[CommandDataLength.SET_TIME];
            setTime[PacketBytes.COMMAND] = Command.SET_TIME;

            //0-5: дата и время[yy.mm.dd hh: mm:ss]
            if (checkBox_autoTime.Checked)
                textBox_setTime.Text = Helpers.DateToString(DateTime.Now.ToUniversalTime());
            byte[] date = Helpers.DateStringToByteArray(textBox_setTime.Text);
            for (int i = 0; i < 6; i++)
                setTime[PacketBytes.DATA_START + i] = date[i];

            SendCommand(setTime);
        }

        private void button_resetStation_Click(object sender, EventArgs e)
        {
            byte[] resetStation = new byte[CommandDataLength.RESET_STATION];
            resetStation[PacketBytes.COMMAND] = Command.RESET_STATION;

            /*0-1: кол-во отмеченных карт (для сверки)
            2-5: время последней отметки unixtime(для сверки)
            6: новый номер станции*/
            uint.TryParse(textBox_checkedChips.Text, out uint chipsNumber);
            resetStation[PacketBytes.DATA_START + 0] = (byte)(chipsNumber >> 8);
            resetStation[PacketBytes.DATA_START + 1] = (byte)(chipsNumber & 0x00ff);

            long tmpTime = Helpers.DateStringToUnixTime(textBox_lastCheck.Text);
            resetStation[PacketBytes.DATA_START + 2] = (byte)((tmpTime & 0xFF000000) >> 24);
            resetStation[PacketBytes.DATA_START + 3] = (byte)((tmpTime & 0x00FF0000) >> 16);
            resetStation[PacketBytes.DATA_START + 4] = (byte)((tmpTime & 0x0000FF00) >> 8);
            resetStation[PacketBytes.DATA_START + 5] = (byte)(tmpTime & 0x000000FF);

            byte.TryParse(textBox_newStationNumber.Text, out resetStation[PacketBytes.DATA_START + 6]);

            SendCommand(resetStation);
        }

        private void button_getStatus_Click(object sender, EventArgs e)
        {
            byte[] getStatus = new byte[CommandDataLength.GET_STATUS];
            getStatus[PacketBytes.COMMAND] = Command.GET_STATUS;

            SendCommand(getStatus);
        }

        private void button_initChip_Click(object sender, EventArgs e)
        {
            byte[] initChip = new byte[CommandDataLength.INIT_CHIP];
            initChip[PacketBytes.COMMAND] = Command.INIT_CHIP;

            /*0-1: номер команды
            2-3: маска участников*/
            uint.TryParse(textBox_initTeamNum.Text, out uint cmd);
            initChip[PacketBytes.DATA_START] = (byte)(cmd >> 8);
            initChip[PacketBytes.DATA_START + 1] = (byte)cmd;

            uint mask = 0;
            byte j = 0;
            for (int i = 15; i >= 0; i--)
            {
                if (textBox_initMask.Text[i] == '1')
                    mask = (uint)Accessory.SetBit(mask, j);
                else
                    mask = (uint)Accessory.ClearBit(mask, j);
                j++;
            }

            initChip[PacketBytes.DATA_START + 2] = (byte)(mask >> 8);
            initChip[PacketBytes.DATA_START + 3] = (byte)(mask & 0x00ff);

            SendCommand(initChip);
        }

        private void button_getLastTeam_Click(object sender, EventArgs e)
        {
            byte[] getLastTeam = new byte[CommandDataLength.GET_LAST_TEAMS];
            getLastTeam[PacketBytes.COMMAND] = Command.GET_LAST_TEAMS;

            SendCommand(getLastTeam);
        }

        private void button_getTeamRecord_Click(object sender, EventArgs e)
        {
            byte[] getTeamRecord = new byte[CommandDataLength.GET_TEAM_RECORD];
            getTeamRecord[PacketBytes.COMMAND] = Command.GET_TEAM_RECORD;

            //0-1: какую запись
            uint.TryParse(textBox_teamNumber.Text, out uint from);
            getTeamRecord[PacketBytes.DATA_START] = (byte)(from >> 8);
            getTeamRecord[PacketBytes.DATA_START + 1] = (byte)(from & 0x00ff);

            SendCommand(getTeamRecord);
        }

        private void button_readCardPage_Click(object sender, EventArgs e)
        {
            byte[] readCardPage = new byte[CommandDataLength.READ_CARD_PAGE];
            readCardPage[PacketBytes.COMMAND] = Command.READ_CARD_PAGE;

            //0: с какой страницу карты
            byte.TryParse(
                textBox_readChipPage.Text.Substring(0, textBox_readChipPage.Text.IndexOf("-", StringComparison.Ordinal)).Trim(), out byte from);
            readCardPage[PacketBytes.DATA_START] = from;
            //1: по какую страницу карты включительно
            byte.TryParse(textBox_readChipPage.Text.Substring(textBox_readChipPage.Text.IndexOf("-", StringComparison.Ordinal) + 1)
                .Trim(), out byte to);
            readCardPage[PacketBytes.DATA_START + 1] = to;

            SendCommand(readCardPage);
        }

        private void button_updateTeamMask_Click(object sender, EventArgs e)
        {
            byte[] updateTeamMask = new byte[CommandDataLength.UPDATE_TEAM_MASK];
            updateTeamMask[PacketBytes.COMMAND] = Command.UPDATE_TEAM_MASK;

            /*0-1: номер команды
            2-5: время выдачи чипа
            6-7: маска участников*/
            uint.TryParse(textBox_teamNumber.Text, out uint teamNum);
            updateTeamMask[PacketBytes.DATA_START] = (byte)(teamNum >> 8);
            updateTeamMask[PacketBytes.DATA_START + 1] = (byte)teamNum;

            //card issue time - 4 byte
            //textBox_issueTime.Text
            long tmpTime = Helpers.DateStringToUnixTime(textBox_issueTime.Text);
            updateTeamMask[PacketBytes.DATA_START + 2] = (byte)((tmpTime & 0xFF000000) >> 24);
            updateTeamMask[PacketBytes.DATA_START + 3] = (byte)((tmpTime & 0x00FF0000) >> 16);
            updateTeamMask[PacketBytes.DATA_START + 4] = (byte)((tmpTime & 0x0000FF00) >> 8);
            updateTeamMask[PacketBytes.DATA_START + 5] = (byte)(tmpTime & 0x000000FF);


            uint mask = 0;
            byte j = 0;
            for (int i = 15; i >= 0; i--)
            {
                if (textBox_teamMask.Text[i] == '1')
                    mask = (uint)Accessory.SetBit(mask, j);
                else
                    mask = (uint)Accessory.ClearBit(mask, j);
                j++;
            }

            updateTeamMask[PacketBytes.DATA_START + 6] = (byte)(mask >> 8);
            updateTeamMask[PacketBytes.DATA_START + 7] = (byte)(mask & 0x00ff);

            SendCommand(updateTeamMask);
        }

        private void button_writeCardPage_Click(object sender, EventArgs e)
        {
            byte[] writeCardPage = new byte[CommandDataLength.WRITE_CARD_PAGE];
            writeCardPage[PacketBytes.COMMAND] = Command.WRITE_CARD_PAGE;

            //0-7: UID чипа
            //8: номер страницы
            //9-12: данные из страницы карты (4 байта)
            byte[] uid = Accessory.ConvertHexToByteArray(textBox_uid.Text);
            if (uid.Length != 8)
                return;
            for (int i = 0; i <= 7; i++)
                writeCardPage[PacketBytes.DATA_START + i] = uid[i];

            byte.TryParse(textBox_writeChipPage.Text, out writeCardPage[PacketBytes.DATA_START + 8]);

            byte[] data = Accessory.ConvertHexToByteArray(textBox_data.Text);

            if (data.Length != 4)
                return;
            for (int i = 0; i <= 3; i++)
                writeCardPage[PacketBytes.DATA_START + 9 + i] = data[i];

            SendCommand(writeCardPage);
        }

        private void button_readFlash_Click(object sender, EventArgs e)
        {
            byte[] readFlash = new byte[CommandDataLength.READ_FLASH];
            readFlash[PacketBytes.COMMAND] = Command.READ_FLASH;

            //0-3: адрес начала чтения
            //4: размер блока

            long.TryParse(textBox_readFlashAddress.Text, out long fromAddr);
            readFlash[PacketBytes.DATA_START] = (byte)((fromAddr & 0xFF000000) >> 24);
            readFlash[PacketBytes.DATA_START + 1] = (byte)((fromAddr & 0x00FF0000) >> 16);
            readFlash[PacketBytes.DATA_START + 2] = (byte)((fromAddr & 0x0000FF00) >> 8);
            readFlash[PacketBytes.DATA_START + 3] = (byte)(fromAddr & 0x000000FF);

            byte.TryParse(textBox_readFlashLength.Text, out byte toAddr);
            readFlash[PacketBytes.DATA_START + 4] = toAddr;

            SendCommand(readFlash);
        }

        private void button_writeFlash_Click(object sender, EventArgs e)
        {
            byte[] data = Accessory.ConvertHexToByteArray(textBox_flashData.Text);

            byte[] writeFlash = new byte[CommandDataLength.WRITE_FLASH + data.Length];
            writeFlash[PacketBytes.COMMAND] = Command.WRITE_FLASH;

            //0-3: адрес начала записи
            //4...: данные для записи
            long.TryParse(textBox_writeAddr.Text, out long tmpTime);
            writeFlash[PacketBytes.DATA_START] = (byte)((tmpTime & 0xFF000000) >> 24);
            writeFlash[PacketBytes.DATA_START + 1] = (byte)((tmpTime & 0x00FF0000) >> 16);
            writeFlash[PacketBytes.DATA_START + 2] = (byte)((tmpTime & 0x0000FF00) >> 8);
            writeFlash[PacketBytes.DATA_START + 3] = (byte)(tmpTime & 0x000000FF);

            for (byte i = 0; i < data.Length; i++)
            {
                writeFlash[PacketBytes.DATA_START + 4 + i] = data[i];
            }

            SendCommand(writeFlash);
        }

        private void button_eraseFlashSector_Click(object sender, EventArgs e)
        {
            byte[] eraseFlashSector = new byte[CommandDataLength.ERASE_FLASH_SECTOR];
            eraseFlashSector[PacketBytes.COMMAND] = Command.ERASE_FLASH_SECTOR;

            //0-1: какой сектор
            uint.TryParse(textBox_eraseSector.Text, out uint sector);
            eraseFlashSector[PacketBytes.DATA_START] = (byte)(sector >> 8);
            eraseFlashSector[PacketBytes.DATA_START + 1] = (byte)(sector & 0x00ff);

            SendCommand(eraseFlashSector);
        }

        private void button_getConfig_Click(object sender, EventArgs e)
        {
            byte[] getConfig = new byte[CommandDataLength.GET_CONFIG];
            getConfig[PacketBytes.COMMAND] = Command.GET_CONFIG;

            SendCommand(getConfig);
        }

        private void button_setVCoeff_Click(object sender, EventArgs e)
        {
            byte[] setKoeff = new byte[CommandDataLength.SET_V_COEFF];
            setKoeff[PacketBytes.COMMAND] = Command.SET_V_COEFF;

            //0-3: коэффициент пересчета напряжения
            float.TryParse(textBox_koeff.Text, out float koeff);
            byte[] k = BitConverter.GetBytes(koeff);
            for (int i = 0; i < 4; i++)
            {
                setKoeff[PacketBytes.DATA_START + i] = k[i];
            }

            SendCommand(setKoeff);
        }

        private void Button_setGain_Click(object sender, EventArgs e)
        {
            byte[] setGain = new byte[CommandDataLength.SET_GAIN];
            setGain[PacketBytes.COMMAND] = Command.SET_GAIN;

            //0: новый коэфф.
            if (!StationSettings.Gain.TryGetValue(comboBox_setGain.SelectedItem.ToString(), out setGain[PacketBytes.DATA_START])) return;
            SendCommand(setGain);
        }

        private void button_setChipType_Click(object sender, EventArgs e)
        {
            byte[] setChipType = new byte[CommandDataLength.SET_CHIP_TYPE];
            setChipType[PacketBytes.COMMAND] = Command.SET_CHIP_TYPE;

            //0: новый тип чипа
            setChipType[PacketBytes.DATA_START] = RfidContainer.ChipTypes.GetSystemId(_selectedChipType);
            SendCommand(setChipType);
        }

        private void Button_setTeamFlashSize_Click(object sender, EventArgs e)
        {
            byte[] setTeamFlashSize = new byte[CommandDataLength.SET_TEAM_FLASH_SIZE];
            setTeamFlashSize[PacketBytes.COMMAND] = Command.SET_TEAM_FLASH_SIZE;

            //0-1: новый размер блока команды
            int.TryParse(textBox_teamFlashSize.Text, out int chip);

            setTeamFlashSize[PacketBytes.DATA_START] = (byte)(chip >> 8);
            setTeamFlashSize[PacketBytes.DATA_START + 1] = (byte)(chip & 0x00ff);
            SendCommand(setTeamFlashSize);
        }

        private void Button_setEraseBlock_Click(object sender, EventArgs e)
        {
            byte[] setEraseBlockSize = new byte[CommandDataLength.SET_FLASH_BLOCK_SIZE];
            setEraseBlockSize[PacketBytes.COMMAND] = Command.SET_FLASH_BLOCK_SIZE;

            //0-1: новый размер стираемого блока
            int.TryParse(textBox_eraseBlock.Text, out int chip);

            setEraseBlockSize[PacketBytes.DATA_START] = (byte)(chip >> 8);
            setEraseBlockSize[PacketBytes.DATA_START + 1] = (byte)(chip & 0x00ff);
            SendCommand(setEraseBlockSize);
        }

        private void Button_SetBtName_Click(object sender, EventArgs e)
        {
            byte[] data = Encoding.ASCII.GetBytes(textBox_BtName.Text);

            byte[] setBtName = new byte[CommandDataLength.SET_BT_NAME + data.Length];
            setBtName[PacketBytes.COMMAND] = Command.SET_BT_NAME;

            //0...: данные для записи
            for (byte i = 0; i < data.Length; i++)
            {
                setBtName[PacketBytes.DATA_START + i] = data[i];
            }

            SendCommand(setBtName);
        }

        private void Button_SetBtPin_Click(object sender, EventArgs e)
        {
            byte[] data = Encoding.ASCII.GetBytes(textBox_BtPin.Text);

            byte[] setBtName = new byte[CommandDataLength.SET_BT_PIN + data.Length];
            setBtName[PacketBytes.COMMAND] = Command.SET_BT_PIN;

            //0...: данные для записи
            for (byte i = 0; i < data.Length; i++)
            {
                setBtName[PacketBytes.DATA_START + i] = data[i];
            }

            SendCommand(setBtName);
        }

        private void Button_setBatteryLimit_Click(object sender, EventArgs e)
        {
            byte[] setBatteryLimit = new byte[CommandDataLength.SET_BATTERY_LIMIT];
            setBatteryLimit[PacketBytes.COMMAND] = Command.SET_BATTERY_LIMIT;

            //0-3: коэффициент пересчета напряжения
            float.TryParse(textBox_setBatteryLimit.Text, out float limit);
            byte[] k = BitConverter.GetBytes(limit);
            for (int i = 0; i < 4; i++)
            {
                setBatteryLimit[PacketBytes.DATA_START + i] = k[i];
            }

            SendCommand(setBatteryLimit);
        }

        private void button_getTeamsList_Click(object sender, EventArgs e)
        {
            UInt32.TryParse(textBox_getTeamsList.Text, out UInt32 teamNumber);

            byte[] scanTeams = new byte[CommandDataLength.SCAN_TEAMS];
            scanTeams[PacketBytes.COMMAND] = Command.SCAN_TEAMS;

            //0-1: начальный номер команды
            scanTeams[PacketBytes.DATA_START] = (byte)(teamNumber >> 8);
            scanTeams[PacketBytes.DATA_START + 1] = (byte)(teamNumber & 0x00ff);

            SendCommand(scanTeams);
        }

        private void button_sendBtCommand_Click(object sender, EventArgs e)
        {
            string btCommand = textBox_sendBtCommand.Text + "\r\n";
            byte[] data = Encoding.ASCII.GetBytes(btCommand);
            if (data.Length > 256 - 8 || data.Length < 1) return;

            byte[] sendBtCommand = new byte[CommandDataLength.SEND_BT_COMMAND + data.Length];
            sendBtCommand[PacketBytes.COMMAND] = Command.SEND_BT_COMMAND;

            //0...: команда
            for (byte i = 0; i < data.Length; i++)
            {
                sendBtCommand[PacketBytes.DATA_START + i] = data[i];
            }

            SendCommand(sendBtCommand);
        }

        #endregion

        #region Parse replies

        private string ParseReply(byte[] data)
        {
            string result = "";
            if (data[PacketBytes.LENGTH] == 1 && data[PacketBytes.DATA_START] > 0)
            {
                result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
                result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
                result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
                return result;
            }
            bool incorrectLengthFlag = false;
            switch (data[PacketBytes.COMMAND])
            {
                case Reply.SET_MODE:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.SET_MODE)
                        result = reply_setMode(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.SET_TIME:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.SET_TIME)
                        result = reply_setTime(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.RESET_STATION:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.RESET_STATION)
                        result = reply_resetStation(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.GET_STATUS:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.GET_STATUS)
                        result = reply_getStatus(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.INIT_CHIP:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.INIT_CHIP)
                        result = reply_initChip(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.GET_LAST_TEAMS:
                    if (data[PacketBytes.LENGTH] >= ReplyDataLength.GET_LAST_TEAMS)
                        result = reply_getLastTeams(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.GET_TEAM_RECORD:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.GET_TEAM_RECORD)
                        result = reply_getTeamRecord(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.READ_CARD_PAGE:
                    if (data[PacketBytes.LENGTH] >= ReplyDataLength.READ_CARD_PAGE)
                        result = reply_readCardPages(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.UPDATE_TEAM_MASK:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.UPDATE_TEAM_MASK)
                        result = reply_updateTeamMask(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.WRITE_CARD_PAGE:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.WRITE_CARD_PAGE)
                        result = reply_writeCardPage(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.READ_FLASH:
                    if (data[PacketBytes.LENGTH] >= ReplyDataLength.READ_FLASH)
                        result = reply_readFlash(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.WRITE_FLASH:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.WRITE_FLASH)
                        result = reply_writeFlash(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.ERASE_FLASH_SECTOR:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.ERASE_FLASH_SECTOR)
                        result = reply_eraseFlashSector(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.GET_CONFIG:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.GET_CONFIG)
                        result = reply_getConfig(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.SET_V_COEFF:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.SET_V_COEFF)
                        result = reply_setVCoeff(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.SET_GAIN:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.SET_GAIN)
                        result = reply_setGain(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.SET_CHIP_TYPE:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.SET_CHIP_TYPE)
                        result = reply_setChipType(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.SET_TEAM_FLASH_SIZE:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.SET_TEAM_FLASH_SIZE)
                        result = reply_setTeamFlashSize(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.SET_FLASH_BLOCK_SIZE:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.SET_FLASH_BLOCK_SIZE)
                        result = reply_setFlashBlockSize(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.SET_BT_NAME:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.SET_BT_NAME)
                        result = reply_setBtName(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.SET_BT_PIN:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.SET_BT_PIN)
                        result = reply_setBtPin(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.SET_BATTERY_LIMIT:
                    if (data[PacketBytes.LENGTH] == ReplyDataLength.SET_BATTERY_LIMIT)
                        result = reply_setBatteryLimit(data);
                    else incorrectLengthFlag = true;
                    break;

                case Reply.SCAN_TEAMS:
                    if (data[PacketBytes.LENGTH] >= ReplyDataLength.SCAN_TEAMS)
                        result = reply_scanTeams(data);
                    else incorrectLengthFlag = true;
                    break;
                case Reply.SEND_BT_COMMAND:
                    if (data[PacketBytes.LENGTH] >= ReplyDataLength.SEND_BT_COMMAND)
                        result = reply_sendBtCommand(data);
                    else incorrectLengthFlag = true;
                    break;

                default:
                    result = "Incorrect reply code: " +
                        data[PacketBytes.COMMAND].ToString() +
                        "\r\n";
                    break;
            }
            if (incorrectLengthFlag)
            {
                result = "Command: " +
                    _replyStrings[data[PacketBytes.COMMAND] - 0x90] +
                    "\r\nIncorrect length: " +
                    data[PacketBytes.COMMAND].ToString() +
                    "\r\n";
            }

            return result;
        }

        private string reply_setMode(byte[] data)
        {
            //0: код ошибки
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            return result;
        }

        private string reply_setTime(byte[] data)
        {
            //0: код ошибки
            //1-4: текущее время
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.LENGTH] == 1 && data[PacketBytes.DATA_START] > 0)
                return result;

            long t = data[PacketBytes.DATA_START + 1] * 16777216 + data[PacketBytes.DATA_START + 2] * 65536 +
                     data[PacketBytes.DATA_START + 3] * 256 + data[PacketBytes.DATA_START + 4];
            DateTime d = Helpers.ConvertFromUnixTimestamp(t);
            result += "\tНовое время: " + Helpers.DateToString(d) + "\r\n";
            return result;
        }

        private string reply_resetStation(byte[] data)
        {
            //0: код ошибки
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            return result;
        }

        private string reply_getStatus(byte[] data)
        {
            string result = "";
            //0: код ошибки
            //1-4: текущее время
            //5-6: количество отметок на станции
            //7-10: время последней отметки на станции
            //11-12: напряжение батареи в условных единицах[0..1023] ~ [0..1.1В]
            //13-14: температура чипа DS3231 (чуть выше окружающей среды)

            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            Invoke((MethodInvoker)delegate
            {
                station.Number = data[PacketBytes.STATION_NUMBER];
                textBox_stationNumber.Text = station.Number.ToString();
            });

            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.LENGTH] == 1 && data[PacketBytes.DATA_START] > 0)
                return result;

            long t = data[PacketBytes.DATA_START + 1] * 16777216 + data[PacketBytes.DATA_START + 2] * 65536 +
                     data[PacketBytes.DATA_START + 3] * 256 + data[PacketBytes.DATA_START + 4];
            DateTime d = Helpers.ConvertFromUnixTimestamp(t);
            result += "\tВремя: " + Helpers.DateToString(d) + "\r\n";

            int n = data[PacketBytes.DATA_START + 5] * 256 + data[PacketBytes.DATA_START + 6];
            result += "\tКол-во отметок#: " + n + "\r\n";
            Invoke((MethodInvoker)delegate
            {
                textBox_checkedChips.Text = n.ToString();
            });

            t = data[PacketBytes.DATA_START + 7] * 16777216 + data[PacketBytes.DATA_START + 8] * 65536 +
                    data[PacketBytes.DATA_START + 9] * 256 + data[PacketBytes.DATA_START + 10];
            d = Helpers.ConvertFromUnixTimestamp(t);
            result += "\tВремя последней отметки: " + Helpers.DateToString(d) + "\r\n";
            //textBox_lastCheck.Text = DateToString(d);

            n = data[PacketBytes.DATA_START + 11] * 256 + data[PacketBytes.DATA_START + 12];
            result += "\tБатарея: " + (station.VoltageCoefficient * n).ToString("F2") + "V (ADC=" + n + ")\r\n";

            n = data[PacketBytes.DATA_START + 13] * 256 + data[PacketBytes.DATA_START + 14];
            result += "\tТемпература: " + n + "\r\n";

            return result;
        }

        private string reply_initChip(byte[] data)
        {
            //0: код ошибки
            // убрать 1-7: UID чипа
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.LENGTH] == 1 && data[PacketBytes.DATA_START] > 0)
                return result;

            result += "\tВремя инициализации: ";
            long t = data[PacketBytes.DATA_START + 1] * 16777216 + data[PacketBytes.DATA_START + 2] * 65536 +
                     data[PacketBytes.DATA_START + 3] * 256 + data[PacketBytes.DATA_START + 4];
            DateTime d = Helpers.ConvertFromUnixTimestamp(t);
            result += Helpers.DateToString(d) + "\r\n";

            result += "\tUID: ";
            for (int i = 1; i <= 8; i++)
                result += Accessory.ConvertByteToHex(data[PacketBytes.DATA_START + 4 + i]);
            result += "\r\n";

            return result;
        }

        private string reply_getLastTeams(byte[] data)
        {
            //0: код ошибки
            //1-2: номер 1й команды
            //3-4: номер 2й команды
            //...
            //(n - 1) - n: номер последней команды
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.LENGTH] == 1 && data[PacketBytes.DATA_START] > 0)
                return result;

            result += "\tНомера последних команд:\r\n";
            for (int i = 1; i < data[PacketBytes.LENGTH]; i++)
            {
                UInt16 n = (UInt16)(data[PacketBytes.DATA_START + i] * 256);
                i++;
                n += data[PacketBytes.DATA_START + i];
                if (n > 0)
                {
                    result += "\t\t" + n + "\r\n";
                    TeamsContainer.TeamData tmpTeam = new TeamsContainer.TeamData();
                    tmpTeam.TeamNumber = n;
                    Teams.Add(tmpTeam);
                }
            }

            return result;
        }

        private string reply_getTeamRecord(byte[] data)
        {
            //0: код ошибки
            //1: данные отметившейся команды
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.LENGTH] == 1 && data[PacketBytes.DATA_START] > 0)
                return result;

            TeamsContainer.TeamData tmpTeam = new TeamsContainer.TeamData();

            UInt16 n = (UInt16)(data[PacketBytes.DATA_START + 1] * 256 + data[PacketBytes.DATA_START + 2]);
            result += "\tКоманда#: " + n + "\r\n";
            tmpTeam.TeamNumber = n;


            long t = data[PacketBytes.DATA_START + 3] * 16777216 + data[PacketBytes.DATA_START + 4] * 65536 +
                     data[PacketBytes.DATA_START + 5] * 256 + data[PacketBytes.DATA_START + 6];
            DateTime d = Helpers.ConvertFromUnixTimestamp(t);
            result += "\tВремя инициализации: " + Helpers.DateToString(d) + "\r\n";
            tmpTeam.InitTime = d;

            n = (UInt16)(data[PacketBytes.DATA_START + 7] * 256 + data[PacketBytes.DATA_START + 8]);
            result += "\tМаска команды: " + Helpers.ConvertMaskToString(n) + "\r\n";
            tmpTeam.TeamMask = n;

            t = data[PacketBytes.DATA_START + 9] * 16777216 + data[PacketBytes.DATA_START + 10] * 65536 +
                data[PacketBytes.DATA_START + 11] * 256 + data[PacketBytes.DATA_START + 12];
            d = Helpers.ConvertFromUnixTimestamp(t);
            result += "\tВремя последней отметки: " + Helpers.DateToString(d) + "\r\n";
            tmpTeam.LastCheckTime = d;

            /*n = (int)(data[PacketBytes.DATA_START + 13] * 256 + data[PacketBytes.DATA_START + 14]);
            if (n == 0xff) n = 0;
            result += "Байт в дампе: " + n + "\r\n";
            tmpTeam.TeamDumpSize = n;*/

            n = data[PacketBytes.DATA_START + 13];
            if (n == 0xff) n = 0;
            result += "Страниц в дампе: " + n + "\r\n";
            tmpTeam.DumpSize = n;

            Teams.Add(tmpTeam);

            return result;
        }

        private string reply_readCardPages(byte[] data)
        {
            //0: код ошибки
            //1-7: UID чипа
            //8-11: данные из страницы карты(4 байта)
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.LENGTH] == 1 && data[PacketBytes.DATA_START] > 0)
                return result;

            result += "\tUID: ";
            for (int i = 1; i <= 8; i++)
                result += Accessory.ConvertByteToHex(data[PacketBytes.DATA_START + i]);
            result += "\r\n";

            result += "\tДанные с карты:\r\n";

            byte startPage = data[PacketBytes.DATA_START + 9];
            result += "\t\tStart page: " + startPage.ToString() + "\r\n";

            byte[] tmpPage = new byte[data[PacketBytes.LENGTH] - 10];
            for (int i = 0; i < tmpPage.Length; i++)
            {
                tmpPage[i] = data[PacketBytes.DATA_START + 10 + i];
            }
            RfidCard.AddPages(startPage, tmpPage);
            result += "\t\tData: ";
            result += Accessory.ConvertByteArrayToHex(tmpPage);
            result += "\r\n";

            return result;
        }

        private string reply_updateTeamMask(byte[] data)
        {
            //0: код ошибки
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            return result;
        }

        private string reply_writeCardPage(byte[] data)
        {
            //0: код ошибки
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            return result;
        }

        private string reply_readFlash(byte[] data)
        {
            //0: код ошибки
            //1...: данные из флэша
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.LENGTH] == 1 && data[PacketBytes.DATA_START] > 0)
                return result;

            long t = data[PacketBytes.DATA_START + 1] * 16777216 + data[PacketBytes.DATA_START + 2] * 65536 +
                data[PacketBytes.DATA_START + 3] * 256 + data[PacketBytes.DATA_START + 4];
            result += "\tАдрес начала чтения: " + t + "\r\n";

            result += "\tДанные флэш: ";
            byte[] tmpData = new byte[data[PacketBytes.LENGTH] - 5];
            for (int i = 0; i < tmpData.Length; i++)
            {
                //result += Accessory.ConvertByteToHex(data[PacketBytes.DATA_START + 5 + i]);
                tmpData[i] = data[PacketBytes.DATA_START + 5 + i];
            }
            StationFlash.Add(t, tmpData);
            result += Accessory.ConvertByteArrayToHex(tmpData);

            result += "\r\n";
            return result;
        }

        private string reply_writeFlash(byte[] data)
        {
            //0: код ошибки
            //1...: данные из флэша
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.LENGTH] == 1 && data[PacketBytes.DATA_START] > 0)
                return result;

            result += "\tЗаписано байт: " + data[PacketBytes.DATA_START + 1] + "\r\n";
            return result;
        }

        private string reply_eraseFlashSector(byte[] data)
        {
            //0: код ошибки
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            return result;
        }

        private string reply_getConfig(byte[] data)
        {
            string result = "";
            //0: код ошибки
            //1: версия прошивки
            //2: номер режима
            //3: тип чипов (емкость разная, а распознать их программно можно только по ошибкам чтения "дальних" страниц)
            //4-7: емкость флэш-памяти
            //12-15: коэффициент пересчета напряжения (float, 4 bytes) - просто умножаешь коэффициент на полученное в статусе число и будет температура
            //16: коэфф. усиления антенны
            //17-18: размер блока хранения команды
            //19-20: размер стираемого блока
            //21-24: минимальное значение напряжения батареи

            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.LENGTH] == 1 && data[PacketBytes.DATA_START] > 0)
                return result;

            station.FwVersion = data[PacketBytes.DATA_START + 1];
            result += "\tПрошивка: " + station.FwVersion.ToString() + "\r\n";
            result += "\tРежим: " + StationSettings.StationMode.FirstOrDefault(x => x.Value == data[PacketBytes.DATA_START + 2]).Key + "\r\n";
            station.Mode = data[PacketBytes.DATA_START + 2];

            if (data[PacketBytes.DATA_START + 3] == 213)
                station.ChipType = RfidContainer.ChipTypes.Types["NTAG213"];
            else if (data[PacketBytes.DATA_START + 3] == 215)
                station.ChipType = RfidContainer.ChipTypes.Types["NTAG215"];
            else if (data[PacketBytes.DATA_START + 3] == 216)
                station.ChipType = RfidContainer.ChipTypes.Types["NTAG216"];
            result += "\tМетка: " + station.ChipType + "\r\n";


            station.FlashSize = (UInt32)(data[PacketBytes.DATA_START + 4] * 16777216 + data[PacketBytes.DATA_START + 5] * 65536 + data[PacketBytes.DATA_START + 6] * 256 + data[PacketBytes.DATA_START + 7]);
            result += "\tРазмер флэш: " + station.FlashSize + " байт\r\n";
            if (station.FlashSize < StationFlash.Size)
            {
                // check _selectedFlashSize
                RefreshFlashGrid(_selectedFlashSize, station.TeamBlockSize, _bytesPerRow);
            }

            byte[] b =
            {
                _uartBuffer[PacketBytes.DATA_START + 8], _uartBuffer[PacketBytes.DATA_START + 9],
                _uartBuffer[PacketBytes.DATA_START + 10], _uartBuffer[PacketBytes.DATA_START + 11]
            };
            station.VoltageCoefficient = Helpers.FloatConversion(b);
            result += "\tКоэфф. пересчета напряжения: " + station.VoltageCoefficient.ToString("F5") + "\r\n";

            station.AntennaGain = data[PacketBytes.DATA_START + 12];
            result += "\tКоэфф. усиления антенны: " + station.AntennaGain.ToString() + "\r\n";

            station.TeamBlockSize = (UInt16)(data[PacketBytes.DATA_START + 13] * 256 + data[PacketBytes.DATA_START + 14]);
            _bytesPerRow = station.TeamBlockSize;
            result += "\tРазмер блока команды: " + station.TeamBlockSize.ToString() + "\r\n";

            station.EraseBlockSize = data[PacketBytes.DATA_START + 15] * 256 + data[PacketBytes.DATA_START + 16];
            result += "\tРазмер стираемого блока: " + station.EraseBlockSize.ToString() + "\r\n";

            b = new[]
            {
                data[PacketBytes.DATA_START + 17], data[PacketBytes.DATA_START + 18],
                data[PacketBytes.DATA_START + 19], data[PacketBytes.DATA_START + 20]
            };
            station.BatteryLimit = Helpers.FloatConversion(b);
            result += "\tМинимальное напряжение батареи: " + station.BatteryLimit.ToString("F3") + "\r\n";

            Invoke((MethodInvoker)delegate
            {
                textBox_fwVersion.Text = station.FwVersion.ToString();
                comboBox_mode.SelectedItem = StationSettings.StationMode.FirstOrDefault(x => x.Value == station.Mode).Key;
                comboBox_chipType.SelectedIndex = station.ChipType;
                textBox_flashSize.Text = (int)(station.FlashSize / 1024 / 1024) + " Mb";
                // switch flash size combobox to new value if bigger than new FlashSize
                textBox_koeff.Text = station.VoltageCoefficient.ToString("F5");
                comboBox_setGain.SelectedItem = StationSettings.Gain.FirstOrDefault(x => x.Value == station.AntennaGain).Key;
                textBox_teamFlashSize.Text = station.TeamBlockSize.ToString();
                textBox_eraseBlock.Text = station.EraseBlockSize.ToString();
                textBox_setBatteryLimit.Text = station.BatteryLimit.ToString("F3");
            });

            return result;
        }

        private string reply_setVCoeff(byte[] data)
        {
            //0: код ошибки
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.DATA_START] == 0)
            {
                float.TryParse(textBox_koeff.Text, out float koeff);
                station.VoltageCoefficient = koeff;
            }

            return result;
        }

        private string reply_setGain(byte[] data)
        {
            //0: код ошибки
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";

            return result;
        }

        private string reply_setChipType(byte[] data)
        {
            //0: код ошибки
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.DATA_START] == 0 && _selectedChipType != station.ChipType)
            {
                RfidCard = new RfidContainer(_selectedChipType);
                RefreshChipGrid(station.ChipType);
            }
            return result;
        }

        private string reply_setTeamFlashSize(byte[] data)
        {
            //0: код ошибки
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            return result;
        }

        private string reply_setFlashBlockSize(byte[] data)
        {
            //0: код ошибки
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            return result;
        }

        private string reply_setBtName(byte[] data)
        {
            //0: код ошибки
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            return result;
        }

        private string reply_setBtPin(byte[] data)
        {
            //0: код ошибки
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            return result;
        }

        private string reply_setBatteryLimit(byte[] data)
        {
            //0: код ошибки
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.DATA_START] == 0)
            {
                float.TryParse(textBox_setBatteryLimit.Text, out float limit);
                station.BatteryLimit = limit;
            }

            return result;
        }

        private string reply_scanTeams(byte[] data)
        {
            //0: код ошибки
            //1-2: номер 1й команды
            //3-4: номер 2й команды           
            //...	                        
            //(n - 1) - n: номер последней команды
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.LENGTH] == 1 && data[PacketBytes.DATA_START] > 0)
                return result;

            result += "\tНомера отмеченных команд:\r\n";
            for (int i = 1; i < data[PacketBytes.LENGTH]; i = i + 2)
            {
                UInt16 n = (UInt16)(data[PacketBytes.DATA_START + i] * 256 + data[PacketBytes.DATA_START + i + 1]);
                result += "\t\t" + n + "\r\n";
                TeamsContainer.TeamData tmpTeam = new TeamsContainer.TeamData();
                tmpTeam.TeamNumber = n;
                Teams.Add(tmpTeam);
            }

            if (data[PacketBytes.LENGTH] < 252 - 7) needMore = true;

            return result;
        }

        private string reply_sendBtCommand(byte[] data)
        {
            //0: код ошибки
            //1-n: ответ BT модуля
            string result = "";
            result += "Ответ:\r\n\tСтанция#: " + data[PacketBytes.STATION_NUMBER] + "\r\n";
            result += "\tОтвет команды: " + _replyStrings[data[PacketBytes.COMMAND] - 0x90] + "\r\n";
            result += "\tОшибка#: " + _errorCodesStrings[data[PacketBytes.DATA_START]] + "\r\n";
            if (data[PacketBytes.LENGTH] == 1 && data[PacketBytes.DATA_START] > 0)
                return result;

            List<byte> str = new List<byte>();
            for (int i = 1; i < data[PacketBytes.LENGTH]; i++)
            {
                str.Add(data[PacketBytes.DATA_START + i]);
            }
            result += "\tОтвет Bluetooth: " + Accessory.ConvertByteArrayToString(str.ToArray()) + "\r\n";

            return result;
        }

        #endregion

        #region Helpers

        private void RefreshFlashGrid(UInt32 flashSize, UInt32 teamDumpSize, UInt32 bytesPerRow)
        {
            StationFlash = new FlashContainer(flashSize, teamDumpSize, bytesPerRow);
            dataGridView_flashRawData.DataSource = StationFlash.Table;
            dataGridView_flashRawData.AutoGenerateColumns = true;
            //dataGridView_flashRawData.AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.AllCells;
            dataGridView_flashRawData.AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.None;
            dataGridView_flashRawData.AutoResizeColumns();
            dataGridView_flashRawData.ScrollBars = ScrollBars.Both;
            dataGridView_flashRawData.AllowUserToResizeColumns = true;
            dataGridView_flashRawData.AllowUserToOrderColumns = false;
            for (int i = 0; i < dataGridView_flashRawData.Columns.Count; i++)
            {
                dataGridView_flashRawData.Columns[i].SortMode = DataGridViewColumnSortMode.NotSortable;
            }
        }

        private void RefreshChipGrid(byte chipTypeId)
        {
            RfidCard = new RfidContainer(chipTypeId);
            dataGridView_chipRawData.DataSource = RfidCard.Table;
            dataGridView_chipRawData.AutoGenerateColumns = true;
            dataGridView_chipRawData.AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.AllCells;
            dataGridView_chipRawData.AutoResizeColumns();
            dataGridView_chipRawData.ScrollBars = ScrollBars.Both;
            dataGridView_chipRawData.AllowUserToResizeColumns = true;
            dataGridView_chipRawData.AllowUserToOrderColumns = false;
            for (int i = 0; i < dataGridView_chipRawData.Columns.Count; i++)
            {
                dataGridView_chipRawData.Columns[i].SortMode = DataGridViewColumnSortMode.NotSortable;
            }
        }

        private void RefreshTeamsGrid()
        {
            Teams = new TeamsContainer();
            dataGridView_teams.DataSource = Teams.Table;
            dataGridView_teams.AutoGenerateColumns = true;
            dataGridView_teams.AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.AllCells;
            dataGridView_teams.AutoResizeColumns();
            dataGridView_teams.ScrollBars = ScrollBars.Both;
            dataGridView_teams.AllowUserToResizeColumns = true;
            dataGridView_teams.AllowUserToOrderColumns = false;
            for (int i = 0; i < dataGridView_teams.Columns.Count; i++)
            {
                dataGridView_teams.Columns[i].SortMode = DataGridViewColumnSortMode.NotSortable;
            }
        }


        #endregion

        #region GUI

        public Form1()
        {
            InitializeComponent();
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            _logAutoSaveFile = Settings.Default.LogAutoSaveFile;
            if (_logAutoSaveFile != "") _logAutoSaveFlag = true;
            _logLinesLimit = Settings.Default.LogLinesLimit;
            _portSpeed = Settings.Default.BaudRate;
            serialPort1.Encoding = Encoding.GetEncoding(InputCodePage);
            //Serial init
            comboBox_portName.Items.Add("None");
            foreach (string portname in SerialPort.GetPortNames())
            {
                comboBox_portName.Items.Add(portname); //добавить порт в список
            }
            if (comboBox_portName.Items.Count == 1)
            {
                comboBox_portName.SelectedIndex = 0;
                button_openPort.Enabled = false;
            }
            else
            {
                comboBox_portName.SelectedIndex = comboBox_portName.Items.Count - 1;
            }

            foreach (var item in StationSettings.StationMode)
            {
                comboBox_mode.Items.Add(item.Key);
            }

            foreach (var item in StationSettings.Gain)
            {
                comboBox_setGain.Items.Add(item.Key);
            }

            foreach (var item in RfidContainer.ChipTypes.Types)
            {
                comboBox_chipType.Items.Add(item.Key);
            }

            foreach (var item in FlashSizeLimit)
            {
                comboBox_flashSize.Items.Add(item.Key);
            }

            textBox_setTime.Text = Helpers.DateToString(DateTime.Now.ToUniversalTime());
            comboBox_mode.SelectedIndex = 0;
            comboBox_setGain.SelectedIndex = 0;

            station = new StationSettings();

            Teams = new TeamsContainer();
            RefreshTeamsGrid();

            RfidCard = new RfidContainer(_selectedChipType);
            RefreshChipGrid(station.ChipType);

            StationFlash = new FlashContainer(_selectedFlashSize, station.TeamBlockSize, _bytesPerRow);
            RefreshFlashGrid(_selectedFlashSize, station.TeamBlockSize, _bytesPerRow);

            textBox_flashSize.Text = (int)(station.FlashSize / 1024 / 1024) + " Mb";
            textBox_teamFlashSize.Text = station.TeamBlockSize.ToString();

            comboBox_flashSize.SelectedIndex = 0;
            comboBox_chipType.SelectedIndex = 1;
            splitContainer1.FixedPanel = FixedPanel.Panel1;
        }

        private void checkBox_autoTime_CheckedChanged(object sender, EventArgs e)
        {
            textBox_setTime.Enabled = !checkBox_autoTime.Checked;
            textBox_setTime.Text = Helpers.DateToString(DateTime.Now.ToUniversalTime());
        }

        private void textBox_stationNumber_Leave(object sender, EventArgs e)
        {
            byte.TryParse(textBox_stationNumber.Text, out station.Number);
            textBox_stationNumber.Text = station.Number.ToString();
        }

        private void textBox_commandNumber_Leave(object sender, EventArgs e)
        {
            int.TryParse(textBox_teamNumber.Text, out int n);
            textBox_teamNumber.Text = n.ToString();
        }

        private void textBox_teamMask_Leave(object sender, EventArgs e)
        {
            if (textBox_teamMask.Text.Length > 16)
                textBox_teamMask.Text = textBox_teamMask.Text.Substring(0, 16);
            else if (textBox_teamMask.Text.Length < 16)
            {
                while (textBox_teamMask.Text.Length < 16)
                    textBox_teamMask.Text = "0" + textBox_teamMask.Text;
            }

            UInt16 n = Helpers.ConvertStringToMask(textBox_teamMask.Text);
            textBox_teamMask.Clear();
            for (int i = 15; i >= 0; i--)
                textBox_teamMask.Text = Helpers.ConvertMaskToString(n);
        }

        private void textBox_setTime_Leave(object sender, EventArgs e)
        {
            long t = Helpers.DateStringToUnixTime(textBox_setTime.Text);
            textBox_setTime.Text = Helpers.DateToString(Helpers.ConvertFromUnixTimestamp(t));
        }

        private void textBox_newStationNumber_Leave(object sender, EventArgs e)
        {
            byte.TryParse(textBox_newStationNumber.Text, out byte n);
            textBox_newStationNumber.Text = n.ToString();
        }

        private void textBox_checkedChips_Leave(object sender, EventArgs e)
        {
            int.TryParse(textBox_checkedChips.Text, out int n);
            textBox_checkedChips.Text = n.ToString();
        }

        private void textBox_lastCheck_Leave(object sender, EventArgs e)
        {
            long t = Helpers.DateStringToUnixTime(textBox_lastCheck.Text);
            textBox_lastCheck.Text = Helpers.DateToString(Helpers.ConvertFromUnixTimestamp(t));
        }

        private void textBox_issueTime_Leave(object sender, EventArgs e)
        {
            long t = Helpers.DateStringToUnixTime(textBox_issueTime.Text);
            textBox_issueTime.Text = Helpers.DateToString(Helpers.ConvertFromUnixTimestamp(t));
        }

        private void textBox_readChipPage_Leave(object sender, EventArgs e)
        {
            if (!textBox_readChipPage.Text.Contains('-')) textBox_readChipPage.Text = "0-" + textBox_readChipPage.Text;
            byte.TryParse(textBox_readChipPage.Text.Substring(0, textBox_readChipPage.Text.IndexOf("-", StringComparison.Ordinal)).Trim(), out byte from);
            byte.TryParse(textBox_readChipPage.Text.Substring(textBox_readChipPage.Text.IndexOf("-", StringComparison.Ordinal) + 1).Trim(), out byte to);
            if (to - from > (256 - 7 - ReplyDataLength.READ_CARD_PAGE) / 4) to = (256 - 7 - ReplyDataLength.READ_CARD_PAGE) / 4;
            textBox_readChipPage.Text = from + "-" + to;
        }

        private void textBox_writeChipPage_Leave(object sender, EventArgs e)
        {
            byte.TryParse(textBox_writeChipPage.Text, out byte n);
            textBox_writeChipPage.Text = n.ToString();
        }

        private void textBox_data_Leave(object sender, EventArgs e)
        {
            textBox_data.Text = Accessory.CheckHexString(textBox_data.Text);
            byte[] n = Accessory.ConvertHexToByteArray(textBox_data.Text);
            textBox_data.Text = Accessory.ConvertByteArrayToHex(n, 4);
        }

        private void textBox_uid_Leave(object sender, EventArgs e)
        {
            textBox_uid.Text = Accessory.CheckHexString(textBox_uid.Text);
            byte[] n = Accessory.ConvertHexToByteArray(textBox_uid.Text);
            textBox_uid.Text = Accessory.ConvertByteArrayToHex(n);
            if (textBox_uid.Text.Length > 24)
                textBox_uid.Text = textBox_uid.Text.Substring(0, 24);
            else if (textBox_uid.Text.Length < 24)
            {
                while (textBox_uid.Text.Length < 24)
                    textBox_uid.Text = "00 " + textBox_uid.Text;
            }
        }

        private void textBox_readFlash_Leave(object sender, EventArgs e)
        {
            long.TryParse(textBox_readFlashAddress.Text, out long from);
            textBox_readFlashAddress.Text = from.ToString();
        }

        private void textBox_writeAddr_Leave(object sender, EventArgs e)
        {
            int.TryParse(textBox_writeAddr.Text, out int n);
            textBox_writeAddr.Text = n.ToString();
        }

        private void textBox_flashData_Leave(object sender, EventArgs e)
        {
            textBox_flashData.Text = Accessory.CheckHexString(textBox_flashData.Text);
            byte[] n = Accessory.ConvertHexToByteArray(textBox_flashData.Text);
            textBox_flashData.Text = Accessory.ConvertByteArrayToHex(n, 256 - CommandDataLength.WRITE_FLASH);
        }

        private void tabControl_teamData_SelectedIndexChanged(object sender, EventArgs e)
        {
            if (tabControl_teamData.SelectedIndex == 0 && checkBox_autoScroll.Checked)
            {
                textBox_terminal.SelectionStart = textBox_terminal.Text.Length;
                textBox_terminal.ScrollToCaret();
            }
        }

        private void textBox_eraseSector_Leave(object sender, EventArgs e)
        {
            int.TryParse(textBox_eraseSector.Text, out int n);
            textBox_eraseSector.Text = n.ToString();
        }

        private void textBox_koeff_Leave(object sender, EventArgs e)
        {
            textBox_koeff.Text =
                textBox_koeff.Text.Replace('.', ',');
            float.TryParse(textBox_koeff.Text, out float koeff);
            textBox_koeff.Text = koeff.ToString("F5");
        }

        private void button_clearLog_Click(object sender, EventArgs e)
        {
            textBox_terminal.Clear();
        }

        private void button_clearTeams_Click(object sender, EventArgs e)
        {
            RefreshTeamsGrid();
        }

        private void button_clearRfid_Click(object sender, EventArgs e)
        {
            RefreshChipGrid(station.ChipType);
        }

        private void button_clearFlash_Click(object sender, EventArgs e)
        {
            RefreshFlashGrid(_selectedFlashSize, station.TeamBlockSize, _bytesPerRow);
        }

        private void button_saveLog_Click(object sender, EventArgs e)
        {
            saveFileDialog1.FileName = "station_" + station.Number.ToString() + ".log";
            saveFileDialog1.Title = "Save log to file";
            saveFileDialog1.DefaultExt = "txt";
            saveFileDialog1.Filter = "Text files|*.txt|All files|*.*";
            saveFileDialog1.ShowDialog();
        }

        private void button_saveTeams_Click(object sender, EventArgs e)
        {
            saveFileDialog1.FileName = "station_" + station.Number.ToString() + "_teams.csv";
            saveFileDialog1.Title = "Save teams to file";
            saveFileDialog1.DefaultExt = "csv";
            saveFileDialog1.Filter = "CSV files|*.csv";
            saveFileDialog1.ShowDialog();
        }

        private void button_saveRfid_Click(object sender, EventArgs e)
        {
            saveFileDialog1.FileName = "uid_" + dataGridView_chipRawData.Rows[0].Cells[2].Value + dataGridView_chipRawData.Rows[1].Cells[2].Value.ToString().Trim() + ".bin";
            saveFileDialog1.FileName = saveFileDialog1.FileName.Replace(' ', '_');
            saveFileDialog1.Title = "Save card dump to file";
            saveFileDialog1.DefaultExt = "bin";
            saveFileDialog1.Filter = "Binary files|*.bin|CSV files|*.csv";
            saveFileDialog1.ShowDialog();
        }

        private void button_saveFlash_Click(object sender, EventArgs e)
        {
            saveFileDialog1.FileName = "station_" + station.Number.ToString() + "_flash.bin";
            saveFileDialog1.Title = "Save flash dump to file";
            saveFileDialog1.DefaultExt = "txt";
            saveFileDialog1.Filter = "Binary files|*.bin|CSV files|*.csv";
            saveFileDialog1.ShowDialog();

        }

        private void saveFileDialog1_FileOk(object sender, CancelEventArgs e)
        {
            if (saveFileDialog1.Title == "Save log to file")
            {
                File.WriteAllText(saveFileDialog1.FileName, textBox_terminal.Text);
            }
            else if (saveFileDialog1.Title == "Save teams to file")
            {
                var sb = new StringBuilder();

                var headers = dataGridView_teams.Columns.Cast<DataGridViewColumn>();
                sb.AppendLine(string.Join(",", headers.Select(column => "\"" + column.HeaderText + "\"").ToArray()));

                foreach (DataGridViewRow row in dataGridView_teams.Rows)
                {
                    var cells = row.Cells.Cast<DataGridViewCell>();
                    sb.AppendLine(string.Join(",", cells.Select(cell => "\"" + cell.Value + "\"").ToArray()));
                }
                File.WriteAllText(saveFileDialog1.FileName, sb.ToString());
            }
            else if (saveFileDialog1.Title == "Save card dump to file")
            {
                if (saveFileDialog1.FilterIndex == 1)
                {
                    byte[] tmp = new byte[RfidCard.Dump.Length];
                    for (int i = 0; i < RfidCard.Dump.Length; i++)
                    {
                        tmp[i] = (byte)RfidCard.Dump[i];
                    }
                    File.WriteAllBytes(saveFileDialog1.FileName, tmp);
                }
                else if (saveFileDialog1.FilterIndex == 2)
                {
                    var sb = new StringBuilder();

                    var headers = RfidCard.Table.Columns.Cast<DataColumn>();
                    sb.AppendLine(string.Join(",", headers.Select(column => "\"" + column.ColumnName + "\"").ToArray()));

                    foreach (DataRow page in RfidCard.Table.Rows)
                    {
                        for (int i = 0; i < page.ItemArray.Count(); i++)
                        {
                            sb.Append(page.ItemArray[i].ToString() + ";");
                        }
                        sb.AppendLine();
                    }
                    File.WriteAllText(saveFileDialog1.FileName, sb.ToString());
                }
            }
            else if (saveFileDialog1.Title == "Save flash dump to file")
            {
                if (saveFileDialog1.FilterIndex == 1)
                {
                    File.WriteAllBytes(saveFileDialog1.FileName, StationFlash.Dump);
                }
                else if (saveFileDialog1.FilterIndex == 2)
                {
                    var sb = new StringBuilder();

                    var headers = StationFlash.Table.Columns.Cast<DataColumn>();
                    sb.AppendLine(string.Join(",", headers.Select(column => "\"" + column.ColumnName + "\"").ToArray()));

                    foreach (DataRow team in StationFlash.Table.Rows)
                    {
                        for (int i = 0; i < team.ItemArray.Count(); i++)
                        {
                            sb.Append(team.ItemArray[i].ToString() + ";");
                        }
                        sb.AppendLine();
                    }
                    File.WriteAllText(saveFileDialog1.FileName, sb.ToString());
                }
            }
        }

        private void button_dumpTeams_Click(object sender, EventArgs e)
        {
            button_dumpTeams.Enabled = false;
            button_getTeamRecord.Enabled = false;
            RefreshTeamsGrid();

            uint maxTeams = StationFlash.Size / station.TeamBlockSize;

            // get list of commands in flash
            byte[] scanTeams = new byte[CommandDataLength.SCAN_TEAMS];
            scanTeams[PacketBytes.COMMAND] = Command.SCAN_TEAMS;

            uint teamNum = 1;
            _noTerminalOutputFlag = true;
            _asyncFlag = 0;
            needMore = false;
            var startTime = DateTime.Now.ToUniversalTime();
            do
            {
                //0-1: какую запись
                scanTeams[PacketBytes.DATA_START] = (byte)(teamNum >> 8);
                scanTeams[PacketBytes.DATA_START + 1] = (byte)(teamNum & 0x00ff);
                _asyncFlag++;
                SendCommand(scanTeams);

                long timeout = 1000;
                while (_asyncFlag > 0)
                {
                    Accessory.Delay_ms(1);
                    if (timeout <= 0)
                        break;
                    timeout--;
                }

                if (dataGridView_teams.RowCount == 0 || !uint.TryParse(dataGridView_teams.Rows[dataGridView_teams.RowCount - 1].Cells[0].Value.ToString(),
                    out teamNum))
                {
                    return;
                }
                if (!needMore) teamNum = maxTeams;

            } while (teamNum < maxTeams);
            SetText("\r\nTeams list time=" +
                    DateTime.Now.ToUniversalTime().Subtract(startTime).TotalMilliseconds + " ms.\r\n");

            // load every command data
            byte[] getTeamRecord = new byte[CommandDataLength.GET_TEAM_RECORD];
            getTeamRecord[PacketBytes.COMMAND] = Command.GET_TEAM_RECORD;

            int rowNum = 0;
            _noTerminalOutputFlag = true;
            _asyncFlag = 0;
            startTime = DateTime.Now.ToUniversalTime();
            while (rowNum < dataGridView_teams.RowCount)
            {
                if (!uint.TryParse(
                        dataGridView_teams.Rows[rowNum].Cells[0].Value.ToString(),
                        out teamNum))
                {
                    break;
                }

                //0-1: какую запись
                getTeamRecord[PacketBytes.DATA_START] = (byte)(teamNum >> 8);
                getTeamRecord[PacketBytes.DATA_START + 1] = (byte)(teamNum & 0x00ff);
                _asyncFlag++;
                SendCommand(getTeamRecord);
                rowNum++;

                long timeout = 1000;
                while (_asyncFlag > 0)
                {
                    Accessory.Delay_ms(1);
                    if (timeout <= 0)
                        break;
                    timeout--;
                }
            }

            SetText("\r\nTeams dump time=" +
                    DateTime.Now.ToUniversalTime().Subtract(startTime).TotalMilliseconds + " ms.\r\n");

            dataGridView_teams.Refresh();
            dataGridView_teams.PerformLayout();

            _noTerminalOutputFlag = false;
            button_dumpTeams.Enabled = true;
            button_getTeamRecord.Enabled = true;
        }

        private void button_dumpChip_Click(object sender, EventArgs e)
        {
            RefreshChipGrid(station.ChipType);
            button_dumpChip.Enabled = false;
            button_readChipPage.Enabled = false;
            byte[] readCardPage = new byte[CommandDataLength.READ_CARD_PAGE];
            readCardPage[PacketBytes.COMMAND] = Command.READ_CARD_PAGE;

            byte chipSize = RfidContainer.ChipTypes.GetSize(RfidCard.CurrentChipType);
            byte maxFramePages = 45;
            int pagesFrom = 0;
            int pagesTo;
            _noTerminalOutputFlag = true;
            _asyncFlag = 0;
            var startTime = DateTime.Now.ToUniversalTime();
            do
            {
                pagesTo = pagesFrom + maxFramePages - 1;
                if (pagesTo >= chipSize)
                    pagesTo = chipSize - 1;

                //0: с какой страницу карты
                readCardPage[PacketBytes.DATA_START] = (byte)pagesFrom;
                //1: по какую страницу карты включительно
                readCardPage[PacketBytes.DATA_START + 1] = (byte)pagesTo;
                _asyncFlag++;
                SendCommand(readCardPage);
                pagesFrom = pagesTo + 1;
                long timeout = 1000;
                while (_asyncFlag > 0)
                {
                    Accessory.Delay_ms(1);
                    if (timeout <= 0)
                        break;
                    timeout--;
                }
            } while (pagesTo < chipSize - 1);
            SetText("\r\nRFID dump time=" +
                    DateTime.Now.ToUniversalTime().Subtract(startTime).TotalMilliseconds +
                    " ms.\r\n");

            dataGridView_chipRawData.Refresh();
            dataGridView_chipRawData.PerformLayout();

            _noTerminalOutputFlag = false;
            button_dumpChip.Enabled = true;
            button_readChipPage.Enabled = true;
        }

        private void button_dumpFlash_Click(object sender, EventArgs e)
        {
            RefreshFlashGrid(_selectedFlashSize, station.TeamBlockSize, _bytesPerRow);
            button_dumpFlash.Enabled = false;
            button_readFlash.Enabled = false;
            byte[] readFlash = new byte[CommandDataLength.READ_FLASH];
            readFlash[PacketBytes.COMMAND] = Command.READ_FLASH;

            byte maxFrameBytes = 256 - 7 - ReplyDataLength.READ_FLASH - 1;
            int currentRow = 0;
            long addrFrom = 0;
            long addrTo;
            _noTerminalOutputFlag = true;
            _asyncFlag = 0;
            var startTime = DateTime.Now.ToUniversalTime();
            do
            {
                addrTo = addrFrom + maxFrameBytes;
                if (addrTo >= StationFlash.Size)
                    addrTo = StationFlash.Size;

                readFlash[PacketBytes.DATA_START] = (byte)((addrFrom & 0xFF000000) >> 24);
                readFlash[PacketBytes.DATA_START + 1] = (byte)((addrFrom & 0x00FF0000) >> 16);
                readFlash[PacketBytes.DATA_START + 2] = (byte)((addrFrom & 0x0000FF00) >> 8);
                readFlash[PacketBytes.DATA_START + 3] = (byte)(addrFrom & 0x000000FF);

                readFlash[PacketBytes.DATA_START + 4] = (byte)(addrTo - addrFrom);
                _asyncFlag++;
                SendCommand(readFlash);
                addrFrom = addrTo;

                long timeout = 1000;
                while (_asyncFlag > 0)
                {
                    Accessory.Delay_ms(1);
                    if (timeout <= 0)
                        break;
                    timeout--;
                }
                //check if it's time to update grid
                if ((int)(addrFrom / _bytesPerRow) > currentRow)
                {
                    currentRow = (int)(addrFrom / _bytesPerRow);
                }
            } while (addrTo < StationFlash.Size);

            SetText("\r\nFlash dump time=" +
                    DateTime.Now.ToUniversalTime().Subtract(startTime).TotalMilliseconds + " ms.\r\n");

            _noTerminalOutputFlag = false;
            dataGridView_flashRawData.Refresh();
            dataGridView_flashRawData.PerformLayout();

            button_dumpFlash.Enabled = true;
            button_readFlash.Enabled = true;
        }

        private void button_quickDump_Click(object sender, EventArgs e)
        {
            button_quickDump.Enabled = false;
            button_dumpFlash.Enabled = false;

            uint maxTeams = StationFlash.Size / station.TeamBlockSize;

            // load every command data
            int rowNum = 0;
            _noTerminalOutputFlag = true;
            _asyncFlag = 0;
            DateTime startTime = DateTime.Now.ToUniversalTime();
            while (rowNum < dataGridView_teams.RowCount)
            {
                if (!ushort.TryParse(
                    dataGridView_teams.Rows[rowNum].Cells[0].Value.ToString(),
                    out ushort teamNum) || teamNum >= dataGridView_flashRawData.RowCount)
                {
                    break;
                }
                dataGridView_flashRawData_CellDoubleClick(this, new DataGridViewCellEventArgs(0, teamNum));
                rowNum++;
            }

            SetText("\r\nTeams dump time=" +
                    DateTime.Now.ToUniversalTime().Subtract(startTime).TotalMilliseconds + " ms.\r\n");

            dataGridView_teams.Refresh();
            dataGridView_teams.PerformLayout();

            _noTerminalOutputFlag = false;
            button_quickDump.Enabled = true;
            button_dumpFlash.Enabled = true;
        }

        private void dataGridView_teams_CellDoubleClick(object sender, DataGridViewCellEventArgs e)
        {
            if (!serialPort1.IsOpen || e.ColumnIndex < 0 || e.RowIndex < 0) return;

            byte[] getTeamRecord = new byte[CommandDataLength.GET_TEAM_RECORD];
            getTeamRecord[PacketBytes.COMMAND] = Command.GET_TEAM_RECORD;

            //0-1: какую запись
            int.TryParse(dataGridView_teams?.Rows[e.RowIndex].Cells[0]?.Value?.ToString(), out int teamNum);
            if (teamNum <= 0) return;

            getTeamRecord[PacketBytes.DATA_START] = (byte)(teamNum >> 8);
            getTeamRecord[PacketBytes.DATA_START + 1] = (byte)(teamNum & 0x00ff);
            _asyncFlag = 0;
            _asyncFlag++;
            SendCommand(getTeamRecord);

            long timeout = 1000;
            while (_asyncFlag > 0)
            {
                Accessory.Delay_ms(1);
                if (timeout <= 0)
                    break;
                timeout--;
            }

            dataGridView_teams.Refresh();
            dataGridView_teams.PerformLayout();
        }

        private void dataGridView_chipRawData_CellDoubleClick(object sender, DataGridViewCellEventArgs e)
        {
            if (!serialPort1.IsOpen || e.ColumnIndex < 0 || e.RowIndex < 0) return;

            byte[] readCardPage = new byte[CommandDataLength.READ_CARD_PAGE];
            readCardPage[PacketBytes.COMMAND] = Command.READ_CARD_PAGE;

            byte page = (byte)e.RowIndex;

            //0: с какой страницу карты
            readCardPage[PacketBytes.DATA_START] = page;
            //1: по какую страницу карты включительно
            readCardPage[PacketBytes.DATA_START + 1] = page;
            _asyncFlag = 0;
            _asyncFlag++;
            SendCommand(readCardPage);

            long timeout = 1000;
            while (_asyncFlag > 0)
            {
                Accessory.Delay_ms(1);
                if (timeout <= 0)
                    break;
                timeout--;
            }
            dataGridView_chipRawData.Refresh();
            dataGridView_chipRawData.PerformLayout();
        }

        private void dataGridView_flashRawData_CellDoubleClick(object sender, DataGridViewCellEventArgs e)
        {
            if (!serialPort1.IsOpen || e.ColumnIndex < 0 || e.RowIndex < 0) return;
            button_dumpFlash.Enabled = false;
            byte[] readFlash = new byte[CommandDataLength.READ_FLASH];
            readFlash[PacketBytes.COMMAND] = Command.READ_FLASH;

            int rowFrom = e.RowIndex;
            byte maxFrameBytes = 256 - 7 - ReplyDataLength.READ_FLASH - 1;
            long addrFrom = rowFrom * _bytesPerRow;
            long addrTo;
            long flashSize = addrFrom + _bytesPerRow;
            _asyncFlag = 0;
            do
            {
                addrTo = addrFrom + maxFrameBytes;
                if (addrTo >= flashSize)
                    addrTo = flashSize;

                readFlash[PacketBytes.DATA_START] = (byte)((addrFrom & 0xFF000000) >> 24);
                readFlash[PacketBytes.DATA_START + 1] = (byte)((addrFrom & 0x00FF0000) >> 16);
                readFlash[PacketBytes.DATA_START + 2] = (byte)((addrFrom & 0x0000FF00) >> 8);
                readFlash[PacketBytes.DATA_START + 3] = (byte)(addrFrom & 0x000000FF);

                readFlash[PacketBytes.DATA_START + 4] = (byte)(addrTo - addrFrom);
                _asyncFlag++;
                SendCommand(readFlash);
                addrFrom = addrTo;

                long timeout = 1000;
                while (_asyncFlag > 0)
                {
                    Accessory.Delay_ms(1);
                    if (timeout <= 0)
                        break;
                    timeout--;
                }
            } while (addrTo < flashSize);

            dataGridView_flashRawData.Refresh();
            dataGridView_flashRawData.PerformLayout();

            button_dumpFlash.Enabled = true;
        }

        private void ComboBox_flashSize_SelectedIndexChanged(object sender, EventArgs e)
        {
            if (comboBox_flashSize.SelectedIndex == 0)
                _selectedFlashSize = 32 * 1024;
            else if (comboBox_flashSize.SelectedIndex == 1)
                _selectedFlashSize = 64 * 1024;
            else if (comboBox_flashSize.SelectedIndex == 2)
                _selectedFlashSize = 128 * 1024;
            else if (comboBox_flashSize.SelectedIndex == 3)
                _selectedFlashSize = 256 * 1024;
            else if (comboBox_flashSize.SelectedIndex == 4)
                _selectedFlashSize = 512 * 1024;
            else if (comboBox_flashSize.SelectedIndex == 5)
                _selectedFlashSize = 1024 * 1024;
            else if (comboBox_flashSize.SelectedIndex == 6)
                _selectedFlashSize = 2048 * 1024;
            else if (comboBox_flashSize.SelectedIndex == 7)
                _selectedFlashSize = 4096 * 1024;
            else if (comboBox_flashSize.SelectedIndex == 8)
                _selectedFlashSize = 8192 * 1024;

            if (_selectedFlashSize > station.FlashSize)
            {
                _selectedFlashSize = station.FlashSize;
                comboBox_flashSize.SelectedIndex--;
            }
            RefreshFlashGrid(_selectedFlashSize, station.TeamBlockSize, _bytesPerRow);
        }

        private void Form1_FormClosing(object sender, FormClosingEventArgs e)
        {
            try
            {
                serialPort1.Close();
            }
            catch (Exception ex)
            {
            }
        }

        private void Button_eraseChip_Click(object sender, EventArgs e)
        {
            button_eraseChip.Enabled = false;
            byte[] writeCardPage = new byte[CommandDataLength.WRITE_CARD_PAGE];
            writeCardPage[PacketBytes.COMMAND] = Command.WRITE_CARD_PAGE;

            //0-7: UID чипа
            // read uid from card or use default
            byte[] uid = Accessory.ConvertHexToByteArray(textBox_uid.Text);

            if (uid.Length != 8)
                return;
            for (int i = 0; i <= 7; i++)
                writeCardPage[PacketBytes.DATA_START + i] = uid[i];

            //9-12: данные страницы карты (4 байта)
            for (int i = 0; i < 4; i++)
                writeCardPage[PacketBytes.DATA_START + 9 + i] = 0;

            int chipSize = RfidContainer.ChipTypes.GetSize(RfidCard.CurrentChipType);
            byte page;
            _asyncFlag = 0;
            var startTime = DateTime.Now.ToUniversalTime();
            for (page = 4; page < chipSize - 4; page++)
            {
                //8: номер страницы
                writeCardPage[PacketBytes.DATA_START + 8] = page;
                _asyncFlag++;
                SendCommand(writeCardPage);

                long timeout = 1000;
                while (_asyncFlag > 0)
                {
                    Accessory.Delay_ms(1);
                    if (timeout <= 0)
                        break;
                    timeout--;
                }
            }
            SetText("\r\nRFID clear time=" +
                    DateTime.Now.ToUniversalTime().Subtract(startTime).TotalMilliseconds + " ms.\r\n");
            button_eraseChip.Enabled = true;
        }

        private void TextBox_teamFlashSize_Leave(object sender, EventArgs e)
        {
            int.TryParse(textBox_teamFlashSize.Text, out int teamFlashSize);
            textBox_teamFlashSize.Text = teamFlashSize.ToString();
        }

        private void TextBox_eraseBlock_Leave(object sender, EventArgs e)
        {
            int.TryParse(textBox_eraseBlock.Text, out int teamFlashSize);
            textBox_eraseBlock.Text = teamFlashSize.ToString();
        }

        private void TextBox_initTeamNum_Leave(object sender, EventArgs e)
        {
            int.TryParse(textBox_initTeamNum.Text, out int n);
            textBox_initTeamNum.Text = n.ToString();
        }

        private void TextBox_initMask_Leave(object sender, EventArgs e)
        {
            if (textBox_initMask.Text.Length > 16) textBox_initMask.Text = textBox_initMask.Text.Substring(0, 16);
            else if (textBox_initMask.Text.Length < 16)
            {
                while (textBox_initMask.Text.Length < 16)
                    textBox_initMask.Text = "0" + textBox_initMask.Text;
            }

            UInt16 n = Helpers.ConvertStringToMask(textBox_initMask.Text);
            textBox_initMask.Clear();
            for (int i = 15; i >= 0; i--) textBox_initMask.Text = Helpers.ConvertMaskToString(n);

        }

        private void TextBox_readFlashLength_Leave(object sender, EventArgs e)
        {
            uint.TryParse(textBox_readFlashLength.Text, out uint toAddr);
            if (toAddr > 256 - 7 - ReplyDataLength.READ_FLASH - 1) toAddr = 256 - 7 - ReplyDataLength.READ_FLASH - 1;
            textBox_readFlashLength.Text = toAddr.ToString();
        }

        private void TextBox_BtName_Leave(object sender, EventArgs e)
        {
            if (textBox_BtName.Text == "") textBox_BtName.Text = "Sportduino-xx";
        }

        private void TextBox_BtPin_Leave(object sender, EventArgs e)
        {
            if (textBox_BtPin.Text == "") textBox_BtPin.Text = "1234";
            List<byte> pin = new List<byte>();
            pin.AddRange(Encoding.ASCII.GetBytes(textBox_BtPin.Text));
            for (int i = 0; i < pin.Count; i++)
            {
                if (pin[i] < 0x30 || pin[i] > 0x39)
                {
                    pin.RemoveAt(i);
                    i--;
                }
            }
            if (pin.Count > 16) pin.RemoveRange(16, pin.Count - 16);
            textBox_BtPin.Text = Encoding.UTF8.GetString(pin.ToArray());
        }

        private void ComboBox_chipType_SelectedIndexChanged(object sender, EventArgs e)
        {
            if (!RfidContainer.ChipTypes.Types.TryGetValue(comboBox_chipType.SelectedItem.ToString(), out byte n))
            {
                comboBox_chipType.SelectedItem = RfidContainer.ChipTypes.GetName(RfidCard.CurrentChipType);
                _selectedChipType = RfidCard.CurrentChipType;
            }
            else
            {
                _selectedChipType = n;
            }
        }

        private void TextBox_setBatteryLimit_Leave(object sender, EventArgs e)
        {
            textBox_setBatteryLimit.Text =
                textBox_setBatteryLimit.Text.Replace('.', ',');
            float.TryParse(textBox_setBatteryLimit.Text, out float limit);
            textBox_setBatteryLimit.Text = limit.ToString("F3");
        }

        private void Button_loadFlash_Click(object sender, EventArgs e)
        {
            openFileDialog1.Title = "Load flash dump";
            openFileDialog1.DefaultExt = "bin";
            openFileDialog1.Filter = "Binary files|*.bin";
            openFileDialog1.ShowDialog();
        }

        private void Button_loadRfid_Click(object sender, EventArgs e)
        {
            openFileDialog1.Title = "Load card dump";
            openFileDialog1.DefaultExt = "bin";
            openFileDialog1.Filter = "Binary files|*.bin";
            openFileDialog1.FileName = "";
            openFileDialog1.ShowDialog();
        }

        private void OpenFileDialog1_FileOk(object sender, CancelEventArgs e)
        {
            if (openFileDialog1.Title == "Load card dump")
            {
                byte[] data = File.ReadAllBytes(openFileDialog1.FileName);

                if (data.Length < 16) return;

                if (data[14] == 0x12)
                {
                    station.ChipType = RfidContainer.ChipTypes.Types["NTAG213"];
                }
                else if (data[14] == 0x3e)
                {
                    station.ChipType = RfidContainer.ChipTypes.Types["NTAG215"];
                }
                else if (data[14] == 0x6d)
                {
                    station.ChipType = RfidContainer.ChipTypes.Types["NTAG216"];
                }
                else return;

                RefreshChipGrid(station.ChipType);

                int pages = data.Length / RfidContainer.ChipTypes.PageSize;
                for (byte i = 0; i < pages; i++)
                {
                    byte[] tmp = new byte[RfidContainer.ChipTypes.PageSize];
                    for (int j = 0; j < tmp.Length; j++)
                    {
                        tmp[j] = data[i * RfidContainer.ChipTypes.PageSize + j];
                    }
                    RfidCard.AddPages(i, tmp);
                }
                dataGridView_chipRawData.Refresh();
                dataGridView_chipRawData.PerformLayout();
            }
            else if (openFileDialog1.Title == "Load flash dump")
            {
                byte[] data = File.ReadAllBytes(openFileDialog1.FileName);
                RefreshFlashGrid((UInt32)data.Length, station.TeamBlockSize, _bytesPerRow);
                StationFlash.Add(0, data);
            }
        }

        private void textBox_getTeamsList_Leave(object sender, EventArgs e)
        {
            int.TryParse(textBox_getTeamsList.Text, out int n);
            textBox_getTeamsList.Text = n.ToString();
        }

        #endregion

    }
}
