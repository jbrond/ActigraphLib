package com.video4coach.gt3x;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.text.ParseException;
import java.util.Vector;

import com.video4coach.Acceleration;
import com.video4coach.ActivityFile;

public class Gt3xFile extends ActivityFile {

	ActigraphInfoParser agip = new ActigraphInfoParser();
	private int sampleRate = 0; 
	
	private int readStartHeader(BufferedInputStream input) throws IOException {
		
		byte [] result = new byte[9];
		
		int bytesRead = input.read(result, 0, 8);
		
		if (bytesRead > 0 ) {					
			//Size of the next header
			return (int)(result[6] & 0xFF);
		}
		
		return -1;
	}
	
	private void readStartInfoHeader(BufferedInputStream input,int infoHeaderLength) throws IOException {
		
		if (infoHeaderLength <= 0)
			return;
		
		byte [] result = new byte[infoHeaderLength];
		
		int bytesRead = input.read(result, 0, infoHeaderLength);
		
	}
	
	//The data header includes
	//time stamp
	//temperature and other stuff????
	private int readDataHeader(BufferedInputStream input) throws IOException {
		byte [] result = new byte[20];
		int tBytesRead = 0;
		int bytesRead = input.read(result, 0, 9);
		
		tBytesRead = bytesRead;
		//
		//We need to extract more header
		//
		if (((int)(result[7]&0xFF)) == 02) {
			
			do {
				bytesRead = input.read(result, 0, 11);
				tBytesRead = tBytesRead + bytesRead;
				
			} while (((int)(result[9]&0xFF)) == 02);
		}
		
		return tBytesRead;
	}
	
	@Override
	public int load(Vector<Acceleration> rawData) {
		
		if (getFilename().isEmpty())
			return 0;
		//extract the activity file
		UnzipActivity unzipFile = new UnzipActivity();		
		unzipFile.extract(getFilename());
		
		log("Reading in log.bin from : " + getFilename());
		
		File file = new File(unzipFile.getFilenameAppend() + "_log.bin");	
		
		try {
			
			ActigraphInfo ai = agip.extractInfo(unzipFile.getFilenameAppend() + "_info.txt");
			
			sampleRate = ai.getSampleRate();
			
			log("Sample Rate: " + sampleRate);
		
		} catch (FileNotFoundException e1) {
			// We did not get the sample rate
			// Bail out
			e1.printStackTrace();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Max buffer length is 450 (100 Hz)
		byte[] result = new byte[450];
		short[] xyzBuffer = new short[9];
		
		int blockLength = (sampleRate / 2) * 9;
		
		//Something is wrong with the info file
		if (blockLength<=0)
			return 0;

		try {
			InputStream input = null;
			try {

				try {

					int totalBytesRead = 0;

					input = new BufferedInputStream(new FileInputStream(file));
										
					//Read 8 bytes from top header					
					int infoHeaderLength = readStartHeader((BufferedInputStream) input);
					
					if (infoHeaderLength < 0) {
						return 0;
					}
					//Read from bracket to end bracket
					readStartInfoHeader((BufferedInputStream) input, infoHeaderLength);
									
					int blocks = 0;
					int bytesRead = blockLength;
					
					totalBytesRead = 8 + infoHeaderLength;
					//Bail out if the readlength is shorter than
					//what we want
			
					while (bytesRead == blockLength) {

						int headerBytesLength = readDataHeader((BufferedInputStream)input);
						
						totalBytesRead += headerBytesLength;
						
						bytesRead = input.read(result, 0, blockLength);

						if (bytesRead > 0) {
							// Data has been read
							// convert to text

							// int startBlock = 0;
							int bl = 9;

							for (int i = 0; i < bytesRead; i += bl) {

								// converting to unsigned byte
								for (int n = 0; n < 9; n++)
									xyzBuffer[n] = (short) (result[i + n] & 0xff);

								// converting to doubles from 12 bit
								double x = convert2Complement2float((short) (xyzBuffer[0] << 4 | ((xyzBuffer[1] & 0xf0) >> 4)));

								double y = convert2Complement2float((short) (((xyzBuffer[1] & 0x0f) << 8) | xyzBuffer[2]));

								double z = convert2Complement2float((short) ((xyzBuffer[3] << 4) | ((xyzBuffer[4] & 0xf0) >> 4)));

								rawData.add(new Acceleration(x,y,z));

								x = convert2Complement2float((short) (((xyzBuffer[4] & 0x0f) << 8) | xyzBuffer[5]));

								y = convert2Complement2float((short) (xyzBuffer[6] << 4 | ((xyzBuffer[7] & 0xf0) >> 4)));

								z = convert2Complement2float((short) (((xyzBuffer[7] & 0x0f) << 8) | xyzBuffer[8]));

								rawData.add(new Acceleration(x,y,z));
							}
							blocks++;

							//System.out.println("Block: "+blocks+"  Bytes Read "+bytesRead);
							
							totalBytesRead += bytesRead;
							
							setLoadProgress((int) (file.length() / totalBytesRead));
							
						}

					}
					
					log("Number of blocks: " + blocks);
					
					setLoadProgress(100);

				} catch (Exception e) {// Catch exception if any
					System.err.println("Error: " + e.getMessage());
				}			

			} finally {
				 log("Closing input stream.");
				input.close();
			}

		} catch (FileNotFoundException ex) {
			 log("File not found.");
		} catch (IOException ex) {
			 log(ex.getMessage());
		}
		
		return 0;
	}

	private static double convert2Complement2float(int ui) {

		// ui = (ui & 0x800) ? ((0x7FF - (ui & 0x7FF)) * -1) : ui;

		if ((ui & 0x800) > 0) {
			return ((0x7FF - (ui & 0x7FF)) * -1) * 2.93 / 1000.0;
		}

		return (double) (ui * 2.93 / 1000.0);
	}

	

}
