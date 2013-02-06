package com.video4coach;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ConvertGT3x {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("usage: java ZipAccess zipfile");
			return;
		}

		int value = 0xF91;
		
		value = (value & 0x800) > 0 ? (0x800 - (value & 0x7FF)) * -1 : value;
		
		double v = (((double)value * 2.93f) / 1000.0);
		log("Value: " + value + " v: " + v);
		// Extract the activity file

		if (unzipActivityFile(args[0]) == true) {
			convertFile("activity.bin");
		}
	}

	private static boolean convertFile(String filename) {
		log("Reading in binary file named : " + filename);
		File file = new File(filename);
		log("File size: " + file.length());
		
		//What should be a good buffer length????
		byte[] result = new byte[4096];
		short[] xyzBuffer = new short[9];
		int[] activityXYZ = new int[3];
		
		try {
			InputStream input = null;
			try {
				
				try {
					
					//open text file for writing
					
					FileWriter fstream = new FileWriter("data.txt");
			        BufferedWriter out = new BufferedWriter(fstream);
			        
				int totalBytesRead = 0;

				input = new BufferedInputStream(new FileInputStream(file));

				while (totalBytesRead < file.length()) {

					int bytesRemaining = (int)file.length() - totalBytesRead;
					// input.read() returns -1, 0, or more :
					
					//Standard read os 4095 bytes
					//dividable with 9 to fit the 9 byte data window of the gt3x data file
					int bytesRead = input.read(result, 0, 4095);
					
					if (bytesRead > 0) {
						//Data has been read
						//convert to text
						
						//int startBlock = 0;
						int blockLength = 9;
						
						for (int i = 0; i < bytesRead; i+=blockLength) {
							
							//converting to unsigned byte
							for (int n = 0;n<9;n++)
								xyzBuffer[n] = (short) (result[i+n] & 0xff);
							
							//converting to doubles from 12 bit
							double x = convert2Complement2float((short)(xyzBuffer[0] << 4 | ((xyzBuffer[1] & 0xf0) >> 4)));
						
							double y = convert2Complement2float( (short) (((xyzBuffer[1] & 0x0f) << 8) | xyzBuffer[2]));
						
							double z = convert2Complement2float( (short) ((xyzBuffer[3] << 4) | ((xyzBuffer[4] & 0xf0) >> 4)));
						
							//write x y z to text file
							String cvsStr = String.format("%2.3f,%2.3f,%2.3f%n", x,y,z);
							
							out.write(cvsStr);
							
							x = convert2Complement2float( (short) (((xyzBuffer[4] & 0x0f) << 8) | xyzBuffer[5]));
							
							y = convert2Complement2float((short)(xyzBuffer[6] << 4 | ((xyzBuffer[7] & 0xf0) >> 4)));
							
							z = convert2Complement2float( (short) (((xyzBuffer[7] & 0x0f) << 8) | xyzBuffer[8]));
						
							//Write x y z to text file
							
							cvsStr = String.format("%2.3f,%2.3f,%2.3f%n", x,y,z);
							out.write(cvsStr);
						}
						//log("X: "+x+" Y: "+y+" Z: "+z);
						
						totalBytesRead = totalBytesRead + bytesRead;
					}

				}
				//Close text file
				out.close();
				log("Num bytes read: " + totalBytesRead);
				
				} catch (Exception e) {//Catch exception if any
				      System.err.println("Error: " + e.getMessage());
				}
				/*
				 * the above style is a bit tricky: it places bytes into the
				 * 'result' array; 'result' is an output parameter; the while
				 * loop usually has a single iteration only.
				 */
				
			} finally {
				log("Closing input stream.");
				input.close();
			}
		
		} catch (FileNotFoundException ex) {
			log("File not found.");
		} catch (IOException ex) {
			log(ex.getMessage());
		} 
			
		return true;
	}

	private static void log(String text) {
		System.out.println(text);
	}

	private static double convert2Complement2float(int ui) {
		
		//ui = (ui & 0x800) ? ((0x7FF - (ui & 0x7FF)) * -1) : ui;
		
		if ((ui & 0x800) > 0) {
			return ((0x7FF - (ui & 0x7FF)) * -1) * 2.93 / 1000.0;
		} 
		
		return (double)(ui * 2.93 / 1000.0);
	}
	
	private static boolean unzipActivityFile(String zipname) {

		try {

			// Take the filename from the input arguments
			// String zipname = args[0];

			FileInputStream fis = new FileInputStream(zipname);

			//
			// Creating input stream that also maintains the checksum of the
			// data which later can be used to validate data integrity.
			//
			CheckedInputStream checksum = new CheckedInputStream(fis,
					new Adler32());
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
					checksum));
			ZipEntry entry;

			//
			// Read each entry from the ZipInputStream until no more entry found
			// indicated by a null return value of the getNextEntry() method.
			//
			while ((entry = zis.getNextEntry()) != null) {
				System.out.println("Unzipping: " + entry.getName());

				if (! (entry.getName().equalsIgnoreCase("activity.bin") == true ||
						entry.getName().equals("info.txt") == true) )
					continue;

				int size;
				byte[] buffer = new byte[2048];

				FileOutputStream fos = new FileOutputStream(entry.getName());
				BufferedOutputStream bos = new BufferedOutputStream(fos,
						buffer.length);

				while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
					bos.write(buffer, 0, size);
				}
				bos.flush();
				bos.close();
			}

			zis.close();
			fis.close();

			//
			// Print out the checksum value
			//
			System.out.println("Checksum = "
					+ checksum.getChecksum().getValue());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

}
