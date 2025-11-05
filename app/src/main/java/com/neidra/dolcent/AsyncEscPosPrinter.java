package com.neidra.dolcent;


import com.dantsu.escposprinter.EscPosPrinterSize;
import com.dantsu.escposprinter.connection.DeviceConnection;

/**
 * Created by Ardian Iqbal Yusmartito on 10/06/24
 * Github : https://github.com/ALU-syntax
 * Twitter : https://twitter.com/mengkerebe
 * Instagram : https://www.instagram.com/ardian_iqbal_
 * LinkedIn : https://www.linkedin.com/in/ardianiqbal
 */
public class AsyncEscPosPrinter extends EscPosPrinterSize {
    private DeviceConnection printerConnection;
    private String[] textsToPrint = new String[0];

    public AsyncEscPosPrinter(DeviceConnection printerConnection, int printerDpi, float printerWidthMM, int printerNbrCharactersPerLine) {
        super(printerDpi, printerWidthMM, printerNbrCharactersPerLine);
        this.printerConnection = printerConnection;
    }

    public DeviceConnection getPrinterConnection() {
        return this.printerConnection;
    }

    public AsyncEscPosPrinter setTextsToPrint(String[] textsToPrint) {
        this.textsToPrint = textsToPrint;
        return this;
    }

    public AsyncEscPosPrinter addTextToPrint(String textToPrint) {
        String[] tmp = new String[this.textsToPrint.length + 1];
        System.arraycopy(this.textsToPrint, 0, tmp, 0, this.textsToPrint.length);
        tmp[this.textsToPrint.length] = textToPrint;
        this.textsToPrint = tmp;
        return this;
    }

    public String[] getTextsToPrint() {
        return this.textsToPrint;
    }
}
