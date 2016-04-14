using System;
using System.IO;
using System.IO.Ports;
using System.ComponentModel;
using System.Text;
using System.Windows.Forms;
using System.Threading;
using System.Collections;

namespace BC_Logger_control
{
    public partial class Form1 : Form
    {
        public static System.Timers.Timer aTimer;
        public Form1()
        {
            InitializeComponent();
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            checkBox_delLog.Checked = false;
            serialPort1.Encoding = Encoding.GetEncoding(inputCodePage);
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
            else comboBox_portName.SelectedIndex = 0;
            textBox_setT.Text = getDateString();
        }

        private void button_openPort_Click(object sender, EventArgs e)
        {
            //if (serialPort1.IsOpen == true) comboBox_portName.SelectedIndex=0;
            if (comboBox_portName.SelectedIndex != 0)
            {
                //comboBox_portname1.Enabled = false;
                serialPort1.PortName = comboBox_portName.Text;
                serialPort1.BaudRate = 57600;
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
                //while (serialPort1.IsOpen == false) ;
                button_getS.Enabled = true;
                button_getL.Enabled = true;
                button_getD.Enabled = true;
                button_getLn.Enabled = true;
                button_getDn.Enabled = true;
                button_delL.Enabled = false;
                button_gett.Enabled = true;
                button_dlAll.Enabled = true;
                button_closePort.Enabled = true;
                button_openPort.Enabled = false;
                comboBox_portName.Enabled = false;
                button_closePort.Enabled = true;
                button_openPort.Enabled = false;
                checkBox_delLog.Enabled = true;
                checkBox_delLog.Checked = false;
                button_refresh.Enabled = false;
                checkBox_setEnable.Enabled = true;
                //aTimer.Enabled = true;
            }
        }

        private void button_setI_Click(object sender, EventArgs e)
        {
            serialPort1.WriteLine(textBox_setIcustom.Text + textBox_setI.Text);
            SetText(textBox_setIcustom.Text + textBox_setI.Text + "\r\n");
        }

        private void button_setC_Click(object sender, EventArgs e)
        {
            serialPort1.WriteLine(textBox_setCcustom.Text + textBox_setC.Text);
            SetText(textBox_setCcustom.Text + textBox_setC.Text + "\r\n");
        }

        private void button_setP_Click(object sender, EventArgs e)
        {
            serialPort1.WriteLine(textBox_setPcustom.Text + textBox_setP.Text);
            SetText(textBox_setPcustom.Text + textBox_setP.Text + "\r\n");
        }

        private void button_setT_Click(object sender, EventArgs e)
        {
            serialPort1.WriteLine(textBox_setTcustom.Text);
            SetText(textBox_setTcustom.Text + "\r\n");
            Thread.Sleep(100);
            if (checkBox_autoTime.Checked == true) textBox_setT.Text = getDateString();
            serialPort1.WriteLine(textBox_setT.Text);
            SetText(textBox_setT.Text + "\r\n");
        }

        private void button_getS_Click(object sender, EventArgs e)
        {
            serialPort1.WriteLine(textBox_getScustom.Text);
            SetText(textBox_getScustom.Text + "\r\n");
        }

        private void button_getL_Click(object sender, EventArgs e)
        {
            string fname = "scanner" + textBox_setI.Text + "-" + getDateString().Replace(':', '_').Replace(' ', '_') + ".csv";
            String logRX = "", errLines = "";
            byte crc = 0;
            checkBox_portMon.Checked = false;

            serialPort1.WriteLine(textBox_getLcustom.Text);
            SetText(textBox_getLcustom.Text + "\r\n");

            serialPort1.ReadTimeout = 500;
            bool flag = false;
            int i = 0;
            logRX = serialPort1.ReadLine()+"\n";
            SetText(logRX);
            if (logRX != "Command received: GETL\r\n") SetText("Error\r\n");
            logRX = serialPort1.ReadLine() + "\n";
            SetText(logRX);
            logRX = serialPort1.ReadLine() + "\n";
            SetText(logRX);
            logRX = serialPort1.ReadLine() + "\n";
            SetText(logRX);
            if (logRX != "====\r\n") SetText("Error\r\n");
            while (flag == false)
            {
                try
                {
                    logRX = serialPort1.ReadLine() + "\n";
                    if (logRX != "====\r\n")
                    {
                        int strLen = logRX.LastIndexOf(",");
                        crc = crcCalc(logRX.Substring(0, strLen));
                        int getcrc = 0;
                        strLen += 7;
                        if (int.TryParse(logRX.Substring(strLen, logRX.Length - strLen - 2), out getcrc) != true)
                        {
                            SetText("CRC not recognized in line " + i.ToString() + ": " + logRX);
                            errLines += i.ToString() + ", ";
                        }
                        if (crc == getcrc)
                        {
                            SetText(logRX);
                            try
                            {
                                File.AppendAllText(fname, logRX, Encoding.GetEncoding(inputCodePage));
                            }
                            catch (Exception ex)
                            {
                                MessageBox.Show("\r\nError write to file " + fname + ": " + ex.Message);
                            }
                        }
                        else
                        {
                            SetText("CRC error in line " + i.ToString() + ": " + logRX);
                        }
                        i++;
                    }
                    else
                    {
                        SetText(logRX);
                        flag = true;
                    }
                }
                catch (TimeoutException ex)
                {
                    SetText("Error reading string: timeout. " + ex.Message);
                    flag = true;
                }
            }
            SetText(i.ToString() + " strings received\r\n");
            SetText("Errors in strings: " + errLines + "\r\n");
            checkBox_portMon.Checked = true;

            i = 0;
            byte[] rx = new byte[5242880];
            while (serialPort1.BytesToRead > 0)
            {
                rx[i] = (byte)serialPort1.ReadByte();
                i++;
            }
            SetText(System.Text.Encoding.GetEncoding(inputCodePage).GetString(rx, 0, i));
        }

        private void button_getD_Click(object sender, EventArgs e)
        {
            string fname = "scanner" + textBox_setI.Text + "-" + getDateString().Replace(':','_').Replace(' ','_') + "_DEBUG.csv";
            String logRX = "", errLines="";
            byte crc = 0;
            checkBox_portMon.Checked = false;

            serialPort1.WriteLine(textBox_getDcustom.Text);
            SetText(textBox_getDcustom.Text + "\r\n");

            serialPort1.ReadTimeout = 500;
            bool flag = false;
            int i = 0;
            logRX = serialPort1.ReadLine() + "\n";
            SetText(logRX);
            if (logRX != "Command received: GETD\r\n") SetText("Error\r\n");
            logRX = serialPort1.ReadLine() + "\n";
            SetText(logRX);
            logRX = serialPort1.ReadLine() + "\n";
            SetText(logRX);
            logRX = serialPort1.ReadLine() + "\n";
            SetText(logRX);
            if (logRX != "====\r\n") SetText("Error\r\n");
            while (flag == false)
            {
                try
                {
                    logRX = serialPort1.ReadLine() + "\n";
                    if (logRX != "====\r\n")
                    {
                        int strLen = logRX.LastIndexOf(",");
                        crc = crcCalc(logRX.Substring(0, strLen));
                        int getcrc = 0;
                        strLen += 7;
                        if (int.TryParse(logRX.Substring(strLen, logRX.Length - strLen - 2), out getcrc) != true)
                        {
                            SetText("CRC not recognized in line " + i.ToString() + ": " + logRX);
                            errLines += i.ToString() + ", ";
                        }
                        if (crc == getcrc)
                        {
                            SetText(logRX);
                            try
                            {
                                File.AppendAllText(fname, logRX, Encoding.GetEncoding(inputCodePage));
                            }
                            catch (Exception ex)
                            {
                                MessageBox.Show("\r\nError write to file " + fname + ": " + ex.Message);
                            }
                        }
                        else
                        {
                            SetText("CRC error in line " + i.ToString() + ": " + logRX);
                            errLines += i.ToString() + ", ";
                        }
                        i++;
                    }
                    else
                    {
                        SetText(logRX);
                        flag = true;
                    }
                }
                catch (TimeoutException ex)
                {
                    SetText("Error reading string: timeout. " + ex.Message);
                    flag = true;
                }
            }
            SetText(i.ToString() + " strings received\r\n");
            SetText("Errors in strings: " + errLines + "\r\n");
            checkBox_portMon.Checked = true;
            //serialPort1_DataReceived(,EventArgs.Empty);
            i = 0;
            byte[] rx = new byte[5242880];
            while (serialPort1.BytesToRead > 0)
            {
                rx[i] = (byte)serialPort1.ReadByte();
                i++;
            }
            SetText(System.Text.Encoding.GetEncoding(inputCodePage).GetString(rx, 0, i));
        }

        private void button_getLn_Click(object sender, EventArgs e)
        {
            serialPort1.WriteLine(textBox_getLncustom.Text + textBox_getLn.Text);
            SetText(textBox_getLncustom.Text + textBox_getLn.Text + "\r\n");
        }

        private void button_getDn_Click(object sender, EventArgs e)
        {
            serialPort1.WriteLine(textBox_getDncustom.Text + textBox_getDn.Text);
            SetText(textBox_getDncustom.Text + textBox_getDn.Text + "\r\n");
        }

        private void button_delL_Click(object sender, EventArgs e)
        {
            serialPort1.WriteLine(textBox_delLogcustom.Text);
            SetText(textBox_delLogcustom.Text + "\r\n");
            checkBox_delLog.Checked = false;
        }

        private void button_end_Click(object sender, EventArgs e)
        {
            serialPort1.WriteLine(textBox_getTcustom.Text);
            SetText(textBox_getTcustom.Text + "\r\n");
            checkBox_delLog.Checked = false;
        }

        private void serialPort1_DataReceived(object sender, System.IO.Ports.SerialDataReceivedEventArgs e)
        {
            if (checkBox_portMon.Checked == true)
            {
                int i = 0;
                byte[] rx = new byte[5242880];
                while (serialPort1.BytesToRead > 0)
                {
                    rx[i] = (byte)serialPort1.ReadByte();
                    i++;
                }
                SetText(System.Text.Encoding.GetEncoding(inputCodePage).GetString(rx, 0, i));
            }
        }

        private void button_closePort_Click(object sender, EventArgs e)
        {
            if (serialPort1.IsOpen == true)
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
            button_closePort.Enabled = false;
            button_getS.Enabled = false;
            button_getL.Enabled = false;
            button_getD.Enabled = false;
            button_getLn.Enabled = false;
            button_getDn.Enabled = false;
            button_delL.Enabled = false;
            checkBox_delLog.Checked = false;
            checkBox_delLog.Enabled = false;
            button_gett.Enabled = false;
            button_dlAll.Enabled = false;
            //aTimer.Enabled = false;
            comboBox_portName.Enabled = true;
            button_openPort.Enabled = true;
            button_refresh.Enabled = true;
            checkBox_setEnable.Checked = false;
            checkBox_setEnable.Enabled = false;
        }

        private void serialPort1_ErrorReceived(object sender, System.IO.Ports.SerialErrorReceivedEventArgs e)
        {
            SetText("COM port error\r\n");
        }

        private void button_clear_Click(object sender, EventArgs e)
        {
            textBox_terminal.Text = "";
        }

        private void saveFileDialog1_FileOk(object sender, CancelEventArgs e)
        {
            File.WriteAllText(saveFileDialog1.FileName, textBox_terminal.Text);
        }

        private void button_saveFile_Click(object sender, EventArgs e)
        {
            saveFileDialog1.ShowDialog();
        }

        private void button_gettime_Click(object sender, EventArgs e)
        {
            textBox_setT.Text = getDateString();
        }

        private void button_getAll_Click(object sender, EventArgs e)
        {
            serialPort1.WriteLine(textBox_getScustom.Text);
            SetText(textBox_getScustom.Text + "\r\n");
            Thread.Sleep(100);
            serialPort1.WriteLine(textBox_getLcustom.Text);
            SetText(textBox_getLcustom.Text + "\r\n");
            while (serialPort1.BytesToRead > 0) Thread.Sleep(100);
            serialPort1.WriteLine(textBox_getDcustom.Text);
            SetText(textBox_getDcustom.Text + "\r\n");
        }

        private void button_setNumOnly_Click(object sender, EventArgs e)
        {
            if (checkBox_setN.Checked == true)
            {
                serialPort1.WriteLine(textBox_setNcustom.Text + "Y");
                SetText(textBox_setNcustom.Text + "Y" + "\r\n");
            }
            else
            {
                serialPort1.WriteLine(textBox_setNcustom.Text + "N");
                SetText(textBox_setNcustom.Text + "N" + "\r\n");
            }
        }

        private void button_setLengthCheck_Click(object sender, EventArgs e)
        {
            if (checkBox_setL.Checked == true)
            {
                serialPort1.WriteLine(textBox_setLcustom.Text + "Y");
                SetText(textBox_setLcustom.Text + "Y" + "\r\n");
            }
            else
            {
                serialPort1.WriteLine(textBox_setLcustom.Text + "N");
                SetText(textBox_setLcustom.Text + "N" + "\r\n");
            }
        }

        private void button_sendSet_Click(object sender, EventArgs e)
        {
            serialPort1.WriteLine(textBox_setIcustom.Text + textBox_setI.Text);
            SetText(textBox_setIcustom.Text + textBox_setI.Text + "\r\n");
            Thread.Sleep(100);
            serialPort1.WriteLine(textBox_setCcustom.Text + textBox_setC.Text);
            SetText(textBox_setCcustom.Text + textBox_setC.Text + "\r\n");
            Thread.Sleep(100);
            serialPort1.WriteLine(textBox_setPcustom.Text + textBox_setP.Text);
            SetText(textBox_setPcustom.Text + textBox_setP.Text + "\r\n");
            Thread.Sleep(100);
            if (checkBox_setN.Checked == true)
            {
                serialPort1.WriteLine(textBox_setNcustom.Text + "Y");
                SetText(textBox_setNcustom.Text + "Y" + "\r\n");
            }
            else
            {
                serialPort1.WriteLine(textBox_setNcustom.Text + "N");
                SetText(textBox_setNcustom.Text + "N" + "\r\n");
            }
            Thread.Sleep(100);
            if (checkBox_setL.Checked == true)
            {
                serialPort1.WriteLine(textBox_setLcustom.Text + "Y");
                SetText(textBox_setLcustom.Text + "Y" + "\r\n");
            }
            else
            {
                serialPort1.WriteLine(textBox_setLcustom.Text + "N");
                SetText(textBox_setLcustom.Text + "N" + "\r\n");
            }
            Thread.Sleep(100);
            serialPort1.WriteLine(textBox_setTcustom.Text);
            SetText(textBox_setTcustom.Text + "\r\n");
            Thread.Sleep(100);
            if (checkBox_autoTime.Checked == true) textBox_setT.Text = getDateString();
            serialPort1.WriteLine(textBox_setT.Text);
            SetText(textBox_setT.Text);
        }

        private void checkBox_delLog_CheckedChanged(object sender, EventArgs e)
        {
            if (checkBox_delLog.Checked == true) button_delL.Enabled = true;
            else button_delL.Enabled = false;
        }

        private void checkBox_autoTime_CheckedChanged(object sender, EventArgs e)
        {
            if (checkBox_autoTime.Checked == true)
            {
                button_gettime.Enabled = false;
                textBox_setT.Enabled = false;
            }
            else
            {
                button_gettime.Enabled = true;
                textBox_setT.Enabled = true;
            }
            textBox_setT.Text = getDateString();
        }

        private void textBox_terminal_TextChanged(object sender, EventArgs e)
        {
            if (checkBox_autoScroll.Checked == true)
            {
                textBox_terminal.SelectionStart = textBox_terminal.Text.Length;
                textBox_terminal.ScrollToCaret();
            }
        }

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
            else comboBox_portName.SelectedIndex = 0;


            Hashtable PortNames = new Hashtable();
            string[] ports = System.IO.Ports.SerialPort.GetPortNames();
            if (ports.Length == 0)
            {
                textBox_terminal.Text += "ERROR: No COM ports exist\n\r";
            }
            else
            {
                PortNames = BuildPortNameHash(ports);
                foreach (String s in PortNames.Keys)
                {
                    textBox_terminal.Text += "\n\r" + PortNames[s] + ": " + s + "\n\r";
                }
            }



        }

        private void checkBoxcustomEdit_CheckedChanged(object sender, EventArgs e)
        {
            if (checkBox_customEdit.Checked == true)
            {
                textBox_getScustom.Enabled = true;
                textBox_getLcustom.Enabled = true;
                textBox_getDcustom.Enabled = true;
                textBox_getLncustom.Enabled = true;
                textBox_getDncustom.Enabled = true;
                textBox_delLogcustom.Enabled = true;
                textBox_getTcustom.Enabled = true;
                textBox_setIcustom.Enabled = true;
                textBox_setCcustom.Enabled = true;
                textBox_setPcustom.Enabled = true;
                textBox_setTcustom.Enabled = true;
                textBox_setNcustom.Enabled = true;
                textBox_setLcustom.Enabled = true;
            }
            else
            {
                textBox_getScustom.Enabled = false;
                textBox_getLcustom.Enabled = false;
                textBox_getDcustom.Enabled = false;
                textBox_getLncustom.Enabled = false;
                textBox_getDncustom.Enabled = false;
                textBox_delLogcustom.Enabled = false;
                textBox_getTcustom.Enabled = false;
                textBox_setIcustom.Enabled = false;
                textBox_setCcustom.Enabled = false;
                textBox_setPcustom.Enabled = false;
                textBox_setTcustom.Enabled = false;
                textBox_setNcustom.Enabled = false;
                textBox_setLcustom.Enabled = false;
            }
        }

        private void checkBox_setEnable_CheckedChanged(object sender, EventArgs e)
        {
            if (checkBox_setEnable.Checked == true)
            {
                button_setI.Enabled = true;
                button_setC.Enabled = true;
                button_setP.Enabled = true;
                button_setC.Enabled = true;
                button_setT.Enabled = true;
                button_setL.Enabled = true;
                button_setN.Enabled = true;
                button_sendAll.Enabled = true;
            }
            else
            {
                button_setI.Enabled = false;
                button_setC.Enabled = false;
                button_setP.Enabled = false;
                button_setC.Enabled = false;
                button_setT.Enabled = false;
                button_setL.Enabled = false;
                button_setN.Enabled = false;
                button_sendAll.Enabled = false;
            }
        }

        private void button_gett_Click(object sender, EventArgs e)
        {
            serialPort1.WriteLine(textBox_getTcustom.Text);
            SetText(textBox_getTcustom.Text + "\r\n");
        }
    }
}
