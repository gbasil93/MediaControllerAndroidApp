package com.utility.mobile.mediacontroller.utils;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.BitSet;
import java.util.Date;
import java.util.UUID;

/**
 *
 * BitsAndByteHelper by default uses little endian byte array storage
 * @author saraya
 * @version $Revision: 1.1 $
 */
public abstract class BitsAndByteHelper {

	public static char[] hexChars_ =
			{'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

	/*
	 * mask off byte
	 */
	public static final short MASK_TO_BYTE = 0xFF;

	/*
	 * number of bits in a byte
	 */
	public static final int NUMBER_OF_BITS_IN_A_BYTE = 8;

	/*
	 * size of an int byte array
	 */
	public static final int SIZE_OF_AN_INT_IN_BYTES  = 4;

	/**
	 * copies a number of bytes from soure array to another
	 *
	 * @param source
	 * @param offset
	 * @param length
	 * @return
	 * @throws Exception
	 */
	public static byte[] copyBytes(byte[] source, int offset, int length)
			throws Exception {
		byte[] destination = new byte[length];

		int index = 0;
		for (int i=offset; i< offset+length; i++){
			destination[index] = source[i];
			index++;
		}
		return destination;

	}

	/**
	 *	copys a bit set into another bit set
	 */
	public BitSet copyBits(BitSet source, int offset, int length)
			throws Exception {
		return source.get(offset, offset+length);
	}

	/**
	 *  returns an int from a byte array
	 */
	public int getInt(byte b)
			throws Exception {
		return (int)(b & 0x00FF);
	}

	/**
	 * reads a long value from a byte array
	 * @param b
	 * @return
	 */
	public long readLong( byte[] b )
			throws Exception {
		return (long)readInt(b);
	}

	/**
	 * Used by default for most applications (little endian)
	 * reads an int value from a byte array
	 * @param b
	 * @return
	 */
	public static int readShort( byte[] b )
			throws Exception {
		if (b.length != 2){
			throw new Exception("a 2 bytes array is needed.");
		}
		int result = ( b[ 0 ] & 0xFF );
		result |= ( ( b[ 1 ] & 0xFF ) << 8 );
		return result;
	}

	/**
	 *
	 * reads an int value from a byte array
	 * @param b
	 * @return
	 */
	public static int readShortBigEndian( byte[] b )
			throws Exception {
		if (b.length != 2){
			throw new Exception("a 2 bytes array is needed.");
		}
		int result = ( b[ 1 ] & 0xFF );
		result |= ( ( b[ 0 ] & 0xFF ) << 8 );
		return result;
	}

	/**
	 * Used by default for most applications (little endian)
	 * writes an int as a byte array
	 * @param value
	 * @param value
	 */
	public static byte[] writeShort(short value )
			throws Exception {
		byte[] b = new byte[2];
		b[0] = (byte)(value & 0xFF);
		value >>= 8;
		b[1] = (byte)(value & 0xFF);
		value >>= 8;
		return b;
	}

	/**
	 * writes an int as a byte array
	 * @param value
	 * @param value
	 */
	public static byte[] writeShortBigEndian(short value )
			throws Exception {
		byte[] b = new byte[2];
		b[1] = (byte)(value & 0xFF);
		value >>= 8;
		b[0] = (byte)(value & 0xFF);
		value >>= 8;
		return b;
	}

	/**
	 * reads an int value from a byte array
	 * @param b
	 * @return
	 */
	public static int readInt( byte[] b )
			throws Exception {
		int result = (   b[ 0 ] & 0xFF );
		result    |= ( ( b[ 1 ] & 0xFF ) << 8 );
		result    |= ( ( b[ 2 ] & 0xFF ) << 16 );
		result    |= ( ( b[ 3 ] & 0xFF ) << 24 );
		return result;
	}

	/**
	 * writes an int as a byte array
	 * @param value
	 * @param value
	 */
	public static byte[] writeInt(int value )
			throws Exception {
		byte[] b = new byte[4];
		b[0] = (byte)(value & 0xFF);
		value >>= 8;
		b[1] = (byte)(value & 0xFF);
		value >>= 8;
		b[2] = (byte)(value & 0xFF);
		value >>= 8;
		b[3] = (byte)(value & 0xFF);
		return b;
	}

	/**
	 * writes an int as a byte array
	 * @param b
	 * @param value
	 */
	public static void writeInt(byte[] b, int value )
			throws Exception {
		if (b.length < 4){
			throw new Exception("minimum number of bytes is 4.");
		}
		b[0] = (byte)(value & 0xFF);
		value >>= 8;
		b[1] = (byte)(value & 0xFF);
		value >>= 8;
		b[2] = (byte)(value & 0xFF);
		value >>= 8;
		b[3] = (byte)(value & 0xFF);
	}

	/**
	 *  returns an int from a byte array
	 */
	public int getInt(byte[] buffer, int offset, int length)
			throws Exception {
		int j = length;
		byte[] data = new byte[4];
		for (int i=offset; i < offset+length; i++){
			data[data.length-j] = buffer[i];
			j--;
		}
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		return in.readInt();
	}

	/**
	 * returns a long as a byte array
	 * @param l
	 * @return
	 * @throws Exception
	 */
	public static byte[] writeLong(long l)
			throws Exception {
		byte[] b = new byte[4];
		b[0] = (byte)((l >> 24) & 0xff);
		b[1] = (byte)((l >> 16) & 0xff);
		b[2] = (byte)((l >> 8) & 0xff);
		b[3] = (byte)(l & 0xff);
		return b;
	}


	/**
	 *  returns a long from a byte array
	 */
	public long getLong(byte[] buffer, int offset, int length)
			throws Exception {
		return (long)getInt(buffer, offset, length);
	}

	/**
	 * Convert a hex string to a byte array.
	 * Permits upper or lower case hex.
	 *
	 * @param s String must have even number of characters.
	 * and be formed only of digits 0-9 A-F or
	 * a-f. No spaces, minus or plus signs.
	 * @return corresponding byte array.
	 */
	public static byte[] fromHexString ( String s ){
		int stringLength = s.length();
		if ( (stringLength & 0x1) != 0 ){
			throw new IllegalArgumentException( "fromHexString requires an even number of hex characters" );
		}
		byte[] b = new byte[stringLength / 2];

		for ( int i=0,j=0; i<stringLength; i+=2,j++ ){
			int high = charToNibble( s.charAt ( i ) );
			int low = charToNibble( s.charAt ( i+1 ) );
			b[j] = (byte)( ( high << 4 ) | low );
		}
		return b;
	}

	/**
	 * Returns a bitset containing the values in bytes.
	 * The byte-ordering of bytes must be big-endian which means
	 * the most significant bit is in element 0.
	 *
	 * @param bytes
	 * @return
	 */
	public static BitSet fromByteArray(byte[] bytes) {
		BitSet bits = new BitSet();
		for (int i=0; i<bytes.length*8; i++) {
			if ((bytes[bytes.length-i/8-1]&(1<<(i%8))) > 0) {
				bits.set(i);
			}
		}
		return bits;
	}

	/**
	 * Returns a byte array of at least length 1.
	 * The most significant bit in the result is guaranteed not to be a 1
	 * (since BitSet does not support sign extension).
	 * The byte-ordering of the result is big-endian which means
	 * the most significant bit is in element 0.
	 *
	 * The bit at index 0 of the bit set is assumed to be the least significant bit.
	 * @param bits
	 * @return
	 */
	public static byte[] toByteArray(BitSet bits) {
		byte[] bytes = new byte[bits.length()/8+1];
		for (int i=0; i<bits.length(); i++) {
			if (bits.get(i)) {
				bytes[bytes.length-i/8-1] |= 1<<(i%8);
			}
		}
		return bytes;
	}

	/**
	 * Convert byte array to a hex string
	 *	@param buf a byte array
	 *	@return a hex string
	 */
	public static String toHexString(byte[] buf)
			throws Exception {
		return toHexString(buf, buf.length);
	}

	/**
	 * Convert byte array to a hex string
	 *	@param buf a byte array
	 *	@param bytesRead the number of byte to convert
	 *	@return a hex string
	 */
	public static String toHexString(byte[] buf, int bytesRead)
			throws Exception {
		StringBuffer sb = new StringBuffer(bytesRead);
		for (int i=0; i<bytesRead; i++){
			String hex = Integer.toHexString(0x0100 + (buf[i] & 0x00FF)).substring(1);
			sb.append((hex.length()<2 ? "0" : "") + hex);
		}
		return sb.toString();
	}

	/**
	 * Decode an array of hex chars.
	 *
	 * @param hexString an array of hex characters.
	 * @return the decode hex chars as bytes.
	 */
	public static byte[] toByteArray(String hexString){
		char[] hexChars = hexString.toCharArray();
		int startIndex = 0;
		int length = hexString.length();

		if ((length & 1) != 0)
			throw new IllegalArgumentException("Length must be even");

		byte[] result = new byte[length / 2];
		for (int j = 0; j < result.length; j++)
		{
			result[j] = (byte)( charToNibble(hexChars[startIndex++]) * 16 + charToNibble(hexChars[startIndex++]));
		}
		return result;
	}

	/**
	 * convert a single char to corresponding nibble.
	 *
	 * @param c char to convert. must be 0-9 a-f A-F, no
	 * spaces, plus or minus signs.
	 *
	 * @return corresponding integer
	 */
	private static int charToNibble ( char c ){
		if ( '0' <= c && c <= '9' ){
			return c - '0';
		}else if ( 'a' <= c && c <= 'f' ){
			return c - 'a' + 0xa;
		}else if ( 'A' <= c && c <= 'F' ){
			return c - 'A' + 0xa;
		}else{
			throw new IllegalArgumentException( "Invalid hex character: " + c );
		}
	}

	/**
	 *
	 * @param value
	 * @return
	 */
	public static byte[] intToByteArray(int value) {
		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) {
			int offset = (b.length - 1 - i) * 8;
			b[i] = (byte) ((value >>> offset) & 0xFF);
		}
		return b;
	}

	/**
	 * returns a java date from a byte array
	 * @param message
	 * @return
	 * @throws Exception
	 */
	public static Date getDate(byte[] message)
			throws Exception {
		byte[] b =  copyBytes(message, 0, 4);
		String hex = toHexString(b, b.length);
		long time = Long.parseLong(hex, 16)*1000;
		//long time = Long.parseLong(hex, 16);
		return new Date(time);
	}

	/**
	 * Convenience method to convert a byte to a hex string.
	 *
	 * @param data the byte to convert
	 * @return String the converted byte
	 */
	public String byteToHex(byte data){
		StringBuffer buf = new StringBuffer();
		buf.append(toHexChar((data>>>4)&0x0F));
		buf.append(toHexChar(data&0x0F));
		return buf.toString();
	}

	/**
	 * Convenience method to convert a byte array to a hex string.
	 *
	 * @param data the byte[] to convert
	 * @return String the converted byte[]
	 */
	public String bytesToHex(byte[] data){
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++)
		{
			buf.append(byteToHex(data[i]));
		}
		return buf.toString();
	}

	/**
	 * Convenience method to convert an int to a hex char.
	 *
	 * @param i the int to convert
	 * @return char the converted char
	 */
	public char toHexChar(int i){
		if ((0 <= i) && (i <= 9 ))
			return (char)('0' + i);
		else
			return (char)('a' + (i-10));
	}

	/**
	 * returns a byte array as long
	 * @param message
	 * @return
	 * @throws Exception
	 */
	public static long getDateAsLong(byte[] message)
			throws Exception {
		byte[] b = copyBytes(message, 0, 4);
		String hex = toHexString(b, b.length);
		long time = Long.parseLong(hex, 16)*1000;
		return time;
	}

	/**
	 * gets the checksum for the request
	 * @param request
	 * @return
	 */
	public static byte getCheckSum(byte[] request){
		byte checksum = 0;
		for (int i=0; i<(request.length - 1); i++){
			checksum += request[i];
		}
		return checksum;
	}

	/**
	 * calculates the two's complement of the supplied parameter value
	 * @param param
	 * @return
	 */
	public static byte twosComplement(byte param) {
		int i = 0 - byteToShort(param);
		i = i & 0xFF;
		return (byte)i;
	}

	/**
	 * returns a byte as a short
	 * @param b
	 * @return
	 */
	public static short byteToShort (byte b) {
		short sh = (short)(b);
		if (sh < 0) sh = (short)(sh + 256);
		return sh;
	}

	/**
	 * Added 08/15/2014 for long conversions
	 *
	 * @param v
	 * @return
	 */
	public static byte[] write64bitLong(long v) {
		byte[] writeBuffer = new byte[8];
		writeBuffer[0] = (byte)(v >>> 56);
		writeBuffer[1] = (byte)(v >>> 48);
		writeBuffer[2] = (byte)(v >>> 40);
		writeBuffer[3] = (byte)(v >>> 32);
		writeBuffer[4] = (byte)(v >>> 24);
		writeBuffer[5] = (byte)(v >>> 16);
		writeBuffer[6] = (byte)(v >>>  8);
		writeBuffer[7] = (byte)(v >>>  0);
		return writeBuffer;
	}

	public static long read64bitLong(byte[] b) throws Exception {
		if (b.length != 8) {
			throw new Exception("Must be 8 bytes");
		}

		return (((long)b[0] << 56) +
				((long)(b[1] & 255) << 48) +
				((long)(b[2] & 255) << 40) +
				((long)(b[3] & 255) << 32) +
				((long)(b[4] & 255) << 24) +
				((b[5] & 255) << 16) +
				((b[6] & 255) <<  8) +
				((b[7] & 255) <<  0));
	}

	public static String getPaddedShort(short value) throws Exception
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream(2);
		DataOutputStream dos = new DataOutputStream(bos);
		dos.writeShort(value);
		return BitsAndByteHelper.toHexString(bos.toByteArray());
	}

	public static String getPaddedInt(int value) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(4);
		DataOutputStream dos = new DataOutputStream(bos);
		dos.writeInt(value);
		return BitsAndByteHelper.toHexString(bos.toByteArray());
	}

	public static final byte[] getGUIDBytes(String guid)
	{
		UUID uuid = UUID.fromString(guid);
		byte[] uuidBytes = new byte[16];
		System.arraycopy(BitsAndByteHelper.write64bitLong(uuid.getLeastSignificantBits()),0, uuidBytes, 0,8);
		System.arraycopy(BitsAndByteHelper.write64bitLong(uuid.getMostSignificantBits()),0, uuidBytes, 8, 8);

		return uuidBytes;
	}

	public static byte[] convertMacToBytes(String mac) throws Exception {
		byte[] values = new byte[6];
		if (mac.length() == 17) {
			for (int i = 0; i < 6; i++) {
				String hex = mac.substring(i*3, i*3+2);
				values[i] = (byte) Integer.parseInt(hex, 16);
			}
		} else {
			throw new Exception("Wrong MAC address length");
		}
		return values;
	}

	public static String convertMacBytesToString(byte[] mac) throws Exception {
		String sMac = "";
		if (mac.length == 6) {
			for (int i = 0; i < 6; i++) {
				try {
					sMac += Integer.toHexString(mac[i] & 0xff);
				} catch (Exception e) {
					sMac += "00";
				}
				if (i != 5) {
					sMac += ":";
				}
			}
		} else {
			throw new Exception("MAC array wrong length");
		}
		return sMac;
	}

	public static byte setBit(byte value, int bit){
		value = (byte)(value | (1 << bit));

		return value;
	}

	public static boolean testBit(int value, int bit) {
		if (((value >>> bit) & 1) != 0) {
			return true;
		}

		return false;
	}
}
