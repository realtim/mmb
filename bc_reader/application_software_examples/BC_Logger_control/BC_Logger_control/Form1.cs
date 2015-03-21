using System;
using System.IO;
using System.IO.Ports;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace BC_Logger_control
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();
        }

        private void Form1_Load(object sender, EventArgs e)
        {
            serialPort1.Encoding = Encoding.Default;

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
            else comboBox_portName.SelectedIndex = 1;
        }

        
        private void button_openPort_Click(object sender, EventArgs e)
        {
            if (serialPort1.IsOpen == true) comboBox_portName.SelectedIndex=0;
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
                serialPort1.Open();
                while (serialPort1.IsOpen == false) ;
                
                button_setI.Enabled = true;
                button_setC.Enabled = true;
                button_setP.Enabled = true;
                button_setC.Enabled = true;
                button_setT.Enabled = true;
                checkBox_setL.Enabled = true;
                checkBox_setN.Enabled = true;
                button_getCurSet.Enabled = true;
                button_getS.Enabled = true;
                button_getL.Enabled = true;
                button_getD.Enabled = true;
                button_getLn.Enabled = true;
                button_getDn.Enabled = true;
                button_delL.Enabled = true;
                button_end.Enabled = true;
                button_sync.Enabled = true;

                //button_send.Enabled = true;
                button_closePort.Enabled = true;
                button_openPort.Enabled = false;
            }
              comboBox_portName.Enabled = false;
              //button_send.Enabled = true;
              button_closePort.Enabled = true;
              button_openPort.Enabled = false;
        }



        private void button_setI_Click(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            serialPort1.WriteLine("SETI" + textBox_setI.Text);
            serialPort1.WriteLine("");
            //SetText1("SETI" + textBox_setI.Text);
            //textBox_terminal += "\n";
            //inStr=serialPort1.ReadLine();
            //SetText1(inStr);
            //inStr = serialPort1.ReadLine();
            //SetText1(inStr);
            //if (inStr == ("Scanner ID set to: " + textBox_setI.Text)) SetText1("OK\n");
        }

        private void button_setC_Click(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            serialPort1.WriteLine("SETC" + textBox_setC.Text);
        }

        private void button_setP_Click(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            serialPort1.WriteLine("SETP" + textBox_setP.Text);
        }

        private void button_setT_Click(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            serialPort1.WriteLine("SETT");
            Thread.Sleep(1000);
            serialPort1.WriteLine(textBox_setT.Text);
        }

        private void checkBox_setL_CheckedChanged(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            if (checkBox_setL.Checked == true) serialPort1.WriteLine("SETLY");
            else serialPort1.WriteLine("SETLN");
        }

        private void checkBox_setN_CheckedChanged(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            if (checkBox_setN.Checked == true) serialPort1.WriteLine("SETNY");
            else serialPort1.WriteLine("SETNN");
        }

        private void button_getCurSet_Click(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            serialPort1.WriteLine("GETS");
            //serialPort1.ReadLine();
        }

        private void button_getS_Click(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            serialPort1.WriteLine("GETS");
        }

        private void button_getL_Click(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            serialPort1.WriteLine("GETL");
        }

        private void button_getD_Click(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            serialPort1.WriteLine("GETD");
        }

        private void button_getLn_Click(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            serialPort1.WriteLine("GET#L" + textBox_getLn.Text);
        }

        private void button_getDn_Click(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            serialPort1.WriteLine("GET#D" + textBox_getDn.Text);
        }

        private void button_delL_Click(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            serialPort1.WriteLine("DELLOG");
        }

        private void button_end_Click(object sender, EventArgs e)
        {
            serialPort1.DiscardInBuffer();
            serialPort1.WriteLine("END");
        }

        
        private void serialPort1_DataReceived(object sender, System.IO.Ports.SerialDataReceivedEventArgs e)
        {
            SetText1(serialPort1.ReadExisting().ToString());
        }
        

        private void button_closePort_Click(object sender, EventArgs e)
        {
            if (serialPort1.IsOpen==true)
            {
                serialPort1.Close();
                while (serialPort1.IsOpen == true) ;
            }

            comboBox_portName.Enabled = true;

            button_openPort.Enabled = true;
            button_closePort.Enabled = false;
            button_setI.Enabled = false;
            button_setC.Enabled = false;
            button_setP.Enabled = false;
            button_setC.Enabled = false;
            button_setT.Enabled = false;
            checkBox_setL.Enabled = false;
            checkBox_setN.Enabled = false;
            button_getCurSet.Enabled = false;
            button_getS.Enabled = false;
            button_getL.Enabled = false;
            button_getD.Enabled = false;
            button_getLn.Enabled = false;
            button_getDn.Enabled = false;
            button_delL.Enabled = false;
            button_end.Enabled = false;
            button_sync.Enabled = false;

        }

        private void serialPort1_ErrorReceived(object sender, System.IO.Ports.SerialErrorReceivedEventArgs e)
        {
            SetText1("COM port error");
        }

        private void button_clear_Click(object sender, EventArgs e)
        {
            textBox_terminal.Text = "";
        }

        private void saveFileDialog1_FileOk(object sender, CancelEventArgs e)
        {
            File.WriteAllText(saveFileDialog1.FileName, textBox_terminal.Text);
        }

        private void button1_Click(object sender, EventArgs e)
        {
            saveFileDialog1.ShowDialog();
        }

    }
}
