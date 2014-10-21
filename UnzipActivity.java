package com.video4coach.gt3x;

import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Enumeration;
import java.io.*;

public class UnzipActivity {

	private String filenameAppend;
	
	public String getFilenameAppend() {
		return filenameAppend;
	}

	public void setFilenameAppend(String filenameAppend) {
		this.filenameAppend = filenameAppend;
	}

	public void extract(String gt3xFilename) {

		try {

			// Take the filename from the input arguments
			// String zipname = args[0];

			FileInputStream fis = new FileInputStream(gt3xFilename);

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

				if (!(entry.getName().equalsIgnoreCase("log.bin") == true || entry
						.getName().equals("info.txt") == true))
					continue;

				int size;
				byte[] buffer = new byte[2048];
				
				//Should create new entry name in unique folder
				String [] fnentries = gt3xFilename.replaceAll(".gt3x", "").split(""+File.separatorChar);
				filenameAppend = fnentries[fnentries.length-1];
				
				FileOutputStream fos = new FileOutputStream(filenameAppend + "_" + entry.getName());
				
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

		return;
	}

}
