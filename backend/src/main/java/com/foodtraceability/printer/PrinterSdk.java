package com.foodtraceability.printer;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public interface PrinterSdk extends Library {
    
    String DLL_PATH = "P:\\my-new-project\\backend\\lib\\printer.sdk";
    
    PrinterSdk INSTANCE = Native.load(DLL_PATH, PrinterSdk.class);
    
    int PrinterCreator(PointerByReference handle, String model);
    
    int OpenPortA(Pointer handle, String setting);
    
    int ClosePort(Pointer handle);
    
    int ReleasePrinter(Pointer handle);
    
    int WriteData(Pointer handle, byte[] buffer, int size);
    
    int ReadData(Pointer handle, byte[] buffer, int size);
    
    int ListPrinters(byte[] buffer, int size, IntByReference needSize);
    
    int FindPrinters(String type, Pointer callback);
    
    int TSPL_Setup(Pointer handle, int printSpeed, int printDensity, 
                   int labelWidth, int labelHeight, int labelType, int gapHeight, int offset);
    
    int TSPL_ClearBuffer(Pointer handle);
    
    int TSPL_Text(Pointer handle, int x, int y, String fontName, String content,
                  int rotation, int xMultiplication, int yMultiplication, int alignment);
    
    int TSPL_Block(Pointer handle, int x, int y, int width, int height,
                   String fontName, String content, int rotation, 
                     int xMultiplication, int yMultiplication, int alignment);
    
    int TSPL_QrCode(Pointer handle, int x, int y, int width, int eccLevel,
                    int mode, int rotate, int model, int mask, String data);
    
    int TSPL_BarCode(Pointer handle, int x, int y, int type, String content,
                     int height, int showText, int rotation, int narrow, int wide);
    
    int TSPL_Print(Pointer handle, int num, int copies);
    
    int TSPL_Box(Pointer handle, int x, int y, int xEnd, int yEnd, int thickness, int radius);
    
    int TSPL_Bar(Pointer handle, int x, int y, int width, int height);
    
    int TSPL_Direction(Pointer handle, int direction, int mirror);
    
    int TSPL_Image(Pointer handle, int x, int y, int mode, String imgPath);
    
    int TSPL_BitMap(Pointer handle, int x, int y, int width, int height, int mode, byte[] data);
    
    int TSPL_GetPrinterStatus(Pointer handle, IntByReference status);
}
