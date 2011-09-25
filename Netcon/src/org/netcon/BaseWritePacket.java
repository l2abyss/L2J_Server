/*     */ package org.netcon;
/*     */ 
/*     */ import java.io.ByteArrayOutputStream;
/*     */ import java.io.IOException;
/*     */ 
/*     */ public abstract class BaseWritePacket
/*     */ {
/*     */   private final ByteArrayOutputStream _bao;
/*     */ 
/*     */   protected BaseWritePacket()
/*     */   {
/*  26 */     this._bao = new ByteArrayOutputStream();
/*     */   }
/*     */ 
/*     */   protected final void writeC(int value)
/*     */   {
/*  31 */     this._bao.write(value & 0xFF);
/*     */   }
/*     */ 
/*     */   protected final void writeH(int value)
/*     */   {
/*  36 */     this._bao.write(value & 0xFF);
/*  37 */     this._bao.write(value >> 8 & 0xFF);
/*     */   }
/*     */ 
/*     */   protected final void writeD(int value)
/*     */   {
/*  42 */     this._bao.write(value & 0xFF);
/*  43 */     this._bao.write(value >> 8 & 0xFF);
/*  44 */     this._bao.write(value >> 16 & 0xFF);
/*  45 */     this._bao.write(value >> 24 & 0xFF);
/*     */   }
/*     */ 
/*     */   protected final void writeF(double org)
/*     */   {
/*  50 */     long value = Double.doubleToRawLongBits(org);
/*  51 */     this._bao.write((int)(value & 0xFF));
/*  52 */     this._bao.write((int)(value >> 8 & 0xFF));
/*  53 */     this._bao.write((int)(value >> 16 & 0xFF));
/*  54 */     this._bao.write((int)(value >> 24 & 0xFF));
/*  55 */     this._bao.write((int)(value >> 32 & 0xFF));
/*  56 */     this._bao.write((int)(value >> 40 & 0xFF));
/*  57 */     this._bao.write((int)(value >> 48 & 0xFF));
/*  58 */     this._bao.write((int)(value >> 56 & 0xFF));
/*     */   }
/*     */ 
/*     */   protected final void writeS(String text)
/*     */   {
/*     */     try
/*     */     {
/*  65 */       if (text != null)
/*     */       {
/*  67 */         this._bao.write(text.getBytes("UTF-16LE"));
/*     */       }
/*     */     }
/*     */     catch (Exception e)
/*     */     {
/*  72 */       e.printStackTrace();
/*     */     }
/*     */ 
/*  75 */     this._bao.write(0);
/*  76 */     this._bao.write(0);
/*     */   }
/*     */ 
/*     */   protected final void writeB(byte[] array)
/*     */   {
/*     */     try
/*     */     {
/*  83 */       this._bao.write(array);
/*     */     }
/*     */     catch (IOException e)
/*     */     {
/*  87 */       e.printStackTrace();
/*     */     }
/*     */   }
/*     */ 
/*     */   public final byte[] getContent() throws IOException
/*     */   {
/*  93 */     writeD(0);
/*     */ 
/*  95 */     int padding = this._bao.size() % 8;
/*  96 */     if (padding != 0)
/*     */     {
/*  98 */       for (int i = padding; i < 8; ++i)
/*     */       {
/* 100 */         writeC(0);
/*     */       }
/*     */     }
/*     */ 
/* 104 */     return this._bao.toByteArray();
/*     */   }
/*     */ }

/* Location:           D:\_l2j\libs\netcon.jar
 * Qualified Name:     org.netcon.BaseWritePacket
 * JD-Core Version:    0.5.3
 */