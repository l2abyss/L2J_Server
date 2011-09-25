/*    */ package org.netcon;
/*    */ 
/*    */ public abstract class BaseReadPacket
/*    */   implements Runnable
/*    */ {
/*    */   private final byte[] _data;
/*    */   private int _off;
/*    */ 
/*    */   protected BaseReadPacket(byte[] data)
/*    */   {
/* 24 */     this._data = data;
/* 25 */     this._off = 2;
/*    */   }
/*    */ 
/*    */   protected final int readC()
/*    */   {
/* 31 */     int result = this._data[(this._off++)] & 0xFF;
/* 32 */     return result;
/*    */   }
/*    */ 
/*    */   protected final int readH()
/*    */   {
/* 38 */     int result = this._data[(this._off++)] & 0xFF;
/* 39 */     result |= this._data[(this._off++)] << 8 & 0xFF00;
/* 40 */     return result;
/*    */   }
/*    */ 
/*    */   protected final int readD()
/*    */   {
/* 46 */     int result = this._data[(this._off++)] & 0xFF;
/* 47 */     result |= this._data[(this._off++)] << 8 & 0xFF00;
/* 48 */     result |= this._data[(this._off++)] << 16 & 0xFF0000;
/* 49 */     result |= this._data[(this._off++)] << 24 & 0xFF000000;
/* 50 */     return result;
/*    */   }
/*    */ 
/*    */   protected final double readF()
/*    */   {
/* 56 */     long result = this._data[(this._off++)] & 0xFF;
/* 57 */     result |= this._data[(this._off++)] << 8 & 0xFF00;
/* 58 */     result |= this._data[(this._off++)] << 16 & 0xFF0000;
/* 59 */     result |= this._data[(this._off++)] << 24 & 0xFF000000;
/* 60 */     result |= this._data[(this._off++)] << 32 & 0x0;
/* 61 */     result |= this._data[(this._off++)] << 40 & 0x0;
/* 62 */     result |= this._data[(this._off++)] << 48 & 0x0;
/* 63 */     result |= this._data[(this._off++)] << 56 & 0x0;
/* 64 */     return Double.longBitsToDouble(result);
/*    */   }
/*    */ 
/*    */   protected final byte[] readB(int length)
/*    */   {
/* 70 */     byte[] result = new byte[length];
/* 71 */     for (int i = 0; i < length; ++i)
/*    */     {
/* 73 */       result[i] = this._data[(this._off + i)];
/*    */     }
/* 75 */     this._off += length;
/* 76 */     return result;
/*    */   }
/*    */ 
/*    */   protected final String readS()
/*    */   {
/* 82 */     String result = null;
/*    */     try
/*    */     {
/* 85 */       result = new String(this._data, this._off, this._data.length - this._off, "UTF-16LE");
/* 86 */       result = result.substring(0, result.indexOf(0));
/*    */     }
/*    */     catch (Exception e)
/*    */     {
/* 90 */       e.printStackTrace();
/*    */     }
/* 92 */     this._off += result.length() * 2 + 2;
/* 93 */     return result;
/*    */   }
/*    */ }

/* Location:           D:\_l2j\libs\netcon.jar
 * Qualified Name:     org.netcon.BaseReadPacket
 * JD-Core Version:    0.5.3
 */