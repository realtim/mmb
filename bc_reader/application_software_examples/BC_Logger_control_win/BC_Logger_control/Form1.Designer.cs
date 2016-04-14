using Microsoft.Win32;
using System;
using System.Collections;
using System.Collections.Generic;
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
            this.tabPage_Get = new System.Windows.Forms.TabPage();
            this.checkBox_delLog = new System.Windows.Forms.CheckBox();
            this.textBox_getDn = new System.Windows.Forms.TextBox();
            this.textBox_getLn = new System.Windows.Forms.TextBox();
            this.button_dlAll = new System.Windows.Forms.Button();
            this.button_delL = new System.Windows.Forms.Button();
            this.button_getDn = new System.Windows.Forms.Button();
            this.button_getLn = new System.Windows.Forms.Button();
            this.button_getD = new System.Windows.Forms.Button();
            this.button_getL = new System.Windows.Forms.Button();
            this.tabPage_Set = new System.Windows.Forms.TabPage();
            this.checkBox_setEnable = new System.Windows.Forms.CheckBox();
            this.checkBox_autoTime = new System.Windows.Forms.CheckBox();
            this.button_setN = new System.Windows.Forms.Button();
            this.button_setL = new System.Windows.Forms.Button();
            this.button_sendAll = new System.Windows.Forms.Button();
            this.button_gett = new System.Windows.Forms.Button();
            this.button_gettime = new System.Windows.Forms.Button();
            this.checkBox_setN = new System.Windows.Forms.CheckBox();
            this.checkBox_setL = new System.Windows.Forms.CheckBox();
            this.textBox_setT = new System.Windows.Forms.TextBox();
            this.textBox_setP = new System.Windows.Forms.TextBox();
            this.textBox_setC = new System.Windows.Forms.TextBox();
            this.textBox_setI = new System.Windows.Forms.TextBox();
            this.button_setT = new System.Windows.Forms.Button();
            this.button_setP = new System.Windows.Forms.Button();
            this.button_setC = new System.Windows.Forms.Button();
            this.button_setI = new System.Windows.Forms.Button();
            this.tabPage_Custom = new System.Windows.Forms.TabPage();
            this.checkBox_customEdit = new System.Windows.Forms.CheckBox();
            this.label12 = new System.Windows.Forms.Label();
            this.label11 = new System.Windows.Forms.Label();
            this.label10 = new System.Windows.Forms.Label();
            this.label9 = new System.Windows.Forms.Label();
            this.label8 = new System.Windows.Forms.Label();
            this.label7 = new System.Windows.Forms.Label();
            this.label6 = new System.Windows.Forms.Label();
            this.label5 = new System.Windows.Forms.Label();
            this.label4 = new System.Windows.Forms.Label();
            this.label3 = new System.Windows.Forms.Label();
            this.label14 = new System.Windows.Forms.Label();
            this.label13 = new System.Windows.Forms.Label();
            this.label2 = new System.Windows.Forms.Label();
            this.textBox_setTcustom = new System.Windows.Forms.TextBox();
            this.textBox_setPcustom = new System.Windows.Forms.TextBox();
            this.textBox_setCcustom = new System.Windows.Forms.TextBox();
            this.textBox_setIcustom = new System.Windows.Forms.TextBox();
            this.textBox_getTcustom = new System.Windows.Forms.TextBox();
            this.textBox_delLogcustom = new System.Windows.Forms.TextBox();
            this.textBox_getDncustom = new System.Windows.Forms.TextBox();
            this.textBox_getLncustom = new System.Windows.Forms.TextBox();
            this.textBox_getDcustom = new System.Windows.Forms.TextBox();
            this.textBox_getLcustom = new System.Windows.Forms.TextBox();
            this.textBox_setLcustom = new System.Windows.Forms.TextBox();
            this.textBox_setNcustom = new System.Windows.Forms.TextBox();
            this.textBox_getScustom = new System.Windows.Forms.TextBox();
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
            this.checkBox_autoScroll = new System.Windows.Forms.CheckBox();
            this.button_refresh = new System.Windows.Forms.Button();
            this.checkBox_portMon = new System.Windows.Forms.CheckBox();
            this.tabControl.SuspendLayout();
            this.tabPage_Get.SuspendLayout();
            this.tabPage_Set.SuspendLayout();
            this.tabPage_Custom.SuspendLayout();
            this.SuspendLayout();
            // 
            // tabControl
            // 
            this.tabControl.Controls.Add(this.tabPage_Get);
            this.tabControl.Controls.Add(this.tabPage_Set);
            this.tabControl.Controls.Add(this.tabPage_Custom);
            this.tabControl.Font = new System.Drawing.Font("Microsoft Sans Serif", 14F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.tabControl.Location = new System.Drawing.Point(2, 0);
            this.tabControl.Margin = new System.Windows.Forms.Padding(6);
            this.tabControl.Name = "tabControl";
            this.tabControl.SelectedIndex = 0;
            this.tabControl.Size = new System.Drawing.Size(440, 365);
            this.tabControl.TabIndex = 0;
            // 
            // tabPage_Get
            // 
            this.tabPage_Get.Controls.Add(this.checkBox_delLog);
            this.tabPage_Get.Controls.Add(this.textBox_getDn);
            this.tabPage_Get.Controls.Add(this.textBox_getLn);
            this.tabPage_Get.Controls.Add(this.button_dlAll);
            this.tabPage_Get.Controls.Add(this.button_delL);
            this.tabPage_Get.Controls.Add(this.button_getDn);
            this.tabPage_Get.Controls.Add(this.button_getLn);
            this.tabPage_Get.Controls.Add(this.button_getD);
            this.tabPage_Get.Controls.Add(this.button_getL);
            this.tabPage_Get.Location = new System.Drawing.Point(4, 33);
            this.tabPage_Get.Margin = new System.Windows.Forms.Padding(6);
            this.tabPage_Get.Name = "tabPage_Get";
            this.tabPage_Get.Padding = new System.Windows.Forms.Padding(6);
            this.tabPage_Get.Size = new System.Drawing.Size(432, 328);
            this.tabPage_Get.TabIndex = 1;
            this.tabPage_Get.Text = "Get data";
            this.tabPage_Get.UseVisualStyleBackColor = true;
            // 
            // checkBox_delLog
            // 
            this.checkBox_delLog.AutoSize = true;
            this.checkBox_delLog.Enabled = false;
            this.checkBox_delLog.Font = new System.Drawing.Font("Microsoft Sans Serif", 14F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.checkBox_delLog.Location = new System.Drawing.Point(227, 234);
            this.checkBox_delLog.Margin = new System.Windows.Forms.Padding(6);
            this.checkBox_delLog.Name = "checkBox_delLog";
            this.checkBox_delLog.RightToLeft = System.Windows.Forms.RightToLeft.Yes;
            this.checkBox_delLog.Size = new System.Drawing.Size(160, 28);
            this.checkBox_delLog.TabIndex = 3;
            this.checkBox_delLog.Text = "?Are you sure";
            this.checkBox_delLog.UseVisualStyleBackColor = true;
            this.checkBox_delLog.CheckedChanged += new System.EventHandler(this.checkBox_delLog_CheckedChanged);
            // 
            // textBox_getDn
            // 
            this.textBox_getDn.Location = new System.Drawing.Point(214, 179);
            this.textBox_getDn.Margin = new System.Windows.Forms.Padding(6);
            this.textBox_getDn.MaxLength = 6;
            this.textBox_getDn.Name = "textBox_getDn";
            this.textBox_getDn.Size = new System.Drawing.Size(204, 29);
            this.textBox_getDn.TabIndex = 2;
            this.textBox_getDn.Text = "0";
            // 
            // textBox_getLn
            // 
            this.textBox_getLn.Location = new System.Drawing.Point(214, 123);
            this.textBox_getLn.Margin = new System.Windows.Forms.Padding(6);
            this.textBox_getLn.MaxLength = 6;
            this.textBox_getLn.Name = "textBox_getLn";
            this.textBox_getLn.Size = new System.Drawing.Size(204, 29);
            this.textBox_getLn.TabIndex = 2;
            this.textBox_getLn.Text = "0";
            // 
            // button_dlAll
            // 
            this.button_dlAll.Enabled = false;
            this.button_dlAll.Location = new System.Drawing.Point(214, 12);
            this.button_dlAll.Margin = new System.Windows.Forms.Padding(6);
            this.button_dlAll.Name = "button_dlAll";
            this.button_dlAll.Size = new System.Drawing.Size(204, 95);
            this.button_dlAll.TabIndex = 1;
            this.button_dlAll.Text = "Get all data";
            this.button_dlAll.UseVisualStyleBackColor = true;
            this.button_dlAll.Click += new System.EventHandler(this.button_getAll_Click);
            // 
            // button_delL
            // 
            this.button_delL.Enabled = false;
            this.button_delL.Location = new System.Drawing.Point(6, 227);
            this.button_delL.Margin = new System.Windows.Forms.Padding(6);
            this.button_delL.Name = "button_delL";
            this.button_delL.Size = new System.Drawing.Size(196, 42);
            this.button_delL.TabIndex = 0;
            this.button_delL.Text = "Delete all logs";
            this.button_delL.UseVisualStyleBackColor = true;
            this.button_delL.Click += new System.EventHandler(this.button_delL_Click);
            // 
            // button_getDn
            // 
            this.button_getDn.Enabled = false;
            this.button_getDn.Location = new System.Drawing.Point(6, 173);
            this.button_getDn.Margin = new System.Windows.Forms.Padding(6);
            this.button_getDn.Name = "button_getDn";
            this.button_getDn.Size = new System.Drawing.Size(196, 42);
            this.button_getDn.TabIndex = 0;
            this.button_getDn.Text = "Get debug line";
            this.button_getDn.UseVisualStyleBackColor = true;
            this.button_getDn.Click += new System.EventHandler(this.button_getDn_Click);
            // 
            // button_getLn
            // 
            this.button_getLn.Enabled = false;
            this.button_getLn.Location = new System.Drawing.Point(6, 120);
            this.button_getLn.Margin = new System.Windows.Forms.Padding(6);
            this.button_getLn.Name = "button_getLn";
            this.button_getLn.Size = new System.Drawing.Size(196, 42);
            this.button_getLn.TabIndex = 0;
            this.button_getLn.Text = "Get scan log line#";
            this.button_getLn.UseVisualStyleBackColor = true;
            this.button_getLn.Click += new System.EventHandler(this.button_getLn_Click);
            // 
            // button_getD
            // 
            this.button_getD.Enabled = false;
            this.button_getD.Location = new System.Drawing.Point(6, 66);
            this.button_getD.Margin = new System.Windows.Forms.Padding(6);
            this.button_getD.Name = "button_getD";
            this.button_getD.Size = new System.Drawing.Size(196, 42);
            this.button_getD.TabIndex = 0;
            this.button_getD.Text = "Get debug log";
            this.button_getD.UseVisualStyleBackColor = true;
            this.button_getD.Click += new System.EventHandler(this.button_getD_Click);
            // 
            // button_getL
            // 
            this.button_getL.Enabled = false;
            this.button_getL.Location = new System.Drawing.Point(6, 12);
            this.button_getL.Margin = new System.Windows.Forms.Padding(6);
            this.button_getL.Name = "button_getL";
            this.button_getL.Size = new System.Drawing.Size(196, 42);
            this.button_getL.TabIndex = 0;
            this.button_getL.Text = "Get scan log";
            this.button_getL.UseVisualStyleBackColor = true;
            this.button_getL.Click += new System.EventHandler(this.button_getL_Click);
            // 
            // tabPage_Set
            // 
            this.tabPage_Set.Controls.Add(this.checkBox_setEnable);
            this.tabPage_Set.Controls.Add(this.checkBox_autoTime);
            this.tabPage_Set.Controls.Add(this.button_setN);
            this.tabPage_Set.Controls.Add(this.button_setL);
            this.tabPage_Set.Controls.Add(this.button_sendAll);
            this.tabPage_Set.Controls.Add(this.button_gett);
            this.tabPage_Set.Controls.Add(this.button_gettime);
            this.tabPage_Set.Controls.Add(this.checkBox_setN);
            this.tabPage_Set.Controls.Add(this.checkBox_setL);
            this.tabPage_Set.Controls.Add(this.textBox_setT);
            this.tabPage_Set.Controls.Add(this.textBox_setP);
            this.tabPage_Set.Controls.Add(this.textBox_setC);
            this.tabPage_Set.Controls.Add(this.textBox_setI);
            this.tabPage_Set.Controls.Add(this.button_setT);
            this.tabPage_Set.Controls.Add(this.button_setP);
            this.tabPage_Set.Controls.Add(this.button_setC);
            this.tabPage_Set.Controls.Add(this.button_setI);
            this.tabPage_Set.Location = new System.Drawing.Point(4, 33);
            this.tabPage_Set.Margin = new System.Windows.Forms.Padding(6);
            this.tabPage_Set.Name = "tabPage_Set";
            this.tabPage_Set.Padding = new System.Windows.Forms.Padding(6);
            this.tabPage_Set.Size = new System.Drawing.Size(432, 328);
            this.tabPage_Set.TabIndex = 0;
            this.tabPage_Set.Text = "Settings";
            this.tabPage_Set.UseVisualStyleBackColor = true;
            // 
            // checkBox_setEnable
            // 
            this.checkBox_setEnable.AutoSize = true;
            this.checkBox_setEnable.Enabled = false;
            this.checkBox_setEnable.Location = new System.Drawing.Point(266, 11);
            this.checkBox_setEnable.Name = "checkBox_setEnable";
            this.checkBox_setEnable.Size = new System.Drawing.Size(157, 28);
            this.checkBox_setEnable.TabIndex = 8;
            this.checkBox_setEnable.Text = "Enable settings";
            this.checkBox_setEnable.UseVisualStyleBackColor = true;
            this.checkBox_setEnable.CheckedChanged += new System.EventHandler(this.checkBox_setEnable_CheckedChanged);
            // 
            // checkBox_autoTime
            // 
            this.checkBox_autoTime.AutoSize = true;
            this.checkBox_autoTime.CheckAlign = System.Drawing.ContentAlignment.MiddleRight;
            this.checkBox_autoTime.Checked = true;
            this.checkBox_autoTime.CheckState = System.Windows.Forms.CheckState.Checked;
            this.checkBox_autoTime.Location = new System.Drawing.Point(140, 287);
            this.checkBox_autoTime.Margin = new System.Windows.Forms.Padding(6);
            this.checkBox_autoTime.Name = "checkBox_autoTime";
            this.checkBox_autoTime.Size = new System.Drawing.Size(70, 28);
            this.checkBox_autoTime.TabIndex = 7;
            this.checkBox_autoTime.Text = " auto";
            this.checkBox_autoTime.UseVisualStyleBackColor = true;
            this.checkBox_autoTime.CheckedChanged += new System.EventHandler(this.checkBox_autoTime_CheckedChanged);
            // 
            // button_setN
            // 
            this.button_setN.Enabled = false;
            this.button_setN.Location = new System.Drawing.Point(6, 172);
            this.button_setN.Margin = new System.Windows.Forms.Padding(6);
            this.button_setN.Name = "button_setN";
            this.button_setN.Size = new System.Drawing.Size(204, 42);
            this.button_setN.TabIndex = 6;
            this.button_setN.Text = "Enable numbers only";
            this.button_setN.UseVisualStyleBackColor = true;
            this.button_setN.Click += new System.EventHandler(this.button_setNumOnly_Click);
            // 
            // button_setL
            // 
            this.button_setL.Enabled = false;
            this.button_setL.Location = new System.Drawing.Point(6, 226);
            this.button_setL.Margin = new System.Windows.Forms.Padding(6);
            this.button_setL.Name = "button_setL";
            this.button_setL.Size = new System.Drawing.Size(204, 42);
            this.button_setL.TabIndex = 5;
            this.button_setL.Text = "Enable length check";
            this.button_setL.UseVisualStyleBackColor = true;
            this.button_setL.Click += new System.EventHandler(this.button_setLengthCheck_Click);
            // 
            // button_sendAll
            // 
            this.button_sendAll.Enabled = false;
            this.button_sendAll.Location = new System.Drawing.Point(269, 48);
            this.button_sendAll.Margin = new System.Windows.Forms.Padding(6);
            this.button_sendAll.Name = "button_sendAll";
            this.button_sendAll.Size = new System.Drawing.Size(149, 59);
            this.button_sendAll.TabIndex = 4;
            this.button_sendAll.Text = "Send all settings";
            this.button_sendAll.UseVisualStyleBackColor = true;
            this.button_sendAll.Click += new System.EventHandler(this.button_sendSet_Click);
            // 
            // button_gett
            // 
            this.button_gett.Enabled = false;
            this.button_gett.Location = new System.Drawing.Point(262, 241);
            this.button_gett.Margin = new System.Windows.Forms.Padding(6);
            this.button_gett.Name = "button_gett";
            this.button_gett.Size = new System.Drawing.Size(156, 32);
            this.button_gett.TabIndex = 3;
            this.button_gett.Text = "Get logger time";
            this.button_gett.UseVisualStyleBackColor = true;
            this.button_gett.Click += new System.EventHandler(this.button_gett_Click);
            // 
            // button_gettime
            // 
            this.button_gettime.Enabled = false;
            this.button_gettime.Location = new System.Drawing.Point(262, 197);
            this.button_gettime.Margin = new System.Windows.Forms.Padding(6);
            this.button_gettime.Name = "button_gettime";
            this.button_gettime.Size = new System.Drawing.Size(156, 32);
            this.button_gettime.TabIndex = 3;
            this.button_gettime.Text = "Get PC time";
            this.button_gettime.UseVisualStyleBackColor = true;
            this.button_gettime.Click += new System.EventHandler(this.button_gettime_Click);
            // 
            // checkBox_setN
            // 
            this.checkBox_setN.AutoSize = true;
            this.checkBox_setN.Location = new System.Drawing.Point(222, 187);
            this.checkBox_setN.Margin = new System.Windows.Forms.Padding(6);
            this.checkBox_setN.Name = "checkBox_setN";
            this.checkBox_setN.Size = new System.Drawing.Size(15, 14);
            this.checkBox_setN.TabIndex = 2;
            this.checkBox_setN.UseVisualStyleBackColor = true;
            // 
            // checkBox_setL
            // 
            this.checkBox_setL.AutoSize = true;
            this.checkBox_setL.Location = new System.Drawing.Point(222, 241);
            this.checkBox_setL.Margin = new System.Windows.Forms.Padding(6);
            this.checkBox_setL.Name = "checkBox_setL";
            this.checkBox_setL.Size = new System.Drawing.Size(15, 14);
            this.checkBox_setL.TabIndex = 2;
            this.checkBox_setL.UseVisualStyleBackColor = true;
            // 
            // textBox_setT
            // 
            this.textBox_setT.Enabled = false;
            this.textBox_setT.Location = new System.Drawing.Point(222, 285);
            this.textBox_setT.Margin = new System.Windows.Forms.Padding(6);
            this.textBox_setT.MaxLength = 20;
            this.textBox_setT.Name = "textBox_setT";
            this.textBox_setT.Size = new System.Drawing.Size(196, 29);
            this.textBox_setT.TabIndex = 1;
            this.textBox_setT.Text = "Mar 01 2015 12:00:00";
            // 
            // textBox_setP
            // 
            this.textBox_setP.Location = new System.Drawing.Point(222, 124);
            this.textBox_setP.Margin = new System.Windows.Forms.Padding(6);
            this.textBox_setP.MaxLength = 100;
            this.textBox_setP.Name = "textBox_setP";
            this.textBox_setP.Size = new System.Drawing.Size(196, 29);
            this.textBox_setP.TabIndex = 1;
            this.textBox_setP.Text = "********";
            // 
            // textBox_setC
            // 
            this.textBox_setC.Location = new System.Drawing.Point(222, 71);
            this.textBox_setC.Margin = new System.Windows.Forms.Padding(6);
            this.textBox_setC.MaxLength = 2;
            this.textBox_setC.Name = "textBox_setC";
            this.textBox_setC.Size = new System.Drawing.Size(35, 29);
            this.textBox_setC.TabIndex = 1;
            this.textBox_setC.Text = "00";
            // 
            // textBox_setI
            // 
            this.textBox_setI.Location = new System.Drawing.Point(222, 17);
            this.textBox_setI.Margin = new System.Windows.Forms.Padding(6);
            this.textBox_setI.MaxLength = 2;
            this.textBox_setI.Name = "textBox_setI";
            this.textBox_setI.Size = new System.Drawing.Size(35, 29);
            this.textBox_setI.TabIndex = 1;
            this.textBox_setI.Text = "00";
            // 
            // button_setT
            // 
            this.button_setT.Enabled = false;
            this.button_setT.Location = new System.Drawing.Point(6, 280);
            this.button_setT.Margin = new System.Windows.Forms.Padding(6);
            this.button_setT.Name = "button_setT";
            this.button_setT.Size = new System.Drawing.Size(122, 42);
            this.button_setT.TabIndex = 0;
            this.button_setT.Text = "Set time";
            this.button_setT.UseVisualStyleBackColor = true;
            this.button_setT.Click += new System.EventHandler(this.button_setT_Click);
            // 
            // button_setP
            // 
            this.button_setP.Enabled = false;
            this.button_setP.Location = new System.Drawing.Point(6, 118);
            this.button_setP.Margin = new System.Windows.Forms.Padding(6);
            this.button_setP.Name = "button_setP";
            this.button_setP.Size = new System.Drawing.Size(204, 42);
            this.button_setP.TabIndex = 0;
            this.button_setP.Text = "Set pattern";
            this.button_setP.UseVisualStyleBackColor = true;
            this.button_setP.Click += new System.EventHandler(this.button_setP_Click);
            // 
            // button_setC
            // 
            this.button_setC.Enabled = false;
            this.button_setC.Location = new System.Drawing.Point(6, 65);
            this.button_setC.Margin = new System.Windows.Forms.Padding(6);
            this.button_setC.Name = "button_setC";
            this.button_setC.Size = new System.Drawing.Size(204, 42);
            this.button_setC.TabIndex = 0;
            this.button_setC.Text = "Set control point";
            this.button_setC.UseVisualStyleBackColor = true;
            this.button_setC.Click += new System.EventHandler(this.button_setC_Click);
            // 
            // button_setI
            // 
            this.button_setI.Enabled = false;
            this.button_setI.Location = new System.Drawing.Point(6, 11);
            this.button_setI.Margin = new System.Windows.Forms.Padding(6);
            this.button_setI.Name = "button_setI";
            this.button_setI.Size = new System.Drawing.Size(204, 42);
            this.button_setI.TabIndex = 0;
            this.button_setI.Text = "Set scanner ID";
            this.button_setI.UseVisualStyleBackColor = true;
            this.button_setI.Click += new System.EventHandler(this.button_setI_Click);
            // 
            // tabPage_Custom
            // 
            this.tabPage_Custom.AutoScroll = true;
            this.tabPage_Custom.Controls.Add(this.checkBox_customEdit);
            this.tabPage_Custom.Controls.Add(this.label12);
            this.tabPage_Custom.Controls.Add(this.label11);
            this.tabPage_Custom.Controls.Add(this.label10);
            this.tabPage_Custom.Controls.Add(this.label9);
            this.tabPage_Custom.Controls.Add(this.label8);
            this.tabPage_Custom.Controls.Add(this.label7);
            this.tabPage_Custom.Controls.Add(this.label6);
            this.tabPage_Custom.Controls.Add(this.label5);
            this.tabPage_Custom.Controls.Add(this.label4);
            this.tabPage_Custom.Controls.Add(this.label3);
            this.tabPage_Custom.Controls.Add(this.label14);
            this.tabPage_Custom.Controls.Add(this.label13);
            this.tabPage_Custom.Controls.Add(this.label2);
            this.tabPage_Custom.Controls.Add(this.textBox_setTcustom);
            this.tabPage_Custom.Controls.Add(this.textBox_setPcustom);
            this.tabPage_Custom.Controls.Add(this.textBox_setCcustom);
            this.tabPage_Custom.Controls.Add(this.textBox_setIcustom);
            this.tabPage_Custom.Controls.Add(this.textBox_getTcustom);
            this.tabPage_Custom.Controls.Add(this.textBox_delLogcustom);
            this.tabPage_Custom.Controls.Add(this.textBox_getDncustom);
            this.tabPage_Custom.Controls.Add(this.textBox_getLncustom);
            this.tabPage_Custom.Controls.Add(this.textBox_getDcustom);
            this.tabPage_Custom.Controls.Add(this.textBox_getLcustom);
            this.tabPage_Custom.Controls.Add(this.textBox_setLcustom);
            this.tabPage_Custom.Controls.Add(this.textBox_setNcustom);
            this.tabPage_Custom.Controls.Add(this.textBox_getScustom);
            this.tabPage_Custom.Location = new System.Drawing.Point(4, 33);
            this.tabPage_Custom.Name = "tabPage_Custom";
            this.tabPage_Custom.Size = new System.Drawing.Size(432, 328);
            this.tabPage_Custom.TabIndex = 2;
            this.tabPage_Custom.Text = "Custom";
            this.tabPage_Custom.UseVisualStyleBackColor = true;
            // 
            // checkBox_customEdit
            // 
            this.checkBox_customEdit.AutoSize = true;
            this.checkBox_customEdit.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.checkBox_customEdit.Location = new System.Drawing.Point(350, 4);
            this.checkBox_customEdit.Name = "checkBox_customEdit";
            this.checkBox_customEdit.Size = new System.Drawing.Size(79, 17);
            this.checkBox_customEdit.TabIndex = 4;
            this.checkBox_customEdit.Text = "Edit enable";
            this.checkBox_customEdit.UseVisualStyleBackColor = true;
            this.checkBox_customEdit.CheckedChanged += new System.EventHandler(this.checkBoxcustomEdit_CheckedChanged);
            // 
            // label12
            // 
            this.label12.AutoSize = true;
            this.label12.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.label12.Location = new System.Drawing.Point(112, 246);
            this.label12.Name = "label12";
            this.label12.Size = new System.Drawing.Size(45, 13);
            this.label12.TabIndex = 3;
            this.label12.Text = "Set time";
            // 
            // label11
            // 
            this.label11.AutoSize = true;
            this.label11.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.label11.Location = new System.Drawing.Point(112, 222);
            this.label11.Name = "label11";
            this.label11.Size = new System.Drawing.Size(59, 13);
            this.label11.TabIndex = 3;
            this.label11.Text = "Set pattern";
            // 
            // label10
            // 
            this.label10.AutoSize = true;
            this.label10.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.label10.Location = new System.Drawing.Point(112, 198);
            this.label10.Name = "label10";
            this.label10.Size = new System.Drawing.Size(92, 13);
            this.label10.TabIndex = 3;
            this.label10.Text = "Set control point #";
            // 
            // label9
            // 
            this.label9.AutoSize = true;
            this.label9.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.label9.Location = new System.Drawing.Point(112, 174);
            this.label9.Name = "label9";
            this.label9.Size = new System.Drawing.Size(77, 13);
            this.label9.TabIndex = 3;
            this.label9.Text = "Set scanner ID";
            // 
            // label8
            // 
            this.label8.AutoSize = true;
            this.label8.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.label8.Location = new System.Drawing.Point(112, 150);
            this.label8.Name = "label8";
            this.label8.Size = new System.Drawing.Size(78, 13);
            this.label8.TabIndex = 3;
            this.label8.Text = "Get logger time";
            // 
            // label7
            // 
            this.label7.AutoSize = true;
            this.label7.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.label7.Location = new System.Drawing.Point(112, 126);
            this.label7.Name = "label7";
            this.label7.Size = new System.Drawing.Size(73, 13);
            this.label7.TabIndex = 3;
            this.label7.Text = "Delete all logs";
            // 
            // label6
            // 
            this.label6.AutoSize = true;
            this.label6.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.label6.Location = new System.Drawing.Point(112, 102);
            this.label6.Name = "label6";
            this.label6.Size = new System.Drawing.Size(99, 13);
            this.label6.TabIndex = 3;
            this.label6.Text = "Get debug log line#";
            // 
            // label5
            // 
            this.label5.AutoSize = true;
            this.label5.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.label5.Location = new System.Drawing.Point(112, 78);
            this.label5.Name = "label5";
            this.label5.Size = new System.Drawing.Size(91, 13);
            this.label5.TabIndex = 3;
            this.label5.Text = "Get scan log line#";
            // 
            // label4
            // 
            this.label4.AutoSize = true;
            this.label4.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.label4.Location = new System.Drawing.Point(112, 54);
            this.label4.Name = "label4";
            this.label4.Size = new System.Drawing.Size(74, 13);
            this.label4.TabIndex = 3;
            this.label4.Text = "Get debug log";
            // 
            // label3
            // 
            this.label3.AutoSize = true;
            this.label3.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.label3.Location = new System.Drawing.Point(112, 30);
            this.label3.Name = "label3";
            this.label3.Size = new System.Drawing.Size(66, 13);
            this.label3.TabIndex = 3;
            this.label3.Text = "Get scan log";
            // 
            // label14
            // 
            this.label14.AutoSize = true;
            this.label14.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.label14.Location = new System.Drawing.Point(112, 294);
            this.label14.Name = "label14";
            this.label14.Size = new System.Drawing.Size(88, 13);
            this.label14.TabIndex = 3;
            this.label14.Text = "Set numbers only";
            // 
            // label13
            // 
            this.label13.AutoSize = true;
            this.label13.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.label13.Location = new System.Drawing.Point(112, 270);
            this.label13.Name = "label13";
            this.label13.Size = new System.Drawing.Size(85, 13);
            this.label13.TabIndex = 3;
            this.label13.Text = "Set length check";
            // 
            // label2
            // 
            this.label2.AutoSize = true;
            this.label2.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.label2.Location = new System.Drawing.Point(112, 6);
            this.label2.Name = "label2";
            this.label2.Size = new System.Drawing.Size(63, 13);
            this.label2.TabIndex = 3;
            this.label2.Text = "Get settings";
            // 
            // textBox_setTcustom
            // 
            this.textBox_setTcustom.Enabled = false;
            this.textBox_setTcustom.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.textBox_setTcustom.Location = new System.Drawing.Point(6, 243);
            this.textBox_setTcustom.Name = "textBox_setTcustom";
            this.textBox_setTcustom.Size = new System.Drawing.Size(100, 18);
            this.textBox_setTcustom.TabIndex = 2;
            this.textBox_setTcustom.Text = "SETT";
            // 
            // textBox_setPcustom
            // 
            this.textBox_setPcustom.Enabled = false;
            this.textBox_setPcustom.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.textBox_setPcustom.Location = new System.Drawing.Point(6, 219);
            this.textBox_setPcustom.Name = "textBox_setPcustom";
            this.textBox_setPcustom.Size = new System.Drawing.Size(100, 18);
            this.textBox_setPcustom.TabIndex = 2;
            this.textBox_setPcustom.Text = "SETP";
            // 
            // textBox_setCcustom
            // 
            this.textBox_setCcustom.Enabled = false;
            this.textBox_setCcustom.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.textBox_setCcustom.Location = new System.Drawing.Point(6, 195);
            this.textBox_setCcustom.Name = "textBox_setCcustom";
            this.textBox_setCcustom.Size = new System.Drawing.Size(100, 18);
            this.textBox_setCcustom.TabIndex = 2;
            this.textBox_setCcustom.Text = "SETC";
            // 
            // textBox_setIcustom
            // 
            this.textBox_setIcustom.Enabled = false;
            this.textBox_setIcustom.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.textBox_setIcustom.Location = new System.Drawing.Point(6, 171);
            this.textBox_setIcustom.Name = "textBox_setIcustom";
            this.textBox_setIcustom.Size = new System.Drawing.Size(100, 18);
            this.textBox_setIcustom.TabIndex = 2;
            this.textBox_setIcustom.Text = "SETI";
            // 
            // textBox_getTcustom
            // 
            this.textBox_getTcustom.Enabled = false;
            this.textBox_getTcustom.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.textBox_getTcustom.Location = new System.Drawing.Point(6, 147);
            this.textBox_getTcustom.Name = "textBox_getTcustom";
            this.textBox_getTcustom.Size = new System.Drawing.Size(100, 18);
            this.textBox_getTcustom.TabIndex = 2;
            this.textBox_getTcustom.Text = "GETT";
            // 
            // textBox_delLogcustom
            // 
            this.textBox_delLogcustom.Enabled = false;
            this.textBox_delLogcustom.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.textBox_delLogcustom.Location = new System.Drawing.Point(6, 123);
            this.textBox_delLogcustom.Name = "textBox_delLogcustom";
            this.textBox_delLogcustom.Size = new System.Drawing.Size(100, 18);
            this.textBox_delLogcustom.TabIndex = 2;
            this.textBox_delLogcustom.Text = "DELLOG";
            // 
            // textBox_getDncustom
            // 
            this.textBox_getDncustom.Enabled = false;
            this.textBox_getDncustom.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.textBox_getDncustom.Location = new System.Drawing.Point(6, 99);
            this.textBox_getDncustom.Name = "textBox_getDncustom";
            this.textBox_getDncustom.Size = new System.Drawing.Size(100, 18);
            this.textBox_getDncustom.TabIndex = 2;
            this.textBox_getDncustom.Text = "GET#D";
            // 
            // textBox_getLncustom
            // 
            this.textBox_getLncustom.Enabled = false;
            this.textBox_getLncustom.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.textBox_getLncustom.Location = new System.Drawing.Point(6, 75);
            this.textBox_getLncustom.Name = "textBox_getLncustom";
            this.textBox_getLncustom.Size = new System.Drawing.Size(100, 18);
            this.textBox_getLncustom.TabIndex = 2;
            this.textBox_getLncustom.Text = "GET#L";
            // 
            // textBox_getDcustom
            // 
            this.textBox_getDcustom.Enabled = false;
            this.textBox_getDcustom.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.textBox_getDcustom.Location = new System.Drawing.Point(6, 51);
            this.textBox_getDcustom.Name = "textBox_getDcustom";
            this.textBox_getDcustom.Size = new System.Drawing.Size(100, 18);
            this.textBox_getDcustom.TabIndex = 2;
            this.textBox_getDcustom.Text = "GETD";
            // 
            // textBox_getLcustom
            // 
            this.textBox_getLcustom.Enabled = false;
            this.textBox_getLcustom.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.textBox_getLcustom.Location = new System.Drawing.Point(6, 27);
            this.textBox_getLcustom.Name = "textBox_getLcustom";
            this.textBox_getLcustom.Size = new System.Drawing.Size(100, 18);
            this.textBox_getLcustom.TabIndex = 2;
            this.textBox_getLcustom.Text = "GETL";
            // 
            // textBox_setLcustom
            // 
            this.textBox_setLcustom.Enabled = false;
            this.textBox_setLcustom.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.textBox_setLcustom.Location = new System.Drawing.Point(6, 291);
            this.textBox_setLcustom.Name = "textBox_setLcustom";
            this.textBox_setLcustom.Size = new System.Drawing.Size(100, 18);
            this.textBox_setLcustom.TabIndex = 2;
            this.textBox_setLcustom.Text = "SETL";
            // 
            // textBox_setNcustom
            // 
            this.textBox_setNcustom.Enabled = false;
            this.textBox_setNcustom.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.textBox_setNcustom.Location = new System.Drawing.Point(6, 267);
            this.textBox_setNcustom.Name = "textBox_setNcustom";
            this.textBox_setNcustom.Size = new System.Drawing.Size(100, 18);
            this.textBox_setNcustom.TabIndex = 2;
            this.textBox_setNcustom.Text = "SETN";
            // 
            // textBox_getScustom
            // 
            this.textBox_getScustom.Enabled = false;
            this.textBox_getScustom.Font = new System.Drawing.Font("Microsoft Sans Serif", 7F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.textBox_getScustom.Location = new System.Drawing.Point(6, 3);
            this.textBox_getScustom.Name = "textBox_getScustom";
            this.textBox_getScustom.Size = new System.Drawing.Size(100, 18);
            this.textBox_getScustom.TabIndex = 2;
            this.textBox_getScustom.Text = "GETS";
            // 
            // button_getS
            // 
            this.button_getS.Enabled = false;
            this.button_getS.Location = new System.Drawing.Point(12, 369);
            this.button_getS.Margin = new System.Windows.Forms.Padding(6);
            this.button_getS.Name = "button_getS";
            this.button_getS.Size = new System.Drawing.Size(204, 42);
            this.button_getS.TabIndex = 0;
            this.button_getS.Text = "Get settings";
            this.button_getS.UseVisualStyleBackColor = true;
            this.button_getS.Click += new System.EventHandler(this.button_getS_Click);
            // 
            // textBox_terminal
            // 
            this.textBox_terminal.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
            | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
            this.textBox_terminal.Location = new System.Drawing.Point(450, 34);
            this.textBox_terminal.Margin = new System.Windows.Forms.Padding(6);
            this.textBox_terminal.Multiline = true;
            this.textBox_terminal.Name = "textBox_terminal";
            this.textBox_terminal.ReadOnly = true;
            this.textBox_terminal.ScrollBars = System.Windows.Forms.ScrollBars.Vertical;
            this.textBox_terminal.Size = new System.Drawing.Size(277, 472);
            this.textBox_terminal.TabIndex = 1;
            this.textBox_terminal.TextChanged += new System.EventHandler(this.textBox_terminal_TextChanged);
            // 
            // serialPort1
            // 
            this.serialPort1.BaudRate = 57600;
            this.serialPort1.ErrorReceived += new System.IO.Ports.SerialErrorReceivedEventHandler(this.serialPort1_ErrorReceived);
            this.serialPort1.DataReceived += new System.IO.Ports.SerialDataReceivedEventHandler(this.serialPort1_DataReceived);
            // 
            // saveFileDialog1
            // 
            this.saveFileDialog1.Title = "Save log to file...";
            this.saveFileDialog1.FileOk += new System.ComponentModel.CancelEventHandler(this.saveFileDialog1_FileOk);
            // 
            // comboBox_portName
            // 
            this.comboBox_portName.DropDownStyle = System.Windows.Forms.ComboBoxStyle.DropDownList;
            this.comboBox_portName.FormattingEnabled = true;
            this.comboBox_portName.Location = new System.Drawing.Point(78, 420);
            this.comboBox_portName.Margin = new System.Windows.Forms.Padding(6);
            this.comboBox_portName.Name = "comboBox_portName";
            this.comboBox_portName.Size = new System.Drawing.Size(360, 32);
            this.comboBox_portName.TabIndex = 2;
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(8, 423);
            this.label1.Margin = new System.Windows.Forms.Padding(6, 0, 6, 0);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(58, 24);
            this.label1.TabIndex = 3;
            this.label1.Text = "Port #";
            // 
            // button_openPort
            // 
            this.button_openPort.Location = new System.Drawing.Point(12, 464);
            this.button_openPort.Margin = new System.Windows.Forms.Padding(6);
            this.button_openPort.Name = "button_openPort";
            this.button_openPort.Size = new System.Drawing.Size(133, 42);
            this.button_openPort.TabIndex = 5;
            this.button_openPort.Text = "Open port";
            this.button_openPort.UseVisualStyleBackColor = true;
            this.button_openPort.Click += new System.EventHandler(this.button_openPort_Click);
            // 
            // button_clear
            // 
            this.button_clear.Location = new System.Drawing.Point(228, 369);
            this.button_clear.Margin = new System.Windows.Forms.Padding(6);
            this.button_clear.Name = "button_clear";
            this.button_clear.Size = new System.Drawing.Size(100, 42);
            this.button_clear.TabIndex = 6;
            this.button_clear.Text = "Clear log";
            this.button_clear.UseVisualStyleBackColor = true;
            this.button_clear.Click += new System.EventHandler(this.button_clear_Click);
            // 
            // button_closePort
            // 
            this.button_closePort.Enabled = false;
            this.button_closePort.Location = new System.Drawing.Point(156, 464);
            this.button_closePort.Margin = new System.Windows.Forms.Padding(6);
            this.button_closePort.Name = "button_closePort";
            this.button_closePort.Size = new System.Drawing.Size(133, 42);
            this.button_closePort.TabIndex = 4;
            this.button_closePort.Text = "Close port";
            this.button_closePort.UseVisualStyleBackColor = true;
            this.button_closePort.Click += new System.EventHandler(this.button_closePort_Click);
            // 
            // button1
            // 
            this.button1.Location = new System.Drawing.Point(340, 369);
            this.button1.Margin = new System.Windows.Forms.Padding(6);
            this.button1.Name = "button1";
            this.button1.Size = new System.Drawing.Size(98, 42);
            this.button1.TabIndex = 7;
            this.button1.Text = "Save log";
            this.button1.UseVisualStyleBackColor = true;
            this.button1.Click += new System.EventHandler(this.button_saveFile_Click);
            // 
            // checkBox_autoScroll
            // 
            this.checkBox_autoScroll.AutoSize = true;
            this.checkBox_autoScroll.Checked = true;
            this.checkBox_autoScroll.CheckState = System.Windows.Forms.CheckState.Checked;
            this.checkBox_autoScroll.Location = new System.Drawing.Point(454, 0);
            this.checkBox_autoScroll.Margin = new System.Windows.Forms.Padding(6);
            this.checkBox_autoScroll.Name = "checkBox_autoScroll";
            this.checkBox_autoScroll.Size = new System.Drawing.Size(112, 28);
            this.checkBox_autoScroll.TabIndex = 8;
            this.checkBox_autoScroll.Text = "Autoscroll";
            this.checkBox_autoScroll.UseVisualStyleBackColor = true;
            // 
            // button_refresh
            // 
            this.button_refresh.Location = new System.Drawing.Point(305, 464);
            this.button_refresh.Name = "button_refresh";
            this.button_refresh.Size = new System.Drawing.Size(133, 42);
            this.button_refresh.TabIndex = 10;
            this.button_refresh.Text = "Refresh";
            this.button_refresh.UseVisualStyleBackColor = true;
            this.button_refresh.Click += new System.EventHandler(this.button_refresh_Click);
            // 
            // checkBox_portMon
            // 
            this.checkBox_portMon.AutoSize = true;
            this.checkBox_portMon.Checked = true;
            this.checkBox_portMon.CheckState = System.Windows.Forms.CheckState.Checked;
            this.checkBox_portMon.Location = new System.Drawing.Point(578, 0);
            this.checkBox_portMon.Margin = new System.Windows.Forms.Padding(6);
            this.checkBox_portMon.Name = "checkBox_portMon";
            this.checkBox_portMon.Size = new System.Drawing.Size(156, 28);
            this.checkBox_portMon.TabIndex = 9;
            this.checkBox_portMon.Text = "Port monitoring";
            this.checkBox_portMon.UseVisualStyleBackColor = true;
            // 
            // Form1
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(11F, 24F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(742, 523);
            this.Controls.Add(this.button_refresh);
            this.Controls.Add(this.checkBox_portMon);
            this.Controls.Add(this.checkBox_autoScroll);
            this.Controls.Add(this.button1);
            this.Controls.Add(this.button_clear);
            this.Controls.Add(this.button_openPort);
            this.Controls.Add(this.button_closePort);
            this.Controls.Add(this.label1);
            this.Controls.Add(this.button_getS);
            this.Controls.Add(this.comboBox_portName);
            this.Controls.Add(this.textBox_terminal);
            this.Controls.Add(this.tabControl);
            this.Font = new System.Drawing.Font("Microsoft Sans Serif", 14F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(204)));
            this.Margin = new System.Windows.Forms.Padding(6);
            this.MinimumSize = new System.Drawing.Size(750, 550);
            this.Name = "Form1";
            this.Text = "BC logger control";
            this.Load += new System.EventHandler(this.Form1_Load);
            this.tabControl.ResumeLayout(false);
            this.tabPage_Get.ResumeLayout(false);
            this.tabPage_Get.PerformLayout();
            this.tabPage_Set.ResumeLayout(false);
            this.tabPage_Set.PerformLayout();
            this.tabPage_Custom.ResumeLayout(false);
            this.tabPage_Custom.PerformLayout();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.TabControl tabControl;
        private System.Windows.Forms.TabPage tabPage_Set;
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
        private System.Windows.Forms.TabPage tabPage_Get;
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
        private System.IO.Ports.SerialPort serialPort1;
        private System.Windows.Forms.Button button_dlAll;
        private System.Windows.Forms.TextBox textBox_getDn;
        private System.Windows.Forms.TextBox textBox_getLn;
        private System.Windows.Forms.Button button_clear;
        private System.Windows.Forms.Button button_gettime;
        private System.Windows.Forms.Button button_closePort;
        private System.Windows.Forms.Button button1;
        private System.Windows.Forms.Button button_sendAll;
        private System.Windows.Forms.Button button_setN;
        private System.Windows.Forms.Button button_setL;
        private System.Windows.Forms.CheckBox checkBox_delLog;
        private System.Windows.Forms.CheckBox checkBox_autoTime;
        private System.Windows.Forms.CheckBox checkBox_autoScroll;
        private System.Windows.Forms.TabPage tabPage_Custom;
        private System.Windows.Forms.Label label8;
        private System.Windows.Forms.Label label7;
        private System.Windows.Forms.Label label6;
        private System.Windows.Forms.Label label5;
        private System.Windows.Forms.Label label4;
        private System.Windows.Forms.Label label3;
        private System.Windows.Forms.Label label2;
        private System.Windows.Forms.TextBox textBox_getTcustom;
        private System.Windows.Forms.TextBox textBox_delLogcustom;
        private System.Windows.Forms.TextBox textBox_getDncustom;
        private System.Windows.Forms.TextBox textBox_getLncustom;
        private System.Windows.Forms.TextBox textBox_getDcustom;
        private System.Windows.Forms.TextBox textBox_getLcustom;
        private System.Windows.Forms.TextBox textBox_getScustom;
        private System.Windows.Forms.Button button_refresh;
        private System.Windows.Forms.CheckBox checkBox_customEdit;
        private System.Windows.Forms.Label label12;
        private System.Windows.Forms.Label label11;
        private System.Windows.Forms.Label label10;
        private System.Windows.Forms.Label label9;
        private System.Windows.Forms.Label label14;
        private System.Windows.Forms.Label label13;
        private System.Windows.Forms.TextBox textBox_setTcustom;
        private System.Windows.Forms.TextBox textBox_setPcustom;
        private System.Windows.Forms.TextBox textBox_setCcustom;
        private System.Windows.Forms.TextBox textBox_setIcustom;
        private System.Windows.Forms.TextBox textBox_setLcustom;
        private System.Windows.Forms.TextBox textBox_setNcustom;
        private System.Windows.Forms.CheckBox checkBox_setEnable;


        const int inputCodePage = 866;

        delegate void SetTextCallback1(string text);
        private void SetText(string text)
        {
            // InvokeRequired required compares the thread ID of the
            // calling thread to the thread ID of the creating thread.
            // If these threads are different, it returns true.
            if (this.textBox_terminal.InvokeRequired)
            {
                SetTextCallback1 d = new SetTextCallback1(SetText);
                this.BeginInvoke(d, new object[] { text });
            }
            else
            {
                this.textBox_terminal.Text += text;
            }
        }

        private string getDateString()
        {
            string dateString = "";
            switch (DateTime.Today.Month.ToString("D2"))
            {
                case "01":
                    dateString = "Jan";
                    break;
                case "02":
                    dateString = "Feb";
                    break;
                case "03":
                    dateString = "Mar";
                    break;
                case "04":
                    dateString = "Apr";
                    break;
                case "05":
                    dateString = "May";
                    break;
                case "06":
                    dateString = "Jun";
                    break;
                case "07":
                    dateString = "Jul";
                    break;
                case "08":
                    dateString = "Aug";
                    break;
                case "09":
                    dateString = "Sep";
                    break;
                case "10":
                    dateString = "Oct";
                    break;
                case "11":
                    dateString = "Nov";
                    break;
                case "12":
                    dateString = "Dec";
                    break;
            }
            dateString += " " + DateTime.Today.Day.ToString("D2");
            dateString += " " + DateTime.Today.Year.ToString("D4");
            dateString += " " + DateTime.Now.Hour.ToString("D2");
            dateString += ":" + DateTime.Now.Minute.ToString("D2");
            dateString += ":" + DateTime.Now.Second.ToString("D2");
            return dateString;
        }

        Hashtable BuildPortNameHash(string[] oPortsToMap)
        {
            Hashtable oReturnTable = new Hashtable();
            MineRegistryForPortName("SYSTEM\\CurrentControlSet\\Enum", oReturnTable, oPortsToMap);
            return oReturnTable;
        }

        void MineRegistryForPortName(string strStartKey, Hashtable oTargetMap, string[] oPortNamesToMatch)
        {
            if (oTargetMap.Count >= oPortNamesToMatch.Length)
                return;
            RegistryKey oCurrentKey = Registry.LocalMachine;

            try
            {
                oCurrentKey = oCurrentKey.OpenSubKey(strStartKey);

                string[] oSubKeyNames = oCurrentKey.GetSubKeyNames();
                if (((IList<string>)oSubKeyNames).Contains("Device Parameters") && strStartKey != "SYSTEM\\CurrentControlSet\\Enum")
                {
                    object oPortNameValue = Registry.GetValue("HKEY_LOCAL_MACHINE\\" + strStartKey + "\\Device Parameters", "PortName", null);

                    if (oPortNameValue == null || ((IList<string>)oPortNamesToMatch).Contains(oPortNameValue.ToString()) == false)
                        return;
                    object oFriendlyName = Registry.GetValue("HKEY_LOCAL_MACHINE\\" + strStartKey, "FriendlyName", null);

                    string strFriendlyName = "N/A";

                    if (oFriendlyName != null)
                        strFriendlyName = oFriendlyName.ToString();
                    if (strFriendlyName.Contains(oPortNameValue.ToString()) == false)
                        strFriendlyName = string.Format("{0} ({1})", strFriendlyName, oPortNameValue);
                    oTargetMap[strFriendlyName] = oPortNameValue;
                }
                else
                {
                    foreach (string strSubKey in oSubKeyNames)
                        MineRegistryForPortName(strStartKey + "\\" + strSubKey, oTargetMap, oPortNamesToMatch);
                }
            }
            catch
            {

            }
        }

        private System.Windows.Forms.Button button_gett;
        private System.Windows.Forms.CheckBox checkBox_portMon;

        byte crcCalc(String inString)
        {
            char[] instr=inString.ToCharArray(0, inString.Length);
            byte crc = 0x00;
            int i = 0;
            while (i < inString.Length)
            {
                for (byte tempI = 8; tempI>0; tempI--)
                {
                    byte sum = (byte) ((crc & 0xFF) ^ (instr[i] & 0xFF));
                    sum = (byte) ((sum & 0xFF) & 0x01);

                    crc >>= 1;
                    if (sum!=0)
                    {
                        crc ^= 0x8C;
                    }
                    instr[i] >>= 1;
                }
                i++;
            }
            return (crc);
        }


    }
}

