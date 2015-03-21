namespace BC_Logger_control
{
    partial class Form1
    {
        /// <summary>
        /// Требуется переменная конструктора.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Освободить все используемые ресурсы.
        /// </summary>
        /// <param name="disposing">истинно, если управляемый ресурс должен быть удален; иначе ложно.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Код, автоматически созданный конструктором форм Windows

        /// <summary>
        /// Обязательный метод для поддержки конструктора - не изменяйте
        /// содержимое данного метода при помощи редактора кода.
        /// </summary>
        private void InitializeComponent()
        {
            this.components = new System.ComponentModel.Container();
            this.tabControl = new System.Windows.Forms.TabControl();
            this.tabPage_set = new System.Windows.Forms.TabPage();
            this.button_gettime = new System.Windows.Forms.Button();
            this.button_getCurSet = new System.Windows.Forms.Button();
            this.checkBox_setL = new System.Windows.Forms.CheckBox();
            this.checkBox_setN = new System.Windows.Forms.CheckBox();
            this.textBox_setT = new System.Windows.Forms.TextBox();
            this.textBox_setP = new System.Windows.Forms.TextBox();
            this.textBox_setC = new System.Windows.Forms.TextBox();
            this.textBox_setI = new System.Windows.Forms.TextBox();
            this.button_setT = new System.Windows.Forms.Button();
            this.button_setP = new System.Windows.Forms.Button();
            this.button_setC = new System.Windows.Forms.Button();
            this.button_setI = new System.Windows.Forms.Button();
            this.tabPage_get = new System.Windows.Forms.TabPage();
            this.textBox_getDn = new System.Windows.Forms.TextBox();
            this.textBox_getLn = new System.Windows.Forms.TextBox();
            this.button_sync = new System.Windows.Forms.Button();
            this.button_end = new System.Windows.Forms.Button();
            this.button_delL = new System.Windows.Forms.Button();
            this.button_getDn = new System.Windows.Forms.Button();
            this.button_getLn = new System.Windows.Forms.Button();
            this.button_getD = new System.Windows.Forms.Button();
            this.button_getL = new System.Windows.Forms.Button();
            this.button_getS = new System.Windows.Forms.Button();
            this.textBox_terminal = new System.Windows.Forms.TextBox();
            this.serialPort1 = new System.IO.Ports.SerialPort(this.components);
            this.saveFileDialog1 = new System.Windows.Forms.SaveFileDialog();
            this.comboBox_portName = new System.Windows.Forms.ComboBox();
            this.label1 = new System.Windows.Forms.Label();
            this.button_openPort = new System.Windows.Forms.Button();
            this.button_clear = new System.Windows.Forms.Button();
            this.button_closePort = new System.Windows.Forms.Button();
            this.button1 = new System.Windows.Forms.Button();
            this.tabControl.SuspendLayout();
            this.tabPage_set.SuspendLayout();
            this.tabPage_get.SuspendLayout();
            this.SuspendLayout();
            // 
            // tabControl
            // 
            this.tabControl.Controls.Add(this.tabPage_set);
            this.tabControl.Controls.Add(this.tabPage_get);
            this.tabControl.Location = new System.Drawing.Point(1, 0);
            this.tabControl.Name = "tabControl";
            this.tabControl.SelectedIndex = 0;
            this.tabControl.Size = new System.Drawing.Size(227, 207);
            this.tabControl.TabIndex = 0;
            // 
            // tabPage_set
            // 
            this.tabPage_set.Controls.Add(this.button_gettime);
            this.tabPage_set.Controls.Add(this.button_getCurSet);
            this.tabPage_set.Controls.Add(this.checkBox_setL);
            this.tabPage_set.Controls.Add(this.checkBox_setN);
            this.tabPage_set.Controls.Add(this.textBox_setT);
            this.tabPage_set.Controls.Add(this.textBox_setP);
            this.tabPage_set.Controls.Add(this.textBox_setC);
            this.tabPage_set.Controls.Add(this.textBox_setI);
            this.tabPage_set.Controls.Add(this.button_setT);
            this.tabPage_set.Controls.Add(this.button_setP);
            this.tabPage_set.Controls.Add(this.button_setC);
            this.tabPage_set.Controls.Add(this.button_setI);
            this.tabPage_set.Location = new System.Drawing.Point(4, 22);
            this.tabPage_set.Name = "tabPage_set";
            this.tabPage_set.Padding = new System.Windows.Forms.Padding(3);
            this.tabPage_set.Size = new System.Drawing.Size(219, 181);
            this.tabPage_set.TabIndex = 0;
            this.tabPage_set.Text = "Settings";
            this.tabPage_set.UseVisualStyleBackColor = true;
            // 
            // button_gettime
            // 
            this.button_gettime.Enabled = false;
            this.button_gettime.Location = new System.Drawing.Point(123, 122);
            this.button_gettime.Name = "button_gettime";
            this.button_gettime.Size = new System.Drawing.Size(89, 23);
            this.button_gettime.TabIndex = 3;
            this.button_gettime.Text = "Get time";
            this.button_gettime.UseVisualStyleBackColor = true;
            this.button_gettime.Click += new System.EventHandler(this.button_getCurSet_Click);
            // 
            // button_getCurSet
            // 
            this.button_getCurSet.Enabled = false;
            this.button_getCurSet.Location = new System.Drawing.Point(123, 6);
            this.button_getCurSet.Name = "button_getCurSet";
            this.button_getCurSet.Size = new System.Drawing.Size(89, 52);
            this.button_getCurSet.TabIndex = 3;
            this.button_getCurSet.Text = "Get settings";
            this.button_getCurSet.UseVisualStyleBackColor = true;
            this.button_getCurSet.Click += new System.EventHandler(this.button_getCurSet_Click);
            // 
            // checkBox_setL
            // 
            this.checkBox_setL.AutoSize = true;
            this.checkBox_setL.Enabled = false;
            this.checkBox_setL.Location = new System.Drawing.Point(7, 122);
            this.checkBox_setL.Name = "checkBox_setL";
            this.checkBox_setL.Size = new System.Drawing.Size(92, 17);
            this.checkBox_setL.TabIndex = 2;
            this.checkBox_setL.Text = "Length check";
            this.checkBox_setL.UseVisualStyleBackColor = true;
            this.checkBox_setL.CheckedChanged += new System.EventHandler(this.checkBox_setL_CheckedChanged);
            // 
            // checkBox_setN
            // 
            this.checkBox_setN.AutoSize = true;
            this.checkBox_setN.Enabled = false;
            this.checkBox_setN.Location = new System.Drawing.Point(8, 145);
            this.checkBox_setN.Name = "checkBox_setN";
            this.checkBox_setN.Size = new System.Drawing.Size(74, 17);
            this.checkBox_setN.TabIndex = 2;
            this.checkBox_setN.Text = "Digits only";
            this.checkBox_setN.UseVisualStyleBackColor = true;
            this.checkBox_setN.CheckedChanged += new System.EventHandler(this.checkBox_setN_CheckedChanged);
            // 
            // textBox_setT
            // 
            this.textBox_setT.Location = new System.Drawing.Point(88, 96);
            this.textBox_setT.MaxLength = 20;
            this.textBox_setT.Name = "textBox_setT";
            this.textBox_setT.Size = new System.Drawing.Size(124, 20);
            this.textBox_setT.TabIndex = 1;
            this.textBox_setT.Text = "Mar 01 2015 12:00:00";
            // 
            // textBox_setP
            // 
            this.textBox_setP.Location = new System.Drawing.Point(89, 67);
            this.textBox_setP.MaxLength = 100;
            this.textBox_setP.Name = "textBox_setP";
            this.textBox_setP.Size = new System.Drawing.Size(123, 20);
            this.textBox_setP.TabIndex = 1;
            this.textBox_setP.Text = "********";
            // 
            // textBox_setC
            // 
            this.textBox_setC.Location = new System.Drawing.Point(89, 38);
            this.textBox_setC.MaxLength = 2;
            this.textBox_setC.Name = "textBox_setC";
            this.textBox_setC.Size = new System.Drawing.Size(28, 20);
            this.textBox_setC.TabIndex = 1;
            this.textBox_setC.Text = "01";
            // 
            // textBox_setI
            // 
            this.textBox_setI.Location = new System.Drawing.Point(89, 8);
            this.textBox_setI.MaxLength = 2;
            this.textBox_setI.Name = "textBox_setI";
            this.textBox_setI.Size = new System.Drawing.Size(28, 20);
            this.textBox_setI.TabIndex = 1;
            this.textBox_setI.Text = "01";
            // 
            // button_setT
            // 
            this.button_setT.Enabled = false;
            this.button_setT.Location = new System.Drawing.Point(7, 93);
            this.button_setT.Name = "button_setT";
            this.button_setT.Size = new System.Drawing.Size(75, 23);
            this.button_setT.TabIndex = 0;
            this.button_setT.Text = "Time";
            this.button_setT.UseVisualStyleBackColor = true;
            this.button_setT.Click += new System.EventHandler(this.button_setT_Click);
            // 
            // button_setP
            // 
            this.button_setP.Enabled = false;
            this.button_setP.Location = new System.Drawing.Point(7, 64);
            this.button_setP.Name = "button_setP";
            this.button_setP.Size = new System.Drawing.Size(75, 23);
            this.button_setP.TabIndex = 0;
            this.button_setP.Text = "Pattern";
            this.button_setP.UseVisualStyleBackColor = true;
            this.button_setP.Click += new System.EventHandler(this.button_setP_Click);
            // 
            // button_setC
            // 
            this.button_setC.Enabled = false;
            this.button_setC.Location = new System.Drawing.Point(7, 35);
            this.button_setC.Name = "button_setC";
            this.button_setC.Size = new System.Drawing.Size(75, 23);
            this.button_setC.TabIndex = 0;
            this.button_setC.Text = "Control point";
            this.button_setC.UseVisualStyleBackColor = true;
            this.button_setC.Click += new System.EventHandler(this.button_setC_Click);
            // 
            // button_setI
            // 
            this.button_setI.Enabled = false;
            this.button_setI.Location = new System.Drawing.Point(7, 6);
            this.button_setI.Name = "button_setI";
            this.button_setI.Size = new System.Drawing.Size(75, 23);
            this.button_setI.TabIndex = 0;
            this.button_setI.Text = "Scanner ID";
            this.button_setI.UseVisualStyleBackColor = true;
            this.button_setI.Click += new System.EventHandler(this.button_setI_Click);
            // 
            // tabPage_get
            // 
            this.tabPage_get.Controls.Add(this.textBox_getDn);
            this.tabPage_get.Controls.Add(this.textBox_getLn);
            this.tabPage_get.Controls.Add(this.button_sync);
            this.tabPage_get.Controls.Add(this.button_end);
            this.tabPage_get.Controls.Add(this.button_delL);
            this.tabPage_get.Controls.Add(this.button_getDn);
            this.tabPage_get.Controls.Add(this.button_getLn);
            this.tabPage_get.Controls.Add(this.button_getD);
            this.tabPage_get.Controls.Add(this.button_getL);
            this.tabPage_get.Controls.Add(this.button_getS);
            this.tabPage_get.Location = new System.Drawing.Point(4, 22);
            this.tabPage_get.Name = "tabPage_get";
            this.tabPage_get.Padding = new System.Windows.Forms.Padding(3);
            this.tabPage_get.Size = new System.Drawing.Size(219, 181);
            this.tabPage_get.TabIndex = 1;
            this.tabPage_get.Text = "Get data";
            this.tabPage_get.UseVisualStyleBackColor = true;
            // 
            // textBox_getDn
            // 
            this.textBox_getDn.Location = new System.Drawing.Point(102, 126);
            this.textBox_getDn.MaxLength = 6;
            this.textBox_getDn.Name = "textBox_getDn";
            this.textBox_getDn.Size = new System.Drawing.Size(111, 20);
            this.textBox_getDn.TabIndex = 2;
            this.textBox_getDn.Text = "0";
            // 
            // textBox_getLn
            // 
            this.textBox_getLn.Location = new System.Drawing.Point(102, 96);
            this.textBox_getLn.MaxLength = 6;
            this.textBox_getLn.Name = "textBox_getLn";
            this.textBox_getLn.Size = new System.Drawing.Size(111, 20);
            this.textBox_getLn.TabIndex = 2;
            this.textBox_getLn.Text = "0";
            // 
            // button_sync
            // 
            this.button_sync.Enabled = false;
            this.button_sync.Location = new System.Drawing.Point(102, 7);
            this.button_sync.Name = "button_sync";
            this.button_sync.Size = new System.Drawing.Size(111, 81);
            this.button_sync.TabIndex = 1;
            this.button_sync.Text = "Sync all";
            this.button_sync.UseVisualStyleBackColor = true;
            // 
            // button_end
            // 
            this.button_end.Enabled = false;
            this.button_end.Location = new System.Drawing.Point(102, 152);
            this.button_end.Name = "button_end";
            this.button_end.Size = new System.Drawing.Size(111, 23);
            this.button_end.TabIndex = 0;
            this.button_end.Text = "Finish";
            this.button_end.UseVisualStyleBackColor = true;
            this.button_end.Click += new System.EventHandler(this.button_end_Click);
            // 
            // button_delL
            // 
            this.button_delL.Enabled = false;
            this.button_delL.Location = new System.Drawing.Point(8, 152);
            this.button_delL.Name = "button_delL";
            this.button_delL.Size = new System.Drawing.Size(88, 23);
            this.button_delL.TabIndex = 0;
            this.button_delL.Text = "Clear device";
            this.button_delL.UseVisualStyleBackColor = true;
            this.button_delL.Click += new System.EventHandler(this.button_delL_Click);
            // 
            // button_getDn
            // 
            this.button_getDn.Enabled = false;
            this.button_getDn.Location = new System.Drawing.Point(8, 123);
            this.button_getDn.Name = "button_getDn";
            this.button_getDn.Size = new System.Drawing.Size(88, 23);
            this.button_getDn.TabIndex = 0;
            this.button_getDn.Text = "Get debug line";
            this.button_getDn.UseVisualStyleBackColor = true;
            this.button_getDn.Click += new System.EventHandler(this.button_getDn_Click);
            // 
            // button_getLn
            // 
            this.button_getLn.Enabled = false;
            this.button_getLn.Location = new System.Drawing.Point(8, 94);
            this.button_getLn.Name = "button_getLn";
            this.button_getLn.Size = new System.Drawing.Size(88, 23);
            this.button_getLn.TabIndex = 0;
            this.button_getLn.Text = "Get log line";
            this.button_getLn.UseVisualStyleBackColor = true;
            this.button_getLn.Click += new System.EventHandler(this.button_getLn_Click);
            // 
            // button_getD
            // 
            this.button_getD.Enabled = false;
            this.button_getD.Location = new System.Drawing.Point(8, 65);
            this.button_getD.Name = "button_getD";
            this.button_getD.Size = new System.Drawing.Size(88, 23);
            this.button_getD.TabIndex = 0;
            this.button_getD.Text = "Get debug log";
            this.button_getD.UseVisualStyleBackColor = true;
            this.button_getD.Click += new System.EventHandler(this.button_getD_Click);
            // 
            // button_getL
            // 
            this.button_getL.Enabled = false;
            this.button_getL.Location = new System.Drawing.Point(8, 36);
            this.button_getL.Name = "button_getL";
            this.button_getL.Size = new System.Drawing.Size(88, 23);
            this.button_getL.TabIndex = 0;
            this.button_getL.Text = "Get scan log";
            this.button_getL.UseVisualStyleBackColor = true;
            this.button_getL.Click += new System.EventHandler(this.button_getL_Click);
            // 
            // button_getS
            // 
            this.button_getS.Enabled = false;
            this.button_getS.Location = new System.Drawing.Point(8, 7);
            this.button_getS.Name = "button_getS";
            this.button_getS.Size = new System.Drawing.Size(88, 23);
            this.button_getS.TabIndex = 0;
            this.button_getS.Text = "Settings";
            this.button_getS.UseVisualStyleBackColor = true;
            this.button_getS.Click += new System.EventHandler(this.button_getS_Click);
            // 
            // textBox_terminal
            // 
            this.textBox_terminal.Location = new System.Drawing.Point(235, 22);
            this.textBox_terminal.MaxLength = 10000000;
            this.textBox_terminal.Multiline = true;
            this.textBox_terminal.Name = "textBox_terminal";
            this.textBox_terminal.ReadOnly = true;
            this.textBox_terminal.ScrollBars = System.Windows.Forms.ScrollBars.Vertical;
            this.textBox_terminal.Size = new System.Drawing.Size(517, 398);
            this.textBox_terminal.TabIndex = 1;
            // 
            // serialPort1
            // 
            this.serialPort1.BaudRate = 57600;
            this.serialPort1.ErrorReceived += new System.IO.Ports.SerialErrorReceivedEventHandler(this.serialPort1_ErrorReceived);
            this.serialPort1.DataReceived += new System.IO.Ports.SerialDataReceivedEventHandler(this.serialPort1_DataReceived);
            // 
            // saveFileDialog1
            // 
            this.saveFileDialog1.DefaultExt = "txt";
            this.saveFileDialog1.FileName = "LOGGER.txt";
            this.saveFileDialog1.Filter = "Text files|*.txt|All files|*.*";
            this.saveFileDialog1.Title = "Save log to file...";
            this.saveFileDialog1.FileOk += new System.ComponentModel.CancelEventHandler(this.saveFileDialog1_FileOk);
            // 
            // comboBox_portName
            // 
            this.comboBox_portName.FormattingEnabled = true;
            this.comboBox_portName.Location = new System.Drawing.Point(44, 215);
            this.comboBox_portName.Name = "comboBox_portName";
            this.comboBox_portName.Size = new System.Drawing.Size(117, 21);
            this.comboBox_portName.Sorted = true;
            this.comboBox_portName.TabIndex = 2;
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(2, 223);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(36, 13);
            this.label1.TabIndex = 3;
            this.label1.Text = "Port #";
            // 
            // button_openPort
            // 
            this.button_openPort.Location = new System.Drawing.Point(5, 242);
            this.button_openPort.Name = "button_openPort";
            this.button_openPort.Size = new System.Drawing.Size(75, 23);
            this.button_openPort.TabIndex = 5;
            this.button_openPort.Text = "Open port";
            this.button_openPort.UseVisualStyleBackColor = true;
            this.button_openPort.Click += new System.EventHandler(this.button_openPort_Click);
            // 
            // button_clear
            // 
            this.button_clear.Location = new System.Drawing.Point(167, 215);
            this.button_clear.Name = "button_clear";
            this.button_clear.Size = new System.Drawing.Size(61, 50);
            this.button_clear.TabIndex = 6;
            this.button_clear.Text = "Clear";
            this.button_clear.UseVisualStyleBackColor = true;
            this.button_clear.Click += new System.EventHandler(this.button_clear_Click);
            // 
            // button_closePort
            // 
            this.button_closePort.Enabled = false;
            this.button_closePort.Location = new System.Drawing.Point(86, 242);
            this.button_closePort.Name = "button_closePort";
            this.button_closePort.Size = new System.Drawing.Size(75, 23);
            this.button_closePort.TabIndex = 4;
            this.button_closePort.Text = "Close port";
            this.button_closePort.UseVisualStyleBackColor = true;
            this.button_closePort.Click += new System.EventHandler(this.button_closePort_Click);
            // 
            // button1
            // 
            this.button1.Location = new System.Drawing.Point(5, 272);
            this.button1.Name = "button1";
            this.button1.Size = new System.Drawing.Size(223, 23);
            this.button1.TabIndex = 7;
            this.button1.Text = "Save log to file...";
            this.button1.UseVisualStyleBackColor = true;
            this.button1.Click += new System.EventHandler(this.button1_Click);
            // 
            // Form1
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(764, 436);
            this.Controls.Add(this.button1);
            this.Controls.Add(this.button_clear);
            this.Controls.Add(this.button_openPort);
            this.Controls.Add(this.button_closePort);
            this.Controls.Add(this.label1);
            this.Controls.Add(this.comboBox_portName);
            this.Controls.Add(this.textBox_terminal);
            this.Controls.Add(this.tabControl);
            this.Name = "Form1";
            this.Text = "BC logger control";
            this.Load += new System.EventHandler(this.Form1_Load);
            this.tabControl.ResumeLayout(false);
            this.tabPage_set.ResumeLayout(false);
            this.tabPage_set.PerformLayout();
            this.tabPage_get.ResumeLayout(false);
            this.tabPage_get.PerformLayout();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.TabControl tabControl;
        private System.Windows.Forms.TabPage tabPage_set;
        private System.Windows.Forms.CheckBox checkBox_setL;
        private System.Windows.Forms.CheckBox checkBox_setN;
        private System.Windows.Forms.TextBox textBox_setT;
        private System.Windows.Forms.TextBox textBox_setP;
        private System.Windows.Forms.TextBox textBox_setC;
        private System.Windows.Forms.TextBox textBox_setI;
        private System.Windows.Forms.Button button_setT;
        private System.Windows.Forms.Button button_setP;
        private System.Windows.Forms.Button button_setC;
        private System.Windows.Forms.Button button_setI;
        private System.Windows.Forms.TabPage tabPage_get;
        private System.Windows.Forms.Button button_end;
        private System.Windows.Forms.Button button_delL;
        private System.Windows.Forms.Button button_getDn;
        private System.Windows.Forms.Button button_getLn;
        private System.Windows.Forms.Button button_getD;
        private System.Windows.Forms.Button button_getL;
        private System.Windows.Forms.Button button_getS;
        private System.Windows.Forms.TextBox textBox_terminal;
        private System.Windows.Forms.SaveFileDialog saveFileDialog1;
        private System.Windows.Forms.ComboBox comboBox_portName;
        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.Button button_openPort;
        public System.IO.Ports.SerialPort serialPort1;

        delegate void SetTextCallback1(string text);
        private void SetText1(string text)
        {
            // InvokeRequired required compares the thread ID of the
            // calling thread to the thread ID of the creating thread.
            // If these threads are different, it returns true.
            if (this.textBox_terminal.InvokeRequired)
            {
                SetTextCallback1 d = new SetTextCallback1(SetText1);
                this.BeginInvoke(d, new object[] { text });
            }
            else
            {
                this.textBox_terminal.Text += text;
            }
        }

        //string inStr = "";

        private System.Windows.Forms.Button button_sync;
        private System.Windows.Forms.TextBox textBox_getDn;
        private System.Windows.Forms.TextBox textBox_getLn;
        private System.Windows.Forms.Button button_getCurSet;
        private System.Windows.Forms.Button button_clear;
        private System.Windows.Forms.Button button_gettime;
        private System.Windows.Forms.Button button_closePort;
        private System.Windows.Forms.Button button1;

    }
}

